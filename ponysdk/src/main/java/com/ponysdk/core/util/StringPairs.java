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

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Static operations on a flat, exactly-sized {@code String[]} of key/value pairs
 * ({@code [k0, v0, k1, v1, ...]}), used where a widget holds a handful of entries: the array itself is
 * the whole structure (no wrapper object, no spare capacity). {@code null} is the empty map. Keys are
 * non-null, values may be null. Insertion order is kept.
 * <p>
 * Not thread-safe.
 */
public final class StringPairs {

    private StringPairs() {
    }

    /** @return the index of {@code key} (a key slot, even), or -1 */
    public static int indexOf(final String[] pairs, final String key) {
        if (pairs == null) return -1;
        for (int i = 0; i < pairs.length; i += 2) {
            if (pairs[i].equals(key)) return i;
        }
        return -1;
    }

    public static String get(final String[] pairs, final String key) {
        final int i = indexOf(pairs, key);
        return i < 0 ? null : pairs[i + 1];
    }

    public static boolean containsKey(final String[] pairs, final String key) {
        return indexOf(pairs, key) >= 0;
    }

    /** @return a new array with {@code key=value} appended (the key must be absent) */
    public static String[] append(final String[] pairs, final String key, final String value) {
        final String[] out;
        if (pairs == null) {
            out = new String[2];
        } else {
            out = Arrays.copyOf(pairs, pairs.length + 2);
        }
        out[out.length - 2] = key;
        out[out.length - 1] = value;
        return out;
    }

    /** @return a new array without the pair at {@code index} (a key slot), or null if it becomes empty */
    public static String[] removeAt(final String[] pairs, final int index) {
        if (pairs.length == 2) return null;
        final String[] out = new String[pairs.length - 2];
        System.arraycopy(pairs, 0, out, 0, index);
        System.arraycopy(pairs, index + 2, out, index, pairs.length - index - 2);
        return out;
    }

    public static int size(final String[] pairs) {
        return pairs == null ? 0 : pairs.length >> 1;
    }

    /** Snapshot of the entries, in insertion order (unmodifiable). */
    public static Set<Map.Entry<String, String>> entrySet(final String[] pairs) {
        if (pairs == null) return Collections.emptySet();
        final Set<Map.Entry<String, String>> set = new LinkedHashSet<>();
        for (int i = 0; i < pairs.length; i += 2) {
            set.add(new AbstractMap.SimpleImmutableEntry<>(pairs[i], pairs[i + 1]));
        }
        return Collections.unmodifiableSet(set);
    }
}
