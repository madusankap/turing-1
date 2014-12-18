package org.wso2.andes.server.cluster.coordination;

import java.util.concurrent.TimeUnit;

public interface ResetEvent {
    public void set();

    public void reset();

    public void waitOne() throws InterruptedException;

    public boolean waitOne(int timeout, TimeUnit unit) throws InterruptedException;

    public boolean isSignalled();
}

