package org.wso2.andes.server.cassandra;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.AMQException;
import org.wso2.andes.AMQStoreException;
import org.wso2.andes.server.AMQChannel;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.message.AMQMessage;
import org.wso2.andes.server.queue.DLCQueueUtils;
import org.wso2.andes.server.queue.QueueEntry;
import org.wso2.andes.server.stats.PerformanceCounter;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.wso2.andes.server.util.AndesConstants;
import org.wso2.andes.server.util.AndesUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class OnflightMessageTracker {

    private static Log log = LogFactory.getLog(OnflightMessageTracker.class);
    private static final Log traceLog = LogFactory.getLog(AndesConstants.TRACE_LOGGER);

    private int acktimeout = 10000;
    private int maximumRedeliveryTimes = 1;

    /**
     * In memory map keeping sent messages. If this map does not have an entry for a delivery scheduled
     * message it is a new message. Otherwise it is a redelivery
     */
    private LinkedHashMap<Long,MsgData> msgId2MsgData = new LinkedHashMap<Long,MsgData>();

    private ConcurrentHashMap<String,Long> deliveryTag2MsgID = new ConcurrentHashMap<String,Long>();
    private ConcurrentHashMap<UUID,HashSet<Long>> channelToMsgIDMap = new ConcurrentHashMap<UUID,HashSet<Long>>();
    private ConcurrentHashMap<Long,QueueEntry> messageIdToQueueEntryMap = new ConcurrentHashMap<Long,QueueEntry>();
    private ConcurrentHashMap<String,AtomicInteger> queueNameToAckedCounterMap = new ConcurrentHashMap<String, AtomicInteger>();
    private ConcurrentHashMap<String,Long> queueNameToLastAckReplicatedTimeStampMap = new ConcurrentHashMap<String, Long>();

    /**
     * In memory set keeping track of sent messageIds. Used to prevent duplicate message count
     * decrements
     */
    private HashSet<Long> deliveredButNotAckedMessages = new HashSet<Long>();

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledExecutorService addedMessagedDeletionScheduler = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledExecutorService acknowledgedMessageCounterDecrementingScheduler = Executors.newSingleThreadScheduledExecutor();

    private boolean isInMemoryMode = false;


    private AtomicLong sendMessageCount = new AtomicLong();
    private AtomicLong sendButNotAckedMessageCount = new AtomicLong();
    private ConcurrentHashMap<String,ArrayList<QueueEntry>> queueTosentButNotAckedMessageMap = new ConcurrentHashMap<String, ArrayList<QueueEntry>>();


    private long startTime = -1;
    private ConcurrentHashMap<Long,Long> alreadyReadFromNodeQueueMessages = new ConcurrentHashMap<Long,Long>();

    /**
     * Class to keep tracking data of a message
     */
    public class MsgData{

        final long msgID;
        boolean ackreceived = false;
        final String queue;
        final long timestamp;
        final String deliveryID;
        final AMQChannel channel;
        int numOfDeliveries;
        boolean ackWaitTimedOut;
        long ackReceivedTimeStamp = -1;

        public MsgData(long msgID, boolean ackreceived, String queue, long timestamp, String deliveryID, AMQChannel channel, int numOfDeliveries, boolean ackWaitTimedOut) {
            this.msgID = msgID;
            this.ackreceived = ackreceived;
            this.queue = queue;
            this.timestamp = timestamp;
            this.deliveryID = deliveryID;
            this.channel = channel;
            this.numOfDeliveries = numOfDeliveries;
            this.ackWaitTimedOut = ackWaitTimedOut;
        }
    }

    private static OnflightMessageTracker instance = new OnflightMessageTracker();

    public static OnflightMessageTracker getInstance() {
        return instance;
    }


    private OnflightMessageTracker(){

        this.acktimeout = ClusterResourceHolder.getInstance().getClusterConfiguration().getMaxAckWaitTime()*1000;
        this.maximumRedeliveryTimes = ClusterResourceHolder.getInstance().getClusterConfiguration().getNumberOfMaximumDeliveryCount();
        /*
         * for all add and remove, following is executed, and it will remove the oldest entry if needed
         */
        msgId2MsgData = new LinkedHashMap<Long, MsgData>() {
            private static final long serialVersionUID = -8681132571102532817L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, MsgData> eldest) {
                MsgData msgData = eldest.getValue();
                boolean todelete = (System.currentTimeMillis() - msgData.timestamp) > (acktimeout*10) && (msgData.ackreceived || msgData.ackWaitTimedOut ) ;
                if(todelete){
                    if(deliveryTag2MsgID.remove(msgData.deliveryID) == null){
                        log.error("Cannot find delivery tag " + msgData.deliveryID + " and message id "+ msgData.msgID);
                    }
                }
                return todelete;
            }
        };

        /**
         * This thread will removed acked messages or messages that breached max redelivery count from tracking
         * These messages are already scheduled to be removed from message store.
         */
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                //TODO replace this with Gvava Cache if possible
                synchronized (msgId2MsgData) {
                    log.debug("Running the scheduler for cleaning msgId2MsgData...");
                    int count = 0;
                    Iterator<MsgData> iterator = msgId2MsgData.values().iterator();
                    while(iterator.hasNext()){
                        MsgData mdata = iterator.next();
                        if(mdata.ackreceived && (System.currentTimeMillis() - mdata.ackReceivedTimeStamp)> 180*1000 || (mdata.numOfDeliveries) > maximumRedeliveryTimes){
                            iterator.remove();
                            count++;
                            deliveryTag2MsgID.remove(mdata.deliveryID);
                            if((mdata.numOfDeliveries) > maximumRedeliveryTimes){
                                log.warn("Message "+ mdata.msgID + " with "+ mdata.deliveryID.substring(mdata.deliveryID.lastIndexOf("/")+1) + " removed as it has gone though max redeliveries");
                            }
                        }
                    }
                    log.debug("Cleared message data from msgId2MsgData for " + count +" entries..");
                }
            }
        },  5, 10, TimeUnit.SECONDS);

        addedMessagedDeletionScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                //TODO replace this with Gvava Cache if possible
                synchronized (this) {
                    Iterator<Map.Entry<Long,Long>> keys =  alreadyReadFromNodeQueueMessages.entrySet().iterator();
                    while (keys.hasNext()) {
                        Map.Entry<Long,Long> entry = keys.next();
                        long timeStamp = entry.getValue();
                        if( timeStamp > 0 && (System.currentTimeMillis() -timeStamp)> 3600000){
                            keys.remove();
                            if(traceLog.isTraceEnabled()){
                                traceLog.trace("TRACING>> OFMT-Removed Message Id-"+entry.getKey()+"-from alreadyReadFromNodeQueueMessages");
                            }

                        }
                    }
                }
            }
        },  5, 10, TimeUnit.SECONDS);


        acknowledgedMessageCounterDecrementingScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        Enumeration e = queueNameToAckedCounterMap.keys();
                        while (e.hasMoreElements()) {
                            String queueName = (String) e.nextElement();
                            if( queueNameToLastAckReplicatedTimeStampMap.get(queueName) == null
                                    ||(System.currentTimeMillis() - queueNameToLastAckReplicatedTimeStampMap.get(queueName)) > 5000 ){
                                int decrementingCount = queueNameToAckedCounterMap.get(queueName).get();
                                ClusterResourceHolder.getInstance().getCassandraMessageStore().decrementQueueCount(queueName,decrementingCount);
                                queueNameToAckedCounterMap.get(queueName).set(queueNameToAckedCounterMap.get(queueName).get()-decrementingCount);
                                queueNameToLastAckReplicatedTimeStampMap.put(queueName, System.currentTimeMillis());
                            }
                        }

                    } catch (Exception e1) {
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        }, 5, 10, TimeUnit.SECONDS);

        isInMemoryMode = ClusterResourceHolder.getInstance().getClusterConfiguration().isInMemoryMode();
    }

    public void stampMessageAsAckTimedOut(long deliveryTag, UUID channelId) {
        long newTimeStamp = System.currentTimeMillis();
        String deliveryID = new StringBuffer(channelId.toString()).append("/").append(deliveryTag).toString();
        Long messageId= deliveryTag2MsgID.get(deliveryID);
        if(messageId != null) {
            if(log.isDebugEnabled()) {
                log.debug("============Message with deliveryID = " + deliveryID + " and channel="+ channelId +" rejected from client side ");
            }
            if(traceLog.isTraceEnabled()) {
                traceLog.trace("============Message with deliveryID = " + deliveryID + " MessageID "+ messageId+" and channel= "+ channelId+" rejected from client side ");
            }

            synchronized (msgId2MsgData){
                if(msgId2MsgData.containsKey(messageId)){
                    MsgData msgData = msgId2MsgData.get(messageId);
                    msgData.ackWaitTimedOut = true;
                }
            }
            deleteFromAlreadyReadFromNodeQueueMessagesInstantly(messageId);
        }else {
            if(log.isDebugEnabled()) {
                log.debug("Error - Though rejected , Unable to find Message with deliveryID = " + deliveryID + " rejected from client side ");
            }
            if(traceLog.isTraceEnabled()) {
                traceLog.trace("Error - Though rejected , Unable to find Message with deliveryID = " + deliveryID + " rejected from client side ");
            }
        }
    }
    /**
     * Message is allowed to be sent if and only if it is a new message or an already sent message whose ack wait time
     * out has happened
     * @param messageId
     * @return boolean if the message should be sent
     */
    public  boolean testMessage(long messageId){
        synchronized (msgId2MsgData) {
            long currentTime = System.currentTimeMillis();
            MsgData mdata = msgId2MsgData.get(messageId);
            //we do not redeliver the message until ack-timeout is breached
            if (mdata == null || (!mdata.ackreceived && mdata.ackWaitTimedOut)) {
                if(mdata != null){
                    mdata.channel.decrementNonAckedMessageCount();
                }
                return true;
            }else{
                return false;
            }
        }
    }

    public  boolean testForAlreadyDeliveredMessage(long messageId){
        synchronized (msgId2MsgData) {
            MsgData msgData = msgId2MsgData.get(messageId);
            if(msgData == null||(!msgData.ackreceived && msgData.ackWaitTimedOut)){
                return false;
            }else{
                return true;
            }
        }
    }

    public  boolean testForAlreadyDeliveredAndAckReceivedMessages(long messageId) {
        synchronized (msgId2MsgData) {
            MsgData msgData = msgId2MsgData.get(messageId);
            if (msgData != null && msgData.ackreceived) {
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean  checkAlreadyReadFromNodeQueue (long messageID){
        synchronized (this) {
            if(alreadyReadFromNodeQueueMessages.get(messageID) == null){
                if(traceLog.isTraceEnabled()) {
                    traceLog.trace("TRACING>> OFMT - checkAlreadyReadFromNodeQueue - There is no item with messageID -"+messageID);
                }
                return false;
            }else {
                if(traceLog.isTraceEnabled()) {
                    traceLog.trace("TRACING>> OFMT - checkAlreadyReadFromNodeQueue - There exists an item with messageID -"+messageID);
                }
                return true;
            }
        }
    }

    public void addReadMessageFromNodeQueueToSet(long messageID){
        synchronized (this) {
            alreadyReadFromNodeQueueMessages.put(messageID, 0L);
        }
    }
    public void scheduleToDeleteMessageFromReadMessageFromNodeQueueMap(long messageID){
        synchronized (this) {
            alreadyReadFromNodeQueueMessages.put(messageID,System.currentTimeMillis());
        }
    }

    public void deleteFromAlreadyReadFromNodeQueueMessagesInstantly(long messageId) {
        alreadyReadFromNodeQueueMessages.remove(messageId);
    }

    /**
     * This cleanup the current message ID form tracking. Useful for undo changes in case of a failure
     * @param deliveryTag
     * @param messageId
     * @param channel
     */
    public void removeMessage(AMQChannel channel, long deliveryTag, long messageId){
        synchronized (msgId2MsgData) {
            String deliveryID = new StringBuffer(channel.getId().toString()).append("/").append(deliveryTag).toString();
            Long messageIDStored = deliveryTag2MsgID.remove(deliveryID);

            if(messageIDStored != null && messageIDStored.longValue() != messageId){
                throw new RuntimeException("Delivery Tag "+deliveryID+ " reused for " +messageId + " and " + messageIDStored +" , this should not happen");
            }
            msgId2MsgData.remove(messageId);

            log.info("OFMT-Unexpected remove for messageID- "+ messageId);
        }
    }

    public  boolean testAndAddMessage(QueueEntry queueEntry, long deliveryTag, AMQChannel channel) throws AMQException {

        long messageId = queueEntry.getMessage().getMessageNumber();

        String queue = ((AMQMessage)queueEntry.getMessage()).getMessageMetaData().getMessagePublishInfo()
                .getRoutingKey().toString();

        String nodeSpecificQueueName = queue + "_" + ClusterResourceHolder.getInstance().getClusterManager().getNodeId();

        String deliveryID = new StringBuffer(channel.getId().toString()).append("/").append(deliveryTag).toString();

        int numOfDeliveriesOfCurrentMsg;
        synchronized (msgId2MsgData) {
            long currentTime = System.currentTimeMillis();
            MsgData mdata = msgId2MsgData.get(messageId);
            numOfDeliveriesOfCurrentMsg = 0;

            if (deliveryTag2MsgID.containsKey(deliveryID)) {
                throw new RuntimeException("Delivery Tag "+deliveryID+" reused, this should not happen");
            }
            if(mdata == null) {
                //this is a new message
                deliveredButNotAckedMessages.add(messageId);
                if(traceLog.isTraceEnabled()) {
                    traceLog.trace("TRACING>> OFMT-testAndAdd-scheduling new message to deliver with MessageID-"+messageId);
                }
            }
            //this is an already sent but ack wait time expired message
            else {
                numOfDeliveriesOfCurrentMsg = mdata.numOfDeliveries;
                // entry should have "ReDelivery" header
                queueEntry.setRedelivered();
                // message has sent once, we will clean lists and consider it a new message, but with delivery times tracked
                deliveryTag2MsgID.remove(mdata.deliveryID);
                msgId2MsgData.remove(messageId);
                if(traceLog.isTraceEnabled()) {
                    traceLog.trace("TRACING>> OFMT- testAndAdd-scheduling ack expired message or rejected " +
                            "message to deliver with MessageID-" + messageId + "number of deliveries: " + numOfDeliveriesOfCurrentMsg);
                }
            }
            numOfDeliveriesOfCurrentMsg++;
            deliveryTag2MsgID.put(deliveryID, messageId);
            msgId2MsgData.put(messageId, new MsgData(messageId, false, nodeSpecificQueueName, currentTime, deliveryID, channel,numOfDeliveriesOfCurrentMsg,false));
        }
        sendButNotAckedMessageCount.incrementAndGet();

        HashSet<Long> messagesDeliveredThroughThisChannel = channelToMsgIDMap.get(channel.getId());
        if(messagesDeliveredThroughThisChannel == null){
            messagesDeliveredThroughThisChannel = new HashSet<Long>();
            messagesDeliveredThroughThisChannel.add(messageId);
            channelToMsgIDMap.put(channel.getId(),messagesDeliveredThroughThisChannel);
        }else {
            messagesDeliveredThroughThisChannel.add(messageId);
        }
        messageIdToQueueEntryMap.put(messageId,queueEntry);
        /**
         * any custom checks or procedures that should be executed before message delivery should happen here. Any message
         * rejected at this stage will be dropped from the node queue permanently.
         */

        //check if number of redelivery tries has breached.
        if(numOfDeliveriesOfCurrentMsg > ClusterResourceHolder.getInstance().getClusterConfiguration().getNumberOfMaximumDeliveryCount()) {
            log.warn("Number of Maximum Redelivery Tries Has Breached. Dropping The Message: "+ messageId + "From Queue " + queue);
            return false;
            //check if queue entry has expired. Any expired message will not be delivered
        }  else if(queueEntry.expired()) {
            log.warn("Message is expired. Dropping The Message: "+messageId);
            return false;
        }
        return true;
    }

    public void ackReceived(AMQChannel channel, long deliveryTag) throws AMQStoreException{
        String deliveryID = new StringBuffer(channel.getId().toString()).append("/").append(deliveryTag).toString();
        Long messageId = deliveryTag2MsgID.get(deliveryID);
        if(messageId != null){
            synchronized (msgId2MsgData) {
                MsgData msgData = msgId2MsgData.get(messageId);
                if(msgData != null){
                    msgData.ackreceived = true;
                    msgData.ackReceivedTimeStamp = System.currentTimeMillis();
                    //TODO we have to revisit the topics case
                    channel.decrementNonAckedMessageCount();
                    handleMessageRemovalWhenAcked(msgData);
                    // then update the tracker
                    if(traceLog.isTraceEnabled()) {
                        traceLog.trace("TRACING>> OFMT-Ack received for MessageID-" + msgData.msgID + "-With Delivery Tag-" + deliveryTag);
                    }
                    PerformanceCounter.recordMessageDelivered(msgData.queue);
                    sendButNotAckedMessageCount.decrementAndGet();
                    channelToMsgIDMap.get(channel.getId()).remove(messageId);
                    messageIdToQueueEntryMap.remove(messageId);
                }else{
                    throw new RuntimeException("No message data found for messageId "+ messageId);
                }
            }
        }else{
            //new Exception("Error -------- messageid = null for delivery tag "+deliveryTag ).printStackTrace();
            //TODO We ignore as this happens only with publish case. May be there is a better way to handle this
            return;
        }
    }

    public void releaseAckTrackingSinceChannelClosed(AMQChannel channel) {
        HashSet<Long> sentButNotAckedMessages = channelToMsgIDMap.get(channel.getId());

        if (sentButNotAckedMessages != null && sentButNotAckedMessages.size() > 0) {
            Iterator iterator = sentButNotAckedMessages.iterator();
            if (iterator != null) {
                while (iterator.hasNext()) {
                    long messageId = (Long) iterator.next();
                    synchronized (msgId2MsgData) {
                        if (msgId2MsgData.get(messageId) != null && !msgId2MsgData.get(messageId).ackreceived) {
                            String nodeIDAppendedQueueName = msgId2MsgData.remove(messageId).queue;
                            String destinationQueueName =  nodeIDAppendedQueueName.substring(0, nodeIDAppendedQueueName.lastIndexOf("_"));
                            sendButNotAckedMessageCount.decrementAndGet();
                            QueueEntry queueEntry = messageIdToQueueEntryMap.remove(messageId);
                            deleteFromAlreadyReadFromNodeQueueMessagesInstantly(messageId);
                            ArrayList<QueueEntry> undeliveredMessages = queueTosentButNotAckedMessageMap.get(destinationQueueName);
                            if (undeliveredMessages == null) {
                                undeliveredMessages = new ArrayList<QueueEntry>();
                                undeliveredMessages.add(queueEntry);
                                queueTosentButNotAckedMessageMap.put(destinationQueueName, undeliveredMessages);
                                if(traceLog.isTraceEnabled()) {
                                    traceLog.trace("TRACING>> OFMT- Added message-"+messageId+"-to delivered but not acked list");
                                }
                            } else {
                                undeliveredMessages.add(queueEntry);
                            }
                        }
                    }
                }
            }
        }
        channelToMsgIDMap.remove(channel.getId());
    }

    /*Ensure that all the messages are being removed befor the graceful shutdown*/
    public void removeAckedMessagesFromMemory(){
        String destinationQueueName = null;

        if(queueNameToAckedCounterMap != null){
            for(Map.Entry<String,AtomicInteger> queueEntry : queueNameToAckedCounterMap.entrySet()){
                destinationQueueName = queueEntry.getKey();
                long decrementBy = queueEntry.getValue().longValue();
                ClusterResourceHolder.getInstance().getCassandraMessageStore().decrementQueueCount(destinationQueueName, decrementBy);
               // queueNameToAckedCounterMap.get(destinationQueueName).set((int) (queueNameToAckedCounterMap.get(destinationQueueName).get()-decrementBy));
               // queueNameToLastAckReplicatedTimeStampMap.put(destinationQueueName, System.currentTimeMillis());
            }
        }
    }
    private void handleMessageRemovalWhenAcked(MsgData msgData) throws AMQStoreException {
        if (deliveredButNotAckedMessages.contains(msgData.msgID)) {
            String destinationQueueName = msgData.queue.substring(0, msgData.queue.lastIndexOf("_"));
            //schedule to remove message from message store
            CassandraMessageStore cassandraMessageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
            if (isInMemoryMode) {
                cassandraMessageStore.removeIncomingQueueMessage(msgData.msgID);
            } else {
                //remove message from node queue instantly (prevent redelivery)
                String nodeQueueName = AndesUtils.getMyNodeQueueName();
                cassandraMessageStore.removeMessageFromNodeQueue(nodeQueueName, msgData.msgID);

                //We need to keep this method call order
                scheduleToDeleteMessageFromReadMessageFromNodeQueueMap(msgData.msgID);
                //schedule message content and properties to be deleted
                cassandraMessageStore.addContentDeletionTask(msgData.msgID);
                //schedule message-queue mapping to be removed
                cassandraMessageStore.addMessageQueueMappingDeletionTask(destinationQueueName, msgData.msgID);
            }
            if(queueNameToAckedCounterMap.get(destinationQueueName) == null){
                queueNameToAckedCounterMap.put(destinationQueueName,new AtomicInteger(0));
            }
            if( queueNameToAckedCounterMap.get(destinationQueueName).incrementAndGet() >= 500){
                //decrement message count from relevant queue at Message Store
                ClusterResourceHolder.getInstance().getCassandraMessageStore().decrementQueueCount(destinationQueueName,500L);
                queueNameToAckedCounterMap.get(destinationQueueName).set(queueNameToAckedCounterMap.get(destinationQueueName).get()-500);
                queueNameToLastAckReplicatedTimeStampMap.put(destinationQueueName, System.currentTimeMillis());
            }

            deliveredButNotAckedMessages.remove(msgData.msgID);
        }

    }

    /*Will add the message contents to the dead letter queue*/
    private void addMessageToDeadLetterQueue(long messageID, CassandraQueueMessage current_meta_info) {
        //Will retrieve the message from the store
        CassandraMessageStore cassandraMessageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
        String destinationQueueName = DLCQueueUtils.identifyTenantInformationAndGenerateDLCString(current_meta_info.getDestinationQueueName(), AndesConstants.DEAD_LETTER_CHANNEL_QUEUE);
        byte[] message = current_meta_info.getMessage();

        try {
            if (cassandraMessageStore != null && message != null) {
                //Will add the message to dead letter queue
                cassandraMessageStore.addMessageToNodeQueue(destinationQueueName, messageID, message);
                //Need to increment the count
                cassandraMessageStore.incrementQueueCount(destinationQueueName, 1L);
                log.info("Message :" + messageID + " Added to the Dead Letter Queue");

            } else {
                log.error("The Message Store is Not Properly Initialised, Error While Adding the Content to Dead Letter Queue");
            }
        } catch (Exception e) {
            log.error("Error While Adding Content to Dead Letter Queue", e);
        }
    }

    /**
     * Delete a given message with all its properties and trackings from Message store
     * @param messageId message ID
     * @param destinationQueueName  destination queue name
     */
    public void removeNodeQueueMessageFromStorePermanentlyAndDecrementMsgCount(long messageId, String destinationQueueName) {

        try {
            CassandraMessageStore cassandraMessageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();

            //we need to remove message from the store. At this moment message is at node queue space, not at global space
            //remove message from node queue instantly (prevent redelivery)
            String nodeQueueName = AndesUtils.getMyNodeQueueName();

            //First Need to Identfy and fetch the contents from the node queue
            //Will first get the existing node queue meta information need to reduce -1 since the query adds 1 to it
            CassandraQueueMessage current_meta_info = cassandraMessageStore.getMessageFromNodeQueue(nodeQueueName, (messageId - 1), 1);
            //we need to remove message from the store. At this moment message is at node queue space, not at global space
            //remove message from node queue instantly (prevent redelivery)
            cassandraMessageStore.removeMessageFromNodeQueue(nodeQueueName, messageId);
            if(log.isDebugEnabled()) {
                log.debug("Removed message " + messageId + "from" + nodeQueueName+ " when removeNodeQueueMessageFromStorePermanentlyAndDecrementMsgCount");
            }

            //schedule message content and properties to be removed
            //Commented the below, The message will be diverted to the DLC
            //cassandraMessageStore.addContentDeletionTask(messageId);
            //schedule message-queue mapping to be removed as well
            cassandraMessageStore.addMessageQueueMappingDeletionTask(destinationQueueName, messageId);
            //decrement number of messages
            cassandraMessageStore.decrementQueueCount(destinationQueueName, 1L);

            //Need to check the validity of the meta information
            if (current_meta_info != null) {
                //Will submit the message to the dead letter queue
                addMessageToDeadLetterQueue(messageId, current_meta_info);
                //Need to mark as already deleted from the list
                deleteFromAlreadyReadFromNodeQueueMessagesInstantly(messageId);
            } else {
                log.warn("Cannot Find Meta Information of The Message, Content Cannot be Added To Dead Letter Queue");
            }

            //if it is an already sent but not acked message we will not decrement message count again
            MsgData messageData = msgId2MsgData.get(messageId);
            if (messageData != null) {
                //we do following to stop trying to delete message again when acked someday
                deliveredButNotAckedMessages.remove(messageId);
            }
        } catch (AMQStoreException e) {
            log.error("Error In Removing Message From Node Queue. ID: " + messageId);
        }
    }

    public long getSentButNotAckedMessageCount(){
        return sendButNotAckedMessageCount.get();
    }

    public ArrayList<QueueEntry> getSentButNotAckedMessagesOfQueue(String queueName){
        return  queueTosentButNotAckedMessageMap.remove(queueName);
    }


    public void checkAndRemoveAlreadySentAndAckedMessagesFromStore(List<Long> messagesDetectedAsAlreadySent){
        CassandraMessageStore cassandraMessageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
        for(long messsageId : messagesDetectedAsAlreadySent){
            MsgData mdata =  msgId2MsgData.get(messsageId);
            if(mdata != null && mdata.ackreceived ){
                String nodeQueueName = AndesUtils.getMyNodeQueueName();
                try {
                    cassandraMessageStore.removeMessageFromNodeQueue(nodeQueueName, messsageId);
                    if(traceLog.isTraceEnabled()) {
                        traceLog.trace("Removed message "+messsageId+" from NodeQueue "+ nodeQueueName + " since it is already acked ");
                    }
                } catch (AMQStoreException e) {
                    log.error("Error in removing already acked message "+messsageId+"  from node queue "+ nodeQueueName,e);
                }

            }
        }
    }
}