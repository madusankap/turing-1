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
import org.wso2.andes.AMQStoreException;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.cassandra.CassandraQueueMessage;
import org.wso2.andes.server.cassandra.DefaultClusteringEnabledSubscriptionManager;
import org.wso2.andes.server.cassandra.OnflightMessageTracker;
import org.wso2.andes.server.cassandra.QueueDeliveryWorker;
import org.wso2.andes.server.cluster.coordination.CoordinationConstants;
import org.wso2.andes.server.cluster.coordination.CoordinationException;
import org.wso2.andes.server.cluster.coordination.ZooKeeperAgent;
import org.wso2.andes.server.configuration.ClusterConfiguration;
import org.wso2.andes.server.queue.DLCQueueUtils;
import org.wso2.andes.server.store.CassandraMessageStore;
import org.apache.zookeeper.*;
import org.wso2.andes.server.store.util.CassandraDataAccessException;
import org.wso2.andes.server.util.AndesConstants;
import org.wso2.andes.server.util.AndesUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * F
 * Cluster Manager is responsible for Handling the Broker Cluster Management Tasks like
 * Queue Worker distribution. Fail over handling for cluster nodes. etc.
 */
public class ClusterManager {
    private Log log = LogFactory.getLog(ClusterManager.class);

    private ZooKeeperAgent zkAgent;
    private int nodeId;
    private String zkNode;

    private GlobalQueueManager globalQueueManager;

    //each node is assigned  an ID 0-x after arranging nodeIDs in an ascending order
    private int globalQueueSyncId;

    //Map that Keeps the node id
    private Map<Integer, ClusterNode> nodeMap = new ConcurrentHashMap<Integer, ClusterNode>();

    //in memory map keeping destination Queues active in the cluster
    private List<String> destinationQueueList = Collections.synchronizedList(new ArrayList<String>());

    //in memory map keeping nodeIds assigned for each node in cluster
    private List<Integer> clusterNodeIDList = Collections.synchronizedList(new ArrayList<Integer>());

    //in memory map keeping global queues assigned to the this node
    private List<String> globalQueuesAssignedToMe = Collections.synchronizedList(new ArrayList<String>());

    private String connectionString;

    /**
     * Create a ClusterManager instance for clustered environment
     *
     * @param messageStore       Underlying CassandraMessageStore
     * @param zkConnectionString zookeeper port
     */
    public ClusterManager(CassandraMessageStore messageStore, String zkConnectionString) {
        this.globalQueueManager = new GlobalQueueManager(messageStore);
        this.connectionString = zkConnectionString;
    }

    /**
     * Create a ClusterManager instance for standalone environment.
     * Then this will handle only standalone operations
     *
     * @param messageStore
     */
    public ClusterManager(CassandraMessageStore messageStore) {
        this.globalQueueManager = new GlobalQueueManager(messageStore);
        this.nodeId = 0;
    }

    private static int getNodeIdFromZkNode(String node) {
        return Integer.parseInt(node.substring(node.length() - 5));
    }

    /**
     * get global queue manager working in the current node
     *
     * @return
     */
    public GlobalQueueManager getGlobalQueueManager() {
        return this.globalQueueManager;
    }

    /**
     * Start all global queues and workers
     *
     * @throws CoordinationException
     */
    public void startAllGlobalQueueWorkers() throws CoordinationException {

        if (!ClusterResourceHolder.getInstance().getClusterConfiguration().isClusteringEnabled()) {
            List<String> globalQueueNames = AndesUtils.getAllGlobalQueueNames();
            for (String globalQueueName : globalQueueNames) {
                globalQueueManager.scheduleWorkForGlobalQueue(globalQueueName);
            }
        }
    }

    public void handleGlobalQueueAddition() {

        try {
            //get the current globalQueue Assignments
            List<String> currentGlobalQueueAssignments = new ArrayList<String>();
            for (String q : globalQueuesAssignedToMe) {
                currentGlobalQueueAssignments.add(q);
            }
            //update GlobalQueues to be assigned as to new situation in cluster
            updateGlobalQueuesAssignedTome();

            //stop any global queue worker that is not assigned to me now
            for (String globalQueue : currentGlobalQueueAssignments) {
                if (!globalQueuesAssignedToMe.contains(globalQueue)) {
                    globalQueueManager.removeWorker(globalQueue);
                }
            }

            //start global queue workers for queues assigned to me
            for (String globalQueue : globalQueuesAssignedToMe) {
                globalQueueManager.scheduleWorkForGlobalQueue(globalQueue);
            }

        } catch (KeeperException e) {
            log.error("Error in handling global queue worker assignment", e);
        } catch (InterruptedException e) {
            log.error("Error in handling global queue worker assignment", e);
        }


    }

    /**
     * Handles changes needs to be done in current node when a node joins to the cluster
     *
     * @param node node name
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void handleNewNodeJoiningToCluster(String node) throws KeeperException, InterruptedException {
        int id = getNodeIdFromZkNode(node);
        log.info("Handling cluster gossip: Node with ID " + id + " Joined the Cluster");
        //update in memory node map
        clusterNodeIDList.add(id);
        //reassign global queue sync id
        reAssignGlobalQueueSyncId();
        //register a node existence listener for the new node
        zkAgent.getZooKeeper().exists(CoordinationConstants.
                QUEUE_WORKER_COORDINATION_PARENT +
                CoordinationConstants.NODE_SEPARATOR + node,
                new NodeExistenceListener(node));
        //reassign global queues for yourself
        handleGlobalQueueAddition();
    }

    /**
     * update global queue synchronizing ID according to current status in cluster
     */
    private void reAssignGlobalQueueSyncId() {
        Collections.sort(clusterNodeIDList);
        int indexOfMyId = clusterNodeIDList.indexOf(new Integer(nodeId));
        globalQueueSyncId = indexOfMyId;
        log.info("ClusterManager- globalQueueSyncId for this node is: " + globalQueueSyncId);
    }

    /**
     * Start and stop global queue workers
     *
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void updateGlobalQueuesAssignedTome() throws KeeperException, InterruptedException {

        List<String> globalQueuesToBeAssigned = new ArrayList<String>();
        int globalQueueCount = ClusterResourceHolder.getInstance().getClusterConfiguration().getGlobalQueueCount();
        // At graceful shut down of broker we close the zk session hence this should only be called
        if (zkAgent.getZooKeeper().getState() != ZooKeeper.States.CLOSED) {
            List<String> nodeList = zkAgent.getZooKeeper().
                    getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, false);
            int clusterNodeCount = nodeList.size();
            for (int count = 0; count < globalQueueCount; count++) {
                if (count % clusterNodeCount == globalQueueSyncId) {
                    globalQueuesToBeAssigned.add(AndesConstants.GLOBAL_QUEUE_NAME_PREFIX + count);
                }
            }
            this.globalQueuesAssignedToMe.clear();
            for (String q : globalQueuesToBeAssigned) {
                globalQueuesAssignedToMe.add(q);
            }

        }

    }

    /**
     * Handles new Queue addition requests coming for this node
     *
     * @param destinationQueueName Queue to be added
     * @throws CoordinationException In case of a unexpected Error happening when running
     *                               the cluster coordination algorithm
     */
    public void handleQueueAddition(String destinationQueueName) throws CoordinationException {
        ClusterConfiguration config = ClusterResourceHolder.
                getInstance().getClusterConfiguration();

        //TODO handle this in once in order impl
        if (config.isOnceInOrderSupportEnabled()) {
            return;
        }

        //In Non Cluster Mode  we do not need any zookeeper related Coordination.
        if (!config.isClusteringEnabled()) {
            ClusterResourceHolder.getInstance().getSubscriptionManager().clearAndUpdateDestinationQueueList();
            return;
        }

        try {
            log.debug("Adding Queue : " + destinationQueueName + " to the cluster ");

            List<String> nodeList = zkAgent.getZooKeeper().
                    getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, false);

            //for every node in the cluster notify that queues were changed
            for (String node : nodeList) {
                String path = CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT +
                        CoordinationConstants.NODE_SEPARATOR + node;
                String data = CoordinationConstants.QUEUES_CHANGED_PREFIX + destinationQueueName;
                zkAgent.getZooKeeper().setData(path, data.getBytes(), -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            String msg = "Error while handling Queue worker Addition";
            log.error(msg, e);
            throw new CoordinationException(msg, e);
        }


    }

    /**
     * This method will only remove messages and related properties form the store for the queue removed.
     * Queues registered with Vhost will be removed separately
     *
     * @param destinationQueueName
     * @throws CoordinationException
     */
    public void handleQueueRemoval(String destinationQueueName) throws CoordinationException {

        ClusterConfiguration config = ClusterResourceHolder.getInstance().getClusterConfiguration();

        //TODO:need to handle this properly
        if (config.isOnceInOrderSupportEnabled()) {
            String globalQueueName = AndesUtils.getGlobalQueueNameForDestinationQueue(destinationQueueName);
            try {
                //remove messages from GQ
                ClusterResourceHolder.getInstance().getCassandraMessageStore().removeMessagesOfQueue(globalQueueName, false, destinationQueueName);
                //remove messages from DLC

            } catch (AMQStoreException e) {
                log.error("Error while removing messages from global queue " + globalQueueName, e);
            }
        }

        /**
         * In Non Cluster Mode  we do not need any zookeeper related Coordination.
         * Just
         * 1. get the message IDs addressed to this queue
         * 2. remove all the messages from node queue addressed to the deleted queue (routing key)
         * 3. remove all the messages from global queue addressed to the deleted queue (routing key)
         * 4. schedule the content of messages deleted above to be removed from the store
         * 5. remove deleted message batch from message property tracking
         * 6. remove destinationQueue-message ID mappings for deleted messages
         * 7. remove DLC messages addressed to this queue
         */
        if (!config.isClusteringEnabled()) {

            try {

                //remove any in-memory messages accumilated for the queue
                removeInMemoryMessagesAccumulated(destinationQueueName);

                //there can be non-acked messages in the node queue. Thus it is logical to remove whatever message (metaData)
                //in the node queue  when queue is removed
                ClusterResourceHolder.getInstance().getCassandraMessageStore().removeMessagesOfQueue(AndesUtils.getMyNodeQueueName(), true, destinationQueueName);

                //check the global queue as well
                String globalQueueName = AndesUtils.getGlobalQueueNameForDestinationQueue(destinationQueueName);
                ClusterResourceHolder.getInstance().getCassandraMessageStore().removeMessagesOfQueue(globalQueueName, false, destinationQueueName);

                //delete the raw allocated for queue mappings
                ClusterResourceHolder.getInstance().getCassandraMessageStore().deleteAllMessageIDsAddressedToQueue(destinationQueueName);

                //delete DLC messages
                ClusterResourceHolder.getInstance().getCassandraMessageStore().removeMessagesOfQueue
                        (DLCQueueUtils.identifyTenantInformationAndGenerateDLCString(destinationQueueName,AndesConstants.DEAD_LETTER_CHANNEL_QUEUE), true, destinationQueueName);

                log.info("Removed Messages Addressed to Queue " + destinationQueueName);

                //update in-memory map of keeping destination queue list
                ClusterResourceHolder.getInstance().getSubscriptionManager().clearAndUpdateDestinationQueueList();

                return;

            } catch (Exception e) {
                String msg = "Error while handling message deletion for the deleted queue:" + destinationQueueName;
                log.error(msg, e);
                throw new CoordinationException(msg, e);
            }
        }

        //1. do all above mentioned steps for global queue messages addressed to this queue
        //2. notify other nodes so that they can remove messages from their node queues
        try {
            log.debug("Removing Queue " + destinationQueueName + " From the cluster");

            //delete messages in the global queue
            String globalQueueName = AndesUtils.getGlobalQueueNameForDestinationQueue(destinationQueueName);
            ClusterResourceHolder.getInstance().getCassandraMessageStore().removeMessagesOfQueue(globalQueueName, false, destinationQueueName);

            List<String> nodeList = zkAgent.getZooKeeper().
                    getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, false);

            for (String node : nodeList) {

                String path = CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT +
                        CoordinationConstants.NODE_SEPARATOR + node;
                String data = CoordinationConstants.QUEUES_CHANGED_PREFIX + destinationQueueName;
                zkAgent.getZooKeeper().setData(path, data.getBytes(), -1);
            }
        } catch (KeeperException e) {
            throw new CoordinationException("Error while removing Queue ", e);
        } catch (InterruptedException e) {
            throw new CoordinationException("Error while removing Queue ", e);
        } catch (AMQStoreException e) {
            throw new CoordinationException("Error while removing Queue ", e);
        } finally {
        }

    }

    public void closeZkAgentWhenShutDownNode() throws CoordinationException {

        try {
            if (zkAgent.getZooKeeper() != null) {
                zkAgent.getZooKeeper().close();
                log.info("ClusterManager removed the exiting ZookeeperAgent for Node ID:" + getNodeId());
            }
        } catch (InterruptedException e) {
            throw new CoordinationException("Error while removing ZookeeperAgent for Node ID " + getNodeId(), e);
        }
    }

    /**
     * Initialize the Cluster manager. This will create ZNodes related to nodes and assign node ids
     *
     * @throws CoordinationException in a Error when communicating with Zookeeper
     */
    public void init() throws CoordinationException {
        final ClusterConfiguration config = ClusterResourceHolder.getInstance().getClusterConfiguration();
        final CassandraMessageStore messageStore = ClusterResourceHolder.getInstance().getCassandraMessageStore();

        /**
         * do following if clustering is disabled. Here no Zookeeper is involved
         * so nodeID will be always 0
         */
        if (!config.isClusteringEnabled()) {

            //update node information in durable store
            List<String> nodeList = messageStore.getStoredNodeIDList();
            for (String node : nodeList) {
                messageStore.deleteNodeData(node);
            }
            ((DefaultClusteringEnabledSubscriptionManager) ClusterResourceHolder.getInstance().
                    getSubscriptionManager()).clearAllPersistedStatesOfDissapearedNode(nodeId);
            messageStore.addNodeDetails("" + nodeId, config.getBindIpAddress());
            return;
        }

        /**
         * do following if cluster is enabled
         */
        try {

            // create a new node with a generated randomId
            // get the node name and id

            zkAgent = new ZooKeeperAgent(connectionString);
            zkAgent.initQueueWorkerCoordination();
            final String nodeName = CoordinationConstants.QUEUE_WORKER_NODE +
                    (UUID.randomUUID()).toString().replace("-", "_");
            String path = CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT
                    + nodeName;
            //Register the watcher for zoo keeper parent to be fired when children changed
            zkAgent.getZooKeeper().
                    getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, new Watcher() {

                        @Override
                        public void process(WatchedEvent watchedEvent) {
                            if (Event.EventType.NodeChildrenChanged == watchedEvent.getType()) {
                                try {
                                    List<String> nodeListFromZK =
                                            zkAgent.getZooKeeper().
                                                    getChildren(
                                                            CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, false);

                                    //identify and register this node
                                    for (String node : nodeListFromZK) {
                                        if ((CoordinationConstants.NODE_SEPARATOR + node).contains(nodeName)) {

                                            zkNode = node;
                                            nodeId = getNodeIdFromZkNode(node);
                                            log.info("Initializing Cluster Manager , " + "Selected Node id : " + nodeId);

                                            //register a listener for changes on my node data only
                                            zkAgent.getZooKeeper().
                                                    getData(CoordinationConstants.
                                                            QUEUE_WORKER_COORDINATION_PARENT +
                                                            CoordinationConstants.NODE_SEPARATOR +
                                                            node, new NodeDataChangeListener(), null);
                                            break;
                                        }

                                    }

                                    for (String node : nodeListFromZK) {

                                        //update in-memory node list
                                        int id = getNodeIdFromZkNode(node);
                                        clusterNodeIDList.add(id);

/*                                        //notify all other nodes that a new node joined with node ID
                                        String data = CoordinationConstants.NODE_CHANGED_PREFIX + zkNode;
                                        zkAgent.getZooKeeper().setData(CoordinationConstants.
                                                QUEUE_WORKER_COORDINATION_PARENT +
                                                CoordinationConstants.NODE_SEPARATOR +
                                                node, data.getBytes(), -1);

                                        //register a listener to be called when node data changed in any node


                                       //Add Listener for node existence of any node in the cluster
                                        zkAgent.getZooKeeper().exists(CoordinationConstants.
                                                QUEUE_WORKER_COORDINATION_PARENT +
                                                CoordinationConstants.NODE_SEPARATOR + node,
                                                new NodeExistenceListener(node));*/
                                    }

                                    List<String> storedNodes = messageStore.getStoredNodeIDList();

                                    //refresh stored nodes in cassandra
                                    /**
                                     * If nodeList size is one, this is the first node joining to cluster. Here we check if there has been
                                     * any nodes that lived before and somehow suddenly got killed. If there are such nodes clear the state of them and
                                     * copy back node queue messages of them back to global queue.
                                     * If node was the same machine:ip and zookeeper assigned a different id this logic will handle the confusion
                                     * We need to clear up current node's state as well as there might have been a node with same id and it was killed
                                     */
                                    ((DefaultClusteringEnabledSubscriptionManager) ClusterResourceHolder.getInstance().
                                            getSubscriptionManager()).clearAllPersistedStatesOfDissapearedNode(nodeId);

                                    //add current node information to durable store
                                    messageStore.addNodeDetails("" + nodeId, config.getBindIpAddress());

                                    for (String storedNode : storedNodes) {
                                        int storedNodeId = Integer.parseInt(storedNode);
                                        if (!clusterNodeIDList.contains(storedNodeId)) {
                                            ((DefaultClusteringEnabledSubscriptionManager) ClusterResourceHolder.getInstance().
                                                    getSubscriptionManager()).clearAllPersistedStatesOfDissapearedNode(storedNodeId);
                                            checkAndCopyMessagesOfNodeQueueBackToGlobalQueue(AndesUtils.getNodeQueueNameForNodeId(storedNodeId));
                                        }

                                    }

                                    // We need to put all messages in my node queue back to GlobalQueue in a restart
                                    checkAndCopyMessagesOfNodeQueueBackToGlobalQueue(AndesUtils.getMyNodeQueueName());
                                } catch (Exception e) {
                                    log.error("Error while coordinating cluster information while joining to cluster", e);
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    });
            // Once this method called above defined watcher will be fired
            zkAgent.getZooKeeper().create(path, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

            //wait until above task is completed
            Thread.sleep(4000);
            //update global queue synchronizing ID
            reAssignGlobalQueueSyncId();
            //handle global queue addition for this node
            handleGlobalQueueAddition();

            //notify node addition to all nodes and register node existence listeners for all nodes on this node
            List<String> nodeList = zkAgent.getZooKeeper().
                    getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, false);

            for (String node : nodeList) {
                String currentNodePath = CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT +
                        CoordinationConstants.NODE_SEPARATOR + node;

                String data = CoordinationConstants.NODE_CHANGED_PREFIX + zkNode;
                //notify all other nodes that a new node joined with node ID
                zkAgent.getZooKeeper().setData(currentNodePath, data.getBytes(), -1);
                //Add Listener for node existence of any node in the cluster
                zkAgent.getZooKeeper().exists(currentNodePath, new NodeExistenceListener(node));

            }

        } catch (Exception e) {
            e.printStackTrace();
            String msg = "Error while initializing the zookeeper coordination ";
            log.error("Error while initializing the zookeeper coordination ", e);
            throw new CoordinationException(msg, e);
        }
    }


    /**
     * This class will handle node data changes in ZooKeeper nodes
     */
    private class NodeDataChangeListener implements Watcher {

        @Override
        public void process(WatchedEvent event) {
            if (Event.EventType.NodeDataChanged == event.getType()) {


                //Date Change Event received. This can be Queue addition , Queue removal, Node addition
                try {

                    //as watcher is consumed we have to register a watcher again
                    byte[] data = zkAgent.getZooKeeper().
                            getData(CoordinationConstants.
                                    QUEUE_WORKER_COORDINATION_PARENT +
                                    CoordinationConstants.NODE_SEPARATOR +
                                    zkNode, new NodeDataChangeListener(), null);
                    String dataStr = new String(data);

                    //check if it is a new node joining
                    if (dataStr.startsWith(CoordinationConstants.NODE_CHANGED_PREFIX)) {
                        String node = dataStr.split(":")[1];
                        handleNewNodeJoiningToCluster(node);

                        //check if it is a queue change
                    } else if (dataStr.startsWith(CoordinationConstants.QUEUES_CHANGED_PREFIX)) {
                        String queueName = dataStr.substring(CoordinationConstants.QUEUES_CHANGED_PREFIX.length());
                        handleQueuesInClusterChanged(queueName);
                    }

                } catch (Exception e) {
                    log.fatal("Error processing the Node data change : This might cause serious " +
                            "issues in distributed queue management", e);
                }
            }
        }

    }

    /**
     * Called when a node in MB cluster disappears. Any logic that should be handled
     * by other nodes should come here
     */
    private class NodeExistenceListener implements Watcher {

        private String watchZNode = null;

        public NodeExistenceListener(String zNode) {
            this.watchZNode = zNode;
        }

        @Override
        public void process(WatchedEvent watchedEvent) {
            //node left the cluster
            if (Event.EventType.NodeDeleted == watchedEvent.getType()) {
                String path = watchedEvent.getPath();


                String[] parts = path.split(CoordinationConstants.NODE_SEPARATOR);
                String deletedNode = parts[parts.length - 1];

                try {
                    int deletedNodeId = getNodeIdFromZkNode(deletedNode);
                    // if the event is triggered for own node when shutting down there is no need to perform these tasks
                    // if it is for another node in cluster then only we do these cleanup tasks
                    if (!(Integer.parseInt(getMyNodeID()) == deletedNodeId)) {
                        log.info("Handling cluster gossip: Node with ID " + deletedNodeId + " left the cluster");
                        //Update the durable store
                        ClusterResourceHolder.getInstance().getCassandraMessageStore().
                                deleteNodeData("" + deletedNodeId);
                        //update in memory list
                        clusterNodeIDList.remove(new Integer(deletedNodeId));
                        //refresh global queue sync ID
                        reAssignGlobalQueueSyncId();
                        //reassign global queue workers
                        handleGlobalQueueAddition();

                        Collections.sort(clusterNodeIDList);
                        if (!clusterNodeIDList.isEmpty()) {
                            int IdOfMessageCopyTaskOwningNode = clusterNodeIDList.get(0);

                            if (nodeId == IdOfMessageCopyTaskOwningNode) {

                                //clear persisted states of disappeared node
                                ((DefaultClusteringEnabledSubscriptionManager) ClusterResourceHolder.getInstance().
                                        getSubscriptionManager()).clearAllPersistedStatesOfDissapearedNode(deletedNodeId);

                                /**
                                 * check and copy back messages of node queue belonging to disappeared node
                                 * the node having smallest zkId out of active nodes should handle this task
                                 */
                                checkAndCopyMessagesOfNodeQueueBackToGlobalQueue(AndesUtils.getNodeQueueNameForNodeId(deletedNodeId));
                            }
                        }

                    }
                } catch (Exception e) {
                    log.error("Error while removing node details", e);
                }

            } else {
                //if some other event type came we have to register the watcher again
                try {
                    zkAgent.getZooKeeper().exists(CoordinationConstants.
                            QUEUE_WORKER_COORDINATION_PARENT +
                            CoordinationConstants.NODE_SEPARATOR + watchZNode,
                            this);

                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Error while registering a watch for loader node : " + watchZNode, e);
                }
            }

        }

    }

    /**
     * get node id assigned by ZooKeeper for this node
     *
     * @return node id
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * get Zookeeper connection
     *
     * @return connection string
     */
    public String getZkConnectionString() {
        return connectionString;
    }

    /**
     * get binding address of the node
     *
     * @param nodeId id of node assigned by zookeeper
     * @return bind address
     */
    public String getNodeAddress(int nodeId) {
        return ClusterResourceHolder.getInstance().getCassandraMessageStore().getNodeData("" + nodeId);
    }

    /**
     * get Id list of ZooKeeper nodes
     *
     * @return list of Ids of cluster nodes
     */
    public List<Integer> getZkNodes() {
        List<String> storedNodes = ClusterResourceHolder.getInstance().getCassandraMessageStore().getStoredNodeIDList();
        ArrayList<Integer> zkNodeIdList = new ArrayList<Integer>();
        for (String storedNode : storedNodes) {
            Integer ZKId = Integer.parseInt(storedNode);
            zkNodeIdList.add(ZKId);
        }

        return zkNodeIdList;
    }

    public String[] getGlobalQueuesAssigned(int nodeId) {
        List<String> globalQueuesToBeAssigned = new ArrayList<String>();
        try {
            Collections.sort(clusterNodeIDList);
            int indexOfRequestedId = clusterNodeIDList.indexOf(new Integer(nodeId));
            int globalQueueCount = ClusterResourceHolder.getInstance().getClusterConfiguration().getGlobalQueueCount();
            List<String> nodeList = zkAgent.getZooKeeper().
                    getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, false);
            int clusterNodeCount = nodeList.size();
            for (int count = 0; count < globalQueueCount; count++) {

                if (count % clusterNodeCount == indexOfRequestedId) {
                    globalQueuesToBeAssigned.add(AndesConstants.GLOBAL_QUEUE_NAME_PREFIX + count);
                }
            }
        } catch (KeeperException e) {
            log.error("Error occurred while getting global queues assigned for node", e);
        } catch (InterruptedException e) {
            log.error("Error occurred while getting global queues assigned for node", e);
        }
        String[] globalQueueList = globalQueuesToBeAssigned.toArray(new String[globalQueuesToBeAssigned.size()]);
        return globalQueueList;
    }

    /**
     * get how many messages in the given global queue
     *
     * @param globalQueue global queue name
     * @return
     */
    public int numberOfMessagesInGlobalQueue(String globalQueue) {
        return globalQueueManager.getMessageCountOfGlobalQueue(globalQueue);
    }

    /**
     * Get all topics in the cluster
     *
     * @return list of topics created
     * @throws Exception
     */
    public List<String> getTopics() throws CassandraDataAccessException {
        return ClusterResourceHolder.getInstance().
                getCassandraMessageStore().getTopics();
    }

    /**
     * get a list of subscribers (destination queue names) in the cluster for the given topic
     *
     * @param topic name of topic
     * @return list of subscribers
     * @throws Exception
     */
    public List<String> getSubscribers(String topic) throws Exception {
        return ClusterResourceHolder.getInstance().
                getCassandraMessageStore().getRegisteredSubscriberQueuesForTopic(topic);
    }

    /**
     * Return number of subscribers in cluster for the given topic
     *
     * @param topic name of topic
     * @return number of subscribers
     * @throws Exception
     */
    public int getSubscriberCount(String topic) throws Exception {
        return ClusterResourceHolder.getInstance().
                getCassandraMessageStore().getRegisteredSubscriberQueuesForTopic(topic).size();
    }

    //TODO:can we implement moving global queue workers?
    public boolean updateWorkerForQueue(String queueToBeMoved, String newNodeToAssign) {
        boolean successful = false;
        return false;
    }

    /**
     * Get whether clustering is enabled
     *
     * @return
     */
    public boolean isClusteringEnabled() {
        ClusterConfiguration config = ClusterResourceHolder.getInstance().getClusterConfiguration();
        return config.isClusteringEnabled();
    }

    /**
     * Get the node ID assigned by ZooKeeper for the current node
     *
     * @return
     */
    public String getMyNodeID() {
        String nodeID = Integer.toString(nodeId);
        return nodeID;
    }

    /**
     * get destination queues in broker cluster
     *
     * @return
     */
    public List<String> getDestinationQueuesInCluster() {
        try {
            return ClusterResourceHolder.getInstance().getCassandraMessageStore().getDestinationQueues();
        } catch (AMQStoreException e) {
            log.error("Error in getting destination queues from store", e);
            return Collections.<String>emptyList();
        }
    }

    /**
     * gracefully stop all global queue workers assigned for the current node
     */
    public void shutDownMyNode() {
        try {
            //stop all global queue Workers
            globalQueueManager.removeAllQueueWorkersLocally();
        } catch (Exception e) {
            log.error("Error stopping global queues while shutting down", e);
        }

    }

    /**
     * get message count of node queue belonging to given node
     *
     * @param zkId             ZK ID of the node
     * @param destinationQueue destination queue name
     * @return message count
     */
    public int getNodeQueueMessageCount(int zkId, String destinationQueue) {
        return ClusterResourceHolder.getInstance().getCassandraMessageStore().
                getMessageCountOfNodeQueueForDestinationQueue(AndesConstants.NODE_QUEUE_NAME_PREFIX + zkId, destinationQueue);
    }

    /**
     * get number of subscribers to given node for given destination queue
     *
     * @param zkId             ZK ID of the node
     * @param destinationQueue destination queue name
     * @return message count
     */
    public int getNodeQueueSubscriberCount(int zkId, String destinationQueue) {
        return ClusterResourceHolder.getInstance().getCassandraMessageStore().
                getNumberOfSubscribersOnNodeForDestinationQueue(zkId, destinationQueue);
    }

    /**
     * remove in-memory messages tracked for this queue
     *
     * @param destinationQueueName name of queue messages should be removed
     */
    public void removeInMemoryMessagesAccumulated(String destinationQueueName) {
        //remove in-memory messages accumulated due to sudden subscription closing
        QueueDeliveryWorker queueDeliveryWorker = ((DefaultClusteringEnabledSubscriptionManager) ClusterResourceHolder.getInstance().
                getSubscriptionManager()).getQueueDeliveryWorkerOnCurrentNode();
        if (queueDeliveryWorker != null) {
            queueDeliveryWorker.clearMessagesAccumilatedDueToInactiveSubscriptionsForQueue(destinationQueueName);
        }
        //remove sent but not acked messages
        OnflightMessageTracker.getInstance().getSentButNotAckedMessagesOfQueue(destinationQueueName);
    }

    /**
     * handles changes needed when a queue is added to or removed from cluster
     *
     * @param queueName name of the queue
     */
    public void handleQueuesInClusterChanged(String queueName) {
        try {
            log.debug("CM>> handleQueuesInClusterChanged called");
            //get queues from durable store
            List<String> queuesFromDurableStore = ClusterResourceHolder.getInstance().getCassandraMessageStore().getDestinationQueues();
            //identify queues newly added
            List<String> queuesToBeAdded = getDestinationQueuesToBeAdded(queuesFromDurableStore);
            //handle queue addition for this node
            if (queuesToBeAdded != null && !queuesToBeAdded.isEmpty()) {
                for (String queue : queuesToBeAdded) {
                    //update in-memory map of keeping destination queue list
                    log.debug("CM >> need to add queue " + queue);
                    ClusterResourceHolder.getInstance().getSubscriptionManager().clearAndUpdateDestinationQueueList();
                    //there is nothing to do here as VirtualHostConfigSynchronizer handles this
                }
            }
            //identify queues removed
            List<String> queuesToBeRemoved = getDestinationQueuesToBeRemoved(queuesFromDurableStore);
            //handle queue removal for this node
            if (queuesToBeRemoved != null && !queuesToBeRemoved.isEmpty()) {
                for (String queue : queuesToBeRemoved) {
                    log.debug("CM >> need to remove queue " + queue);
                    //remove any in-memory messages accumulated for the queue
                    removeInMemoryMessagesAccumulated(queue);
                    //clear messages from node queue
                    ClusterResourceHolder.getInstance().getCassandraMessageStore().removeMessagesOfQueue(AndesUtils.getMyNodeQueueName(), true, queue);
                    //clear DLC messages
                    //delete DLC messages
                    ClusterResourceHolder.getInstance().getCassandraMessageStore().removeMessagesOfQueue
                            (DLCQueueUtils.identifyTenantInformationAndGenerateDLCString(queue,AndesConstants.DEAD_LETTER_CHANNEL_QUEUE), true, queue);
                    //update in-memory map of keeping destination queue list
                    ClusterResourceHolder.getInstance().getSubscriptionManager().clearAndUpdateDestinationQueueList();
                    //invalidate any consumers for the deleted queue
                    ClusterResourceHolder.getInstance().getSubscriptionManager().updateNodeQueuesForDestinationQueueMap();
                    //remove the queue and all bindings from underlying qpid
                    ClusterResourceHolder.getInstance().getVirtualHostConfigSynchronizer().removeQueueAndAllItsBindings(queue);

                }
            }
            //handle queue purging for this node
            if(!queuesToBeAdded.contains(queueName) && queuesFromDurableStore.contains(queueName)){
                //remove any in-memory messages accumulated for the queue
                removeInMemoryMessagesAccumulated(queueName);
                //clear messages in the node queue
                ClusterResourceHolder.getInstance().getCassandraMessageStore().removeMessagesOfQueue(AndesUtils.getMyNodeQueueName(), true, queueName);
                //delete DLC messages
                ClusterResourceHolder.getInstance().getCassandraMessageStore().removeMessagesOfQueue
                        (DLCQueueUtils.identifyTenantInformationAndGenerateDLCString(queueName,AndesConstants.DEAD_LETTER_CHANNEL_QUEUE), true, queueName);
            }
            //update the memory map from durable store
            destinationQueueList.clear();
            for (String q : queuesFromDurableStore) {
                destinationQueueList.add(q);
            }
        } catch (Exception e) {
            log.error("Error in handling queue changing in the cluster", e);
        }
    }

    private List<String> getDestinationQueuesToBeRemoved(List<String> queuesFromDurableStore) {
        ArrayList<String> queueList = new ArrayList<String>();
        for (String q : destinationQueueList) {
            if (!queuesFromDurableStore.contains(q)) {
                queueList.add(q);
            }
        }

        return queueList;
    }

    private List<String> getDestinationQueuesToBeAdded(List<String> queuesFromDurableStore) {
        ArrayList<String> queueList = new ArrayList<String>();
        for (String q : queuesFromDurableStore) {
            if (!destinationQueueList.contains(q)) {
                queueList.add(q);
            }
        }

        return queueList;
    }

    private void checkAndCopyMessagesOfNodeQueueBackToGlobalQueue(String nodeQueueName) {
        ClusterResourceHolder.getInstance().getCassandraMessageStore().removeMessagesFromNodeQueueAndCopyToGlobalQueues(nodeQueueName);
    }

}
