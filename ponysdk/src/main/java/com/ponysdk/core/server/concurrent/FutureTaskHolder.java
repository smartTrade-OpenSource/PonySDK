
package com.ponysdk.core.server.concurrent;

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

//Wrap the real runnable of a scheduled future task so that the ScheduledFuture provided by the executor is
//strongly referenced until executed. Needed to forbid SchedulingContextImpl.scheduledTasks to drop weak references
class FutureTaskHolder {

    static FutureTaskHolder scheduleFutureTask(final SchedulingContextImpl context, final long delayMillis, final Runnable task) {
        final FutureTaskHolder holder = new FutureTaskHolder(task);
        holder.future = context.getScheduler().executeLater(delayMillis, context, holder::run);
        return holder;
    }

    private final Runnable task;
    private ScheduledFuture<?> future;

    private FutureTaskHolder(final Runnable task) {
        Objects.requireNonNull(task);
        this.task = task;
    }

    private void run() {
        task.run();
    }

    ScheduledFuture<?> getFuture() {
        return future;
    }
}