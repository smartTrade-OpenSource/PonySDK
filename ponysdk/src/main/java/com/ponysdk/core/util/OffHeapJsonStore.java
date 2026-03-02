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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Off-heap storage for JSON property values.
 * <p>
 * Stores JSON strings as UTF-8 bytes in direct ByteBuffers (off-heap memory),
 * keeping only a lightweight on-heap index (key → offset/length/hash).
 * This prevents large JSON payloads from pressuring the GC while still
 * enabling delta computation by reading back the previous value on demand.
 * </p>
 * <p>
 * The store uses a single growing direct buffer. When it runs out of space,
 * it compacts (removes gaps from deleted entries) or grows.
 * </p>
 * <p>
 * Thread safety: NOT thread-safe. Must be accessed within the UIContext lock.
 * </p>
 */
public final class OffHeapJsonStore {

    private static final int INITIAL_CAPACITY = 4096;   // 4 KB initial buffer
    private static final int MAX_CAPACITY = 16 << 20;   // 16 MB hard cap per store

    /** On-heap index entry: lightweight metadata per stored value. */
    private static final class Entry {
        int offset;     // position in the direct buffer
        int length;     // byte length of the UTF-8 encoded value
        long hash;      // fast equality check without reading off-heap

        Entry(final int offset, final int length, final long hash) {
            this.offset = offset;
            this.length = length;
            this.hash = hash;
        }
    }

    private final Map<String, Entry> index = new HashMap<>();
    private ByteBuffer buffer;
    private int writePos = 0;       // next write position
    private int wastedBytes = 0;    // bytes from removed/overwritten entries (reclaimable)

    public OffHeapJsonStore() {
        this(INITIAL_CAPACITY);
    }

    public OffHeapJsonStore(final int initialCapacity) {
        buffer = ByteBuffer.allocateDirect(Math.min(initialCapacity, MAX_CAPACITY));
    }

    /**
     * Stores a JSON value off-heap. Returns the previous value's hash, or 0 if new.
     * The actual previous String is NOT returned to avoid bringing it on-heap unnecessarily.
     * Use {@link #hasSameValue(String, String)} or {@link #readValue(String)} when needed.
     */
    public long put(final String key, final String value) {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        final long newHash = fnv1a(bytes);

        final Entry existing = index.get(key);
        final long oldHash;
        if (existing != null) {
            oldHash = existing.hash;
            // If hash matches and length matches, very likely identical — skip
            if (oldHash == newHash && existing.length == bytes.length) {
                // Confirm with actual byte comparison (rare false positive path)
                if (bytesEqual(existing, bytes)) return oldHash; // no change sentinel
            }
            // Mark old space as wasted
            wastedBytes += existing.length;
        } else {
            oldHash = 0;
        }

        // Ensure space
        ensureCapacity(bytes.length);

        // Write bytes
        final int offset = writePos;
        buffer.position(offset);
        buffer.put(bytes);
        writePos += bytes.length;

        // Update index
        index.put(key, new Entry(offset, bytes.length, newHash));
        return oldHash;
    }

    /**
     * Reads a value back from off-heap into a temporary on-heap String.
     * Returns null if the key doesn't exist.
     * The returned String is ephemeral — use it and let it be GC'd.
     */
    public String readValue(final String key) {
        final Entry entry = index.get(key);
        if (entry == null) return null;
        return readString(entry);
    }

    /**
     * Checks if a key exists in the store.
     */
    public boolean containsKey(final String key) {
        return index.containsKey(key);
    }

    /**
     * Returns true if the stored value for the key has the same hash as the given value.
     * Fast check that avoids reading off-heap bytes in most cases.
     */
    public boolean hasSameValue(final String key, final String value) {
        final Entry entry = index.get(key);
        if (entry == null) return false;
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (entry.length != bytes.length) return false;
        if (entry.hash != fnv1a(bytes)) return false;
        return bytesEqual(entry, bytes);
    }

    /**
     * Removes a key and marks its space as reclaimable.
     */
    public boolean remove(final String key) {
        final Entry removed = index.remove(key);
        if (removed != null) {
            wastedBytes += removed.length;
            return true;
        }
        return false;
    }

    /**
     * Returns the set of stored keys (for iteration during enrichForUpdate).
     */
    public Set<String> keys() {
        return index.keySet();
    }

    public int size() {
        return index.size();
    }

    public boolean isEmpty() {
        return index.isEmpty();
    }

    /**
     * Returns approximate off-heap bytes used (including wasted space).
     */
    public int offHeapBytes() {
        return buffer.capacity();
    }

    /**
     * Returns approximate on-heap overhead (index entries only).
     */
    public int onHeapOverheadEstimate() {
        // ~80 bytes per HashMap.Node + Entry object
        return index.size() * 80;
    }

    /**
     * Releases the direct buffer. Call when the PWebComponent is destroyed.
     */
    public void release() {
        index.clear();
        // Direct ByteBuffers are freed by the GC via Cleaner, but we can help
        // by nulling the reference and triggering a compact if needed.
        buffer = null;
        writePos = 0;
        wastedBytes = 0;
    }

    // ---- Internal ----

    private void ensureCapacity(final int needed) {
        if (writePos + needed <= buffer.capacity()) return;

        // Try compacting first if >25% is wasted
        if (wastedBytes > buffer.capacity() / 4) {
            compact();
            if (writePos + needed <= buffer.capacity()) return;
        }

        // Grow: double or fit, capped at MAX_CAPACITY
        final int required = writePos + needed - wastedBytes;
        int newCap = Math.max(buffer.capacity() * 2, required + INITIAL_CAPACITY);
        newCap = Math.min(newCap, MAX_CAPACITY);
        if (newCap < required) {
            throw new IllegalStateException(
                "OffHeapJsonStore: cannot grow beyond " + MAX_CAPACITY + " bytes (need " + required + ")");
        }

        final ByteBuffer newBuf = ByteBuffer.allocateDirect(newCap);
        // Copy only live data via compact
        compactInto(newBuf);
        buffer = newBuf;
    }

    /**
     * Compacts the buffer in-place: moves all live entries to the front,
     * eliminating gaps from removed/overwritten entries.
     */
    private void compact() {
        final ByteBuffer newBuf = ByteBuffer.allocateDirect(buffer.capacity());
        compactInto(newBuf);
        buffer = newBuf;
    }

    private void compactInto(final ByteBuffer target) {
        int pos = 0;
        for (final Entry entry : index.values()) {
            // Read from old buffer
            final byte[] tmp = new byte[entry.length];
            buffer.position(entry.offset);
            buffer.get(tmp);
            // Write to new buffer
            target.position(pos);
            target.put(tmp);
            entry.offset = pos;
            pos += entry.length;
        }
        writePos = pos;
        wastedBytes = 0;
    }

    private String readString(final Entry entry) {
        final byte[] tmp = new byte[entry.length];
        buffer.position(entry.offset);
        buffer.get(tmp);
        return new String(tmp, StandardCharsets.UTF_8);
    }

    private boolean bytesEqual(final Entry entry, final byte[] expected) {
        if (entry.length != expected.length) return false;
        buffer.position(entry.offset);
        for (int i = 0; i < expected.length; i++) {
            if (buffer.get() != expected[i]) return false;
        }
        return true;
    }

    /** FNV-1a 64-bit hash — fast, good distribution, no allocations. */
    private static long fnv1a(final byte[] data) {
        long hash = 0xcbf29ce484222325L;
        for (final byte b : data) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
