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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;

/**
 * Non-regression tests for {@link SchedulingContextImpl} — the per-context task queue + executor
 * loop of the {@link Scheduler}. The {@link Scheduler} is mocked so the context is driven
 * deterministically (no real thread pool): {@code executeContext} is a no-op and {@link
 * SchedulingContextImpl#run()} is invoked directly, or stubbed to run the context synchronously.
 */
public class SchedulingContextImplTest {

    private Scheduler scheduler;
    private SchedulingContextImpl ctx;

    @Before
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
        assertTrue("queued standard task must run when the context is processed", ran.get());
    }

    @Test
    public void testRun_executesQueuedRealtimeTask() {
        final AtomicBoolean ran = new AtomicBoolean(false);
        ctx.forceExecute(() -> ran.set(true));
        ctx.run();
        assertTrue("queued real-time task must run when the context is processed", ran.get());
    }

    @Test
    public void testDestroy_preventsEnqueueAndExecution() {
        ctx.destroy();
        final AtomicBoolean ran = new AtomicBoolean(false);
        ctx.execute(() -> ran.set(true));

        verify(scheduler, never()).executeContext(any(), anyBoolean());
        ctx.run();
        assertFalse("a destroyed context must not run newly submitted tasks", ran.get());
    }

    @Test
    public void testRun_doesNotRunTaskWhenContextDestroyedAfterEnqueue() {
        final AtomicBoolean ran = new AtomicBoolean(false);
        ctx.execute(() -> ran.set(true)); // enqueued while alive
        ctx.destroy(); // alive = false
        ctx.run(); // the alive guard in the run loop must skip the task
        assertFalse("task must be skipped once the context is destroyed", ran.get());
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
        assertTrue("executeAndWait must block until the task has run", ran.get());
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
        assertTrue("the original task exception must be rethrown", thrown == boom);
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
}
