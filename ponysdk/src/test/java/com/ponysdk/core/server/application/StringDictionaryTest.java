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

package com.ponysdk.core.server.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.StringDictionary.InternResult;

/**
 * Unit tests for {@link StringDictionary} — interning rules, single-pass internOrGet semantics,
 * LFU eviction with a per-flush budget, pre-seeding, and frequency flush to the shared provider.
 */
public class StringDictionaryTest {

    @Test
    public void internRejectsNullEmptyAndTooShort() {
        final StringDictionary dict = new StringDictionary(); // default min length 4
        assertEquals(StringDictionary.NOT_INTERNED, dict.intern(null));
        assertEquals(StringDictionary.NOT_INTERNED, dict.intern(""));
        assertEquals(StringDictionary.NOT_INTERNED, dict.intern("abc")); // < 4 chars
        assertFalse(dict.canIntern("abc"));
        assertTrue(dict.canIntern("abcd"));
    }

    @Test
    public void internAssignsSequentialIdsAndReverseLookupWorks() {
        final StringDictionary dict = new StringDictionary();
        final int idHello = dict.intern("hello");
        final int idWorld = dict.intern("world");

        assertEquals(0, idHello);
        assertEquals(1, idWorld);
        assertEquals("hello", dict.getString(idHello));
        assertEquals("world", dict.getString(idWorld));
        assertEquals(2, dict.size());
    }

    @Test
    public void internSameStringReturnsSameId() {
        final StringDictionary dict = new StringDictionary();
        assertEquals(dict.intern("repeated"), dict.intern("repeated"));
        assertEquals(1, dict.size());
    }

    @Test
    public void internOrGetReportsNewThenExisting() {
        final StringDictionary dict = new StringDictionary();
        final long first = dict.internOrGet("hello");
        final long second = dict.internOrGet("hello");

        assertFalse("first occurrence is new", InternResult.isExisting(first));
        assertTrue("second occurrence is existing", InternResult.isExisting(second));
        assertEquals(InternResult.id(first), InternResult.id(second));
    }

    @Test
    public void getIdRequiresTheEntryToHaveBeenSentToClient() {
        final StringDictionary dict = new StringDictionary();
        // intern() registers the mapping but does not mark it as sent to the client
        final int id = dict.intern("payload");
        assertEquals(StringDictionary.NOT_INTERNED, dict.getId("payload"));

        // internOrGet() marks it as sent → getId now resolves
        dict.internOrGet("payload");
        assertEquals(id, dict.getId("payload"));
    }

    @Test
    public void isFullReflectsCapacity() {
        final StringDictionary dict = new StringDictionary(2, 1, 32);
        assertEquals(2, dict.getMaxSize());
        dict.intern("a");
        assertFalse(dict.isFull());
        dict.intern("b");
        assertTrue(dict.isFull());
    }

    @Test(timeout = 5_000)
    public void evictionRemovesLeastFrequentlyUsedEntry() {
        final StringDictionary dict = new StringDictionary(2, 1, 32);
        // "a" used 3 times, "b" once → "b" is the LFU
        dict.internOrGet("a");
        dict.internOrGet("a");
        dict.internOrGet("a");
        final long bResult = dict.internOrGet("b");

        // Dictionary full → interning "c" must evict the LFU ("b"), reusing its id
        final long cResult = dict.internOrGet("c");

        assertEquals("the frequent entry survives", 0, dict.getId("a"));
        assertEquals("the LFU entry was evicted", StringDictionary.NOT_INTERNED, dict.getId("b"));
        assertEquals("evicted id is reused", InternResult.id(bResult), InternResult.id(cResult));
        assertEquals("c", dict.getString(InternResult.id(cResult)));
        assertEquals(2, dict.size());
    }

    @Test(timeout = 5_000)
    public void evictionBudgetCapsEvictionsPerFlushCycle() {
        final StringDictionary dict = new StringDictionary(1, 1, 1); // 1 eviction per flush
        dict.internOrGet("a");                 // fills the single slot
        final long b = dict.internOrGet("b");  // evicts "a" (budget = 1)
        assertNotEquals(StringDictionary.NOT_INTERNED, InternResult.id(b));

        // Budget exhausted → "c" cannot be interned (sent inline instead)
        final long c = dict.internOrGet("c");
        assertEquals(StringDictionary.NOT_INTERNED, InternResult.id(c));

        // Resetting the budget allows eviction again
        dict.resetEvictionBudget();
        final long c2 = dict.internOrGet("c");
        assertNotEquals(StringDictionary.NOT_INTERNED, InternResult.id(c2));
        assertEquals("c", dict.getString(InternResult.id(c2)));
    }

    @Test
    public void preSeedingMarksEntriesAndResolvesThem() {
        final SharedDictionaryProvider provider = Mockito.mock(SharedDictionaryProvider.class);
        Mockito.when(provider.getPreSeedStrings()).thenReturn(List.of("alpha", "beta"));

        final StringDictionary dict = new StringDictionary(8, 1, 32);
        dict.initFromSharedProvider(provider);

        assertEquals(2, dict.getPreSeedCount());
        assertTrue(dict.isPreSeeded(0));
        assertTrue(dict.isPreSeeded(1));
        assertFalse(dict.isPreSeeded(2));
        assertEquals(0, dict.intern("alpha")); // resolves to the pre-seeded id
        assertEquals("beta", dict.getString(1));
    }

    @Test(timeout = 5_000)
    public void preSeededEntriesAreNeverEvictedEvenWhenUsed() {
        // Regression: a full dictionary whose lowest-frequency bucket holds only pre-seeded
        // ids must NOT loop forever in evictLFU — it must refuse to evict and return NOT_INTERNED.
        final SharedDictionaryProvider provider = Mockito.mock(SharedDictionaryProvider.class);
        Mockito.when(provider.getPreSeedStrings()).thenReturn(List.of("alpha", "beta"));

        final StringDictionary dict = new StringDictionary(2, 1, 32);
        dict.initFromSharedProvider(provider);

        // Use a pre-seeded string so it enters the frequency index (lowest bucket)
        dict.internOrGet("alpha");

        // Dictionary is full of pre-seeded entries → a new string cannot evict any of them
        final long result = dict.internOrGet("gamma");
        assertEquals(StringDictionary.NOT_INTERNED, InternResult.id(result));
        // The pre-seeded entries are intact
        assertEquals(0, dict.intern("alpha"));
        assertEquals(1, dict.intern("beta"));
    }

    @Test
    public void flushToSharedProviderReportsUsageDeltas() {
        final SharedDictionaryProvider provider = Mockito.mock(SharedDictionaryProvider.class);
        Mockito.when(provider.getPreSeedStrings()).thenReturn(List.of());

        final StringDictionary dict = new StringDictionary(16, 1, 32);
        dict.initFromSharedProvider(provider);
        dict.internOrGet("one");
        dict.internOrGet("one");
        dict.internOrGet("two");

        dict.flushToSharedProvider();

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, Long>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(provider).recordBulkUsage(captor.capture());
        final Map<String, Long> deltas = captor.getValue();
        assertEquals(Long.valueOf(2L), deltas.get("one"));
        assertEquals(Long.valueOf(1L), deltas.get("two"));
    }
}
