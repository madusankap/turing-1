package org.wso2.andes.server.queue;


import org.apache.log4j.Logger;
import org.wso2.andes.AMQException;
import org.wso2.andes.framing.AMQShortString;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.util.AndesConstants;
import org.wso2.andes.server.virtualhost.VirtualHost;

public class DLCQueueUtils {

    private static final Logger _logger = Logger.getLogger(DLCQueueUtils.class);

    public static String identifyTenantInformationAndGenerateDLCString(String owner, String dlc_string) {
        String destination_string = null;
        if (owner.contains("!")) {
            //The Queue is in the tenant realm
            destination_string = owner.substring(owner.lastIndexOf("!")+1)+"/"+dlc_string;
        } else {
            destination_string = dlc_string;
        }
        return destination_string;
    }

    public static boolean isDeadLetterQueue(String queue_name) {
        if (queue_name.contains("/")) {
            if (queue_name.split("/")[1].contains(AndesConstants.DEAD_LETTER_CHANNEL_QUEUE)) {
                return true;
            } else {
                return false;
            }
        } else {
            if (queue_name.equals(AndesConstants.DEAD_LETTER_CHANNEL_QUEUE)) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static void createDLCQueue(AMQQueue queue_entry, VirtualHost host, String owner) throws AMQException {
        String queueName = identifyTenantInformationAndGenerateDLCString(owner, AndesConstants.DEAD_LETTER_CHANNEL_QUEUE);
        QueueRegistry _queueRegistry = host.getQueueRegistry();

        AMQQueue queue = _queueRegistry.getQueue(new AMQShortString(queueName));
        try {
            if (queue == null) {
                AMQShortString ownerShortString = null;
                if (owner != null) {
                    ownerShortString = new AMQShortString(owner);
                }

                queue = AMQQueueFactory.createAMQQueueImpl(new AMQShortString(queueName), true, ownerShortString, false, false, host, null);
                if (queue.isDurable() && !queue.isAutoDelete()) {
                    host.getDurableConfigurationStore().createQueue(queue);
                    //send cluster notification to sync queues
                    ClusterResourceHolder.getInstance().getClusterManager().handleQueueAddition(queue.getName());
                }
                _queueRegistry.registerQueue(queue);
                //Will Create the Message Counter
                ClusterResourceHolder.getInstance().getCassandraMessageStore().addMessageCounterForQueue(queueName);
                //Will Create The Destination-Node Relationship
                ClusterResourceHolder.getInstance().getCassandraMessageStore().addNodeQueueToDestinationQueue(queueName, queueName);

                _logger.info(queueName + " Queue Created as Dead Letter Channel");

            } else {
                // _logger.info(AndesConstants.DEAD_LETTER_CHANNEL_QUEUE + " Queue Already Exisits");
            }


        } catch (Exception ex) {
            throw new AMQException("Exception Caught While Creating the Dead Letter Queue :", ex);
        }

    }
}
