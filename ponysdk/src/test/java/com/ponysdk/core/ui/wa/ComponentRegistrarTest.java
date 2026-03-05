/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.ui.wa;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.wa.codegen.ComponentDefinition;
import com.ponysdk.core.ui.wa.codegen.WebAwesomeCodeGenerator;

/**
 * Tests verifying that all generated Web Awesome components are properly
 * registered in the ComponentRegistrar and that the generated ComponentIndex
 * lists all components with correct metadata.
 *
 * <p>Since ComponentIndex.java is generated at build-time, these tests use
 * the Code Generator directly to produce component definitions and verify
 * the registration mechanism works end-to-end.</p>
 *
 * <p>Validates: Requirements 12.3, 13.8</p>
 */
class ComponentRegistrarTest {

    /**
     * The Web Awesome component tag names as defined in the spec (52 components).
     * This list represents the full set of wa-* components that the Code Generator
     * produces wrappers for.
     */
    private static final List<String> ALL_WA_TAG_NAMES = List.of(
        "wa-alert",
        "wa-animated-image",
        "wa-avatar",
        "wa-badge",
        "wa-breadcrumb",
        "wa-breadcrumb-item",
        "wa-button",
        "wa-button-group",
        "wa-card",
        "wa-carousel",
        "wa-carousel-item",
        "wa-checkbox",
        "wa-color-picker",
        "wa-copy-button",
        "wa-details",
        "wa-dialog",
        "wa-divider",
        "wa-drawer",
        "wa-dropdown",
        "wa-format-bytes",
        "wa-format-date",
        "wa-format-number",
        "wa-icon",
        "wa-icon-button",
        "wa-image-comparer",
        "wa-include",
        "wa-input",
        "wa-menu",
        "wa-menu-item",
        "wa-menu-label",
        "wa-option",
        "wa-popup",
        "wa-progress-bar",
        "wa-progress-ring",
        "wa-qr-code",
        "wa-radio",
        "wa-radio-button",
        "wa-radio-group",
        "wa-range",
        "wa-rating",
        "wa-relative-time",
        "wa-resize-observer",
        "wa-select",
        "wa-skeleton",
        "wa-spinner",
        "wa-split-panel",
        "wa-switch",
        "wa-tab",
        "wa-tab-group",
        "wa-tab-panel",
        "wa-tag",
        "wa-textarea",
        "wa-tooltip",
        "wa-tree",
        "wa-tree-item",
        "wa-visually-hidden"
    );

    private ComponentRegistrar registrar;
    private WebAwesomeCodeGenerator generator;

    @BeforeEach
    void setUp() {
        registrar = new ComponentRegistrar();
        generator = new WebAwesomeCodeGenerator(
            Path.of("dummy-manifest.json"), Path.of("dummy-java"), Path.of("dummy-ts"));
    }

    // ========== ComponentRegistrar unit tests ==========

    @Test
    void registerAll_registersAllEntries() {
        final List<ComponentRegistrar.ComponentEntry> entries = buildEntriesForAll();
        registrar.registerAll(entries);

        assertEquals(ALL_WA_TAG_NAMES.size(), registrar.size(),
            "All components should be registered");
    }

    @Test
    void lookup_findsAllRegisteredComponents() {
        registrar.registerAll(buildEntriesForAll());

        for (final String tagName : ALL_WA_TAG_NAMES) {
            assertTrue(registrar.lookup(tagName).isPresent(),
                "Lookup should find registered component: " + tagName);
        }
    }

    @Test
    void lookup_returnsCorrectMetadata() {
        registrar.registerAll(buildEntriesForAll());

        final var entry = registrar.lookup("wa-button");
        assertTrue(entry.isPresent());
        assertEquals("wa-button", entry.get().tagName());
        assertEquals("com.ponysdk.core.ui.wa.WAButton", entry.get().wrapperClassName());
        assertEquals("com.ponysdk.core.ui.wa.props.ButtonProps", entry.get().propsClassName());
        assertEquals("stable", entry.get().status());
    }

    @Test
    void lookup_returnsEmptyForUnregistered() {
        registrar.registerAll(buildEntriesForAll());

        assertTrue(registrar.lookup("wa-nonexistent").isEmpty());
    }

    @Test
    void isRegistered_returnsTrueForAllComponents() {
        registrar.registerAll(buildEntriesForAll());

        for (final String tagName : ALL_WA_TAG_NAMES) {
            assertTrue(registrar.isRegistered(tagName),
                "isRegistered should return true for: " + tagName);
        }
    }

    @Test
    void isRegistered_returnsFalseForUnknown() {
        registrar.registerAll(buildEntriesForAll());

        assertFalse(registrar.isRegistered("wa-unknown"));
    }

    @Test
    void getAll_returnsUnmodifiableMap() {
        registrar.registerAll(buildEntriesForAll());

        assertThrows(UnsupportedOperationException.class, () ->
            registrar.getAll().put("wa-test", new ComponentRegistrar.ComponentEntry(
                "wa-test", "PTest", "TestProps", "stable")));
    }

    @Test
    void register_skipsBlankTagName() {
        registrar.register(new ComponentRegistrar.ComponentEntry("", "PEmpty", "EmptyProps", "stable"));
        registrar.register(new ComponentRegistrar.ComponentEntry(null, "PNull", "NullProps", "stable"));

        assertEquals(0, registrar.size());
    }

    // ========== Code Generator integration: ComponentIndex ==========

    @Test
    void generatedComponentIndex_containsAll52Entries() {
        final List<ComponentDefinition> defs = buildDefinitionsForAll52();
        final String indexSource = generator.generateComponentIndex(defs);

        // Verify the index contains an entry for each tag name
        for (final String tagName : ALL_WA_TAG_NAMES) {
            assertTrue(indexSource.contains("\"" + tagName + "\""),
                "ComponentIndex should contain tag name: " + tagName);
        }
    }

    @Test
    void generatedComponentIndex_hasCorrectWrapperClassNames() {
        final List<ComponentDefinition> defs = buildDefinitionsForAll52();
        final String indexSource = generator.generateComponentIndex(defs);

        // Verify a sample of wrapper class names
        assertTrue(indexSource.contains("com.ponysdk.core.ui.wa.WAButton"));
        assertTrue(indexSource.contains("com.ponysdk.core.ui.wa.WAInput"));
        assertTrue(indexSource.contains("com.ponysdk.core.ui.wa.WADialog"));
        assertTrue(indexSource.contains("com.ponysdk.core.ui.wa.WATabGroup"));
        assertTrue(indexSource.contains("com.ponysdk.core.ui.wa.WAProgressBar"));
        assertTrue(indexSource.contains("com.ponysdk.core.ui.wa.WAAnimatedImage"));
        assertTrue(indexSource.contains("com.ponysdk.core.ui.wa.WAColorPicker"));
    }

    @Test
    void generatedComponentIndex_hasCorrectPropsClassNames() {
        final List<ComponentDefinition> defs = buildDefinitionsForAll52();
        final String indexSource = generator.generateComponentIndex(defs);

        assertTrue(indexSource.contains("com.ponysdk.core.ui.wa.props.ButtonProps"));
        assertTrue(indexSource.contains("com.ponysdk.core.ui.wa.props.InputProps"));
        assertTrue(indexSource.contains("com.ponysdk.core.ui.wa.props.DialogProps"));
        assertTrue(indexSource.contains("com.ponysdk.core.ui.wa.props.TabGroupProps"));
        assertTrue(indexSource.contains("com.ponysdk.core.ui.wa.props.ProgressBarProps"));
    }

    @Test
    void generatedComponentIndex_entryCountMatches52() {
        final List<ComponentDefinition> defs = buildDefinitionsForAll52();
        final String indexSource = generator.generateComponentIndex(defs);

        // Count occurrences of "new ComponentEntry("
        final int entryCount = countOccurrences(indexSource, "new ComponentEntry(");
        assertEquals(ALL_WA_TAG_NAMES.size(), entryCount,
            "ComponentIndex should contain exactly " + ALL_WA_TAG_NAMES.size() + " entries");
    }

    // ========== Code Generator integration: wrapper getComponentSignature ==========

    @Test
    void generatedWrappers_allReturnCorrectSignature() {
        final List<ComponentDefinition> defs = buildDefinitionsForAll52();

        for (final ComponentDefinition def : defs) {
            final String wrapperSource = generator.generateWrapperClass(def);
            assertTrue(wrapperSource.contains("return \"" + def.tagName() + "\";"),
                "Wrapper for " + def.tagName() + " should have getComponentSignature() returning \"" + def.tagName() + "\"");
        }
    }

    @Test
    void generatedWrappers_allExtendPWebComponent() {
        final List<ComponentDefinition> defs = buildDefinitionsForAll52();

        for (final ComponentDefinition def : defs) {
            final String wrapperSource = generator.generateWrapperClass(def);
            assertTrue(wrapperSource.contains("extends PWebComponent<"),
                "Wrapper for " + def.tagName() + " should extend PWebComponent");
        }
    }

    // ========== Registrar + CodeGen end-to-end ==========

    @Test
    void registrar_worksWithGeneratedIndexEntries() {
        final List<ComponentDefinition> defs = buildDefinitionsForAll52();
        final String indexSource = generator.generateComponentIndex(defs);

        // Parse entries from the generated source and register them
        final List<ComponentRegistrar.ComponentEntry> entries = parseEntriesFromSource(indexSource);
        registrar.registerAll(entries);

        assertEquals(ALL_WA_TAG_NAMES.size(), registrar.size());

        for (final String tagName : ALL_WA_TAG_NAMES) {
            final var entry = registrar.lookup(tagName);
            assertTrue(entry.isPresent(), "Should find: " + tagName);
            assertEquals(tagName, entry.get().tagName());
            assertTrue(entry.get().wrapperClassName().startsWith("com.ponysdk.core.ui.wa.P"),
                "Wrapper class should start with 'com.ponysdk.core.ui.wa.P': " + entry.get().wrapperClassName());
            assertTrue(entry.get().propsClassName().endsWith("Props"),
                "Props class should end with 'Props': " + entry.get().propsClassName());
        }
    }

    // ========== Helpers ==========

    private List<ComponentRegistrar.ComponentEntry> buildEntriesForAll() {
        final List<ComponentRegistrar.ComponentEntry> entries = new ArrayList<>();
        for (final String tagName : ALL_WA_TAG_NAMES) {
            final String wrapperClass = "com.ponysdk.core.ui.wa.P" + tagNameToPascalCase(tagName);
            final String propsClass = "com.ponysdk.core.ui.wa.props." + tagNameToPascalCase(tagName) + "Props";
            entries.add(new ComponentRegistrar.ComponentEntry(tagName, wrapperClass, propsClass, "stable"));
        }
        return entries;
    }

    /**
     * Converts a wa-* tag name to PascalCase (e.g., "wa-tab-group" → "TabGroup").
     * Mirrors the logic in WebAwesomeCodeGenerator.tagNameToWrapperClassName (minus the "P" prefix).
     */
    private static String tagNameToPascalCase(final String tagName) {
        final String withoutPrefix = tagName.startsWith("wa-") ? tagName.substring(3) : tagName;
        final StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (final char c : withoutPrefix.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private List<ComponentDefinition> buildDefinitionsForAll52() {
        final List<ComponentDefinition> defs = new ArrayList<>();
        for (final String tagName : ALL_WA_TAG_NAMES) {
            final String className = "Wa" + tagName.substring(3);
            defs.add(new ComponentDefinition(
                tagName, className, "A " + tagName + " component", "",
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), "stable"
            ));
        }
        return defs;
    }

    /**
     * Parses ComponentEntry data from the generated ComponentIndex source code.
     * This simulates what happens at runtime when ComponentIndex.ALL_COMPONENTS is accessed.
     */
    private List<ComponentRegistrar.ComponentEntry> parseEntriesFromSource(final String source) {
        final List<ComponentRegistrar.ComponentEntry> entries = new ArrayList<>();
        final String marker = "new ComponentEntry(";
        int idx = source.indexOf(marker);
        while (idx >= 0) {
            final int start = idx + marker.length();
            final int end = source.indexOf(")", start);
            final String args = source.substring(start, end);
            // Parse: "wa-button", "com.ponysdk.core.ui.wa.PButton", "com.ponysdk.core.ui.wa.props.ButtonProps", "stable"
            final String[] parts = args.split(",");
            if (parts.length == 4) {
                entries.add(new ComponentRegistrar.ComponentEntry(
                    stripQuotes(parts[0]),
                    stripQuotes(parts[1]),
                    stripQuotes(parts[2]),
                    stripQuotes(parts[3])
                ));
            }
            idx = source.indexOf(marker, end);
        }
        return entries;
    }

    private static String stripQuotes(final String s) {
        return s.trim().replace("\"", "");
    }

    private static int countOccurrences(final String text, final String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) >= 0) {
            count++;
            idx += pattern.length();
        }
        return count;
    }
}
