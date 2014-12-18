/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.andes.server.cassandra;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.AMQException;
import org.wso2.andes.AMQInternalException;
import org.wso2.andes.AMQStoreException;
import org.wso2.andes.server.AMQChannel;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.binding.Binding;
import org.wso2.andes.server.cluster.GlobalQueueWorker;
import org.wso2.andes.server.cluster.coordination.CoordinationException;
import org.wso2.andes.server.exchange.Exchange;
import org.wso2.andes.server.queue.AMQQueue;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.wso2.andes.server.store.util.CassandraDataAccessException;
import org.wso2.andes.server.subscription.SubscriptionImpl;
import org.wso2.andes.server.util.AndesConstants;
import org.wso2.andes.server.util.AndesUtils;
import org.wso2.andes.server.util.QueueMessageRemovalLock;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DefaultClusteringEnabledSubscriptionManager implements ClusteringEnabledSubscriptionManager{

    private static Log log = LogFactory.getLog(DefaultClusteringEnabledSubscriptionManager.class);
    private static final Log traceLog = LogFactory.getLog(AndesConstants.TRACE_LOGGER);

    /**
     * keep in memory map of <destination queue, list of node queues having
     * subscriptions for destination queue> across whole cluster
     */
    private ConcurrentHashMap<String,List<String>> globalSubscriptionsMap = new ConcurrentHashMap<String,List<String>>();

    //keep in memory map of destination queues created in the cluster
    private ConcurrentHashMap<String,String> destinationQueues = new ConcurrentHashMap<String,String>();


//    private ReadWriteLock inMemorySubscriptionListUpdateReadWriteLock = new ReentrantReadWriteLock();

    private Map<String,QueueDeliveryWorker> workMap =
            new ConcurrentHashMap<String,QueueDeliveryWorker>();

    /**
     * Keeps Subscription that have for this given queue locally
     */
    private Map<String,Map<String,CassandraSubscription>> subscriptionMap =
            new ConcurrentHashMap<String,Map<String,CassandraSubscription>>();



    private ExecutorService messageFlusherExecutor =  null;
    private SequentialThreadPoolExecutor messageDeliveryExecutor = null;



    /**
     * Hash map that keeps the unacked messages.
     */
    private Map<AMQChannel, Map<Long, Semaphore>> unAckedMessagelocks =
            new ConcurrentHashMap<AMQChannel, Map<Long, Semaphore>>();


    private Map<AMQChannel,QueueSubscriptionAcknowledgementHandler> acknowledgementHandlerMap =
            new ConcurrentHashMap<AMQChannel,QueueSubscriptionAcknowledgementHandler>();

    private int queueWorkerWaitInterval;


    public void init()  {
        ThreadFactory qDWNamedFactory = new ThreadFactoryBuilder().setNameFormat("QueueDeliveryWorker-%d").build();
        messageFlusherExecutor =  Executors.newFixedThreadPool(ClusterResourceHolder.getInstance().getClusterConfiguration().
                                      getFlusherPoolSize(), qDWNamedFactory);
        messageDeliveryExecutor = new SequentialThreadPoolExecutor((ClusterResourceHolder.getInstance().getClusterConfiguration().
                getPublisherPoolSize()), "AsyncQueueDelivery");
        queueWorkerWaitInterval = ClusterResourceHolder.getInstance().getClusterConfiguration().
                getQueueWorkerInterval();
        clearAndUpdateDestinationQueueList();
    }

    /**
     * Register a subscription for a Given Queue
     * This will handle the subscription addition task.
     * @param queue
     * @param subscription
     */
    public void addSubscription(AMQQueue queue, CassandraSubscription subscription) throws AMQException, CoordinationException {
            List<Binding> bindingList = queue.getBindings();
            if(bindingList !=null && !bindingList.isEmpty())
            for(Binding b : bindingList) {

                Exchange exchange = b.getExchange();
                if (exchange.getName().equalsIgnoreCase("amq.direct")) {
                    //this is handled on a different path
                }
                if (exchange.getName().equalsIgnoreCase("amq.topic")) {
                     if(log.isDebugEnabled()) {
                         log.debug("adding subscription for binding - " + b.getQueue() + "-" + b.getBindingKey());
                     }
                    //register subscription
                    String topicNodeQueueName = AndesUtils.getTopicNodeQueueName();

                    //if this is a durable subscription check same queue exists already cluster-wide, if so do not allow the subscription
                    if (queue.isExclusive()) {
                        if(log.isDebugEnabled()) {
                            log.debug("Checking if an exclusive subscription exists cluster wide");
                        }
                        boolean durableTopicSubscriberExists = ClusterResourceHolder.getInstance().
                                getCassandraMessageStore().checkIfDuableExclusiveSubscriptionAlreadyExists(queue.getResourceName());
                        if(log.isDebugEnabled()) {
                            log.debug("check for an exclusive subscription. Result: " + durableTopicSubscriberExists);
                        }
                        if (durableTopicSubscriberExists) {
                            throw new AMQQueue.ExistingExclusiveSubscription();
                        }
                    }
                    ClusterResourceHolder.getInstance().getCassandraMessageStore().registerSubscriberForTopic
                            (b.getBindingKey(), topicNodeQueueName, queue.getResourceName(), queue.isDurable(), true);
                    if(log.isDebugEnabled()) {
                        log.debug("DCESM - Adding subscriber for " + topicNodeQueueName + "binding key " + b.getBindingKey() + " queueName " + queue.getResourceName());
                    }

                    //now we have a subscription on this node. Start a topicDeliveryWorker if one has not started
                    if (!ClusterResourceHolder.getInstance().getTopicDeliveryWorker().isWorking()) {
                        ClusterResourceHolder.getInstance().getTopicDeliveryWorker().setWorking();
                    }
                    try {
                        //notify the subscription change
                        ClusterResourceHolder.getInstance().getTopicSubscriptionCoordinationManager().notifyTopicSubscriptionChange(b.getBindingKey());
                        if(log.isDebugEnabled()) {
                            log.debug("DCESM - notifying topic subscription change " + b.getBindingKey());
                        }
                    } catch (CoordinationException e) {
                        throw new AMQInternalException("Error in notifying subscription change when adding subscription", e);
                    }
                }
            }
            if (subscription.getSubscription() instanceof SubscriptionImpl.BrowserSubscription) {
                boolean isInMemoryMode = ClusterResourceHolder.getInstance().getClusterConfiguration().isInMemoryMode();
                QueueBrowserDeliveryWorker deliveryWorker = new QueueBrowserDeliveryWorker(subscription.getSubscription(),queue,subscription.getSession(),isInMemoryMode);
                deliveryWorker.send();
            } else {

                Map<String, CassandraSubscription> subscriptions = subscriptionMap.get(queue.getResourceName());

                if (subscriptions == null || subscriptions.size() == 0) {
                    synchronized (subscriptionMap) {
                        subscriptions = subscriptionMap.get(queue.getResourceName());
                        if (subscriptions == null || subscriptions.size() == 0) {
                            subscriptions = subscriptionMap.get(queue.getResourceName());
                            if (subscriptions == null) {
                                subscriptions = new ConcurrentHashMap<String, CassandraSubscription>();
                                subscriptions.put(subscription.getSubscription().getSubscriptionID() + "",
                                        subscription);
                                subscriptionMap.put(queue.getResourceName(), subscriptions);
                                //for topic subscriptions no need to handleSubscription
                                if(queue.isDurable() || !queue.checkIfBoundToTopicExchange()) {
                                    handleSubscription(queue);
                                }
                            } else if (subscriptions.size() == 0) {
                                subscriptions.put(subscription.getSubscription().getSubscriptionID() + "",
                                        subscription);
                                //for topic subscriptions no need to handleSubscription
                                if(queue.isDurable() || !queue.checkIfBoundToTopicExchange()) {
                                    handleSubscription(queue);
                                }
                            }
                            incrementSubscriptionCount(true,queue.getResourceName());
                            if (traceLog.isTraceEnabled()) {
                                traceLog.trace("TRACING>> DCESM- Called Increment sub count for-" + queue.getResourceName() + "-with instantiateColumn=true");
                            }
                        } else {

                            subscriptions.put(subscription.getSubscription().getSubscriptionID() + "", subscription);
                        }
                    }
                } else {
                    subscriptions.put(subscription.getSubscription().getSubscriptionID() + "", subscription);
                    incrementSubscriptionCount(false, queue.getResourceName());
                    if (traceLog.isTraceEnabled()) {
                        traceLog.trace("TRACING>> DCESM- Called Increment sub count for-" + queue.getResourceName() + "-with instantiateColumn=false");
                    }
                }

                if(log.isDebugEnabled()) {
                    log.debug("Binding Subscription "+subscription.getSubscription().getSubscriptionID()+" to queue "+queue.getName());
                }

            }
            //if in stand-alone mode update destination queue-node queue map and reset global queue workers
            if(!ClusterResourceHolder.getInstance().getClusterConfiguration().isClusteringEnabled()) {
              
                    List<String> destinationQueuesHavingFreshSubscriptions = ClusterResourceHolder.getInstance().
                            getSubscriptionManager().updateNodeQueuesForDestinationQueueMap();
                    //say global queue workers of this node to read from beginning for above queues
                    for(String destinationQueue : destinationQueuesHavingFreshSubscriptions) {
                        String globalQueueName = AndesUtils.getGlobalQueueNameForDestinationQueue(destinationQueue);
                        ClusterResourceHolder.getInstance().getClusterManager().getGlobalQueueManager().resetGlobalQueueWorkerIfRunning(globalQueueName);
                    }
                
            } else {
                //notify the subscription change to the cluster nodes
                if(log.isDebugEnabled()) {
                    log.debug("Notifying queue subscription change to the cluster");
                }
                ClusterResourceHolder.getInstance().getSubscriptionCoordinationManager().handleSubscriptionChange();
            }
    }

    public List<String> getDestinationQueues() {
        return new ArrayList<String>(destinationQueues.keySet());
    }
    /**
     * Clear and update destination queue list
     */
    public void clearAndUpdateDestinationQueueList() {

            destinationQueues.clear();
            log.debug("DCESM-ClearAndUpdateDestinationQueueList- Cleared Destination Queues");
            try {
                List<String> destinationQueues = ClusterResourceHolder.getInstance().getCassandraMessageStore().getDestinationQueues();
                for(String q : destinationQueues) {
                    this.destinationQueues.put(q,"");
                    log.debug("DCESM- ClearAndUpdateDestinationQueueList >> added queue" + q + "to destinationQueues");
                }
            } catch (AMQStoreException e) {
                log.error("Error in updating in-memory list of destination queues" , e);
            }
        }

    /**
     * update in memory map keeping which nodes has subscriptions for given destination queue name
     */
    public List<String> updateNodeQueuesForDestinationQueueMap() {
            List<String> destinationQueuesHavingFreshSubscriptions = new ArrayList<String>();
            try {
                log.debug("TRACING>> DCESM- UpdateNodeQueuesForDestinationQueueMap called. Updating globalSubscriptionsMap ");
                //record subscription count in cluster for each destination queue before update
                HashMap<String, Integer> destinationQueuesNumOfSubscriptionMap = new HashMap<String, Integer>();
                Enumeration destinationQueuesEnumeration = destinationQueues.keys();
                while (destinationQueuesEnumeration.hasMoreElements()){
                    String destinationQueue =  (String)destinationQueuesEnumeration.nextElement();
                    if (globalSubscriptionsMap.get(destinationQueue) == null) {
                        destinationQueuesNumOfSubscriptionMap.put(destinationQueue, 0);
                    } else {
                        destinationQueuesNumOfSubscriptionMap.put(destinationQueue, globalSubscriptionsMap.get(destinationQueue).size());
                    }
                }
                CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
                globalSubscriptionsMap.clear();
                log.debug("TRACING>> DCESM- Cleared globalSubscriptionsMap");


                Enumeration destinationQueuesEnumeration2 = destinationQueues.keys();
                while (destinationQueuesEnumeration2.hasMoreElements()){
                    String destinationQueueName = (String)destinationQueuesEnumeration2.nextElement();
                    List<String> nodeQueuesList = messageStore.getNodeQueuesForDestinationQueue(destinationQueueName);
                    if (log.isDebugEnabled()) {
                        log.debug("TRACING>> DCESM- Read node queues with size-" + nodeQueuesList.size() + "-for destination queue-" + destinationQueueName);
                    }
                    if (nodeQueuesList.size() > 0) {
                        List<String> queueList = new ArrayList<String>();
                        for (String nodeQueueName : nodeQueuesList) {
                            long subscriptionCount =  messageStore.getSubscriptionCountForQueue(destinationQueueName, nodeQueueName);
                            log.debug("TRACING>> DCESM-Subscription count from cassandra for Destination queue-" + destinationQueueName + "-in node queue-" + nodeQueueName + "-is-" + subscriptionCount);
                            for (long i = 0; i < subscriptionCount; i++) {
                                queueList.add(nodeQueueName);
                            }
                        }
                        globalSubscriptionsMap.put(destinationQueueName, queueList);
                        if (log.isDebugEnabled()) {
                            log.debug("TRACING>> DCESM-UpdateNodeQueuesForDestinationQueueMap >> added queueList of size " + queueList.size() + "to destination queue -" + destinationQueueName);
                        }
                    }
                }
                //compare with previous state and identify to which queues subscriptions newly came
                Enumeration destinationQueuesEnumeration3 = destinationQueues.keys();
                while (destinationQueuesEnumeration3.hasMoreElements()){
                    String destinationQueue = (String)destinationQueuesEnumeration3.nextElement();
                    int currentSubscriptionCount;
                    if(globalSubscriptionsMap.get(destinationQueue) == null) {
                        currentSubscriptionCount = 0;
                    } else {
                        currentSubscriptionCount = globalSubscriptionsMap.get(destinationQueue).size();
                    }
                    int previousSubscriptionCount = 0;
                    if(destinationQueuesNumOfSubscriptionMap.containsKey(destinationQueue)){
                       previousSubscriptionCount = destinationQueuesNumOfSubscriptionMap.get(destinationQueue);
                    }
                    if (currentSubscriptionCount > 0 && previousSubscriptionCount == 0) {
                        destinationQueuesHavingFreshSubscriptions.add(destinationQueue);
                    }
                }

                List<GlobalQueueWorker> globalQueueWorkerList = ClusterResourceHolder.getInstance().getClusterManager().getGlobalQueueManager().getGlobalQueueWorkersInThisNode();
                for(GlobalQueueWorker globalQueueWorker : globalQueueWorkerList){
                    globalQueueWorker.wakeUpGlobalQueueWorker();
                }
            }
            catch (CassandraDataAccessException ce){
                log.error("Error in getting the Node Queues as cassandra connection is down");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return destinationQueuesHavingFreshSubscriptions;


    }

    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    /**
     * Handle Subscription removal for a queue.
     * @param queue  queue for this Subscription
     * @param subId  SubscriptionId
     */
    public void removeSubscription(String queue, String subId, boolean isBoundToTopics ) {
        synchronized (queueNamesWithNoSubscriptions) {
            try {
                Map<String,CassandraSubscription> subs = subscriptionMap.get(queue);
                if (subs != null && subs.containsKey(subId)) {
                    subs.remove(subId);
                    log.debug("Removing Subscription " + subId + " from queue " + queue);
                    if(log.isDebugEnabled()) {
                        log.debug("TRACING>> DCESM - Removing subscription with id " + subId + " from queue " + queue);
                    }
                    ClusterResourceHolder.getInstance().getCassandraMessageStore().decrementSubscriptionCount(queue, AndesUtils.getMyNodeQueueName(), 1L);
                    if (subs.size() == 0) {
                        queueNamesWithNoSubscriptions.add(queue);
                    }
                }
            } catch (Exception e) {
                log.error("Error while removing subscription for queue: " + queue,e);
            }
        }

        try {
            if(ClusterResourceHolder.getInstance().getClusterConfiguration().isClusteringEnabled()) {
                ClusterResourceHolder.getInstance().getSubscriptionCoordinationManager().handleSubscriptionChange();
            } else {
                //update destination queue node queue map
                List<String> destinationQueuesHavingFreshSubscriptions = ClusterResourceHolder.getInstance().
                        getSubscriptionManager().updateNodeQueuesForDestinationQueueMap();
                //say global queue workers of this node to read from beginning for above queues
                for(String destinationQueue : destinationQueuesHavingFreshSubscriptions) {
                    String globalQueueName = AndesUtils.getGlobalQueueNameForDestinationQueue(destinationQueue);
                    ClusterResourceHolder.getInstance().getClusterManager().getGlobalQueueManager().resetGlobalQueueWorkerIfRunning(globalQueueName);
                }
            }

        } catch (Exception e) {
            log.error("Error while notifying Subscription change");
        }

    }

    /**
     * get a List of nodes having subscriptions to the given destination queue
     * @param destinationQueue destination queue name
     * @return list of nodes
     */
    public List<String> getNodeQueuesHavingSubscriptionsForQueue(String destinationQueue) {
        return globalSubscriptionsMap.get(destinationQueue);
    }

    private HashSet<String> queueNamesWithNoSubscriptions = new HashSet<String>();


    public void  handleFreshSubscriptionsJoiningToCluster() {
        //update destination queue node queue map
        List<String> destinationQueuesHavingFreshSubscriptions = updateNodeQueuesForDestinationQueueMap();
        //say global queue workers of this node to read from beginning for above queues
        for(String destinationQueue : destinationQueuesHavingFreshSubscriptions) {
            String globalQueueName = AndesUtils.getGlobalQueueNameForDestinationQueue(destinationQueue);
            ClusterResourceHolder.getInstance().getClusterManager().getGlobalQueueManager().resetGlobalQueueWorkerIfRunning(globalQueueName);
        }

        synchronized (queueNamesWithNoSubscriptions) {
            Iterator iterator = queueNamesWithNoSubscriptions.iterator() ;
            while (iterator.hasNext()) {
                String queueName = (String) iterator.next();

                try {
                    log.debug("Executing subscription removal handler to minimize message losses");
                    //if in clustered mode copy messages addressed to that queue back to global queue
                    if (ClusterResourceHolder.getInstance().getClusterConfiguration().isClusteringEnabled()) {
                        handleMessageRemoval(queueName, AndesUtils.getGlobalQueueNameForDestinationQueue(queueName));

                    }
//                    ClusterResourceHolder.getInstance().getCassandraMessageStore().removeSubscriptionCounterForQueue(queueName, AndesUtils.getMyNodeQueueName());
                } catch (AMQStoreException e) {
                    log.error("Error while removing subscription for queue: " + queueName, e);
                }
                iterator.remove();
            }
        }
    }

    public void clearAllPersistedStatesOfDissapearedNode(int nodeID) {

        log.info("Clearing the Persisted State of Node with ID " + nodeID);

        CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
        String nodeQueueName = AndesUtils.getNodeQueueNameForNodeId(nodeID);
        try {

            //remove node from nodes list
            messageStore.deleteNodeData("" + nodeID);

            //remove this node queue from all destination queues  and update in-memory map
            List<String> destinationQueueList = messageStore.getDestinationQueues();
            for (String destinationQueue : destinationQueueList) {
                //decrement subscription counts
                long subscriptionCountExisting = messageStore.getSubscriptionCountForQueue(destinationQueue,nodeQueueName);
                messageStore.decrementSubscriptionCount(destinationQueue, nodeQueueName, subscriptionCountExisting);
                messageStore.removeNodeQueueFromDestinationQueue(destinationQueue, nodeQueueName);
            }

            //if in stand-alone mode update destination queue-node queue map and reset global queue workers
            if(!ClusterResourceHolder.getInstance().getClusterConfiguration().isClusteringEnabled()) {
                List<String> destinationQueuesHavingFreshSubscriptions = ClusterResourceHolder.getInstance().
                        getSubscriptionManager().updateNodeQueuesForDestinationQueueMap();
                //say global queue workers of this node to read from beginning for above queues
                for(String destinationQueue : destinationQueuesHavingFreshSubscriptions) {
                    String globalQueueName = AndesUtils.getGlobalQueueNameForDestinationQueue(destinationQueue);
                    ClusterResourceHolder.getInstance().getClusterManager().getGlobalQueueManager().resetGlobalQueueWorkerIfRunning(globalQueueName);
                }
            } else {
                //notify the subscription change to the cluster nodes
                ClusterResourceHolder.getInstance().getSubscriptionCoordinationManager().handleSubscriptionChange();
            }

            //remove topic subscriptions of disappeared node and update in-memory map
            messageStore.removeTopicSubscriptionsOfDisappearedNode(AndesUtils.getTopicNodeQueueNameForNodeID(nodeID));
            //notify the change to the cluster
            if(ClusterResourceHolder.getInstance().getClusterConfiguration().isClusteringEnabled()) {
                List<String> topics = messageStore.getTopics();
                for(String topic : topics) {
                    ClusterResourceHolder.getInstance().getTopicSubscriptionCoordinationManager().notifyTopicSubscriptionChange(topic);
                }
            }


        } catch (AMQStoreException e) {
            log.error("Error while clearing state of disappeared node", e);
        } catch (CassandraDataAccessException ex) {
            log.error("Error while reading from Cassandra" , ex);
        } catch (CoordinationException exe) {
            log.error("Error while notifying subscription change to cluster", exe);
        }
    }

    private void handleMessageRemoval(String destinationQueue, String globalQueue) throws AMQStoreException {
        synchronized (QueueMessageRemovalLock.class) {
            try {
                long ignoredFirstMessageId = Long.MAX_VALUE;
                try {
                    Thread.sleep(10*1000);
                } catch (InterruptedException e) {
                    //silently ignore
                }
                //move messages from node queue to global queue
                CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
                String nodeQueueName = AndesUtils.getMyNodeQueueName();
                int numberOfMessagesMoved = 0;
                long lastProcessedMessageID = 0;
                List<CassandraQueueMessage> messages = messageStore.getMessagesFromNodeQueue(nodeQueueName, 40, lastProcessedMessageID);
                while (messages.size() != 0) {
                    for (CassandraQueueMessage msg : messages) {
                        String destinationQueueName = msg.getDestinationQueueName();
                        //move messages addressed to this destination queue only
                        if (destinationQueueName.equals(destinationQueue)
                                && !OnflightMessageTracker.getInstance().testForAlreadyDeliveredAndAckReceivedMessages(msg.getMessageId())
                                ) {
                            if (getNodeQueuesHavingSubscriptionsForQueue(destinationQueue)!= null) {
                                if (getNodeQueuesHavingSubscriptionsForQueue(destinationQueue).size() > 0 && !getNodeQueuesHavingSubscriptionsForQueue(destinationQueue).contains(nodeQueueName)) {
                                    numberOfMessagesMoved++;
                                    messageStore.removeMessageFromNodeQueue(nodeQueueName, msg.getMessageId());
                                    try {
                                        //when adding back to global queue we mark it as an message that was already came in (as un-acked)
                                        //we do not evaluate if message addressed queue is bound to topics as it is not used. Just pass false for that.
                                        //for message properties  just pass default values as they will not be written to Cassandra again.
                                        //we should add it to relevant globalQueue also
                                        //even if messages are addressed to durable subscriptions we need to add (force)

                                        messageStore.addMessageToGlobalQueue(globalQueue, msg.getDestinationQueueName() , msg.getMessageId(), msg.getMessage(), false, 0, false, true);
                                        if (traceLog.isTraceEnabled()) {
                                            traceLog.trace("TRACING>> DCESM- Moving message-"+ AndesUtils.getHID(msg.getAmqMessage()) +
                                                     "- with MessageID-"+msg.getMessageId() + "-from NQ " + nodeQueueName + " to GQ-"+globalQueue);
                                        }
                                    } catch (Exception e) {
                                        log.error(e);
                                    }
                                }else {
                                    if(traceLog.isTraceEnabled()) {
                                        traceLog.trace("TRACING >> DCESM >> Skipped moving message "+ AndesUtils.getHID(msg.getAmqMessage()) + " with message ID"+ msg.getMessageId() + "-from NQ " + nodeQueueName + " to GQ-"+globalQueue + " since there is no other subscriptions");
                                    }
                                }
                            }
                            OnflightMessageTracker.getInstance().deleteFromAlreadyReadFromNodeQueueMessagesInstantly(msg.getMessageId());
                            if(traceLog.isTraceEnabled()) {
                                traceLog.trace("TRACING >> DCESM >> Invoked deleteFromAlreadyReadFromNodeQueueMessagesInstantly for message "+ AndesUtils.getHID(msg.getAmqMessage()) +" with messageID "+ msg.getMessageId());
                            }
                        }else{
                            messageStore.removeMessageFromNodeQueue(nodeQueueName, msg.getMessageId());
                            if(log.isDebugEnabled()) {
                                log.debug("+++ TRACING >> DCESM >> Removing the message from node queue "+ nodeQueueName+" for the second time since it is already delivered and acked messageId "+ msg.getMessageId());
                            }
                            try {
                                //when adding back to global queue we mark it as an message that was already came in (as un-acked)
                                //we do not evaluate if message addressed queue is bound to topics as it is not used. Just pass false for that.
                                //for message properties  just pass default values as they will not be written to Cassandra again.
                                //we should add it to relevant globalQueue also
                                //even if messages are addressed to durable subscriptions we need to add (force)

                                messageStore.addMessageToGlobalQueue(globalQueue, msg.getDestinationQueueName() , msg.getMessageId(), msg.getMessage(), false, 0, false, true);
                                if (traceLog.isTraceEnabled()) {
                                    traceLog.trace("TRACING>> DCESM- Moving message-"+ AndesUtils.getHID(msg.getAmqMessage()) +
                                            "- with MessageID-"+msg.getMessageId() + "-from NQ " + nodeQueueName + " to GQ-"+globalQueue);
                                }
                            } catch (Exception e) {
                                log.error(e);
                            }
                        }
                        lastProcessedMessageID = msg.getMessageId();
                        if (ignoredFirstMessageId > lastProcessedMessageID) {
                            ignoredFirstMessageId = lastProcessedMessageID;
                        }
//                        OnflightMessageTracker.getInstance().scheduleToDeleteMessageFromReadMessageFromNodeQueueMap(lastProcessedMessageID);


                    }
                    messages = messageStore.getMessagesFromNodeQueue(nodeQueueName, 40, lastProcessedMessageID);
                }

               //remove any in-memory messages accumulated for the queue
                ClusterResourceHolder.getInstance().getClusterManager().removeInMemoryMessagesAccumulated(destinationQueue);

                if(log.isDebugEnabled()) {
                    log.debug("Moved " + numberOfMessagesMoved + " Number of Messages Addressed to Queue " +
                            destinationQueue + " from Node Queue " + nodeQueueName + "to Global Queue");
                }
                updateQueueDeliveryInformation(destinationQueue,ignoredFirstMessageId);
            } catch (AMQStoreException e) {
                log.error("Error removing messages addressed to " + destinationQueue + "from relevant node queue");
            }

        }

    }

    private void updateQueueDeliveryInformation(String queueName, long ignoredFirstMessageID){
        String myNodeQueue = AndesUtils.getMyNodeQueueName();
        QueueDeliveryWorker queueDeliveryWorker =  workMap.get(myNodeQueue);
        if(queueDeliveryWorker == null) {
            return;
        }
        QueueDeliveryWorker.QueueDeliveryInfo queueDeliveryInfo = queueDeliveryWorker.getQueueDeliveryInfo(queueName);
        if(queueDeliveryInfo == null) {
            return;
        }
        queueDeliveryInfo. setIgnoredFirstMessageId(ignoredFirstMessageID);
        queueDeliveryInfo.setNeedToReset(true);
        log.debug("TRACING>> DCESM-updateQueueDeliveryInformation >> Updated the QDI Object of queue-"+queueName+"-to ignoredFirstMessageID = " + ignoredFirstMessageID);
    }

    private void handleSubscription(AMQQueue queue) {
        try {
            String globalQueueName = AndesUtils.getGlobalQueueNameForDestinationQueue(queue.getResourceName());
            String nodeQueueName = AndesUtils.getMyNodeQueueName();
            ClusterResourceHolder.getInstance().getCassandraMessageStore().addNodeQueueToGlobalQueue(globalQueueName, nodeQueueName);
            ClusterResourceHolder.getInstance().getCassandraMessageStore().addNodeQueueToDestinationQueue(queue.getResourceName(), nodeQueueName);
            ClusterResourceHolder.getInstance().getCassandraMessageStore().addMessageCounterForQueue(queue.getName());
            if (workMap.get(nodeQueueName) == null) {
                boolean isInMemoryMode = ClusterResourceHolder.getInstance().getClusterConfiguration().isInMemoryMode();
                QueueDeliveryWorker work = new QueueDeliveryWorker(nodeQueueName,queue,
                        subscriptionMap, messageDeliveryExecutor, queueWorkerWaitInterval,isInMemoryMode);
                workMap.put(nodeQueueName,work);
                messageFlusherExecutor.execute(work);
            } else {
                log.debug("TRACING>> There exists a QueueDeliveryWorker for NodeQueue: " + workMap.get(nodeQueueName));
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error while adding subscription to queue :" + queue ,e);
        }

    }

    public List<String> getAllSubscriptionInformation() {
        //  subscriptionInfo =  subscriptionIdentifier |  subscribedQueueOrTopicName | subscriberQueueBoundExchange |
        // subscriberQueueName |  isDurable | isActive | numberOfMessagesRemainingForSubscriber | subscriberNodeAddress

        List<String> allSubscriptions = new ArrayList<String>();

        //durable clusterwide topic subscriptions
        //temp local topic subscriptions
        List<String> topicSubscriptions = ClusterResourceHolder.getInstance().getCassandraMessageStore().getTopicSubscriptions();
        allSubscriptions.addAll(topicSubscriptions);
        //durable clusterwide queue subscriptions
        List<String> getAllQueueSubscriptionInformationInCluster =  getAllQueueSubscriptionInformationInCluster();
        allSubscriptions.addAll(getAllQueueSubscriptionInformationInCluster);
        //temp local queue subscriptions

        return allSubscriptions;

    }

    public List<String> getAllQueueSubscriptionInformationInCluster() {
        //  subscriptionInfo =  subscriptionIdentifier |  subscribedQueueOrTopicName | subscriberQueueBoundExchange |
        // subscriberQueueName |  isDurable | isActive | numberOfMessagesRemainingForSubscriber | subscriberNodeAddress
        List<String> globalQueueSubscriptions = new ArrayList<String>();
        if(globalSubscriptionsMap != null && !globalSubscriptionsMap.isEmpty()) {
            for(String destinationQueue : globalSubscriptionsMap.keySet()) {
                String boundQueue = destinationQueue;
                int count = 1;
                for(String nodeQueue : globalSubscriptionsMap.get(destinationQueue)) {
                    //each entry is a subscription
                    String nodeID = AndesUtils.getNodeIDFromNodeQueueName(nodeQueue);
                    String subscriptionIdentifier = count + "_" + nodeID + "@" + destinationQueue;
                    String subscriberNodeAddress = ClusterResourceHolder.getInstance().getCassandraMessageStore().getNodeData(nodeID);
                    boolean isDurable = true;
                    boolean isActive = true;
                    String  subscriberQueueName = destinationQueue;
                    String boundExchange = "amq.direct";
                    //long  numberOfMessagesRemainingForSubscriber = ClusterResourceHolder.getInstance().getCassandraMessageStore().getMessageCountOfNodeQueueForDestinationQueue(nodeQueue,destinationQueue);
                    long  numberOfMessagesRemainingForSubscriber = ClusterResourceHolder.getInstance().getCassandraMessageStore().getCassandraMessageCountForQueue(destinationQueue);

                    String encodedSubscriptionInfo = subscriptionIdentifier + "|" + subscriberQueueName + "|" + boundExchange + "|" + subscriberQueueName + "|" +
                            isDurable + "|" + isActive + "|" + numberOfMessagesRemainingForSubscriber + "|" + subscriberNodeAddress;
                    globalQueueSubscriptions.add(encodedSubscriptionInfo);
                }
            }
        }

        return globalQueueSubscriptions;

    }
    public QueueDeliveryWorker getQueueDeliveryWorkerOnCurrentNode() {
        return workMap.get(AndesUtils.getMyNodeQueueName());
    }

    public void incrementSubscriptionCount(boolean instantiateColumn, String destinationQueueName) {
        String nodeQueueName = AndesUtils.getMyNodeQueueName();
        if (instantiateColumn) {
//            ClusterResourceHolder.getInstance().getCassandraMessageStore().removeSubscriptionCounterForQueue(destinationQueueName, nodeQueueName);
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                //silently ignore
//            }
            ClusterResourceHolder.getInstance().getCassandraMessageStore().addSubscriptionCounterForQueue(destinationQueueName, nodeQueueName);
        }else{

            ClusterResourceHolder.getInstance().getCassandraMessageStore().incrementSubscriptionCount(destinationQueueName, nodeQueueName, 1);
        }
    }

    public void markSubscriptionForRemovel(String queue) {
        QueueDeliveryWorker work = workMap.get(queue);

        if (work != null) {
            work.stopFlusher();
        }

    }

    public int getNumberOfSubscriptionsForQueue(String queueName) {
        int numberOfSubscriptions = 0;
        Map<String,CassandraSubscription> subs = subscriptionMap.get(queueName);
        if(subs != null){
            numberOfSubscriptions = subs.size();
        }
        return numberOfSubscriptions;
    }

    public void stopAllMessageFlushers() {
        Collection<QueueDeliveryWorker> workers = workMap.values();
        for (QueueDeliveryWorker flusher : workers) {
            flusher.stopFlusher();
        }
    }

    public void startAllMessageFlushers() {
        Collection<QueueDeliveryWorker> workers = workMap.values();
        for (QueueDeliveryWorker flusher : workers) {
            flusher.startFlusher();
        }
    }

    public Map<String, QueueDeliveryWorker> getWorkMap() {
        return workMap;
    }

    public Map<AMQChannel, Map<Long, Semaphore>> getUnAcknowledgedMessageLocks() {
        return unAckedMessagelocks;
    }

    @Override
    public Map<AMQChannel, QueueSubscriptionAcknowledgementHandler> getAcknowledgementHandlerMap() {
        return acknowledgementHandlerMap;
    }

}
