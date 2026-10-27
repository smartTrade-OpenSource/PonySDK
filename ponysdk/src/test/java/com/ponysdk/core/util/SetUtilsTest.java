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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class SetUtilsTest {

    @Test
    public void arraySetKeepsOneOfEachElementInInsertionOrder() {
        final Set<String> set = SetUtils.newArraySet(List.of("b", "a", "b"));
        assertEquals(List.of("b", "a"), List.copyOf(set));
        assertFalse(set.add("a"));
        assertTrue(set.add("c"));
        assertFalse(set.addAll(List.of("a", "b")));
        assertTrue(set.addAll(List.of("a", "d")));
        assertEquals(List.of("b", "a", "c", "d"), List.copyOf(set));
    }

    @Test
    public void arraySetFollowsTheSetContract() {
        final Set<String> set = SetUtils.newArraySet(4);
        set.add("x");
        set.add(null);
        assertEquals(Set.of("x").hashCode(), set.hashCode(), "null counts 0 in the hash");
        assertEquals(new HashSet<>(Arrays.asList("x", null)), set);
        assertEquals(set, new HashSet<>(Arrays.asList(null, "x")));
        assertEquals(set, set);
        assertNotEquals(set, Arrays.asList("x", null), "a list is not a set");
        assertNotEquals(set, Set.of("x"));
        assertNotEquals(set, new HashSet<>(Arrays.asList("y", null)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void arraySetCannotBeUsedAsAList() {
        final List<String> asList = (List<String>) SetUtils.<String> newArraySet();
        asList.add("a");
        assertThrows(UnsupportedOperationException.class, () -> asList.set(0, "b"));
        assertThrows(UnsupportedOperationException.class, () -> asList.add(0, "b"));
        assertThrows(UnsupportedOperationException.class, () -> asList.addAll(0, List.of("b")));
        assertThrows(UnsupportedOperationException.class, () -> asList.listIterator());
        assertThrows(UnsupportedOperationException.class, () -> {
            final ListIterator<String> unused = asList.listIterator(0);
        });
        assertEquals(List.of("a"), asList);
    }
}
