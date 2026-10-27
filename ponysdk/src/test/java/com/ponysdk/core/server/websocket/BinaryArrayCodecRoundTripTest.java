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

package com.ponysdk.core.server.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.driver.PonyFrame;
import com.ponysdk.driver.PonyMessageListener;
import com.ponysdk.driver.PonySDKWebDriver;

import jakarta.json.JsonObject;

/**
 * Cross-implementation guard for the binary array-length codec. Bytes produced by the real server
 * encoder ({@link WebSocketPusher}) are decoded back by the real Pony Driver decoder
 * ({@link PonySDKWebDriver}) — two independent implementations of the protocol. Covers the uint31
 * length escape (254 / 255 / 256) and the short&rarr;int boundary up to 40,000 elements, the sizes
 * the old single-byte length could not represent. Fully deterministic: no server, WebSocket or
 * browser involved.
 */
public class BinaryArrayCodecRoundTripTest {

    @Test
    public void serverEncodedArrayRoundTripsThroughPonyDriver() throws Exception {
        for (final int size : new int[] { 0, 1, 254, 255, 256, 1000, 40_000 }) {
            final Object[] input = new Object[size];
            for (int i = 0; i < size; i++) input[i] = i;

            final Object[] decoded = decodeWithDriver(encodeArrayMessage(input));

            assertEquals(size, decoded.length, "size " + size);
            for (int i = 0; i < size; i++) {
                assertEquals(i, ((Number) decoded[i]).intValue(), "size " + size + " idx " + i);
            }
        }
    }

    /** Encodes {@code [PADDON_ARGUMENTS array][END]} with the real server pusher; returns the wire bytes. */
    private static byte[] encodeArrayMessage(final Object[] array) throws IOException {
        return encode(null, p -> {
            p.encode(ServerToClientModel.PADDON_ARGUMENTS, array);
            p.encode(ServerToClientModel.END, null);
        });
    }

    private interface Writes {
        void write(WebSocketPusher pusher) throws IOException;
    }

    /** Runs {@code writes} on a real pusher (optionally with a string dictionary); returns the wire bytes. */
    private static byte[] encode(final com.ponysdk.core.server.application.StringDictionary dictionary,
                                 final Writes writes) throws IOException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final Session session = Mockito.mock(Session.class);
        Mockito.when(session.isOpen()).thenReturn(true);
        Mockito.doAnswer(inv -> {
            final ByteBuffer buf = inv.getArgument(0);
            final Callback cb = inv.getArgument(1);
            final byte[] chunk = new byte[buf.remaining()];
            buf.duplicate().get(chunk);
            stream.writeBytes(chunk);
            cb.succeed();
            return null;
        }).when(session).sendBinary(Mockito.any(ByteBuffer.class), Mockito.any(Callback.class));

        final ByteBufferPool pool = new ByteBufferPool(1 << 20, 4);
        final WebSocketPusher pusher = new WebSocketPusher(session, 1 << 19, 1_000, pool);
        if (dictionary != null) pusher.setStringDictionary(dictionary);
        writes.write(pusher);
        pusher.flush();
        return stream.toByteArray();
    }

    /** Feeds the wire bytes to the real Pony Driver decoder; returns the decoded PADDON_ARGUMENTS array. */
    private static Object[] decodeWithDriver(final byte[] wire) throws Exception {
        for (final List<PonyFrame> message : decodeAll(wire)) {
            for (final PonyFrame frame : message) {
                if (frame.getModel() == ServerToClientModel.PADDON_ARGUMENTS) return (Object[]) frame.getValue();
            }
        }
        throw new AssertionError("PADDON_ARGUMENTS frame was not decoded");
    }

    /** Feeds the wire bytes to the real Pony Driver decoder; returns every decoded message. */
    private static List<List<PonyFrame>> decodeAll(final byte[] wire) throws Exception {
        final List<List<PonyFrame>> received = new ArrayList<>();
        final PonyMessageListener listener = new PonyMessageListener() {
            @Override
            public void onSendMessage(final JsonObject message) {
                // not used in this test
            }

            @Override
            public void onReceiveMessage(final List<PonyFrame> message) {
                received.add(new ArrayList<>(message)); // copy: the driver reuses and clears its list after dispatch
            }
        };
        final PonySDKWebDriver driver = new PonySDKWebDriver(listener, null, null, false);

        final Method onMessage = PonySDKWebDriver.class.getDeclaredMethod("onMessage", ByteBuffer.class);
        onMessage.setAccessible(true);
        onMessage.invoke(driver, ByteBuffer.wrap(wire));
        return received;
    }

    @Test
    public void dictionaryEncodedStringsRoundTripThroughPonyDriver() throws Exception {
        // ADD then REF for model values and array elements, ASCII and UTF-8, short ids and a long
        // (uint31 4-byte) id. The dictionary is on by default, so the driver must decode all of it.
        final com.ponysdk.core.server.application.StringDictionary dictionary =
                new com.ponysdk.core.server.application.StringDictionary(70_000, 4, 70_000);
        for (int i = 0; i < 40_000; i++) dictionary.internOrGet("filler-" + i); // push next ids past 32767
        final byte[] wire = encode(dictionary, p -> {
            for (int round = 0; round < 2; round++) { // round 0: ADD, round 1: REF
                p.encode(ServerToClientModel.TYPE_UPDATE, 7);
                p.encode(ServerToClientModel.TEXT, "repeated-ascii");
                p.encode(ServerToClientModel.WIDGET_TITLE, "\u00e9t\u00e9 \u20ac caf\u00e9");
                p.encode(ServerToClientModel.PADDON_ARGUMENTS, new Object[] { "array-string", 1, "array-\u00e9l\u00e9ment", null });
                p.encode(ServerToClientModel.END, null);
            }
        });

        final List<List<PonyFrame>> messages = decodeAll(wire);
        assertEquals(2, messages.size());
        for (final List<PonyFrame> message : messages) {
            assertEquals("repeated-ascii", valueOf(message, ServerToClientModel.TEXT));
            assertEquals("\u00e9t\u00e9 \u20ac caf\u00e9", valueOf(message, ServerToClientModel.WIDGET_TITLE));
            final Object[] array = (Object[]) valueOf(message, ServerToClientModel.PADDON_ARGUMENTS);
            assertEquals("array-string", array[0]);
            assertEquals(1, ((Number) array[1]).intValue());
            assertEquals("array-\u00e9l\u00e9ment", array[2]);
            assertEquals(null, array[3]);
        }
    }

    @Test
    public void asciiStringsAroundTheDictionaryTagValuesRoundTrip() throws Exception {
        // Regression: inline ASCII lengths 248 / 249 were written as the length byte itself, which is
        // also STRING_DICTIONARY_ADD / _REF: the client read them as a dictionary entry.
        for (final int length : new int[] { 246, 247, 248, 249, 250, 251 }) {
            final String value = "x".repeat(length);
            for (final boolean withDictionary : new boolean[] { false, true }) {
                final byte[] wire = encode(withDictionary ? new com.ponysdk.core.server.application.StringDictionary() : null, p -> {
                    p.encode(ServerToClientModel.TYPE_UPDATE, 7);
                    p.encode(ServerToClientModel.TEXT, value);
                    p.encode(ServerToClientModel.END, null);
                });
                final List<List<PonyFrame>> messages = decodeAll(wire);
                assertEquals(1, messages.size(), "length " + length + " dict=" + withDictionary);
                assertEquals(value, valueOf(messages.get(0), ServerToClientModel.TEXT), "length " + length + " dict=" + withDictionary);
            }
        }
    }

    /** Every element type and every length form of the array codec, with and without the dictionary. */
    @Test
    public void everyArrayElementFormRoundTripsThroughPonyDriver() throws Exception {
        final String utf8Medium = "é".repeat(200); // 400 bytes: UTF-8 uint16 length
        final String utf8Long = "é".repeat(40_000); // 80 000 bytes: UTF-8 uint32 length
        final String asciiMedium = "a".repeat(300); // ASCII uint16 length
        final String asciiLong = "b".repeat(70_000); // past uint16: sent as UTF-8 uint32
        final Object[] input = { (byte) 5, (short) 7, (short) 3000, 100, 20_000, 3_000_000, 9L, 300L, 70_000L, 1L << 40,
            1.5, 0.1, 2.5f, true, false, null, "abc", "été", utf8Medium, utf8Long, asciiMedium, asciiLong,
            new StringBuilder("other") };
        final Object[] expected = { 5, 7, 3000, 100, 20_000, 3_000_000, 9, 300, 70_000, 1L << 40, 1.5, 0.1, 2.5, true, false,
            null, "abc", "été", utf8Medium, utf8Long, asciiMedium, asciiLong, "other" };
        for (final boolean withDictionary : new boolean[] { false, true }) {
            final com.ponysdk.core.server.application.StringDictionary dictionary = withDictionary
                    ? new com.ponysdk.core.server.application.StringDictionary(1_000, 0, 32) : null;
            final byte[] wire = encode(dictionary, p -> {
                for (int round = 0; round < 2; round++) {
                    p.encode(ServerToClientModel.TYPE_UPDATE, 7);
                    p.encode(ServerToClientModel.PADDON_ARGUMENTS, input);
                    p.encode(ServerToClientModel.END, null);
                }
            });

            final List<List<PonyFrame>> messages = decodeAll(wire);
            assertEquals(2, messages.size());
            for (final List<PonyFrame> message : messages) {
                final Object[] decoded = (Object[]) valueOf(message, ServerToClientModel.PADDON_ARGUMENTS);
                assertEquals(expected.length, decoded.length);
                for (int i = 0; i < expected.length; i++) {
                    final Object e = expected[i];
                    final Object d = decoded[i];
                    final String where = "element " + i + " dict=" + withDictionary;
                    if (e instanceof Long l) assertEquals(l.longValue(), ((Number) d).longValue(), where);
                    else if (e instanceof Integer n) assertEquals(n.intValue(), ((Number) d).intValue(), where);
                    else if (e instanceof Double x) assertEquals(x, ((Number) d).doubleValue(), 1e-12, where);
                    else assertEquals(e, d, where);
                }
            }
        }
    }

    /** Every length form of a model's string value, inline and through the dictionary. */
    @Test
    public void everyStringValueFormRoundTripsThroughPonyDriver() throws Exception {
        final String[] values = { "ab", "été!", "é".repeat(200), "é".repeat(40_000), "a".repeat(300),
            "b".repeat(70_000) };
        for (final boolean withDictionary : new boolean[] { false, true }) {
            final com.ponysdk.core.server.application.StringDictionary dictionary = withDictionary
                    ? new com.ponysdk.core.server.application.StringDictionary(1_000, 0, 32) : null;
            for (final String value : values) {
                final byte[] wire = encode(dictionary, p -> {
                    for (int round = 0; round < 2; round++) {
                        p.encode(ServerToClientModel.TYPE_UPDATE, 7);
                        p.encode(ServerToClientModel.TEXT, value);
                        p.encode(ServerToClientModel.END, null);
                    }
                });
                final List<List<PonyFrame>> messages = decodeAll(wire);
                assertEquals(2, messages.size());
                for (final List<PonyFrame> message : messages) {
                    assertEquals(value, valueOf(message, ServerToClientModel.TEXT), value.length() + " chars, dict=" + withDictionary);
                }
            }
        }
    }

    /** A full dictionary with no eviction budget left sends new strings inline, in values and in arrays. */
    @Test
    public void aFullDictionarySendsNewStringsInline() throws Exception {
        final com.ponysdk.core.server.application.StringDictionary dictionary =
                new com.ponysdk.core.server.application.StringDictionary(1, 0, 0);
        final byte[] wire = encode(dictionary, p -> {
            p.encode(ServerToClientModel.TYPE_UPDATE, 7);
            p.encode(ServerToClientModel.TEXT, "first value");
            p.encode(ServerToClientModel.WIDGET_TITLE, "second value");
            p.encode(ServerToClientModel.PADDON_ARGUMENTS, new Object[] { "third value", "first value" });
            p.encode(ServerToClientModel.END, null);
        });

        final List<PonyFrame> message = decodeAll(wire).get(0);
        assertEquals("first value", valueOf(message, ServerToClientModel.TEXT));
        assertEquals("second value", valueOf(message, ServerToClientModel.WIDGET_TITLE));
        final Object[] array = (Object[]) valueOf(message, ServerToClientModel.PADDON_ARGUMENTS);
        assertEquals("third value", array[0]);
        assertEquals("first value", array[1], "the one interned string is referenced");
    }

    private static Object valueOf(final List<PonyFrame> message, final ServerToClientModel model) {
        for (final PonyFrame frame : message) if (frame.getModel() == model) return frame.getValue();
        throw new AssertionError(model + " not decoded in " + message);
    }
}
