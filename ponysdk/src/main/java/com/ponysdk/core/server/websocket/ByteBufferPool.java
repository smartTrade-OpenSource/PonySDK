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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe pool of direct {@link ByteBuffer}s for WebSocket I/O.
 * <p>
 * Instead of each session owning 2 × 32KB DirectByteBuffers permanently,
 * buffers are borrowed on demand and returned after each flush. Since sessions
 * rarely flush simultaneously, a small pool serves hundreds of sessions.
 * </p>
 * <p>
 * Memory savings: 500 sessions × 64KB = 32MB → pool of ~50 buffers × 32KB = 1.6MB
 * </p>
 * <p>
 * Implementation: lock-free {@link ConcurrentLinkedQueue} for borrow/return.
 * Buffers are direct (off-heap) to avoid GC pressure and enable zero-copy
 * sendBinary in Jetty.
 * </p>
 */
public final class ByteBufferPool {

    private static final Logger log = LoggerFactory.getLogger(ByteBufferPool.class);

    private final int bufferCapacity;
    private final int maxPoolSize;
    private final ConcurrentLinkedQueue<ByteBuffer> pool = new ConcurrentLinkedQueue<>();
    private final AtomicInteger totalAllocated = new AtomicInteger();
    private final AtomicInteger pooledCount = new AtomicInteger();

    /**
     * @param bufferCapacity capacity of each buffer in bytes
     * @param maxPoolSize    maximum number of buffers to retain in the pool;
     *                       excess buffers returned after this limit are discarded (GC'd)
     */
    public ByteBufferPool(final int bufferCapacity, final int maxPoolSize) {
        this.bufferCapacity = bufferCapacity;
        this.maxPoolSize = maxPoolSize;
    }

    /**
     * Borrows a buffer from the pool, or allocates a new one if the pool is empty.
     * The returned buffer is cleared and ready for writing.
     */
    public ByteBuffer acquire() {
        final ByteBuffer buf = pool.poll();
        if (buf != null) {
            pooledCount.decrementAndGet();
            buf.clear();
            return buf;
        }
        final int count = totalAllocated.incrementAndGet();
        if (log.isDebugEnabled()) log.debug("Allocating new direct buffer #{} ({}KB)", count, bufferCapacity / 1024);
        return ByteBuffer.allocateDirect(bufferCapacity);
    }

    /**
     * Returns a buffer to the pool. If the pool is full, the buffer is discarded.
     * The buffer must not be used after this call.
     *
     * @param buffer the buffer to return (may be null — no-op)
     */
    public void release(final ByteBuffer buffer) {
        if (buffer == null) return;
        // Only pool buffers that match our capacity (reject grown/oversized buffers)
        if (buffer.capacity() != bufferCapacity) {
            if (log.isDebugEnabled()) log.debug("Discarding oversized buffer ({}KB vs pool {}KB)",
                    buffer.capacity() / 1024, bufferCapacity / 1024);
            return;
        }
        if (pooledCount.get() < maxPoolSize) {
            buffer.clear();
            pool.offer(buffer);
            pooledCount.incrementAndGet();
        }
        // else: discard — let GC reclaim the native memory
    }

    /**
     * Returns the number of buffers currently available in the pool.
     */
    public int availableCount() {
        return pooledCount.get();
    }

    /**
     * Returns the total number of buffers ever allocated by this pool.
     */
    public int totalAllocated() {
        return totalAllocated.get();
    }

    /**
     * Returns the capacity of each buffer in the pool.
     */
    public int bufferCapacity() {
        return bufferCapacity;
    }
}
