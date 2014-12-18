package org.wso2.andes.server.cluster.coordination;

import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;

public abstract class ZookeeperSyncPrimitive implements Watcher {

    ZooKeeper zooKeeper;
    private ZooKeeperAgent session;
    private List<Runnable> stateUpdateListeners;
    private List<Runnable> dieListeners;
    private ManualResetEvent isSynchronized;
    private volatile boolean resynchronizeNeeded;
    private volatile ZookeeperException killedByException;
    private Runnable retryOnConnect;
    private int retries;
    private Integer mutex;

    protected ZookeeperSyncPrimitive(ZooKeeperAgent session) {
        this.session = session;
        zooKeeper = this.session.getZooKeeper();
        stateUpdateListeners = null;
        dieListeners = null;
        isSynchronized = new ManualResetEvent(false);
        resynchronizeNeeded = false;
        retryOnConnect = null;
        retries = 0;
        mutex = new Integer(-1);
    }

    public void waitSynchronized() throws ZookeeperException, InterruptedException {
        isSynchronized.waitOne();

        if (getKillerException() == null)
            return;

        throw getKillerException();
    }

    public void addUpdateListener(Runnable handler, boolean doStartupRun) {
        synchronized (mutex) {
            if (stateUpdateListeners == null) {
                stateUpdateListeners = new ArrayList<Runnable>(8);
            }
            stateUpdateListeners.add(handler);
            if (doStartupRun && killedByException == null && isSynchronized.isSignalled()) {
                handler.run();
            }
        }
    }

    public void removeUpdateListener(Runnable handler) {
        stateUpdateListeners.remove(handler);
    }

    public void addDieListener(Runnable handler) {
        synchronized (mutex) {
            if (dieListeners == null) {
                dieListeners = new ArrayList<Runnable>(8);
            }
            dieListeners.add(handler);
            if (killedByException != null) {
                handler.run();
            }
        }
    }

    public boolean isAlive() {
        return killedByException != null;
    }

    public ZookeeperException getKillerException() {
        return killedByException;
    }

    public boolean isResynchronizing() {
        return resynchronizeNeeded;
    }

    protected void onStateUpdated() {
        synchronized (mutex) {
            killedByException = null;
            // Notify handlers ***before*** signalling state update to allow pre-processing
            if (stateUpdateListeners != null) {
                for (Runnable handler : stateUpdateListeners)
                    handler.run();
            }
            // Signal state updated
            isSynchronized.set();
        }
    }

    protected void resynchronize() {}

    protected void onDie(ZookeeperException killerException) {}

    protected void onConnected() {}

    protected void onDisconnected() {}

    protected void onSessionExpired() {}

    protected void onNodeCreated(String path) {}

    protected void onNodeDeleted(String path) {}

    protected void onNodeDataChanged(String path) {}

    protected void onNodeChildrenChanged(String path) {}

    protected boolean shouldRetryOnError() { return false; }

    protected boolean shouldRetryOnTimeout() { return true; }

    protected boolean shouldResurrectOnSessionExpiry() { return false; }

    ZooKeeper zooKeeper() {
        return zooKeeper;
    }

    protected void die(Code rc) {
        KeeperException killerException = KeeperException.create(rc);
        die(killerException);
    }

    protected void die(KeeperException killerException) {
        die(new ZookeeperException(killerException));
    }

    protected void die(ZookeeperException killerException) {
        synchronized (mutex) {
            this.killedByException = killerException;
            onDie(killerException);
            if (dieListeners != null) {
                for (Runnable handler : dieListeners)
                    handler.run();
            }
            isSynchronized.set();
        }
    }

    protected boolean passOrTryRepeat(int rc, Code[] acceptable, Runnable operation) {

        Code opResult = Code.get(rc);

        for (Code code : acceptable) {
            if (opResult == code) {
                retries = 0;
                return true;
            }
        }

        switch (opResult) {
            case CONNECTIONLOSS:
                retryOnConnect(operation);
                break;
            case SESSIONMOVED:
            case OPERATIONTIMEOUT:
                if (shouldRetryOnTimeout()) {
                    retryAfterDelay(operation, retries++);
                }
                break;
            case SESSIONEXPIRED:
                if (shouldResurrectOnSessionExpiry()) {
                    resynchronizeNeeded = true;
                    requestRessurrection();
                } else {
                    die(opResult);
                }
                break;
            default:
                if (shouldRetryOnError()) {
                    retryAfterDelay(operation, retries++);
                } else {
                    die(opResult);
                }
                break;
        }

        return false;
    }

    @Override
    public void process(WatchedEvent event)  {
        String eventPath = event.getPath();
        EventType eventType = event.getType();
        KeeperState keeperState = event.getState();

        switch (eventType) {
            case None:
                switch (keeperState) {
                    case SyncConnected:
                        if (resynchronizeNeeded) {
                            resynchronizeNeeded = false;
                            resynchronize();
                        } else {
                            onConnected();
                            if (retryOnConnect != null) {
                                retryOnConnect.run();
                                retryOnConnect = null;
                            }
                        }
                        break;
                    case Disconnected:
                        onDisconnected();
                        break;
                    case Expired:
                        if (shouldResurrectOnSessionExpiry()) {
                            resynchronizeNeeded = true;
                            requestRessurrection();
                        } else {
                            die(Code.SESSIONEXPIRED);
                        }
                        onSessionExpired();
                        break;
                }
                break;
            case NodeCreated:
                onNodeCreated(eventPath);
                break;
            case NodeDeleted:
                onNodeDeleted(eventPath);
                break;
            case NodeDataChanged:
                onNodeDataChanged(eventPath);
                break;
            case NodeChildrenChanged:
                onNodeChildrenChanged(eventPath);
                break;
            default:
                // in case version mismatch
                die(Code.SYSTEMERROR);
                break;
        }
    }

    private void retryOnConnect(Runnable operation) {
        retryOnConnect = operation;
    }

    private void retryAfterDelay(Runnable operation, int retries) {
        session.retryPrimitiveOperation(operation, retries);
    }

    private void requestRessurrection() {
        session.resurrectPrimitive(this);
    }
}

