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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ponysdk.core.util.StringIntMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bidirectional mapping between strings and numeric IDs for protocol optimization.
 * <p>
 * This dictionary enables string interning in the WebSocket protocol, replacing
 * frequently used strings with compact numeric IDs to reduce bandwidth consumption.
 * </p>
 * <p>
 * Supports LFU eviction: when the dictionary is full, the least-frequently-used
 * non-pre-seeded entry is evicted to make room for new strings. Evicted IDs are
 * reused, and the client is notified via a DICTIONARY_ADD with the same ID.
 * </p>
 * <p>
 * Thread safety: This class is NOT thread-safe. It should only be accessed
 * within the UIContext lock (via UIContext.execute()).
 * </p>
 */
public class StringDictionary {

    private static final Logger log = LoggerFactory.getLogger(StringDictionary.class);

    /**
     * Sentinel value returned when a string cannot be interned.
     */
    public static final int NOT_INTERNED = -1;

    /**
     * Default maximum number of entries in the dictionary.
     */
    public static final int DEFAULT_MAX_SIZE = 4096;

    /**
     * Default minimum string length required for interning.
     */
    public static final int DEFAULT_MIN_STRING_LENGTH = 4;

    private final int maxSize;
    private final int minStringLength;

    // String -> ID mapping for O(1) lookup
    // String -> ID (open addressing: no Node / boxed Integer per entry)
    private final StringIntMap stringToId;

    // ID -> String mapping for reverse lookup
    private final List<String> idToString;

    // Per-ID usage count (dense array, indexed by ID) — lazily grown
    private int[] usageCount;

    // Snapshot of usageCount at last flush — lazily grown to match usageCount
    private int[] lastFlushedCount;

    // Rotating start position of the LFU scan: spreads evictions across equally-cold entries
    // instead of always recycling the lowest id.
    private int evictionCursor;

    // Number of pre-seeded entries (IDs 0..preSeedCount-1 are known by client at startup)
    private int preSeedCount;


    // Tracks which IDs have been sent to the client (via DICTIONARY_ADD)
    // Pre-seeded entries must be sent as ADD on first use since the client doesn't know them yet
    private final BitSet sentToClient;

    /**
     * Max evictions allowed per flush cycle — configurable via constructor.
     */
    private final int maxEvictionsPerFlush;

    /** Evictions performed in the current flush cycle — reset by {@link #resetEvictionBudget()}. */
    private int evictionsThisFlush = 0;

    // Optional shared provider for frequency reporting
    private SharedDictionaryProvider sharedProvider;

    public StringDictionary() {
        this(DEFAULT_MAX_SIZE, DEFAULT_MIN_STRING_LENGTH, 32);
    }

    public StringDictionary(final int maxSize, final int minStringLength) {
        this(maxSize, minStringLength, 32);
    }

    public StringDictionary(final int maxSize, final int minStringLength, final int maxEvictionsPerFlush) {
        if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be positive: " + maxSize);
        if (minStringLength < 0) throw new IllegalArgumentException("minStringLength must be non-negative: " + minStringLength);
        if (maxEvictionsPerFlush < 0) throw new IllegalArgumentException("maxEvictionsPerFlush must be non-negative: " + maxEvictionsPerFlush);
        this.maxSize = maxSize;
        this.minStringLength = minStringLength;
        this.maxEvictionsPerFlush = maxEvictionsPerFlush;
        this.stringToId = new StringIntMap();
        this.idToString = new ArrayList<>();
        // Start small — arrays grow on demand up to maxSize
        final int initialArraySize = Math.min(64, maxSize);
        this.usageCount = new int[initialArraySize];
        this.lastFlushedCount = new int[initialArraySize];
        this.sentToClient = new BitSet();
        this.preSeedCount = 0;
    }

    /**
     * Ensures the usageCount and lastFlushedCount arrays can hold the given ID.
     * Grows by doubling (capped at maxSize) to amortize allocation cost.
     */
    private void ensureArrayCapacity(final int id) {
        if (id < usageCount.length) return;
        final int newLen = Math.min(maxSize, Math.max(id + 1, usageCount.length * 2));
        usageCount = java.util.Arrays.copyOf(usageCount, newLen);
        lastFlushedCount = java.util.Arrays.copyOf(lastFlushedCount, newLen);
    }

    /**
     * Pre-seeds the dictionary with strings from the shared provider.
     * These entries are assigned IDs starting from 0 and are assumed to be
     * already known by the client, so they never need a DICTIONARY_ADD on the wire.
     * <p>
     * Must be called before any regular {@link #intern(String)} calls.
     * </p>
     *
     * @param provider The shared dictionary provider (may be null)
     */
    public void initFromSharedProvider(final SharedDictionaryProvider provider) {
        this.sharedProvider = provider;
        if (provider == null) return;

        final List<String> seeds = provider.getPreSeedStrings();
        for (final String s : seeds) {
            if (idToString.size() >= maxSize) break;
            if (s != null && !s.isEmpty() && !stringToId.containsKey(s)) {
                final int id = idToString.size();
                stringToId.put(s, id);
                idToString.add(s);
                ensureArrayCapacity(id);
                usageCount[id] = 0;
            }
        }
        this.preSeedCount = idToString.size();
    }

    /**
     * Returns true if the given ID corresponds to a pre-seeded entry.
     */
    public boolean isPreSeeded(final int id) {
        return id >= 0 && id < preSeedCount;
    }

    /**
     * Returns the number of pre-seeded entries.
     */
    public int getPreSeedCount() {
        return preSeedCount;
    }

    /**
     * Gets or creates an ID for the given string.
     * If the dictionary is full, evicts the least-frequently-used non-pre-seeded entry.
     *
     * @param value The string to intern
     * @return The string ID, or {@link #NOT_INTERNED} if the string cannot be interned
     */
    public int intern(final String value) {
        if (!canIntern(value)) {
            return NOT_INTERNED;
        }

        final int existingId = stringToId.get(value);
        if (existingId != StringIntMap.MISSING) {
            trackUsage(existingId);
            return existingId;
        }

        final int newId = allocateId();
        if (newId == NOT_INTERNED) {
            return NOT_INTERNED;
        }

        stringToId.put(value, newId);
        if (newId < idToString.size()) {
            idToString.set(newId, value);
        } else {
            idToString.add(value);
        }
        ensureArrayCapacity(newId);
        usageCount[newId] = 0;
        trackUsage(newId);
        return newId;
    }

    /**
     * Result holder for {@link #internOrGet(String)} to avoid double HashMap lookups.
     */
    public static final class InternResult {
        private static final long EXISTING_FLAG = 1L << 32;

        /** Sentinel: string could not be interned */
        public static final long NOT_FOUND = encodeRaw(NOT_INTERNED, false);

        private InternResult() {}

        public static long ofExisting(final int id) {
            return encodeRaw(id, true);
        }

        public static long ofNew(final int id) {
            return encodeRaw(id, false);
        }

        private static long encodeRaw(final int id, final boolean existing) {
            return (existing ? EXISTING_FLAG : 0L) | (id & 0xFFFFFFFFL);
        }

        public static int id(final long result) {
            return (int) result;
        }

        public static boolean isExisting(final long result) {
            return (result & EXISTING_FLAG) != 0;
        }
    }

    /**
     * Single-pass intern: looks up the string and, if absent, adds it.
     * If the dictionary is full, evicts the least-frequently-used entry.
     *
     * @param value The string to intern
     * @return Encoded result (use InternResult to decode), or InternResult.NOT_FOUND
     */
    public long internOrGet(final String value) {
        if (!canIntern(value)) {
            return InternResult.NOT_FOUND;
        }

        final int existingId = stringToId.get(value);
        if (existingId != StringIntMap.MISSING) {
            trackUsage(existingId);
            // If this ID was never sent to the client, treat it as new (needs DICTIONARY_ADD)
            if (!sentToClient.get(existingId)) {
                sentToClient.set(existingId);
                return InternResult.ofNew(existingId);
            }
            return InternResult.ofExisting(existingId);
        }

        final int newId = allocateId();
        if (newId == NOT_INTERNED) {
            return InternResult.NOT_FOUND;
        }

        stringToId.put(value, newId);
        if (newId < idToString.size()) {
            idToString.set(newId, value);
        } else {
            idToString.add(value);
        }
        ensureArrayCapacity(newId);
        usageCount[newId] = 0;
        sentToClient.set(newId);
        trackUsage(newId);
        return InternResult.ofNew(newId);
    }

    /**
     * Allocates an ID for a new entry.
     * - Uses the free list first (previously evicted IDs).
     * - Grows the dictionary if below maxSize.
     * - If full, evicts the LFU entry — but only if the per-flush eviction budget
     *   ({@code maxEvictionsPerFlush}) has not been exhausted. This prevents a burst
     *   of DICTIONARY_ADD frames (e.g. 1960/tick from a grid) from saturating the socket buffer.
     *   Strings that exceed the budget are simply sent inline (not interned).
     *
     * @return an available ID, or NOT_INTERNED if the dictionary is full and budget exhausted
     */
    private int allocateId() {
        if (idToString.size() < maxSize) {
            return idToString.size();
        }
        // Full — evict only if budget allows
        if (evictionsThisFlush >= maxEvictionsPerFlush) {
            return NOT_INTERNED;
        }
        final int evicted = evictLFU();
        if (evicted != NOT_INTERNED) evictionsThisFlush++;
        return evicted;
    }

    public void resetEvictionBudget() {
        evictionsThisFlush = 0;
    }

    /**
     * Forgets which entries the client knows about, so every id is re-announced with a
     * DICTIONARY_ADD on its next use. Server-side mappings and usage counts are kept.
     * <p>
     * Call this when the client-side cache can no longer be trusted, typically on transparent
     * reconnection: DICTIONARY_ADD frames still in flight when the old socket dropped never reached
     * the client, and a later reference to such an id would fail with "Unknown dictionary ID".
     * Re-announcing reuses the same ids, so a client that did receive them just overwrites the slot.
     */
    public void resetClientSync() {
        sentToClient.clear();
    }

    /**
     * Finds and evicts the least-frequently-used non-pre-seeded entry.
     * <p>
     * Linear scan over the dense {@code usageCount} array: O(maxSize) per eviction, but evictions are
     * capped per flush cycle and only happen once the dictionary is full, whereas the previous sorted
     * frequency index charged a TreeMap + boxed-set update on <em>every</em> dictionary hit. The scan
     * starts at a rotating cursor (fair among equally-cold entries) and stops early on an entry used
     * at most once, which is the minimum for a live entry.
     *
     * @return the evicted ID, or NOT_INTERNED if no evictable entry exists
     */
    private int evictLFU() {
        final int size = idToString.size();
        final int candidates = size - preSeedCount;
        if (candidates <= 0) return NOT_INTERNED; // only pre-seeded entries — never evicted

        int victim = NOT_INTERNED;
        int victimCount = Integer.MAX_VALUE;
        int idx = evictionCursor >= preSeedCount && evictionCursor < size ? evictionCursor : preSeedCount;
        for (int n = 0; n < candidates; n++) {
            if (idToString.get(idx) != null) {
                final int count = usageCount[idx];
                if (count < victimCount) {
                    victim = idx;
                    victimCount = count;
                    if (count <= 1) break; // can't do better than a single use
                }
            }
            if (++idx == size) idx = preSeedCount;
        }
        if (victim == NOT_INTERNED) return NOT_INTERNED;

        evictionCursor = victim + 1;
        stringToId.remove(idToString.get(victim));
        idToString.set(victim, null);
        usageCount[victim] = 0;
        lastFlushedCount[victim] = 0;
        sentToClient.clear(victim);
        return victim;
    }

    private void trackUsage(final int id) {
        // Saturating increment — a plain int write, no allocation on the hot path
        if (usageCount[id] != Integer.MAX_VALUE) usageCount[id]++;
    }

    /**
     * Gets the ID for an existing string without creating a new entry.
     * Only returns the ID if it has already been sent to the client.
     */
    public int getId(final String value) {
        if (value == null) {
            return NOT_INTERNED;
        }
        final int id = stringToId.get(value);
        if (id != StringIntMap.MISSING && sentToClient.get(id)) {
            trackUsage(id);
            return id;
        }
        return NOT_INTERNED;
    }

    /**
     * Gets the string for a given ID.
     */
    public String getString(final int id) {
        return idToString.get(id);
    }

    /**
     * Checks if a string is eligible for interning.
     */
    public boolean canIntern(final String value) {
        return value != null && !value.isEmpty() && value.length() >= minStringLength;
    }

    /**
     * Returns the current number of active entries in the dictionary.
     */
    public int size() {
        return stringToId.size();
    }

    /**
     * Checks if the dictionary has reached its maximum size with no free slots.
     */
    public boolean isFull() {
        return idToString.size() >= maxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getMinStringLength() {
        return minStringLength;
    }

    /**
     * Flushes the per-session frequency deltas to the shared provider.
     * Computes deltas from usageCount vs lastFlushedCount — zero allocation on the hot path.
     */
    public void flushToSharedProvider() {
        if (sharedProvider == null) return;
        final int size = Math.min(idToString.size(), usageCount.length);
        final Map<String, Long> deltas = new HashMap<>();
        for (int i = 0; i < size; i++) {
            final String s = idToString.get(i);
            if (s == null) continue;
            final long delta = (long) usageCount[i] - lastFlushedCount[i];
            if (delta > 0) {
                deltas.put(s, delta);
                lastFlushedCount[i] = usageCount[i];
            }
        }
        if (!deltas.isEmpty()) {
            sharedProvider.recordBulkUsage(deltas);
        }
    }

    /**
     * Returns the per-session frequency map (for diagnostics).
     * Computed on demand from usageCount — not maintained in a separate map.
     */
    public Map<String, Long> getSessionFrequency() {
        final int size = Math.min(idToString.size(), usageCount.length);
        final Map<String, Long> freq = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            final String s = idToString.get(i);
            if (s != null && usageCount[i] > 0) {
                freq.put(s, (long) usageCount[i]);
            }
        }
        return java.util.Collections.unmodifiableMap(freq);
    }
}
