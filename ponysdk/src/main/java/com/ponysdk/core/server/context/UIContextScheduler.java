package com.ponysdk.core.server.context;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UIContextScheduler {

    private static final Logger log = LoggerFactory.getLogger(UIContextScheduler.class);
    private final Thread schedulerThread;
    private final PriorityQueue<ScheduledTask> tasks = new PriorityQueue<>();
    private final ReentrantLock taskLock = new ReentrantLock();
    private final Condition available = taskLock.newCondition();
    private boolean stopped = false;

    UIContextScheduler(UIContext context) {
        schedulerThread = Thread.ofVirtual().name("UIContext-thread-" + context.getID()).unstarted(this::mainLoop);
    }


    /**
     * starts the virtual thread that will process instructions for this UIContext
     */
    void start() {
        schedulerThread.start();
    }

    /**
     * stops the UI thread and cancel any pending task. This method will block until the UI thread is stopped, and may interrupt the current running task to do so
     */
    void stop() {
        taskLock.lock();
        try {
            stopped = true;
            tasks.forEach(ScheduledTask::cancel);
        } finally {
            taskLock.unlock();
        }
        schedulerThread.interrupt();
        try {
            schedulerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Submit another task for asynchronous execution. Tasks submitted through this
     * method will be treated in a strict FIFO order
     *
     * @param task the task to execute
     */
    void execute(Runnable task) {
        offerScheduledTask(new ScheduledTask(task, false));
    }

    /**
     * Submit another real-time task for asynchronous execution. Tasks submitted
     * through this method will be treated in a strict FIFO order, however they will
     * be executed before standard and periodic tasks
     *
     * @param task the task to execute as soon as possible
     */
    void forceExecute(Runnable task) {
        offerScheduledTask(new ScheduledTask(task, true));
    }


    /**
     * Schedule a given task in the future, with same policy than
     * {@link #execute(Runnable)}
     *
     * @param delayNanos the delay before actually submitting the task
     * @param task       the task to execute in the future
     * @return a ScheduledTaskHandler that allow this task to be cancelled
     */
    ScheduledTaskHandler executeLater(long delayNanos, Runnable task) {
        ScheduledTask handler = new ScheduledTask(task, delayNanos, false);
        offerScheduledTask(handler);
        return handler;
    }

    /**
     * Schedule a periodic task, with the lowest priority possible : the task can
     * execute only if there is no other task to execute for this context There is
     * some fairness logic implemented between scheduled task of the same context :
     * the one that has been denied execution for the longest time will be triggered
     * before the others
     *
     * @param periodNanos the delay before actually submitting the task
     * @param task        the periodic task to execute
     * @return a ScheduledTaskHandler that allow this task to be cancelled
     */
    ScheduledTaskHandler schedule(long periodNanos, Runnable task) {
        ScheduledTask handler = new ScheduledTask(task, periodNanos, true);
        offerScheduledTask(handler);
        return handler;
    }

    private void mainLoop() {
        while (!stopped) {
            ScheduledTask scheduledTask = getTask();
            Runnable task = scheduledTask == null ? null : scheduledTask.task;
            if (task == null) continue;
            task.run();
            scheduledTask.afterExecution(); //update task for a potential further execution
            if (scheduledTask.task != null) offerScheduledTask(scheduledTask);
        }
        log.info("UIContext thread {} ended", Thread.currentThread().getName());
    }

    private void offerScheduledTask(ScheduledTask task) {
        taskLock.lock();
        try {
            if (stopped) return;
            tasks.offer(task);
            if (task == tasks.peek()) available.signal();
        } finally {
            taskLock.unlock();
        }

    }


    private ScheduledTask getTask() {
        taskLock.lock();
        try {
            while (!stopped) {
                ScheduledTask task = tasks.peek();
                if (task == null) {
                    available.await();
                } else if (task.task == null) {
                    tasks.poll(); // remove deleted task
                } else {
                    long awaitNanos = task.computeAwaitNanos();
                    if (awaitNanos <= 0) return tasks.poll();
                    available.awaitNanos(awaitNanos);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            taskLock.unlock();
        }
        return null;
    }

    private static class ScheduledTask implements ScheduledTaskHandler, Comparable<ScheduledTask> {

        private static final long ORIGIN = System.nanoTime() - TimeUnit.SECONDS.toNanos(1L);
        private static final long PERIODIC_FLG = 1L << 62;

        private Runnable task;
        private long trigger;
        private long period;

        private ScheduledTask(Runnable task, boolean realtime) {
            this.task = task;
            trigger = realtime ? computeRealTimeTrigger() : computeTrigger();
        }

        private ScheduledTask(Runnable task, long delay, boolean periodic) {
            this.task = task;
            period = delay;
            trigger = periodic ? computePeriodicTrigger(delay) : computeDelayedTrigger(delay);
        }

        private long computeAwaitNanos() {
            //if trigger is negative => real time => no wait, values are here just for FIFO ordering
            return trigger < 0 ? 0 : (trigger & ~PERIODIC_FLG) + ORIGIN - System.nanoTime();
        }


        private void afterExecution() {
            if ((trigger & PERIODIC_FLG) == 0) {
                cancel();
            } else {
                //periodic => reschedule
                trigger = computePeriodicTrigger(period);
            }
        }

        @Override
        public final void cancel() {
            task = null;
        }

        private static long computeTrigger() {
            return System.nanoTime() - ORIGIN;
        }

        private static long computeDelayedTrigger(long delay) {
            return computeTrigger() + delay;
        }

        private static long computePeriodicTrigger(long period) {
            return computeDelayedTrigger(period) | PERIODIC_FLG;
        }

        private static long computeRealTimeTrigger() {
            return Long.MIN_VALUE + computeTrigger();
        }

        @Override
        public int compareTo(ScheduledTask o) {
            return Long.compare(trigger, o.trigger);
        }
    }


}
