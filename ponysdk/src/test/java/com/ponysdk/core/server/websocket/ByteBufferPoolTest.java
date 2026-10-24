/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.websocket;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

public class ByteBufferPoolTest {

    @Test
    public void reusesReturnedBuffersAndKeepsAtMostItsSize() {
        final ByteBufferPool pool = new ByteBufferPool(1024, 2);
        final ByteBuffer a = pool.acquire();
        final ByteBuffer b = pool.acquire();
        final ByteBuffer c = pool.acquire();
        assertEquals(3, pool.totalAllocated());

        a.put((byte) 1);
        pool.release(a);
        pool.release(b);
        pool.release(c); // full: discarded
        pool.release(ByteBuffer.allocateDirect(2048)); // another capacity: discarded
        assertEquals(2, pool.availableCount());

        final ByteBuffer again = pool.acquire();
        assertSame(a, again);
        assertEquals(0, again.position());
        assertEquals(3, pool.totalAllocated());
    }

    /** A flush borrows and returns one buffer: the pool allocates nothing for it (a linked queue took a node). */
    @Test
    public void acquireAndReleaseAllocateNothing() {
        final com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        final ByteBufferPool pool = new ByteBufferPool(1024, 4);
        for (int i = 0; i < 100_000; i++) pool.release(pool.acquire()); // warm up
        final long before = threads.getCurrentThreadAllocatedBytes();
        for (int i = 0; i < 100_000; i++) pool.release(pool.acquire());
        final long allocated = threads.getCurrentThreadAllocatedBytes() - before;
        assertTrue(allocated < 100_000, "allocated " + allocated + " B for 100,000 flushes");
    }
}
