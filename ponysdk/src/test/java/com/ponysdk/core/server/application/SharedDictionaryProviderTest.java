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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SharedDictionaryProvider} — disk persistence round-trip, frequency decay,
 * the minimum-persist threshold, and the pre-seed list (ordering + size cap).
 */
public class SharedDictionaryProviderTest {

    private Path tmpDir;
    private final List<SharedDictionaryProvider> providers = new ArrayList<>();

    @Before
    public void setUp() throws IOException {
        tmpDir = Files.createTempDirectory("pony-dict-test");
    }

    @After
    public void tearDown() throws IOException {
        // Stop the auto-persist schedulers before removing the temp directory.
        for (final SharedDictionaryProvider p : providers) {
            try { p.shutdown(); } catch (final Exception ignored) {}
        }
        if (tmpDir != null && Files.exists(tmpDir)) {
            try (final Stream<Path> walk = Files.walk(tmpDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (final IOException ignored) {}
                });
            }
        }
    }

    private SharedDictionaryProvider newProvider(final int preSeedSize) {
        final SharedDictionaryProvider p = new SharedDictionaryProvider(tmpDir, preSeedSize);
        providers.add(p);
        return p;
    }

    @Test
    public void recordBulkUsageGrowsGlobalSize() {
        final SharedDictionaryProvider p = newProvider(512);
        assertEquals(0, p.globalSize());
        p.recordBulkUsage(Map.of("alpha", 5L, "beta", 3L));
        assertEquals(2, p.globalSize());
        p.recordUsage("gamma");
        assertEquals(3, p.globalSize());
    }

    @Test
    public void persistThenReloadPreSeedsFrequentStrings() {
        final SharedDictionaryProvider p1 = newProvider(512);
        p1.recordBulkUsage(Map.of("hello", 10L));
        p1.persist();
        assertTrue("frequent string is pre-seeded after persist", p1.getPreSeedStrings().contains("hello"));

        // A fresh provider on the same directory must load the persisted dictionary.
        final SharedDictionaryProvider p2 = newProvider(512);
        assertTrue("reloaded provider pre-seeds the persisted string", p2.getPreSeedStrings().contains("hello"));
    }

    @Test
    public void persistAppliesDecayAndFileFormat() throws IOException {
        final SharedDictionaryProvider p = newProvider(512);
        p.recordBulkUsage(Map.of("hello", 10L));
        p.persist();

        // persist() decays by 0.9 (10 -> 9) and writes "freq\tstring".
        final Path file = tmpDir.resolve(SharedDictionaryProvider.DEFAULT_FILE_NAME);
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertEquals("9\thello", lines.get(0));
    }

    @Test
    public void infrequentStringsAreNotPersisted() {
        // Below MIN_PERSIST_FREQUENCY (3) after decay → never written / pre-seeded.
        final SharedDictionaryProvider p = newProvider(512);
        p.recordBulkUsage(Map.of("rare", 2L));
        p.persist();
        assertFalse(p.getPreSeedStrings().contains("rare"));

        final SharedDictionaryProvider p2 = newProvider(512);
        assertFalse(p2.getPreSeedStrings().contains("rare"));
    }

    @Test
    public void preSeedListIsCappedAndOrderedByFrequency() {
        final SharedDictionaryProvider p = newProvider(2); // cap at 2
        p.recordBulkUsage(Map.of("a", 100L, "b", 50L, "c", 10L));
        p.persist();

        final List<String> preSeed = p.getPreSeedStrings();
        assertEquals("pre-seed list is capped at preSeedSize", 2, preSeed.size());
        assertEquals("ordered by frequency desc", List.of("a", "b"), preSeed);
    }
}
