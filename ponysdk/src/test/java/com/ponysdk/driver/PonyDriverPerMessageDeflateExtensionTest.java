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

package com.ponysdk.driver;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.glassfish.tyrus.core.extension.ExtendedExtension.ExtensionContext;
import org.glassfish.tyrus.core.frame.Frame;
import org.junit.jupiter.api.Test;

/** The bandwidth the driver reports for compressed frames: the payload plus the exact WebSocket frame header. */
public class PonyDriverPerMessageDeflateExtensionTest {

    private static final byte BINARY = 0x2;

    private final List<Integer> sent = new ArrayList<>();
    private final List<Integer> received = new ArrayList<>();
    private final PonyBandwithListener listener = new PonyBandwithListener() {

        @Override
        public void onSend(final int bytes) {
        }

        @Override
        public void onReceive(final int bytes) {
        }

        @Override
        public void onSendCompressed(final int bytes) {
            sent.add(bytes);
        }

        @Override
        public void onReceiveCompressed(final int bytes) {
            received.add(bytes);
        }
    };

    private static ExtensionContext context() {
        final Map<String, Object> properties = new HashMap<>();
        return () -> properties;
    }

    private static byte[] incompressible(final int size) {
        final byte[] bytes = new byte[size];
        new Random(42).nextBytes(bytes);
        return bytes;
    }

    /** Sends {@code payload} masked, as a client does, then receives the compressed frame unmasked, as from a server. */
    private void roundTrip(final byte[] payload, final int expectedLengthBytes) {
        sent.clear();
        received.clear();
        final PonyDriverPerMessageDeflateExtension client = new PonyDriverPerMessageDeflateExtension(listener);
        final ExtensionContext clientContext = context();
        client.onHandshakeResponse(clientContext, List.of());
        final Frame outgoing = Frame.builder().fin(true).opcode(BINARY).mask(true).maskingKey(0x01020304).payloadData(payload)
            .build();

        final Frame compressed = client.processOutgoing(clientContext, outgoing);

        final int compressedLength = (int) compressed.getPayloadLength();
        assertTrue(compressed.isRsv1(), "compressed");
        // 2 bytes of header, the extended payload length, a 4-byte masking key
        assertEquals(List.of(2 + expectedLengthBytes + 4 + compressedLength), sent);

        final PonyDriverPerMessageDeflateExtension server = new PonyDriverPerMessageDeflateExtension(listener);
        final ExtensionContext serverContext = context();
        server.onExtensionNegotiation(serverContext, List.of());
        final Frame incoming = Frame.builder(compressed).mask(false).maskingKey(null).build();

        final Frame decompressed = server.processIncoming(serverContext, incoming);

        assertArrayEquals(payload, decompressed.getPayloadData());
        assertEquals(List.of(2 + expectedLengthBytes + compressedLength), received, "an unmasked frame has no masking key");
    }

    @Test
    public void countsTheHeaderOfASmallFrame() {
        roundTrip("hello hello hello hello".getBytes(), 0);
    }

    @Test
    public void countsTheHeaderOfAMediumFrame() {
        roundTrip(incompressible(1_000), 2);
    }

    @Test
    public void countsTheHeaderOfALargeFrame() {
        roundTrip(incompressible(70_000), 8);
    }

    @Test
    public void withoutAListenerNothingIsCounted() {
        final PonyDriverPerMessageDeflateExtension client = new PonyDriverPerMessageDeflateExtension(null);
        final ExtensionContext clientContext = context();
        client.onHandshakeResponse(clientContext, List.of());
        final Frame frame = Frame.builder().fin(true).opcode(BINARY).payloadData("abc".getBytes()).build();
        final Frame compressed = client.processOutgoing(clientContext, frame);

        final PonyDriverPerMessageDeflateExtension server = new PonyDriverPerMessageDeflateExtension(null);
        final ExtensionContext serverContext = context();
        server.onExtensionNegotiation(serverContext, List.of());
        assertArrayEquals("abc".getBytes(), server.processIncoming(serverContext, compressed).getPayloadData());
        assertTrue(sent.isEmpty());
        assertTrue(received.isEmpty());
    }
}
