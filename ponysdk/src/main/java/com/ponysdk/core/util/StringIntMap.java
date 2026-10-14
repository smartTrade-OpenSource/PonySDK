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
    private int[] values;
    /**
     * Mixed hash of each slot's key. Probes compare it first, so a lookup only dereferences the key
     * String (object header + byte[] in other cache lines) of a slot whose hash already matches.
     */
    private int[] hashes;
    private int size;
    private int mask;
    private int resizeAt;

    public StringIntMap() {
        this(MIN_CAPACITY);
    }

    public StringIntMap(final int expectedSize) {
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
        hashes = new int[capacity];
        mask = capacity - 1;
        resizeAt = capacity >> 1;
    }

    private static int mix(final int h) {
        // spread the String hash so that low bits are usable as an index
        final int x = h * 0x9E3779B9;
        return x ^ (x >>> 16);
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
        final int[] hs = hashes;
        final int h = mix(key.hashCode());
        int i = h & mask;
        String cur;
        while ((cur = k[i]) != null) {
            if (hs[i] == h && (cur == key || cur.equals(key))) return values[i];
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
            if (hashes[i] == h && (cur == key || cur.equals(key))) {
                final int old = values[i];
                values[i] = value;
                return old;
            }
            i = (i + 1) & mask;
        }
        keys[i] = key;
        values[i] = value;
        hashes[i] = h;
        if (++size > resizeAt) rehash(keys.length << 1);
        return MISSING;
    }

    /** @return the removed value, or {@link #MISSING} */
    public int remove(final String key) {
        final int h = mix(key.hashCode());
        int i = h & mask;
        String cur;
        while ((cur = keys[i]) != null) {
            if (hashes[i] == h && (cur == key || cur.equals(key))) {
                final int old = values[i];
                shiftBack(i);
                size--;
                return old;
            }
            i = (i + 1) & mask;
        }
        return MISSING;
    }

    /** Backward-shift deletion: moves later entries of the probe chain into the freed slot. */
    private void shiftBack(int hole) {
        int i = hole;
        while (true) {
            i = (i + 1) & mask;
            final String k = keys[i];
            if (k == null) break;
            final int home = hashes[i] & mask;
            // entry at i may move to the hole if its home is not in (hole, i] (cyclically)
            final boolean between = hole <= i ? (hole < home && home <= i) : (hole < home || home <= i);
            if (!between) {
                keys[hole] = k;
                values[hole] = values[i];
                hashes[hole] = hashes[i];
                hole = i;
            }
        }
        keys[hole] = null;
        values[hole] = 0;
        hashes[hole] = 0;
    }

    private void rehash(final int newCapacity) {
        final String[] oldKeys = keys;
        final int[] oldValues = values;
        final int[] oldHashes = hashes;
        allocate(newCapacity);
        for (int j = 0; j < oldKeys.length; j++) {
            final String k = oldKeys[j];
            if (k == null) continue;
            int i = oldHashes[j] & mask;
            while (keys[i] != null) i = (i + 1) & mask;
            keys[i] = k;
            values[i] = oldValues[j];
            hashes[i] = oldHashes[j];
        }
    }

    public void clear() {
        Arrays.fill(keys, null);
        Arrays.fill(values, 0);
        Arrays.fill(hashes, 0);
        size = 0;
    }
}
