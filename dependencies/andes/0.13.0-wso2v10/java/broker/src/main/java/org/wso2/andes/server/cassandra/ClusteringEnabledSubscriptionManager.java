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


import org.wso2.andes.AMQException;
import org.wso2.andes.AMQInternalException;
import org.wso2.andes.server.AMQChannel;
import org.wso2.andes.server.cluster.coordination.CoordinationException;
import org.wso2.andes.server.queue.AMQQueue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * <code>ClusteringEnabledSubscriptionManager</code> Manage the Queue Subscriptions Handling
 * Scenarios. This can have many implementation based on the level of
 * 1) Consistency
 * 2) Performance
 * etc..
 * expected from the Broker
 */
public interface ClusteringEnabledSubscriptionManager {


    /**
     * initialize the Subscription manager
     */
    public void init();

    /**
     * Handle the Subscription addition for a queue
     * @param queue AMQQueue object that client subscribing
     * @param subscription  Subscription for a Queue
     */
    public void addSubscription(AMQQueue queue ,CassandraSubscription subscription) throws AMQException, CoordinationException;

    /**
     * Handle Subscription removal
     * @param queueName
     * @param subscriptionId
     */
    public void removeSubscription(String queueName , String subscriptionId, boolean isBoundToTopics);

    /**
     * Get the Map that keeps the Locks for Un acknowledged messages
     * This Api is provided either to add the message Lock and wait on that lock
     * or get a lock and release that lock in case of a message acknowledgement
     * @return UnAcknowledged message lock map
     */
    public Map<AMQChannel,Map<Long, Semaphore>> getUnAcknowledgedMessageLocks();


    public Map<AMQChannel,QueueSubscriptionAcknowledgementHandler> getAcknowledgementHandlerMap();

    /**
     * Stop all the queue workers running at the moment
     * */
    public void stopAllMessageFlushers();

    /**
     * Start all the queue workers that were running
     */
    public void startAllMessageFlushers();

    /**
     * Clear and update destination queue list
     */
    public void clearAndUpdateDestinationQueueList();

    /**
     * get destination queue list in cluster
     * @return destination
     */
    public List<String> getDestinationQueues();

    /**
     * update in memory map keeping which nodes has subscriptions for given destination queue name
     * @return List of destination queues where there was no subscriptions in cluster and after update there are some
     */
    public List<String> updateNodeQueuesForDestinationQueueMap();

    /**
     * get a List of nodes having subscriptions to the given destination queue
     * @param destinationQueue destination queue name
     * @return list of nodes
     */
    public List<String> getNodeQueuesHavingSubscriptionsForQueue(String destinationQueue);

    /**
     *Find and handle the first subscription joins to the cluster for an existing queue
     */
    public void handleFreshSubscriptionsJoiningToCluster();


    /**
     * get a list of encoded subscription information
     * @return list of encoded string
     *
     */
    public List<String> getAllSubscriptionInformation();

}
