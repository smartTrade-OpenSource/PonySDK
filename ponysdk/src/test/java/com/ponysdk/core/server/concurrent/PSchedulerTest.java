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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.ContextDestroyListener;
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

    /**
     * A task scheduled while its context is being destroyed (not alive, its destroy listeners not called yet) must
     * not make the context forget the tasks it already had: the destroy cancels them.
     */
    @Test
    public void aTaskScheduledDuringTheDestroyLeavesTheOthersToIt() throws ReflectiveOperationException, InterruptedException {
        final UIContext context = Mockito.mock(UIContext.class);
        Mockito.when(context.isAlive()).thenReturn(true);
        final int before = settledQueueSize();
        final UIRunnable first = PScheduler.schedule(context, () -> {}, Duration.ofHours(1));
        final ArgumentCaptor<ContextDestroyListener> destroy = ArgumentCaptor.forClass(ContextDestroyListener.class);
        Mockito.verify(context).addContextDestroyListener(destroy.capture());

        Mockito.when(context.isAlive()).thenReturn(false);
        final UIRunnable late = PScheduler.schedule(context, () -> {}, Duration.ofHours(1));
        late.cancel();
        destroy.getValue().onBeforeDestroy(context);

        assertEquals(before, timerQueueSize(), "the destroy cancels the first task");
        first.run();
        Mockito.verify(context, Mockito.never()).execute(Mockito.any());
    }

    /** A context destroyed before the destroy listener is registered (which it then ignores) leaves nothing behind. */
    @Test
    public void aTaskOfAContextDestroyedWhileItRegistersIsCancelled() throws ReflectiveOperationException, InterruptedException {
        final UIContext context = Mockito.mock(UIContext.class);
        Mockito.when(context.isAlive()).thenReturn(true);
        Mockito.doAnswer(inv -> Mockito.when(context.isAlive()).thenReturn(false)).when(context)
            .addContextDestroyListener(Mockito.any());
        final int before = settledQueueSize();

        final UIRunnable task = PScheduler.schedule(context, () -> {}, Duration.ofHours(1));

        assertEquals(before, timerQueueSize(), "the task leaves the timer");
        task.run();
        Mockito.verify(context, Mockito.never()).execute(Mockito.any());
        final UIRunnable next = PScheduler.schedule(context, () -> {}, Duration.ofHours(1));
        assertEquals(before, timerQueueSize(), "a dead context takes no task");
        next.run();
        Mockito.verify(context, Mockito.never()).execute(Mockito.any());
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
    public void aCancelledTaskLeavesTheTimerAtOnce() throws ReflectiveOperationException, InterruptedException {
        final UIContext context = Mockito.mock(UIContext.class);
        Mockito.when(context.isAlive()).thenReturn(true);
        final int before = settledQueueSize();

        final UIRunnable task = PScheduler.schedule(context, () -> {}, Duration.ofHours(1));
        assertEquals(before + 1, timerQueueSize());
        task.cancel();

        assertEquals(before, timerQueueSize());
    }

    /**
     * The shared timer's size once it stays the same for 50 ms. A repeated task of a previous test, cancelled between
     * its run and its requeue (the JDK's reExecutePeriodic), leaves at its next due time: read before, the size would
     * count it, and the test would see it go as its own task's doing.
     */
    private static int settledQueueSize() throws InterruptedException {
        int size = queueSize();
        for (int stable = 0; stable < 10; stable++) {
            Thread.sleep(5);
            final int now = queueSize();
            if (now != size) {
                size = now;
                stable = -1;
            }
        }
        return size;
    }

    private static int queueSize() {
        try {
            return timerQueueSize();
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @AfterEach
    public void tearDown() {
        UIContext.remove();
    }

    private static void await(final BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            assertTrue(System.nanoTime() < deadline, "not reached in 5 s");
            Thread.sleep(5);
        }
    }

    private static UIContext runningContext() {
        final UIContext context = Mockito.mock(UIContext.class);
        Mockito.when(context.isAlive()).thenReturn(true);
        Mockito.when(context.execute(Mockito.any())).thenAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return true;
        });
        return context;
    }

    @Test
    public void theCurrentContextRunsTheRepeatedTasksUntilCancelled() throws Exception {
        final CountDownLatch fixedRate = new CountDownLatch(2);
        final CountDownLatch delayedRate = new CountDownLatch(2);
        final CountDownLatch fixedDelay = new CountDownLatch(2);
        final UIContext context = runningContext();
        UIContext.setCurrent(context);
        final int before = settledQueueSize();

        final UIRunnable rate = PScheduler.scheduleAtFixedRate(fixedRate::countDown, Duration.ofMillis(1));
        final UIRunnable delayed = PScheduler.scheduleAtFixedRate(delayedRate::countDown, Duration.ZERO, Duration.ofMillis(1));
        final UIRunnable delay = PScheduler.scheduleWithFixedDelay(context, fixedDelay::countDown, Duration.ZERO,
            Duration.ofMillis(1));

        assertTrue(fixedRate.await(5, TimeUnit.SECONDS));
        assertTrue(delayedRate.await(5, TimeUnit.SECONDS));
        assertTrue(fixedDelay.await(5, TimeUnit.SECONDS));
        assertEquals(context, rate.getUIContext());
        rate.cancel();
        delayed.cancel();
        delay.cancel();
        // a task cancelled between its run and its requeue (the JDK's reExecutePeriodic) leaves at its next due time
        await(() -> queueSize() == before);
    }

    @Test
    public void aRepeatedTaskThatThrowsIsCancelled() throws Exception {
        final UIContext context = Mockito.mock(UIContext.class);
        Mockito.when(context.isAlive()).thenReturn(true);
        final CountDownLatch ran = new CountDownLatch(1);
        Mockito.when(context.execute(Mockito.any())).thenAnswer(invocation -> {
            ran.countDown();
            throw new IllegalStateException("boom");
        });
        final int before = settledQueueSize();

        PScheduler.scheduleAtFixedRate(context, () -> {}, Duration.ofMillis(1));
        assertTrue(ran.await(5, TimeUnit.SECONDS));

        await(() -> {
            try {
                return timerQueueSize() == before;
            } catch (final ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        });
        Mockito.verify(context, Mockito.times(1)).execute(Mockito.any());
    }

    /** A context alive when the task was scheduled but gone when it runs: its execute refuses, the task stops. */
    @Test
    public void aRepeatedTaskWhoseContextIsGoneWhenItRunsStops() throws Exception {
        final UIContext context = Mockito.mock(UIContext.class);
        Mockito.when(context.isAlive()).thenReturn(true);
        final CountDownLatch ran = new CountDownLatch(1);
        Mockito.when(context.execute(Mockito.any())).thenAnswer(invocation -> {
            ran.countDown();
            return false;
        });
        final int before = settledQueueSize();

        PScheduler.scheduleWithFixedDelay(context, () -> {}, Duration.ZERO, Duration.ofMillis(1));
        assertTrue(ran.await(5, TimeUnit.SECONDS));

        await(() -> {
            try {
                return timerQueueSize() == before;
            } catch (final ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        });
        Mockito.verify(context, Mockito.times(1)).execute(Mockito.any());
    }

    @Test
    public void aRepeatedTaskOfADeadContextNeverRuns() throws Exception {
        final UIContext context = Mockito.mock(UIContext.class);
        Mockito.when(context.isAlive()).thenReturn(false);
        final int before = settledQueueSize();

        final UIRunnable task = PScheduler.scheduleWithFixedDelay(context, () -> {}, Duration.ZERO, Duration.ofMillis(1));

        assertEquals(before, timerQueueSize(), "it leaves the timer at once");
        task.run();
        Mockito.verify(context, Mockito.never()).execute(Mockito.any());
        Mockito.verify(context, Mockito.never()).addContextDestroyListener(Mockito.any());
    }
}
