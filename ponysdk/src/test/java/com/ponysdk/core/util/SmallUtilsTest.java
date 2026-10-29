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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** The small helpers of the package: references, mapped views, key codes, string pairs and search, the compact map. */
public class SmallUtilsTest {

    @Test
    public void referenceHoldsAMutableValue() {
        final Reference<String> ref = new Reference<>("a");
        assertEquals("a", ref.get());
        ref.set("b");
        assertEquals("b", ref.get());
    }

    @Test
    public void mappedViewsFollowTheirSource() {
        final List<Integer> source = new ArrayList<>(List.of(1, 2));
        final MappedCollection<Integer, String> collection = new MappedCollection<>(source, i -> "#" + i);
        final MappedList<Integer, String> list = new MappedList<>(source, i -> "#" + i);
        assertEquals(List.of("#1", "#2"), new ArrayList<>(collection));
        assertEquals(List.of("#1", "#2"), list);

        source.add(3);
        assertEquals(3, collection.size());
        assertEquals(List.of("#1", "#2", "#3"), new ArrayList<>(collection));
        assertEquals("#3", list.get(2));
        assertEquals(3, list.size());
    }

    @Test
    public void keyCodesOfDigitsLettersAndKeypad() {
        assertEquals("0", KeyCodeUtil.getString(48));
        assertEquals("Z", KeyCodeUtil.getString(90));
        assertEquals("9", KeyCodeUtil.getString(105));
        assertNull(KeyCodeUtil.getString(13));
    }

    @Test
    public void stringPairsAsAnOrderedMap() {
        assertEquals(-1, StringPairs.indexOf(null, "a"));
        assertNull(StringPairs.get(null, "a"));
        assertEquals(0, StringPairs.size(null));
        assertTrue(StringPairs.entrySet(null).isEmpty());

        String[] pairs = StringPairs.append(null, "a", "1");
        pairs = StringPairs.append(pairs, "b", null);
        pairs = StringPairs.append(pairs, "c", "3");
        assertEquals(3, StringPairs.size(pairs));
        assertEquals(2, StringPairs.indexOf(pairs, "b"));
        assertTrue(StringPairs.containsKey(pairs, "b"));
        assertNull(StringPairs.get(pairs, "b"));
        assertEquals("3", StringPairs.get(pairs, "c"));
        assertFalse(StringPairs.containsKey(pairs, "z"));

        final Set<Map.Entry<String, String>> entries = StringPairs.entrySet(pairs);
        assertEquals(List.of(new AbstractMap.SimpleImmutableEntry<>("a", "1"), new AbstractMap.SimpleImmutableEntry<>("b", null),
            new AbstractMap.SimpleImmutableEntry<>("c", "3")), new ArrayList<>(entries));
        assertThrows(UnsupportedOperationException.class, entries::clear);

        pairs = StringPairs.removeAt(pairs, 2);
        assertArrayEquals(new String[] { "a", "1", "c", "3" }, pairs);
        pairs = StringPairs.removeAt(pairs, 0);
        assertArrayEquals(new String[] { "c", "3" }, pairs);
        assertNull(StringPairs.removeAt(pairs, 0), "the last pair removed is the empty map");
    }

    @Test
    public void containsIgnoreCaseOnAsciiLetters() {
        assertTrue(StringUtils.containsIgnoreCase("Hello World", "WORLD"));
        assertTrue(StringUtils.containsIgnoreCase("HELLO", "ell"));
        assertTrue(StringUtils.containsIgnoreCase("aaB", "AB"), "restarts after a partial match");
        assertTrue(StringUtils.containsIgnoreCase("x-1_y", "-1_Y"));
        assertTrue(StringUtils.containsIgnoreCase("abc", ""));
        assertTrue(StringUtils.containsIgnoreCase("abc", "ABC"));
        assertFalse(StringUtils.containsIgnoreCase("abc", "abcd"));
        assertFalse(StringUtils.containsIgnoreCase("abc", "x"));
        assertFalse(StringUtils.containsIgnoreCase("abc", "bd"));
        assertFalse(StringUtils.containsIgnoreCase("a[c", "A{C"), "only letters fold their case");
    }

    @Test
    public void compactMapReplacesInPlaceAndKeepsAppendedValues() {
        final CompactIntRunnableMap map = new CompactIntRunnableMap(1);
        final List<String> runs = new ArrayList<>();
        map.put(1, () -> runs.add("one"));
        map.append(() -> runs.add("added"));
        map.put(2, () -> runs.add("two"));
        map.append(() -> runs.add("added again"));
        map.put(1, () -> runs.add("one replaced"));

        map.runAll();
        assertEquals(List.of("one replaced", "added", "two", "added again"), runs);

        runs.clear();
        map.forEachValue(Runnable::run);
        assertEquals(List.of("one replaced", "added", "two", "added again"), runs);
    }

    @Test
    public void charactersOutsideTheAlphabetOnlyMatchThemselves() {
        assertFalse(StringUtils.containsIgnoreCase("{", "}"));
        assertTrue(StringUtils.containsIgnoreCase("{", "{"));
    }
}
