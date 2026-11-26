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

import java.nio.ByteBuffer;
import java.util.ArrayDeque;

/**
 * The bytes written to a WebSocket that its terminal has not acknowledged yet, so that a terminal that reconnects
 * gets again what was in flight when the connection was lost.
 * <p>
 * Offsets count the bytes of the server to terminal stream since the context was created; they carry on from one
 * WebSocket to the next one of the same context. The terminal acknowledges the offset it has received with its
 * heartbeats: the bytes below it are released. At most {@code maxBytes} are held: past it, the oldest bytes are
 * released too, and a terminal that has not received them can no longer resume.
 * <p>
 * Written by a single thread (the one that holds the UIContext); {@link #acknowledge(long)} may be called from any
 * thread.
 */
public final class ReplayBuffer {

    private static final int CHUNK_SIZE = 1 << 14;

    /** The smallest {@code maxBytes}: one chunk. */
    public static final int MIN_SIZE = CHUNK_SIZE;

    private final int maxBytes;
    private final ArrayDeque<byte[]> chunks = new ArrayDeque<>();

    /** Offset of the first byte held (the first byte of the first chunk). */
    private long startOffset;
    /** Offset of the next byte to write. */
    private long endOffset;
    /** Bytes used in the last chunk. */
    private int lastChunkUsed = CHUNK_SIZE;

    private volatile long acknowledgedOffset;

    private ByteBuffer view;
    private ByteBuffer viewSource;

    /**
     * @param startOffset the offset of the first byte this buffer will be given
     * @param maxBytes the bytes held at most
     */
    public ReplayBuffer(final long startOffset, final int maxBytes) {
        if (maxBytes < CHUNK_SIZE) throw new IllegalArgumentException("maxBytes must be at least " + CHUNK_SIZE);
        this.startOffset = startOffset;
        this.endOffset = startOffset;
        this.acknowledgedOffset = startOffset;
        this.maxBytes = maxBytes;
    }

    /** Keeps the bytes [position, position + length[ of the buffer, which is not modified. */
    public void append(final ByteBuffer buffer, final int position, final int length) {
        if (viewSource != buffer) {
            view = buffer.duplicate();
            viewSource = buffer;
        }
        view.limit(position + length).position(position);
        int remaining = length;
        while (remaining > 0) {
            if (lastChunkUsed == CHUNK_SIZE) {
                chunks.addLast(new byte[CHUNK_SIZE]);
                lastChunkUsed = 0;
            }
            final int n = Math.min(remaining, CHUNK_SIZE - lastChunkUsed);
            view.get(chunks.peekLast(), lastChunkUsed, n);
            lastChunkUsed += n;
            remaining -= n;
        }
        endOffset += length;
        release();
    }

    /** Keeps the given bytes. */
    public void append(final byte[] bytes) {
        append(ByteBuffer.wrap(bytes), 0, bytes.length);
    }

    /** Releases the bytes below the given offset, which the terminal has received. Safe from any thread. */
    public void acknowledge(final long offset) {
        if (offset > acknowledgedOffset) acknowledgedOffset = offset;
    }

    private void release() {
        final long acknowledged = Math.min(acknowledgedOffset, endOffset);
        // Keep the chunk being written: only whole chunks before it are released
        while (chunks.size() > 1 && (startOffset + CHUNK_SIZE <= acknowledged || endOffset - startOffset > maxBytes)) {
            chunks.removeFirst();
            startOffset += CHUNK_SIZE;
        }
    }

    /**
     * Returns the bytes from the given offset to the end, or null when they are not all held any more, or when the
     * offset is past the end.
     */
    public byte[] from(final long offset) {
        if (offset < startOffset || offset > endOffset) return null;
        final int length = (int) (endOffset - offset);
        final byte[] result = new byte[length];
        long chunkStart = startOffset;
        int copied = 0;
        for (final byte[] chunk : chunks) {
            final long chunkEnd = chunkStart + CHUNK_SIZE;
            if (chunkEnd > offset && copied < length) {
                final int from = (int) Math.max(0, offset - chunkStart);
                final int n = (int) Math.min(CHUNK_SIZE - from, length - copied);
                System.arraycopy(chunk, from, result, copied, n);
                copied += n;
            }
            chunkStart = chunkEnd;
        }
        return result;
    }

    /** Offset of the next byte to write: the bytes written since the context was created. */
    public long getEndOffset() {
        return endOffset;
    }

    /** Offset of the first byte held. */
    public long getStartOffset() {
        return startOffset;
    }

    /** Bytes held now. */
    public long size() {
        return endOffset - startOffset;
    }
}
