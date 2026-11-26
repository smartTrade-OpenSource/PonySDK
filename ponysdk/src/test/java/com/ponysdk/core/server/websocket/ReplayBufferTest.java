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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

public class ReplayBufferTest {

    private static final int CHUNK = 1 << 14;

    private static byte[] bytes(final int from, final int length) {
        final byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (from + i);
        }
        return bytes;
    }

    @Test
    public void givesBackTheBytesFromAnOffset() {
        final ReplayBuffer buffer = new ReplayBuffer(0, 4 * CHUNK);
        buffer.append(bytes(0, 100));

        assertArrayEquals(bytes(40, 60), buffer.from(40));
        assertArrayEquals(bytes(0, 100), buffer.from(0));
        assertArrayEquals(new byte[0], buffer.from(100));
    }

    @Test
    public void anOffsetPastTheEndIsRefused() {
        final ReplayBuffer buffer = new ReplayBuffer(0, 4 * CHUNK);
        buffer.append(bytes(0, 100));

        assertNull(buffer.from(101));
    }

    @Test
    public void offsetsCarryOnFromTheStartOffset() {
        final ReplayBuffer buffer = new ReplayBuffer(1000, 4 * CHUNK);
        buffer.append(bytes(0, 10));

        assertEquals(1010, buffer.getEndOffset());
        assertArrayEquals(bytes(5, 5), buffer.from(1005));
        assertNull(buffer.from(999));
    }

    @Test
    public void bytesAcrossChunksComeBackInOrder() {
        final ReplayBuffer buffer = new ReplayBuffer(0, 8 * CHUNK);
        final byte[] all = bytes(0, 3 * CHUNK + 17);
        // Written in uneven pieces, from a buffer at a non-zero position, as AutoFlushedBuffer does
        final ByteBuffer source = ByteBuffer.allocateDirect(all.length + 10);
        source.position(10);
        source.put(all);
        int written = 0;
        for (final int piece : new int[] { 1, 7, CHUNK - 3, 2 * CHUNK, 12 }) {
            buffer.append(source, 10 + written, piece);
            written += piece;
        }

        assertEquals(all.length, written);
        assertArrayEquals(all, buffer.from(0));
        assertArrayEquals(Arrays.copyOfRange(all, CHUNK + 5, all.length), buffer.from(CHUNK + 5));
    }

    @Test
    public void acknowledgedChunksAreReleased() {
        final ReplayBuffer buffer = new ReplayBuffer(0, 8 * CHUNK);
        buffer.append(bytes(0, 3 * CHUNK));
        buffer.acknowledge(2 * CHUNK + 1);
        buffer.append(bytes(0, 1)); // released on the next write

        assertEquals(2 * CHUNK, buffer.getStartOffset());
        assertNull(buffer.from(CHUNK));
        assertArrayEquals(bytes(1, CHUNK - 1), Arrays.copyOf(buffer.from(2 * CHUNK + 1), CHUNK - 1));
    }

    @Test
    public void theOldestBytesAreReleasedPastTheLimit() {
        final ReplayBuffer buffer = new ReplayBuffer(0, 2 * CHUNK);
        buffer.append(bytes(0, 5 * CHUNK));

        assertEquals(5 * CHUNK, buffer.getEndOffset());
        assertEquals(true, buffer.size() <= 2 * CHUNK);
        assertNull(buffer.from(0));
        assertEquals(buffer.size(), buffer.from(buffer.getStartOffset()).length);
    }
}
