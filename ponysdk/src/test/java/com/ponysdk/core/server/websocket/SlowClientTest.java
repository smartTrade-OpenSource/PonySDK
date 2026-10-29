/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.websocket;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;

/**
 * A client slow to read (its previous send still in flight) must not hold the thread that writes to it: flushes
 * wait in a short per-session queue, in order; only a client that stays stuck past the queue blocks, then times out.
 */
public class SlowClientTest {

    /** A session whose sends complete only when the test says so. */
    private static final class ManualSession {
        final Session session = Mockito.mock(Session.class);
        final List<Callback> pending = new ArrayList<>();
        final ByteArrayOutputStream wire = new ByteArrayOutputStream();
        int sends;

        ManualSession() {
            Mockito.when(session.isOpen()).thenReturn(true);
            Mockito.doAnswer(inv -> {
                final ByteBuffer buf = inv.getArgument(0);
                final byte[] chunk = new byte[buf.remaining()];
                buf.duplicate().get(chunk);
                synchronized (this) {
                    wire.writeBytes(chunk);
                    sends++;
                    pending.add(inv.getArgument(1));
                }
                return null;
            }).when(session).sendBinary(Mockito.any(ByteBuffer.class), Mockito.any(Callback.class));
        }

        void completeOne() {
            final Callback cb;
            synchronized (this) {
                cb = pending.remove(0);
            }
            cb.succeed();
        }

        synchronized int sends() {
            return sends;
        }
    }

    private static byte[] bytes(final int n, final int seed) {
        final byte[] b = new byte[n];
        for (int i = 0; i < n; i++) b[i] = (byte) (seed + i);
        return b;
    }

    @Test @Timeout(value = 5_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void aFlushWhileTheClientIsBusyDoesNotWait() throws IOException {
        final ManualSession s = new ManualSession();
        final WebSocketPusher p = new WebSocketPusher(s.session, 1 << 15, 2_000, new ByteBufferPool(1 << 12, 8));
        p.put(bytes(100, 1));
        p.flush(); // in flight, never completed yet

        final long t0 = System.nanoTime();
        p.put(bytes(100, 2));
        p.flush();
        assertTrue(System.nanoTime() - t0 < TimeUnit.MILLISECONDS.toNanos(500), "returned without waiting for the client");
        assertEquals(1, s.sends(), "queued, not sent");

        s.completeOne(); // the client caught up: the queued data goes out
        assertEquals(2, s.sends());
        final ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.writeBytes(bytes(100, 1));
        expected.writeBytes(bytes(100, 2));
        assertArrayEquals(expected.toByteArray(), s.wire.toByteArray());
    }

    @Test @Timeout(value = 5_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void aSendThatFailsWhileAFlushWaitsForRoomEndsThatFlushWithItsError() throws Exception {
        final ManualSession s = new ManualSession();
        final WebSocketPusher p = new WebSocketPusher(s.session, 1 << 15, 10_000, new ByteBufferPool(1 << 12, 8));
        for (int i = 0; i <= WebSocketPusher.QUEUE_LIMIT; i++) { // one in flight, then a full queue: 3 000 B do not fit together
            p.put(bytes(3000, i));
            p.flush();
        }
        assertEquals(1, s.sends());

        final AtomicReference<IOException> failure = new AtomicReference<>();
        final Thread writer = Thread.ofPlatform().start(() -> {
            try {
                p.put(bytes(3000, 9));
                p.flush();
            } catch (final IOException e) {
                failure.set(e);
            }
        });
        while (writer.getState() != Thread.State.TIMED_WAITING) Thread.sleep(1); // it waits for room in the queue
        final Callback inFlight;
        synchronized (s) {
            inFlight = s.pending.remove(0);
        }
        inFlight.fail(new IOException("reset"));
        writer.join();

        assertEquals("reset", failure.get().getCause().getMessage(), "the send's error, not a timeout");
        assertEquals(1, s.sends(), "nothing more sent to a gone connection");
    }

    @Test @Timeout(value = 5_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void withoutChunkSizeEachWriteLeavesAloneAndNothingEmptyIsSent() throws IOException {
        final ManualSession s = new ManualSession();
        final WebSocketPusher p = new WebSocketPusher(s.session, 0, 2_000, new ByteBufferPool(1 << 12, 8));

        p.put(bytes(3, 1)); // the chunk check flushes the fresh, empty buffer: nothing to send
        assertEquals(0, s.sends());
        p.put(bytes(3, 2)); // the first write leaves before the second
        assertEquals(1, s.sends());

        assertArrayEquals(bytes(3, 1), s.wire.toByteArray());
    }

    @Test @Timeout(value = 5_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void flushesMadeMeanwhileGoOutTogetherInOrder() throws IOException {
        final ManualSession s = new ManualSession();
        final WebSocketPusher p = new WebSocketPusher(s.session, 1 << 15, 2_000, new ByteBufferPool(1 << 12, 8));
        final ByteArrayOutputStream expected = new ByteArrayOutputStream();
        for (int i = 0; i < 6; i++) {
            final byte[] b = bytes(50, i * 10);
            expected.writeBytes(b);
            p.put(b);
            p.flush();
        }
        assertEquals(1, s.sends(), "one in flight, the 5 others wait");
        s.completeOne();
        assertEquals(2, s.sends(), "the 5 waiting flushes leave as one message");
        s.completeOne();
        assertArrayEquals(expected.toByteArray(), s.wire.toByteArray());
    }

    @Test @Timeout(value = 5_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void aClientStuckPastTheQueueTimesOutAsBefore() throws IOException {
        final ManualSession s = new ManualSession();
        final WebSocketPusher p = new WebSocketPusher(s.session, 1 << 10, 300, new ByteBufferPool(1 << 10, 16));
        final IOException e = assertThrows(IOException.class, () -> {
            for (int i = 0; i < 50; i++) { // full buffers: nothing merges, the queue fills up
                p.put(bytes(1000, i));
                p.flush();
            }
        });
        assertTrue(e.getMessage().contains("Timeout"), e.getMessage());
        assertTrue(p.isClosed());
    }

    @Test @Timeout(value = 20_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void sendsCompletingOnAnotherThreadKeepTheOrder() throws Exception {
        final Session session = Mockito.mock(Session.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        final ByteArrayOutputStream wire = new ByteArrayOutputStream();
        final ScheduledExecutorService network = Executors.newSingleThreadScheduledExecutor();
        Mockito.doAnswer(inv -> {
            final ByteBuffer buf = inv.getArgument(0);
            final byte[] chunk = new byte[buf.remaining()];
            buf.duplicate().get(chunk);
            synchronized (wire) {
                wire.writeBytes(chunk);
            }
            final Callback cb = inv.getArgument(1);
            network.schedule(cb::succeed, 200, TimeUnit.MICROSECONDS);
            return null;
        }).when(session).sendBinary(Mockito.any(ByteBuffer.class), Mockito.any(Callback.class));
        final WebSocketPusher p = new WebSocketPusher(session, 1 << 12, 5_000, new ByteBufferPool(1 << 12, 16));
        final ByteArrayOutputStream expected = new ByteArrayOutputStream();
        for (int i = 0; i < 2_000; i++) {
            final byte[] b = bytes(1 + i % 700, i);
            expected.writeBytes(b);
            p.put(b);
            if (i % 3 == 0) p.flush();
        }
        p.flush();
        for (int i = 0; i < 200 && wire.size() < expected.size(); i++) Thread.sleep(10);
        network.shutdown();
        synchronized (wire) {
            assertArrayEquals(expected.toByteArray(), wire.toByteArray());
        }
    }
}
