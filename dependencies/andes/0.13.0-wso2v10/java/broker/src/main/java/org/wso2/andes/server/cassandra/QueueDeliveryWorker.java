package org.wso2.andes.server.cassandra;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.AMQStoreException;
import org.wso2.andes.server.AMQChannel;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.configuration.ClusterConfiguration;
import org.wso2.andes.server.message.AMQMessage;
import org.wso2.andes.server.protocol.AMQProtocolSession;
import org.wso2.andes.server.queue.AMQQueue;
import org.wso2.andes.server.queue.QueueEntry;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.wso2.andes.server.subscription.Subscription;
import org.wso2.andes.server.subscription.SubscriptionImpl;
import org.wso2.andes.server.util.AndesConstants;
import org.wso2.andes.server.util.AndesUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <code>QueueDeliveryWorker</code> Handles the task of polling the user queues and flushing
 * the messages to subscribers
 * There will be one Flusher per Queue Per Node
 */
public class QueueDeliveryWorker extends Thread{
    private final AMQQueue queue;
    private final String nodeQueue;
    private boolean running = true;
    private static Log log = LogFactory.getLog(QueueDeliveryWorker.class);
    private static final Log traceLog = LogFactory.getLog(AndesConstants.TRACE_LOGGER);

    private int messageCountToRead = 50;
    private int maxMessageCountToRead = 300;
    private int minMessageCountToRead = 20;


    private int maxNumberOfUnAckedMessages = 20000;
    private int maxNumberOfReadButUndeliveredMessages = 1000;

    private long lastProcessedId = 0;

    private int resetCounter;

    private int maxRestCounter = 10;

    private long totMsgSent = 0;
    private long totMsgRead = 0;

    private long lastRestTime = 0;

    private SequentialThreadPoolExecutor executor;

    private final int queueWorkerWaitInterval;

    private int queueMsgDeliveryCurserResetTimeInterval;

    private OnflightMessageTracker onflightMessageTracker;


    private long iterations = 0;
    private int workqueueSize = 0;

    private long failureCount = 0;

    private Map<String,Map<String,CassandraSubscription>> subscriptionMap =
            new ConcurrentHashMap<String,Map<String,CassandraSubscription>>();

    private AtomicInteger totalReadButUndeliveredMessages = new AtomicInteger(0);
    private boolean isInMemoryMode = false;
    ConcurrentLinkedQueue<QueueEntry> laggards =  new ConcurrentLinkedQueue<QueueEntry>();

    public class QueueDeliveryInfo{
        String queueName;
        Iterator<CassandraSubscription> iterator;
        List<QueueEntry> readButUndeliveredMessages = Collections.synchronizedList(new ArrayList<QueueEntry>());
        boolean messageIgnored = false;
        boolean hasQueueFullAndMessagesIgnored = false;
        long ignoredFirstMessageId = -1;
        boolean needToReset = false;

        public void setIgnoredFirstMessageId(long ignoredFirstMessageId) {
            this.ignoredFirstMessageId = ignoredFirstMessageId;
        }

        public void setNeedToReset(boolean needToReset) {
            this.needToReset = needToReset;
        }
    }

    private Map<String, QueueDeliveryInfo> subscriptionCursar4QueueMap = new HashMap<String, QueueDeliveryInfo>();

    /**
     * Get the next subscription for the given queue. If at end of the subscriptions, it circles around to the first one
     * @param queueName
     * @return
     */
    public CassandraSubscription findNextSubscriptionToSent(String queueName){
        Map<String, CassandraSubscription> subscriptions = subscriptionMap.get(queueName);
        if(subscriptions == null || subscriptions.size() == 0){
            subscriptionCursar4QueueMap.remove(queueName);
            return null;
        }

        QueueDeliveryInfo queueDeliveryInfo = getQueueDeliveryInfo(queueName);
        Iterator<CassandraSubscription> it = queueDeliveryInfo.iterator;
        if(it.hasNext()){
            return it.next();
        }else{
            it = subscriptions.values().iterator();
            queueDeliveryInfo.iterator = it;
            if(it.hasNext()){
                return it.next();
            }else{
                return null;
            }
        }
    }



    public QueueDeliveryInfo getQueueDeliveryInfo(String queueName) {
        QueueDeliveryInfo queueDeliveryInfo = subscriptionCursar4QueueMap.get(queueName);
        if(queueDeliveryInfo == null){
            queueDeliveryInfo = new QueueDeliveryInfo();
            queueDeliveryInfo.queueName = queueName;
            Map<String, CassandraSubscription> subscriptions = subscriptionMap.get(queueName);
            if(subscriptions != null){
                queueDeliveryInfo.iterator = subscriptions.values().iterator();
            }else {
                // if subscriptions map is null, we are returning an iterator for an empty list to avoid
                // queueDeliveryInfo.iterator being null
                queueDeliveryInfo.iterator = Collections.<CassandraSubscription>emptyList().iterator();
            }
            subscriptionCursar4QueueMap.put(queueName,queueDeliveryInfo);
        }
        return queueDeliveryInfo;
    }



    public QueueDeliveryWorker(final String nodeQueue,final AMQQueue queue, Map<String, Map<String, CassandraSubscription>> subscriptionMap,
                               SequentialThreadPoolExecutor executorService,final int queueWorkerWaitInterval, boolean isInMemoryMode) {

        //this.cassandraSubscriptions = cassandraSubscriptions;
        this.queue = queue;
        this.nodeQueue = nodeQueue;
        this.executor = executorService;

        ClusterConfiguration clusterConfiguration = ClusterResourceHolder.getInstance().getClusterConfiguration();
        this.messageCountToRead = clusterConfiguration.getMessageBatchSizeForSubscribers();
        this.maxMessageCountToRead = clusterConfiguration.getMaxMessageBatchSizeForSubscribers();
        this.minMessageCountToRead = clusterConfiguration.getMinMessageBatchSizeForSubscribers();
        this.maxNumberOfUnAckedMessages = clusterConfiguration.getMaxNumberOfUnackedMessages();
        this.maxNumberOfReadButUndeliveredMessages = clusterConfiguration.getMaxNumberOfReadButUndeliveredMessages();
        this.queueMsgDeliveryCurserResetTimeInterval = clusterConfiguration.getQueueMsgDeliveryCurserResetTimeInterval();
        this.queueWorkerWaitInterval = queueWorkerWaitInterval;
        this.subscriptionMap =  subscriptionMap;
        onflightMessageTracker = OnflightMessageTracker.getInstance();
        this.isInMemoryMode = isInMemoryMode;

        new Thread() {
            public void run() {
                try {
                    CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
                    long lastReadLaggardMessageID = 0 ;
                    int laggardQueueEntriesListSize = 0;
                    int repeatedSleepingCounter = 0;
                    sleep4waitInterval(60000);
                    while (running) {
                        try {
                            if(laggardQueueEntriesListSize == 0||lastReadLaggardMessageID >= lastProcessedId){
                                while (lastReadLaggardMessageID >= lastProcessedId) {
                                    lastReadLaggardMessageID = 0;
                                    sleep4waitInterval(queueWorkerWaitInterval*5);
                                }
                            }

                            List<QueueEntry> laggardQueueEntriesList = messageStore.getMessagesFromNodeQueue(nodeQueue, queue, messageCountToRead, lastReadLaggardMessageID,lastProcessedId);
                            if(laggardQueueEntriesList.size() == 0){
                                repeatedSleepingCounter++;
                                if (repeatedSleepingCounter > 2) {
                                   lastReadLaggardMessageID = 0;
                                    repeatedSleepingCounter = 0;
                                }
                            }
                            if (log.isDebugEnabled()) {
                                log.debug("QDW >> Read "+laggardQueueEntriesList.size()+ " number of messages " +
                                        "from laggards thread of node queue "+nodeQueue + " with range starting from "+
                                        lastReadLaggardMessageID + " and ending from "+lastProcessedId);
                            }
                            for (QueueEntry entry : laggardQueueEntriesList) {
                                String routingKey = ((AMQMessage) entry.getMessage()).getMessageMetaData().getMessagePublishInfo().getRoutingKey().toString();
                                if (((DefaultClusteringEnabledSubscriptionManager) ClusterResourceHolder.getInstance().getSubscriptionManager()).getNumberOfSubscriptionsForQueue(routingKey) > 0) {
                                    laggards.add(entry);
                                }
                            }
                            laggardQueueEntriesListSize = laggardQueueEntriesList.size();
                            if (laggardQueueEntriesListSize > 0) {
                                lastReadLaggardMessageID =  laggardQueueEntriesList.get(laggardQueueEntriesListSize-1).getMessage().getMessageNumber();
                            }
                            sleep4waitInterval(20000);
                        } catch (AMQStoreException e) {
                            log.warn("Error in laggard message reading thread ",e);
                            sleep4waitInterval(queueWorkerWaitInterval*2);
                        }

                    }
                } catch (Exception e) {
                    log.error("Error in laggard message reader thread, it will break the thread" ,e);
                }

            }
        }.start();

        log.info("Queue worker started for queue: "+ queue.getResourceName() + " with on flight message checks");

    }

    @Override
    public void run() {
        iterations = 0;
        workqueueSize = 0;
        lastRestTime = System.currentTimeMillis();
        failureCount = 0;

        while (running) {
            try {
                /**
                 *    Following check is to avoid the worker queue been full with too many pending tasks.
                 *    those pending tasks are best left in Cassandra until we have some breathing room
                 */
                workqueueSize = executor.getSize();

                if(workqueueSize > 1000){
                    if(workqueueSize > 5000){
                        log.error("Flusher queue is growing, and this should not happen. Please check cassandra Flusher");
                    }
                    if(log.isDebugEnabled()) {
                        log.debug("skipping content cassandra reading thread as flusher queue has "+ workqueueSize + " tasks");
                    }
                    sleep4waitInterval(queueWorkerWaitInterval);
                    continue;
                }

                resetOffsetAtCassadraQueueIfNeeded(false);

                /**
                 * Following reads from cassandara, it reads only if there are not enough messages loaded in memory
                 */
                int msgReadThisTime = 0;
                List<QueueEntry> messagesFromCassansdra = null;
                List<Long> alreadyAddedMessages = new ArrayList<Long>();

                if(totalReadButUndeliveredMessages.get() < 10000){
                    if (isInMemoryMode){
                        CassandraMessageStore messageStore =
                                ClusterResourceHolder.getInstance().getCassandraMessageStore();

                        messagesFromCassansdra = messageStore.
                                getNextIgnoredQueueMessagesToDeliver(queue, messageCountToRead);

                        if(messagesFromCassansdra.size() == 0){
                            messagesFromCassansdra = messageStore.
                                    getNextQueueMessagesToDeliver(queue, messageCountToRead);
                        }
                        for(QueueEntry message: messagesFromCassansdra){
                            String queueName = ((AMQMessage)message.getMessage()).
                                    getMessageMetaData().getMessagePublishInfo().getRoutingKey().toString();
                            QueueDeliveryInfo queueDeliveryInfo = getQueueDeliveryInfo(queueName);
                            // TODO : malinga this is unnecessary looping.
                            if(!queueDeliveryInfo.messageIgnored){
                                if(queueDeliveryInfo.readButUndeliveredMessages.size() < maxNumberOfReadButUndeliveredMessages){
                                    queueDeliveryInfo.readButUndeliveredMessages.add(message);
                                    totalReadButUndeliveredMessages.incrementAndGet();
//                                }
                                } else {
                                    queueDeliveryInfo.hasQueueFullAndMessagesIgnored = true;
                                    queueDeliveryInfo.ignoredFirstMessageId = message.getMessage().getMessageNumber();
                                    OnflightMessageTracker.getInstance().deleteFromAlreadyReadFromNodeQueueMessagesInstantly(message.getMessage().getMessageNumber());
                                }
                            }

                            if(queueDeliveryInfo.messageIgnored){
                                // TODO : There is a better way to do this, instead of polling
                                // pendingMessageIdsQueue messages from here, we can just get the
                                // messages and remove once the ack comes so we can preserve sequence i
                                // n clean manner. This w can think about in next version of MB
                                lastProcessedId = message.getMessage().getMessageNumber();
                                messageStore.setNextIgnoredQueueMessageId(lastProcessedId);
                            }
                        }

                        if(messagesFromCassansdra.size() == 0) {
                            sleep4waitInterval(queueWorkerWaitInterval);
                        }

                        //If we have read all messages we asked for, we increase the reading count. Else we reduce it.
                        //TODO we might want to take into account the size of the message while we change the batch size
                        if(messagesFromCassansdra.size() == messageCountToRead) {
                            messageCountToRead += 100;
                            if(messageCountToRead > maxMessageCountToRead){
                                messageCountToRead = maxMessageCountToRead;
                            }
                        } else {
                            messageCountToRead -= 50;
                            if(messageCountToRead < minMessageCountToRead) {
                                messageCountToRead = minMessageCountToRead;
                            }
                        }
                        totMsgRead = totMsgRead + messagesFromCassansdra.size();
                        msgReadThisTime = messagesFromCassansdra.size();
                    }else {
                        CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
                        messagesFromCassansdra = new ArrayList<QueueEntry>();

                        List<QueueEntry>  intermediateListOfMessagesFromCassansdra = messageStore.getMessagesFromNodeQueue(nodeQueue, queue, messageCountToRead, lastProcessedId,-1);

                        for (QueueEntry message : intermediateListOfMessagesFromCassansdra) {
                            Long messageID = message.getMessage().getMessageNumber();
                            if (!onflightMessageTracker.checkAlreadyReadFromNodeQueue(message.getMessage().getMessageNumber())) {
                                onflightMessageTracker.addReadMessageFromNodeQueueToSet(messageID);
                                if (traceLog.isTraceEnabled()) {
                                    traceLog.trace("TRACING>> QDW - ===Adding " + message.getMessage().getMessageNumber() + " From leading thread to deliver");
                                }
                                messagesFromCassansdra.add(message);
                            }else{
                                alreadyAddedMessages.add(message.getMessage().getMessageNumber());
                            }
                            lastProcessedId = messageID;
                        }

                        Iterator<QueueEntry> laggardsIterator  =  laggards.iterator();

                        while (laggardsIterator.hasNext()){
                            QueueEntry message = laggardsIterator.next();
                            if (!onflightMessageTracker.checkAlreadyReadFromNodeQueue(message.getMessage().getMessageNumber())) {
                                String routingKey = ((AMQMessage) message.getMessage()).getMessageMetaData().getMessagePublishInfo().getRoutingKey().toString();
                                if (((DefaultClusteringEnabledSubscriptionManager) ClusterResourceHolder.getInstance().getSubscriptionManager()).getNumberOfSubscriptionsForQueue(routingKey) > 0) {
                                    messagesFromCassansdra.add(message);
                                    onflightMessageTracker.addReadMessageFromNodeQueueToSet(message.getMessage().getMessageNumber());
                                    if (traceLog.isTraceEnabled()) {
                                        traceLog.trace("TRACING>> QDW - ===Adding " + message.getMessage().getMessageNumber() + " From laggards to deliver");
                                    }
                                }
                            }else {
                                alreadyAddedMessages.add(message.getMessage().getMessageNumber());
                            }
                            laggardsIterator.remove();
                        }

                        // Asking Onflight message tracker to remove the messages which are marked as already sent , if they are already acked
                        if (alreadyAddedMessages.size() > 0 ) {
                            OnflightMessageTracker.getInstance().checkAndRemoveAlreadySentAndAckedMessagesFromStore(alreadyAddedMessages);
                            alreadyAddedMessages.clear();
                        }
                        for(QueueEntry message: messagesFromCassansdra){

                            /**
                             * If this is a message that had sent already, just drop them.
                             */
                            if (!onflightMessageTracker.testMessage(message.getMessage().getMessageNumber())) {
                                continue;
                            }

                            String queueName = ((AMQMessage)message.getMessage()).getMessageMetaData().getMessagePublishInfo().getRoutingKey().toString();
                            QueueDeliveryInfo queueDeliveryInfo = getQueueDeliveryInfo(queueName);

                            if (!queueDeliveryInfo.hasQueueFullAndMessagesIgnored) {
                                if (queueDeliveryInfo.readButUndeliveredMessages.size() < maxNumberOfReadButUndeliveredMessages) {

                                    long currentMessageId = message.getMessage().getMessageNumber();

                                    //We only add messages , if they are not already read and added to the queue
//                                if (!queueDeliveryInfo.alreadyAddedMessages.contains(currentMessageId)) {
//                                    System.out.println("Adding messages to deliver currentMessageId = " + currentMessageId + "from node queue "+ nodeQueue);

                                    queueDeliveryInfo.readButUndeliveredMessages.add(message);
                                    totalReadButUndeliveredMessages.incrementAndGet();
//                                }
                                } else {
                                    queueDeliveryInfo.hasQueueFullAndMessagesIgnored = true;
                                    queueDeliveryInfo.ignoredFirstMessageId = message.getMessage().getMessageNumber();
                                    OnflightMessageTracker.getInstance().deleteFromAlreadyReadFromNodeQueueMessagesInstantly(message.getMessage().getMessageNumber());
                                }
                            } else {


                                if(queueDeliveryInfo.hasQueueFullAndMessagesIgnored && queueDeliveryInfo.ignoredFirstMessageId == -1){
                                    queueDeliveryInfo.ignoredFirstMessageId = message.getMessage().getMessageNumber();
                                }
                                OnflightMessageTracker.getInstance().deleteFromAlreadyReadFromNodeQueueMessagesInstantly(message.getMessage().getMessageNumber());
                                //All subscription in this queue are full and we were forced to ignore messages. We have to rest
                                //Cursor location at the Cassandra Queue before adding new messages to avoid loosing the message order.
                                //When Cursor (Offset) reset happened, this will be set to false
                            }
                        }

                        if(messagesFromCassansdra.size() == 0) {
                            sleep4waitInterval(queueWorkerWaitInterval);
                        }

                        //If we have read all messages we asked for, we increase the reading count. Else we reduce it.
                        //TODO we might want to take into account the size of the message while we change the batch size
                        if(messagesFromCassansdra.size() == messageCountToRead) {
                            messageCountToRead += 100;
                            if(messageCountToRead > maxMessageCountToRead){
                                messageCountToRead = maxMessageCountToRead;
                            }
                        } else {
                            messageCountToRead -= 50;
                            if(messageCountToRead < minMessageCountToRead) {
                                messageCountToRead = minMessageCountToRead;
                            }
                        }
                        totMsgRead = totMsgRead + messagesFromCassansdra.size();
                        msgReadThisTime = messagesFromCassansdra.size();
                    }
                }else {
                    if(log.isDebugEnabled()) {
                        log.debug("QDW >> Total ReadButUndeliveredMessages count " + totalReadButUndeliveredMessages.get() + " is over the accepted limit " );
                    }
                }

                //Then we schedule them to be sent to subcribers
                int sentMessageCount = 0;
                for(QueueDeliveryInfo queueDeliveryInfo:subscriptionCursar4QueueMap.values()) {
                    sentMessageCount = sendMessagesToSubscriptions(queueDeliveryInfo.queueName, queueDeliveryInfo.readButUndeliveredMessages);
                    queueDeliveryInfo.messageIgnored = false;
                }

                if(iterations%20 == 0){
                    /*log.info("[Flusher"+this+"]readNow="+ msgReadThisTime + " totRead="+ totMsgRead+ " totprocessed= "+ totMsgSent + ", totalReadButNotSent="+
                            totalReadButUndeliveredMessages+". workQueue= "+ workqueueSize  + " lastID="+ lastProcessedId);*/
                    if(log.isDebugEnabled()) {
                        log.debug("[Flusher"+this+"]readNow="+ msgReadThisTime + " totRead="+ totMsgRead+ " totprocessed= "+ totMsgSent + ", totalReadButNotSent="+
                                totalReadButUndeliveredMessages+". workQueue= "+ workqueueSize  + " lastID="+ lastProcessedId);
                    }
                }
                iterations++;
                //Message reading work is over in this iteration. If read message count in this iteration is 0 definitely
                // we have to force reset the counter
                if(msgReadThisTime == 0) {
                    boolean f = resetOffsetAtCassadraQueueIfNeeded(false);
                }
                //on every 10th, we sleep a bit to give cassandra a break, we do the same if we have not sent any messages
                if(sentMessageCount == 0 || iterations%10 == 0){
                    sleep4waitInterval(queueWorkerWaitInterval);
                }
                failureCount = 0;
            } catch (Throwable e) {
                /**
                 * When there is a error, we will wait to avoid looping.
                 */
                long waitTime = queueWorkerWaitInterval;
                failureCount++;
                long faultWaitTime = Math.max(waitTime*5, failureCount*waitTime);
                try {
                    Thread.sleep(faultWaitTime);
                } catch (InterruptedException e1) {}
                log.error("Error running Cassandra Message Flusher"+ e.getMessage(), e);
            }
        }
    }



    private void sleep4waitInterval(long sleepInterval) {
        try {
            Thread.sleep(queueWorkerWaitInterval);
        } catch (InterruptedException ignored) {}
    }

    private boolean isThisSubscriptionHasRoom(CassandraSubscription cassandraSubscription){
        AMQChannel channel = null;
        if(cassandraSubscription !=null && cassandraSubscription.getSubscription() instanceof SubscriptionImpl.AckSubscription){
            channel = ((SubscriptionImpl.AckSubscription)cassandraSubscription.getSubscription()).getChannel();
        }
        //is that queue has too many messages pending
        int notAckedMsgCount = channel.getNotAckedMessageCount();

        //Here we ignore messages that has been scheduled but not execuated, so it might send few messages than maxNumberOfUnAckedMessages
        if(notAckedMsgCount < maxNumberOfUnAckedMessages){
            return true;
        }else{

            if(log.isDebugEnabled()){
                log.debug("Not selected, channel=" +queue.getName() + "/"+ channel + " pending count ="  + (notAckedMsgCount + workqueueSize));
            }
            return false;
        }
    }


    public int sendMessagesToSubscriptions(String targetQueue, List<QueueEntry> messages){
        //before doing this we need to check if this queue has any subscription
        if(((DefaultClusteringEnabledSubscriptionManager)ClusterResourceHolder.getInstance().getSubscriptionManager()).
                getNumberOfSubscriptionsForQueue(targetQueue) == 0) {
            if(log.isDebugEnabled()) {
                log.debug("TRACING >> QDW >> returning from sending messages to subscriptions for target queue "+ targetQueue + " since the number of subscripion is 0");
            }
            return 0;
        }
        ArrayList<QueueEntry> previouslyUndeliveredMessages =   getUndeliveredMessagesOfQueue(targetQueue);
        if (previouslyUndeliveredMessages != null && previouslyUndeliveredMessages.size() >0) {
            messages.addAll(previouslyUndeliveredMessages);
            Collections.sort(messages, new Comparator<QueueEntry>(){
                public int compare(QueueEntry m1, QueueEntry m2) {
                    return m1.getMessage().getMessageNumber().compareTo(m2.getMessage().getMessageNumber());
                }
            });
        }
        int sentMessageCount = 0;

        Iterator<QueueEntry> iterator = messages.iterator();
        while (iterator.hasNext()) {
            QueueEntry message = iterator.next();
            Map<String, CassandraSubscription> subscriptions4Queue = subscriptionMap.get(targetQueue);
            if(subscriptions4Queue != null){
                /*
                 * we do this in a for loop to avoid iterating for a subscriptions for ever. We only iterate as
                 * once for each subscription
                 */

             // check whether this queue is for a durable topic or a queue and deliver the message accordingly
             // this check is added since msg selectors needs to be handled different for queues/durable topics
             if(targetQueue.contains("carbon:")){
                for(int j =0;j< subscriptions4Queue.size();j++){
                    CassandraSubscription cassandraSubscription = findNextSubscriptionToSent(targetQueue);

                    if(isThisSubscriptionHasRoom(cassandraSubscription)){

                        if(isThisSubscriptionInterestedInMessage(cassandraSubscription,message)){
                           AMQProtocolSession session = cassandraSubscription.getSession();

                            ((AMQMessage) message.getMessage()).setClientIdentifier(session);

                            //TODO: this is intentional
/*                        if(log.isDebugEnabled()){
                            log.debug("readFromCassandra"+ AndesUtils.printAMQMessage(message));
                        }else {
                        }*/
                            try {
                                Thread.sleep(0,500000);
                            } catch (InterruptedException e) {
                                //ignore
                            }
                            deliverAsynchronously(cassandraSubscription.getSubscription(), message);
                            totMsgSent++;
                            sentMessageCount++;
                            totalReadButUndeliveredMessages.decrementAndGet();

                        }
                            // even if the message is not interested for the subscription we have to remove it from iterator
                            // in order to avoid msg getting looped continuously
                            iterator.remove();
                            break;
                    }
                }

            } else {
                 for(int j =0;j< subscriptions4Queue.size();j++){
                     CassandraSubscription cassandraSubscription = findNextSubscriptionToSent(targetQueue);
                     if(isThisSubscriptionHasRoom(cassandraSubscription) && isThisSubscriptionInterestedInMessage(cassandraSubscription,message)){
                             AMQProtocolSession session = cassandraSubscription.getSession();

                             ((AMQMessage) message.getMessage()).setClientIdentifier(session);

                             try {
                                 Thread.sleep(0,500000);
                             } catch (InterruptedException e) {
                                 //ignore
                             }


                             deliverAsynchronously(cassandraSubscription.getSubscription(), message);
                             totMsgSent++;
                             sentMessageCount++;
                             totalReadButUndeliveredMessages.decrementAndGet();
                             iterator.remove();
                             break;
                     }
                 }
            }
                /*if(!messageSent){
                    log.warn("Message Delivery Warning - Sending out message messageID: "+ message.getMessage().getMessageNumber() +" failed. " +
                            "All subscriptions for queue "+ targetQueue + " have max Unacked messages "+ queue.getName());
                }*/
            }
        }
        return sentMessageCount;
    }

    private boolean isThisSubscriptionInterestedInMessage(CassandraSubscription cassandraSubscription, QueueEntry message) {

        return cassandraSubscription.getSubscription().hasInterest(message);

    }

    public AMQQueue getQueue() {
        return queue;
    }



    private void deliverAsynchronously(final Subscription subscription , final QueueEntry message) {
        if(onflightMessageTracker.testMessage(message.getMessage().getMessageNumber())){
            AMQChannel channel = null;
            if(subscription instanceof SubscriptionImpl.AckSubscription){
                channel = ((SubscriptionImpl.AckSubscription)subscription).getChannel();
            }
            channel.incrementNonAckedMessageCount();
            if(traceLog.isTraceEnabled()){
                traceLog.trace("TRACING>> QDW - sent out message for for delivery channel id=" +
                        channel + " " + queue.getName() + " message id " + message.getMessage().getMessageNumber());
            }else {
                try {
                    Thread.sleep(0,500000);
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (subscription instanceof SubscriptionImpl.AckSubscription) {

                            //this check is needed to detect if subscription has suddenly closed
                            if(subscription.isActive()) {
                                if (!OnflightMessageTracker.getInstance().testForAlreadyDeliveredMessage(message.getMessage().getMessageNumber())) {
                                    subscription.send(message);
      //                              log.info("Message sent from delivery worker: " + message.getMessage().getMessageNumber() + " to consumer: " + subscription.getConsumerTag() + " for queue: " + subscription.getQueue().getName());
                                    if (traceLog.isTraceEnabled()) {
                                        traceLog.trace("TRACING>> QDW- sent messageID-" + message.getMessage().getMessageNumber() + "-to subscription "
                                                + ((SubscriptionImpl.AckSubscription) subscription).getName());
                                    }
                                }else{
                                    if(traceLog.isTraceEnabled()) {
                                        traceLog.trace("TRACING >> QDW - Filtered out from sending out " + message.getMessage().getMessageNumber());
                                    }
                                }
                            }else {
                                storeUndeliveredMessagesDueToInactiveSubscriptions(message);
                                if (log.isDebugEnabled()) {
                                    log.debug("TRACING>> QDW- storing due to subscription vanish - messageID:" + message.getMessage().getMessageNumber()+" for subscription "
                                            + ((SubscriptionImpl.AckSubscription) subscription).getName());
                                }
                            }
                        } else {
                            log.error("Unexpected Subscription Implementation : " +
                                    subscription !=null?subscription.getClass().getName():null);
                        }
                    } catch (Throwable e) {
                        log.error("Error while delivering message " ,e);
                    }
                }
            };
            executor.submit(r, subscription.getSubscriptionID());
        }  else {
            if (traceLog.isTraceEnabled()) {
                traceLog.trace("Rejecting message with messageID " + message.getMessage().getMessageNumber());
            }
        }
    }
    private ConcurrentHashMap<String,ArrayList<QueueEntry>> undeliveredMessagesMap = new ConcurrentHashMap<String, ArrayList<QueueEntry>>();

    private void storeUndeliveredMessagesDueToInactiveSubscriptions(QueueEntry message) {
        String queueName = message.getQueue().getName();
        ArrayList<QueueEntry> undeliveredMessages = undeliveredMessagesMap.get(queueName);
        if (undeliveredMessages == null) {
            undeliveredMessages = new ArrayList<QueueEntry>();
            undeliveredMessages.add(message);
            undeliveredMessagesMap.put(queueName, undeliveredMessages);
        } else {
            undeliveredMessages.add(message);
        }
    }

    private ArrayList<QueueEntry> getUndeliveredMessagesOfQueue(String queueName) {
        ArrayList<QueueEntry> processedButUndeliveredMessages = new ArrayList<QueueEntry>();
        ArrayList<QueueEntry> undeliveredMessagesOfQueue = onflightMessageTracker.getSentButNotAckedMessagesOfQueue(queueName);
        if (undeliveredMessagesOfQueue != null && !undeliveredMessagesOfQueue.isEmpty()) {
            processedButUndeliveredMessages.addAll(undeliveredMessagesOfQueue);
            if(traceLog.isTraceEnabled()) {
                for(QueueEntry undeliveredMsg: undeliveredMessagesOfQueue) {
                    traceLog.trace("TRACING >> QDW - scheduling sent but not acked message kept in memory to deliver messageID: " +
                            undeliveredMsg.getMessage().getMessageNumber());
                }
            }
        }
        ArrayList<QueueEntry> messagesUndeliveredDueToInactiveSubscriptions = undeliveredMessagesMap.remove(queueName);
        if (messagesUndeliveredDueToInactiveSubscriptions != null && !messagesUndeliveredDueToInactiveSubscriptions.isEmpty()) {
            processedButUndeliveredMessages.addAll(messagesUndeliveredDueToInactiveSubscriptions);
            if(traceLog.isTraceEnabled()) {
                for(QueueEntry missedMsg: messagesUndeliveredDueToInactiveSubscriptions) {
                    traceLog.trace("TRACING >> QDW - scheduling delivery missed message due to inactive subscriptions message kept in memory to deliver messageID: " +
                            missedMsg.getMessage().getMessageNumber());
                }
            }
        }

        return processedButUndeliveredMessages;
    }


    public void stopFlusher() {
        running = false;
        log.debug("Shutting down the message flusher for the queue "+ queue.getName());
    }

    public void startFlusher() {
        log.debug("staring flusher for "+ queue.getName());
        running = true;
    }

    private  boolean resetOffsetAtCassadraQueueIfNeeded(boolean force) {
        resetCounter++;
        if (force || (resetCounter > maxRestCounter && (System.currentTimeMillis() - lastRestTime) > queueMsgDeliveryCurserResetTimeInterval)) {
            resetCounter = 0;
            lastRestTime = System.currentTimeMillis();
            lastProcessedId = getStartingIndex();
            if (log.isDebugEnabled()) {
                log.debug("TRACING>> QDW - Reset offset called and Updated lastProcessedId is= " + lastProcessedId);
            }
            return true;
        }
        return false;
    }

    private long getStartingIndex() {
        long startingIndex = lastProcessedId;
        if (subscriptionCursar4QueueMap.values().size() == 0) {
            startingIndex = 0;
        }
        for (QueueDeliveryInfo queueDeliveryInfo : subscriptionCursar4QueueMap.values()) {

            if (queueDeliveryInfo.hasQueueFullAndMessagesIgnored) {
                if (startingIndex > queueDeliveryInfo.ignoredFirstMessageId && queueDeliveryInfo.ignoredFirstMessageId != -1) {
                    startingIndex = queueDeliveryInfo.ignoredFirstMessageId;
                }
                if (queueDeliveryInfo.readButUndeliveredMessages.size() < maxNumberOfReadButUndeliveredMessages / 2) {
                    queueDeliveryInfo.hasQueueFullAndMessagesIgnored = false;
                }
            }
            if(queueDeliveryInfo.needToReset){
                if(startingIndex > queueDeliveryInfo.ignoredFirstMessageId) {
                    startingIndex = queueDeliveryInfo.ignoredFirstMessageId ;
                }
                queueDeliveryInfo.setNeedToReset(false);
            }
        }
        if (startingIndex > 0) {
            startingIndex--;
        }
        return startingIndex;
    }

    public void clearMessagesAccumilatedDueToInactiveSubscriptionsForQueue(String destinationQueueName) {
        undeliveredMessagesMap.remove(destinationQueueName);
        // as readButUndeliveredMessages list is also iterated by sendMessagesToSubscriptions() method we need to lock
        // it before clear
        synchronized (getQueueDeliveryInfo(destinationQueueName).readButUndeliveredMessages){
            getQueueDeliveryInfo(destinationQueueName).readButUndeliveredMessages.clear();
        }
        Iterator<QueueEntry> laggardsIterator = laggards.iterator();
        while (laggardsIterator.hasNext()) {
            QueueEntry message = laggardsIterator.next();
            String routingKey = ((AMQMessage) message.getMessage()).getMessageMetaData().getMessagePublishInfo().getRoutingKey().toString();
            if(routingKey.equals(destinationQueueName)) {
                laggardsIterator.remove();
            }
        }
    }

}
