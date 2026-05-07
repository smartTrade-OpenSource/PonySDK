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

import java.util.List;

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

    // ========== Task 1.4: extractLiteralValues() method ==========

    @Test
    void extractLiteralValues_simpleUnionLiteral() {
        List<String> result = TypeMapper.extractLiteralValues("'small' | 'medium' | 'large'");
        assertEquals(List.of("small", "medium", "large"), result);
    }

    @Test
    void extractLiteralValues_twoValues() {
        List<String> result = TypeMapper.extractLiteralValues("'on' | 'off'");
        assertEquals(List.of("on", "off"), result);
    }

    @Test
    void extractLiteralValues_withWhitespaceVariations() {
        List<String> result = TypeMapper.extractLiteralValues("'a'|'b'|'c'");
        assertEquals(List.of("a", "b", "c"), result);
    }

    @Test
    void extractLiteralValues_withExtraWhitespace() {
        List<String> result = TypeMapper.extractLiteralValues("  'small'  |  'medium'  |  'large'  ");
        assertEquals(List.of("small", "medium", "large"), result);
    }

    @Test
    void extractLiteralValues_optionalUnionLiteral_filtersUndefined() {
        List<String> result = TypeMapper.extractLiteralValues("'a' | 'b' | undefined");
        assertEquals(List.of("a", "b"), result);
    }

    @Test
    void extractLiteralValues_optionalUnionLiteral_singleValue() {
        List<String> result = TypeMapper.extractLiteralValues("'only' | undefined");
        assertEquals(List.of("only"), result);
    }

    @Test
    void extractLiteralValues_nullInput() {
        List<String> result = TypeMapper.extractLiteralValues(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void extractLiteralValues_emptyInput() {
        List<String> result = TypeMapper.extractLiteralValues("");
        assertTrue(result.isEmpty());
    }

    @Test
    void extractLiteralValues_blankInput() {
        List<String> result = TypeMapper.extractLiteralValues("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    void extractLiteralValues_nonUnionLiteralType() {
        List<String> result = TypeMapper.extractLiteralValues("string");
        assertTrue(result.isEmpty());
    }

    @Test
    void extractLiteralValues_complexUnion() {
        // Complex unions (not pure union literals) should return empty list
        List<String> result = TypeMapper.extractLiteralValues("string | number");
        assertTrue(result.isEmpty());
    }

    @Test
    void extractLiteralValues_withHyphenatedValues() {
        List<String> result = TypeMapper.extractLiteralValues("'beat-fade' | 'flip-both' | 'spin-pulse'");
        assertEquals(List.of("beat-fade", "flip-both", "spin-pulse"), result);
    }

    // ========== Task 2.1: generateEnumName() method ==========

    @Test
    void generateEnumName_simpleComponentAndProperty() {
        String result = TypeMapper.generateEnumName("wa-button", "variant");
        assertEquals("ButtonVariant", result);
    }

    @Test
    void generateEnumName_iconSize() {
        String result = TypeMapper.generateEnumName("wa-icon", "size");
        assertEquals("IconSize", result);
    }

    @Test
    void generateEnumName_multiPartComponentName() {
        String result = TypeMapper.generateEnumName("wa-progress-bar", "appearance");
        assertEquals("ProgressBarAppearance", result);
    }

    @Test
    void generateEnumName_multiPartPropertyName() {
        String result = TypeMapper.generateEnumName("wa-button", "loading-state");
        assertEquals("ButtonLoadingState", result);
    }

    @Test
    void generateEnumName_bothMultiPart() {
        String result = TypeMapper.generateEnumName("wa-date-picker", "display-mode");
        assertEquals("DatePickerDisplayMode", result);
    }

    @Test
    void generateEnumName_withoutWaPrefix() {
        // Should still work even without wa- prefix
        String result = TypeMapper.generateEnumName("button", "variant");
        assertEquals("ButtonVariant", result);
    }

    @Test
    void generateEnumName_caseInsensitiveWaPrefix() {
        // wa- prefix stripping should be case-insensitive
        String result = TypeMapper.generateEnumName("WA-button", "variant");
        assertEquals("ButtonVariant", result);
    }

    @Test
    void generateEnumName_withWhitespace() {
        String result = TypeMapper.generateEnumName("  wa-button  ", "  variant  ");
        assertEquals("ButtonVariant", result);
    }

    @Test
    void generateEnumName_nullComponentName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            TypeMapper.generateEnumName(null, "variant"));
    }

    @Test
    void generateEnumName_emptyComponentName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            TypeMapper.generateEnumName("", "variant"));
    }

    @Test
    void generateEnumName_blankComponentName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            TypeMapper.generateEnumName("   ", "variant"));
    }

    @Test
    void generateEnumName_nullPropertyName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            TypeMapper.generateEnumName("wa-button", null));
    }

    @Test
    void generateEnumName_emptyPropertyName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            TypeMapper.generateEnumName("wa-button", ""));
    }

    @Test
    void generateEnumName_blankPropertyName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            TypeMapper.generateEnumName("wa-button", "   "));
    }

    @Test
    void generateEnumName_preservesCasingAfterHyphen() {
        // Each part after hyphen should be capitalized, rest lowercased
        String result = TypeMapper.generateEnumName("wa-BUTTON", "VARIANT");
        assertEquals("ButtonVariant", result);
    }

    @Test
    void generateEnumName_singleCharacterParts() {
        String result = TypeMapper.generateEnumName("wa-a-b-c", "x-y");
        assertEquals("ABCXY", result);
    }

    @Test
    void generateEnumName_consecutiveHyphens() {
        // Consecutive hyphens result in empty parts which are skipped
        String result = TypeMapper.generateEnumName("wa-button--extra", "variant");
        assertEquals("ButtonExtraVariant", result);
    }

    // ========== Task 5.1: mapToJavaWithContext() method ==========

    @Test
    void mapToJavaWithContext_unionLiteral_generatesEnumMapping() {
        // Clear enum cache to ensure fresh generation
        TypeMapper.clearEnumCache();
        
        TypeMapping result = TypeMapper.mapToJavaWithContext(
            "'neutral' | 'brand' | 'success'",
            "wa-button",
            "variant"
        );
        
        assertEquals("ButtonVariant", result.javaType());
        assertTrue(result.needsRecordGeneration());
        assertTrue(result.needsImport());
        assertEquals("com.ponysdk.core.ui.wa.enums", result.importPackage());
        assertNotNull(result.recordDefinition());
        assertTrue(result.recordDefinition().contains("package com.ponysdk.core.ui.wa.enums;"));
        assertTrue(result.recordDefinition().contains("public enum ButtonVariant"));
        assertTrue(result.recordDefinition().contains("NEUTRAL(\"neutral\")"));
        assertTrue(result.recordDefinition().contains("BRAND(\"brand\")"));
        assertTrue(result.recordDefinition().contains("SUCCESS(\"success\")"));
    }

    @Test
    void mapToJavaWithContext_optionalUnionLiteral_generatesEnumMapping() {
        TypeMapper.clearEnumCache();
        
        TypeMapping result = TypeMapper.mapToJavaWithContext(
            "'small' | 'medium' | 'large' | undefined",
            "wa-icon",
            "size"
        );
        
        assertEquals("IconSize", result.javaType());
        assertTrue(result.needsRecordGeneration());
        assertTrue(result.needsImport());
        assertEquals("com.ponysdk.core.ui.wa.enums", result.importPackage());
        assertNotNull(result.recordDefinition());
        // Should not include undefined as an enum constant
        assertFalse(result.recordDefinition().contains("UNDEFINED"));
        assertTrue(result.recordDefinition().contains("SMALL(\"small\")"));
        assertTrue(result.recordDefinition().contains("MEDIUM(\"medium\")"));
        assertTrue(result.recordDefinition().contains("LARGE(\"large\")"));
    }

    @Test
    void mapToJavaWithContext_nonUnionLiteral_delegatesToMapToJava() {
        TypeMapping result = TypeMapper.mapToJavaWithContext(
            "string",
            "wa-button",
            "label"
        );
        
        assertEquals("String", result.javaType());
        assertFalse(result.needsRecordGeneration());
        assertFalse(result.needsImport());
    }

    @Test
    void mapToJavaWithContext_optionalString_delegatesToMapToJava() {
        TypeMapping result = TypeMapper.mapToJavaWithContext(
            "string | undefined",
            "wa-button",
            "label"
        );
        
        assertEquals("Optional<String>", result.javaType());
        assertTrue(result.needsImport());
        assertEquals("java.util", result.importPackage());
        assertFalse(result.needsRecordGeneration());
    }

    @Test
    void mapToJavaWithContext_number_delegatesToMapToJava() {
        TypeMapping result = TypeMapper.mapToJavaWithContext(
            "number",
            "wa-slider",
            "value"
        );
        
        assertEquals("double", result.javaType());
        assertFalse(result.needsImport());
        assertFalse(result.needsRecordGeneration());
    }

    @Test
    void mapToJavaWithContext_enumSourceContainsFromValueMethod() {
        TypeMapper.clearEnumCache();
        
        TypeMapping result = TypeMapper.mapToJavaWithContext(
            "'a' | 'b'",
            "wa-test",
            "prop"
        );
        
        String enumSource = result.recordDefinition();
        assertTrue(enumSource.contains("public static TestProp fromValue(String value)"));
        assertTrue(enumSource.contains("throw new IllegalArgumentException"));
        assertTrue(enumSource.contains("Unknown TestProp value"));
    }

    @Test
    void mapToJavaWithContext_enumSourceContainsGetValueMethod() {
        TypeMapper.clearEnumCache();
        
        TypeMapping result = TypeMapper.mapToJavaWithContext(
            "'x' | 'y'",
            "wa-coord",
            "axis"
        );
        
        String enumSource = result.recordDefinition();
        assertTrue(enumSource.contains("public String getValue()"));
        assertTrue(enumSource.contains("return value;"));
    }

    @Test
    void mapToJavaWithContext_enumSourceContainsPrivateValueField() {
        TypeMapper.clearEnumCache();
        
        TypeMapping result = TypeMapper.mapToJavaWithContext(
            "'red' | 'green' | 'blue'",
            "wa-color",
            "primary"
        );
        
        String enumSource = result.recordDefinition();
        assertTrue(enumSource.contains("private final String value;"));
    }

    @Test
    void mapToJavaWithContext_enumSourceContainsPrivateConstructor() {
        TypeMapper.clearEnumCache();
        
        TypeMapping result = TypeMapper.mapToJavaWithContext(
            "'on' | 'off'",
            "wa-switch",
            "state"
        );
        
        String enumSource = result.recordDefinition();
        assertTrue(enumSource.contains("SwitchState(String value)"));
    }

    @Test
    void mapToJavaWithContext_hyphenatedLiterals_convertedToUpperSnakeCase() {
        TypeMapper.clearEnumCache();
        
        TypeMapping result = TypeMapper.mapToJavaWithContext(
            "'beat-fade' | 'flip-both' | 'spin-pulse'",
            "wa-icon",
            "animation"
        );
        
        String enumSource = result.recordDefinition();
        assertTrue(enumSource.contains("BEAT_FADE(\"beat-fade\")"));
        assertTrue(enumSource.contains("FLIP_BOTH(\"flip-both\")"));
        assertTrue(enumSource.contains("SPIN_PULSE(\"spin-pulse\")"));
    }

    @Test
    void mapToJavaWithContext_complexUnion_delegatesToMapToJava() {
        TypeMapping result = TypeMapper.mapToJavaWithContext(
            "string | number",
            "wa-input",
            "value"
        );
        
        assertEquals("Object", result.javaType());
        assertTrue(result.isFallback());
    }
}
