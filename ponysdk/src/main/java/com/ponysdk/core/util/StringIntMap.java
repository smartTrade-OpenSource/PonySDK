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

package com.ponysdk.core.util;

import java.util.Arrays;

/**
 * Open-addressing (linear probing) map from non-null {@link String} keys to {@code int} values.
 * <p>
 * Replaces {@code HashMap<String, Integer>} where it is per-session and hot: no {@code Node} and no
 * boxed {@code Integer} per entry (~48 bytes saved per entry), no unboxing on lookups. Removal uses
 * backward-shift deletion, so there are no tombstones and lookups stay short under churn.
 * <p>
 * Not thread-safe.
 */
public final class StringIntMap {

    /** Returned by {@link #get} when the key is absent. */
    public static final int MISSING = Integer.MIN_VALUE;

    private static final int MIN_CAPACITY = 16;

    private String[] keys;
    /**
     * Per slot: the value, plus (packed mode) the high bits of the key's mixed hash. Probes compare that
     * tag first, so a lookup only dereferences the key String (object header + byte[] in other cache
     * lines) of a slot whose hash already matches. Generic mode keeps full hashes in {@link #hashes}.
     */
    private int[] values;
    private int[] hashes;          // generic mode only (null when packed)
    private final int valueMask;   // packed mode: values are in [0, valueMask]; 0 = generic
    private int size;
    private int mask;
    private int resizeAt;

    public StringIntMap() {
        this(MIN_CAPACITY);
    }

    public StringIntMap(final int expectedSize) {
        this(expectedSize, -1);
    }

    /**
     * @param maxValue if in [0, 2^24), every value stored must lie in [0, maxValue]: the hash tag then
     *                 shares the value's int (one int per slot instead of two). Negative = any int value.
     */
    public StringIntMap(final int expectedSize, final int maxValue) {
        if (maxValue >= 0 && maxValue < (1 << 24)) {
            this.valueMask = (Integer.highestOneBit(Math.max(1, maxValue)) << 1) - 1;
        } else {
            this.valueMask = 0;
        }
        allocate(tableSizeFor(expectedSize));
    }

    private static int tableSizeFor(final int expected) {
        // load factor 0.5 -> short probe sequences
        int cap = MIN_CAPACITY;
        while (cap < (long) expected * 2) cap <<= 1;
        return cap;
    }

    private void allocate(final int capacity) {
        keys = new String[capacity];
        values = new int[capacity];
        hashes = valueMask == 0 ? new int[capacity] : null;
        mask = capacity - 1;
        resizeAt = capacity >> 1;
    }

    private static int mix(final int h) {
        // spread the String hash so that low bits are usable as an index
        final int x = h * 0x9E3779B9;
        return x ^ (x >>> 16);
    }

    /** True if slot i may hold a key of mixed hash h (exact in generic mode, tag match when packed). */
    private boolean hashMatches(final int i, final int h) {
        final int vm = valueMask;
        return vm == 0 ? hashes[i] == h : ((values[i] ^ h) & ~vm) == 0;
    }

    private int valueAt(final int i) {
        final int vm = valueMask;
        return vm == 0 ? values[i] : values[i] & vm;
    }

    private void store(final int i, final int h, final int value) {
        final int vm = valueMask;
        if (vm == 0) {
            values[i] = value;
            hashes[i] = h;
        } else {
            if (value < 0 || value > vm) throw new IllegalArgumentException("value out of range: " + value);
            values[i] = (h & ~vm) | value;
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** @return the value, or {@link #MISSING} */
    public int get(final String key) {
        final String[] k = keys;
        final int h = mix(key.hashCode());
        int i = h & mask;
        String cur;
        while ((cur = k[i]) != null) {
            if (hashMatches(i, h) && (cur == key || cur.equals(key))) return valueAt(i);
            i = (i + 1) & mask;
        }
        return MISSING;
    }

    public boolean containsKey(final String key) {
        return get(key) != MISSING;
    }

    /**
     * Associates {@code value} with {@code key}.
     *
     * @return the previous value, or {@link #MISSING}
     */
    public int put(final String key, final int value) {
        if (key == null) throw new NullPointerException("key");
        final int h = mix(key.hashCode());
        int i = h & mask;
        String cur;
        while ((cur = keys[i]) != null) {
            if (hashMatches(i, h) && (cur == key || cur.equals(key))) {
                final int old = valueAt(i);
                store(i, h, value);
                return old;
            }
            i = (i + 1) & mask;
        }
        store(i, h, value);
        keys[i] = key;
        if (++size > resizeAt) rehash(keys.length << 1);
        return MISSING;
    }

    /** @return the removed value, or {@link #MISSING} */
    public int remove(final String key) {
        final int h = mix(key.hashCode());
        int i = h & mask;
        String cur;
        while ((cur = keys[i]) != null) {
            if (hashMatches(i, h) && (cur == key || cur.equals(key))) {
                final int old = valueAt(i);
                shiftBack(i);
                size--;
                return old;
            }
            i = (i + 1) & mask;
        }
        return MISSING;
    }

    /** Home slot of the key stored at i (String caches its hash: no rehashing of the characters). */
    private int home(final int i) {
        return (valueMask == 0 ? hashes[i] : mix(keys[i].hashCode())) & mask;
    }

    /** Backward-shift deletion: moves later entries of the probe chain into the freed slot. */
    private void shiftBack(int hole) {
        int i = hole;
        while (true) {
            i = (i + 1) & mask;
            final String k = keys[i];
            if (k == null) break;
            final int home = home(i);
            // entry at i may move to the hole if its home is not in (hole, i] (cyclically)
            final boolean between = hole <= i ? (hole < home && home <= i) : (hole < home || home <= i);
            if (!between) {
                keys[hole] = k;
                values[hole] = values[i];
                if (hashes != null) hashes[hole] = hashes[i];
                hole = i;
            }
        }
        keys[hole] = null;
        values[hole] = 0;
        if (hashes != null) hashes[hole] = 0;
    }

    private void rehash(final int newCapacity) {
        final String[] oldKeys = keys;
        final int[] oldValues = values;
        final int[] oldHashes = hashes;
        allocate(newCapacity);
        for (int j = 0; j < oldKeys.length; j++) {
            final String k = oldKeys[j];
            if (k == null) continue;
            final int h = oldHashes != null ? oldHashes[j] : mix(k.hashCode());
            int i = h & mask;
            while (keys[i] != null) i = (i + 1) & mask;
            keys[i] = k;
            values[i] = oldValues[j];
            if (hashes != null) hashes[i] = h;
        }
    }

    public void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, 0);
        if (hashes != null) Arrays.fill(hashes, 0);
        size = 0;
    }
}
