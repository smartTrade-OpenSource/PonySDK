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

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

public class StringIntMapTest {

    @Test
    public void putGetRemoveBasics() {
        final StringIntMap m = new StringIntMap();
        assertTrue(m.isEmpty());
        assertEquals(StringIntMap.MISSING, m.put("a", 1));
        assertEquals(1, m.put("a", 2));
        assertEquals(2, m.get("a"));
        assertEquals(StringIntMap.MISSING, m.get("b"));
        assertTrue(m.containsKey("a"));
        assertEquals(1, m.size());
        assertEquals(2, m.remove("a"));
        assertEquals(StringIntMap.MISSING, m.remove("a"));
        assertFalse(m.containsKey("a"));
        assertEquals(0, m.size());
        assertThrows(NullPointerException.class, () -> m.put(null, 1));
    }

    @Test
    public void equalButDistinctKeyInstancesMatch() {
        final StringIntMap m = new StringIntMap();
        m.put(new String("price"), 7);
        assertEquals(7, m.get(new StringBuilder("pri").append("ce").toString()));
    }

    @Test
    public void collidingHashesSurviveRemovalsAnywhereInTheChain() {
        // "Aa" and "BB" have the same String.hashCode: build long collision chains
        final String[] keys = { "AaAa", "AaBB", "BBAa", "BBBB", "AaAaAa", "AaAaBB", "AaBBAa", "BBBBBB" };
        for (int removed = 0; removed < keys.length; removed++) {
            final StringIntMap m = new StringIntMap();
            for (int i = 0; i < keys.length; i++) m.put(keys[i], i);
            m.remove(keys[removed]);
            for (int i = 0; i < keys.length; i++) {
                assertEquals(i == removed ? StringIntMap.MISSING : i, m.get(keys[i]), "removed=" + removed + " key=" + keys[i]);
            }
        }
    }

    @Test
    public void randomOperationsMatchHashMap() {
        final Random random = new Random(1234);
        final StringIntMap m = new StringIntMap();
        final Map<String, Integer> ref = new HashMap<>();
        for (int op = 0; op < 200_000; op++) {
            final String key = "k" + random.nextInt(3_000);
            switch (random.nextInt(3)) {
                case 0 -> {
                    final int v = random.nextInt(1_000_000);
                    final Integer old = ref.put(key, v);
                    assertEquals(old == null ? StringIntMap.MISSING : old, m.put(key, v));
                }
                case 1 -> {
                    final Integer old = ref.remove(key);
                    assertEquals(old == null ? StringIntMap.MISSING : old, m.remove(key));
                }
                default -> {
                    final Integer v = ref.get(key);
                    assertEquals(v == null ? StringIntMap.MISSING : v, m.get(key));
                }
            }
            assertEquals(ref.size(), m.size());
        }
        for (final Map.Entry<String, Integer> e : ref.entrySet()) assertEquals((int) e.getValue(), m.get(e.getKey()));
        m.clear();
        assertEquals(0, m.size());
        assertEquals(StringIntMap.MISSING, m.get(ref.keySet().iterator().next()));
    }

    @Test
    public void packedModeMatchesAHashMapUnderChurn() {
        final java.util.Random rnd = new java.util.Random(11);
        for (final int maxValue : new int[] {0, 1, 255, 4095, (1 << 24) - 1}) {
            final StringIntMap m = new StringIntMap(4, maxValue);
            final java.util.Map<String, Integer> ref = new java.util.HashMap<>();
            for (int op = 0; op < 20_000; op++) {
                final String key = "k" + rnd.nextInt(300);
                final int v = rnd.nextInt(maxValue + 1);
                switch (rnd.nextInt(3)) {
                    case 0 -> {
                        final Integer old = ref.put(key, v);
                        assertEquals(old == null ? StringIntMap.MISSING : old, m.put(key, v));
                    }
                    case 1 -> {
                        final Integer old = ref.remove(key);
                        assertEquals(old == null ? StringIntMap.MISSING : old, m.remove(key));
                    }
                    default -> {
                        final Integer cur = ref.get(key);
                        assertEquals(cur == null ? StringIntMap.MISSING : cur, m.get(key));
                    }
                }
                assertEquals(ref.size(), m.size());
            }
        }
    }

    @Test
    public void packedModeRejectsAValueAboveItsBound() {
        assertThrows(IllegalArgumentException.class, () ->
            new StringIntMap(4, 255).put("x", 1 << 20));
    }
}
