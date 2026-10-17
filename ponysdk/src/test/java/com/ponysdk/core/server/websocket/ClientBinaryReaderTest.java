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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.ponysdk.core.model.ClientToServerModel;

/**
 * Tests {@link ClientBinaryReader}, which parses <strong>untrusted</strong> client bytes.
 * Beyond the happy path, this guards against malformed input: a length prefix or a model ordinal
 * is attacker-controlled, so the reader must reject out-of-bounds values instead of allocating a
 * huge/negative array (OutOfMemoryError / crash — a DoS) or indexing the enum array out of bounds.
 */
public class ClientBinaryReaderTest {

    @Test
    public void happyPathRoundTrip() {
        final ByteBuffer buf = ByteBuffer.allocate(128);
        buf.put((byte) 0).put((byte) 2).putInt(42);                  // key#0, INT 42
        buf.put((byte) 0).put((byte) 1).put((byte) 1);               // key#0, BOOLEAN true
        buf.put((byte) 0).put((byte) 3).putDouble(3.5);              // key#0, DOUBLE 3.5
        final byte[] s = "caf\u00e9".getBytes(StandardCharsets.UTF_8);
        buf.put((byte) 0).put((byte) 4).putInt(s.length).put(s);     // key#0, STRING "café"
        buf.put((byte) ClientBinaryReader.END_MARKER);
        buf.flip();

        final ClientBinaryReader r = new ClientBinaryReader(buf);
        final ClientToServerModel k0 = ClientToServerModel.values()[0];

        assertTrue(r.hasNext());
        assertEquals(k0, r.readKey());
        assertEquals(ClientBinaryReader.tagInt(), r.readTag());
        assertEquals(42, r.readInt());

        assertTrue(r.hasNext());
        assertEquals(k0, r.readKey());
        assertEquals(ClientBinaryReader.tagBoolean(), r.readTag());
        assertTrue(r.readBoolean());

        assertTrue(r.hasNext());
        r.readKey();
        assertEquals(ClientBinaryReader.tagDouble(), r.readTag());
        assertEquals(3.5, r.readDouble(), 0.0);

        assertTrue(r.hasNext());
        r.readKey();
        assertEquals(ClientBinaryReader.tagString(), r.readTag());
        assertEquals("caf\u00e9", r.readString());

        assertFalse(r.hasNext());
        r.skipEnd();
        assertFalse(r.hasMoreMessages());
    }

    @Test
    public void negativeStringLengthIsRejected() {
        final ByteBuffer buf = (ByteBuffer) ByteBuffer.allocate(8).putInt(-1).flip();
        final ClientBinaryReader r = new ClientBinaryReader(buf);
        assertThrows(IllegalStateException.class, r::readString);
    }

    @Test
    public void hugeStringLengthDoesNotAllocate() {
        // DoS regression: a tiny message claiming a ~2 GB string must be rejected, not allocated.
        final ByteBuffer buf = (ByteBuffer) ByteBuffer.allocate(8).putInt(Integer.MAX_VALUE).flip();
        final ClientBinaryReader r = new ClientBinaryReader(buf);
        assertThrows(IllegalStateException.class, r::readString);
    }

    @Test
    public void truncatedLengthPrefixIsRejected() {
        final ByteBuffer buf = (ByteBuffer) ByteBuffer.allocate(2).put((byte) 1).put((byte) 2).flip();
        final ClientBinaryReader r = new ClientBinaryReader(buf);
        assertThrows(IllegalStateException.class, r::readString);
    }

    @Test
    public void outOfRangeKeyOrdinalIsRejected() {
        final int firstInvalid = ClientToServerModel.values().length; // valid ordinals are 0..len-1
        final ByteBuffer buf = (ByteBuffer) ByteBuffer.allocate(4).put((byte) firstInvalid).flip();
        final ClientBinaryReader r = new ClientBinaryReader(buf);
        assertThrows(IllegalStateException.class, r::readKey);
    }

    @Test
    public void skipValueHugeStringLengthIsRejected() {
        final ByteBuffer buf = (ByteBuffer) ByteBuffer.allocate(8).putInt(Integer.MAX_VALUE).flip();
        final ClientBinaryReader r = new ClientBinaryReader(buf);
        assertThrows(IllegalStateException.class, () -> r.skipValue(ClientBinaryReader.tagString()));
    }

    @Test
    public void skipValueUnknownTagIsRejected() {
        final ClientBinaryReader r = new ClientBinaryReader(ByteBuffer.allocate(4));
        assertThrows(IllegalStateException.class, () -> r.skipValue(99));
    }

    @Test
    public void skipValueHandlesAllKnownTags() {
        final ByteBuffer buf = ByteBuffer.allocate(32);
        buf.put((byte) 1);                                       // boolean payload
        buf.putInt(7);                                           // int payload
        buf.putDouble(1.5);                                      // double payload
        final byte[] s = "hi".getBytes(StandardCharsets.UTF_8);
        buf.putInt(s.length).put(s);                             // string payload
        buf.flip();

        final ClientBinaryReader r = new ClientBinaryReader(buf);
        r.skipValue(ClientBinaryReader.tagNull());
        r.skipValue(ClientBinaryReader.tagBoolean());
        r.skipValue(ClientBinaryReader.tagInt());
        r.skipValue(ClientBinaryReader.tagDouble());
        r.skipValue(ClientBinaryReader.tagString());
        assertFalse(buf.hasRemaining());
    }
}
