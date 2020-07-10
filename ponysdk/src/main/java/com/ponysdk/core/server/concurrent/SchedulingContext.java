
package com.ponysdk.core.server.concurrent;

public interface SchedulingContext {

    /**
     * Submit another task to the scheduler using this context. For a given context,
     * task submitted through this method will be treated in a strict FIFO order
     * 
     * @param task the task to execute
     */
    void execute(Runnable task);

    /**
     * Submit another real-time task to the scheduler using this context. For a
     * given context, task submitted through this method will be treated in a strict
     * FIFO order, however they will be executed before standard and periodic time
     * in that context. Moreover, other contexts that are ready to process a non
     * real-time task will try to execute that real-time task as soon as possible
     * 
     * @param task the task to execute as soon as possible
     */
    void forceExecute(Runnable task);

    /**
     * Submit another task with same policy as {@link #execute(Runnable)}, but wait
     * for the asynchronous task to complete (or being cancelled). This behavior is
     * recommended to apply ack pressure on a very active task producer
     * 
     * @param task the task to execute
     * @throws InterruptedException
     * @throws RuntimeException if running the underlying task throws an exception
     */
    void executeAndWait(Runnable task) throws InterruptedException;

    /**
     * Same as {@link #executeAndWait(Runnable)}, but the task will be a real-time
     * task
     * 
     * @param task the task to execute as soon as possible
     * @throws InterruptedException
     * @throws RuntimeException if running the underlying task throws an exception
     */
    void forceExecuteAndWait(Runnable task) throws InterruptedException;

    /**
     * Schedule a given task in the future, with same policy than
     * {@link #execute(Runnable)}
     * 
     * @param delayMillis the delay before actually submitting the task
     * @param task the task to execute in the future
     * @return a ScheduledTaskHandler that allow this task to be cancelled
     */
    ScheduledTaskHandler executeLater(long delayMillis, Runnable task);

    /**
     * Schedule a given task in the future, with same policy than
     * {@link #forceExecute(Runnable)}
     * 
     * @param delayMillis the delay before actually submitting the task
     * @param task the task to execute in the future
     * @return a ScheduledTaskHandler that allow this task to be cancelled
     */
    ScheduledTaskHandler forceExecuteLater(long delayMillis, Runnable task);

    /**
     * Schedule a periodic task, with the lowest priority possible : the task can
     * execute only if there is no other task to execute for this context There is
     * some fairness logic implemented between scheduled task of the same context :
     * the one that has been denied execution for the longest time will be triggered
     * before the others
     * 
     * @param delayMillis the delay before actually submitting the task
     * @param task the periodic task to execute
     * @return a ScheduledTaskHandler that allow this task to be cancelled
     */
    ScheduledTaskHandler schedule(long periodMillis, Runnable task);

    /**
     * Destroy this context, canceling all pending tasks.
     */
    void destroy();

    /**
     * 
     * Wait for the context to shutdown properly up to the specified delay
     * 
     * @param timeoutMillis the maximum delay to wait (in millisecond)
     * @return true if the context is terminated (no more relative tasks are
     *         running, queued or scheduled), false otherwise
     */
    boolean awaitDestruction(long timeoutInMillis) throws InterruptedException;

}