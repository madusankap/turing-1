package org.wso2.andes.messageStore;

import static org.wso2.andes.messageStore.CassandraConstants.GLOBAL_QUEUES_COLUMN_FAMILY;
import static org.wso2.andes.messageStore.CassandraConstants.INTEGER_TYPE;
import static org.wso2.andes.messageStore.CassandraConstants.KEYSPACE;
import static org.wso2.andes.messageStore.CassandraConstants.LONG_TYPE;
import static org.wso2.andes.messageStore.CassandraConstants.MESSAGE_CONTENT_COLUMN_FAMILY;
import static org.wso2.andes.messageStore.CassandraConstants.MESSAGE_COUNTERS_COLUMN_FAMILY;
import static org.wso2.andes.messageStore.CassandraConstants.MESSAGE_COUNTERS_RAW_NAME;
import static org.wso2.andes.messageStore.CassandraConstants.NODE_QUEUES_COLUMN_FAMILY;
import static org.wso2.andes.messageStore.CassandraConstants.PUB_SUB_MESSAGE_IDS_COLUMN_FAMILY;
import static org.wso2.andes.messageStore.CassandraConstants.byteBufferSerializer;
import static org.wso2.andes.messageStore.CassandraConstants.bytesArraySerializer;
import static org.wso2.andes.messageStore.CassandraConstants.integerSerializer;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.kernel.AndesAckData;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.kernel.AndesMessageMetadata;
import org.wso2.andes.kernel.AndesMessagePart;
import org.wso2.andes.kernel.AndesRemovableMetadata;
import org.wso2.andes.kernel.DurableStoreConnection;
import org.wso2.andes.kernel.MessagingEngine;
import org.wso2.andes.kernel.QueueAddress;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.cassandra.CQLConnection;
import org.wso2.andes.server.cassandra.OnflightMessageTracker;
import org.wso2.andes.server.cassandra.dao.GenericCQLDAO;
import org.wso2.andes.server.stats.PerformanceCounter;
import org.wso2.andes.server.store.util.CQLDataAccessHelper;
import org.wso2.andes.server.store.util.CassandraDataAccessException;
import org.wso2.andes.server.util.AndesConstants;
import org.wso2.andes.server.util.AndesUtils;
import org.wso2.andes.tools.utils.DisruptorBasedExecutor.PendingJob;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Delete;
import com.datastax.driver.core.querybuilder.Insert;

public class CQLBasedMessageStoreImpl implements org.wso2.andes.kernel.MessageStore {
    private static Log log = LogFactory.getLog(CQLBasedMessageStoreImpl.class);

    private ConcurrentSkipListMap<Long, Long> contentDeletionTasks = new ConcurrentSkipListMap<Long, Long>();
    private MessageContentRemoverTask messageContentRemoverTask;
    private boolean isMessageCoutingAllowed;
    private Cluster cluster;

    public CQLBasedMessageStoreImpl() {
        isMessageCoutingAllowed = ClusterResourceHolder.getInstance().getClusterConfiguration().getViewMessageCounts();
    }

    public void initializeMessageStore(DurableStoreConnection cassandraconnection) throws AndesException {
        initializeCassandraMessageStore(cassandraconnection);
        messageContentRemoverTask = new MessageContentRemoverTask(ClusterResourceHolder.getInstance().getClusterConfiguration().
                getContentRemovalTaskInterval(), contentDeletionTasks, this, cassandraconnection);
        messageContentRemoverTask.start();
    }


    private void initializeCassandraMessageStore(DurableStoreConnection cassandraconnection) throws AndesException {
        try {
            cluster = ((CQLConnection) cassandraconnection).getCluster();
            createColumnFamilies();
        } catch (CassandraDataAccessException e) {
            log.error("Error while initializing cassandra message store", e);
            throw new AndesException(e);
        }

    }

    /**
     * Create a cassandra column families for andes usage
     *
     * @throws CassandraDataAccessException
     */
    private void createColumnFamilies() throws CassandraDataAccessException {
        CQLDataAccessHelper.createColumnFamily(MESSAGE_CONTENT_COLUMN_FAMILY, KEYSPACE, this.cluster, LONG_TYPE, DataType.blob());
        CQLDataAccessHelper.createColumnFamily(NODE_QUEUES_COLUMN_FAMILY, KEYSPACE, this.cluster, LONG_TYPE, DataType.blob());
        CQLDataAccessHelper.createColumnFamily(GLOBAL_QUEUES_COLUMN_FAMILY, KEYSPACE, this.cluster, LONG_TYPE, DataType.blob());
        CQLDataAccessHelper.createColumnFamily(PUB_SUB_MESSAGE_IDS_COLUMN_FAMILY, KEYSPACE, this.cluster, LONG_TYPE, DataType.blob());
        CQLDataAccessHelper.createCounterColumnFamily(MESSAGE_COUNTERS_COLUMN_FAMILY, KEYSPACE, this.cluster);
    }

    public void storeMessagePart(List<AndesMessagePart> partList) throws AndesException {
        try {
            /*Mutator<String> messageMutator = HFactory.createMutator(keyspace, stringSerializer);*/
        	List<Insert> inserts = new ArrayList<Insert>();
            for (AndesMessagePart part : partList) {
                final String rowKey = AndesConstants.MESSAGE_CONTENT_CASSANDRA_ROW_NAME_PREFIX
                        + part.getMessageID();
                Insert insert = CQLDataAccessHelper.addMessageToQueue(KEYSPACE,MESSAGE_CONTENT_COLUMN_FAMILY,
                        rowKey, part.getOffSet(), part.getData(), false);
                inserts.add(insert);
                System.out.println("STORE >> message part id" + part.getMessageID() + " offset " + part.getOffSet());
            }
            /*messageMutator.execute();*/
            GenericCQLDAO.batchExecute(KEYSPACE, inserts.toArray(new Insert[inserts.size()]));
        } catch (CassandraDataAccessException e) {
            //TODO handle Cassandra failures
            //When a error happened, we should remember that and stop accepting messages
            log.error(e);
            throw new AndesException("Error in adding the message part to the store", e);
        }
    }

    /**
     * get andes message meta-data staring from startMsgID + 1
     *
     * @param queueAddress source address to read metadata
     * @param startMsgID   starting message ID
     * @param count        message count to read
     * @return list of andes message meta-data
     * @throws AndesException
     */
    public List<AndesMessageMetadata> getNextNMessageMetadataFromQueue(QueueAddress queueAddress, long startMsgID, int count) throws AndesException {

        try {
        	List<AndesMessageMetadata> metadataList = CQLDataAccessHelper.getMessagesFromQueue(queueAddress.queueName,
                    getColumnFamilyFromQueueAddress(queueAddress), KEYSPACE, startMsgID + 1, Long.MAX_VALUE, count,true,true);
            //combining metadata with message properties create QueueEntries
            /*for (Object column : messagesColumnSlice.getColumns()) {
                if (column instanceof HColumn) {
                    long messageId = ((HColumn<Long, byte[]>) column).getName();
                    byte[] value = ((HColumn<Long, byte[]>) column).getValue();
                    metadataList.add(new AndesMessageMetadata(messageId, value));
                }
            }*/
            return metadataList;
        } catch (CassandraDataAccessException e) {
            throw new AndesException(e);
        }

    }

    public void duplicateMessageContent(long messageId, long messageIdOfClone) throws AndesException {
        String originalRowKey = AndesConstants.MESSAGE_CONTENT_CASSANDRA_ROW_NAME_PREFIX + messageId;
        String cloneMessageKey = AndesConstants.MESSAGE_CONTENT_CASSANDRA_ROW_NAME_PREFIX + messageIdOfClone;
        try {

            long tryCount = 0;
            //read from store
        /*    ColumnQuery columnQuery = HFactory.createColumnQuery(KEYSPACE, stringSerializer,
                    integerSerializer, byteBufferSerializer);
            columnQuery.setColumnFamily(MESSAGE_CONTENT_COLUMN_FAMILY);
            columnQuery.setKey(originalRowKey.trim());

            SliceQuery<String, Integer, ByteBuffer> query = HFactory.createSliceQuery(KEYSPACE, stringSerializer, integerSerializer, byteBufferSerializer);
            query.setColumnFamily(MESSAGE_CONTENT_COLUMN_FAMILY).setKey(originalRowKey).setRange(0, Integer.MAX_VALUE, false, Integer.MAX_VALUE);
            QueryResult<ColumnSlice<Integer, ByteBuffer>> result = query.execute();*/
            
            List<AndesMessageMetadata> messages = CQLDataAccessHelper.getMessagesFromQueue(originalRowKey.trim(), MESSAGE_CONTENT_COLUMN_FAMILY, KEYSPACE, 0,Long.MAX_VALUE, Long.MAX_VALUE,true, false);

            //if there are values duplicate them

            /*if (!result.get().getColumns().isEmpty()) {*/
            if(!messages.isEmpty()){
               /* Mutator<String> mutator = HFactory.createMutator(KEYSPACE, stringSerializer);*/
                /*for (HColumn<Integer, ByteBuffer> column : result.get().getColumns()) {
                    int offset = column.getName();
                    final byte[] chunkData = bytesArraySerializer.fromByteBuffer(column.getValue());
                    CQLDataAccessHelper.addMessageToQueue(MESSAGE_CONTENT_COLUMN_FAMILY, cloneMessageKey,
                            offset, chunkData, mutator, false);
                    System.out.println("DUPLICATE>> new id " + messageIdOfClone + " cloned from id " + messageId + " offset" + offset);

                }*/
                //mutator.execute();
            	for(AndesMessageMetadata msg : messages){
            		long offset =  msg.getMessageID();
                    final byte[] chunkData = msg.getMetadata();
                    CQLDataAccessHelper.addMessageToQueue(KEYSPACE, MESSAGE_CONTENT_COLUMN_FAMILY, cloneMessageKey,
                            offset, chunkData, false);
                    System.out.println("DUPLICATE>> new id " + messageIdOfClone + " cloned from id " + messageId + " offset" + offset);
            	}
            } else {
                tryCount += 1;
                if (tryCount == 3) {
                    throw new AndesException("Original Content is not written. Cannot duplicate content. Tried 3 times");
                }
                try {
                    Thread.sleep(20 * tryCount);
                } catch (InterruptedException e) {
                    //silently ignore
                }

                this.duplicateMessageContent(messageId, messageIdOfClone);
            }

        } catch (CassandraDataAccessException e) {
            throw new AndesException(e);
        }
    }


    public int getContent(String messageId, int offsetValue, ByteBuffer dst) {
        System.out.println("GET CONTENT >> id " + messageId + " offset " + offsetValue);
        int written = 0;
        int chunkSize = 65534;
        byte[] content = null;
        try {
        	log.info(offsetValue);
        	
            String rowKey = "mid" + messageId;
            if (offsetValue == 0) {

                /*ColumnQuery columnQuery = HFactory.createColumnQuery(KEYSPACE, stringSerializer,
                        integerSerializer, byteBufferSerializer);
                columnQuery.setColumnFamily(MESSAGE_CONTENT_COLUMN_FAMILY);
                columnQuery.setKey(rowKey.trim());
                columnQuery.setName(offsetValue);

                QueryResult<HColumn<Integer, ByteBuffer>> result = columnQuery.execute();
                HColumn<Integer, ByteBuffer> column = result.get();*/
            	List<AndesMessageMetadata> messages = CQLDataAccessHelper.getMessagesFromQueue(rowKey.trim(), MESSAGE_CONTENT_COLUMN_FAMILY, KEYSPACE, 0, 0, 10,false,false);
                if (!messages.isEmpty()) {
                	AndesMessageMetadata msg = messages.iterator().next();
                    int offset = (int) msg.getMessageID();//column.getName();
                    content = msg.getMetadata();//bytesArraySerializer.fromByteBuffer(column.getValue());

                    final int size =  content.length;
                    int posInArray = written;
                    int count = size - posInArray;
                    if (count > dst.remaining()) {
                        count = dst.remaining();
                    }
                    dst.put(content, 0, count);
                    written = count;
                } else {
                    throw new RuntimeException("Unexpected Error , content already deleted for message id :" + messageId);
                }
            } else {
                ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
                int k = offsetValue;
               /* SliceQuery query = HFactory.createSliceQuery(KEYSPACE, stringSerializer,
                        integerSerializer, byteBufferSerializer);
                query.setColumnFamily(MESSAGE_CONTENT_COLUMN_FAMILY);
                query.setKey(rowKey.trim());
                query.setRange(k * chunkSize, (k + 1) * chunkSize + 1, false, 10);

                QueryResult<ColumnSlice<Integer, ByteBuffer>> result = query.execute();
                ColumnSlice<Integer, ByteBuffer> columnSlice = result.get();
                for (HColumn<Integer, ByteBuffer> column : columnSlice.getColumns()) {
                    byteOutputStream.write(bytesArraySerializer.fromByteBuffer(column.getValue()));
                }*/
                List<AndesMessageMetadata> messages = CQLDataAccessHelper.getMessagesFromQueue(rowKey.trim(), MESSAGE_CONTENT_COLUMN_FAMILY, KEYSPACE, k , (k+2),10,true, false);
                for (AndesMessageMetadata msg : messages) {
                    byteOutputStream.write(msg.getMetadata());
                }
                
                content = byteOutputStream.toByteArray();
                final int size = (int) content.length;
                int posInArray = offsetValue - k;
                int count = size - posInArray;
                if (count > dst.remaining()) {
                    count = dst.remaining();
                }

                dst.put(content, posInArray, count);

                written += count;
            }

        } catch (Exception e) {
            log.error("Error in reading content", e);
        }
        return written;
    }

    public void deleteMessageParts(long messageID, byte[] data) {
    }

    public void ackReceived(List<AndesAckData> ackList) throws AndesException {
        try {

            List<AndesRemovableMetadata> messagesAddressedToQueues = new ArrayList<AndesRemovableMetadata>();
            List<AndesRemovableMetadata> messagesAddressedToTopics = new ArrayList<AndesRemovableMetadata>();

            for (AndesAckData ackData : ackList) {
                if (ackData.isTopic) {

                    messagesAddressedToTopics.add(ackData.convertToRemovableMetaData());

                    //schedule to remove queue and topic message content
                    long timeGapConfigured = ClusterResourceHolder.getInstance().
                            getClusterConfiguration().getPubSubMessageRemovalTaskInterval() *1000000;
                    addContentDeletionTask(System.nanoTime() + timeGapConfigured,ackData.messageID);

                } else {

                    messagesAddressedToQueues.add(ackData.convertToRemovableMetaData());
                    OnflightMessageTracker onflightMessageTracker = OnflightMessageTracker.getInstance();
                    onflightMessageTracker.updateDeliveredButNotAckedMessages(ackData.messageID);

                    //decrement message count
                    if (isMessageCoutingAllowed) {
                        decrementQueueCount(ackData.qName, 1);
                    }

                    //schedule to remove queue and topic message content
                    addContentDeletionTask(System.nanoTime(),ackData.messageID);
                }

                PerformanceCounter.recordMessageRemovedAfterAck();
            }

            //remove queue message metadata now
            String nodeQueueName = MessagingEngine.getMyNodeQueueName();
            QueueAddress nodeQueueAddress = new QueueAddress
                    (QueueAddress.QueueType.QUEUE_NODE_QUEUE, nodeQueueName);
            deleteMessageMetadataFromQueue(nodeQueueAddress, messagesAddressedToQueues);

            //remove topic message metadata now
            String topicNodeQueueName = AndesUtils.getTopicNodeQueueName();
            QueueAddress topicNodeQueueAddress = new QueueAddress
                    (QueueAddress.QueueType.TOPIC_NODE_QUEUE, topicNodeQueueName);
            deleteMessageMetadataFromQueue(topicNodeQueueAddress, messagesAddressedToTopics);

        } catch (CassandraDataAccessException e) {
            //TODO: hasitha - handle Cassandra failures
            log.error(e);
            throw new AndesException("Error in handling acknowledgments ", e);
        }
    }

    private void addContentDeletionTask(long currentNanoTime, long messageID) {
        contentDeletionTasks.put(currentNanoTime, messageID);
    }

    public void addMessageMetaData(QueueAddress queueAddress, List<AndesMessageMetadata> messageList) throws AndesException {
        try {
/*            Mutator<String> messageMutator = HFactory.createMutator(KEYSPACE, stringSerializer);*/
            HashMap<String, Integer> incomingMessagesMap = new HashMap<String, Integer>();
            List<Insert> inserts = new ArrayList<Insert>();
            for (AndesMessageMetadata md : messageList) {

                //TODO Stop deleting from QMD_ROW_NAME and GLOBAL_QUEUE_LIST_COLUMN_FAMILY

                //TODO this is to avoid having to group messages in AlternatingCassandraWriter
                if (queueAddress == null) {
                    queueAddress = md.queueAddress;
                }
                Insert insert = CQLDataAccessHelper.addMessageToQueue(KEYSPACE, getColumnFamilyFromQueueAddress(queueAddress), queueAddress.queueName,
                        md.getMessageID(), md.getMetadata(), false);
                if (incomingMessagesMap.get(md.getDestination()) == null) {
                    incomingMessagesMap.put(md.getDestination(), 1);
                } else {
                    incomingMessagesMap.put(md.getDestination(), incomingMessagesMap.get(md.getDestination()) + 1);
                }
                inserts.add(insert);
                PerformanceCounter.recordIncomingMessageWrittenToCassandra();
                log.info("Wrote message " + md.getMessageID() + " to Global Queue " + queueAddress.queueName);

            }
            long start = System.currentTimeMillis();
            GenericCQLDAO.batchExecute(KEYSPACE, inserts.toArray(new Insert[inserts.size()]));
            //messageMutator.execute();

            PerformanceCounter.recordIncomingMessageWrittenToCassandraLatency((int) (System.currentTimeMillis() - start));

            if (isMessageCoutingAllowed) {
                for (AndesMessageMetadata md : messageList) {
                    incrementQueueCount(md.getDestination(), 1);
                }
            }

            // Client waits for these message ID to be written, this signal those, if there is a error
            //we will not signal and client who tries to close the connection will timeout.
            //We can do this better, but leaving this as is or now.
            for (AndesMessageMetadata md : messageList) {
                PendingJob jobData = md.getPendingJobsTracker().get(md.getMessageID());
                if (jobData != null) {
                    jobData.semaphore.release();
                }
            }
        } catch (Exception e) {
            //TODO handle Cassandra failures
            //TODO may be we can write those message to a disk, or do something. Currently we will just loose them
            log.error("Error writing incoming messages to Cassandra", e);
            throw new AndesException("Error writing incoming messages to Cassandra", e);
        }
    }

    /**
     * Here if target address is null, we will try to find the address from each AndesMessageMetadata
     */
    public void moveMessageMetaData(QueueAddress sourceAddress, QueueAddress targetAddress, List<AndesMessageMetadata> messageList) throws AndesException {
        /*Mutator<String> messageMutator = HFactory.createMutator(KEYSPACE, stringSerializer);*/
        try {
        	List<Statement> statements = new ArrayList<Statement>();
            for (AndesMessageMetadata messageMetaData : messageList) {
                if (targetAddress == null) {
                    targetAddress = messageMetaData.queueAddress;
                }
                Delete delete = CQLDataAccessHelper.deleteLongColumnFromRaw(KEYSPACE,getColumnFamilyFromQueueAddress(sourceAddress), sourceAddress.queueName, messageMetaData.getMessageID(), false);
                Insert insert = CQLDataAccessHelper.addMessageToQueue(KEYSPACE,getColumnFamilyFromQueueAddress(targetAddress), targetAddress.queueName,
                        messageMetaData.getMessageID(), messageMetaData.getMetadata(), false);
                if (log.isDebugEnabled()) {
                    log.debug("TRACING>> CMS-Removing messageID-" + messageMetaData.getMessageID() + "-from source Queue-" + sourceAddress.queueName
                            + "- to target Queue " + targetAddress.queueName);
                }
                statements.add(insert);
                statements.add(delete);
            }
           // messageMutator.execute();
            GenericCQLDAO.batchExecute(KEYSPACE, statements.toArray(new Statement[statements.size()]));
        } catch (CassandraDataAccessException e) {
            log.error("Error in moving messages ", e);
            throw new AndesException("Error in moving messages from source -" + getColumnFamilyFromQueueAddress(sourceAddress) + " - to target -" + getColumnFamilyFromQueueAddress(targetAddress), e);
        }

    }

    public long moveAllMessageMetaDataOfQueue(QueueAddress sourceAddress, QueueAddress targetAddress, String destinationQueue) throws AndesException {
        //Mutator<String> messageMutator = HFactory.createMutator(KEYSPACE, stringSerializer);
        try {
            long ignoredFirstMessageId = Long.MAX_VALUE;
            int numberOfMessagesMoved = 0;
            long lastProcessedMessageID = 0;
            List<AndesMessageMetadata> messageList = getNextNMessageMetadataFromQueue(sourceAddress, lastProcessedMessageID, 40);
            List<Statement> statements = new ArrayList<Statement>();
            while (messageList.size() != 0) {
                int numberOfMessagesMovedInIteration = 0;
                for (AndesMessageMetadata messageMetaData : messageList) {
                    if (messageMetaData.getDestination().equals(destinationQueue)) {
                        if (targetAddress == null) {
                            targetAddress = messageMetaData.queueAddress;
                        }
                        Delete delete = CQLDataAccessHelper.deleteLongColumnFromRaw(KEYSPACE, getColumnFamilyFromQueueAddress(sourceAddress),
                                sourceAddress.queueName, messageMetaData.getMessageID(), false);

                        Insert insert = CQLDataAccessHelper.addMessageToQueue(KEYSPACE, getColumnFamilyFromQueueAddress(targetAddress), targetAddress.queueName,
                                messageMetaData.getMessageID(), messageMetaData.getMetadata(), false);
                        
                        statements.add(insert);
                        statements.add(delete);
                        numberOfMessagesMovedInIteration++;
                        if (log.isDebugEnabled()) {
                            log.debug("TRACING>> CMS-Removed messageID-" + messageMetaData.getMessageID() + "-from Node Queue-" + sourceAddress.queueName
                                    + "- to GlobalQueue " + targetAddress.queueName);
                        }
                    }
                    lastProcessedMessageID = messageMetaData.getMessageID();
                    if (ignoredFirstMessageId > lastProcessedMessageID) {
                        ignoredFirstMessageId = lastProcessedMessageID;
                    }
                }
                GenericCQLDAO.batchExecute(KEYSPACE, statements.toArray(new Statement[statements.size()]));
                //messageMutator.execute();
                numberOfMessagesMoved = numberOfMessagesMoved + numberOfMessagesMovedInIteration;
                messageList = getNextNMessageMetadataFromQueue(sourceAddress, lastProcessedMessageID, 40);
            }
            log.info("moved " + numberOfMessagesMoved + "number of messages from source -"
                    + getColumnFamilyFromQueueAddress(sourceAddress) + "- to target -" + getColumnFamilyFromQueueAddress(targetAddress) + "-");

            return lastProcessedMessageID;
        } catch (CassandraDataAccessException e) {
            log.error("Error in moving messages ", e);
            throw new AndesException("Error in moving messages from source -"
                    + getColumnFamilyFromQueueAddress(sourceAddress) + " - to target -" + getColumnFamilyFromQueueAddress(targetAddress), e);
        }
    }

    @Override
    //TODO:hasitha - do we want this method?
    public AndesMessageMetadata getMetaData(long messageId) {
        AndesMessageMetadata metadata = null;
        try {

            byte[] value = CQLDataAccessHelper.getMessageMetaDataFromQueue(CassandraConstants.NODE_QUEUES_COLUMN_FAMILY, KEYSPACE, messageId);
            if (value == null) {
                value = CQLDataAccessHelper.getMessageMetaDataFromQueue(CassandraConstants.PUB_SUB_MESSAGE_IDS_COLUMN_FAMILY, KEYSPACE, messageId);
            }
            metadata = new AndesMessageMetadata(messageId, value, true);

        } catch (Exception e) {
            log.error("Error in getting meta data of provided message id", e);
        }
        return metadata;
    }


    @Override
    public void deleteMessageParts(List<Long> messageIdList) throws AndesException {
        try {
            List<String> rows2Remove = new ArrayList<String>();
            for (long messageId : messageIdList) {
                rows2Remove.add(new StringBuffer(
                        AndesConstants.MESSAGE_CONTENT_CASSANDRA_ROW_NAME_PREFIX).append(messageId).toString());
                System.out.println("REMOVE CONTENT>> id " + messageId);
            }
            //remove content
            if (!rows2Remove.isEmpty()) {
                CQLDataAccessHelper.deleteIntegerRowListFromColumnFamily(MESSAGE_CONTENT_COLUMN_FAMILY, rows2Remove, KEYSPACE);
            }
        } catch (CassandraDataAccessException e) {
            throw new AndesException(e);
        }

    }

    @Override
    public void deleteMessageMetadataFromQueue(QueueAddress queueAddress,
                                               List<AndesRemovableMetadata> messagesToRemove) throws AndesException {
        try {
           // Mutator<String> mutator = HFactory.createMutator(keyspace, stringSerializer);
        	List<Statement> statements = new ArrayList<Statement>();
            for (AndesRemovableMetadata message : messagesToRemove) {
                //mutator.addDeletion(queueAddress.queueName, getColumnFamilyFromQueueAddress(queueAddress), message.messageID, longSerializer);
                Delete delete = CQLDataAccessHelper.deleteLongColumnFromRaw(KEYSPACE, getColumnFamilyFromQueueAddress(queueAddress),queueAddress.queueName, message.messageID, false);
                statements.add(delete);
            }
            //mutator.execute();
            GenericCQLDAO.batchExecute(KEYSPACE, statements.toArray(new Statement[statements.size()]));
            if (isMessageCoutingAllowed) {
                for (AndesRemovableMetadata message : messagesToRemove) {
                    decrementQueueCount(message.destination, message.messageID);
                }
            }
        } catch (Exception e) {
            log.error("Error while deleting messages", e);
            throw new AndesException(e);
        }
    }

    public int countMessagesOfQueue(QueueAddress queueAddress, String destinationQueueNameToMatch) throws AndesException {
        long lastProcessedMessageID = 0;
        int messageCount = 0;
        List<AndesMessageMetadata> messageList = getNextNMessageMetadataFromQueue(queueAddress, lastProcessedMessageID, 500);
        while (messageList.size() != 0) {
            Iterator<AndesMessageMetadata> metadataIterator = messageList.iterator();
            while (metadataIterator.hasNext()) {
                AndesMessageMetadata metadata = metadataIterator.next();
                String destinationQueue = metadata.getDestination();
                if (destinationQueueNameToMatch != null) {
                    if (destinationQueue.equals(destinationQueueNameToMatch)) {
                        messageCount++;
                    } else {
                        metadataIterator.remove();
                    }
                } else {
                    messageCount++;
                }

                lastProcessedMessageID = metadata.getMessageID();

            }
            messageList = getNextNMessageMetadataFromQueue(queueAddress, lastProcessedMessageID, 500);
        }
        return messageCount;
    }

    private void incrementQueueCount(String destinationQueueName, long incrementBy) throws CassandraDataAccessException {
        CQLDataAccessHelper.incrementCounter(destinationQueueName, MESSAGE_COUNTERS_COLUMN_FAMILY, MESSAGE_COUNTERS_RAW_NAME, KEYSPACE, incrementBy);
    }

    private void decrementQueueCount(String destinationQueueName, long decrementBy) throws CassandraDataAccessException {

        CQLDataAccessHelper.decrementCounter(destinationQueueName, MESSAGE_COUNTERS_COLUMN_FAMILY, MESSAGE_COUNTERS_RAW_NAME,
                KEYSPACE, decrementBy);
    }

    private String getColumnFamilyFromQueueAddress(QueueAddress address) {
        String columnFamilyName;
        if (address.queueType.equals(QueueAddress.QueueType.QUEUE_NODE_QUEUE)) {
            columnFamilyName = CassandraConstants.NODE_QUEUES_COLUMN_FAMILY;
        } else if (address.queueType.equals(QueueAddress.QueueType.GLOBAL_QUEUE)) {
            columnFamilyName = CassandraConstants.GLOBAL_QUEUES_COLUMN_FAMILY;
        } else if (address.queueType.equals(QueueAddress.QueueType.TOPIC_NODE_QUEUE)) {
            columnFamilyName = CassandraConstants.PUB_SUB_MESSAGE_IDS_COLUMN_FAMILY;
        } else {
            columnFamilyName = null;
        }
        return columnFamilyName;
    }

    public long getMessageCountForQueue(String destinationQueueName) throws AndesException {
        long msgCount = 0;
        try {
            msgCount = CQLDataAccessHelper.getCountValue(KEYSPACE, MESSAGE_COUNTERS_COLUMN_FAMILY, destinationQueueName,
                    MESSAGE_COUNTERS_RAW_NAME);
        } catch (Exception e) {
            log.error("Error while getting message count for queue " + destinationQueueName);
            throw new AndesException(e);
        }
        return msgCount;
    }


    public void close() {
        if (messageContentRemoverTask != null && messageContentRemoverTask.isRunning())
            this.messageContentRemoverTask.setRunning(false);
    }

/*    public int removeMessaesOfQueue(QueueAddress queueAddress, String destinationQueueNameToMatch) throws AndesException {
        long lastProcessedMessageID = 0;
        int messageCount = 0;
        List<AndesMessageMetadata>  messageList = getNextNMessageMetadataFromQueue(queueAddress, lastProcessedMessageID, 500);
        while (messageList.size() != 0) {
            Iterator<AndesMessageMetadata> metadataIterator = messageList.iterator();
            while (metadataIterator.hasNext()) {
                AndesMessageMetadata metadata = metadataIterator.next();
                String destinationQueue = metadata.getDestination();
                if(destinationQueueNameToMatch != null) {
                    if (destinationQueue.equals(destinationQueueNameToMatch)) {
                        messageCount++;
                    } else {
                        metadataIterator.remove();
                    }
                }  else {
                    messageCount++;
                }

                lastProcessedMessageID = metadata.getMessageID();

            }
            messageList = getNextNMessageMetadataFromQueue(queueAddress, lastProcessedMessageID, 500);
        }
        return messageCount;
    } */
}
