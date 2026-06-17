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

package com.ponysdk.core.util;

import java.util.*;

/**
 * A compact String→String map backed by a flat String[] array.
 * Keys are stored at even indices, values at odd indices.
 * <p>
 * Optimized for very small maps (1-8 entries) where HashMap overhead
 * (16-byte Node objects, hash table array, load factor) dominates.
 * Linear scan is faster than hashing for small N.
 * <p>
 * Memory: ~(2*N + 1) references vs HashMap's ~(16*N + table) bytes.
 */
public final class CompactStringMap {

    private String[] data;
    private int size;

    public CompactStringMap() {
        this.data = new String[4]; // room for 2 entries initially
        this.size = 0;
    }

    /**
     * Puts a key-value pair. Returns the previous value, or null.
     */
    public String put(final String key, final String value) {
        for (int i = 0; i < size * 2; i += 2) {
            if (data[i].equals(key)) {
                final String old = data[i + 1];
                data[i + 1] = value;
                return old;
            }
        }
        ensureCapacity();
        data[size * 2] = key;
        data[size * 2 + 1] = value;
        size++;
        return null;
    }

    /**
     * Gets the value for a key, or null if not found.
     */
    public String get(final String key) {
        for (int i = 0; i < size * 2; i += 2) {
            if (data[i].equals(key)) return data[i + 1];
        }
        return null;
    }

    /**
     * Removes a key. Returns the previous value, or null.
     */
    public String remove(final String key) {
        for (int i = 0; i < size * 2; i += 2) {
            if (data[i].equals(key)) {
                final String old = data[i + 1];
                final int last = (size - 1) * 2;
                // Move last entry into this slot
                data[i] = data[last];
                data[i + 1] = data[last + 1];
                data[last] = null;
                data[last + 1] = null;
                size--;
                return old;
            }
        }
        return null;
    }

    public boolean containsKey(final String key) {
        for (int i = 0; i < size * 2; i += 2) {
            if (data[i].equals(key)) return true;
        }
        return false;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns an unmodifiable set of entries for iteration.
     */
    public Set<Map.Entry<String, String>> entrySet() {
        if (size == 0) return Collections.emptySet();
        final Set<Map.Entry<String, String>> set = new LinkedHashSet<>(size);
        for (int i = 0; i < size * 2; i += 2) {
            set.add(new AbstractMap.SimpleImmutableEntry<>(data[i], data[i + 1]));
        }
        return Collections.unmodifiableSet(set);
    }

    private void ensureCapacity() {
        if (size * 2 >= data.length) {
            data = Arrays.copyOf(data, data.length * 2);
        }
    }
}
