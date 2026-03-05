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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for TypeMapper.
 * <p>
 * Feature: ui-library-wrapper, Property 13: Code Generator Type Mapping
 * </p>
 * <p>
 * <b>Validates: Requirements 13.6</b>
 * </p>
 * <p>
 * For any property type string from the Custom Elements Manifest, the TypeMapper
 * SHALL produce the correct Java type according to the mapping rules, and the
 * mapping SHALL be deterministic (same input always produces same output).
 * </p>
 */
@Tag("Feature: ui-library-wrapper, Property 13: Code Generator Type Mapping")
public class TypeMapperPropertyTest {

    private static final Map<String, String> KNOWN_MAPPINGS = Map.of(
        "string", "String",
        "number", "double",
        "boolean", "boolean",
        "string | undefined", "Optional<String>",
        "Date", "String"
    );

    // ========== Property 1: Known type mappings always produce the correct Java type ==========

    /**
     * For any known type from the direct mapping set, TypeMapper SHALL produce
     * the expected Java type.
     * <p><b>Validates: Requirements 13.6</b></p>
     */
    @Property(tries = 100)
    void knownTypeMappingsAreCorrect(@ForAll("knownJsTypes") String jsType) {
        final String expected = KNOWN_MAPPINGS.get(jsType);
        final String actual = TypeMapper.mapToJavaType(jsType);
        assertEquals(expected, actual,
            "TypeMapper should map '" + jsType + "' to '" + expected + "'");
    }

    // ========== Property 2: Same input always produces same output (deterministic) ==========

    /**
     * For any type string, calling mapToJavaType twice with the same input
     * SHALL always produce the same output.
     * <p><b>Validates: Requirements 13.6</b></p>
     */
    @Property(tries = 100)
    void mappingIsDeterministic(@ForAll("allTypeStrings") String jsType) {
        final String first = TypeMapper.mapToJavaType(jsType);
        final String second = TypeMapper.mapToJavaType(jsType);
        assertEquals(first, second,
            "TypeMapper must be deterministic: two calls with '" + jsType + "' should return the same result");
    }

    // ========== Property 3: Union literal types always map to String ==========

    /**
     * For any union literal type (e.g. 'primary' | 'success' | 'neutral'),
     * TypeMapper SHALL map to "String".
     * <p><b>Validates: Requirements 13.6</b></p>
     */
    @Property(tries = 100)
    void unionLiteralTypesMapToString(@ForAll("unionLiteralTypes") String unionType) {
        final String result = TypeMapper.mapToJavaType(unionType);
        assertEquals("String", result,
            "Union literal type '" + unionType + "' should map to String");
    }

    // ========== Property 4: Null/blank inputs always map to Object ==========

    /**
     * For any null or blank input, TypeMapper SHALL map to "Object".
     * <p><b>Validates: Requirements 13.6</b></p>
     */
    @Property(tries = 100)
    void nullOrBlankInputsMapsToObject(@ForAll("nullOrBlankStrings") String input) {
        final String result = TypeMapper.mapToJavaType(input);
        assertEquals("Object", result,
            "Null or blank input should map to Object");
    }

    // ========== Property 5: Unknown types always map to Object ==========

    /**
     * For any type string that is not in the known mappings and is not a union literal,
     * TypeMapper SHALL map to "Object".
     * <p><b>Validates: Requirements 13.6</b></p>
     */
    @Property(tries = 100)
    void unknownTypesMapToObject(@ForAll("unknownTypes") String unknownType) {
        final String result = TypeMapper.mapToJavaType(unknownType);
        assertEquals("Object", result,
            "Unknown type '" + unknownType + "' should map to Object");
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<String> knownJsTypes() {
        return Arbitraries.of(KNOWN_MAPPINGS.keySet().toArray(new String[0]));
    }

    @Provide
    Arbitrary<String> allTypeStrings() {
        return Arbitraries.oneOf(
            knownJsTypes(),
            unionLiteralTypes(),
            unknownTypes(),
            Arbitraries.just(null),
            Arbitraries.of("", "   ", "\t", "\n")
        );
    }

    @Provide
    Arbitrary<String> unionLiteralTypes() {
        // Generate union literal types with 2-5 quoted alternatives
        final Arbitrary<String> singleValue = Arbitraries.of(
            "primary", "success", "neutral", "warning", "danger",
            "small", "medium", "large", "start", "end", "center",
            "top", "bottom", "left", "right", "auto", "none"
        );

        return singleValue.list().ofMinSize(2).ofMaxSize(5)
            .map(values -> values.stream()
                .map(v -> "'" + v + "'")
                .reduce((a, b) -> a + " | " + b)
                .orElse("'a' | 'b'"));
    }

    @Provide
    Arbitrary<String> nullOrBlankStrings() {
        return Arbitraries.oneOf(
            Arbitraries.just(null),
            Arbitraries.of("", " ", "   ", "\t", "\n", "  \t  ")
        );
    }

    @Provide
    Arbitrary<String> unknownTypes() {
        return Arbitraries.of(
            "HTMLElement", "CustomType", "void", "any", "undefined",
            "Array<string>", "Record<string, number>", "Promise<void>",
            "Map<string, string>", "Set<number>", "Function",
            "EventTarget", "Element", "Node", "CSSStyleDeclaration",
            "int", "float", "long", "char", "byte"
        );
    }
}
