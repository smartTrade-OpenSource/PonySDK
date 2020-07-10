
package com.ponysdk.core.server.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SchedulingContextImpl implements Runnable, SchedulingContext {

    private static final Logger logger = LoggerFactory.getLogger(SchedulingContextImpl.class);

    private final Scheduler scheduler;
    private final String name;

    // count the number of active tasks in the context itself (future and periodic
    // tasks ignored until they are scheduled)
    // if > 0, then the Context should be added in the scheduler internal executor
    // queue
    private final AtomicInteger taskCount = new AtomicInteger(0);
    // the number of task a context is allowed to execute when scheduled.
    // However only real time tasks are allowed to execute starting from the second
    // token
    private final AtomicInteger executionTokens = new AtomicInteger(0);
    // Monotonic counter to allow old periodic tasks to execute before newer
    // periodic tasks
    private final AtomicLong ticketGenerator = new AtomicLong(0);

    private final ConcurrentLinkedQueue<Runnable> realTimeTasks = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> standardTasks = new ConcurrentLinkedQueue<>();
    private final AtomicReference<PeriodicTaskHolder> nextPeriodicTask = new AtomicReference<>(null);

    // Weak set of scheduled tasks relative to this context, they should be
    // cancelled when the context is destroyed
    private final Set<ScheduledFuture<?>> scheduledTasks = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    // correctly shutdown of the context will involved a Dekker pattern between
    // alive and scheduledTasks :
    // destroy will set alive flag then drain the tasks, whereas submitting tasks
    // will submit, check alive after and purge
    // tasks if context is not alive
    private volatile boolean alive = true;

    SchedulingContextImpl(final String name, final Scheduler scheduler) {
        this.name = name;
        this.scheduler = scheduler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final Runnable task) {
        execute0(task, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forceExecute(final Runnable task) {
        execute0(task, true);
    }

    /**
     * {@inheritDoc}
     *
     * @throws InterruptedException
     */
    @Override
    public void executeAndWait(final Runnable task) throws InterruptedException {
        executeAndWait0(task, false);
    }

    /**
     * {@inheritDoc}
     *
     * @throws InterruptedException
     */
    @Override
    public void forceExecuteAndWait(final Runnable task) throws InterruptedException {
        executeAndWait0(task, true);
    }

    private void executeAndWait0(final Runnable task, final boolean realtime) throws InterruptedException {
        if (!alive) return;
        final FutureTask<?> future = new FutureTask<>(task, null);
        execute0(future, realtime);
        try {
            while (alive && !scheduler.isShutdown()) {
                try {
                    future.get(1, TimeUnit.SECONDS);
                } catch (final TimeoutException e) {
                    continue;
                }
                break;
            }
        } catch (final ExecutionException e) {
            rethrowUnchecked(e.getCause());
        } catch (final CancellationException e) {
            // the context or scheduler is destroyed ....
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledTaskHandler executeLater(final long delayMillis, final Runnable task) {
        return executeLater0(delayMillis, () -> execute0(task, false));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledTaskHandler forceExecuteLater(final long delayMillis, final Runnable task) {
        return executeLater0(delayMillis, () -> execute0(task, true));
    }

    private ScheduledTaskHandler executeLater0(final long delayMillis, final Runnable task) {
        final FutureTaskHolder holder = FutureTaskHolder.scheduleFutureTask(this, delayMillis, task);
        return toHandler(holder.getFuture());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScheduledTaskHandler schedule(final long periodMillis, final Runnable task) {
        final PeriodicTaskHolder pTask = PeriodicTaskHolder.schedulePeriodicTask(this, periodMillis, task);
        return toHandler(pTask.getFuture());
    }

    private void execute0(final Runnable task, final boolean realtime) {
        if (!alive) return;
        final ConcurrentLinkedQueue<Runnable> taskQueue = realtime ? realTimeTasks : standardTasks;
        taskQueue.offer(task);
        if (taskCount.getAndIncrement() == 0) {
            scheduler.executeContext(this, realtime);
        }
    }

    private ScheduledTaskHandler toHandler(final ScheduledFuture<?> future) {
        // DON'T CHECK LIVENESS BEFORE ADD !! KEEP THIS ORDER (Dekker pattern with
        // destroy, cf alive declaration)
        scheduledTasks.add(future);
        if (!alive) cancelScheduledTasks();
        return new ScheduledTaskHandler(future);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        alive = false;
        cancelScheduledTasks();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean awaitDestruction(final long timeoutInMillis) throws InterruptedException {

        final long deadline = System.nanoTime() + MILLISECONDS.toNanos(timeoutInMillis);

        while (!scheduler.isShutdown() && (alive || taskCount.get() > 0)) {
            if (System.nanoTime() - deadline > 0) return false;
            if (Thread.interrupted()) throw new InterruptedException();
            LockSupport.parkNanos(MILLISECONDS.toNanos(1L));
        }

        if (scheduler.isShutdown()) {
            destroy();
            return scheduler.awaitTermination(NANOSECONDS.toMillis(deadline - System.nanoTime()));
        } else {
            return true;
        }

    }

    @Override
    public void run() {
        // Work stealing on the realtime context queue
        while (realTimeTasks.isEmpty()) {
            final SchedulingContextImpl realtimeContext = scheduler.pollRealtimeContext();
            if (realtimeContext == null || realtimeContext == this) break; // no realtime task to execute
            realtimeContext.run0(true);
        }
        run0(false);
    }

    private void run0(boolean onlyRealtimeTasks) {

        int currentTokens = executionTokens.incrementAndGet();
        int currentTaskCount = taskCount.get();
        if (currentTokens != 1) return; // another thread is processing this context => gave it another token and exit

        // We now have an exclusive access to the scheduling context tasks,
        // decrementing executionTokens must be the last instruction of the loop
        // especially decrementing taskCount should be made during this critical session
        while (currentTokens > 0) {

            Runnable task;
            if (currentTaskCount <= 0 || (task = pollTask(onlyRealtimeTasks)) == null) {
                // no more task => try to release tokens, but refresh task count if we are given
                // an additionnal token
                if ((currentTokens = executionTokens.addAndGet(-currentTokens)) != 0) {
                    currentTaskCount = taskCount.get();
                }
                continue;
            }

            try {
                if (alive) task.run();
            } catch (final Exception e) {
                logger.warn("Exception while processing context {}", name, e);
            } finally {
                onlyRealtimeTasks = true; // more execution token can only be used on other realtime tasks
                currentTaskCount = taskCount.decrementAndGet();
                currentTokens = executionTokens.decrementAndGet();
            }
        }

        if (currentTaskCount > 0) {
            // potential benign race there : we might insert the context twice in the queue,
            // but it is supported
            scheduler.executeContext(this, false);
        }
    }

    private Runnable pollTask(final boolean onlyRealtimeTasks) {
        Runnable task = realTimeTasks.poll();
        if (task == null && !onlyRealtimeTasks) task = standardTasks.poll();
        if (task == null && !onlyRealtimeTasks) {
            final PeriodicTaskHolder pTask = nextPeriodicTask.getAndSet(null);
            if (pTask != null) {
                task = pTask.getTask();
                pTask.setTicketNumber(ticketGenerator.incrementAndGet());
            }

        }
        return task;
    }

    private void cancelScheduledTasks() {
        final Iterator<ScheduledFuture<?>> it = scheduledTasks.iterator();
        synchronized (it) {
            while (it.hasNext()) {
                it.next().cancel(false);
                it.remove();
            }
        }
    }

    static void rethrowUnchecked(final Throwable t) {
        SchedulingContextImpl.<RuntimeException> rethrow(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrow(final Throwable t) throws T {
        throw (T) t;
    }

    void tryRegisterPeriodicTask(final PeriodicTaskHolder candidate) {
        if (!alive) return;
        boolean race = true;
        while (race) {
            if (nextPeriodicTask.compareAndSet(null, candidate)) {
                // fast path : no pending scheduled task -> register as the next periodic one
                if (taskCount.getAndIncrement() == 0) {
                    scheduler.executeContext(this, false);
                }
                race = false;
            } else {
                // try to steal the slot of a newer ticket
                final PeriodicTaskHolder currentTask = nextPeriodicTask.get();
                race = currentTask == null || candidate.getTicketNumber() < currentTask.getTicketNumber()
                        && !nextPeriodicTask.compareAndSet(currentTask, candidate);
            }
        }
    }

    Scheduler getScheduler() {
        return scheduler;
    }

}
