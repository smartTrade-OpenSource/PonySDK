/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ponysdk.core.server.concurrent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Non-regression tests for {@link SchedulingContextImpl} — the per-context task queue + executor
 * loop of the {@link Scheduler}. The {@link Scheduler} is mocked so the context is driven
 * deterministically (no real thread pool): {@code executeContext} is a no-op and {@link
 * SchedulingContextImpl#run()} is invoked directly, or stubbed to run the context synchronously.
 */
public class SchedulingContextImplTest {

    private Scheduler scheduler;
    private SchedulingContextImpl ctx;

    @BeforeEach
    public void setUp() {
        scheduler = mock(Scheduler.class);
        when(scheduler.isShutdown()).thenReturn(false);
        when(scheduler.pollRealtimeContext()).thenReturn(null);
        ctx = new SchedulingContextImpl("test", scheduler);
    }

    @Test
    public void testExecute_notifiesSchedulerOnFirstTaskOnly() {
        ctx.execute(() -> {});
        ctx.execute(() -> {});
        // The context is enqueued in the scheduler only when it transitions from 0 to 1 pending task.
        verify(scheduler, times(1)).executeContext(ctx, false);
    }

    @Test
    public void testForceExecute_notifiesSchedulerAsRealtime() {
        ctx.forceExecute(() -> {});
        verify(scheduler).executeContext(ctx, true);
    }

    @Test
    public void testRun_executesQueuedStandardTask() {
        final AtomicBoolean ran = new AtomicBoolean(false);
        ctx.execute(() -> ran.set(true)); // executeContext is a no-op mock here
        ctx.run();
        assertTrue(ran.get(), "queued standard task must run when the context is processed");
    }

    @Test
    public void testRun_executesQueuedRealtimeTask() {
        final AtomicBoolean ran = new AtomicBoolean(false);
        ctx.forceExecute(() -> ran.set(true));
        ctx.run();
        assertTrue(ran.get(), "queued real-time task must run when the context is processed");
    }

    @Test
    public void testDestroy_preventsEnqueueAndExecution() {
        ctx.destroy();
        final AtomicBoolean ran = new AtomicBoolean(false);
        ctx.execute(() -> ran.set(true));

        verify(scheduler, never()).executeContext(any(), anyBoolean());
        ctx.run();
        assertFalse(ran.get(), "a destroyed context must not run newly submitted tasks");
    }

    @Test
    public void testRun_doesNotRunTaskWhenContextDestroyedAfterEnqueue() {
        final AtomicBoolean ran = new AtomicBoolean(false);
        ctx.execute(() -> ran.set(true)); // enqueued while alive
        ctx.destroy(); // alive = false
        ctx.run(); // the alive guard in the run loop must skip the task
        assertFalse(ran.get(), "task must be skipped once the context is destroyed");
    }

    @Test
    public void testExecuteAndWait_runsTaskSynchronously() throws InterruptedException {
        // Simulate the executor: running the context synchronously when it is enqueued.
        doAnswer(inv -> {
            ctx.run();
            return null;
        }).when(scheduler).executeContext(eq(ctx), anyBoolean());

        final AtomicBoolean ran = new AtomicBoolean(false);
        ctx.executeAndWait(() -> ran.set(true));
        assertTrue(ran.get(), "executeAndWait must block until the task has run");
    }

    @Test
    public void testExecuteAndWait_propagatesTaskException() {
        doAnswer(inv -> {
            ctx.run();
            return null;
        }).when(scheduler).executeContext(eq(ctx), anyBoolean());

        final RuntimeException boom = new IllegalStateException("boom");
        final RuntimeException thrown = assertThrows(RuntimeException.class, () -> ctx.executeAndWait(() -> {
            throw boom;
        }));
        assertTrue(thrown == boom, "the original task exception must be rethrown");
    }

    @Test
    public void testExecuteLater_returnsHandlerThatCancelsTheFuture() {
        final ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(scheduler).executeLater(anyLong(), any(), any());

        final ScheduledTaskHandler handler = ctx.executeLater(100, () -> {});
        handler.cancel(true);

        verify(future).cancel(true);
    }

    @Test
    public void testSchedule_returnsHandlerThatCancelsTheFuture() {
        final ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(scheduler).schedulePeriodicTask(anyLong(), any(), any());

        final ScheduledTaskHandler handler = ctx.schedule(50, () -> {});
        handler.cancel(false);

        verify(future).cancel(false);
    }

    @Test
    public void testDestroy_cancelsScheduledFutures() {
        final ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(scheduler).executeLater(anyLong(), any(), any());
        ctx.executeLater(100, () -> {});

        ctx.destroy();

        verify(future).cancel(false);
    }

    @Test
    public void testExecuteLater_afterDestroy_cancelsFutureImmediately() {
        ctx.destroy();
        final ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(scheduler).executeLater(anyLong(), any(), any());

        ctx.executeLater(100, () -> {});

        // toHandler must cancel a future scheduled onto an already-destroyed context (Dekker guard).
        verify(future).cancel(false);
    }

    @Test
    public void testForceExecuteAndWait_runsTaskAsRealtime() throws InterruptedException {
        doAnswer(inv -> {
            ctx.run();
            return null;
        }).when(scheduler).executeContext(eq(ctx), anyBoolean());

        final AtomicBoolean ran = new AtomicBoolean(false);
        ctx.forceExecuteAndWait(() -> ran.set(true));

        assertTrue(ran.get());
        verify(scheduler).executeContext(ctx, true);
    }

    @Test
    public void testExecuteAndWait_returnsAtOnceOnADestroyedContext() throws InterruptedException {
        ctx.destroy();
        final AtomicBoolean ran = new AtomicBoolean(false);

        ctx.executeAndWait(() -> ran.set(true));

        assertFalse(ran.get());
        verify(scheduler, never()).executeContext(any(), anyBoolean());
    }

    @Test
    public void testExecuteAndWait_returnsWhenTheContextIsDestroyedMeanwhile() throws InterruptedException {
        doAnswer(inv -> {
            ctx.destroy();
            return null;
        }).when(scheduler).executeContext(eq(ctx), anyBoolean());
        final AtomicBoolean ran = new AtomicBoolean(false);

        ctx.executeAndWait(() -> ran.set(true));

        assertFalse(ran.get(), "a task of a context destroyed before it ran is dropped, the caller released");
    }

    @Test
    public void testRun_aFailingTaskDoesNotStopTheNextOnes() {
        final List<String> ran = new ArrayList<>();
        ctx.execute(() -> {
            throw new IllegalStateException("boom");
        });
        ctx.forceExecute(() -> ran.add("realtime"));
        ctx.execute(() -> ran.add("standard"));

        ctx.run();
        ctx.run();
        ctx.run();

        assertEquals(List.of("realtime", "standard"), ran);
    }

    @Test
    public void testRun_stealsTheRealtimeTasksOfAnotherContextFirst() {
        final SchedulingContextImpl other = new SchedulingContextImpl("other", scheduler);
        final List<String> ran = new ArrayList<>();
        other.forceExecute(() -> ran.add("other realtime"));
        ctx.execute(() -> ran.add("own standard"));
        when(scheduler.pollRealtimeContext()).thenReturn(other, ctx, null);

        ctx.run();

        assertEquals(List.of("other realtime", "own standard"), ran);
    }

    @Test
    public void testRun_withOwnRealtimeTaskDoesNotSteal() {
        ctx.forceExecute(() -> {});

        ctx.run();

        verify(scheduler, never()).pollRealtimeContext();
    }

    @Test
    public void testRun_requeuesTheContextWhenOnlyStandardTasksRemain() {
        final List<String> ran = new ArrayList<>();
        ctx.execute(() -> ran.add("first"));
        ctx.execute(() -> ran.add("second"));

        ctx.run();

        assertEquals(List.of("first"), ran, "one standard task per scheduling, for fairness among contexts");
        verify(scheduler, times(2)).executeContext(ctx, false);
        ctx.run();
        assertEquals(List.of("first", "second"), ran);
    }

    @Test
    public void testRun_aContextBusyOnAnotherThreadIsNotRunTwice() throws InterruptedException {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final List<String> ran = new CopyOnWriteArrayList<>();
        ctx.execute(() -> {
            started.countDown();
            await(release);
            ran.add("first");
        });
        ctx.execute(() -> ran.add("second"));
        final Thread busy = new Thread(ctx::run);
        busy.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));

        ctx.run(); // the context is being run: this one only leaves it a token

        assertEquals(List.of(), ran);
        release.countDown();
        busy.join(5_000);
        assertEquals(List.of("first"), ran, "the extra token serves realtime tasks only");
        verify(scheduler, times(2)).executeContext(ctx, false);
        ctx.run();
        assertEquals(List.of("first", "second"), ran);
    }

    @Test
    public void testExecuteAndWait_keepsWaitingPastItsOneSecondPoll() throws InterruptedException {
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            // The context runs after the first one-second poll of the waiting caller timed out
            doAnswer(inv -> executor.schedule(ctx::run, 1_200, TimeUnit.MILLISECONDS)).when(scheduler)
                .executeContext(eq(ctx), anyBoolean());
            final AtomicBoolean ran = new AtomicBoolean(false);

            ctx.executeAndWait(() -> ran.set(true));

            assertTrue(ran.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testAwaitDestruction_anInterruptedWaitThrows() {
        Thread.currentThread().interrupt();

        assertThrows(InterruptedException.class, () -> ctx.awaitDestruction(5_000));
        assertFalse(Thread.currentThread().isInterrupted(), "the interrupt is reported by the exception");
    }

    @Test
    public void testAShutdownSchedulerReleasesTheWaitersAndDestroysTheContext() throws InterruptedException {
        when(scheduler.isShutdown()).thenReturn(true);
        when(scheduler.awaitTermination(anyLong())).thenReturn(true);
        final AtomicBoolean ran = new AtomicBoolean(false);

        ctx.executeAndWait(() -> ran.set(true));
        assertFalse(ran.get(), "nothing runs the context any more");

        assertTrue(ctx.awaitDestruction(1_000));
        assertFalse(ctx.isAlive());
    }

    private static void await(final CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Runnable schedulePeriodic(final Runnable task, final ScheduledFuture<?> future) {
        final ArgumentCaptor<Runnable> tick = ArgumentCaptor.forClass(Runnable.class);
        doReturn(future).when(scheduler).schedulePeriodicTask(anyLong(), eq(ctx), tick.capture());
        ctx.schedule(10, task);
        return tick.getValue();
    }

    @Test
    public void testSchedule_eachTickRunsThePeriodicTaskOnTheContext() {
        final List<String> ran = new ArrayList<>();
        final Runnable tick = schedulePeriodic(() -> ran.add("tick"), mock(ScheduledFuture.class));

        tick.run();
        verify(scheduler).executeContext(ctx, false);
        ctx.run();
        tick.run();
        ctx.run();

        assertEquals(List.of("tick", "tick"), ran);
    }

    @Test
    public void testSchedule_aFailingPeriodicTaskCancelsItsFuture() {
        final ScheduledFuture<?> future = mock(ScheduledFuture.class);
        final Runnable tick = schedulePeriodic(() -> {
            throw new IllegalStateException("boom");
        }, future);

        tick.run();
        ctx.run();

        verify(future).cancel(true);
    }

    @Test
    public void testSchedule_aTickOfADestroyedContextIsIgnored() {
        final Runnable tick = schedulePeriodic(() -> {}, mock(ScheduledFuture.class));
        ctx.destroy();

        tick.run();

        verify(scheduler, never()).executeContext(any(), anyBoolean());
    }

    @Test
    public void testSchedule_theOlderPeriodicTaskTakesTheSlotOfANewerOne() {
        final List<String> ran = new ArrayList<>();
        final Runnable older = schedulePeriodic(() -> ran.add("older"), mock(ScheduledFuture.class));
        final Runnable newer = schedulePeriodic(() -> ran.add("newer"), mock(ScheduledFuture.class));

        older.run();
        ctx.run(); // older gets ticket 1
        newer.run();
        ctx.run(); // newer gets ticket 2
        newer.run(); // newer waits in the slot
        older.run(); // ticket 1 < 2: older steals the slot
        ctx.run(); // older gets ticket 3
        older.run();
        newer.run(); // ticket 2 < 3: newer steals the slot back
        ctx.run();

        assertEquals(List.of("older", "newer", "older", "newer"), ran);
    }

    @Test
    public void testSchedule_aNewerPeriodicTaskLeavesTheSlotToAnOlderOne() {
        final List<String> ran = new ArrayList<>();
        final Runnable older = schedulePeriodic(() -> ran.add("older"), mock(ScheduledFuture.class));
        final Runnable newer = schedulePeriodic(() -> ran.add("newer"), mock(ScheduledFuture.class));
        older.run();
        ctx.run(); // older gets ticket 1
        newer.run();
        ctx.run(); // newer gets ticket 2

        older.run(); // older waits in the slot
        newer.run(); // ticket 2 > 1: the slot stays to older
        older.run(); // already in the slot
        ctx.run();
        ctx.run();

        assertEquals(List.of("older", "newer", "older"), ran);
        verify(scheduler, times(3)).executeContext(ctx, false);
    }

    @Test
    public void testSchedule_aTickOfAContextWithPendingTasksDoesNotQueueItAgain() {
        final List<String> ran = new ArrayList<>();
        final Runnable tick = schedulePeriodic(() -> ran.add("tick"), mock(ScheduledFuture.class));
        ctx.execute(() -> ran.add("task"));

        tick.run();

        verify(scheduler).executeContext(ctx, false);
        ctx.run();
        ctx.run();
        assertEquals(List.of("task", "tick"), ran);
    }

    @Test
    public void testSchedule_aPeriodicTaskFailingBeforeItsFutureIsKnownIsCancelled() {
        final ScheduledFuture<?> future = mock(ScheduledFuture.class);
        // The executor ticks and the context runs the failing task before schedulePeriodicTask returns
        doAnswer(inv -> {
            inv.getArgument(2, Runnable.class).run();
            ctx.run();
            verify(future, never()).cancel(anyBoolean());
            return future;
        }).when(scheduler).schedulePeriodicTask(anyLong(), eq(ctx), any());

        ctx.schedule(10, () -> {
            throw new IllegalStateException("boom");
        });

        verify(future).cancel(true);
    }

    @Test
    public void testAwaitDestruction_waitsForTheTasksPendingInADestroyedContext() throws InterruptedException {
        ctx.execute(() -> {});
        ctx.destroy();

        assertFalse(ctx.awaitDestruction(0), "the pending task is not consumed yet");
        ctx.run();
        assertTrue(ctx.awaitDestruction(0));
    }
}
