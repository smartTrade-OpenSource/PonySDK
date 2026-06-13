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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.Test;
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

            assertEquals("size " + size, size, decoded.length);
            for (int i = 0; i < size; i++) {
                assertEquals("size " + size + " idx " + i, i, ((Number) decoded[i]).intValue());
            }
        }
    }

    /** Encodes {@code [PADDON_ARGUMENTS array][END]} with the real server pusher; returns the wire bytes. */
    private static byte[] encodeArrayMessage(final Object[] array) throws IOException {
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
        pusher.encode(ServerToClientModel.PADDON_ARGUMENTS, array);
        pusher.encode(ServerToClientModel.END, null);
        pusher.flush();
        return stream.toByteArray();
    }

    /** Feeds the wire bytes to the real Pony Driver decoder; returns the decoded PADDON_ARGUMENTS array. */
    private static Object[] decodeWithDriver(final byte[] wire) throws Exception {
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

        for (final List<PonyFrame> message : received) {
            for (final PonyFrame frame : message) {
                if (frame.getModel() == ServerToClientModel.PADDON_ARGUMENTS) return (Object[]) frame.getValue();
            }
        }
        throw new AssertionError("PADDON_ARGUMENTS frame was not decoded");
    }
}
