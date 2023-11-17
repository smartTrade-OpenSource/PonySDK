package com.ponysdk.core.server.concurrent;

import java.time.Duration;
import java.util.concurrent.*;

public class Scheduler {
    private static final Scheduler INSTANCE = new Scheduler();

    private final ExecutorService executor;
    private final ScheduledExecutorService scheduledExecutor;

    private Scheduler() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Submit a task to the scheduler.
     *
     * @param task the task to execute
     */
    public static void execute(Runnable task) {
        INSTANCE.executor.submit(task);
    }

    /**
     * Schedule a task to be executed after a delay.
     *
     * @param delay the delay before the task is executed
     * @param task  the task to execute
     * @return a handle to the scheduled task
     */
    public static ScheduledTaskHandler executeLater(Duration delay, Runnable task) {
        if (delay.isNegative()) {
            throw new IllegalArgumentException("Delay must be positive");
        }
        Future<?> future = INSTANCE.scheduledExecutor.schedule(() -> execute(task), delay.toMillis(), TimeUnit.MILLISECONDS);
        return new ScheduledTaskHandler(future);
    }

    /**
     * Schedule a periodic task with the lowest priority. The task will only be executed
     * if there are no other tasks to execute.
     *
     * @param period the period between executions of the task
     * @param task   the periodic task to execute
     * @return a handle to the scheduled task
     */
    public static ScheduledTaskHandler schedule(Duration period, Runnable task) {
        if (period.isNegative() || period.isZero()) {
            throw new IllegalArgumentException("Period must be positive");
        }
        Future<?> future = INSTANCE.scheduledExecutor.scheduleAtFixedRate(() -> execute(task), period.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
        return new ScheduledTaskHandler(future);
    }

    public static class ScheduledTaskHandler {
        private final Future<?> future;

        public ScheduledTaskHandler(Future<?> future) {
            this.future = future;
        }

        /**
         * Cancel the scheduled task.
         *
         * @return true if the task was cancelled successfully, false otherwise
         */
        public boolean cancel() {
            return future.cancel(false);
        }
    }
}
