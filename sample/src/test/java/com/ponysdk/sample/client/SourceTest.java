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

package com.ponysdk.sample.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** The code panels of the showcase show regions of the pages' sources (Source.java): each one exists, once. */
class SourceTest {

    private static final Path SOURCES = Path.of("src/main/java/com/ponysdk/sample/client");
    private static final Path QUICKSTART = Path.of("src/main/java/com/ponysdk/sample/quickstart");
    private static final Path MAIN = Path.of("src/main/java/com/ponysdk/impl/java");
    private static final Path WA = Path.of("src/main/java/com/ponysdk/sample/client/wa");
    private static final Path PERSPECTIVE = Path.of("src/main/java/com/ponysdk/sample/perspective");
    /** The directories whose sources the build copies for the panels (sample/build.gradle), and their packages. */
    private static final Map<Path, String> PACKAGES = Map.of(SOURCES, "com.ponysdk.sample.client.", QUICKSTART,
        "com.ponysdk.sample.quickstart.", MAIN, "com.ponysdk.impl.java.", WA, "com.ponysdk.sample.client.wa.", PERSPECTIVE,
        "com.ponysdk.sample.perspective.");
    private static final Pattern USE = Pattern.compile("Source\\.code\\((\\w+)\\.class, \"([^\"]+)\"\\)");
    private static final Pattern MARKER = Pattern.compile("^\\s*// region (\\w+)\\b");

    @Test
    void everyRegionAPanelShowsExistsOnceAndHasCode() throws Exception {
        final List<String> uses = new ArrayList<>();
        try (Stream<Path> files = Files.list(SOURCES)) {
            for (final Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                final Matcher m = USE.matcher(Files.readString(file));
                while (m.find()) uses.add(m.group(1) + "#" + m.group(2));
            }
        }
        assertTrue(uses.size() >= 40, "the panels found: " + uses.size());
        for (final String use : uses) {
            final String[] parts = use.split("#");
            assertEquals(1, markers(parts[0]).getOrDefault(parts[1], 0), use + ": one region of that name");
            final Class<?> page = Class.forName(PACKAGES.get(directoryOf(parts[0])) + parts[0]);
            final List<String> lines = Source.lines(page, parts[1]);
            assertFalse(lines.stream().allMatch(String::isBlank), use + " is empty");
            assertTrue(lines.stream().noneMatch(l -> l.contains("// region") || l.contains("// endregion")), use);
        }
    }

    @Test
    void aRegionIsShownHighlightedAndEscaped() {
        final String html = Source.code(InputsPage.class, "textbox");
        assertTrue(html.startsWith("<span class='code-line'><span class='code-kw'>final</span> <span class='code-type'>PTextBox</span>"), html);
        assertTrue(html.contains("<span class='code-str'>\"Click me\"</span>") || html.contains("<span class='code-str'>"), html);
        assertFalse(html.contains("// region"), html);
        final String table = Source.code(DataPage.class, "flextable");
        assertTrue(table.contains("row &lt; fx.length"), table);
        assertFalse(table.contains("row < fx"), table);
    }

    @Test
    void aMissingRegionSaysWhere() {
        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> Source.lines(InputsPage.class, "none"));
        assertEquals("No region 'none' in source/InputsPage.java", e.getMessage());
    }

    private static Map<String, Integer> markers(final String page) throws IOException {
        final Map<String, Integer> count = new HashMap<>();
        final Path file = directoryOf(page).resolve(page + ".java");
        for (final String line : Files.readAllLines(file)) {
            final Matcher m = MARKER.matcher(line);
            if (m.find()) count.merge(m.group(1), 1, Integer::sum);
        }
        return count;
    }

    private static Path directoryOf(final String page) {
        return PACKAGES.keySet().stream().filter(dir -> Files.exists(dir.resolve(page + ".java"))).findFirst().orElseThrow();
    }
}
