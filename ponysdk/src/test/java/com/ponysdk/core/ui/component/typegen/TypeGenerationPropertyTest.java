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

import com.ponysdk.core.ui.component.typegen.RecordParser.FieldInfo;
import com.ponysdk.core.ui.component.typegen.RecordParser.RecordInfo;
import com.ponysdk.core.ui.component.typegen.RecordParser.TypeInfo;
import com.ponysdk.core.ui.component.typegen.RecordParser.TypeKind;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Type Generation Correctness.
 * <p>
 * Feature: pcomponent, Property 8: Type Generation Correctness
 * </p>
 * <p>
 * **Validates: Requirements 8.1, 8.2, 8.3, 8.4**
 * </p>
 * <p>
 * For any valid Java Record definition, THE Type_Generator SHALL produce a TypeScript interface where:
 * <ul>
 *   <li>All Record fields map to interface properties</li>
 *   <li>Nested Records produce nested interfaces</li>
 *   <li>Java primitives map to TypeScript equivalents (int→number, String→string, boolean→boolean)</li>
 *   <li>Optional fields become optional properties (?)</li>
 * </ul>
 * </p>
 */
public class TypeGenerationPropertyTest {

    // ========== Test Record Types ==========

    /**
     * Simple test record with primitive types.
     */
    public record SimpleProps(
            String name,
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
            float floatValue
    ) {}

    /**
     * Record with Optional field.
     */
    public record OptionalProps(
            String required,
            Optional<String> optional
    ) {}

    /**
     * Nested record for testing hierarchical types.
     */
    public record NestedProps(
            String title,
            SimpleProps inner
    ) {}

    /**
     * Record with List field.
     */
    public record ListProps(
            String name,
            List<String> items
    ) {}

    // ========== Helper Methods ==========

    /**
     * Creates a TypeInfo for a primitive type.
     */
    private TypeInfo createPrimitiveTypeInfo(String typeName, String fullTypeName) {
        return new TypeInfo(
                TypeKind.PRIMITIVE,
                typeName,
                fullTypeName,
                null, null, null, false
        );
    }

    /**
     * Creates a TypeInfo for a String type.
     */
    private TypeInfo createStringTypeInfo() {
        return new TypeInfo(
                TypeKind.STRING,
                "String",
                "java.lang.String",
                null, null, null, false
        );
    }

    /**
     * Creates a TypeInfo for an Optional type.
     */
    private TypeInfo createOptionalTypeInfo(TypeInfo elementType) {
        return new TypeInfo(
                TypeKind.OPTIONAL,
                "Optional",
                "java.util.Optional",
                elementType, null, null, true
        );
    }

    /**
     * Creates a TypeInfo for a List type.
     */
    private TypeInfo createListTypeInfo(TypeInfo elementType) {
        return new TypeInfo(
                TypeKind.LIST,
                "List",
                "java.util.List",
                elementType, null, null, false
        );
    }

    /**
     * Creates a TypeInfo for a nested Record type.
     */
    private TypeInfo createRecordTypeInfo(String typeName, RecordInfo nestedInfo) {
        return new TypeInfo(
                TypeKind.RECORD,
                typeName,
                "com.test." + typeName,
                null, null, null, false, nestedInfo
        );
    }

    /**
     * Creates a FieldInfo with no annotations.
     */
    private FieldInfo createFieldInfo(String name, TypeInfo typeInfo) {
        return new FieldInfo(name, typeInfo, List.of());
    }

    /**
     * Creates a RecordInfo with no annotations.
     */
    private RecordInfo createRecordInfo(String name, List<FieldInfo> fields) {
        return new RecordInfo(name, "com.test." + name, fields, List.of(), false);
    }

    // ========== Property 8: Type Generation Correctness Tests ==========

    /**
     * Property 8: All Record fields map to interface properties
     * <p>
     * **Validates: Requirements 8.1**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 8: All Record fields map to interface properties")
    void allFieldsMapToProperties(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String fieldName1,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String fieldName2,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String fieldName3
    ) {
        // Create a RecordInfo with multiple fields
        RecordInfo recordInfo = createRecordInfo("TestRecord", List.of(
                createFieldInfo(fieldName1, createStringTypeInfo()),
                createFieldInfo(fieldName2, createPrimitiveTypeInfo("int", "int")),
                createFieldInfo(fieldName3, createPrimitiveTypeInfo("boolean", "boolean"))
        ));

        TypeScriptGenerator generator = new TypeScriptGenerator();
        String tsInterface = generator.generate(recordInfo);

        // Verify all fields are present in the generated interface
        assertTrue(tsInterface.contains(fieldName1 + ":"),
                "Generated interface should contain field: " + fieldName1);
        assertTrue(tsInterface.contains(fieldName2 + ":"),
                "Generated interface should contain field: " + fieldName2);
        assertTrue(tsInterface.contains(fieldName3 + ":"),
                "Generated interface should contain field: " + fieldName3);
    }

    /**
     * Property 8: Java primitives map to TypeScript equivalents
     * <p>
     * **Validates: Requirements 8.3**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 8: Java primitives map to TypeScript equivalents")
    void primitivesMapCorrectly(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String fieldName
    ) {
        TypeScriptGenerator generator = new TypeScriptGenerator();

        // Test int -> number
        RecordInfo intRecord = createRecordInfo("TestRecord", List.of(
                createFieldInfo(fieldName, createPrimitiveTypeInfo("int", "int"))
        ));
        String intInterface = generator.generate(intRecord);
        assertTrue(intInterface.contains(fieldName + ": number"),
                "int should map to number");

        // Test String -> string
        RecordInfo stringRecord = createRecordInfo("TestRecord", List.of(
                createFieldInfo(fieldName, createStringTypeInfo())
        ));
        String stringInterface = generator.generate(stringRecord);
        assertTrue(stringInterface.contains(fieldName + ": string"),
                "String should map to string");

        // Test boolean -> boolean
        RecordInfo boolRecord = createRecordInfo("TestRecord", List.of(
                createFieldInfo(fieldName, createPrimitiveTypeInfo("boolean", "boolean"))
        ));
        String boolInterface = generator.generate(boolRecord);
        assertTrue(boolInterface.contains(fieldName + ": boolean"),
                "boolean should map to boolean");

        // Test double -> number
        RecordInfo doubleRecord = createRecordInfo("TestRecord", List.of(
                createFieldInfo(fieldName, createPrimitiveTypeInfo("double", "double"))
        ));
        String doubleInterface = generator.generate(doubleRecord);
        assertTrue(doubleInterface.contains(fieldName + ": number"),
                "double should map to number");
    }

    /**
     * Property 8: Optional fields become optional properties
     * <p>
     * **Validates: Requirements 8.4**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 8: Optional fields become optional properties")
    void optionalFieldsBecomeOptionalProperties(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String fieldName
    ) {
        TypeInfo optionalType = createOptionalTypeInfo(createStringTypeInfo());

        RecordInfo recordInfo = createRecordInfo("TestRecord", List.of(
                createFieldInfo(fieldName, optionalType)
        ));

        TypeScriptGenerator generator = new TypeScriptGenerator();
        String tsInterface = generator.generate(recordInfo);

        // Verify the field is marked as optional with ?
        assertTrue(tsInterface.contains(fieldName + "?:"),
                "Optional field should be marked with ? in TypeScript: " + tsInterface);
    }

    /**
     * Property 8: Nested Records produce nested interfaces
     * <p>
     * **Validates: Requirements 8.2**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 8: Nested Records produce nested interfaces")
    void nestedRecordsProduceNestedInterfaces(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String outerField,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String innerField
    ) {
        // Create inner record info
        RecordInfo innerRecord = createRecordInfo("InnerRecord", List.of(
                createFieldInfo(innerField, createStringTypeInfo())
        ));

        // Create outer record with nested type
        TypeInfo nestedType = createRecordTypeInfo("InnerRecord", innerRecord);
        RecordInfo outerRecord = createRecordInfo("OuterRecord", List.of(
                createFieldInfo(outerField, nestedType)
        ));

        TypeScriptGenerator generator = new TypeScriptGenerator();
        String tsInterface = generator.generate(outerRecord);

        // Verify the outer field references the inner type
        assertTrue(tsInterface.contains(outerField + ": InnerRecord"),
                "Nested record field should reference the inner type name: " + tsInterface);
    }

    /**
     * Property 8: List fields map to array types
     * <p>
     * **Validates: Requirements 8.2**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 8: List fields map to array types")
    void listFieldsMapToArrayTypes(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String fieldName
    ) {
        TypeInfo listType = createListTypeInfo(createStringTypeInfo());

        RecordInfo recordInfo = createRecordInfo("TestRecord", List.of(
                createFieldInfo(fieldName, listType)
        ));

        TypeScriptGenerator generator = new TypeScriptGenerator();
        String tsInterface = generator.generate(recordInfo);

        // Verify the field is an array type
        assertTrue(tsInterface.contains(fieldName + ": string[]"),
                "List<String> should map to string[]: " + tsInterface);
    }

    /**
     * Property 8: Type guard generation produces valid function
     * <p>
     * **Validates: Requirements 8.5**
     * </p>
     */
    @Property(tries = 100)
    @Label("Property 8: Type guard generation produces valid function")
    void typeGuardGenerationProducesValidFunction(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String recordName,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String fieldName
    ) {
        RecordInfo recordInfo = createRecordInfo(recordName, List.of(
                createFieldInfo(fieldName, createStringTypeInfo())
        ));

        TypeGuardGenerator guardGenerator = new TypeGuardGenerator();
        String typeGuard = guardGenerator.generate(recordInfo);

        // Verify the type guard function structure
        assertTrue(typeGuard.contains("function is" + recordName),
                "Type guard should have function name is" + recordName);
        assertTrue(typeGuard.contains("obj is " + recordName),
                "Type guard should return type predicate");
        assertTrue(typeGuard.contains("'" + fieldName + "'"),
                "Type guard should check for field: " + fieldName);
    }
}
