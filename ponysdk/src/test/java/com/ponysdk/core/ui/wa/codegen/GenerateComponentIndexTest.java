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

package com.ponysdk.core.ui.wa.codegen;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WebAwesomeCodeGenerator#generateComponentIndex(List)}.
 */
class GenerateComponentIndexTest {

    private WebAwesomeCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new WebAwesomeCodeGenerator(
            Path.of("dummy-manifest.json"), Path.of("dummy-java"), Path.of("dummy-ts"));
    }

    @Test
    void generateComponentIndex_packageDeclaration() {
        final String source = generator.generateComponentIndex(List.of(createDef("wa-button", "stable")));
        assertTrue(source.contains("package com.ponysdk.core.ui.wa;"));
    }

    @Test
    void generateComponentIndex_classStructure() {
        final String source = generator.generateComponentIndex(List.of(createDef("wa-button", "stable")));
        assertTrue(source.contains("public class ComponentIndex {"));
        assertTrue(source.contains("public record ComponentEntry("));
        assertTrue(source.contains("public static final List<ComponentEntry> ALL_COMPONENTS = List.of("));
    }

    @Test
    void generateComponentIndex_singleComponent() {
        final String source = generator.generateComponentIndex(List.of(createDef("wa-button", "stable")));

        assertTrue(source.contains("\"wa-button\""));
        assertTrue(source.contains("\"com.ponysdk.core.ui.wa.PButton\""));
        assertTrue(source.contains("\"com.ponysdk.core.ui.wa.props.ButtonProps\""));
        assertTrue(source.contains("\"stable\""));
    }

    @Test
    void generateComponentIndex_multipleComponents() {
        final List<ComponentDefinition> defs = List.of(
            createDef("wa-button", "stable"),
            createDef("wa-input", "stable"),
            createDef("wa-dialog", "experimental")
        );

        final String source = generator.generateComponentIndex(defs);

        assertTrue(source.contains("\"wa-button\""));
        assertTrue(source.contains("\"com.ponysdk.core.ui.wa.PInput\""));
        assertTrue(source.contains("\"com.ponysdk.core.ui.wa.props.DialogProps\""));
        assertTrue(source.contains("\"experimental\""));
    }

    @Test
    void generateComponentIndex_multiPartTagName() {
        final String source = generator.generateComponentIndex(List.of(createDef("wa-tab-group", "stable")));

        assertTrue(source.contains("\"wa-tab-group\""));
        assertTrue(source.contains("\"com.ponysdk.core.ui.wa.PTabGroup\""));
        assertTrue(source.contains("\"com.ponysdk.core.ui.wa.props.TabGroupProps\""));
    }

    @Test
    void generateComponentIndex_emptyList() {
        final String source = generator.generateComponentIndex(Collections.emptyList());

        assertTrue(source.contains("public class ComponentIndex {"));
        assertTrue(source.contains("ALL_COMPONENTS = List.of("));
        // Should have an empty list — no ComponentEntry lines
        assertFalse(source.contains("new ComponentEntry("));
    }

    @Test
    void generateComponentIndex_statusPreserved() {
        final List<ComponentDefinition> defs = List.of(
            createDef("wa-alert", "stable"),
            createDef("wa-popup", "experimental"),
            createDef("wa-old-widget", "deprecated")
        );

        final String source = generator.generateComponentIndex(defs);

        assertTrue(source.contains("\"stable\""));
        assertTrue(source.contains("\"experimental\""));
        assertTrue(source.contains("\"deprecated\""));
    }

    @Test
    void generateComponentIndex_nullStatusDefaultsToStable() {
        final String source = generator.generateComponentIndex(List.of(createDef("wa-button", null)));
        assertTrue(source.contains("\"stable\""));
    }

    @Test
    void generateComponentIndex_javadocPresent() {
        final String source = generator.generateComponentIndex(List.of(createDef("wa-button", "stable")));
        assertTrue(source.contains("@generated from custom-elements.json"));
        assertTrue(source.contains("Index of all generated Web Awesome component wrappers."));
    }

    @Test
    void generateComponentIndex_commasBetweenEntries() {
        final List<ComponentDefinition> defs = List.of(
            createDef("wa-button", "stable"),
            createDef("wa-input", "stable")
        );

        final String source = generator.generateComponentIndex(defs);

        // First entry should have a comma, last should not
        final int firstEntryEnd = source.indexOf("\"stable\")") + "\"stable\")".length();
        final char afterFirst = source.charAt(firstEntryEnd);
        assertEquals(',', afterFirst, "First entry should be followed by a comma");
    }

    private static ComponentDefinition createDef(final String tagName, final String status) {
        return new ComponentDefinition(
            tagName, "Wa" + tagName.substring(3), "", "",
            Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(),
            status
        );
    }
}
