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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link CompactStringMap} — the flat-array String→String map used for small
 * web-component property/attribute sets. Exercises put/get/update, the swap-remove (including
 * the remove-last and remove-first edge cases), array growth, and entrySet.
 */
public class CompactStringMapTest {

    @Test
    public void putGetAndSize() {
        final CompactStringMap m = new CompactStringMap();
        assertTrue(m.isEmpty());
        assertNull(m.put("a", "1"));
        assertEquals("1", m.get("a"));
        assertEquals(1, m.size());
        assertFalse(m.isEmpty());
        assertTrue(m.containsKey("a"));
        assertNull("missing key", m.get("zzz"));
        assertFalse(m.containsKey("zzz"));
    }

    @Test
    public void putExistingReturnsOldAndUpdates() {
        final CompactStringMap m = new CompactStringMap();
        m.put("k", "old");
        assertEquals("old", m.put("k", "new"));
        assertEquals("new", m.get("k"));
        assertEquals("update does not grow size", 1, m.size());
    }

    @Test
    public void removeReturnsOldAndShrinks() {
        final CompactStringMap m = new CompactStringMap();
        m.put("k", "v");
        assertEquals("v", m.remove("k"));
        assertEquals(0, m.size());
        assertTrue(m.isEmpty());
        assertNull(m.get("k"));
        assertFalse(m.containsKey("k"));
        assertNull("removing a missing key returns null", m.remove("k"));
    }

    @Test
    public void removeLastEntryEdgeCase() {
        // Removing the last entry hits the i == last self-assignment path in swap-remove.
        final CompactStringMap m = new CompactStringMap();
        m.put("a", "1");
        m.put("b", "2");
        assertEquals("2", m.remove("b")); // b is the last entry
        assertEquals(1, m.size());
        assertEquals("1", m.get("a"));
        assertFalse(m.containsKey("b"));
    }

    @Test
    public void removeFirstOfManyKeepsOthers() {
        // Swap-remove moves the last entry into the removed slot — no data must be lost.
        final CompactStringMap m = new CompactStringMap();
        m.put("a", "1");
        m.put("b", "2");
        m.put("c", "3");
        assertEquals("1", m.remove("a")); // c gets swapped into a's slot
        assertEquals(2, m.size());
        assertFalse(m.containsKey("a"));
        assertEquals("2", m.get("b"));
        assertEquals("3", m.get("c"));
    }

    @Test
    public void growsBeyondInitialCapacity() {
        final CompactStringMap m = new CompactStringMap(); // initial room for 2 entries
        for (int i = 0; i < 25; i++) {
            assertNull(m.put("key" + i, "val" + i));
        }
        assertEquals(25, m.size());
        for (int i = 0; i < 25; i++) {
            assertEquals("val" + i, m.get("key" + i));
        }
    }

    @Test
    public void entrySetReflectsContent() {
        final CompactStringMap m = new CompactStringMap();
        m.put("a", "1");
        m.put("b", "2");
        m.put("c", "3");
        m.remove("b");
        m.put("d", "4");

        final Map<String, String> snapshot = new HashMap<>();
        m.entrySet().forEach(e -> snapshot.put(e.getKey(), e.getValue()));

        assertEquals(Map.of("a", "1", "c", "3", "d", "4"), snapshot);
        assertEquals(3, m.size());
    }

    @Test
    public void entrySetIsEmptyForEmptyMap() {
        assertTrue(new CompactStringMap().entrySet().isEmpty());
    }

    @Test
    public void interleavedPutRemoveStaysConsistent() {
        final CompactStringMap m = new CompactStringMap();
        for (int i = 0; i < 10; i++) m.put("k" + i, "v" + i);
        // Remove the even keys
        for (int i = 0; i < 10; i += 2) assertEquals("v" + i, m.remove("k" + i));
        assertEquals(5, m.size());
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0) assertFalse(m.containsKey("k" + i));
            else assertEquals("v" + i, m.get("k" + i));
        }
    }
}
