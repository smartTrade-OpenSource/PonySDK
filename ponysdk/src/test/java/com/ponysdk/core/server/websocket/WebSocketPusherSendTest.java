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

package com.ponysdk.core.server.websocket;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.core.WebSocketConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ArrayValueModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.ValueTypeModel;
import com.ponysdk.core.server.LogLevel;
import com.ponysdk.core.server.application.StringDictionary;
import com.ponysdk.core.server.application.UIContext;

/**
 * {@link WebSocketPusher}'s sends and buffers: the queue behind a send in flight, its bound, a value larger than a
 * pooled buffer, the byte counters reported to a listener, and the stats recording.
 */
public class WebSocketPusherSendTest {

    /** A session that completes a send only when the test says so. */
    private static final class ManualSession {

        final Session session = Mockito.mock(Session.class);
        final List<byte[]> sent = new ArrayList<>();
        final List<Callback> callbacks = new ArrayList<>();

        ManualSession() {
            Mockito.when(session.isOpen()).thenReturn(true);
            Mockito.doAnswer(inv -> {
                final ByteBuffer buffer = inv.getArgument(0);
                final byte[] bytes = new byte[buffer.remaining()];
                buffer.duplicate().get(bytes);
                sent.add(bytes);
                callbacks.add(inv.getArgument(1));
                return null;
            }).when(session).sendBinary(Mockito.any(ByteBuffer.class), Mockito.any(Callback.class));
        }

        void complete(final int index) {
            callbacks.get(index).succeed();
        }
    }

    @AfterEach
    public void tearDown() {
        WebSocketPusher.stopRecordingStats();
    }

    private static byte[] longFrame(final long value) {
        return ByteBuffer.allocate(9).put((byte) ServerToClientModel.ROUNDTRIP_LATENCY.getValue()).putLong(value).array();
    }

    private static byte[] concat(final byte[]... parts) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (final byte[] part : parts) out.writeBytes(part);
        return out.toByteArray();
    }

    @Test
    public void flushesDuringASendWaitAndSmallOnesLeaveAsOneMessage() throws IOException {
        final ManualSession client = new ManualSession();
        final ByteBufferPool pool = new ByteBufferPool(64, 8);
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, pool);

        for (long i = 1; i <= 3; i++) {
            pusher.encode(ServerToClientModel.ROUNDTRIP_LATENCY, i);
            pusher.flush();
        }
        assertEquals(1, client.sent.size(), "the second and third flushes wait for the first send");

        client.complete(0);

        assertEquals(2, client.sent.size());
        assertArrayEquals(longFrame(1), client.sent.get(0));
        assertArrayEquals(concat(longFrame(2), longFrame(3)), client.sent.get(1), "appended into one message");
        client.complete(1);
        assertEquals(3, pool.availableCount(), "every buffer back to the pool");
    }

    /** A pusher whose pooled buffers hold a single 9-byte frame: no two flushes share a buffer. */
    private static WebSocketPusher stuckClient(final ManualSession client, final long timeoutMillis) throws IOException {
        return stuckClient(client, timeoutMillis, new ByteBufferPool(12, 8));
    }

    private static WebSocketPusher stuckClient(final ManualSession client, final long timeoutMillis, final ByteBufferPool pool)
        throws IOException {
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, timeoutMillis, pool);
        for (long i = 0; i <= WebSocketPusher.QUEUE_LIMIT; i++) {
            pusher.encode(ServerToClientModel.ROUNDTRIP_LATENCY, i);
            pusher.flush();
        }
        assertEquals(1, client.sent.size(), "one in flight, the queue full");
        pusher.encode(ServerToClientModel.ROUNDTRIP_LATENCY, 99L);
        return pusher;
    }

    @Test
    public void aClientStuckPastTheQueueTimesTheFlushOutAndClosesThePusher() throws IOException {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = stuckClient(client, 20);

        final IOException timeout = assertThrows(IOException.class, pusher::flush);

        assertTrue(timeout.getMessage().contains("Timeout"), timeout.getMessage());
        assertTrue(pusher.isClosed());
        Mockito.verify(client.session).close(Mockito.anyInt(), Mockito.anyString(), Mockito.any(Callback.class));
    }

    @Test
    public void anInterruptedWaitForTheQueueFailsTheFlush() throws IOException {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = stuckClient(client, 60_000);

        Thread.currentThread().interrupt();
        final IOException interrupted = assertThrows(IOException.class, pusher::flush);

        assertTrue(Thread.interrupted(), "the interrupt status is kept");
        assertInstanceOf(InterruptedException.class, interrupted.getCause());
    }

    @Test
    public void aSendCompletingWhileTheQueueIsFullLetsTheFlushThrough() throws Exception {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = stuckClient(client, 60_000);
        final Thread flusher = Thread.ofPlatform().start(() -> {
            try {
                pusher.flush();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        });
        while (flusher.getState() != Thread.State.TIMED_WAITING) Thread.onSpinWait();

        client.complete(0);
        flusher.join(5_000);

        assertFalse(flusher.isAlive());
        for (int i = 1; i <= WebSocketPusher.QUEUE_LIMIT; i++) client.complete(i);
        assertArrayEquals(longFrame(99), client.sent.get(client.sent.size() - 1), "the waiting flush is sent last");
    }

    @Test
    public void aFailedSendDropsTheQueueAndTheNextWriteFails() throws IOException {
        final ManualSession client = new ManualSession();
        final ByteBufferPool pool = new ByteBufferPool(12, 8);
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, pool);
        for (long i = 0; i < 3; i++) {
            pusher.encode(ServerToClientModel.ROUNDTRIP_LATENCY, i);
            pusher.flush();
        }

        client.callbacks.get(0).fail(new IOException("reset"));

        assertEquals(1, client.sent.size(), "the queued buffers are never sent");
        assertEquals(3, pool.availableCount(), "they are back in the pool");
        final IOException failure = assertThrows(IOException.class, () -> pusher.encode(ServerToClientModel.ROUNDTRIP_LATENCY, 9L));
        assertEquals("reset", failure.getCause().getMessage());
        assertTrue(pusher.isClosed());
    }

    @Test
    public void aTimedOutFlushGivesItsBufferBackToThePool() throws IOException {
        final ManualSession client = new ManualSession();
        final ByteBufferPool pool = new ByteBufferPool(12, 8);
        final WebSocketPusher pusher = stuckClient(client, 20, pool);

        assertThrows(IOException.class, pusher::flush);
        client.complete(0);

        assertEquals(pool.totalAllocated(), pool.availableCount(), "no buffer lost with the flush that timed out");
    }

    @Test
    public void anInterruptedFlushGivesItsBufferBackToThePool() throws IOException {
        final ManualSession client = new ManualSession();
        final ByteBufferPool pool = new ByteBufferPool(12, 8);
        final WebSocketPusher pusher = stuckClient(client, 60_000, pool);

        Thread.currentThread().interrupt();
        assertThrows(IOException.class, pusher::flush);
        Thread.interrupted();
        for (int i = 0; i <= WebSocketPusher.QUEUE_LIMIT; i++) client.complete(i);

        assertEquals(pool.totalAllocated(), pool.availableCount(), "no buffer lost with the interrupted flush");
    }

    /**
     * The connection fails while a flush waits for room behind a stuck client: the flush fails with that error, its
     * buffer is not sent on the failed connection.
     */
    @Test
    public void aSendFailingWhileAFlushWaitsFailsThatFlush() throws Exception {
        final ManualSession client = new ManualSession();
        final ByteBufferPool pool = new ByteBufferPool(12, 8);
        final WebSocketPusher pusher = stuckClient(client, 60_000, pool);
        final CompletableFuture<Throwable> outcome = new CompletableFuture<>();
        final Thread flusher = Thread.ofPlatform().start(() -> {
            try {
                pusher.flush();
                outcome.complete(null);
            } catch (final Throwable e) {
                outcome.complete(e);
            }
        });
        while (flusher.getState() != Thread.State.TIMED_WAITING) Thread.onSpinWait();

        client.callbacks.get(0).fail(new IOException("reset"));

        final Throwable failure = outcome.get(5, TimeUnit.SECONDS);
        assertInstanceOf(IOException.class, failure, "the waiting flush fails");
        assertEquals("reset", failure.getCause().getMessage());
        assertEquals(1, client.sent.size(), "nothing sent on the failed connection");
        assertTrue(pusher.isClosed());
        assertEquals(pool.totalAllocated(), pool.availableCount(), "every buffer back to the pool");
    }

    /** A client catching up while a flush waits empties the queue: the flush is then sent, not failed. */
    @Test
    public void aFlushWaitingWhileTheClientCatchesUpIsSent() throws Exception {
        for (int run = 0; run < 50; run++) {
            final ManualSession client = new ManualSession();
            final ByteBufferPool pool = new ByteBufferPool(12, 8);
            final WebSocketPusher pusher = stuckClient(client, 60_000, pool);
            final CompletableFuture<Throwable> outcome = new CompletableFuture<>();
            final Thread flusher = Thread.ofPlatform().start(() -> {
                try {
                    pusher.flush();
                    outcome.complete(null);
                } catch (final Throwable e) {
                    outcome.complete(e);
                }
            });
            while (flusher.getState() != Thread.State.TIMED_WAITING) Thread.onSpinWait();

            for (int i = 0; i <= WebSocketPusher.QUEUE_LIMIT; i++) client.complete(i); // the client catches up

            assertNull(outcome.get(5, TimeUnit.SECONDS), "the waiting flush succeeds");
            flusher.join();
            assertFalse(pusher.isClosed());
            assertEquals(WebSocketPusher.QUEUE_LIMIT + 2, client.sent.size(), "the flushed buffer is sent");
        }
    }

    @Test
    public void aFirstValueLargerThanAPooledBufferGrowsTheEmptyBuffer() throws IOException {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, new ByteBufferPool(4, 8));

        pusher.putLong(0x0102030405060708L);
        pusher.flush();

        assertArrayEquals(ByteBuffer.allocate(8).putLong(0x0102030405060708L).array(), client.sent.get(0));
    }

    @Test
    public void aShortStringOfWideCharactersGrowsTheEncodingBuffer() throws IOException {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, new ByteBufferPool(1 << 10, 8));
        final String accented = "é".repeat(100); // under the JDK encoder's threshold, 300 bytes at worst

        pusher.encode(ServerToClientModel.TEXT, accented);
        pusher.flush();

        final byte[] utf8 = accented.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(concat(new byte[] { (byte) ServerToClientModel.TEXT.getValue(), (byte) ValueTypeModel.STRING_UTF8_UINT8,
            (byte) utf8.length }, utf8), client.sent.get(0));
    }

    @Test
    public void encodeIntWritesAnIntAndTakesTheBoxedPathForAnyOtherModel() throws IOException {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, new ByteBufferPool(64, 8));

        pusher.encodeInt(ServerToClientModel.HEARTBEAT_PERIOD, 5);
        pusher.encodeInt(ServerToClientModel.END, 0);
        pusher.flush();

        assertArrayEquals(new byte[] { (byte) ServerToClientModel.HEARTBEAT_PERIOD.getValue(), 0, 0, 0, 5,
            (byte) ServerToClientModel.END.getValue() }, client.sent.get(0));
    }

    @Test
    public void aNegativeUint31IsRefused() {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, new ByteBufferPool(64, 8));

        assertThrows(IllegalArgumentException.class, () -> pusher.encodeInt(ServerToClientModel.TYPE_CREATE, -1));
    }

    @Test
    public void negativeArrayNumbersTakeTheSmallestWidthThatHoldsThem() throws IOException {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, new ByteBufferPool(256, 8));

        pusher.encode(ServerToClientModel.PADDON_ARGUMENTS,
            new Object[] { -200L, -40_000L, Long.MIN_VALUE, -200, -40_000, (short) -200 });
        pusher.flush();

        final ByteBuffer expected = ByteBuffer.allocate(64).put((byte) ServerToClientModel.PADDON_ARGUMENTS.getValue()).put((byte) 6)
            .put(ArrayValueModel.SHORT.getValue()).putShort((short) -200)
            .put(ArrayValueModel.INTEGER.getValue()).putInt(-40_000)
            .put(ArrayValueModel.LONG.getValue()).putLong(Long.MIN_VALUE)
            .put(ArrayValueModel.SHORT.getValue()).putShort((short) -200)
            .put(ArrayValueModel.INTEGER.getValue()).putInt(-40_000)
            .put(ArrayValueModel.SHORT.getValue()).putShort((short) -200)
            .flip();
        final byte[] bytes = new byte[expected.remaining()];
        expected.get(bytes);
        assertArrayEquals(bytes, client.sent.get(0));
    }

    @Test
    public void rebaseWithoutAConnectionDoesNothing() {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, new ByteBufferPool(64, 8));
        pusher.rebaseWireBytes();
        assertEquals(-1, pusher.getWireBytesSent());
    }

    @Test
    public void aValueLargerThanAPooledBufferGrowsTheBuffer() throws IOException {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, new ByteBufferPool(4, 8));

        pusher.encode(ServerToClientModel.ROUNDTRIP_LATENCY, 0x0102030405060708L);
        pusher.flush();
        client.complete(0);

        assertEquals(2, client.sent.size(), "the key leaves first, the 8-byte value in a grown buffer");
        assertArrayEquals(longFrame(0x0102030405060708L), concat(client.sent.get(0), client.sent.get(1)));
    }

    @Test
    public void theDebugLogsOfTheBuffersChangeNoByte() throws IOException {
        final ManualSession client = new ManualSession();
        final ByteBufferPool pool = new ByteBufferPool(4, 8);
        try (LogLevel pusherLog = LogLevel.set(WebSocketPusher.class, "DEBUG");
             LogLevel poolLog = LogLevel.set(ByteBufferPool.class, "DEBUG")) {
            final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, pool);

            pusher.encode(ServerToClientModel.ROUNDTRIP_LATENCY, 0x0102030405060708L);
            pusher.flush();
            client.complete(0);
            client.complete(1);
        }

        assertArrayEquals(longFrame(0x0102030405060708L), concat(client.sent.get(0), client.sent.get(1)));
        assertEquals(2, pool.availableCount(), "the two pooled buffers are back, the grown one is not pooled");
    }

    @Test
    public void aFlushWithNothingWrittenSendsNothing() throws IOException {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, new ByteBufferPool(64, 8));
        pusher.flush();
        assertTrue(client.sent.isEmpty());
    }

    @Test
    public void closeOfAClosedSessionDoesNotCloseItAgain() throws IOException {
        final ManualSession client = new ManualSession();
        Mockito.when(client.session.isOpen()).thenReturn(false);
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, new ByteBufferPool(64, 8));

        pusher.close();
        pusher.close();

        Mockito.verify(client.session, Mockito.never()).close(Mockito.anyInt(), Mockito.anyString(), Mockito.any(Callback.class));
    }

    @Test
    public void theListenerHearsTheProtocolAndTheWireBytes() throws IOException {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, new ByteBufferPool(64, 8));
        final WebSocket.Listener listener = Mockito.mock(WebSocket.Listener.class);
        final WebSocketConnection connection = Mockito.mock(WebSocketConnection.class);
        Mockito.when(connection.getBytesOut()).thenReturn(100L, 100L, 130L, 3_000_000_000L);
        assertEquals(-1, pusher.getWireBytesSent(), "no connection: unknown");
        pusher.setConnection(connection);
        pusher.setWebSocketListener(listener);
        pusher.rebaseWireBytes(); // 100: what was sent before the listener does not count

        assertEquals(100, pusher.getWireBytesSent());
        pusher.encode(ServerToClientModel.ROUNDTRIP_LATENCY, 1L);
        pusher.flush(); // 130
        client.complete(0);
        pusher.encode(ServerToClientModel.ROUNDTRIP_LATENCY, 2L);
        pusher.flush(); // 3 000 000 000: more than an int

        Mockito.verify(listener, Mockito.times(2)).onOutgoingPonyFramesBytes(9);
        Mockito.verify(listener).onOutgoingWebSocketFrame(0, 30);
        Mockito.verify(listener).onOutgoingWebSocketFrame(0, Integer.MAX_VALUE);
        Mockito.verify(listener).onOutgoingWebSocketFrame(0, (int) (3_000_000_000L - 130 - Integer.MAX_VALUE));
        assertEquals(18, pusher.getProtocolBytesSent());
    }

    @Test
    public void aShortStringAlreadyKnownToTheClientIsReferenced() throws IOException {
        final ManualSession client = new ManualSession();
        final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, new ByteBufferPool(256, 8));
        final StringDictionary dictionary = new StringDictionary(100, 0, 32);
        final int id = StringDictionary.InternResult.id(dictionary.internOrGet("ok")); // sent to the client before
        pusher.setStringDictionary(dictionary);
        assertSame(dictionary, pusher.getStringDictionary());

        pusher.encode(ServerToClientModel.TEXT, "ok");
        pusher.encode(ServerToClientModel.TEXT, "no");
        pusher.encode(ServerToClientModel.TEXT, null);
        pusher.encode(ServerToClientModel.PADDON_ARGUMENTS, new Object[] { "ok", "no" });
        pusher.flush();

        final byte text = (byte) ServerToClientModel.TEXT.getValue();
        final byte array = (byte) ServerToClientModel.PADDON_ARGUMENTS.getValue();
        final byte ref = (byte) com.ponysdk.core.model.ArrayValueModel.STRING_DICTIONARY_REF.getValue();
        final byte ascii8 = (byte) com.ponysdk.core.model.ArrayValueModel.STRING_ASCII_UINT8_LENGTH.getValue();
        assertArrayEquals(new byte[] { text, (byte) ValueTypeModel.STRING_DICTIONARY_REF, 0, (byte) id, // ref
            text, 2, 'n', 'o', // inline: never added for a short string
            text, 0, // null
            array, 2, ref, 0, (byte) id, ascii8, 2, 'n', 'o' }, client.sent.get(0));
    }

    @Test
    public void theStatsRecordEveryValueWrittenUntilStopped() throws IOException {
        final UIContext context = Mockito.mock(UIContext.class);
        final Map<String, Object> attributes = new HashMap<>();
        Mockito.doAnswer(inv -> attributes.get((String) inv.getArgument(0))).when(context).getAttribute(Mockito.anyString());
        Mockito.doAnswer(inv -> attributes.put(inv.getArgument(0), inv.getArgument(1))).when(context)
            .setAttribute(Mockito.anyString(), Mockito.any());
        final List<WebSocketStats> reported = new ArrayList<>();
        final WebSocketStats stats;
        UIContext.setCurrent(context);
        try {
            assertTrue(WebSocketPusher.startRecordingStats(1, TimeUnit.HOURS, reported::add, null));
            assertFalse(WebSocketPusher.startRecordingStats(1, TimeUnit.HOURS, reported::add, null), "one recording at a time");
            assertTrue(WebSocketPusher.isRecordingStats());
            final ManualSession client = new ManualSession();
            final WebSocketPusher pusher = new WebSocketPusher(client.session, 1 << 15, 1_000, new ByteBufferPool(1 << 16, 8));
            pusher.setStringDictionary(new StringDictionary(100, 0, 32));

            pusher.encode(ServerToClientModel.TEXT, "dictionary value");
            pusher.encode(ServerToClientModel.TEXT, "dictionary value");
            pusher.encode(ServerToClientModel.PADDON_ARGUMENTS, new Object[] { 1, "x" });
            pusher.encodeInt(ServerToClientModel.TYPE_UPDATE, 7);
            pusher.encode(ServerToClientModel.HANDLER_TYPE, (byte) 1);
            pusher.encode(ServerToClientModel.HEARTBEAT_PERIOD, 2);
            pusher.encode(ServerToClientModel.ROUNDTRIP_LATENCY, 3L);
            pusher.encode(ServerToClientModel.LEFT, 4.5);
            pusher.encode(ServerToClientModel.END, null);

            stats = WebSocketPusher.stopRecordingStats();
        } finally {
            UIContext.remove();
        }
        assertNotNull(stats);
        assertFalse(WebSocketPusher.isRecordingStats());
        assertEquals(List.of(stats), reported, "the listener gets the stats once stopped");
        final Map<Object, ? extends WebSocketStats.Record> texts = stats.getStats().get(ServerToClientModel.TEXT);
        assertTrue(texts.containsKey("dictionary value"), "the added value: " + texts);
        assertTrue(texts.containsKey("ref:0"), "then its reference: " + texts);
        assertNotNull(stats.getStats().get(ServerToClientModel.PADDON_ARGUMENTS));
        assertNull(WebSocketPusher.stopRecordingStats(), "nothing left to stop");
        assertNotNull(stats.getStats().get(ServerToClientModel.LEFT));
    }

    @Test
    public void aRecordingWithoutAListenerStopsQuietly() {
        assertTrue(WebSocketPusher.startRecordingStats(1, TimeUnit.HOURS, null, null));
        assertNotNull(WebSocketPusher.stopRecordingStats());
        assertFalse(WebSocketPusher.isRecordingStats());
    }
}
