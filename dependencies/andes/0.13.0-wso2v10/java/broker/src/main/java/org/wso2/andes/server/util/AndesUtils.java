package org.wso2.andes.server.util;

import org.wso2.andes.AMQException;
import org.wso2.andes.framing.AMQShortString;
import org.wso2.andes.framing.ContentHeaderBody;
import org.wso2.andes.framing.EncodingUtils;
import org.wso2.andes.framing.abstraction.MessagePublishInfo;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.cassandra.CassandraQueueMessage;
import org.wso2.andes.server.cassandra.ClusteringEnabledSubscriptionManager;
import org.wso2.andes.server.cluster.ClusterManager;
import org.wso2.andes.server.message.AMQMessage;
import org.wso2.andes.server.message.CustomMessagePublishInfo;
import org.wso2.andes.server.message.MessageMetaData;
import org.wso2.andes.server.queue.QueueEntry;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.wso2.andes.server.store.StorableMessageMetaData;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AndesUtils {

    private static AndesUtils self;
    private int cassandraPort = 9160;

    //This will be used to co-relate between the id used in the browser and the id used internally in MB
    private static ConcurrentHashMap<String, Long> browserMessageIDCorrelater = new ConcurrentHashMap<String, Long>();
    private static ConcurrentHashMap<String, String> browserMessageQueueNameCorrelater = new ConcurrentHashMap<String, String>();

    public static AndesUtils getInstance() {
        if(self == null){
            self = new AndesUtils();
        }
        return self;
    }

    public static String printAMQMessage(QueueEntry message){
        ByteBuffer buf = ByteBuffer.allocate(100); 
        int readCount = message.getMessage().getContent(buf, 0);
        return "("+ message.getMessage().getMessageNumber() + ")" + new String(buf.array(),0, readCount); 
    }
    public static synchronized void registerBrowserMessageID(String browserMessageID, long andesMessageID) {
        browserMessageIDCorrelater.put(browserMessageID, andesMessageID);
    }

    public static synchronized void registerQueueName(String browserMessageID, String queuename) {
        browserMessageQueueNameCorrelater.put(browserMessageID, queuename);
    }

    public static synchronized Long getAndesMessageID(String browserMessageID) {
        if (browserMessageIDCorrelater.containsKey(browserMessageID)) {
            return browserMessageIDCorrelater.get(browserMessageID);
        } else {
            return Long.valueOf(-1);
        }
    }

    public static synchronized String getQueueName(String browserMessageID) {
        if (browserMessageQueueNameCorrelater.containsKey(browserMessageID)) {
            return browserMessageQueueNameCorrelater.get(browserMessageID);
        } else {
            return "";
        }
    }

    public static synchronized void removeEntryFromBrowserMessageIDCorrelater(String browserMessageID) {
        if (browserMessageIDCorrelater.containsKey(browserMessageID)) {
            browserMessageIDCorrelater.remove(browserMessageID);
        }
    }

    public static synchronized void removeEntryFromQueueNameCorrelater(String browserMessageID) {
        if (browserMessageQueueNameCorrelater.containsKey(browserMessageID)) {
            browserMessageQueueNameCorrelater.remove(browserMessageID);
        }
    }


    public static synchronized void flushBrowserMessageIDCorrelater() {
        browserMessageIDCorrelater.clear();
    }

    public static synchronized void flushQueueNameCorelater() {
        browserMessageQueueNameCorrelater.clear();
    }

    /*Wrting the message to the buffer*/
    private static int writeDestinationToBuffer(int offset, ByteBuffer dest, MessagePublishInfo messagePublishInfo, ContentHeaderBody _contentHeaderBody, int storableSize, long arrival_time) {
        byte MANDATORY_FLAG = 1;
        byte IMMEDIATE_FLAG = 2;

        ByteBuffer src = ByteBuffer.allocate((int) storableSize);

        org.apache.mina.common.ByteBuffer minaSrc = org.apache.mina.common.ByteBuffer.wrap(src);
        EncodingUtils.writeInteger(minaSrc, _contentHeaderBody.getSize());
        _contentHeaderBody.writePayload(minaSrc);
        EncodingUtils.writeShortStringBytes(minaSrc, messagePublishInfo.getExchange());
        EncodingUtils.writeShortStringBytes(minaSrc, messagePublishInfo.getRoutingKey());
        byte flags = 0;
        if (messagePublishInfo.isMandatory()) {
            flags |= MANDATORY_FLAG;
        }
        if (messagePublishInfo.isImmediate()) {
            flags |= IMMEDIATE_FLAG;
        }
        EncodingUtils.writeByte(minaSrc, flags);
        EncodingUtils.writeLong(minaSrc, arrival_time);
        src.position(minaSrc.position());
        src.flip();
        src.position(offset);
        src = src.slice();
        if (dest.remaining() < src.limit()) {
            src.limit(dest.remaining());
        }
        dest.put(src);


        return src.limit();
    }

    /*Will change the routing key of a message*/
    //TODO need to dicsuss whether this method should be incorperated in this class
    public static void changeRoutingKeyOfMessage(CassandraQueueMessage sourceMessage, String destination) throws Exception {
        //Will extract the storable message meta data
        AMQMessage sourceAMQPMessage = sourceMessage.getAmqMessage();
        //Will need to swap the message meta information
        //We need the to create a storable message meta data object for swapping of content
        //Need to write the storable meta data
        // Creates s storable meta data objec and will re set the destination queue
        ByteBuffer current_meta_information = ByteBuffer.wrap(sourceMessage.getMessage());
        current_meta_information.position(1);
        StorableMessageMetaData current_storable_meta_data = sourceAMQPMessage.getMessageMetaData().getType().getFactory().createMetaData(current_meta_information);
        //Creates s storable meta data objec and will re set the destination queue
        //Need to create a new publisher info instance since the existing is polymophisic and setter is empty
        MessagePublishInfo messagePublishInfo = new CustomMessagePublishInfo(current_storable_meta_data);
        messagePublishInfo.setRoutingKey(new AMQShortString(destination));
        messagePublishInfo.setRoutingKey(new AMQShortString(destination));
        try {
            MessageMetaData modifiedSourceMetaData = new MessageMetaData(messagePublishInfo, sourceAMQPMessage.getContentHeaderBody(), 0);
            //First will convert to a byte array
            //Will set the phase for the content to be re written
            final int contentSize = 1 + modifiedSourceMetaData.getStorableSize();
            byte[] destinationMetaInformation = new byte[contentSize];
            destinationMetaInformation[0] = (byte) modifiedSourceMetaData.getType().ordinal();
            ByteBuffer new_meta_information = ByteBuffer.wrap(destinationMetaInformation);
            new_meta_information.position(1);
            new_meta_information = new_meta_information.slice();

            //Finally will add the content to the apporopriate buffer
            writeDestinationToBuffer(0, new_meta_information, messagePublishInfo, modifiedSourceMetaData.getContentHeaderBody(), contentSize, modifiedSourceMetaData.getArrivalTime());

            //Will refelct the existing source information with the new information
            sourceMessage.setMessage(destinationMetaInformation);
            sourceMessage.setDestinationQueueName(destination);
        } catch (AMQException ex) {
            //log.error(ex);
            throw new Exception("Error while swapping the destination : " + ex);
        }
    }

    /**
     * Calculate the name of the global queue , with using the queue name of the message
     * passed in to the method.It will get the hash code of the passed queue name
     * and get mod value of it after dividing by the number of available global queue
     * and append that value to the string "GlobalQueue_"
     *
     * Eg: if the mod value is 7, global queue name will be : GlobalQueue_7
     * @param destinationQueueName - Name of the queue that require to calculate the global queue
     * @return globalQueueName - Name of the global queue
     * */
    public  static String getGlobalQueueNameForDestinationQueue(String destinationQueueName) {
        int globalQueueCount = ClusterResourceHolder.getInstance().getClusterConfiguration().getGlobalQueueCount();
        int globalQueueId = Math.abs(destinationQueueName.hashCode()) % globalQueueCount;
        return AndesConstants.GLOBAL_QUEUE_NAME_PREFIX + globalQueueId;
    }

    /**
     * Gets all the names of the global queue according the user configured global queue count
     * @return list of global queue names
     * */
    public static ArrayList<String> getAllGlobalQueueNames(){
        ArrayList<String> globalQueueNames = new ArrayList<String>();
        int globalQueueCount = ClusterResourceHolder.getInstance().getClusterConfiguration().getGlobalQueueCount();
        for(int i=0; i < globalQueueCount; i ++ ){
            globalQueueNames.add(AndesConstants.GLOBAL_QUEUE_NAME_PREFIX + i);
        }
        return globalQueueNames;
    }

/*    *//**
     * get global queue name for given destination queue name
     * @param destinationQueueName  AmQqueue name (routing key)
     * @return node Queue Name
     *//*
    public static String getNodeQueueNameForDestinationQueue(String destinationQueueName) {
        //node queue name is always coupled with global queue name. Thus we find the global queue name for the given
        //destination queue and get the associated node queue name
        String globalQueueName = getGlobalQueueNameForDestinationQueue(destinationQueueName);
        String nodeQueueName = getNodeQueueNameForGlobalQueue(globalQueueName);
        return nodeQueueName;
    }*/

/*    *//**
     * get node queue name for a given global queue name
     * @param globalQueueName
     * @return node queue name
     *//*
    public static String getNodeQueueNameForGlobalQueue(String globalQueueName) {
        ClusterManager clusterManager = ClusterResourceHolder.getInstance().getClusterManager();
        List<Integer> zkNodeIds =  clusterManager.getZkNodes();
        int nodeCount = zkNodeIds.size();
        int assignedNodeId = Math.abs(globalQueueName.hashCode()) % nodeCount;
        String nodeQueueName = "NodeQueue_" +zkNodeIds.get(assignedNodeId) ;
        return nodeQueueName;
    }*/

    public static String getMyNodeQueueName(){
        ClusterManager clusterManager = ClusterResourceHolder.getInstance().getClusterManager();
        String nodeQueueName = AndesConstants.NODE_QUEUE_NAME_PREFIX + clusterManager.getMyNodeID() ;
        return nodeQueueName;
    }

    public static String getTopicNodeQueueName(){
        int nodeId = ClusterResourceHolder.getInstance().getClusterManager().getNodeId();
        String topicNodeQueueName = AndesConstants.TOPIC_NODE_QUEUE_NAME_PREFIX + nodeId;
        return topicNodeQueueName;
    }

    public static String getTopicNodeQueueNameForNodeID(int nodeId){;
        String topicNodeQueueName = AndesConstants.TOPIC_NODE_QUEUE_NAME_PREFIX + nodeId;
        return topicNodeQueueName;
    }

    public static String getNodeQueueNameForNodeId(int nodeId) {
        String nodeQueueName = AndesConstants.NODE_QUEUE_NAME_PREFIX + nodeId;
        return nodeQueueName;
    }

    public static String getHID(AMQMessage message) {
        String header = (String)message.getMessageHeader().getHeader(AndesConstants.MESSAGE_IDENTIFIER_HEADER_KEY);
        if(header == null) {
            return "";
        }  else {
            return header;
        }
    }

    /**
     * Check if this topic has any durable subscription
     * @param topicName name of topic
     * @return if topic is having any durable subscriber
     * @throws Exception
     */
    public static boolean checkIfTopicHasDurableSubscriptions(String topicName) throws Exception {

        boolean hasDurableSubscriptions = false;

        CassandraMessageStore cms = ClusterResourceHolder.getInstance().getCassandraMessageStore();
        ClusteringEnabledSubscriptionManager csm = ClusterResourceHolder.getInstance().getSubscriptionManager();

        //check globally destination queues having subscription for this topic
        List<String> destinationQueuesRegistered = cms.getRegisteredSubscriberQueuesForTopic(topicName);
        for(String destinationQueue : destinationQueuesRegistered) {
            List<String> nodeQueues = csm.getNodeQueuesHavingSubscriptionsForQueue(destinationQueue);
            //if node queue is there in below list it means that destination queue is a durable one. We write it directly to global queue
            //thus no need to add it to topics
            if(nodeQueues != null && !nodeQueues.isEmpty()) {
                hasDurableSubscriptions = true;
                break;
            }
        }

        return hasDurableSubscriptions;
    }

    public int getCassandraPort() {
        return cassandraPort;
    }

    public void setCassandraPort(int cassandraPort) {
        this.cassandraPort = cassandraPort;
    }

    public static String getNodeIDFromNodeQueueName(String nodeQueue) {
        String[] parts = nodeQueue.split("_");
        return parts[1];
    }
}
