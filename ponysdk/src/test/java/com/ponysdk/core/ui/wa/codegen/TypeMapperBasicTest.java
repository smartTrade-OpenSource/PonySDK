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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic unit tests for TypeMapper to verify Task 4.1 implementation.
 * Tests primitive types, optional types, array types, and TypeScript mapping.
 */
class TypeMapperBasicTest {

    // ========== Task 4.1: Primitive type mappings ==========

    @Test
    void mapToJava_string() {
        TypeMapping result = TypeMapper.mapToJava("string");
        assertEquals("String", result.javaType());
        assertFalse(result.needsImport());
        assertFalse(result.isFallback());
    }

    @Test
    void mapToJava_number() {
        TypeMapping result = TypeMapper.mapToJava("number");
        assertEquals("double", result.javaType());
        assertFalse(result.needsImport());
    }

    @Test
    void mapToJava_boolean() {
        TypeMapping result = TypeMapper.mapToJava("boolean");
        assertEquals("boolean", result.javaType());
        assertFalse(result.needsImport());
    }

    // ========== Task 4.1: Optional types ==========

    @Test
    void mapToJava_optionalString() {
        TypeMapping result = TypeMapper.mapToJava("string | undefined");
        assertEquals("Optional<String>", result.javaType());
        assertTrue(result.needsImport());
        assertEquals("java.util", result.importPackage());
    }

    @Test
    void mapToJava_optionalNumber() {
        TypeMapping result = TypeMapper.mapToJava("number | undefined");
        assertEquals("Optional<Double>", result.javaType());
        assertTrue(result.needsImport());
        assertEquals("java.util", result.importPackage());
    }

    // ========== Task 4.1: Array types ==========

    @Test
    void mapToJava_stringArray() {
        TypeMapping result = TypeMapper.mapToJava("string[]");
        assertEquals("List<String>", result.javaType());
        assertTrue(result.needsImport());
        assertEquals("java.util", result.importPackage());
    }

    @Test
    void mapToJava_numberArray() {
        TypeMapping result = TypeMapper.mapToJava("number[]");
        assertEquals("List<Double>", result.javaType());
        assertTrue(result.needsImport());
        assertEquals("java.util", result.importPackage());
    }

    @Test
    void mapToJava_booleanArray() {
        TypeMapping result = TypeMapper.mapToJava("boolean[]");
        assertEquals("List<Boolean>", result.javaType());
        assertTrue(result.needsImport());
    }

    // ========== Task 4.1: TypeScript mapping ==========

    @Test
    void mapToTypeScript_string() {
        String result = TypeMapper.mapToTypeScript("string");
        assertEquals("string", result);
    }

    @Test
    void mapToTypeScript_number() {
        String result = TypeMapper.mapToTypeScript("number");
        assertEquals("number", result);
    }

    @Test
    void mapToTypeScript_boolean() {
        String result = TypeMapper.mapToTypeScript("boolean");
        assertEquals("boolean", result);
    }

    @Test
    void mapToTypeScript_optionalString() {
        String result = TypeMapper.mapToTypeScript("string | undefined");
        assertEquals("string | undefined", result);
    }

    @Test
    void mapToTypeScript_stringArray() {
        String result = TypeMapper.mapToTypeScript("string[]");
        assertEquals("string[]", result);
    }

    @Test
    void mapToTypeScript_unionLiteral() {
        String result = TypeMapper.mapToTypeScript("'primary' | 'success' | 'neutral'");
        assertEquals("'primary' | 'success' | 'neutral'", result);
    }

    // ========== Fallback behavior ==========

    @Test
    void mapToJava_unmappableType() {
        TypeMapping result = TypeMapper.mapToJava("ComplexUnionType");
        assertEquals("Object", result.javaType());
        assertTrue(result.isFallback());
    }

    @Test
    void mapToTypeScript_unmappableType() {
        String result = TypeMapper.mapToTypeScript("ComplexUnionType");
        assertEquals("unknown", result);
    }

    // ========== Backward compatibility ==========

    @Test
    void mapToJavaType_legacyMethod_string() {
        @SuppressWarnings("deprecation")
        String result = TypeMapper.mapToJavaType("string");
        assertEquals("String", result);
    }

    @Test
    void mapToJavaType_legacyMethod_optionalString() {
        @SuppressWarnings("deprecation")
        String result = TypeMapper.mapToJavaType("string | undefined");
        assertEquals("Optional<String>", result);
    }

    @Test
    void mapToJavaType_legacyMethod_stringArray() {
        @SuppressWarnings("deprecation")
        String result = TypeMapper.mapToJavaType("string[]");
        assertEquals("List<String>", result);
    }

    // ========== Task 4.2: Union literal types (enum generation) ==========

    @Test
    void mapToJava_unionLiteral_fallsBackToString() {
        // Until code generator supports enum generation, union literals map to String
        TypeMapping result = TypeMapper.mapToJava("'small' | 'medium' | 'large'");
        assertEquals("String", result.javaType());
        assertFalse(result.needsRecordGeneration());
    }

    @Test
    void mapToJava_unionLiteral_twoValues() {
        TypeMapping result = TypeMapper.mapToJava("'on' | 'off'");
        assertEquals("String", result.javaType());
    }

    @Test
    void mapToTypeScript_unionLiteral_preserved() {
        String result = TypeMapper.mapToTypeScript("'primary' | 'success' | 'neutral'");
        assertEquals("'primary' | 'success' | 'neutral'", result);
    }

    // ========== Task 4.2: Object type handling (record generation) ==========

    @Test
    void mapToJava_objectType_fallsBackToObject() {
        // Until code generator supports record generation, object types map to Object
        TypeMapping result = TypeMapper.mapToJava("{ x: number, y: number }");
        assertEquals("Object", result.javaType());
        assertFalse(result.needsRecordGeneration());
    }

    @Test
    void mapToJava_objectType_withStringField() {
        TypeMapping result = TypeMapper.mapToJava("{ name: string, age: number }");
        assertEquals("Object", result.javaType());
    }

    @Test
    void mapToTypeScript_objectType_preserved() {
        String result = TypeMapper.mapToTypeScript("{ x: number, y: number }");
        assertEquals("{ x: number, y: number }", result);
    }

    // ========== Task 4.2: Complex union fallback with TODO ==========

    @Test
    void mapToJava_complexUnion_fallbackWithTodo() {
        TypeMapping result = TypeMapper.mapToJava("string | number | boolean");
        assertEquals("Object", result.javaType());
        assertTrue(result.isFallback());
        assertNotNull(result.recordDefinition());
        assertTrue(result.recordDefinition().contains("TODO"));
        assertTrue(result.recordDefinition().contains("Complex union"));
    }

    @Test
    void mapToJava_complexUnionWithObject_fallbackWithTodo() {
        TypeMapping result = TypeMapper.mapToJava("string | { custom: boolean }");
        assertEquals("Object", result.javaType());
        assertTrue(result.isFallback());
        assertNotNull(result.recordDefinition());
        assertTrue(result.recordDefinition().contains("TODO"));
    }

    @Test
    void mapToTypeScript_complexUnion_preserved() {
        String result = TypeMapper.mapToTypeScript("string | number | boolean");
        assertEquals("string | number | boolean", result);
    }

    // ========== Task 4.2: Helper methods for future enum/record generation ==========

    @Test
    void handleUnionLiterals_generatesEnumDefinition() {
        // Test the helper method directly (will be used by code generator in future tasks)
        TypeMapping result = TypeMapper.handleUnionLiterals("'small' | 'medium' | 'large'");
        assertEquals("GeneratedEnum", result.javaType());
        assertTrue(result.needsRecordGeneration());
        assertNotNull(result.recordDefinition());
        assertTrue(result.recordDefinition().contains("public enum GeneratedEnum"));
        assertTrue(result.recordDefinition().contains("SMALL(\"small\")"));
        assertTrue(result.recordDefinition().contains("MEDIUM(\"medium\")"));
        assertTrue(result.recordDefinition().contains("LARGE(\"large\")"));
    }

    @Test
    void handleObjectType_generatesRecordDefinition() {
        // Test the helper method directly (will be used by code generator in future tasks)
        TypeMapping result = TypeMapper.handleObjectType("{ x: number, y: number }");
        assertEquals("GeneratedRecord", result.javaType());
        assertTrue(result.needsRecordGeneration());
        assertNotNull(result.recordDefinition());
        assertTrue(result.recordDefinition().contains("public record GeneratedRecord"));
        assertTrue(result.recordDefinition().contains("double x"));
        assertTrue(result.recordDefinition().contains("double y"));
    }
}
