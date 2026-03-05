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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TypeMapper custom type mapping registry (Task 4.3).
 * Tests exact mappings, pattern-based mappings, and configuration loading.
 * <p>
 * <b>Validates: Requirements 13.5</b>
 * </p>
 */
class TypeMapperCustomMappingTest {

    @BeforeEach
    void setUp() {
        // Clear any existing custom mappings before each test
        TypeMapper.clearCustomMappings();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        TypeMapper.clearCustomMappings();
    }

    // ========== Task 4.3: Exact custom mappings ==========

    @Test
    void registerCustomMapping_exactMatch_overridesDefault() {
        // Register custom mapping for HTMLElement
        TypeMapper.registerCustomMapping("HTMLElement", "IsPWidget", "HTMLElement");

        // Verify Java mapping
        TypeMapping javaResult = TypeMapper.mapToJava("HTMLElement");
        assertEquals("IsPWidget", javaResult.javaType());

        // Verify TypeScript mapping
        String tsResult = TypeMapper.mapToTypeScript("HTMLElement");
        assertEquals("HTMLElement", tsResult);
    }

    @Test
    void registerCustomMapping_Date_overridesDefault() {
        // Register custom mapping for Date to LocalDateTime
        TypeMapper.registerCustomMapping("Date", "LocalDateTime", "Date");

        TypeMapping result = TypeMapper.mapToJava("Date");
        assertEquals("LocalDateTime", result.javaType());
    }

    @Test
    void registerCustomMapping_multipleExactMappings() {
        // Register multiple custom mappings
        TypeMapper.registerCustomMapping("HTMLElement", "IsPWidget", "HTMLElement");
        TypeMapper.registerCustomMapping("Date", "LocalDateTime", "Date");
        TypeMapper.registerCustomMapping("CustomType", "MyCustomType", "CustomType");

        // Verify all mappings work
        assertEquals("IsPWidget", TypeMapper.mapToJava("HTMLElement").javaType());
        assertEquals("LocalDateTime", TypeMapper.mapToJava("Date").javaType());
        assertEquals("MyCustomType", TypeMapper.mapToJava("CustomType").javaType());
    }

    @Test
    void registerCustomMapping_nullCemType_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            TypeMapper.registerCustomMapping(null, "IsPWidget", "HTMLElement")
        );
    }

    @Test
    void registerCustomMapping_blankCemType_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            TypeMapper.registerCustomMapping("  ", "IsPWidget", "HTMLElement")
        );
    }

    @Test
    void registerCustomMapping_nullJavaType_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            TypeMapper.registerCustomMapping("HTMLElement", null, "HTMLElement")
        );
    }

    @Test
    void registerCustomMapping_nullTsType_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            TypeMapper.registerCustomMapping("HTMLElement", "IsPWidget", null)
        );
    }

    // ========== Task 4.3: Pattern-based custom mappings ==========

    @Test
    void registerPatternMapping_matchesMultipleTypes() {
        // Register pattern for all HTML*Element types
        TypeMapper.registerPatternMapping("HTML.*Element", "IsPWidget", "HTMLElement");

        // Verify multiple types match the pattern
        assertEquals("IsPWidget", TypeMapper.mapToJava("HTMLElement").javaType());
        assertEquals("IsPWidget", TypeMapper.mapToJava("HTMLDivElement").javaType());
        assertEquals("IsPWidget", TypeMapper.mapToJava("HTMLButtonElement").javaType());
        assertEquals("IsPWidget", TypeMapper.mapToJava("HTMLInputElement").javaType());
    }

    @Test
    void registerPatternMapping_eventTypes() {
        // Register pattern for all *Event types
        TypeMapper.registerPatternMapping(".*Event", "CustomEvent", "Event");

        assertEquals("CustomEvent", TypeMapper.mapToJava("ClickEvent").javaType());
        assertEquals("CustomEvent", TypeMapper.mapToJava("MouseEvent").javaType());
        assertEquals("CustomEvent", TypeMapper.mapToJava("KeyboardEvent").javaType());
    }

    @Test
    void registerPatternMapping_withAnchors() {
        // Register pattern with start and end anchors
        TypeMapper.registerPatternMapping("^Custom.*$", "MyCustomType", "any");

        assertEquals("MyCustomType", TypeMapper.mapToJava("CustomButton").javaType());
        assertEquals("MyCustomType", TypeMapper.mapToJava("CustomInput").javaType());
        
        // Should not match types that don't start with "Custom"
        assertEquals("Object", TypeMapper.mapToJava("NotCustom").javaType());
    }

    @Test
    void registerPatternMapping_nullPattern_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
            TypeMapper.registerPatternMapping(null, "IsPWidget", "HTMLElement")
        );
    }

    @Test
    void registerPatternMapping_invalidRegex_throwsException() {
        assertThrows(Exception.class, () ->
            TypeMapper.registerPatternMapping("[invalid(", "IsPWidget", "HTMLElement")
        );
    }

    // ========== Task 4.3: Mapping precedence ==========

    @Test
    void customMapping_takePrecedenceOverDefault() {
        // Default mapping for "string" is "String"
        assertEquals("String", TypeMapper.mapToJava("string").javaType());

        // Register custom mapping
        TypeMapper.registerCustomMapping("string", "CustomString", "string");

        // Custom mapping should override default
        assertEquals("CustomString", TypeMapper.mapToJava("string").javaType());
    }

    @Test
    void exactMapping_takesPrecedenceOverPattern() {
        // Register pattern mapping
        TypeMapper.registerPatternMapping("HTML.*", "PatternWidget", "HTMLElement");
        
        // Register exact mapping for specific type
        TypeMapper.registerCustomMapping("HTMLElement", "ExactWidget", "HTMLElement");

        // Exact mapping should take precedence
        assertEquals("ExactWidget", TypeMapper.mapToJava("HTMLElement").javaType());
        
        // Pattern should still match other types
        assertEquals("PatternWidget", TypeMapper.mapToJava("HTMLDivElement").javaType());
    }

    @Test
    void customMapping_doesNotAffectOptionalWrapping() {
        // Register custom mapping
        TypeMapper.registerCustomMapping("HTMLElement", "IsPWidget", "HTMLElement");

        // Optional wrapping should still work
        TypeMapping result = TypeMapper.mapToJava("HTMLElement | undefined");
        assertEquals("Optional<IsPWidget>", result.javaType());
        assertTrue(result.needsImport());
    }

    @Test
    void customMapping_doesNotAffectArrayWrapping() {
        // Register custom mapping
        TypeMapper.registerCustomMapping("HTMLElement", "IsPWidget", "HTMLElement");

        // Array wrapping should still work
        TypeMapping result = TypeMapper.mapToJava("HTMLElement[]");
        assertEquals("List<IsPWidget>", result.javaType());
        assertTrue(result.needsImport());
    }

    // ========== Task 4.3: Load custom mappings from configuration ==========

    @Test
    void loadCustomMappings_exactMappings() {
        Map<String, TypeMapper.CustomTypeMapping> config = new HashMap<>();
        config.put("HTMLElement", new TypeMapper.CustomTypeMapping("IsPWidget", "HTMLElement"));
        config.put("Date", new TypeMapper.CustomTypeMapping("LocalDateTime", "Date"));

        TypeMapper.loadCustomMappings(config);

        assertEquals("IsPWidget", TypeMapper.mapToJava("HTMLElement").javaType());
        assertEquals("LocalDateTime", TypeMapper.mapToJava("Date").javaType());
    }

    @Test
    void loadCustomMappings_patternMappings() {
        Map<String, TypeMapper.CustomTypeMapping> config = new HashMap<>();
        config.put("HTML.*Element", new TypeMapper.CustomTypeMapping("IsPWidget", "HTMLElement"));
        config.put(".*Event", new TypeMapper.CustomTypeMapping("CustomEvent", "Event"));

        TypeMapper.loadCustomMappings(config);

        assertEquals("IsPWidget", TypeMapper.mapToJava("HTMLDivElement").javaType());
        assertEquals("CustomEvent", TypeMapper.mapToJava("ClickEvent").javaType());
    }

    @Test
    void loadCustomMappings_mixedExactAndPattern() {
        Map<String, TypeMapper.CustomTypeMapping> config = new HashMap<>();
        config.put("HTMLElement", new TypeMapper.CustomTypeMapping("ExactWidget", "HTMLElement"));
        config.put("HTML.*", new TypeMapper.CustomTypeMapping("PatternWidget", "HTMLElement"));

        TypeMapper.loadCustomMappings(config);

        // Exact should take precedence
        assertEquals("ExactWidget", TypeMapper.mapToJava("HTMLElement").javaType());
        // Pattern should match others
        assertEquals("PatternWidget", TypeMapper.mapToJava("HTMLDivElement").javaType());
    }

    @Test
    void loadCustomMappings_emptyMap_doesNothing() {
        Map<String, TypeMapper.CustomTypeMapping> config = new HashMap<>();
        
        // Should not throw exception
        assertDoesNotThrow(() -> TypeMapper.loadCustomMappings(config));
        
        // Default mappings should still work
        assertEquals("String", TypeMapper.mapToJava("string").javaType());
    }

    @Test
    void loadCustomMappings_nullMap_doesNothing() {
        // Should not throw exception
        assertDoesNotThrow(() -> TypeMapper.loadCustomMappings(null));
        
        // Default mappings should still work
        assertEquals("String", TypeMapper.mapToJava("string").javaType());
    }

    // ========== Task 4.3: Clear custom mappings ==========

    @Test
    void clearCustomMappings_removesAllMappings() {
        // Register some mappings
        TypeMapper.registerCustomMapping("HTMLElement", "IsPWidget", "HTMLElement");
        TypeMapper.registerPatternMapping(".*Event", "CustomEvent", "Event");

        // Verify they work
        assertEquals("IsPWidget", TypeMapper.mapToJava("HTMLElement").javaType());
        assertEquals("CustomEvent", TypeMapper.mapToJava("ClickEvent").javaType());

        // Clear mappings
        TypeMapper.clearCustomMappings();

        // Should fall back to default behavior
        assertEquals("Object", TypeMapper.mapToJava("HTMLElement").javaType());
        assertEquals("Object", TypeMapper.mapToJava("ClickEvent").javaType());
    }

    @Test
    void clearCustomMappings_doesNotAffectDefaultMappings() {
        // Register custom mapping
        TypeMapper.registerCustomMapping("string", "CustomString", "string");
        assertEquals("CustomString", TypeMapper.mapToJava("string").javaType());

        // Clear custom mappings
        TypeMapper.clearCustomMappings();

        // Should revert to default mapping
        assertEquals("String", TypeMapper.mapToJava("string").javaType());
    }

    // ========== Task 4.3: TypeScript mapping with custom types ==========

    @Test
    void customMapping_affectsTypeScriptMapping() {
        TypeMapper.registerCustomMapping("HTMLElement", "IsPWidget", "Element");

        String tsResult = TypeMapper.mapToTypeScript("HTMLElement");
        assertEquals("Element", tsResult);
    }

    @Test
    void patternMapping_affectsTypeScriptMapping() {
        TypeMapper.registerPatternMapping("HTML.*Element", "IsPWidget", "Element");

        assertEquals("Element", TypeMapper.mapToTypeScript("HTMLDivElement"));
        assertEquals("Element", TypeMapper.mapToTypeScript("HTMLButtonElement"));
    }
}
