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

package com.ponysdk.core.server.util;

import java.util.Arrays;
import java.util.function.IntFunction;

/**
 * Open-addressing hash map with {@code int} keys and object values.
 * <p>
 * Uses linear probing for collision resolution. No boxing of keys,
 * no per-entry Node objects — significantly less memory and GC pressure
 * than {@code HashMap<Integer, V>}.
 * </p>
 * <p>
 * Memory savings vs HashMap per entry:
 * <ul>
 *   <li>No Integer boxing: -16 bytes</li>
 *   <li>No HashMap.Node: -32 bytes</li>
 *   <li>Total: ~48 bytes saved per entry</li>
 * </ul>
 * </p>
 * <p>
 * Thread safety: NOT thread-safe. Caller must synchronize externally.
 * </p>
 *
 * @param <V> the value type
 */
public class IntObjectHashMap<V> {

    private static final int FREE = 0;
    private static final float LOAD_FACTOR = 0.75f;
    private static final int DEFAULT_CAPACITY = 16;

    /** Parallel arrays: keys[i] and values[i] form a pair. */
    private int[] keys;
    @SuppressWarnings("unchecked")
    private V[] values;

    /** State flags: true = occupied (even if key == FREE sentinel). */
    private boolean[] occupied;

    private int size;
    private int threshold;

    public IntObjectHashMap() {
        this(DEFAULT_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public IntObjectHashMap(final int initialCapacity) {
        final int cap = tableSizeFor(Math.max(initialCapacity, DEFAULT_CAPACITY));
        keys = new int[cap];
        values = (V[]) new Object[cap];
        occupied = new boolean[cap];
        threshold = (int) (cap * LOAD_FACTOR);
    }

    public V get(final int key) {
        final int mask = keys.length - 1;
        int idx = mix(key) & mask;
        while (true) {
            if (!occupied[idx]) return null;
            if (keys[idx] == key) return values[idx];
            idx = (idx + 1) & mask;
        }
    }

    public V put(final int key, final V value) {
        if (size >= threshold) rehash(keys.length << 1);
        return doPut(key, value);
    }

    private V doPut(final int key, final V value) {
        final int mask = keys.length - 1;
        int idx = mix(key) & mask;
        while (true) {
            if (!occupied[idx]) {
                keys[idx] = key;
                values[idx] = value;
                occupied[idx] = true;
                size++;
                return null;
            }
            if (keys[idx] == key) {
                final V old = values[idx];
                values[idx] = value;
                return old;
            }
            idx = (idx + 1) & mask;
        }
    }

    public V remove(final int key) {
        final int mask = keys.length - 1;
        int idx = mix(key) & mask;
        while (true) {
            if (!occupied[idx]) return null;
            if (keys[idx] == key) {
                final V old = values[idx];
                // Remove and fix the probe chain (backward-shift deletion)
                removeAt(idx, mask);
                return old;
            }
            idx = (idx + 1) & mask;
        }
    }

    /**
     * Backward-shift deletion: instead of tombstones, shift subsequent
     * entries back to fill the gap. This keeps probe chains short and
     * avoids the need for periodic rehashing to clear tombstones.
     */
    private void removeAt(int idx, final int mask) {
        size--;
        int next = (idx + 1) & mask;
        while (occupied[next]) {
            final int natural = mix(keys[next]) & mask;
            // If 'next' is displaced from its natural position past 'idx',
            // move it back to fill the gap
            if ((next > idx && (natural <= idx || natural > next)) ||
                (next < idx && (natural <= idx && natural > next))) {
                keys[idx] = keys[next];
                values[idx] = values[next];
                occupied[idx] = true;
                idx = next;
            }
            next = (next + 1) & mask;
        }
        keys[idx] = FREE;
        values[idx] = null;
        occupied[idx] = false;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(final int key) {
        final int mask = keys.length - 1;
        int idx = mix(key) & mask;
        while (true) {
            if (!occupied[idx]) return false;
            if (keys[idx] == key) return true;
            idx = (idx + 1) & mask;
        }
    }

    /**
     * Iterates over all entries. The consumer receives (key, value) pairs.
     */
    public void forEach(final IntObjectConsumer<V> consumer) {
        for (int i = 0; i < keys.length; i++) {
            if (occupied[i]) {
                consumer.accept(keys[i], values[i]);
            }
        }
    }

    /**
     * Gets the value for the key, or computes and stores it if absent.
     */
    public V computeIfAbsent(final int key, final IntFunction<V> mappingFunction) {
        final V existing = get(key);
        if (existing != null) return existing;
        final V newValue = mappingFunction.apply(key);
        if (newValue != null) put(key, newValue);
        return newValue;
    }

    @SuppressWarnings("unchecked")
    private void rehash(final int newCapacity) {
        final int cap = tableSizeFor(newCapacity);
        final int[] oldKeys = keys;
        final V[] oldValues = values;
        final boolean[] oldOccupied = occupied;

        keys = new int[cap];
        values = (V[]) new Object[cap];
        occupied = new boolean[cap];
        threshold = (int) (cap * LOAD_FACTOR);
        size = 0;

        for (int i = 0; i < oldKeys.length; i++) {
            if (oldOccupied[i]) {
                doPut(oldKeys[i], oldValues[i]);
            }
        }
    }

    /** Fibonacci hashing — better distribution than modulo for linear probing. */
    private static int mix(final int key) {
        return key * 0x9E3779B9; // golden ratio × 2^31
    }

    /** Round up to next power of 2. */
    private static int tableSizeFor(final int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n < DEFAULT_CAPACITY ? DEFAULT_CAPACITY : n + 1;
    }

    @FunctionalInterface
    public interface IntObjectConsumer<V> {
        void accept(int key, V value);
    }
}
