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

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for type mapping fallback behavior.
 * <p>
 * <b>Property 7: Type Mapping Fallback</b>
 * </p>
 * <p>
 * <b>Validates: Requirements 5.7</b>
 * </p>
 * <p>
 * For any CEM type that cannot be cleanly mapped, the Type Mapper should use
 * Object in Java and unknown in TypeScript, and include a TODO comment in the
 * recordDefinition field.
 * </p>
 */
@Tag("Feature: generic-webcomponent-wrapper, Property 7: Type Mapping Fallback")
public class TypeMappingFallbackPropertyTest {

    /**
     * For any unmappable CEM type, Java mapping should fall back to Object
     * with isFallback() returning true and a TODO comment in recordDefinition.
     * <p><b>Validates: Requirements 5.7</b></p>
     */
    @Property(tries = 100)
    void unmappableTypesFallbackToObject(@ForAll("unmappableTypes") String cemType) {
        final TypeMapping javaMapping = TypeMapper.mapToJava(cemType);
        
        // Java should fall back to Object
        assertEquals("Object", javaMapping.javaType(),
            String.format("Unmappable type '%s' should map to Object in Java", cemType));
        
        // Should be marked as fallback
        assertTrue(javaMapping.isFallback(),
            String.format("Unmappable type '%s' should be marked as fallback", cemType));
        
        // Should have TODO comment
        assertNotNull(javaMapping.recordDefinition(),
            String.format("Unmappable type '%s' should have a TODO comment", cemType));
        assertTrue(javaMapping.recordDefinition().contains("TODO"),
            String.format("Unmappable type '%s' should have a TODO comment containing 'TODO'", cemType));
    }

    /**
     * For any unmappable CEM type, TypeScript mapping should fall back to unknown.
     * <p><b>Validates: Requirements 5.7</b></p>
     */
    @Property(tries = 100)
    void unmappableTypesFallbackToUnknown(@ForAll("unmappableTypes") String cemType) {
        final String tsType = TypeMapper.mapToTypeScript(cemType);
        
        // TypeScript should fall back to unknown
        assertEquals("unknown", tsType,
            String.format("Unmappable type '%s' should map to unknown in TypeScript", cemType));
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<String> unmappableTypes() {
        return Arbitraries.oneOf(
            complexUnions(),
            unknownTypes(),
            mixedUnions(),
            edgeCases()
        );
    }

    /**
     * Generates complex union types with 3+ different type categories.
     * Examples: "string | number | boolean", "number | { x: number }"
     */
    @Provide
    Arbitrary<String> complexUnions() {
        final Arbitrary<String> primitives = Arbitraries.of("string", "number", "boolean");
        final Arbitrary<String> objects = Arbitraries.of(
            "{ custom: boolean }",
            "{ x: number, y: number }",
            "{ value: string }"
        );
        final Arbitrary<String> customTypes = Arbitraries.of("HTMLElement", "CustomType", "MyInterface");
        
        // Generate unions with 3+ different types
        return Arbitraries.oneOf(
            // Primitive unions (3 types)
            Combinators.combine(primitives, primitives, primitives)
                .as((t1, t2, t3) -> String.format("%s | %s | %s", t1, t2, t3)),
            
            // Mixed primitive and object
            Combinators.combine(primitives, objects)
                .as((p, o) -> String.format("%s | %s", p, o)),
            
            // Mixed primitive and custom type
            Combinators.combine(primitives, customTypes)
                .as((p, c) -> String.format("%s | %s", p, c)),
            
            // Complex 4-type union
            Combinators.combine(primitives, primitives, objects, customTypes)
                .as((t1, t2, o, c) -> String.format("%s | %s | %s | %s", t1, t2, o, c))
        );
    }

    /**
     * Generates unknown/custom type identifiers.
     * Examples: "CustomType", "MyInterface", "SomeLibraryType"
     */
    @Provide
    Arbitrary<String> unknownTypes() {
        final Arbitrary<String> prefix = Arbitraries.of("Custom", "My", "Some", "Library", "Unknown");
        final Arbitrary<String> suffix = Arbitraries.of("Type", "Interface", "Class", "Element", "Component");
        
        return Combinators.combine(prefix, suffix)
            .as((p, s) -> p + s);
    }

    /**
     * Generates mixed unions combining primitives and objects.
     * Examples: "number | { x: number }", "string | HTMLElement"
     */
    @Provide
    Arbitrary<String> mixedUnions() {
        final Arbitrary<String> primitives = Arbitraries.of("string", "number", "boolean");
        final Arbitrary<String> complex = Arbitraries.of(
            "{ x: number }",
            "HTMLElement",
            "CustomType",
            "{ value: string, label: string }"
        );
        
        return Combinators.combine(primitives, complex)
            .as((p, c) -> String.format("%s | %s", p, c));
    }

    /**
     * Generates edge case types.
     * Examples: empty string, whitespace, null-like strings
     */
    @Provide
    Arbitrary<String> edgeCases() {
        return Arbitraries.of(
            "",           // Empty string
            "   ",        // Whitespace only
            "null",       // Null as string
            "undefined",  // Undefined as string (not "| undefined")
            "any",        // TypeScript any
            "never"       // TypeScript never
        );
    }
}
