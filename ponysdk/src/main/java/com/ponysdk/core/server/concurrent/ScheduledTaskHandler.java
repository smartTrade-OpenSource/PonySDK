
package com.ponysdk.core.server.concurrent;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledFuture;

/**
 * A wrapper around the ScheduledFuture owned by the Scheduler internal
 * executor.<br>
 * This wrapper only allows cancelling a future or periodic task.<br>
 *
 * @implNote this wrapper use weak reference to make sure that the task
 *           submitted to the executor cannot be source of a memory leak once the task is
 *           executed or cancelled
 * @author amaire
 *
 */

public class ScheduledTaskHandler {

    private final WeakReference<ScheduledFuture<?>> futureRef;

    ScheduledTaskHandler(final ScheduledFuture<?> future) {
        futureRef = new WeakReference<>(future);
    }

    public void cancel(final boolean mayInterruptIfRunning) {
        final ScheduledFuture<?> future = futureRef.get();
        if (future != null) {
            future.cancel(mayInterruptIfRunning);
            futureRef.clear();
        }
    }

}
