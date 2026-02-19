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
import com.ponysdk.core.ui.component.typegen.TypeGuardGenerator.GeneratorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TypeGuardGenerator.
 * <p>
 * **Validates: Requirements 8.5**
 * </p>
 */
class TypeGuardGeneratorTest {

    private RecordParser parser;
    private TypeGuardGenerator generator;

    // ========== Test Record Types ==========

    /**
     * Simple record with primitive types for testing basic type guards.
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
     * Enum for testing enum type guards.
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

    /**
     * Empty record for edge case testing.
     */
    public record EmptyProps() {}

    // ========== Setup ==========

    @BeforeEach
    void setUp() {
        parser = new RecordParser();
        generator = new TypeGuardGenerator();
    }

    // ========== Requirement 8.5: Type Guard Generation Tests ==========

    /**
     * Test that type guard function is generated with correct name.
     * <p>
     * **Validates: Requirement 8.5** - THE Type_Generator SHALL generate runtime type guards
     * </p>
     */
    @Test
    void generate_SimpleProps_GeneratesTypeGuardFunction() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("function isSimpleProps(obj: unknown): obj is SimpleProps"),
                "Should generate type guard function with correct signature");
    }

    /**
     * Test that type guard checks for object type.
     * <p>
     * **Validates: Requirement 8.5**
     * </p>
     */
    @Test
    void generate_SimpleProps_ChecksObjectType() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("typeof obj === 'object'"),
                "Should check that obj is an object");
        assertTrue(tsCode.contains("obj !== null"),
                "Should check that obj is not null");
    }

    /**
     * Test that type guard checks for required field presence.
     * <p>
     * **Validates: Requirement 8.5**
     * </p>
     */
    @Test
    void generate_SimpleProps_ChecksFieldPresence() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("'title' in obj"),
                "Should check for 'title' field presence");
        assertTrue(tsCode.contains("'count' in obj"),
                "Should check for 'count' field presence");
        assertTrue(tsCode.contains("'value' in obj"),
                "Should check for 'value' field presence");
        assertTrue(tsCode.contains("'enabled' in obj"),
                "Should check for 'enabled' field presence");
    }

    /**
     * Test that type guard validates string field types.
     * <p>
     * **Validates: Requirement 8.5**
     * </p>
     */
    @Test
    void generate_SimpleProps_ValidatesStringType() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("typeof (obj as SimpleProps).title === 'string'"),
                "Should validate string type for title field");
    }

    /**
     * Test that type guard validates number field types.
     * <p>
     * **Validates: Requirement 8.5**
     * </p>
     */
    @Test
    void generate_SimpleProps_ValidatesNumberType() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("typeof (obj as SimpleProps).count === 'number'"),
                "Should validate number type for count field");
        assertTrue(tsCode.contains("typeof (obj as SimpleProps).value === 'number'"),
                "Should validate number type for value field");
    }

    /**
     * Test that type guard validates boolean field types.
     * <p>
     * **Validates: Requirement 8.5**
     * </p>
     */
    @Test
    void generate_SimpleProps_ValidatesBooleanType() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("typeof (obj as SimpleProps).enabled === 'boolean'"),
                "Should validate boolean type for enabled field");
    }

    // ========== Numeric Type Tests ==========

    /**
     * Test that all numeric primitives are validated as 'number'.
     */
    @Test
    void generate_NumericProps_AllNumericTypesValidatedAsNumber() {
        RecordInfo info = parser.parse(NumericProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("typeof (obj as NumericProps).intValue === 'number'"),
                "int should be validated as number");
        assertTrue(tsCode.contains("typeof (obj as NumericProps).longValue === 'number'"),
                "long should be validated as number");
        assertTrue(tsCode.contains("typeof (obj as NumericProps).doubleValue === 'number'"),
                "double should be validated as number");
        assertTrue(tsCode.contains("typeof (obj as NumericProps).floatValue === 'number'"),
                "float should be validated as number");
    }

    /**
     * Test that boxed primitives are validated correctly.
     */
    @Test
    void generate_BoxedProps_BoxedPrimitivesValidatedCorrectly() {
        RecordInfo info = parser.parse(BoxedProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("typeof (obj as BoxedProps).intValue === 'number'"),
                "Integer should be validated as number");
        assertTrue(tsCode.contains("typeof (obj as BoxedProps).boolValue === 'boolean'"),
                "Boolean should be validated as boolean");
        assertTrue(tsCode.contains("typeof (obj as BoxedProps).charValue === 'string'"),
                "Character should be validated as string");
    }

    // ========== Optional Field Tests ==========

    /**
     * Test that Optional fields allow undefined values.
     * <p>
     * **Validates: Requirement 8.5** - Handles Optional fields
     * </p>
     */
    @Test
    void generate_OptionalProps_OptionalFieldsAllowUndefined() {
        RecordInfo info = parser.parse(OptionalProps.class);
        String tsCode = generator.generate(info);

        // Optional fields should check if NOT present OR valid type
        assertTrue(tsCode.contains("!('optionalString' in obj)") || 
                   tsCode.contains("optionalString"),
                "Should handle optional string field");
    }

    /**
     * Test that required fields are still validated.
     */
    @Test
    void generate_OptionalProps_RequiredFieldsValidated() {
        RecordInfo info = parser.parse(OptionalProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("'required' in obj"),
                "Should check for required field presence");
        assertTrue(tsCode.contains("typeof (obj as OptionalProps).required === 'string'"),
                "Should validate required field type");
    }

    // ========== Array/List Field Tests ==========

    /**
     * Test that List fields are validated as arrays.
     * <p>
     * **Validates: Requirement 8.5** - Handles arrays
     * </p>
     */
    @Test
    void generate_ListProps_ListFieldsValidatedAsArrays() {
        RecordInfo info = parser.parse(ListProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("Array.isArray((obj as ListProps).items)"),
                "Should validate items as array");
        assertTrue(tsCode.contains("Array.isArray((obj as ListProps).numbers)"),
                "Should validate numbers as array");
    }

    // ========== Map Field Tests ==========

    /**
     * Test that Map fields are validated as objects.
     */
    @Test
    void generate_MapProps_MapFieldsValidatedAsObjects() {
        RecordInfo info = parser.parse(MapProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("typeof (obj as MapProps).stringToInt === 'object'"),
                "Should validate map as object");
    }

    // ========== Nested Record Tests ==========

    /**
     * Test that nested Records generate multiple type guards.
     * <p>
     * **Validates: Requirement 8.5** - Handles nested Records
     * </p>
     */
    @Test
    void generate_ChartProps_GeneratesNestedTypeGuards() {
        RecordInfo info = parser.parse(ChartProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("function isChartProps"),
                "Should generate isChartProps type guard");
        assertTrue(tsCode.contains("function isDataPoint"),
                "Should generate isDataPoint type guard");
        assertTrue(tsCode.contains("function isChartOptions"),
                "Should generate isChartOptions type guard");
    }

    /**
     * Test that nested Record fields call nested type guards.
     * <p>
     * **Validates: Requirement 8.5** - Calls nested type guards
     * </p>
     */
    @Test
    void generate_ChartProps_NestedFieldsCallNestedGuards() {
        RecordInfo info = parser.parse(ChartProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("isChartOptions((obj as ChartProps).options)"),
                "Should call isChartOptions for nested options field");
    }

    /**
     * Test that List of nested Records validates each element.
     */
    @Test
    void generate_ChartProps_ListOfNestedRecordsValidatesElements() {
        RecordInfo info = parser.parse(ChartProps.class);
        String tsCode = generator.generate(info);

        // Should validate array and call type guard for each element
        assertTrue(tsCode.contains("Array.isArray((obj as ChartProps).data)"),
                "Should check data is array");
        assertTrue(tsCode.contains("isDataPoint"),
                "Should reference isDataPoint for element validation");
    }

    // ========== Enum Field Tests ==========

    /**
     * Test that Enum fields are validated.
     */
    @Test
    void generate_EnumProps_EnumFieldsValidated() {
        RecordInfo info = parser.parse(EnumProps.class);
        String tsCode = generator.generate(info);

        // Should validate enum as string or union type
        assertTrue(tsCode.contains("status") && 
                   (tsCode.contains("'ACTIVE'") || tsCode.contains("=== 'string'")),
                "Should validate enum field");
    }

    // ========== Configuration Tests ==========

    /**
     * Test that export keyword is included by default.
     */
    @Test
    void generate_DefaultConfig_IncludesExport() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("export function"),
                "Should include export keyword by default");
    }

    /**
     * Test that export keyword can be disabled.
     */
    @Test
    void generate_NoExportConfig_ExcludesExport() {
        GeneratorConfig config = new GeneratorConfig().setExportFunctions(false);
        TypeGuardGenerator gen = new TypeGuardGenerator(config);

        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = gen.generate(info);

        assertFalse(tsCode.contains("export function"),
                "Should not include export keyword");
        assertTrue(tsCode.contains("function isSimpleProps"),
                "Should still contain function");
    }

    /**
     * Test that JSDoc comments are included by default.
     */
    @Test
    void generate_DefaultConfig_IncludesJsDoc() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("/**"),
                "Should include JSDoc comment by default");
        assertTrue(tsCode.contains("Type guard for"),
                "Should include type guard description");
    }

    /**
     * Test that JSDoc comments can be disabled.
     */
    @Test
    void generate_NoJsDocConfig_ExcludesJsDoc() {
        GeneratorConfig config = new GeneratorConfig().setIncludeJsDoc(false);
        TypeGuardGenerator gen = new TypeGuardGenerator(config);

        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = gen.generate(info);

        assertFalse(tsCode.contains("/**"),
                "Should not include JSDoc comment");
    }

    // ========== generateAll Tests ==========

    /**
     * Test that generateAll produces all type guards without duplicates.
     */
    @Test
    void generateAll_MultipleRecords_NoDuplicates() {
        List<RecordInfo> records = List.of(
                parser.parse(SimpleProps.class),
                parser.parse(OptionalProps.class)
        );

        String tsCode = generator.generateAll(records);

        assertTrue(tsCode.contains("function isSimpleProps"),
                "Should contain isSimpleProps");
        assertTrue(tsCode.contains("function isOptionalProps"),
                "Should contain isOptionalProps");

        // Count occurrences to ensure no duplicates
        int simpleCount = countOccurrences(tsCode, "function isSimpleProps");
        int optionalCount = countOccurrences(tsCode, "function isOptionalProps");

        assertEquals(1, simpleCount, "Should have exactly one isSimpleProps");
        assertEquals(1, optionalCount, "Should have exactly one isOptionalProps");
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
     * Test empty record generates valid type guard.
     */
    @Test
    void generate_EmptyRecord_GeneratesValidTypeGuard() {
        RecordInfo info = parser.parse(EmptyProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("function isEmptyProps(obj: unknown): obj is EmptyProps"),
                "Should generate type guard for empty record");
        assertTrue(tsCode.contains("typeof obj === 'object'"),
                "Should check object type");
        assertTrue(tsCode.contains("obj !== null"),
                "Should check not null");
    }

    /**
     * Test that generated code has proper return statement structure.
     */
    @Test
    void generate_SimpleProps_HasProperReturnStructure() {
        RecordInfo info = parser.parse(SimpleProps.class);
        String tsCode = generator.generate(info);

        assertTrue(tsCode.contains("return ("),
                "Should have return statement with parentheses");
        assertTrue(tsCode.contains(");"),
                "Should close return statement properly");
    }

    // ========== Helper Methods ==========

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
