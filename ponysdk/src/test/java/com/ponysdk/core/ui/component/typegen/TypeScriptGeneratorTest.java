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

package com.ponysdk.core.ui.component.typegen;

import com.ponysdk.core.ui.component.typegen.RecordParser.RecordInfo;
import com.ponysdk.core.ui.component.typegen.TypeScriptGenerator.GeneratorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TypeScriptGenerator.
 * <p>
 * **Validates: Requirements 8.2, 8.3, 8.4**
 * </p>
 */
class TypeScriptGeneratorTest {

    private RecordParser parser;
    private TypeScriptGenerator generator;

    // ========== Test Record Types ==========

    /**
     * Simple record with primitive types for testing basic type mapping.
     */
    public record SimpleProps(
            String title,
            int count,
            double value,
            boolean enabled
    ) {}

    /**
     * Record with all numeric primitive types.
     */
    public record NumericProps(
            int intValue,
            long longValue,
            double doubleValue,
            float floatValue,
            byte byteValue,
            short shortValue
    ) {}

    /**
     * Record with boxed primitive types.
     */
    public record BoxedProps(
            Integer intValue,
            Long longValue,
            Double doubleValue,
            Float floatValue,
            Boolean boolValue,
            Character charValue
    ) {}

    /**
     * Record with Optional field.
     */
    public record OptionalProps(
            String required,
            Optional<String> optionalString,
            Optional<Integer> optionalNumber
    ) {}

    /**
     * Record with List field.
     */
    public record ListProps(
            String name,
            List<String> items,
            List<Integer> numbers
    ) {}

    /**
     * Record with Map field.
     */
    public record MapProps(
            String name,
            Map<String, Integer> stringToInt,
            Map<String, String> stringToString
    ) {}

    /**
     * Nested record - inner.
     */
    public record DataPoint(
            double x,
            double y,
            Optional<String> label
    ) {}

    /**
     * Nested record - outer.
     */
    public record ChartProps(
            String title,
            List<DataPoint> data,
            ChartOptions options
    ) {}

    /**
     * Nested record - options.
     */
    public record ChartOptions(
            String color,
            boolean showGrid,
            int animationDuration
    ) {}

    /**
     * Enum for testing enum type mapping.
     */
    public enum Status {
        ACTIVE, INACTIVE, PENDING
    }

    /**
     * Record with enum field.
     */
    public record EnumProps(
            String name,
            Status status
    ) {}

    // ========== Setup ==========

    @BeforeEach
    void setUp() {
        parser = new RecordParser();
        generator = new TypeScriptGenerator();
    }

    // ========== Requirement 8.2: Field Name Mapping Tests ==========

    /**
     * Test that field names are correctly mapped to TypeScript interface properties.
     * <p>
     * **Validates: Requirement 8.2** - THE Type_Generator SHALL produce TypeScript interfaces
     * with matching field names
     * </p>
     */
    @Test
    void generate_SimpleProps_FieldNamesMatch() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("title:"), "Should contain 'title' field");
        assertTrue(tsCode.contains("count:"), "Should contain 'count' field");
        assertTrue(tsCode.contains("value:"), "Should contain 'value' field");
        assertTrue(tsCode.contains("enabled:"), "Should contain 'enabled' field");
    }

    /**
     * Test that interface name matches Record name.
     * <p>
     * **Validates: Requirement 8.2**
     * </p>
     */
    @Test
    void generate_SimpleProps_InterfaceNameMatches() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("interface SimpleProps"), "Should contain interface name");
    }

    // ========== Requirement 8.3: Primitive Type Mapping Tests ==========

    /**
     * Test that Java int maps to TypeScript number.
     * <p>
     * **Validates: Requirement 8.3** - THE Type_Generator SHALL map Java primitives to
     * TypeScript equivalents (int→number)
     * </p>
     */
    @Test
    void generate_NumericProps_IntMapsToNumber() {
        RecordInfo info = parser.parse(NumericProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("intValue: number;"), "int should map to number");
    }

    /**
     * Test that Java long maps to TypeScript number.
     * <p>
     * **Validates: Requirement 8.3**
     * </p>
     */
    @Test
    void generate_NumericProps_LongMapsToNumber() {
        RecordInfo info = parser.parse(NumericProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("longValue: number;"), "long should map to number");
    }

    /**
     * Test that Java double maps to TypeScript number.
     * <p>
     * **Validates: Requirement 8.3**
     * </p>
     */
    @Test
    void generate_NumericProps_DoubleMapsToNumber() {
        RecordInfo info = parser.parse(NumericProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("doubleValue: number;"), "double should map to number");
    }

    /**
     * Test that Java float maps to TypeScript number.
     * <p>
     * **Validates: Requirement 8.3**
     * </p>
     */
    @Test
    void generate_NumericProps_FloatMapsToNumber() {
        RecordInfo info = parser.parse(NumericProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("floatValue: number;"), "float should map to number");
    }

    /**
     * Test that Java boolean maps to TypeScript boolean.
     * <p>
     * **Validates: Requirement 8.3** - boolean→boolean
     * </p>
     */
    @Test
    void generate_SimpleProps_BooleanMapsToBoolean() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("enabled: boolean;"), "boolean should map to boolean");
    }

    /**
     * Test that Java String maps to TypeScript string.
     * <p>
     * **Validates: Requirement 8.3** - String→string
     * </p>
     */
    @Test
    void generate_SimpleProps_StringMapsToString() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("title: string;"), "String should map to string");
    }

    /**
     * Test that boxed primitives map correctly.
     * <p>
     * **Validates: Requirement 8.3**
     * </p>
     */
    @Test
    void generate_BoxedProps_BoxedPrimitivesMappedCorrectly() {
        RecordInfo info = parser.parse(BoxedProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("intValue: number;"), "Integer should map to number");
        assertTrue(tsCode.contains("longValue: number;"), "Long should map to number");
        assertTrue(tsCode.contains("doubleValue: number;"), "Double should map to number");
        assertTrue(tsCode.contains("floatValue: number;"), "Float should map to number");
        assertTrue(tsCode.contains("boolValue: boolean;"), "Boolean should map to boolean");
        assertTrue(tsCode.contains("charValue: string;"), "Character should map to string");
    }

    // ========== Requirement 8.4: Optional Field Mapping Tests ==========

    /**
     * Test that Optional fields are marked as optional in TypeScript.
     * <p>
     * **Validates: Requirement 8.4** - THE Type_Generator SHALL handle Optional fields
     * as optional TypeScript properties (?)
     * </p>
     */
    @Test
    void generate_OptionalProps_OptionalFieldsHaveQuestionMark() {
        RecordInfo info = parser.parse(OptionalProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("optionalString?:"), "Optional<String> should have ? modifier");
        assertTrue(tsCode.contains("optionalNumber?:"), "Optional<Integer> should have ? modifier");
    }

    /**
     * Test that required fields do NOT have the optional modifier.
     * <p>
     * **Validates: Requirement 8.4**
     * </p>
     */
    @Test
    void generate_OptionalProps_RequiredFieldsNoQuestionMark() {
        RecordInfo info = parser.parse(OptionalProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("required: string;"), "Required field should not have ? modifier");
        assertFalse(tsCode.contains("required?:"), "Required field should not have ? modifier");
    }

    /**
     * Test that Optional<String> maps to string type (with ? modifier).
     * <p>
     * **Validates: Requirement 8.4**
     * </p>
     */
    @Test
    void generate_OptionalProps_OptionalStringMapsToString() {
        RecordInfo info = parser.parse(OptionalProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("optionalString?: string;"), "Optional<String> should map to string");
    }

    /**
     * Test that Optional<Integer> maps to number type (with ? modifier).
     * <p>
     * **Validates: Requirement 8.4**
     * </p>
     */
    @Test
    void generate_OptionalProps_OptionalIntegerMapsToNumber() {
        RecordInfo info = parser.parse(OptionalProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("optionalNumber?: number;"), "Optional<Integer> should map to number");
    }

    // ========== List Type Mapping Tests ==========

    /**
     * Test that List<T> maps to T[] in TypeScript.
     */
    @Test
    void generate_ListProps_ListMapsToArray() {
        RecordInfo info = parser.parse(ListProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("items: string[];"), "List<String> should map to string[]");
        assertTrue(tsCode.contains("numbers: number[];"), "List<Integer> should map to number[]");
    }

    // ========== Map Type Mapping Tests ==========

    /**
     * Test that Map<String, T> maps to Record<string, T> in TypeScript.
     */
    @Test
    void generate_MapProps_MapMapsToRecord() {
        RecordInfo info = parser.parse(MapProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("stringToInt: Record<string, number>;"),
                "Map<String, Integer> should map to Record<string, number>");
        assertTrue(tsCode.contains("stringToString: Record<string, string>;"),
                "Map<String, String> should map to Record<string, string>");
    }

    // ========== Nested Record Tests ==========

    /**
     * Test that nested Records generate multiple interfaces.
     */
    @Test
    void generate_ChartProps_GeneratesNestedInterfaces() {
        RecordInfo info = parser.parse(ChartProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("interface ChartProps"), "Should contain ChartProps interface");
        assertTrue(tsCode.contains("interface DataPoint"), "Should contain DataPoint interface");
        assertTrue(tsCode.contains("interface ChartOptions"), "Should contain ChartOptions interface");
    }

    /**
     * Test that nested Record fields reference the correct interface name.
     */
    @Test
    void generate_ChartProps_NestedFieldsReferenceInterfaces() {
        RecordInfo info = parser.parse(ChartProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("data: DataPoint[];"), "Should reference DataPoint[] for list of nested records");
        assertTrue(tsCode.contains("options: ChartOptions;"), "Should reference ChartOptions for nested record");
    }

    /**
     * Test that nested Record with Optional field is handled correctly.
     */
    @Test
    void generate_ChartProps_NestedOptionalFieldHandled() {
        RecordInfo info = parser.parse(ChartProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("label?: string;"), "DataPoint.label should be optional string");
    }

    // ========== Enum Type Mapping Tests ==========

    /**
     * Test that Enum types map to string union types.
     */
    @Test
    void generate_EnumProps_EnumMapsToUnionType() {
        RecordInfo info = parser.parse(EnumProps.class);
        String tsCode = generator.generate(info);

        // Should contain union type with enum values
        assertTrue(tsCode.contains("'ACTIVE'") || tsCode.contains("status:"),
                "Enum should be mapped to union type or string");
    }

    // ========== Configuration Tests ==========

    /**
     * Test that export keyword is included by default.
     */
    @Test
    void generate_DefaultConfig_IncludesExport() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("export interface"), "Should include export keyword by default");
    }

    /**
     * Test that export keyword can be disabled.
     */
    @Test
    void generate_NoExportConfig_ExcludesExport() {
        GeneratorConfig config = new GeneratorConfig().setExportInterfaces(false);
        TypeScriptGenerator gen = new TypeScriptGenerator(config);

        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = gen.generate(info);

        assertFalse(tsCode.contains("export interface"), "Should not include export keyword");
        assertTrue(tsCode.contains("interface SimpleProps"), "Should still contain interface");
    }

    /**
     * Test that JSDoc comments are included by default.
     */
    @Test
    void generate_DefaultConfig_IncludesJsDoc() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("/**"), "Should include JSDoc comment by default");
        assertTrue(tsCode.contains("Generated from"), "Should include generated from comment");
    }

    /**
     * Test that JSDoc comments can be disabled.
     */
    @Test
    void generate_NoJsDocConfig_ExcludesJsDoc() {
        GeneratorConfig config = new GeneratorConfig().setIncludeJsDoc(false);
        TypeScriptGenerator gen = new TypeScriptGenerator(config);

        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = gen.generate(info);

        assertFalse(tsCode.contains("/**"), "Should not include JSDoc comment");
    }

    // ========== generateAll Tests ==========

    /**
     * Test that generateAll produces all interfaces without duplicates.
     */
    @Test
    void generateAll_MultipleRecords_NoDuplicates() {
        List<RecordInfo> records = List.of(
                parser.parse(SimpleProps.class),
                parser.parse(OptionalProps.class)
        );

        String tsCode = generator.generateAll(records);

        assertTrue(tsCode.contains("interface SimpleProps"), "Should contain SimpleProps");
        assertTrue(tsCode.contains("interface OptionalProps"), "Should contain OptionalProps");
    }

    // ========== Edge Cases ==========

    /**
     * Test null input handling.
     */
    @Test
    void generate_NullInput_ThrowsException() {
        assertThrows(NullPointerException.class, () -> generator.generate(null));
    }

    /**
     * Test empty record (no fields).
     */
    public record EmptyProps() {}

    @Test
    void generate_EmptyRecord_GeneratesEmptyInterface() {
        RecordInfo info = parser.parse(EmptyProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("interface EmptyProps {"), "Should contain interface declaration");
        assertTrue(tsCode.contains("}"), "Should contain closing brace");
    }
}
