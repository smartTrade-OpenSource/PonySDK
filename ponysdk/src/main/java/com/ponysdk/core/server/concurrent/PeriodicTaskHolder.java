package com.ponysdk.core.server.concurrent;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

//Wrap the real runnable of a scheduled future task so that the ScheduledFuture provided by the executor is
//strongly referenced until executed. Needed to forbid SchedulingContextImpl.scheduledTasks to drop weak references.
//Provides cancellation on the main future when the periodic tasks throws an exception
class PeriodicTaskHolder {

	private final Runnable task;
	private final SchedulingContextImpl context;
	private long ticketNumber = 0; // ensure fairness between delayed periodicTasks
	// Dekker pair: a failure before the future is known is seen by schedulePeriodicTask, otherwise by the
	// failing task itself
	private volatile ScheduledFuture<?> future;
	private volatile boolean failed;

	static PeriodicTaskHolder schedulePeriodicTask(final SchedulingContextImpl context, final long periodInMillis,
			final Runnable task) {
		final PeriodicTaskHolder pTask = new PeriodicTaskHolder(context, task);
		final ScheduledFuture<?> future = context.getScheduler().schedulePeriodicTask(periodInMillis, context,
				pTask::run);
		pTask.future = future;
		if (pTask.failed)
			future.cancel(true);
		return pTask;
	}

	private PeriodicTaskHolder(final SchedulingContextImpl context, final Runnable task) {
		Objects.requireNonNull(task);
		this.context = context;
		this.task = () -> executeOrCancel(task);
	}

	long getTicketNumber() {
		return ticketNumber;
	}

	void setTicketNumber(final long ticketNumber) {
		this.ticketNumber = ticketNumber;
	}

	Runnable getTask() {
		return task;
	}

	ScheduledFuture<?> getFuture() {
		return future;
	}

	private void run() {
		context.tryRegisterPeriodicTask(this);
	}

	private void executeOrCancel(final Runnable task) {
		try {
			task.run();
		} catch (final Throwable t) {
			failed = true;
			final ScheduledFuture<?> f = future;
			if (f != null)
				f.cancel(true);
			throw t; // precise rethrow: run() throws nothing checked
		}
	}
}