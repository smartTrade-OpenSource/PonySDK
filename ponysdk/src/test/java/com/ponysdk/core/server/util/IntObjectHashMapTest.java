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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

public class IntObjectHashMapTest {

    @Test
    public void emptyMapHasNoEntries() {
        final IntObjectHashMap<String> map = new IntObjectHashMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get(1));
        assertFalse(map.containsKey(1));
        assertNull(map.remove(1));
    }

    @Test
    public void putGetReplaceAndRemove() {
        final IntObjectHashMap<String> map = new IntObjectHashMap<>();
        assertNull(map.put(7, "a"));
        assertEquals("a", map.get(7));
        assertTrue(map.containsKey(7));
        assertFalse(map.isEmpty());

        assertEquals("a", map.put(7, "b"));
        assertEquals(1, map.size());
        assertEquals("b", map.get(7));

        assertEquals("b", map.remove(7));
        assertNull(map.get(7));
        assertFalse(map.containsKey(7));
        assertTrue(map.isEmpty());
    }

    @Test
    public void zeroAndNegativeKeysAreRegularKeys() {
        final IntObjectHashMap<String> map = new IntObjectHashMap<>();
        map.put(0, "zero");
        map.put(-1, "minus one");
        map.put(Integer.MIN_VALUE, "min");

        assertEquals("zero", map.get(0));
        assertEquals("minus one", map.get(-1));
        assertEquals("min", map.get(Integer.MIN_VALUE));
        assertTrue(map.containsKey(0));
        assertEquals("zero", map.remove(0));
        assertFalse(map.containsKey(0));
        assertEquals(2, map.size());
    }

    @Test
    public void growsPastItsInitialCapacity() {
        final IntObjectHashMap<Integer> map = new IntObjectHashMap<>(2);
        for (int i = 0; i < 1_000; i++) {
            map.put(i, i * 10);
        }
        assertEquals(1_000, map.size());
        for (int i = 0; i < 1_000; i++) {
            assertEquals(i * 10, map.get(i));
        }
    }

    @Test
    public void forEachVisitsEveryEntryOnce() {
        final IntObjectHashMap<String> map = new IntObjectHashMap<>(100);
        final Map<Integer, String> expected = new HashMap<>();
        for (int i = -50; i < 50; i++) {
            map.put(i, "v" + i);
            expected.put(i, "v" + i);
        }
        final Map<Integer, String> visited = new HashMap<>();
        map.forEach((key, value) -> assertNull(visited.put(key, value)));
        assertEquals(expected, visited);
    }

    @Test
    public void computeIfAbsentComputesOnlyForMissingKeys() {
        final IntObjectHashMap<String> map = new IntObjectHashMap<>();
        final AtomicInteger calls = new AtomicInteger();

        final String first = map.computeIfAbsent(3, key -> {
            calls.incrementAndGet();
            return "v" + key;
        });
        final String second = map.computeIfAbsent(3, key -> {
            calls.incrementAndGet();
            return "other";
        });

        assertEquals("v3", first);
        assertSame(first, second);
        assertEquals(1, calls.get());
    }

    @Test
    public void computeIfAbsentDoesNotStoreANullValue() {
        final IntObjectHashMap<String> map = new IntObjectHashMap<>();
        assertNull(map.computeIfAbsent(4, key -> null));
        assertFalse(map.containsKey(4));
        assertEquals(0, map.size());
    }

    /**
     * Random puts and removes on a dense key range force long probe chains and wrap-around, so removals
     * exercise the backward-shift deletion. The map must agree with a HashMap at every step.
     */
    @Test
    public void behavesLikeAHashMapUnderRandomPutsAndRemoves() {
        final IntObjectHashMap<Integer> map = new IntObjectHashMap<>();
        final Map<Integer, Integer> reference = new HashMap<>();
        final Random random = new Random(42);

        for (int i = 0; i < 20_000; i++) {
            final int key = random.nextInt(64) - 32;
            if (random.nextInt(3) == 0) {
                assertEquals(reference.remove(key), map.remove(key));
            } else {
                assertEquals(reference.put(key, i), map.put(key, i));
            }
            assertEquals(reference.size(), map.size());
        }
        for (int key = -32; key < 32; key++) {
            assertEquals(reference.get(key), map.get(key));
            assertEquals(reference.containsKey(key), map.containsKey(key));
        }
    }

}
