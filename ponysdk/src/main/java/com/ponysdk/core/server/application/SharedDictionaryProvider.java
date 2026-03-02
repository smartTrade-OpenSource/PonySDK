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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Application-level shared dictionary that aggregates string frequency data
 * across all UIContext sessions and persists the most frequent strings to disk.
 * <p>
 * On startup, loads the persisted dictionary so new sessions immediately benefit
 * from pre-seeded strings (no wire cost for known strings). During runtime,
 * each session reports its string usage. Periodically (or on shutdown), the
 * aggregated frequencies are persisted back to disk, so the dictionary evolves
 * and improves over time.
 * </p>
 * <p>
 * Thread safety: This class is fully thread-safe. Multiple UIContexts can
 * report usage concurrently.
 * </p>
 */
public class SharedDictionaryProvider {

    private static final Logger log = LoggerFactory.getLogger(SharedDictionaryProvider.class);

    /**
     * Default file name for the persisted dictionary.
     */
    public static final String DEFAULT_FILE_NAME = "ponysdk-string-dictionary.txt";

    /**
     * Default maximum number of pre-seeded strings to load from the persisted file.
     */
    public static final int DEFAULT_PRESEED_SIZE = 512;

    /**
     * Minimum number of total occurrences across sessions before a string is
     * considered worth persisting.
     */
    private static final long MIN_PERSIST_FREQUENCY = 3;

    /**
     * Default auto-persist interval in minutes.
     */
    public static final long DEFAULT_AUTO_PERSIST_MINUTES = 10;

    /**
     * Decay factor applied to frequencies on each persist cycle.
     * This ensures that strings which were popular in the past but are no longer
     * used gradually lose their position, making room for new frequent strings.
     * A factor of 0.9 means frequencies lose 10% each persist cycle.
     */
    private static final double FREQUENCY_DECAY_FACTOR = 0.9;

    private final Path persistPath;
    private final int preSeedSize;

    // Global frequency map: string -> cumulative usage count across all sessions
    private final ConcurrentHashMap<String, LongAdder> globalFrequency = new ConcurrentHashMap<>();

    // Immutable pre-seed list loaded at startup (ordered by frequency, most frequent first)
    private volatile List<String> preSeedStrings = Collections.emptyList();

    // Tracks total number of bulk flushes received (i.e., sessions ended)
    private final AtomicLong sessionFlushCount = new AtomicLong();

    // Auto-persist scheduler
    private final ScheduledExecutorService scheduler;
    private final ScheduledFuture<?> autoPersistTask;

    /**
     * Creates a provider that persists to the given directory.
     *
     * @param persistDirectory Directory where the dictionary file will be stored
     * @param preSeedSize      Maximum number of strings to pre-seed into new sessions
     */
    public SharedDictionaryProvider(final Path persistDirectory, final int preSeedSize) {
        this.persistPath = persistDirectory.resolve(DEFAULT_FILE_NAME);
        this.preSeedSize = preSeedSize > 0 ? preSeedSize : DEFAULT_PRESEED_SIZE;
        load();

        // Start auto-persist daemon thread
        this.scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().name("ponysdk-dict-persist").factory());
        this.autoPersistTask = scheduler.scheduleAtFixedRate(
            this::persistQuietly,
            DEFAULT_AUTO_PERSIST_MINUTES,
            DEFAULT_AUTO_PERSIST_MINUTES,
            TimeUnit.MINUTES
        );
    }

    /**
     * Creates a provider with default pre-seed size.
     *
     * @param persistDirectory Directory where the dictionary file will be stored
     */
    public SharedDictionaryProvider(final Path persistDirectory) {
        this(persistDirectory, DEFAULT_PRESEED_SIZE);
    }

    /**
     * Stops the auto-persist scheduler and performs a final persist.
     * Should be called on application shutdown.
     */
    public void shutdown() {
        if (autoPersistTask != null) {
            autoPersistTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        persist();
    }

    /**
     * Returns the current pre-seed list (immutable, ordered by frequency desc).
     * This list is used to initialize new StringDictionary instances so that
     * the most common strings are already known without any wire cost.
     *
     * @return Unmodifiable list of pre-seeded strings
     */
    public List<String> getPreSeedStrings() {
        return preSeedStrings;
    }

    /**
     * Reports that a string was used in a session. Called by StringDictionary
     * each time a string is interned or referenced.
     *
     * @param value The string that was used
     */
    public void recordUsage(final String value) {
        if (value == null || value.isEmpty()) return;
        globalFrequency.computeIfAbsent(value, k -> new LongAdder()).increment();
    }

    /**
     * Reports multiple usages at once (e.g., when a session ends and flushes
     * its local frequency counters).
     *
     * @param frequencies Map of string to usage count
     */
    public void recordBulkUsage(final Map<String, Long> frequencies) {
        if (frequencies == null) return;
        for (final Map.Entry<String, Long> entry : frequencies.entrySet()) {
            if (entry.getKey() != null && entry.getValue() > 0) {
                globalFrequency.computeIfAbsent(entry.getKey(), k -> new LongAdder()).add(entry.getValue());
            }
        }
        sessionFlushCount.incrementAndGet();
    }

    /**
     * Wrapper for scheduled auto-persist that catches exceptions.
     */
    private void persistQuietly() {
        try {
            persist();
        } catch (final Exception e) {
            log.warn("Auto-persist failed", e);
        }
    }

    /**
     * Persists the current frequency data to disk and refreshes the pre-seed list.
     * Applies a decay factor to all frequencies so that strings which are no longer
     * used gradually lose their position, keeping the dictionary fresh.
     * <p>
     * File format: one line per entry, tab-separated: {@code frequency\tstring}
     * Lines are sorted by frequency descending.
     * </p>
     */
    public synchronized void persist() {
        try {
            Files.createDirectories(persistPath.getParent());

            // Apply decay and collect entries
            final List<Map.Entry<String, LongAdder>> entries = new ArrayList<>(globalFrequency.size());
            final var iterator = globalFrequency.entrySet().iterator();
            while (iterator.hasNext()) {
                final var entry = iterator.next();
                final long current = entry.getValue().sum();
                final long decayed = (long) (current * FREQUENCY_DECAY_FACTOR);
                if (decayed < 1) {
                    // Frequency decayed to zero — remove from global map
                    iterator.remove();
                } else {
                    // Reset the adder to the decayed value
                    entry.getValue().reset();
                    entry.getValue().add(decayed);
                    entries.add(entry);
                }
            }

            // Sort by frequency descending
            entries.sort((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()));

            try (final BufferedWriter writer = Files.newBufferedWriter(persistPath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                for (final Map.Entry<String, LongAdder> entry : entries) {
                    final long freq = entry.getValue().sum();
                    if (freq >= MIN_PERSIST_FREQUENCY) {
                        writer.write(Long.toString(freq));
                        writer.write('\t');
                        writer.write(entry.getKey());
                        writer.newLine();
                    }
                }
            }

            // Refresh pre-seed list
            refreshPreSeedList(entries);

            log.info("Persisted string dictionary: {} entries ({} pre-seeded, {} sessions flushed) to {}",
                    entries.size(), preSeedStrings.size(), sessionFlushCount.get(), persistPath);
        } catch (final IOException e) {
            log.warn("Failed to persist string dictionary to {}", persistPath, e);
        }
    }

    /**
     * Loads the persisted dictionary from disk. Called once at construction.
     */
    private void load() {
        if (!Files.exists(persistPath)) {
            log.info("No persisted string dictionary found at {}, starting fresh", persistPath);
            return;
        }

        try (final BufferedReader reader = Files.newBufferedReader(persistPath, StandardCharsets.UTF_8)) {
            String line;
            final List<String> loaded = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                final int tab = line.indexOf('\t');
                if (tab <= 0 || tab >= line.length() - 1) continue;

                try {
                    final long freq = Long.parseLong(line.substring(0, tab));
                    final String value = line.substring(tab + 1);
                    if (!value.isEmpty()) {
                        globalFrequency.computeIfAbsent(value, k -> new LongAdder()).add(freq);
                        if (loaded.size() < preSeedSize) {
                            loaded.add(value);
                        }
                    }
                } catch (final NumberFormatException e) {
                    // Skip malformed lines
                }
            }

            preSeedStrings = Collections.unmodifiableList(loaded);
            log.info("Loaded persisted string dictionary: {} entries ({} pre-seeded) from {}",
                    globalFrequency.size(), preSeedStrings.size(), persistPath);
        } catch (final IOException e) {
            log.warn("Failed to load string dictionary from {}", persistPath, e);
        }
    }

    private void refreshPreSeedList(final List<Map.Entry<String, LongAdder>> sortedEntries) {
        final List<String> newPreSeed = new ArrayList<>(Math.min(sortedEntries.size(), preSeedSize));
        for (final Map.Entry<String, LongAdder> entry : sortedEntries) {
            if (newPreSeed.size() >= preSeedSize) break;
            if (entry.getValue().sum() >= MIN_PERSIST_FREQUENCY) {
                newPreSeed.add(entry.getKey());
            }
        }
        preSeedStrings = Collections.unmodifiableList(newPreSeed);
    }

    /**
     * Returns the number of unique strings tracked globally.
     */
    public int globalSize() {
        return globalFrequency.size();
    }
}
