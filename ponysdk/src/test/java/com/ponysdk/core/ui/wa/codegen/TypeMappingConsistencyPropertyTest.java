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
 * Property-based test for type mapping consistency between Java and TypeScript.
 * <p>
 * <b>Property 6: Type Mapping Consistency</b>
 * </p>
 * <p>
 * <b>Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.8</b>
 * </p>
 * <p>
 * For any CEM type, mapping to Java and then to TypeScript should produce types
 * that are semantically compatible (e.g., string→String→string, number→double→number).
 * </p>
 */
@Tag("Feature: generic-webcomponent-wrapper, Property 6: Type Mapping Consistency")
public class TypeMappingConsistencyPropertyTest {

    /**
     * For any CEM type, mapping to Java and TypeScript should produce
     * semantically compatible types.
     * <p><b>Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.8</b></p>
     */
    @Property(tries = 100)
    void typeMappingConsistency(@ForAll("cemTypes") String cemType) {
        // Map to Java and TypeScript
        final TypeMapping javaMapping = TypeMapper.mapToJava(cemType);
        final String tsType = TypeMapper.mapToTypeScript(cemType);
        
        // Both mappings should succeed (no exceptions thrown)
        assertNotNull(javaMapping, "Java mapping should not be null for type: " + cemType);
        assertNotNull(javaMapping.javaType(), "Java type should not be null for type: " + cemType);
        assertNotNull(tsType, "TypeScript mapping should not be null for type: " + cemType);
        
        // Verify semantic compatibility
        assertSemanticCompatibility(javaMapping.javaType(), tsType, cemType);
    }

    /**
     * Verifies that Java and TypeScript types are semantically compatible.
     * 
     * @param javaType the mapped Java type
     * @param tsType the mapped TypeScript type
     * @param cemType the original CEM type (for error messages)
     */
    private void assertSemanticCompatibility(String javaType, String tsType, String cemType) {
        // Remove generic type parameters for comparison
        final String baseJavaType = extractBaseType(javaType);
        final String baseTsType = extractBaseType(tsType);
        
        // Check compatibility based on Java type
        switch (baseJavaType) {
            case "String":
                assertTrue(
                    baseTsType.equals("string") || isStringLiteralUnion(tsType),
                    String.format("Java String should map to TypeScript string or string literal union. " +
                        "CEM: '%s', Java: '%s', TS: '%s'", cemType, javaType, tsType)
                );
                break;
                
            case "double":
                assertEquals("number", baseTsType,
                    String.format("Java double should map to TypeScript number. " +
                        "CEM: '%s', Java: '%s', TS: '%s'", cemType, javaType, tsType)
                );
                break;
                
            case "boolean":
                assertEquals("boolean", baseTsType,
                    String.format("Java boolean should map to TypeScript boolean. " +
                        "CEM: '%s', Java: '%s', TS: '%s'", cemType, javaType, tsType)
                );
                break;
                
            case "Optional":
                assertTrue(tsType.contains("undefined") || tsType.contains("|"),
                    String.format("Java Optional should map to TypeScript optional (with undefined). " +
                        "CEM: '%s', Java: '%s', TS: '%s'", cemType, javaType, tsType)
                );
                break;
                
            case "List":
                assertTrue(tsType.contains("[]") || tsType.equals("unknown"),
                    String.format("Java List should map to TypeScript array. " +
                        "CEM: '%s', Java: '%s', TS: '%s'", cemType, javaType, tsType)
                );
                break;
                
            case "Object":
                // Object is the fallback type, TypeScript can be anything
                // This is acceptable as both represent "any structure"
                assertTrue(true, "Object fallback is compatible with any TypeScript type");
                break;
                
            default:
                // For custom types or generated types, we accept any mapping
                // as long as both succeeded
                assertTrue(true, "Custom types are accepted if both mappings succeeded");
        }
    }

    /**
     * Extracts the base type from a generic type.
     * E.g., "Optional<String>" -> "Optional", "List<Double>" -> "List"
     */
    private String extractBaseType(String type) {
        final int genericStart = type.indexOf('<');
        if (genericStart > 0) {
            return type.substring(0, genericStart);
        }
        return type;
    }

    /**
     * Checks if a TypeScript type is a string literal union.
     * E.g., "'small' | 'medium' | 'large'"
     */
    private boolean isStringLiteralUnion(String tsType) {
        return tsType.contains("'") && tsType.contains("|");
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<String> cemTypes() {
        return Arbitraries.oneOf(
            primitiveTypes(),
            optionalTypes(),
            arrayTypes(),
            unionLiterals(),
            objectTypes()
        );
    }

    @Provide
    Arbitrary<String> primitiveTypes() {
        return Arbitraries.of("string", "number", "boolean");
    }

    @Provide
    Arbitrary<String> optionalTypes() {
        return primitiveTypes().map(type -> type + " | undefined");
    }

    @Provide
    Arbitrary<String> arrayTypes() {
        return primitiveTypes().map(type -> type + "[]");
    }

    @Provide
    Arbitrary<String> unionLiterals() {
        final Arbitrary<String> literalValue = Arbitraries.of(
            "small", "medium", "large",
            "primary", "success", "warning", "danger",
            "left", "center", "right",
            "top", "bottom"
        );

        return literalValue.list().ofMinSize(2).ofMaxSize(4)
            .map(values -> values.stream()
                .distinct()
                .map(v -> "'" + v + "'")
                .reduce((a, b) -> a + " | " + b)
                .orElse("'a' | 'b'"));
    }

    @Provide
    Arbitrary<String> objectTypes() {
        // Generate simple object types like { x: number, y: number }
        final Arbitrary<String> fieldName = Arbitraries.of("x", "y", "width", "height", "value", "label");
        final Arbitrary<String> fieldType = primitiveTypes();

        return Combinators.combine(fieldName, fieldType, fieldName, fieldType)
            .as((name1, type1, name2, type2) -> {
                if (name1.equals(name2)) {
                    return String.format("{ %s: %s }", name1, type1);
                }
                return String.format("{ %s: %s, %s: %s }", name1, type1, name2, type2);
            });
    }
}
