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

/**
 * Compact ordered map from int keys to Runnable values, optimized for small sizes (1-8 entries).
 * <p>
 * Replaces LinkedHashMap&lt;Integer, Runnable&gt; for PObject.stackedInstructions.
 * Saves ~150-200 bytes per widget vs LinkedHashMap(4) by avoiding:
 * - HashMap Node objects (32 bytes each)
 * - LinkedHashMap doubly-linked list pointers
 * - Integer boxing for keys
 * <p>
 * Supports put-with-replace semantics: if a key already exists, its value is replaced in-place
 * (preserving insertion order). Values are executed in insertion order via {@link #forEachValue}.
 */
public final class CompactIntRunnableMap {

    private int[] keys;
    private Runnable[] values;
    private int size;

    public CompactIntRunnableMap(final int initialCapacity) {
        keys = new int[initialCapacity];
        values = new Runnable[initialCapacity];
    }

    /**
     * Puts a key-value pair. If the key already exists, replaces the value in-place.
     */
    public void put(final int key, final Runnable value) {
        // Check for existing key (replace semantics)
        for (int i = 0; i < size; i++) {
            if (keys[i] == key) {
                values[i] = value;
                return;
            }
        }
        // New key — append
        if (size >= keys.length) {
            final int newCap = keys.length * 2;
            final int[] newKeys = new int[newCap];
            final Runnable[] newValues = new Runnable[newCap];
            System.arraycopy(keys, 0, newKeys, 0, size);
            System.arraycopy(values, 0, newValues, 0, size);
            keys = newKeys;
            values = newValues;
        }
        keys[size] = key;
        values[size] = value;
        size++;
    }

    /**
     * Executes all values in insertion order.
     */
    public void forEachValue(final java.util.function.Consumer<Runnable> action) {
        for (int i = 0; i < size; i++) {
            action.accept(values[i]);
        }
    }

    /**
     * Runs all stored Runnables in insertion order.
     */
    public void runAll() {
        for (int i = 0; i < size; i++) {
            values[i].run();
        }
    }
}
