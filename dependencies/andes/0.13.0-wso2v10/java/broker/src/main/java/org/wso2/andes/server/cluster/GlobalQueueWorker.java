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
package org.wso2.andes.server.cluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.cassandra.CassandraQueueMessage;
import org.wso2.andes.server.cassandra.ClusteringEnabledSubscriptionManager;
import org.wso2.andes.server.stats.PerformanceCounter;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.wso2.andes.server.util.AndesConstants;
import org.wso2.andes.server.util.AndesUtils;

import java.util.*;
import java.util.concurrent.*;

/**
 * <code>GlobalQueueWorker</code> is responsible for polling global queues and
 * distribute messages to the subscriber userQueues.
 */
public class GlobalQueueWorker implements Runnable {

    private static Log log = LogFactory.getLog(GlobalQueueWorker.class);
    private static final Log traceLog = LogFactory.getLog(AndesConstants.TRACE_LOGGER);

    private String globalQueueName;
    private boolean running;
    private int messageCountToReadFromCasssandra;
    private long lastProcessedMessageId;
    private CassandraMessageStore cassandraMessageStore;
    private long totMsgMoved = 0;
    private Semaphore semaphore = new Semaphore(0);

    private ConcurrentHashMap<Long,Long> alreadyReadFromGlobalQueueMessages = new ConcurrentHashMap<Long, Long>();
    //time to keep a msg in alreadyReadFromGlobalQueueMessages in-memory map, default is 10 mins
    private long timeOutPerMessage = 600000000000L;
    private static final ScheduledExecutorService alreadyReadMessageIDsRemovingScheduler = Executors.newSingleThreadScheduledExecutor();
    private SortedMap<Long,Long> alreadyReadFromGlobalQueueMessagesRemovalTasks = new ConcurrentSkipListMap<Long, Long>();

    public GlobalQueueWorker(String queueName, CassandraMessageStore cassandraMessageStore,
                             int messageCountToReadFromCasssandra) {
        this.cassandraMessageStore = cassandraMessageStore;
        this.globalQueueName = queueName;
        this.messageCountToReadFromCasssandra = messageCountToReadFromCasssandra;
        this.lastProcessedMessageId = 0;

        startRemovingAlreadyReadGQMessageIDS();
    }

    public void run() {
        int queueWorkerWaitTime = ClusterResourceHolder.getInstance().getClusterConfiguration()
                .getQueueWorkerInterval();

        ClusteringEnabledSubscriptionManager csm = ClusterResourceHolder.getInstance().getSubscriptionManager();
        int repeatedSleepingCounter = 0;
        int loggingCounter = 0;
        while (running) {
            try {
                if (isThisWorkerShouldWork()) {
                    /**
                     * Steps
                     *
                     * 1)Poll Global queue and get chunk of messages 2) Put messages
                     * one by one to node queues and delete them
                     */
                    List<CassandraQueueMessage> cassandraMessages = cassandraMessageStore.getMessagesFromGlobalQueue(
                            globalQueueName, lastProcessedMessageId, messageCountToReadFromCasssandra);
                    int size = cassandraMessages.size();
                    if (size > 0) {
                        if (log.isDebugEnabled()) {
                            log.debug("GQW >> Read " + size + " messages from GQ " + globalQueueName + " with last processed id " + lastProcessedMessageId);
                        }
                        if (traceLog.isTraceEnabled()) {
                            traceLog.trace("GQW >> Read " + size + " messages from GQ " + globalQueueName + " with last processed id " + lastProcessedMessageId);
                        }
                    } else {
                        loggingCounter++;
                        if (loggingCounter % 100 == 0) {
                            if (log.isDebugEnabled()) {
                                log.debug("GQW >> Read " + size + " messages from GQ " + globalQueueName + " with last processed id " + lastProcessedMessageId);
                            }
                            if (traceLog.isTraceEnabled()) {
                                traceLog.trace("GQW >> Read " + size + " messages from GQ " + globalQueueName + " with last processed id " + lastProcessedMessageId);
                            }
                            loggingCounter = 0;
                        }
                    }
                    PerformanceCounter.recordGlobalQueueMsgMove(size);

                    if (csm == null) {
                        csm = ClusterResourceHolder.getInstance().getSubscriptionManager();
                    }
                    //Checking whether we have messages from the Global Queue
                    if (cassandraMessages != null && cassandraMessages.size() > 0) {
                        repeatedSleepingCounter = 0;
                        Iterator<CassandraQueueMessage> messageIterator = cassandraMessages.iterator();
                        while (messageIterator.hasNext()) {
                            CassandraQueueMessage msg = messageIterator.next();
                            String destinationQueue = msg.getDestinationQueueName();
                            long messageId = msg.getMessageId();
                            Random random = new Random();
                            //check if the cluster has some subscriptions for that message and distribute to relevant node queues
                            if (csm.getNodeQueuesHavingSubscriptionsForQueue(destinationQueue) != null &&
                                    csm.getNodeQueuesHavingSubscriptionsForQueue(destinationQueue).size() > 0 && msgOKToSendToNodeQueue(messageId)) {

                                int index = random.nextInt(csm.getNodeQueuesHavingSubscriptionsForQueue(destinationQueue).size());
                                String nodeQueue = csm.getNodeQueuesHavingSubscriptionsForQueue(destinationQueue).get(index);
                                msg.setNodeQueue(nodeQueue);
                                addTaskToRemoveMessageFromAlreadyReadFromGlobalQueueMessages(messageId);
                                if (log.isDebugEnabled()) {
                                    log.debug("TRACING>> GQW " + globalQueueName + ">> copying message-" + AndesUtils.getHID(msg.getAmqMessage()) +
                                            " to " + nodeQueue + " message ID: " + msg.getAmqMessage().getMessageId());
                                }
                                if (traceLog.isTraceEnabled()) {
                                    traceLog.trace("TRACING>> GQW " + globalQueueName + ">> copying message-" + AndesUtils.getHID(msg.getAmqMessage()) +
                                            " to " + nodeQueue + " message ID: " + msg.getAmqMessage().getMessageId());
                                }
                            } else {
                                //if there is no node queue to move message we skip
                                messageIterator.remove();
                                if (log.isDebugEnabled()) {
                                    log.debug("TRACING>> GQW " + globalQueueName + ">> skipping message " + AndesUtils.getHID(msg.getAmqMessage()) +
                                            " message ID: " + msg.getAmqMessage().getMessageId());
                                }
                                if (traceLog.isTraceEnabled()) {
                                    traceLog.trace("TRACING>> GQW " + globalQueueName + ">> skipping message " + AndesUtils.getHID(msg.getAmqMessage()) +
                                            " message ID: " + msg.getAmqMessage().getMessageId());
                                }
                            }

                            lastProcessedMessageId = msg.getMessageId();
                        }

                        cassandraMessageStore.transferMessageBatchFromGlobalQueueToNodeQueue(cassandraMessages, globalQueueName);

                        totMsgMoved = totMsgMoved + cassandraMessages.size();
                        if (log.isDebugEnabled()) {
                            log.debug("[Global, " + globalQueueName + "] moved " + cassandraMessages.size()
                                    + " to node queues, tot = " + totMsgMoved + " ,Last ID:" + lastProcessedMessageId);
                        }
                    } else {
                        try {
                            semaphore.drainPermits();
                            semaphore.tryAcquire(queueWorkerWaitTime, TimeUnit.MILLISECONDS);
                            repeatedSleepingCounter++;
                            resetMessageReading();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    semaphore.drainPermits();
                    semaphore.tryAcquire(queueWorkerWaitTime, TimeUnit.MILLISECONDS);
                }
            } catch (Exception e) {
                log.error("Error in moving messages from global queue to node queue", e);
            }
        }
    }

    private boolean msgOKToSendToNodeQueue(long msgID) {
        if(alreadyReadFromGlobalQueueMessages.get(msgID) != null){
            cassandraMessageStore.removeMessageFromGlobalQueue(this.globalQueueName,msgID);
            if(traceLog.isTraceEnabled()) {
                traceLog.trace("TRACING >> GQW - Removing message id =" + msgID + " as it is already read from " + this.globalQueueName);
            }
            return false;
        } else {
            alreadyReadFromGlobalQueueMessages.put(msgID,msgID);
            if(traceLog.isTraceEnabled()) {
                traceLog.trace("TRACING>> GQW - allowing to send message id - " + msgID  + " from " + this.globalQueueName);
            }
            return true;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void resetMessageReading() {
        this.lastProcessedMessageId = 0;
    }

    public void wakeUpGlobalQueueWorker() {
        semaphore.release();
    }

    private boolean isThisWorkerShouldWork() {
        //check all destination queues in cluster
        //if some destination queue hashes to this global queue, check if there is any subscribers available for that queue in cluster
        //if not do not read. Sleep and continue
        boolean isToWork = false;
        ClusterManager cm = ClusterResourceHolder.getInstance().getClusterManager();
        ClusteringEnabledSubscriptionManager csm = ClusterResourceHolder.getInstance().getSubscriptionManager();
        List<String> destinationQueues = cm.getDestinationQueuesInCluster();
        for (String destinationQueue : destinationQueues) {
            String hashedGlobalQueueName = AndesUtils.getGlobalQueueNameForDestinationQueue(destinationQueue);
            if (this.globalQueueName.equals(hashedGlobalQueueName)) {
                if ((csm.getNodeQueuesHavingSubscriptionsForQueue(destinationQueue) != null) &&
                        csm.getNodeQueuesHavingSubscriptionsForQueue(destinationQueue).size() > 0) {
                    isToWork = true;
                    break;
                }
            }
        }
        return isToWork;
    }

    private void startRemovingAlreadyReadGQMessageIDS() {
        alreadyReadMessageIDsRemovingScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                while (!alreadyReadFromGlobalQueueMessagesRemovalTasks.isEmpty()) {
                    long currentTime = System.nanoTime();
                    SortedMap<Long, Long> timedOutContentList = alreadyReadFromGlobalQueueMessagesRemovalTasks.headMap(currentTime - timeOutPerMessage);
                    for (Long key : timedOutContentList.keySet()) {
                        long msgid = alreadyReadFromGlobalQueueMessagesRemovalTasks.get(key);
                        alreadyReadFromGlobalQueueMessages.remove(msgid);
                        alreadyReadFromGlobalQueueMessagesRemovalTasks.remove(key);
                        if(traceLog.isTraceEnabled()) {
                            traceLog.trace("TRACING>> GQW - removing already read message id from list id=" + msgid);
                        }
                    }
                }
            }
        },  5, 10, TimeUnit.SECONDS);
    }

    // todo : call this method in correct place
    public void addTaskToRemoveMessageFromAlreadyReadFromGlobalQueueMessages(long messageID) {
        alreadyReadFromGlobalQueueMessagesRemovalTasks.put(System.nanoTime(), messageID);
    }

    public void removeMessageIdFromAlreadyReadMessagesMap(long messageID){
        alreadyReadFromGlobalQueueMessages.remove(messageID);
    }
}
