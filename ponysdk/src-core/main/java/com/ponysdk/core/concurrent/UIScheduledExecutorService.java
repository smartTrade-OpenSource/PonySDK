
package com.ponysdk.core.concurrent;

import java.util.concurrent.TimeUnit;

import com.ponysdk.core.concurrent.UIScheduledThreadPoolExecutor.UIRunnable;

public interface UIScheduledExecutorService {

    public UIRunnable schedule(Runnable command, long delay, TimeUnit unit);

    public UIRunnable scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit);

    public UIRunnable scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit);

}
