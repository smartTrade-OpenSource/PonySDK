/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.websocket;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.Test;
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

    @Test(timeout = 5_000)
    public void aFlushWhileTheClientIsBusyDoesNotWait() throws IOException {
        final ManualSession s = new ManualSession();
        final WebSocketPusher p = new WebSocketPusher(s.session, 1 << 15, 2_000, new ByteBufferPool(1 << 12, 8));
        p.put(bytes(100, 1));
        p.flush(); // in flight, never completed yet

        final long t0 = System.nanoTime();
        p.put(bytes(100, 2));
        p.flush();
        assertTrue("returned without waiting for the client", System.nanoTime() - t0 < TimeUnit.MILLISECONDS.toNanos(500));
        assertEquals("queued, not sent", 1, s.sends());

        s.completeOne(); // the client caught up: the queued data goes out
        assertEquals(2, s.sends());
        final ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.writeBytes(bytes(100, 1));
        expected.writeBytes(bytes(100, 2));
        assertArrayEquals(expected.toByteArray(), s.wire.toByteArray());
    }

    @Test(timeout = 5_000)
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
        assertEquals("one in flight, the 5 others wait", 1, s.sends());
        s.completeOne();
        assertEquals("the 5 waiting flushes leave as one message", 2, s.sends());
        s.completeOne();
        assertArrayEquals(expected.toByteArray(), s.wire.toByteArray());
    }

    @Test(timeout = 5_000)
    public void aClientStuckPastTheQueueTimesOutAsBefore() throws IOException {
        final ManualSession s = new ManualSession();
        final WebSocketPusher p = new WebSocketPusher(s.session, 1 << 10, 300, new ByteBufferPool(1 << 10, 16));
        final IOException e = assertThrows(IOException.class, () -> {
            for (int i = 0; i < 50; i++) { // full buffers: nothing merges, the queue fills up
                p.put(bytes(1000, i));
                p.flush();
            }
        });
        assertTrue(e.getMessage(), e.getMessage().contains("Timeout"));
        assertTrue(p.isClosed());
    }

    @Test(timeout = 20_000)
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
