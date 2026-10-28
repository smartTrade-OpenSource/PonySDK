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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class SetPoolTest {

    private final SetPool<String> pool = new SetPool<>();
    private final SetPool<String>.ImmutableSet empty = pool.emptyImmutableSet();

    private static Set<String> ordered(final String... values) {
        return new LinkedHashSet<>(List.of(values));
    }

    @Test
    public void addAndRemoveGiveThePooledInstance() {
        final SetPool<String>.ImmutableSet ab = empty.getAdd("a").getAdd("b");
        assertEquals(Set.of("a", "b"), ab);
        assertSame(ab, empty.getAdd("b").getAdd("a"), "same content, same instance");
        assertSame(ab, ab.getAdd("a"), "adding a present element is a no-op");
        assertSame(ab, pool.add(ab, "b"));

        final SetPool<String>.ImmutableSet a = ab.getRemove("b");
        assertEquals(Set.of("a"), a);
        assertSame(a, empty.getAdd("a"));
        assertSame(a, a.getRemove("z"), "removing an absent element is a no-op");
        assertSame(empty, a.getRemove("a"));
        assertSame(empty, ab.getClear());
    }

    @Test
    public void memoizedTransitionsAnswerTheSameInstance() {
        final SetPool<String>.ImmutableSet a = empty.getAdd("a");
        for (int i = 0; i < 3; i++) {
            assertSame(a, empty.getAdd("a"));
            assertSame(empty, a.getRemove("a"));
        }
        // Elements colliding on the memo slot do not answer each other's result
        final SetPool<String>.ImmutableSet e = empty.getAdd("e");
        assertEquals(Set.of("e"), e);
        assertEquals(Set.of("a"), empty.getAdd("a"));
    }

    @Test
    public void nullIsAnElementLikeAnyOther() {
        final SetPool<String>.ImmutableSet withNull = empty.getAdd(null);
        assertEquals(1, withNull.size());
        assertTrue(withNull.contains(null));
        assertSame(withNull, empty.getAdd(null));
        assertSame(empty, withNull.getRemove(null));
    }

    @Test
    public void addAllIsAUnionPooledOnce() {
        final SetPool<String>.ImmutableSet a = empty.getAdd("a");
        final SetPool<String>.ImmutableSet abc = a.getAddAll(ordered("b", "c"));
        assertEquals(Set.of("a", "b", "c"), abc);
        assertSame(abc, a.getAddAll(ordered("b", "c")), "the same union must be the same pooled instance");
        assertSame(abc, pool.addAll(empty.getAdd("b"), ordered("a", "c")));
        assertSame(abc, a.getAdd("b").getAdd("c"));
        assertSame(abc, abc.getAddAll(ordered("a", "b")), "a subset adds nothing");
    }

    @Test
    public void addAllOfAnExistingUnionBuildsNoNewSet() {
        final int[] created = new int[1];
        final SetPool<String> counting = new SetPool<>(capacity -> {
            created[0]++;
            return SetUtils.newArraySet(capacity);
        });
        final SetPool<String>.ImmutableSet a = counting.emptyImmutableSet().getAdd("a");
        final SetPool<String>.ImmutableSet b = counting.emptyImmutableSet().getAdd("b");
        final SetPool<String>.ImmutableSet ab = a.getAddAll(Set.of("b"));
        final int before = created[0];

        assertSame(ab, a.getAddAll(Set.of("b")));
        assertSame(ab, b.getAddAll(Set.of("a")));
        assertEquals(before, created[0], "a pool hit must not build the set again");
    }

    @Test
    public void retainAllIsAnIntersection() {
        final SetPool<String>.ImmutableSet abc = empty.getAddAll(ordered("a", "b", "c"));
        final SetPool<String>.ImmutableSet ab = abc.getRetainAll(ordered("a", "b", "z"));
        assertEquals(Set.of("a", "b"), ab);
        assertSame(ab, abc.getRetainAll(ordered("b", "a")));
        assertSame(ab, empty.getAdd("a").getAdd("b"));
        assertSame(abc, pool.retainAll(abc, ordered("a", "b", "c", "d")), "a subset of the argument is kept");
        assertSame(empty, abc.getRetainAll(Set.of()));
    }

    @Test
    public void retainAllDoesNotAnswerAnotherSetWithTheSameHash() {
        // "Aa" and "BB" share their hash code: {BB} is a pooled set with the size and hash of {Aa}
        final SetPool<String>.ImmutableSet bb = empty.getAdd("BB");
        final SetPool<String>.ImmutableSet both = empty.getAddAll(ordered("Aa", "BB"));

        final SetPool<String>.ImmutableSet retained = both.getRetainAll(Set.of("Aa"));

        assertEquals(Set.of("Aa"), retained);
        assertEquals(Set.of("BB"), bb);
    }

    @Test
    public void removeAllIsADifference() {
        final SetPool<String>.ImmutableSet abc = empty.getAddAll(ordered("a", "b", "c"));
        final SetPool<String>.ImmutableSet c = abc.getRemoveAll(ordered("a", "b", "z"));
        assertEquals(Set.of("c"), c);
        assertSame(c, empty.getAdd("c"));
        assertSame(c, pool.removeAll(abc, ordered("b", "a")));
        assertSame(abc, abc.getRemoveAll(ordered("x", "y")), "no intersection removes nothing");
    }

    @Test
    public void setsWithTheSameHashStayDistinct() {
        final SetPool<String>.ImmutableSet aa = empty.getAdd("Aa");
        final SetPool<String>.ImmutableSet bb = empty.getAdd("BB");
        assertNotSame(aa, bb);
        assertEquals(aa.hashCode(), bb.hashCode());
        assertNotEquals(aa, bb);
        assertSame(aa, bb.getRemove("BB").getAdd("Aa"));
        assertEquals(Set.of("Aa", "BB"), aa.getAdd("BB"));
        assertEquals(Set.of("Aa", "BB"), aa.getAddAll(Set.of("BB")));
        assertEquals(Set.of("BB"), aa.getAdd("BB").getRemoveAll(Set.of("Aa")));
        assertEquals(Set.of("Aa"), aa.getAdd("BB").getRemove("BB"));
    }

    @Test
    public void bulkOperationsDoNotAnswerAnotherSetWithTheSameHash() {
        // {x, Aa} and {x, BB} have the same size and hash: each one, built first, must not answer for the other
        final SetPool<String>.ImmutableSet x = empty.getAdd("x");
        final SetPool<String>.ImmutableSet xbb = x.getAdd("BB");
        final SetPool<String>.ImmutableSet xaa = x.getAddAll(Set.of("Aa"));
        assertEquals(Set.of("x", "Aa"), xaa);

        final SetPool<String>.ImmutableSet y = empty.getAdd("y");
        final SetPool<String>.ImmutableSet yaa = y.getAdd("Aa");
        final SetPool<String>.ImmutableSet yaabb = yaa.getAdd("BB");
        assertEquals(Set.of("y", "BB"), yaabb.getRemoveAll(Set.of("Aa")));
        assertEquals(Set.of("x", "BB"), xbb);
    }

    @Test
    public void removalsDoNotAnswerAnotherSetWithTheSameHash() {
        // {z, Aa} is pooled: removing Aa from {z, Aa, BB} gives {z, BB}, of the same size and hash
        final SetPool<String>.ImmutableSet za = empty.getAdd("z").getAdd("Aa");
        assertEquals(Set.of("z", "BB"), za.getAdd("BB").getRemove("Aa"));

        // {x, BB} is pooled: retaining {x, Aa, BB} of {x, y, Aa} gives {x, Aa}
        final SetPool<String>.ImmutableSet xbb = empty.getAdd("x").getAdd("BB");
        final SetPool<String>.ImmutableSet xya = empty.getAdd("y").getAdd("Aa").getAdd("x");
        assertEquals(Set.of("x", "Aa"), xya.getRetainAll(Set.of("x", "Aa", "BB")));

        // "AaAa", "BBBB", "AaBB" and "BBAa" share their hash: {AaBB, BBAa} is pooled
        final SetPool<String>.ImmutableSet mixed = empty.getAdd("AaBB").getAdd("BBAa");
        final SetPool<String>.ImmutableSet w = empty.getAdd("w").getAdd("AaAa").getAdd("BBBB");
        assertEquals(Set.of("AaAa", "BBBB"), w.getRemoveAll(Set.of("w")));
        assertEquals(Set.of("AaBB", "BBAa"), mixed);
        assertEquals(Set.of("x", "BB"), xbb);
    }

    @Test
    public void behavesAsAReadOnlySet() {
        final SetPool<String>.ImmutableSet ab = empty.getAddAll(ordered("a", "b"));
        assertEquals(2, ab.size());
        assertFalse(ab.isEmpty());
        assertTrue(empty.isEmpty());
        assertTrue(ab.containsAll(List.of("a", "b")));
        assertFalse(ab.containsAll(List.of("a", "z")));
        assertEquals(List.of("a", "b"), List.of(ab.toArray()));
        assertArrayEquals(new String[] { "a", "b" }, ab.toArray(new String[0]));
        assertEquals(Set.of("a", "b"), ab.stream().collect(Collectors.toSet()));
        assertEquals(Set.of("a", "b"), ab.parallelStream().collect(Collectors.toSet()));
        assertEquals(2, ab.spliterator().estimateSize());
        assertEquals("[a, b]", ab.toString());

        final List<String> seen = new ArrayList<>();
        ab.forEach(seen::add);
        final Iterator<String> iterator = ab.iterator();
        assertTrue(iterator.hasNext());
        seen.add(iterator.next());
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        iterator.forEachRemaining(seen::add);
        assertEquals(List.of("a", "b", "a", "b"), seen);

        assertEquals(new HashSet<>(List.of("a", "b")), ab);
        assertEquals(ab, new HashSet<>(List.of("a", "b")));
        assertEquals(ab, ab);
        assertNotEquals(ab, List.of("a", "b"));
        assertNotEquals(ab, Set.of("a"));
        assertNotEquals(ab, Set.of("a", "z"));
        assertEquals(Set.of("a", "b").hashCode(), ab.hashCode());
    }

    @Test
    public void mutatorsAreUnsupported() {
        final SetPool<String>.ImmutableSet a = empty.getAdd("a");
        assertThrows(UnsupportedOperationException.class, () -> a.add("b"));
        assertThrows(UnsupportedOperationException.class, () -> a.remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> a.addAll(List.of("b")));
        assertThrows(UnsupportedOperationException.class, () -> a.retainAll(List.of("b")));
        assertThrows(UnsupportedOperationException.class, () -> a.removeAll(List.of("a")));
        assertThrows(UnsupportedOperationException.class, a::clear);
        assertThrows(UnsupportedOperationException.class, () -> a.removeIf(s -> true));
        assertEquals(Set.of("a"), a);
    }

    @Test
    public void joinSeparatesTheElementsWithASpaceOnce() {
        final SetPool<String>.ImmutableSet classes = empty.getAddAll(ordered("btn", "primary"));
        final String joined = classes.join();
        assertEquals("btn primary", joined);
        assertSame(joined, classes.join(), "computed once per pooled set");
        assertEquals("", empty.join());
    }

    @Test
    public void customFactoryAndCapacity() {
        final SetPool<Integer> hashPool = new SetPool<>(HashSet::new);
        final SetPool<Integer>.ImmutableSet one = hashPool.emptyImmutableSet().getAdd(1);
        assertSame(one, hashPool.emptyImmutableSet().getAdd(1));
        assertEquals(Set.of(1, 2), one.getAdd(2));

        final SetPool<Integer> sized = new SetPool<>(HashSet::new, 4);
        assertEquals(Set.of(3), sized.emptyImmutableSet().getAdd(3));
    }

    @Test
    public void aRemovalDoesNotAnswerASetWithAnElementTheOriginalLacks() {
        final SetPool<String>.ImmutableSet fresh = new SetPool<String>().emptyImmutableSet();
        final SetPool<String>.ImmutableSet bb = fresh.getAdd("BB");
        final SetPool<String>.ImmutableSet aa = fresh.getAdd("x").getAdd("Aa").getRemove("x");
        assertEquals(Set.of("Aa"), aa);
        assertNotSame(bb, aa);
    }
}
