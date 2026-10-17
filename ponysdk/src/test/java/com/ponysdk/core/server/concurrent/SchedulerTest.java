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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

/**
 * Integration tests for {@link Scheduler} exercising the <strong>real</strong> {@code
 * ScheduledThreadPoolExecutor} (not a mock): task execution, FIFO ordering and context isolation,
 * delayed and periodic scheduling with cancellation, and orderly shutdown.
 *
 * <p>Synchronization uses latches with generous timeouts, so the test is deterministic without
 * depending on precise timing.
 */
public class SchedulerTest {

    private static final long TIMEOUT_S = 5;

    private final Scheduler scheduler = new Scheduler(4);

    @After
    public void tearDown() throws InterruptedException {
        scheduler.shutdown();
        scheduler.awaitTermination(TIMEOUT_S * 1000);
    }

    @Test
    public void testExecute_runsTaskOnThePool() throws InterruptedException {
        final SchedulingContext ctx = scheduler.createContext("ctx");
        final CountDownLatch latch = new CountDownLatch(1);

        ctx.execute(latch::countDown);

        assertTrue("submitted task must run on the scheduler pool", latch.await(TIMEOUT_S, SECONDS));
    }

    @Test
    public void testExecute_preservesFifoOrderWithinContext() throws InterruptedException {
        final SchedulingContext ctx = scheduler.createContext("ctx");
        final int n = 50;
        final List<Integer> order = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch latch = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            final int idx = i;
            ctx.execute(() -> {
                order.add(idx);
                latch.countDown();
            });
        }

        assertTrue(latch.await(TIMEOUT_S, SECONDS));
        final List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < n; i++) expected.add(i);
        assertEquals("tasks of a single context must run in strict FIFO order", expected, order);
    }

    @Test
    public void testExecuteAndWait_blocksUntilTaskCompletes() throws InterruptedException {
        final SchedulingContext ctx = scheduler.createContext("ctx");
        final AtomicBoolean ran = new AtomicBoolean(false);

        ctx.executeAndWait(() -> ran.set(true));

        assertTrue("executeAndWait must return only after the task has executed", ran.get());
    }

    @Test
    public void testExecuteLater_runsAfterDelay() throws InterruptedException {
        final SchedulingContext ctx = scheduler.createContext("ctx");
        final CountDownLatch latch = new CountDownLatch(1);

        ctx.executeLater(30, latch::countDown);

        assertTrue("delayed task must eventually run", latch.await(TIMEOUT_S, SECONDS));
    }

    @Test
    public void testExecuteLater_cancelledBeforeDelayDoesNotRun() throws InterruptedException {
        final SchedulingContext ctx = scheduler.createContext("ctx");
        final AtomicBoolean ran = new AtomicBoolean(false);

        final ScheduledTaskHandler handler = ctx.executeLater(1000, () -> ran.set(true));
        handler.cancel(false); // cancel well before the 1s delay elapses

        Thread.sleep(300);
        assertFalse("a cancelled delayed task must not run", ran.get());
    }

    @Test
    public void testSchedule_periodicRunsRepeatedlyThenStopsOnCancel() throws InterruptedException {
        final SchedulingContext ctx = scheduler.createContext("ctx");
        final AtomicInteger count = new AtomicInteger(0);

        final ScheduledTaskHandler handler = ctx.schedule(20, count::incrementAndGet);

        // Wait until it has ticked several times.
        final long deadline = System.nanoTime() + SECONDS.toNanos(TIMEOUT_S);
        while (count.get() < 3 && System.nanoTime() < deadline) Thread.sleep(10);
        assertTrue("periodic task must run repeatedly", count.get() >= 3);

        handler.cancel(false);
        Thread.sleep(100); // let any in-flight tick settle
        final int afterCancel = count.get();
        Thread.sleep(150);
        assertEquals("cancelled periodic task must stop ticking", afterCancel, count.get());
    }

    @Test
    public void testDestroyContext_stopsPeriodicTask() throws InterruptedException {
        final SchedulingContext ctx = scheduler.createContext("ctx");
        final AtomicInteger count = new AtomicInteger(0);

        ctx.schedule(20, count::incrementAndGet);

        final long deadline = System.nanoTime() + SECONDS.toNanos(TIMEOUT_S);
        while (count.get() < 2 && System.nanoTime() < deadline) Thread.sleep(10);
        assertTrue(count.get() >= 2);

        ctx.destroy();
        Thread.sleep(100);
        final int afterDestroy = count.get();
        Thread.sleep(150);
        assertEquals("destroying the context must cancel its periodic task", afterDestroy, count.get());
    }

    @Test
    public void testShutdownThenAwaitTerminationReturnsTrue() throws InterruptedException {
        scheduler.createContext("ctx").execute(() -> {});
        scheduler.shutdown();
        assertTrue("scheduler must terminate after shutdown", scheduler.awaitTermination(TIMEOUT_S * 1000));
    }
}
