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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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

        assertFalse(InternResult.isExisting(first), "first occurrence is new");
        assertTrue(InternResult.isExisting(second), "second occurrence is existing");
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

    @Test @Timeout(value = 5_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void evictionRemovesLeastFrequentlyUsedEntry() {
        final StringDictionary dict = new StringDictionary(2, 1, 32);
        // "a" used 3 times, "b" once → "b" is the LFU
        dict.internOrGet("a");
        dict.internOrGet("a");
        dict.internOrGet("a");
        final long bResult = dict.internOrGet("b");

        // Dictionary full → interning "c" must evict the LFU ("b"), reusing its id
        final long cResult = dict.internOrGet("c");

        assertEquals(0, dict.getId("a"), "the frequent entry survives");
        assertEquals(StringDictionary.NOT_INTERNED, dict.getId("b"), "the LFU entry was evicted");
        assertEquals(InternResult.id(bResult), InternResult.id(cResult), "evicted id is reused");
        assertEquals("c", dict.getString(InternResult.id(cResult)));
        assertEquals(2, dict.size());
    }

    @Test @Timeout(value = 5_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
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
    public void hitRateCountsTheUsesServedAsAReference() {
        final StringDictionary dict = new StringDictionary();
        assertEquals(0, dict.getSessionHitRate());
        dict.intern("alpha"); // added
        dict.intern("alpha"); // reference
        dict.intern("alpha"); // reference
        dict.intern("beta1"); // added

        assertEquals(50, dict.getSessionHitRate(), "2 references out of 4 uses");
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

    @Test @Timeout(value = 5_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
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

    // ---- LFU scan (int-array, rotating cursor) ----

    @Test @Timeout(value = 5_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void evictionPicksTheColdestEntryWhateverItsPosition() {
        final StringDictionary dict = new StringDictionary(4, 1, 32);
        // ids a=0 b=1 c=2 d=3; counts a=3, b=3, c=2 (coldest, in the middle), d=3
        for (final String s : new String[] { "a", "b", "c", "d" }) dict.internOrGet(s);
        for (final String s : new String[] { "a", "b", "d" }) {
            dict.internOrGet(s);
            dict.internOrGet(s);
        }
        dict.internOrGet("c");

        // getString has no side effect (getId/intern would bump the usage count)
        final int cId = 2;
        assertEquals("c", dict.getString(cId));
        final long e = dict.internOrGet("e");

        assertEquals(cId, InternResult.id(e), "the coldest entry's id is recycled");
        assertEquals(StringDictionary.NOT_INTERNED, dict.getId("c"));
        for (final String s : new String[] { "a", "b", "d" }) {
            assertNotEquals(StringDictionary.NOT_INTERNED, dict.getId(s), s + " survives");
        }
    }

    @Test @Timeout(value = 5_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void evictionRotatesAmongEquallyColdEntries() {
        // All entries used once: successive evictions must not keep recycling the same id
        final StringDictionary dict = new StringDictionary(3, 1, 32);
        dict.internOrGet("a");
        dict.internOrGet("b");
        dict.internOrGet("c");

        final int first = InternResult.id(dict.internOrGet("x"));
        // "x" now has count 1 too; bump it so the next scan must move past it
        dict.internOrGet("x");
        final int second = InternResult.id(dict.internOrGet("y"));

        assertNotEquals(first, second, "the cursor advances after an eviction");
        assertEquals("x", dict.getString(first));
        assertEquals("y", dict.getString(second));
        assertEquals(3, dict.size());
    }

    @Test @Timeout(value = 5_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void evictionSkipsPreSeededEntriesButEvictsRegularOnes() {
        final SharedDictionaryProvider provider = Mockito.mock(SharedDictionaryProvider.class);
        Mockito.when(provider.getPreSeedStrings()).thenReturn(List.of("seed0", "seed1"));
        final StringDictionary dict = new StringDictionary(3, 1, 32);
        dict.initFromSharedProvider(provider);

        final int regular = InternResult.id(dict.internOrGet("regular"));
        // Pre-seeded entries are never used here (count 0, colder than "regular") yet must not be evicted
        final int next = InternResult.id(dict.internOrGet("next"));

        assertEquals(regular, next, "only the regular entry is evictable");
        assertEquals(0, dict.intern("seed0"));
        assertEquals(1, dict.intern("seed1"));
    }

    @Test @Timeout(value = 10_000, unit = TimeUnit.MILLISECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void sustainedChurnKeepsMappingsConsistent() {
        // Full dictionary + steady stream of new strings: the forward and reverse maps must stay in sync
        final StringDictionary dict = new StringDictionary(64, 1, 1_000_000);
        for (int i = 0; i < 10_000; i++) {
            final String s = "v" + (i % 200);
            final long r = dict.internOrGet(s);
            final int id = InternResult.id(r);
            assertNotEquals(StringDictionary.NOT_INTERNED, id);
            assertEquals(s, dict.getString(id));
            assertTrue(dict.size() <= 64);
        }
        assertEquals(64, dict.size());
    }

    // ---- client sync reset (transparent reconnection) ----

    @Test
    public void resetClientSyncForcesReAnnouncementWithSameIds() {
        final StringDictionary dict = new StringDictionary();
        final long first = dict.internOrGet("payload");
        assertTrue(InternResult.isExisting(dict.internOrGet("payload")));
        assertEquals(InternResult.id(first), dict.getId("payload"));

        dict.resetClientSync();

        // getId (short-string path) must not hand out a ref the client may not know
        assertEquals(StringDictionary.NOT_INTERNED, dict.getId("payload"));
        // next use is a DICTIONARY_ADD again, with the same id
        final long again = dict.internOrGet("payload");
        assertFalse(InternResult.isExisting(again), "must be re-announced");
        assertEquals(InternResult.id(first), InternResult.id(again));
        // ...and only once
        assertTrue(InternResult.isExisting(dict.internOrGet("payload")));
        assertEquals(1, dict.size());
    }

    @Test
    public void resetClientSyncKeepsUsageCounts() {
        final SharedDictionaryProvider provider = Mockito.mock(SharedDictionaryProvider.class);
        Mockito.when(provider.getPreSeedStrings()).thenReturn(List.of());
        final StringDictionary dict = new StringDictionary(16, 1, 32);
        dict.initFromSharedProvider(provider);
        dict.internOrGet("hot");
        dict.internOrGet("hot");

        dict.resetClientSync();

        assertEquals(Long.valueOf(2L), dict.getSessionFrequency().get("hot"));
    }

    @Test
    public void secondUseAdmissionSendsTheFirstUseInline() {
        final StringDictionary d = new StringDictionary(64, 4);
        d.setAdmitOnSecondUse(true);
        assertEquals(StringDictionary.InternResult.NOT_FOUND, d.internOrGet("unique tooltip"));
        assertEquals(0, d.size(), "nothing retained for a single use");
        final long second = d.internOrGet("unique tooltip");
        assertFalse(StringDictionary.InternResult.isExisting(second)); // DICTIONARY_ADD now
        assertTrue(StringDictionary.InternResult.isExisting(d.internOrGet("unique tooltip")));
        assertEquals(1, d.size());
    }

    @Test
    public void admittedStringsShareOneInstanceAcrossSessions() {
        final StringDictionary a = new StringDictionary(64, 4), b = new StringDictionary(64, 4);
        a.setAdmitOnSecondUse(true);
        b.setAdmitOnSecondUse(true);
        for (final StringDictionary d : new StringDictionary[] { a, b }) {
            d.internOrGet(new String("EUR/USD 1.10850"));
            d.internOrGet(new String("EUR/USD 1.10850"));
        }
        final String sa = a.getString(StringDictionary.InternResult.id(a.internOrGet("EUR/USD 1.10850")));
        final String sb = b.getString(StringDictionary.InternResult.id(b.internOrGet("EUR/USD 1.10850")));
        assertEquals("EUR/USD 1.10850", sa);
        assertSame(sa, sb);
    }

    @Test
    public void firstUseAdmissionStaysTheDefaultOfTheClass() {
        final StringDictionary d = new StringDictionary(64, 4);
        assertFalse(d.isAdmitOnSecondUse());
        assertFalse(StringDictionary.InternResult.isExisting(d.internOrGet("first time")));
        assertEquals(1, d.size());
    }

    @Test
    public void manyOneOffStringsStillLetRepeatedOnesIn() {
        final StringDictionary d = new StringDictionary(4096, 4);
        d.setAdmitOnSecondUse(true);
        int admitted = 0;
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 300; i++) d.internOrGet("one-off " + round + "-" + i);
            for (int i = 0; i < 300; i++) d.internOrGet("price " + i);
        }
        for (int i = 0; i < 300; i++) if (d.getId("price " + i) != StringDictionary.NOT_INTERNED) admitted++;
        assertTrue(admitted > 250, "repeated values admitted: " + admitted);
    }

    @Test
    public void theLimitsAreCheckedAndReadBack() {
        assertThrows(IllegalArgumentException.class, () -> new StringDictionary(0, 4, 1));
        assertThrows(IllegalArgumentException.class, () -> new StringDictionary(8, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new StringDictionary(8, 4, -1));

        final StringDictionary dict = new StringDictionary(8, 3);
        assertEquals(8, dict.getMaxSize());
        assertEquals(3, dict.getMinStringLength());
        assertEquals(StringDictionary.NOT_INTERNED, dict.getId(null));
        assertFalse(dict.isPreSeeded(-1));
    }

    @Test
    public void internOfADictionaryFullOfPreSeededEntriesInternsNothing() {
        final SharedDictionaryProvider provider = Mockito.mock(SharedDictionaryProvider.class);
        Mockito.when(provider.getPreSeedStrings()).thenReturn(List.of("alpha", "beta"));
        final StringDictionary dict = new StringDictionary(2, 1, 32);
        dict.initFromSharedProvider(provider);

        assertEquals(StringDictionary.NOT_INTERNED, dict.intern("gamma"));
        assertEquals(2, dict.size());
    }

    @Test
    public void internReusesTheSlotOfTheEntryItEvicts() {
        final StringDictionary dict = new StringDictionary(2, 1, 32);
        final int cold = dict.intern("cold");
        dict.intern("hot");
        dict.intern("hot");

        final int id = dict.intern("new");

        assertEquals(cold, id);
        assertEquals("new", dict.getString(id));
        assertEquals(2, dict.size());
        assertEquals(Map.of("hot", 2L, "new", 1L), dict.getSessionFrequency());
    }

    @Test
    public void preSeedingSkipsEmptyAndRepeatedStringsAndStopsAtTheMaximumSize() {
        final SharedDictionaryProvider provider = Mockito.mock(SharedDictionaryProvider.class);
        Mockito.when(provider.getPreSeedStrings()).thenReturn(java.util.Arrays.asList("dup", null, "", "dup", "x", "y"));
        final StringDictionary dict = new StringDictionary(2, 1, 32);

        dict.initFromSharedProvider(provider);

        assertEquals(2, dict.getPreSeedCount());
        assertEquals("dup", dict.getString(0));
        assertEquals("x", dict.getString(1));
    }

    @Test
    public void withoutProviderNothingIsPreSeededNorFlushed() {
        final StringDictionary dict = new StringDictionary(8, 1, 32);
        dict.initFromSharedProvider(null);
        dict.intern("value");

        dict.flushToSharedProvider();

        assertEquals(0, dict.getPreSeedCount());
        assertEquals(0, dict.intern("value"));
    }

    @Test
    public void everyUseOfAPreSeededEntryIsAReference() {
        final SharedDictionaryProvider provider = Mockito.mock(SharedDictionaryProvider.class);
        Mockito.when(provider.getPreSeedStrings()).thenReturn(List.of("seed", "idle"));
        final StringDictionary dict = new StringDictionary(8, 1, 32);
        dict.initFromSharedProvider(provider);
        dict.intern("seed");
        dict.intern("seed");
        dict.intern("fresh");

        assertEquals(66, dict.getSessionHitRate(), "2 references out of 3 uses");
        assertEquals(Map.of("seed", 2L, "fresh", 1L), dict.getSessionFrequency(), "an unused entry is left out");
    }

    @Test
    public void anEntryTakingTheSlotOfAFlushedOneIsFlushedWithItsOwnUses() {
        final SharedDictionaryProvider provider = Mockito.mock(SharedDictionaryProvider.class);
        Mockito.when(provider.getPreSeedStrings()).thenReturn(List.of());
        final StringDictionary dict = new StringDictionary(2, 1, 32);
        dict.initFromSharedProvider(provider);
        dict.intern("first");
        dict.intern("second");
        dict.flushToSharedProvider();

        dict.intern("third"); // evicts "first"
        dict.flushToSharedProvider();

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, Long>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(provider, Mockito.times(2)).recordBulkUsage(captor.capture());
        assertEquals(Map.of("third", 1L), captor.getAllValues().get(1));
    }

    @Test
    public void entriesAddedAfterAFlushAreFlushedNextTime() {
        final SharedDictionaryProvider provider = Mockito.mock(SharedDictionaryProvider.class);
        Mockito.when(provider.getPreSeedStrings()).thenReturn(List.of());
        final StringDictionary dict = new StringDictionary(1024, 1, 32);
        dict.initFromSharedProvider(provider);
        dict.intern("first");
        dict.flushToSharedProvider();
        for (int i = 0; i < 100; i++) dict.intern("later " + i); // beyond the arrays sized at the first flush

        dict.flushToSharedProvider();
        dict.flushToSharedProvider(); // nothing new

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, Long>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(provider, Mockito.times(2)).recordBulkUsage(captor.capture());
        assertEquals(Map.of("first", 1L), captor.getAllValues().get(0));
        assertEquals(100, captor.getAllValues().get(1).size());
        assertEquals(Long.valueOf(1L), captor.getAllValues().get(1).get("later 99"));
    }
}
