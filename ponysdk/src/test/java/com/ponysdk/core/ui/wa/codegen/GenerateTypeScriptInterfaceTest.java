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
 * Unit tests for {@link WebAwesomeCodeGenerator#generateTypeScriptInterface(ComponentDefinition)}
 * and the {@code javaTypeToTypeScript} helper.
 */
class GenerateTypeScriptInterfaceTest {

    private WebAwesomeCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new WebAwesomeCodeGenerator(
            Path.of("dummy-manifest.json"), Path.of("dummy-java"), Path.of("dummy-ts"));
    }

    // --- javaTypeToTypeScript helper tests ---

    @Test
    void javaTypeToTypeScript_string() {
        assertEquals("string", WebAwesomeCodeGenerator.javaTypeToTypeScript("String"));
    }

    @Test
    void javaTypeToTypeScript_double() {
        assertEquals("number", WebAwesomeCodeGenerator.javaTypeToTypeScript("double"));
    }

    @Test
    void javaTypeToTypeScript_boolean() {
        assertEquals("boolean", WebAwesomeCodeGenerator.javaTypeToTypeScript("boolean"));
    }

    @Test
    void javaTypeToTypeScript_object() {
        assertEquals("any", WebAwesomeCodeGenerator.javaTypeToTypeScript("Object"));
    }

    @Test
    void javaTypeToTypeScript_optionalString() {
        assertEquals("string", WebAwesomeCodeGenerator.javaTypeToTypeScript("Optional<String>"));
    }

    @Test
    void javaTypeToTypeScript_null() {
        assertEquals("any", WebAwesomeCodeGenerator.javaTypeToTypeScript(null));
    }

    @Test
    void javaTypeToTypeScript_unknownType() {
        assertEquals("any", WebAwesomeCodeGenerator.javaTypeToTypeScript("SomeUnknownType"));
    }

    // --- generateTypeScriptInterface tests ---

    @Test
    void generateTsInterface_simpleComponent() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("type", "String"),
            prop("value", "String"),
            prop("disabled", "boolean")
        ));

        final String ts = generator.generateTypeScriptInterface(def);

        assertTrue(ts.contains("export interface InputProps {"));
        assertTrue(ts.contains("    type: string;"));
        assertTrue(ts.contains("    value: string;"));
        assertTrue(ts.contains("    disabled: boolean;"));
        assertTrue(ts.contains("@generated from custom-elements.json"));
    }

    @Test
    void generateTsInterface_optionalFieldHasQuestionMark() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("pattern", "Optional<String>"),
            prop("value", "String")
        ));

        final String ts = generator.generateTypeScriptInterface(def);

        assertTrue(ts.contains("    pattern?: string;"));
        assertTrue(ts.contains("    value: string;"));
        assertFalse(ts.contains("    value?:"));
    }

    @Test
    void generateTsInterface_numberType() {
        final ComponentDefinition def = minimalDef("wa-range", List.of(
            prop("min", "double"),
            prop("max", "double")
        ));

        final String ts = generator.generateTypeScriptInterface(def);

        assertTrue(ts.contains("    min: number;"));
        assertTrue(ts.contains("    max: number;"));
    }

    @Test
    void generateTsInterface_objectFallback() {
        final ComponentDefinition def = minimalDef("wa-custom", List.of(
            prop("data", "Object")
        ));

        final String ts = generator.generateTypeScriptInterface(def);

        assertTrue(ts.contains("    data: any;"));
    }

    @Test
    void generateTsInterface_kebabCaseConvertedToCamelCase() {
        final ComponentDefinition def = minimalDef("wa-input", List.of(
            prop("help-text", "String"),
            prop("password-toggle", "boolean")
        ));

        final String ts = generator.generateTypeScriptInterface(def);

        assertTrue(ts.contains("    helpText: string;"));
        assertTrue(ts.contains("    passwordToggle: boolean;"));
    }

    @Test
    void generateTsInterface_jsdocContainsTagName() {
        final ComponentDefinition def = minimalDef("wa-button", List.of(
            prop("disabled", "boolean")
        ));

        final String ts = generator.generateTypeScriptInterface(def);

        assertTrue(ts.contains("Props interface for <wa-button>"));
    }

    @Test
    void generateTsInterface_multiPartTagName() {
        final ComponentDefinition def = minimalDef("wa-tab-group", List.of(
            prop("active", "String")
        ));

        final String ts = generator.generateTypeScriptInterface(def);

        assertTrue(ts.contains("export interface TabGroupProps {"));
    }

    @Test
    void generateTsInterface_emptyProperties() {
        final ComponentDefinition def = minimalDef("wa-divider", Collections.emptyList());

        final String ts = generator.generateTypeScriptInterface(def);

        assertTrue(ts.contains("export interface DividerProps {"));
        assertTrue(ts.contains("}\n"));
    }

    // --- helpers ---

    private static ComponentDefinition minimalDef(final String tagName, final List<PropertyDef> props) {
        return new ComponentDefinition(
            tagName, "Wa" + tagName.substring(3), "", "",
            props,
            Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), Collections.emptyList(), "stable"
        );
    }

    private static PropertyDef prop(final String name, final String javaType) {
        return new PropertyDef(name, "string", javaType, "", null, false);
    }
}
