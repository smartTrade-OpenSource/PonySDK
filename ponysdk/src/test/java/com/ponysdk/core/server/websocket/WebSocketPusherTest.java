/*
 * Copyright (c) 2017 PonySDK
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.BooleanModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.StringDictionary;

/**
 * Regression tests for {@link WebSocketPusher} string encoding.
 * <p>
 * The former ASCII fast-path decided whether a string was ASCII with a sampling heuristic
 * ("isLikelyAscii") that only inspected chars at indices 0–15 then every 32nd. A non-ASCII char at
 * any other position was missed, and the fast path then truncated it with a {@code (byte)} cast —
 * silently corrupting the value on the wire (it was tagged ASCII but held a wrong byte). Encoding
 * must now always be lossless, whatever the position of a non-ASCII character.
 * <p>
 * {@code encodeStringToBuffer} is private and has no public seam, so it is exercised directly via
 * reflection (the value-type tagging it drives is what the wire format depends on).
 */
public class WebSocketPusherTest {

    private WebSocketPusher pusher;
    private Method encode;
    private Field bufferField;
    private Field asciiField;

    @Before
    public void setUp() throws Exception {
        final Session session = Mockito.mock(Session.class);
        final ByteBufferPool pool = new ByteBufferPool(1 << 16, 4);
        pusher = new WebSocketPusher(session, 1 << 15, 1_000, pool);

        encode = WebSocketPusher.class.getDeclaredMethod("encodeStringToBuffer", String.class);
        encode.setAccessible(true);
        bufferField = WebSocketPusher.class.getDeclaredField("stringEncodeBuffer");
        bufferField.setAccessible(true);
        asciiField = WebSocketPusher.class.getDeclaredField("lastEncodedAscii");
        asciiField.setAccessible(true);
    }

    /** Encodes {@code value}, asserts it round-trips losslessly, and returns whether the ASCII path was taken. */
    private boolean assertRoundTrips(final String value) throws Exception {
        final int byteLength = (int) encode.invoke(pusher, value);
        final byte[] buffer = (byte[]) bufferField.get(pusher);
        final boolean ascii = (boolean) asciiField.get(pusher);
        final byte[] encoded = Arrays.copyOf(buffer, byteLength);
        final String decoded = new String(encoded, ascii ? StandardCharsets.US_ASCII : StandardCharsets.UTF_8);
        assertEquals("encoding must round-trip losslessly", value, decoded);
        return ascii;
    }

    @Test
    public void nonAsciiBeyondSampledPositionsIsNotTruncated() throws Exception {
        // 20 ASCII chars, then 'é' at index 20 — exactly the case the old sampling heuristic missed.
        assertFalse("a string holding a non-ASCII char must use the UTF-8 path",
                assertRoundTrips("abcdefghijklmnopqrst\u00e9uvwxyz0123456789"));
    }

    @Test
    public void nonAsciiAtVeryEndOfLongStringIsNotTruncated() throws Exception {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60; i++) sb.append('x');
        sb.append('\u20ac'); // € (3-byte UTF-8) at the very end, beyond any sampling
        assertFalse(assertRoundTrips(sb.toString()));
    }

    @Test
    public void shortNonAsciiRoundTrips() throws Exception {
        assertFalse(assertRoundTrips("caf\u00e9")); // "café"
    }

    @Test
    public void multiByteAndSurrogatePairRoundTrips() throws Exception {
        // 你好世界 こんにちは 😀 (the emoji is a surrogate pair)
        assertFalse(assertRoundTrips("\u4f60\u597d\u4e16\u754c \u3053\u3093\u306b\u3061\u306f \uD83D\uDE00"));
    }

    @Test
    public void pureAsciiKeepsUsingTheFastPath() throws Exception {
        assertTrue("pure ASCII (even long) must keep using the fast path",
                assertRoundTrips("a long but perfectly ASCII string 0123456789 abcdefghijklmnop"));
    }

    @Test
    public void emptyStringRoundTrips() throws Exception {
        assertTrue(assertRoundTrips(""));
    }

    // ── Binary array length encoding (lifts the old 255-element cap via a uint31 escape) ──

    /** Encodes an ARRAY-typed model and returns the raw bytes sent on the wire. */
    private static byte[] encodeArrayAndCapture(final Object[] array) throws Exception {
        final java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        final org.eclipse.jetty.websocket.api.Session session = Mockito.mock(org.eclipse.jetty.websocket.api.Session.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        Mockito.doAnswer(inv -> {
            final java.nio.ByteBuffer buf = inv.getArgument(0);
            final org.eclipse.jetty.websocket.api.Callback cb = inv.getArgument(1);
            final byte[] chunk = new byte[buf.remaining()];
            buf.duplicate().get(chunk);
            stream.writeBytes(chunk); // accumulate, in case the pusher flushes in several chunks
            cb.succeed();
            return null;
        }).when(session).sendBinary(Mockito.any(java.nio.ByteBuffer.class),
                Mockito.any(org.eclipse.jetty.websocket.api.Callback.class));

        final ByteBufferPool pool = new ByteBufferPool(1 << 20, 4);
        final WebSocketPusher p = new WebSocketPusher(session, 1 << 19, 1_000, pool);
        p.encode(com.ponysdk.core.model.ServerToClientModel.PADDON_ARGUMENTS, array);
        p.flush();
        return stream.toByteArray();
    }

    @Test
    public void smallArrayLengthIsASingleByte() throws Exception {
        final byte[] bytes = encodeArrayAndCapture(new Object[] { 1, 2, 3 });
        assertEquals(com.ponysdk.core.model.ServerToClientModel.PADDON_ARGUMENTS.getValue() & 0xFF, bytes[0] & 0xFF);
        assertEquals("length < 255 stays a single byte", 3, bytes[1] & 0xFF);
    }

    @Test
    public void largeArrayLengthUsesUint31Escape() throws Exception {
        final Object[] array = new Object[300]; // > 255 — used to throw; now uses the uint31 escape
        for (int i = 0; i < array.length; i++) array[i] = i;
        final byte[] bytes = encodeArrayAndCapture(array);

        assertEquals(com.ponysdk.core.model.ServerToClientModel.PADDON_ARGUMENTS.getValue() & 0xFF, bytes[0] & 0xFF);
        assertEquals("escape byte signals a uint31 length follows",
                com.ponysdk.core.model.ArrayValueModel.LENGTH_UINT31_ESCAPE, bytes[1] & 0xFF);
        // 300 <= Short.MAX → 2-byte big-endian, high bit clear (positive uint31)
        final int uint31 = (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
        assertEquals(300, uint31);
    }

    @Test
    public void binaryArrayLengthRoundTripsAcrossBoundaries() throws Exception {
        // Encode with the real server pusher, then decode with the same length+element logic the
        // terminal (ReaderBuffer) uses — across the escape (254/255/256) and the uint31 short→int
        // (32767/32768) boundaries. Proves the wire format is self-consistent end to end.
        for (final int size : new int[] { 0, 1, 254, 255, 256, 32767, 32768, 33000 }) {
            final Object[] array = new Object[size];
            for (int i = 0; i < size; i++) array[i] = i % 100; // small ints → BYTE-encoded elements
            final int[] decoded = decodeSmallIntArray(encodeArrayAndCapture(array));
            assertEquals("size " + size, size, decoded.length);
            for (int i = 0; i < size; i++) assertEquals("size " + size + " idx " + i, i % 100, decoded[i]);
        }
    }

    /** Decoder mirroring {@code ReaderBuffer}: [modelKey][length][BYTE-encoded int elements...]. */
    private static int[] decodeSmallIntArray(final byte[] bytes) {
        int pos = 1; // skip the 1-byte model key
        int len = bytes[pos++] & 0xFF;
        if (len == com.ponysdk.core.model.ArrayValueModel.LENGTH_UINT31_ESCAPE) {
            final short s = (short) ((bytes[pos++] & 0xFF) << 8 | (bytes[pos++] & 0xFF));
            if (s >= 0) {
                len = s;
            } else {
                final int lo = (bytes[pos++] & 0xFF) << 8 | (bytes[pos++] & 0xFF);
                len = (s << 16 | lo) & 0x7F_FF_FF_FF;
            }
        }
        final int[] result = new int[len];
        final int byteTag = com.ponysdk.core.model.ArrayValueModel.BYTE.getValue() & 0xFF;
        for (int i = 0; i < len; i++) {
            final int tag = bytes[pos++] & 0xFF;
            if (tag != byteTag) throw new IllegalStateException("expected BYTE tag, got " + tag);
            result[i] = bytes[pos++]; // signed byte payload
        }
        if (pos != bytes.length) throw new IllegalStateException("trailing bytes: pos=" + pos + " total=" + bytes.length);
        return result;
    }

    // ── Capture harness for scalar / dictionary / lifecycle paths ──

    /** A pusher whose session accumulates everything sent and (optionally) fails the send callback. */
    private static final class Capture {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int sendCount;
        final Session session;
        final WebSocketPusher pusher;

        Capture(final int maxChunk, final boolean failSend) {
            session = Mockito.mock(Session.class);
            Mockito.when(session.isOpen()).thenReturn(true);
            Mockito.doAnswer(inv -> {
                final ByteBuffer buf = inv.getArgument(0);
                final Callback cb = inv.getArgument(1);
                final byte[] chunk = new byte[buf.remaining()];
                buf.duplicate().get(chunk);
                out.writeBytes(chunk);
                sendCount++;
                if (failSend) cb.fail(new RuntimeException("send failed"));
                else cb.succeed();
                return null;
            }).when(session).sendBinary(Mockito.any(ByteBuffer.class), Mockito.any(Callback.class));
            pusher = new WebSocketPusher(session, maxChunk, 1_000, new ByteBufferPool(1 << 20, 4));
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }

    private static byte[] encodeAndFlush(final ServerToClientModel model, final Object value) throws IOException {
        final Capture c = new Capture(1 << 19, false);
        c.pusher.encode(model, value);
        c.pusher.flush();
        return c.bytes();
    }

    private static int key(final ServerToClientModel model) {
        return model.getValue() & 0xFF;
    }

    @Test
    public void encodesIntegerAsModelKeyPlusFourBigEndianBytes() throws Exception {
        final byte[] b = encodeAndFlush(ServerToClientModel.HEARTBEAT_PERIOD, 0x01020304);
        assertEquals(key(ServerToClientModel.HEARTBEAT_PERIOD), b[0] & 0xFF);
        assertEquals(0x01, b[1] & 0xFF);
        assertEquals(0x02, b[2] & 0xFF);
        assertEquals(0x03, b[3] & 0xFF);
        assertEquals(0x04, b[4] & 0xFF);
        assertEquals(5, b.length);
    }

    @Test
    public void encodesLongAsEightBytes() throws Exception {
        final byte[] b = encodeAndFlush(ServerToClientModel.ROUNDTRIP_LATENCY, 0x0102030405060708L);
        assertEquals(9, b.length); // 1 model key + 8
        long v = 0;
        for (int i = 1; i < 9; i++) v = v << 8 | (b[i] & 0xFF);
        assertEquals(0x0102030405060708L, v);
    }

    @Test
    public void encodesBooleanUsingBooleanModelValues() throws Exception {
        final byte[] t = encodeAndFlush(ServerToClientModel.OPTION_FORMFIELD_TABULATION, true);
        assertEquals(BooleanModel.TRUE.getValue() & 0xFF, t[1] & 0xFF);
        final byte[] f = encodeAndFlush(ServerToClientModel.OPTION_FORMFIELD_TABULATION, false);
        assertEquals(BooleanModel.FALSE.getValue() & 0xFF, f[1] & 0xFF);
    }

    @Test
    public void encodesByteAndDoubleAndNull() throws Exception {
        final byte[] bb = encodeAndFlush(ServerToClientModel.VALUE_CHECKBOX, (byte) 7);
        assertEquals(7, bb[1]);
        assertEquals(2, bb.length);

        final byte[] db = encodeAndFlush(ServerToClientModel.LEFT, 1.5d);
        assertEquals(9, db.length);

        final byte[] nb = encodeAndFlush(ServerToClientModel.HEARTBEAT, null);
        assertEquals("NULL-typed model writes only the model key", 1, nb.length);
    }

    @Test
    public void encodesUint31AsShortWhenSmallAndIntWithHighBitWhenLarge() throws Exception {
        final byte[] small = encodeAndFlush(ServerToClientModel.CREATE_CONTEXT, 5);
        assertEquals("small uint31 -> 2 bytes", 3, small.length);
        assertEquals(5, (small[1] & 0xFF) << 8 | (small[2] & 0xFF));

        final byte[] large = encodeAndFlush(ServerToClientModel.CREATE_CONTEXT, 100_000);
        assertEquals("large uint31 -> 4 bytes", 5, large.length);
        final int raw = (large[1] & 0xFF) << 24 | (large[2] & 0xFF) << 16 | (large[3] & 0xFF) << 8 | (large[4] & 0xFF);
        assertTrue("high bit set signals an int-width uint31", (raw & 0x80000000) != 0);
        assertEquals(100_000, raw & 0x7FFFFFFF);
    }

    @Test
    public void encodesShortAsciiStringAsLengthPrefixedBytes() throws Exception {
        final byte[] b = encodeAndFlush(ServerToClientModel.TYPE_HISTORY, "hello");
        assertEquals(key(ServerToClientModel.TYPE_HISTORY), b[0] & 0xFF);
        assertEquals("short ASCII -> single length byte", 5, b[1] & 0xFF);
        assertEquals("hello", new String(b, 2, 5, StandardCharsets.US_ASCII));
    }

    @Test
    public void encodesArrayWithAllElementTypesWithoutError() throws Exception {
        final Capture c = new Capture(1 << 19, false);
        final Object[] mixed = { null, 1, "stringValue", (byte) 2, (short) 3, true, false, 4L, 5.0d, 6.0f,
            new Object() {
                @Override
                public String toString() {
                    return "custom";
                }
            } };
        c.pusher.encode(ServerToClientModel.PADDON_ARGUMENTS, mixed);
        c.pusher.flush();
        assertTrue(c.sendCount >= 1);
        assertEquals(mixed.length, c.bytes()[1] & 0xFF); // length byte
    }

    @Test
    public void stringDictionaryAddsThenReferencesRepeatedValues() throws Exception {
        final Capture c = new Capture(1 << 19, false);
        c.pusher.setStringDictionary(new StringDictionary());
        assertNotNull(c.pusher.getStringDictionary());

        c.pusher.encode(ServerToClientModel.TYPE_HISTORY, "repeated-value"); // dictionary ADD
        c.pusher.encode(ServerToClientModel.TYPE_HISTORY, "repeated-value"); // dictionary REF
        c.pusher.encode(ServerToClientModel.TYPE_HISTORY, "ab"); // shorter than min-profitable -> inline
        c.pusher.flush();

        assertTrue(c.sendCount >= 1);
        assertTrue(c.bytes().length > 0);
    }

    @Test
    public void smallMaxChunkSizeProducesMultipleSends() throws Exception {
        final Capture c = new Capture(8, false); // flush proactively after ~8 bytes
        for (int i = 0; i < 6; i++) {
            c.pusher.encode(ServerToClientModel.ROUNDTRIP_LATENCY, (long) i); // 1 + 8 bytes each
        }
        c.pusher.flush();
        assertTrue("a small maxChunkSize must split the output across several sends", c.sendCount >= 2);
    }

    @Test
    public void closeClosesOpenSessionAndRejectsFurtherWrites() throws Exception {
        final Capture c = new Capture(1 << 19, false);
        c.pusher.encode(ServerToClientModel.HEARTBEAT_PERIOD, 1);
        c.pusher.close();

        assertTrue(c.pusher.isClosed());
        Mockito.verify(c.session).close(Mockito.anyInt(), Mockito.anyString(), Mockito.any(Callback.class));
        assertThrows(IOException.class, () -> c.pusher.encode(ServerToClientModel.HEARTBEAT_PERIOD, 2));
    }

    @Test
    public void asyncSendFailureSurfacesOnNextOperation() throws Exception {
        final Capture c = new Capture(1 << 19, true); // the send callback fails
        c.pusher.encode(ServerToClientModel.HEARTBEAT_PERIOD, 1);
        c.pusher.flush(); // triggers sendBinary -> callback.fail(...) -> asyncError set

        assertThrows("a failed async send must surface on the next write",
            IOException.class, () -> c.pusher.encode(ServerToClientModel.HEARTBEAT_PERIOD, 2));
    }
}
