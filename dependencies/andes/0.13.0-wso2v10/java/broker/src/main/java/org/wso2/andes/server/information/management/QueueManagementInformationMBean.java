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
package org.wso2.andes.server.information.management;

import org.wso2.andes.management.common.mbeans.QueueManagementInformation;
import org.wso2.andes.management.common.mbeans.annotations.MBeanOperationParameter;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.cassandra.DefaultClusteringEnabledSubscriptionManager;
import org.wso2.andes.server.cluster.ClusterManager;
import org.wso2.andes.server.cluster.GlobalQueueManager;
import org.wso2.andes.server.management.AMQManagedObject;
import org.wso2.andes.server.queue.DLCQueueUtils;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.wso2.andes.server.util.AndesConstants;
import org.wso2.andes.server.util.AndesUtils;

import javax.management.NotCompliantMBeanException;
import java.util.Iterator;
import java.util.List;

public class QueueManagementInformationMBean extends AMQManagedObject implements QueueManagementInformation {

    GlobalQueueManager globalQueueManager;
    CassandraMessageStore messageStore;
    private final static String QUEUE_ACTION_REMOVE = "remove";
    private final static String QUEUE_ACTION_RESOTRE = "resore";

    public QueueManagementInformationMBean() throws NotCompliantMBeanException {
        super(QueueManagementInformation.class, QueueManagementInformation.TYPE);
        this.messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
        this.globalQueueManager = new GlobalQueueManager(messageStore);
    }

     /*Will remove message from a provided Node*/
    private void processMessageActionOverNodeQueue(long messageID, String queueName, String action, String destination) throws Exception {
          if (messageStore != null && messageID > 0 && queueName != null && action != null) {
             if (action.equals(QUEUE_ACTION_RESOTRE)) {
                 if (destination != null && !isQueueExists(destination)) {
                    throw new Exception("Not a Registered Queue");
                 }
                 else if(destination != null && DLCQueueUtils.isDeadLetterQueue(destination)){
                    throw new Exception("Cannot Restore Back to DLC");
                 }
                 messageStore.removeMessagesFromNodeQueueAndCopyToGlobalQueues(queueName, messageID, destination);
             } else if (action.equals(QUEUE_ACTION_REMOVE)) {
               //Will remove the message from its node queue
               messageStore.removeMessageFromNodeQueue(queueName, messageID);
               //Will remove the message from the global queue
               messageStore.removeMessageFromGlobalQueue(AndesUtils.getGlobalQueueNameForDestinationQueue(queueName), messageID);
               //Will add the content to be removed
               messageStore.addContentDeletionTask(messageID);
             }
             messageStore.decrementQueueCount(queueName, 1L);
           } else {
               throw new Exception("Error While Removing the Message From the " + queueName);
           }
    }
    public String getObjectInstanceName() {
        return QueueManagementInformation.TYPE;
    }

    public synchronized String[] getAllQueueNames() {

        try {
            List<String> queuesList = ClusterResourceHolder.getInstance().getSubscriptionManager().getDestinationQueues();
            Iterator itr = queuesList.iterator();
            //remove topic specific queues
            while (itr.hasNext()) {
                String destinationQueueName = (String) itr.next();
                if(destinationQueueName.startsWith("tmp_") || destinationQueueName.contains(":") || destinationQueueName.startsWith("TempQueue")) {
                    itr.remove();
                }
            }
            String[] queues= new String[queuesList.size()];
            queuesList.toArray(queues);
            return queues;
        } catch (Exception e) {
          throw new RuntimeException("Error in accessing destination queues",e);
        }

    }

    public synchronized String[] getAllSubscriptionInformation() {
        try {
            List<String> subscriptionInformation = ClusterResourceHolder.getInstance().getSubscriptionManager().getAllSubscriptionInformation();
            String[] subscriptions = new String[subscriptionInformation.size()];
            subscriptionInformation.toArray(subscriptions);
            return subscriptions;
        } catch (Exception e) {
            throw  new RuntimeException("Error in accessing subscription information",e);
        }
    }

    public boolean isQueueExists(String queueName) {
        try {
            List<String> queuesList = ClusterResourceHolder.getInstance().getClusterManager().getDestinationQueuesInCluster();
            return queuesList.contains(queueName);
        } catch (Exception e) {
          throw new RuntimeException("Error in accessing destination queues",e);
        }
    }

    @Override
    public void deleteAllMessagesInQueue(@MBeanOperationParameter(name = "queueName",
            description = "Name of the queue to delete messages from") String queueName) {
        try{
            ClusterManager clusterManager = ClusterResourceHolder.getInstance().getClusterManager();
            clusterManager.handleQueueRemoval(queueName);
        } catch (Exception e) {
            throw new RuntimeException("Error while purging messages of queue:" + queueName, e);
        }
    }

    @Override
    public void deleteMessagesFromDeadLetterQueue(@MBeanOperationParameter(name = "messageIDs",
            description = "ID of the Messages to Be Deleted") String[] messageIDs) {
        try {
            for (String messageID : messageIDs) {
                long message_id = AndesUtils.getAndesMessageID(messageID);
                String queueName = AndesUtils.getQueueName(messageID);
                if (message_id != -1) {
                    processMessageActionOverNodeQueue(message_id, DLCQueueUtils.identifyTenantInformationAndGenerateDLCString(queueName, AndesConstants.DEAD_LETTER_CHANNEL_QUEUE), QUEUE_ACTION_REMOVE, null);
                    //Finally will remove the entry from the cache
                    AndesUtils.removeEntryFromBrowserMessageIDCorrelater(messageID);
                    AndesUtils.removeEntryFromQueueNameCorrelater(messageID);
                } else {
                    throw new Exception("The Message ID specified cannot be found");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error While Deleting the Message From Dead Letter Queue :", e);
        }
    }

    @Override
    public void restoreMessagesFromDeadLetterQueue(@MBeanOperationParameter(name = "messageIDs",
        description = "IDs of the Messages to Be Restored") String[] messageIDs) {
        try {
            for (String messageID : messageIDs) {
                long message_id = AndesUtils.getAndesMessageID(messageID);
                String queueName = AndesUtils.getQueueName(messageID);
                if (message_id != -1) {
                    processMessageActionOverNodeQueue(message_id, DLCQueueUtils.identifyTenantInformationAndGenerateDLCString(queueName, AndesConstants.DEAD_LETTER_CHANNEL_QUEUE), QUEUE_ACTION_RESOTRE, null);
                    //Finally will remove the entry from the cache
                    AndesUtils.removeEntryFromBrowserMessageIDCorrelater(messageID);
                    AndesUtils.removeEntryFromQueueNameCorrelater(messageID);
                } else {
                    throw new Exception("The Message ID specified cannot be found");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error While Restoring Message From Queue :", e);
        }
    }

    @Override
    public void restoreMessagesFromDeadLetterQueue(@MBeanOperationParameter(name = "messageIDs",
        description = "IDs of the Messages to Be Restored") String[] messageIDs, @MBeanOperationParameter(name = "destination",
        description = "Destination of the message to be restored") String destination) {

        try {
            for (String messageID : messageIDs) {
                long message_id = AndesUtils.getAndesMessageID(messageID);
                String queueName = AndesUtils.getQueueName(messageID);
                if (message_id != -1) {
                    processMessageActionOverNodeQueue(message_id, DLCQueueUtils.identifyTenantInformationAndGenerateDLCString(queueName, AndesConstants.DEAD_LETTER_CHANNEL_QUEUE), QUEUE_ACTION_RESOTRE, destination);
                    //Finally will remove the entry from the cache
                    AndesUtils.removeEntryFromBrowserMessageIDCorrelater(messageID);
                    AndesUtils.removeEntryFromQueueNameCorrelater(messageID);
                } else {
                    throw new Exception("The Message ID specified cannot be found");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error While Restoring Message From Queue :", e);
        }

    }

    //TODO:when deleting queues from UI this is not get called. Instead we use AMQBrokerManagerMBean. Why are we keeping this?
    public void deleteQueue(@MBeanOperationParameter(name = "queueName",
            description = "Name of the queue to be deleted") String queueName) {
        ClusterManager clusterManager = ClusterResourceHolder.getInstance().getClusterManager();
        CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();
        DefaultClusteringEnabledSubscriptionManager subscriptionManager =
                (DefaultClusteringEnabledSubscriptionManager) ClusterResourceHolder.getInstance().getSubscriptionManager();
        try {
            if(subscriptionManager.getNumberOfSubscriptionsForQueue(queueName)>0) {
                throw new Exception("Queue" + queueName +" Has Active Subscribers. Please Stop Them First.");
            }
            messageStore.removeMessageCounterForQueue(queueName);
            clusterManager.handleQueueRemoval(queueName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * We are returning message count to the UI from this method.
     * When it has received Acks from the clients more than the message actual
     * message in the  queue,( This can happen when a copy of a message get
     * delivered to the consumer while the ACK for the previouse message was
     * on the way back to server), Message count is becoming minus.
     *
     * So from now on , we ll not provide minus values to the front end since
     * it is not acceptable
     *
     * */
    public int getMessageCount(String queueName) {

        int messageCount = (int) messageStore.getCassandraMessageCountForQueue(queueName);
        if (messageCount < 0) {
            messageCount = 0;
        }
        return messageCount;
    }

    public int getSubscriptionCount( String queueName){
        try {
            return globalQueueManager.getSubscriberCount(queueName);
        } catch (Exception e) {
            throw new RuntimeException("Error in getting subscriber count",e);
        }
    }
}
