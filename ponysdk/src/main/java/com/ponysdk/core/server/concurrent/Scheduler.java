package com.ponysdk.core.server.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

	private final ScheduledThreadPoolExecutor executor;
	private final ConcurrentLinkedQueue<SchedulingContextImpl> realtimeContexts = new ConcurrentLinkedQueue<>();

	public Scheduler(final int poolSize) {
		this(poolSize, Executors.defaultThreadFactory());
	}

	public Scheduler(final int poolSize, final ThreadFactory factory) {
		executor = new ContextAwareExecutor(poolSize, factory);
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
			if (r instanceof QueuedContext queued) {
				queued.context.destroy();
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

	ScheduledFuture<?> schedulePeriodicTask(final long periodMillis, final SchedulingContextImpl context,
			final Runnable task) {
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
			if (realtime)
				realtimeContexts.offer(context);
		} catch (final RejectedExecutionException e) {
			context.destroy();
		}
	}

	SchedulingContextImpl pollRealtimeContext() {
		return realtimeContexts.poll();
	}

	// The executor queues its own wrappers, so shutdownNow() cannot hand the contexts back unless the
	// wrapper keeps them
	private static final class ContextAwareExecutor extends ScheduledThreadPoolExecutor {

		ContextAwareExecutor(final int poolSize, final ThreadFactory factory) {
			super(poolSize, factory);
		}

		@Override
		protected <V> RunnableScheduledFuture<V> decorateTask(final Runnable runnable,
				final RunnableScheduledFuture<V> task) {
			return runnable instanceof SchedulingContextImpl context ? new QueuedContext<>(context, task) : task;
		}
	}

	private record QueuedContext<V>(SchedulingContextImpl context, RunnableScheduledFuture<V> task)
			implements RunnableScheduledFuture<V> {

		@Override
		public void run() {
			task.run();
		}

		@Override
		public boolean isPeriodic() {
			return false;
		}

		@Override
		public long getDelay(final TimeUnit unit) {
			return task.getDelay(unit);
		}

		@Override
		public int compareTo(final Delayed other) {
			return task.compareTo(other instanceof QueuedContext<?> queued ? queued.task : other);
		}

		@Override
		public boolean cancel(final boolean mayInterruptIfRunning) {
			return task.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled() {
			return task.isCancelled();
		}

		@Override
		public boolean isDone() {
			return task.isDone();
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			return task.get();
		}

		@Override
		public V get(final long timeout, final TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			return task.get(timeout, unit);
		}
	}

}
