
package com.ponysdk.core.server.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * An executor that provides context isolation and priorities of tasks within a
 * context.<br>
 * Each task is attached to a {@link SchedulingContext}, destroying the context
 * will cancel all task attached to it. The implementation guarantees fairness
 * among several context (unless a real-time task is submitted), context
 * isolation (two task of the same context cannot be processed concurrently),
 * FIFO guarantee between task of same priority within a context
 *
 * @author amaire
 *
 */
public final class Scheduler {

    private final ScheduledExecutorService executor;
    private final ConcurrentLinkedQueue<SchedulingContextImpl> realtimeContexts = new ConcurrentLinkedQueue<>();

    public Scheduler(final int poolSize) {
        executor = Executors.newScheduledThreadPool(poolSize);
    }

    public Scheduler(final int poolSize, final ThreadFactory factory) {
        executor = Executors.newScheduledThreadPool(poolSize, factory);
    }

    /**
     * Create a new {@link SchedulingContext} that can be used to submit new tasks
     */
    public SchedulingContext createContext(final String name) {
        return new SchedulingContextImpl(name, this);
    }

    /**
     * Shutdown the scheduler, trying to cancel all pending tasks as fast as
     * possible.<br>
     */
    public void shutdown() {
        for (final Runnable r : executor.shutdownNow()) {
            if (r instanceof SchedulingContext) {
                ((SchedulingContext) r).destroy();
            }
        }

        realtimeContexts.clear();
    }

    /**
     * Wait for the Scheduler to shutdown properly up to the specify delay
     * 
     * @param timeoutMillis the maximum delay to wait (in millisecond)
     * @return true if the Scheduler is terminated, false otherwise
     */
    public boolean awaitTermination(final long timeoutMillis) throws InterruptedException {
        return executor.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    ////////////////////////////////////////

    boolean isShutdown() {
        return executor.isShutdown();
    }

    ScheduledFuture<?> executeLater(final long delayMillis, final SchedulingContextImpl context, final Runnable task) {
        try {
            return executor.schedule(task, delayMillis, MILLISECONDS);
        } catch (final RejectedExecutionException e) {
            context.destroy();
            throw e;
        }
    }

    ScheduledFuture<?> schedulePeriodicTask(final long periodMillis, final SchedulingContextImpl context, final Runnable task) {
        try {
            return executor.scheduleWithFixedDelay(task, periodMillis, periodMillis, MILLISECONDS);
        } catch (final RejectedExecutionException e) {
            context.destroy();
            throw e;
        }
    }

    void executeContext(final SchedulingContextImpl context, final boolean realtime) {
        try {
            executor.execute(context);
            if (realtime) realtimeContexts.offer(context);
        } catch (final RejectedExecutionException e) {
            context.destroy();
        }
    }

    SchedulingContextImpl pollRealtimeContext() {
        return realtimeContexts.poll();
    }

}
