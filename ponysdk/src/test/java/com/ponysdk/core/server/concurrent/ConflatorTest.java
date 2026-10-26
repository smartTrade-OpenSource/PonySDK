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

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;

/** ADR-0013: the latest value per key, applied on the context's thread, one drain at a time. */
public class ConflatorTest {

    private Application application;
    private UIContext context;
    /** The drains, run when the test says so. */
    private final ArrayDeque<Runnable> drains = new ArrayDeque<>();
    private final Executor manual = drains::add;
    private final List<String> applied = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setStringDictionaryEnabled(false);
        application = new Application("conflator", Mockito.mock(HttpSession.class), config);
        SessionManager.get().registerApplication(application);
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData()).thenReturn(new UpgradeRequestData(Map.of(), "test", Mockito.mock(HttpSession.class)));
        final TxnContext txnContext = new TxnContext(socket);
        txnContext.setApplication(application);
        context = new UIContext(socket, txnContext, config);
        application.registerUIContext(context);
    }

    @AfterEach
    public void tearDown() {
        SessionManager.get().unregisterApplication(application);
        UIContext.remove();
    }

    private Conflator<String, Integer> manual() {
        return Conflator.<String, Integer> builder(context, this::record).executor(manual).build();
    }

    private void record(final String key, final Integer value) {
        assertSame(context, UIContext.get(), "applied on the context's thread");
        synchronized (applied) {
            applied.add(key + "=" + value);
        }
    }

    private static void await(final BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            assertTrue(System.nanoTime() < deadline, "not reached in 5 s");
            Thread.sleep(5);
        }
    }

    @Test
    public void aDrainAppliesTheLatestValuePerKeyInTheOrderTheKeysCame() {
        final Conflator<String, Integer> quotes = manual();
        quotes.offer("EURUSD", 1);
        quotes.offer("USDJPY", 1);
        quotes.offer("EURUSD", 2);
        assertEquals(1, drains.size(), "one drain for the three values");

        drains.poll().run();

        assertEquals(List.of("EURUSD=2", "USDJPY=1"), applied);
        assertEquals(3, quotes.getOffered());
        assertEquals(2, quotes.getApplied());
        assertTrue(drains.isEmpty(), "nothing left: no other drain");
    }

    @Test
    public void aValueOfferedDuringADrainGoesToTheNextOne() {
        final Conflator<String, Integer> quotes = Conflator.<String, Integer> builder(context, (key, value) -> {
            record(key, value);
            if (value == 1) assertTrue(isOffered(key, 2), "an offer while draining does not wait");
        }).executor(manual).build();
        current = quotes;
        quotes.offer("EURUSD", 1);
        drains.poll().run();

        assertEquals(1, drains.size(), "the value offered while draining has its drain");
        drains.poll().run();
        assertEquals(List.of("EURUSD=1", "EURUSD=2"), applied);
    }

    private Conflator<String, Integer> current;

    private boolean isOffered(final String key, final int value) {
        return current == null || current.offer(key, value);
    }

    @Test
    public void anOfferNeverWaitsForADrainBlockedByItsClient() throws InterruptedException {
        final CountDownLatch slowClient = new CountDownLatch(1);
        final CountDownLatch draining = new CountDownLatch(1);
        final Map<String, Integer> shown = new ConcurrentHashMap<>();
        final Conflator<String, Integer> quotes = Conflator.of(context, (key, value) -> {
            draining.countDown();
            try {
                slowClient.await(); // the drain waits as it would in a flush to a client that does not read
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            shown.put(key, value);
        });
        quotes.offer("k0", -1);
        assertTrue(draining.await(5, TimeUnit.SECONDS), "the first drain holds the context, blocked");

        final long start = System.nanoTime();
        for (int i = 0; i < 100_000; i++) quotes.offer("k" + i % 10, i);
        final long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsedMs < 2_000, "100 000 offers took " + elapsedMs + " ms while the drain waited");

        slowClient.countDown();
        await(() -> quotes.getApplied() == 11);
        assertEquals(100_001, quotes.getOffered());
        for (int k = 0; k < 10; k++) assertEquals(99_990 + k, shown.get("k" + k), "the latest of k" + k);
    }

    @Test
    public void minIntervalSpacesTheDrains() throws InterruptedException {
        final List<Long> drainedAt = new ArrayList<>();
        final Conflator<String, Integer> quotes = Conflator.<String, Integer> builder(context, (key, value) -> {
            synchronized (drainedAt) {
                drainedAt.add(System.nanoTime());
            }
        }).minInterval(Duration.ofMillis(200)).build();
        quotes.offer("EURUSD", 1);
        await(() -> quotes.getApplied() == 1);
        quotes.offer("EURUSD", 2);
        await(() -> quotes.getApplied() == 2);

        synchronized (drainedAt) {
            final long gapMs = TimeUnit.NANOSECONDS.toMillis(drainedAt.get(1) - drainedAt.get(0));
            assertTrue(gapMs >= 180, "drains " + gapMs + " ms apart");
        }
    }

    @Test
    public void nothingIsAppliedWhileTheContextIsSuspendedThenTheLatest() throws InterruptedException {
        final UIContext suspendable = Mockito.spy(context);
        Mockito.doReturn(true).when(suspendable).isSuspended();
        final Map<String, Integer> shown = new ConcurrentHashMap<>();
        final Conflator<String, Integer> quotes = Conflator.of(suspendable, shown::put);
        for (int i = 0; i < 1_000; i++) quotes.offer("EURUSD", i);
        Thread.sleep(300);
        assertEquals(0, quotes.getApplied(), "suspended: the values wait");

        Mockito.doReturn(false).when(suspendable).isSuspended();
        await(() -> quotes.getApplied() == 1);
        assertEquals(999, shown.get("EURUSD"));
    }

    @Test
    public void theContextsDestroyClosesIt() {
        final Conflator<String, Integer> quotes = manual();
        context.destroy();
        assertTrue(quotes.isClosed());
        assertFalse(quotes.offer("EURUSD", 1), "an offer after the close is dropped");
        assertTrue(drains.isEmpty());
    }

    @Test
    public void closeDropsWhatIsPending() {
        final Conflator<String, Integer> quotes = manual();
        quotes.offer("EURUSD", 1);
        quotes.close();
        drains.poll().run();
        assertEquals(List.of(), applied);
        assertEquals(0, quotes.getApplied());
    }

    @Test
    public void aValueThatFailsLeavesTheOthersApplied() {
        final Conflator<String, Integer> quotes = Conflator.<String, Integer> builder(context, (key, value) -> {
            if (key.equals("BAD")) throw new IllegalStateException("no row for " + key);
            record(key, value);
        }).executor(manual).build();
        quotes.offer("BAD", 1);
        quotes.offer("EURUSD", 1);
        drains.poll().run();
        quotes.offer("EURUSD", 2);
        drains.poll().run();
        assertEquals(List.of("EURUSD=1", "EURUSD=2"), applied);
    }

    @Test
    public void aPublisherFeedsItAndItsCompletionClosesIt() throws InterruptedException {
        final Map<String, Integer> shown = new ConcurrentHashMap<>();
        final Conflator<String, Integer> quotes = Conflator.of(context, shown::put);
        try (SubmissionPublisher<Integer> feed = new SubmissionPublisher<>()) {
            feed.subscribe(quotes.subscriber(i -> "k" + i % 3));
            for (int i = 0; i < 300; i++) feed.submit(i);
            await(() -> Integer.valueOf(299).equals(shown.get("k2")) && Integer.valueOf(298).equals(shown.get("k1")));
            assertEquals(297, shown.get("k0"));
        }
        await(quotes::isClosed);
    }

    @Test
    public void closingItCancelsItsSubscription() throws InterruptedException {
        final Conflator<String, Integer> quotes = manual();
        try (SubmissionPublisher<Integer> feed = new SubmissionPublisher<>()) {
            feed.subscribe(quotes.subscriber(String::valueOf));
            await(() -> feed.getNumberOfSubscribers() == 1);
            quotes.close();
            feed.submit(1); // a cancelled subscription is removed by the publisher's next item
            await(() -> feed.getNumberOfSubscribers() == 0);
        }
    }
}
