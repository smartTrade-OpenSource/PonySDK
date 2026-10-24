/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.server.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler.UIRunnable;

/**
 * Regression tests for the {@link PScheduler.UIRunnable} scheduling race.
 * <p>
 * A 0-delay task can start running (and be cancelled) before the scheduling call has set its
 * future: {@code uiRunnable.setFuture(executor.schedule(uiRunnable, ...))} sets the future only
 * after the executor has already accepted — and possibly started — the task. Previously
 * {@code onCancel()} did {@code this.future.cancel(false)} unconditionally, throwing an NPE when
 * the future was still {@code null}; and a cancel that landed before {@code setFuture} was lost,
 * leaving the (repeating) future running.
 */
public class PSchedulerTest {

    @Test
    public void onCancelBeforeFutureIsSetDoesNotThrow() {
        final UIRunnable runnable = new UIRunnable(null, null, () -> {}, false);
        // No future has been assigned yet — cancelling must not NPE.
        runnable.onCancel();
    }

    @Test
    public void cancelBeforeSetFutureStillCancelsTheLateFuture() {
        final UIRunnable runnable = new UIRunnable(null, null, () -> {}, true);
        runnable.onCancel(); // cancelled while future is still null

        @SuppressWarnings("unchecked")
        final ScheduledFuture<Object> future = Mockito.mock(ScheduledFuture.class);
        runnable.setFuture(future); // the future arrives late

        // It must be cancelled, otherwise a repeating task would keep running after a cancel.
        Mockito.verify(future).cancel(false);
    }

    /** Tasks waiting in the shared timer (all sessions). */
    private static int timerQueueSize() throws ReflectiveOperationException {
        final Field instance = PScheduler.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        final Field executor = PScheduler.class.getDeclaredField("executor");
        executor.setAccessible(true);
        return ((ScheduledThreadPoolExecutor) executor.get(instance.get(null))).getQueue().size();
    }

    /**
     * A cancelled task leaves the shared timer at once: otherwise it waits there until its due time, and
     * purging it means copying and scanning the whole queue (every session's tasks) on each session close.
     */
    @Test
    public void aCancelledTaskLeavesTheTimerAtOnce() throws ReflectiveOperationException {
        final UIContext context = Mockito.mock(UIContext.class);
        Mockito.when(context.isAlive()).thenReturn(true);
        final int before = timerQueueSize();

        final UIRunnable task = PScheduler.schedule(context, () -> {}, Duration.ofHours(1));
        assertEquals(before + 1, timerQueueSize());
        task.cancel();

        assertEquals(before, timerQueueSize());
    }
}
