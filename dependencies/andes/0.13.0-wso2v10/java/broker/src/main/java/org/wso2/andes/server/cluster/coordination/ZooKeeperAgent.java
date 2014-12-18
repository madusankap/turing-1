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
package org.wso2.andes.server.cluster.coordination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.*;

public class ZooKeeperAgent implements Watcher {

    private static Log log = LogFactory.getLog(ZooKeeper.class);
    private volatile ZooKeeper zk;
    volatile boolean shutdown;
    final String connectString;
    final ManualResetEvent isConnected;
    final ExecutorService connectExecutor;
    final ScheduledExecutorService callbackExecutor;
    final Set<ZookeeperSyncPrimitive> resurrectList;
    final int sessionTimeout;
    int maxConnectAttempts;
    IOException exception;

    public ZooKeeperAgent(String connectString) throws InterruptedException, IOException, ExecutionException {
        this(connectString, 1200000, 5);
    }

    public ZooKeeperAgent(String connectString, int sessionTimeout, int maxConnectAttempts) throws InterruptedException, IOException, ExecutionException {

        if (maxConnectAttempts < 1)
            throw new IllegalArgumentException("maxConnectAttempts must be greater than or equal to 0");

        shutdown = false;
        this.connectString = connectString;
        this.sessionTimeout = sessionTimeout;
        this.maxConnectAttempts = maxConnectAttempts;
        isConnected = new ManualResetEvent(false);
        callbackExecutor = Executors.newScheduledThreadPool(8);
        resurrectList = Collections.newSetFromMap(new WeakHashMap<ZookeeperSyncPrimitive, Boolean>());
        exception = null;

        connectExecutor = Executors.newSingleThreadExecutor();
        connectExecutor.submit(clientCreator).get();
        isConnected.waitOne();
    }

    public void shutdown() throws InterruptedException {
        shutdown = true;
        zk.close();
        callbackExecutor.shutdownNow();
    }

    public boolean isShutdown() {
        return shutdown;
    }

    void retryPrimitiveOperation(Runnable operation, int retries) {
        if (!shutdown) {
            int delay = 250 + retries * 500;
            if (delay > 7500)
                delay = 7500;
            callbackExecutor.schedule(operation, delay, TimeUnit.MILLISECONDS);
        }
    }

    void resurrectPrimitive(ZookeeperSyncPrimitive primitive) {
        if (!shutdown) {
            synchronized (resurrectList) {
                if (primitive.zooKeeper != this.zk) {
                    primitive.zooKeeper = this.zk;
                    primitive.resynchronize();
                } else {
                    resurrectList.add(primitive);
                }
            }
        }
    }

    @Override
    public void process(WatchedEvent event)  {
        if (event.getType() == Event.EventType.None) {
            Event.KeeperState state = event.getState();
            switch (state) {
                case SyncConnected:
                    onConnected();
                    break;
                case Disconnected:
                    onDisconnection();
                    break;
                case Expired:
                    onSessionExpired();
                    break;
            }
        }
    }

    private void onConnected() {
        isConnected.set();
        processResurrectList();
    }

    private void onDisconnection() {
        isConnected.reset();
    }

    private void onSessionExpired() {
        isConnected.reset();
        connectExecutor.submit(clientCreator);
    }

    private void processResurrectList() {
        synchronized (resurrectList) {
            ZookeeperSyncPrimitive[] toResurrect = resurrectList.toArray(new ZookeeperSyncPrimitive[] {});
            resurrectList.clear(); // clear before resynchronizing to prevent reentrancy issues
            for (ZookeeperSyncPrimitive primitive : toResurrect) {
                primitive.zooKeeper = this.zk;
                primitive.resynchronize();
            }
        }
    }

    private Callable<ZooKeeper> clientCreator = new Callable<ZooKeeper> () {

        @Override
        public ZooKeeper call() throws IOException, InterruptedException {

            int attempts = 0;
            int retryDelay = 50;

            while (true) {
                try {
                    zk = new ZooKeeper(connectString, sessionTimeout, ZooKeeperAgent.this);
                    return zk;
                } catch (IOException e) {
                    log.error("ZooKeeperAgent failed to connect client across network to specified cluster.", e);
                    attempts++;
                    if (maxConnectAttempts != 0 && attempts >= maxConnectAttempts)
                        throw (IOException)e.getCause();
                    retryDelay *= 2;
                    if (retryDelay > 7500)
                        retryDelay = 7500;
                }
                Thread.sleep(retryDelay);
            }
        }
    };

    private static ZooKeeperAgent instance;

    public static ZooKeeperAgent instance() {
        return instance;
    }

    public static void initializeInstance(String connectString) throws InterruptedException, IOException, ExecutionException {
        instance = new ZooKeeperAgent(connectString);
    }

    public static void initializeInstance(String connectString, int sessionTimeout, int maxAttempts) throws InterruptedException, IOException, ExecutionException {
        instance = new ZooKeeperAgent(connectString, sessionTimeout, maxAttempts);
    }

    public void initQueueWorkerCoordination() throws CoordinationException {

        try {
            if (zk.exists(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, false) == null) {
                zk.create(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT, new byte[0],
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } else {

                if(zk.getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT,false)
                        == null
                        || zk.getChildren(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT,
                        false).size() == 0) {
                        zk.delete(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT , -1);
                        zk.create(CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT,
                                new byte[0],
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                }

            }
        } catch (Exception e) {
            String msg = "Error while creating Queue worker coordination parent at " +
                    CoordinationConstants.QUEUE_WORKER_COORDINATION_PARENT;
            log.error(msg ,e );
            throw new CoordinationException(msg,e);
        }
    }

    /**
     * init the zookeeper agent to handle the Distributed Locks per queue
     * @param queue
     * @throws CoordinationException
     */
    public void initQueueResourceLockCoordination(String queue) throws CoordinationException {

        try {
            if (zk.exists(CoordinationConstants.QUEUE_RESOURCE_LOCK_PARENT + "_" + queue,
                    false) == null) {
                zk.create(CoordinationConstants.QUEUE_RESOURCE_LOCK_PARENT + "_" + queue,
                        new byte[0],
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

        } catch (Exception e) {
            String msg = "Error while creating Queue worker coordination parent at " +
                    CoordinationConstants.QUEUE_RESOURCE_LOCK_PARENT + "_" + queue;
            throw new CoordinationException(msg, e);
        }

    }

    public void initSubscriptionCoordination() throws CoordinationException {
        try {
            if (zk.exists(CoordinationConstants.SUBSCRIPTION_COORDINATION_PARENT,
                    false) == null) {
                zk.create(CoordinationConstants.SUBSCRIPTION_COORDINATION_PARENT,
                        new byte[0],
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

        } catch (Exception e) {
            String msg = "Error while creating Subscription coordination parent at " +
                    CoordinationConstants.SUBSCRIPTION_COORDINATION_PARENT;
            throw new CoordinationException(msg, e);
        }
    }

    public void initTopicSubscriptionCoordination() throws CoordinationException {
        try {
            if (zk.exists(CoordinationConstants.TOPIC_SUBSCRIPTION_COORDINATION_PARENT,
                    false) == null) {
                zk.create(CoordinationConstants.TOPIC_SUBSCRIPTION_COORDINATION_PARENT,
                        new byte[0],
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

        } catch (Exception e) {
            String msg = "Error while creating Subscription coordination parent at " +
                    CoordinationConstants.TOPIC_SUBSCRIPTION_COORDINATION_PARENT;
            throw new CoordinationException(msg, e);
        }
    }


    public ZooKeeper getZooKeeper() {
        return zk;
    }
}
