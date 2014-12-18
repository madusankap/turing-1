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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.AMQStoreException;
import org.wso2.andes.exchange.ExchangeDefaults;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.binding.Binding;
import org.wso2.andes.server.exchange.Exchange;
import org.wso2.andes.server.exchange.ExchangeRegistry;
import org.wso2.andes.server.message.AMQMessage;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.wso2.andes.server.util.AndesConstants;
import org.wso2.andes.server.util.AndesUtils;
import org.wso2.andes.server.virtualhost.VirtualHost;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>TopicDeliveryWorker</code>
 * Handle the task of publishing messages to all the subscribers
 * of a topic
 * */
public class TopicDeliveryWorker extends Thread{
    private long lastDeliveredMessageID = 0;
    private long lastProcessedId = 0;
    private VirtualHost virtualHost;
    private boolean working = false;
    private boolean markedForRemoval;
    private String id;
    private String topicNodeQueueName;
    private CassandraMessageStore messageStore = null;
    private boolean isInMemoryMode = false;
    private SortedMap<Long,Long> alreadyReadFromTopicNodeQueueMessagesRemovalTasks = new ConcurrentSkipListMap<Long, Long>();
    private ConcurrentHashMap<Long,Long>  alreadyReadFromTopicNodeQueueMessages = new ConcurrentHashMap<Long, Long>();
    private boolean killMe = false;

    //keep in-memory list for 15 mins
    private long timeOutPerMessage = 900000000000L;
    private ConcurrentLinkedQueue<AMQMessage> laggards =  new ConcurrentLinkedQueue<AMQMessage>();

    private SequentialThreadPoolExecutor messagePublishingExecutor = null;
    private static final ScheduledExecutorService alreadyDeliveredMessageIDsRemovingScheduler = Executors.newSingleThreadScheduledExecutor();

    private static Log log = LogFactory.getLog(TopicDeliveryWorker.class);
    private static final Log traceLog = LogFactory.getLog(AndesConstants.TRACE_LOGGER);

    public TopicDeliveryWorker(VirtualHost virtualHost,boolean isInMemoryMode){
        this.virtualHost = virtualHost;
        this.topicNodeQueueName = AndesUtils.getTopicNodeQueueName();
        this.id = topicNodeQueueName;
        this.messageStore = ClusterResourceHolder.getInstance().
                getCassandraMessageStore();
        this.isInMemoryMode = isInMemoryMode;

        messagePublishingExecutor = new SequentialThreadPoolExecutor((ClusterResourceHolder.getInstance().getClusterConfiguration().
                getPublisherPoolSize()),"TopicMessagePublishingExecutor");
        this.start();
        this.setWorking();
        startRemovingAlreadyDeliveredTopicMessageIDS();


        new Thread() {
            public void run() {
                try {
                    CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
                    long lastReadLaggardMessageID = 0 ;
                    int laggardQueueEntriesListSize = 0;
                    int repeatedSleepingCounter = 0;
                    int queueWorkerWaitInterval =  ClusterResourceHolder.getInstance().getClusterConfiguration().getQueueWorkerInterval();
                    //start working after 1 min
                    sleep4waitInterval(60000);
                    while (!killMe) {
                        try {
                            if(laggardQueueEntriesListSize == 0 || lastReadLaggardMessageID >= lastProcessedId){
                                while (lastReadLaggardMessageID >= lastProcessedId) {
                                    lastReadLaggardMessageID = 0;
                                    sleep4waitInterval(queueWorkerWaitInterval*5);
                                }
                            }

                            List<AMQMessage> laggardTopicMessagesList = messageStore.getSubscriberMessages(topicNodeQueueName,
                                    lastReadLaggardMessageID,lastProcessedId);

                            if(log.isDebugEnabled()) {
                                log.debug("Read " + laggardTopicMessagesList.size() + " messages from topic laggards thread. " +
                                        "lastReadLaggardMessageID=" + lastReadLaggardMessageID + " lastProcessedID=" + lastProcessedId);
                            }
                            if(laggardTopicMessagesList.size() == 0){
                                repeatedSleepingCounter++;
                                if (repeatedSleepingCounter > 2) {
                                    lastReadLaggardMessageID = 0;
                                    repeatedSleepingCounter = 0;
                                }
                            }
                            if(log.isDebugEnabled()) {
                                log.debug("TDW >> Read "+laggardTopicMessagesList.size()+ " number of messages from laggards thread of topic node queue" +
                                        " "+topicNodeQueueName + " with range starting from "+ lastReadLaggardMessageID + " and ending from "+lastProcessedId);
                            }

                            List<Long> laggardsRejectedMessages = new ArrayList<Long>();

                            for (AMQMessage laggrdsTopicMsg : laggardTopicMessagesList) {
                                boolean isNewMessage = testAndAddMessage(laggrdsTopicMsg.getMessageNumber());
                                if(isNewMessage) {
                                    laggards.add(laggrdsTopicMsg);
                                    if(traceLog.isTraceEnabled()) {
                                        traceLog.trace("TRACING>> TDW - adding to laggards id= " + laggrdsTopicMsg.getMessageNumber());
                                    }
                                } else {
                                    laggardsRejectedMessages.add(laggrdsTopicMsg.getMessageNumber());
                                    if(traceLog.isTraceEnabled()) {
                                        traceLog.trace("TRACING>> TDW - rejecting from laggards id= " + laggrdsTopicMsg.getMessageNumber());
                                    }
                                }
                                addTaskremoveMessageFromAlreadyReadFromTopicNodeQueueMessages(laggrdsTopicMsg.getMessageNumber());
                            }

                            messageStore.removeDeliveredTopicMessageIds(laggardsRejectedMessages, topicNodeQueueName);

                            laggardQueueEntriesListSize = laggardTopicMessagesList.size();
                            if (laggardQueueEntriesListSize > 0) {
                                lastReadLaggardMessageID =  laggardTopicMessagesList.get(laggardQueueEntriesListSize-1).getMessageNumber();
                            }
                            sleep4waitInterval(20000);
                        } catch (Exception e) {
                            log.warn("Error in laggard topic message reading thread ",e);
                            sleep4waitInterval(queueWorkerWaitInterval*2);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error in topic laggard message reader thread, it will break the thread" ,e);
                }
            }
        }.start();
    }

/*    public class MsgData{

        final long msgID;
        boolean ackreceived = false;
        final String routingKey;
        final long timestamp;

        public MsgData(long msgID, boolean ackreceived, String routingKey, long timestamp) {
            this.msgID = msgID;
            this.ackreceived = ackreceived;
            this.routingKey = routingKey;
            this.timestamp = timestamp;
        }
    }*/

    /**
     * 1. Get messages for the queue from last delivered message id
     * 2. Enqueue the retrived message to the queue
     * 3. Remove delivered messaged IDs from the data base
     * */
    @Override
    public void run() {
        while (!killMe) {
            if (working) {
                if (isInMemoryMode) {
                    try {
                        List<AMQMessage> messages = messageStore.getNextTopicMessageToDeliver();
                        if (messages != null) {
                            List<Long> publishedMids = new ArrayList<Long>();
                            try {
                                for (AMQMessage message : messages) {
                                    enqueueMessageToWorkerDestinationQueue(message);
                                    publishedMids.add(message.getMessageNumber());
                                    lastDeliveredMessageID = message.getMessageNumber();
                                    if (traceLog.isTraceEnabled()) {
                                        traceLog.trace("TRACING>> TDW - Sending message  " + lastDeliveredMessageID + "from cassandra topic publisher");
                                    }
                                }

                            } catch (Exception e) {
                                log.error("Error on enqueue messages to relevant queue:" + e.getMessage(), e);
                            }
                            messageStore.removeDeliveredTopicMessageIdsFromIncomingMessagesTable(publishedMids);

                        } else {
                            try {
                                Thread.sleep(ClusterResourceHolder.getInstance().getClusterConfiguration().
                                        getQueueWorkerInterval());
                            } catch (InterruptedException e) {
                                //silently ignore
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error in sending message out in in memory mode ", e);
                    } finally {
                        working = false;
                    }
                } else {
                    try {
                        if(traceLog.isTraceEnabled()) {
                            traceLog.trace("***************************************\r\n\r\n");
                        }
                        lastDeliveredMessageID ++;

                        List<AMQMessage> messages = messageStore.getSubscriberMessages(topicNodeQueueName,
                                lastDeliveredMessageID, Long.MAX_VALUE);

                        if (messages != null && messages.size() > 0) {

                            if(log.isDebugEnabled()) {
                                log.debug("TRACING >> TDW - read " +messages.size()+ " messages from store starting from id " + lastDeliveredMessageID);
                            }

                            Iterator<AMQMessage> msgIterator  =  messages.iterator();
                            while (msgIterator.hasNext()){
                                AMQMessage message = msgIterator.next();
                                if(!testAndAddMessage(message.getMessageNumber())) {
                                    msgIterator.remove();
                                }
                                addTaskremoveMessageFromAlreadyReadFromTopicNodeQueueMessages(message.getMessageNumber());
                            }
                        }

                        Iterator<AMQMessage> laggardsIterator  =  laggards.iterator();

                        while (laggardsIterator.hasNext()){
                            AMQMessage laggardsMessage = laggardsIterator.next();
                            messages.add(laggardsMessage);
                            if(traceLog.isTraceEnabled()) {
                                traceLog.trace("TRACING>> TDW- adding from laggard delivery thread  id=" + laggardsMessage.getMessageNumber());
                            }
                            laggardsIterator.remove();
                        }

                        if (messages != null && messages.size() > 0) {

                            Collections.sort(messages, new Comparator<AMQMessage>() {
                                public int compare(AMQMessage m1, AMQMessage m2) {
                                    return m1.getMessageNumber().compareTo(m2.getMessageNumber());
                                }
                            });

                            List<Long> publishedMids = new ArrayList<Long>();
                            for (AMQMessage message : messages) {
                                try {
                                    enqueueMessage(message);
                                    publishedMids.add(message.getMessageNumber());
                                    lastDeliveredMessageID = message.getMessageNumber();
                                    lastProcessedId = message.getMessageNumber();
                                    if(traceLog.isTraceEnabled()) {
                                        traceLog.trace("TRACING >> TDW - Sending message  " + lastDeliveredMessageID + " from cassandra topic publisher msgID: " + AndesUtils.getHID(message));
                                    }
                                } catch (Exception e) {
                                    log.error("Error on enqueue messages to relevant queue:" + e.getMessage(), e);
                                }
                            }
                            //we do not consider acknowledgements. We think qpid simpleAMQQueue model will handle re-delivery, expiration etc
                            //we remove stored messages here.
                            messageStore.removeDeliveredTopicMessageIds(publishedMids, topicNodeQueueName);
                            if(traceLog.isTraceEnabled()) {
                                traceLog.trace("***************************************\r\n\r\n");
                            }
                        } else {
                            try {
                                Thread.sleep(ClusterResourceHolder.getInstance().getClusterConfiguration().
                                        getQueueWorkerInterval());
                            } catch (InterruptedException e) {
                                //silently ignore
                            }
                        }
                        Thread.sleep(100);
                    } catch (AMQStoreException e) {
                        log.error("Error removing delivered Message Ids from Message store ", e);
                    } catch (InterruptedException e) {
                        //silently ignore
                    }
                }
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //silently ignore
                }
            }
        }
    }

    private void startRemovingAlreadyDeliveredTopicMessageIDS() {
        alreadyDeliveredMessageIDsRemovingScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                while (!alreadyReadFromTopicNodeQueueMessagesRemovalTasks.isEmpty()) {
                    long currentTime = System.nanoTime();
                    SortedMap<Long, Long> timedOutContentList = alreadyReadFromTopicNodeQueueMessagesRemovalTasks.headMap(currentTime - timeOutPerMessage);
                    for (Long key : timedOutContentList.keySet()) {
                        long msgid = alreadyReadFromTopicNodeQueueMessagesRemovalTasks.get(key);
                        alreadyReadFromTopicNodeQueueMessages.remove(msgid);
                        alreadyReadFromTopicNodeQueueMessagesRemovalTasks.remove(key);
                        messageStore.removeAlreadyMetaDataConsumedMessageIdFromList(key);
                        if(traceLog.isTraceEnabled()) {
                            traceLog.trace("TRACING>> TDW - removing already delivered message id from list id=" + msgid);
                        }
                    }
                }
            }
        },  5, 10, TimeUnit.SECONDS);
    }

    private void sleep4waitInterval(long sleepInterval) {
        try {
            Thread.sleep(sleepInterval);
        } catch (InterruptedException ignored) {}
    }

    public void setLastdeliveredMessageID(long lastDeliveredMessageID) {
        this.lastDeliveredMessageID = lastDeliveredMessageID;
    }

    private  boolean testAndAddMessage(long messageID) {
        if(traceLog.isTraceEnabled()) {
            traceLog.trace("TRACING>> TDW - Checking message " + messageID);
        }
        if(alreadyReadFromTopicNodeQueueMessages.get(messageID) != null) {
            if(traceLog.isTraceEnabled()) {
                traceLog.trace("TRACING>> TDW - testAndAddMessage - rejecting - " + messageID);
            }
            try {
                messageStore.removeDeliveredTopicMessageId(messageID, topicNodeQueueName);
            } catch (AMQStoreException e) {
                log.error("Error removing already delivered Message Id "+messageID+" from Message store ", e);
            }
            return false;
        } else {
            alreadyReadFromTopicNodeQueueMessages.put(messageID,messageID);
            messageStore.addAlreadyMetaDataConsumerMessageIdToList(messageID);
            if(traceLog.isTraceEnabled()) {
                traceLog.trace("TRACING>> TDW - testAndAddMessage - allowing to send - " + messageID);
            }
            return true;
        }
    }

    public void addTaskremoveMessageFromAlreadyReadFromTopicNodeQueueMessages(long messageID) {
          alreadyReadFromTopicNodeQueueMessagesRemovalTasks.put(System.nanoTime(), messageID);
    }

    /**
     * Enqueue a given message to all subscriber queues bound to TOPIC_EXCHANGE matching with routing key
     * @param message AMQ message
     */
    private void enqueueMessage(AMQMessage message) {
        Exchange exchange;
        ExchangeRegistry exchangeRegistry = virtualHost.getExchangeRegistry();
        exchange = exchangeRegistry.getExchange(ExchangeDefaults.TOPIC_EXCHANGE_NAME);
        if (exchange != null) {
            /**
             * There can be more than one binding to the same topic
             * We need to publish the message to the exact matching queues
             * */
            String queueName = message.getMessageMetaData().getMessagePublishInfo().getRoutingKey().toString();
            //TODO Srinath, it might be better to publish messages directly to the client like we do with queues rather than going through the enqueue path

            if (traceLog.isTraceEnabled()) {
                traceLog.trace("TRACING>> TDW- enqueue message   with messageID-" +
                        message.getMessageNumber() + " binding size  "+ exchange.getBindings().size());
            }
            for(Binding binding: exchange.getBindings()){
                  if(isMatching(binding.getBindingKey(),queueName)){
                    message.setTopicMessage(true);
                    deliverAsynchronously(binding,message);
                }else {
                      if (traceLog.isTraceEnabled()) {
                          traceLog.trace("TRACING>> TDW- enqueue message   with messageID-" +
                                  message.getMessageNumber() + " not matching  "+ binding.getBindingKey() + " to the queue name "+ queueName);
                      }
                  }
            }
        }else {
            if (traceLog.isTraceEnabled()) {
                traceLog.trace("TRACING>> TDW- enqueue message   with messageID-" +
                        message.getMessageNumber() + "exchange = null ");
            }
        }
    }

    /**
     * Enqueue a given message to the subscriber queue assigned with this worker thread
     * @param message AMQ message
     */
    private void enqueueMessageToWorkerDestinationQueue(AMQMessage message) {
        Exchange exchange;
        ExchangeRegistry exchangeRegistry = virtualHost.getExchangeRegistry();
        exchange = exchangeRegistry.getExchange(ExchangeDefaults.TOPIC_EXCHANGE_NAME);

        if (exchange != null) {
            /**
             * There can be more bindings. But only one queue will be there of the name
             * assigned to this worker thread
             * */
            String topicName = message.getMessageMetaData().getMessagePublishInfo().getRoutingKey().toString();
             for(Binding binding: exchange.getBindings()){
                if(isMatching(binding.getBindingKey(), topicName)){
                    message.setTopicMessage(true);
                    deliverAsynchronously(binding,message);
                }
            }
        }

    }

    public boolean isMatching(String binding, String topic) {
        boolean isMatching = false;
        if (binding.equals(topic)) {
            isMatching = true;
        } else if (binding.indexOf(".#") > 1) {
            String p = binding.substring(0, binding.indexOf(".#"));
            Pattern pattern = Pattern.compile(p + ".*");
            Matcher matcher = pattern.matcher(topic);
            isMatching = matcher.matches();
        } else if (binding.indexOf(".*") > 1) {
            String p = binding.substring(0, binding.indexOf(".*"));
            Pattern pattern = Pattern.compile("^" + p + "[.][^.]+$");
            Matcher matcher = pattern.matcher(topic);
            isMatching = matcher.matches();
        }
        return isMatching;
    }


    /**
     * get if topic delivery task active
     * @return
     */
    public boolean isWorking() {
        return working;
    }

    /**
     *set topic delivery task active
     */
    public void setWorking() {
        working = true;
    }

    public void stopWorking() {
        working = false;
    }

    public void setKillMe(boolean killMe) {
        this.killMe = killMe;
    }

    /**
     * Check if this delivery thread is marked to be inactive
     * @return state
     */
    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }

    /**
     * Mark or un-mark this delivery thread to be removed
     * @param markedForRemoval if to mark for removal
     */
    public void setMarkedForRemoval(boolean markedForRemoval) {
        this.markedForRemoval = markedForRemoval;
    }

    public String getQueueId() {
        return id;
    }

    private void deliverAsynchronously(final Binding binding , final AMQMessage message) {

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        if(binding.getQueue().isDurable()) {
                            //do nothing. These will be handled via queue path
                        }
                        else {
                            binding.getQueue().enqueue(message);
                            if (traceLog.isTraceEnabled()) {
                                traceLog.trace("TRACING>> TDW- Sent message " + AndesUtils.getHID(message)+ " with messageID-" +
                                        message.getMessageNumber() + "-to subscription-" + binding.getQueue().getName());
                            }
                        }
                    } catch (Throwable e) {
                         log.error("Error while delivering message " ,e);
                    }
                }
            };
            long subscriptionId = Math.abs(binding.getId().hashCode());
            messagePublishingExecutor.submit(r, subscriptionId);
    }

}
