package com.ponysdk.core.server.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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

	private final ScheduledThreadPoolExecutor executor;
	private final ConcurrentLinkedQueue<SchedulingContextImpl> realtimeContexts = new ConcurrentLinkedQueue<>();
	// The contexts created here, for shutdown() to destroy the ones still in the executor queue: shutdownNow()
	// hands back the executor's own wrappers, not the contexts
	private final Set<SchedulingContextImpl> contexts = Collections
			.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

	public Scheduler(final int poolSize) {
		this(poolSize, Executors.defaultThreadFactory());
	}

	public Scheduler(final int poolSize, final ThreadFactory factory) {
		executor = new ScheduledThreadPoolExecutor(poolSize, factory);
	}

	/**
	 * Create a new {@link SchedulingContext} that can be used to submit new tasks
	 */
	public SchedulingContext createContext(final String name) {
		final SchedulingContextImpl context = new SchedulingContextImpl(name, this);
		contexts.add(context);
		return context;
	}

	/**
	 * Shutdown the scheduler, trying to cancel all pending tasks as fast as
	 * possible.<br>
	 */
	public void shutdown() {
		executor.shutdownNow();
		synchronized (contexts) {
			for (final SchedulingContextImpl context : contexts) {
				if (context.isQueued())
					context.destroy();
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

}
