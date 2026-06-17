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

package com.ponysdk.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link OffHeapJsonStore} — covers the off-heap put/read/remove lifecycle,
 * the no-change delta sentinel, buffer growth and compaction, and resource release.
 */
public class OffHeapJsonStoreTest {

    @Test
    public void putThenReadReturnsValue() {
        final OffHeapJsonStore store = new OffHeapJsonStore();
        final long previous = store.put("k", "{\"a\":1}");

        assertEquals("new key has no previous hash", 0L, previous);
        assertEquals("{\"a\":1}", store.readValue("k"));
        assertTrue(store.containsKey("k"));
        assertEquals(1, store.size());
        assertFalse(store.isEmpty());
    }

    @Test
    public void readMissingKeyReturnsNull() {
        final OffHeapJsonStore store = new OffHeapJsonStore();
        assertNull(store.readValue("absent"));
        assertFalse(store.containsKey("absent"));
        assertTrue(store.isEmpty());
    }

    @Test
    public void overwriteReplacesValueAndKeepsSingleEntry() {
        final OffHeapJsonStore store = new OffHeapJsonStore();
        store.put("k", "old-value");
        final long oldHash = store.put("k", "new-value");

        assertTrue("overwrite returns a non-zero previous hash", oldHash != 0L);
        assertEquals("new-value", store.readValue("k"));
        assertEquals(1, store.size());
    }

    @Test
    public void puttingIdenticalValueIsANoOp() {
        final OffHeapJsonStore store = new OffHeapJsonStore();
        final long firstHash = store.put("k", "same");
        final long secondHash = store.put("k", "same");

        // The no-change path returns the existing hash (the "no change" sentinel)
        assertEquals(secondHash, store.put("k", "same"));
        assertTrue(secondHash != 0L);
        assertEquals("same", store.readValue("k"));
        // first put was a new key → 0
        assertEquals(0L, firstHash);
    }

    @Test
    public void hasSameValueDetectsEqualityAndDifference() {
        final OffHeapJsonStore store = new OffHeapJsonStore();
        store.put("k", "{\"x\":42}");

        assertTrue(store.hasSameValue("k", "{\"x\":42}"));
        assertFalse(store.hasSameValue("k", "{\"x\":43}"));
        assertFalse("missing key is never equal", store.hasSameValue("missing", "{\"x\":42}"));
    }

    @Test
    public void removeDropsEntry() {
        final OffHeapJsonStore store = new OffHeapJsonStore();
        store.put("k", "v");

        assertTrue(store.remove("k"));
        assertFalse(store.containsKey("k"));
        assertEquals(0, store.size());
        assertNull(store.readValue("k"));
        assertFalse("removing an absent key returns false", store.remove("k"));
    }

    @Test
    public void multipleKeysAreIndependent() {
        final OffHeapJsonStore store = new OffHeapJsonStore();
        store.put("a", "1");
        store.put("b", "2");
        store.put("c", "3");

        assertEquals(3, store.size());
        assertEquals("1", store.readValue("a"));
        assertEquals("2", store.readValue("b"));
        assertEquals("3", store.readValue("c"));
        assertTrue(store.keys().containsAll(java.util.Set.of("a", "b", "c")));
    }

    @Test
    public void handlesUtf8AndEmptyValues() {
        final OffHeapJsonStore store = new OffHeapJsonStore();
        store.put("emoji", "\"caf\u00e9 \uD83D\uDE80\"");
        store.put("empty", "");

        assertEquals("\"caf\u00e9 \uD83D\uDE80\"", store.readValue("emoji"));
        assertEquals("", store.readValue("empty"));
        assertTrue(store.containsKey("empty"));
    }

    @Test
    public void growsBeyondInitialCapacity() {
        final OffHeapJsonStore store = new OffHeapJsonStore(64); // tiny initial buffer
        final int initialCapacity = store.offHeapBytes();

        final StringBuilder big = new StringBuilder();
        for (int i = 0; i < 500; i++) big.append("0123456789"); // 5000 chars > 64 and > 4096
        final String bigValue = big.toString();
        store.put("big", bigValue);

        assertTrue("buffer must have grown", store.offHeapBytes() > initialCapacity);
        assertEquals(bigValue, store.readValue("big"));
    }

    @Test
    public void survivesManyOverwritesTriggeringCompaction() {
        final OffHeapJsonStore store = new OffHeapJsonStore(256);

        // Repeatedly overwrite the same key with growing values to accumulate wasted bytes,
        // forcing the store to compact/grow. The latest value must always read back correctly.
        String last = "";
        for (int i = 0; i < 200; i++) {
            last = "value-" + i + "-" + "x".repeat(i % 40);
            store.put("k", last);
        }

        assertEquals(1, store.size());
        assertEquals(last, store.readValue("k"));
    }

    @Test
    public void releaseClearsTheStore() {
        final OffHeapJsonStore store = new OffHeapJsonStore();
        store.put("a", "1");
        store.put("b", "2");

        store.release();

        assertEquals(0, store.size());
        assertTrue(store.isEmpty());
    }

    @Test
    public void offHeapBytesReflectsCapacity() {
        final OffHeapJsonStore store = new OffHeapJsonStore(4096);
        assertEquals(4096, store.offHeapBytes());
        assertTrue(store.onHeapOverheadEstimate() >= 0);
    }

    @Test
    public void overwritingLargeValueDuringGrowDoesNotOverflow() {
        // Regression: overwriting a large value (larger than the grow cushion) with one big enough
        // to force a grow used to under-allocate the new buffer. The stale entry was still copied by
        // the compaction inside ensureCapacity, while the new capacity was computed as if that entry
        // were already reclaimed (its bytes were added to wastedBytes) — so the write below
        // overflowed (BufferOverflowException). This is exactly the "update a large off-heap
        // dataset" path that off-heap storage is meant for.
        final OffHeapJsonStore store = new OffHeapJsonStore(32 * 1024); // 32 KB

        final String oldValue = "a".repeat(6_000);   // > 4 KB cushion and <= cap/4 (so no in-place compact)
        store.put("dataset", oldValue);
        assertEquals(oldValue, store.readValue("dataset"));

        final String newValue = "b".repeat(62_000);  // large enough to force a grow
        store.put("dataset", newValue);               // must not throw BufferOverflowException

        assertEquals(newValue, store.readValue("dataset"));
        assertEquals(1, store.size());
    }
}
