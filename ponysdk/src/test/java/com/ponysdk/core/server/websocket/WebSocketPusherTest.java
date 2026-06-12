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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.eclipse.jetty.websocket.api.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
}
