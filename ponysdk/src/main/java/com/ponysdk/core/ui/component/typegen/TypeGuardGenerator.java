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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Generates TypeScript type guard functions from parsed Java Record definitions.
 * <p>
 * Type guards are runtime validation functions that check if an unknown object
 * conforms to a specific TypeScript interface. They use TypeScript's type predicate
 * syntax to provide type narrowing.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * RecordParser parser = new RecordParser();
 * RecordInfo info = parser.parse(ChartProps.class);
 * 
 * TypeGuardGenerator generator = new TypeGuardGenerator();
 * String tsCode = generator.generate(info);
 * 
 * // Output:
 * // export function isChartProps(obj: unknown): obj is ChartProps {
 * //   return (
 * //     typeof obj === 'object' &&
 * //     obj !== null &&
 * //     'title' in obj &&
 * //     typeof (obj as ChartProps).title === 'string' &&
 * //     'data' in obj &&
 * //     Array.isArray((obj as ChartProps).data) &&
 * //     'options' in obj &&
 * //     isChartOptions((obj as ChartProps).options)
 * //   );
 * // }
 * }</pre>
 *
 * @see RecordParser
 * @see RecordInfo
 */
public class TypeGuardGenerator {

    private static final String INDENT = "  ";
    private static final String NEWLINE = "\n";

    /**
     * Configuration options for the generator.
     */
    private final GeneratorConfig config;

    /**
     * Creates a new TypeGuardGenerator with default configuration.
     */
    public TypeGuardGenerator() {
        this(new GeneratorConfig());
    }

    /**
     * Creates a new TypeGuardGenerator with the specified configuration.
     *
     * @param config the generator configuration
     */
    public TypeGuardGenerator(final GeneratorConfig config) {
        this.config = Objects.requireNonNull(config, "Config must not be null");
    }

    /**
     * Generates TypeScript type guard functions from a RecordInfo.
     * <p>
     * This method generates the main type guard and all nested type guards
     * required by the Record structure.
     * </p>
     *
     * @param recordInfo the parsed Record information
     * @return the generated TypeScript code as a String
     * @throws NullPointerException if recordInfo is null
     */
    public String generate(final RecordInfo recordInfo) {
        Objects.requireNonNull(recordInfo, "RecordInfo must not be null");

        final StringBuilder output = new StringBuilder();
        final Set<String> generatedGuards = new HashSet<>();
        final Map<String, RecordInfo> pendingGuards = new LinkedHashMap<>();

        // Collect all records that need type guards
        collectNestedRecords(recordInfo, pendingGuards, generatedGuards);

        // Generate all type guards
        final List<String> guardNames = new ArrayList<>(pendingGuards.keySet());
        for (int i = 0; i < guardNames.size(); i++) {
            final String name = guardNames.get(i);
            final RecordInfo info = pendingGuards.get(name);

            if (i > 0) {
                output.append(NEWLINE);
            }

            generateTypeGuard(info, output);
        }

        return output.toString();
    }

    /**
     * Generates TypeScript type guards for multiple Records.
     * <p>
     * This method generates all type guards in a single output, avoiding
     * duplicate guard definitions for shared nested types.
     * </p>
     *
     * @param recordInfos the list of parsed Record information
     * @return the generated TypeScript code as a String
     * @throws NullPointerException if recordInfos is null
     */
    public String generateAll(final List<RecordInfo> recordInfos) {
        Objects.requireNonNull(recordInfos, "RecordInfos must not be null");

        final StringBuilder output = new StringBuilder();
        final Set<String> generatedGuards = new HashSet<>();
        final Map<String, RecordInfo> pendingGuards = new LinkedHashMap<>();

        // Collect all records from all inputs
        for (final RecordInfo recordInfo : recordInfos) {
            collectNestedRecords(recordInfo, pendingGuards, generatedGuards);
        }

        // Generate all type guards
        final List<String> guardNames = new ArrayList<>(pendingGuards.keySet());
        for (int i = 0; i < guardNames.size(); i++) {
            final String name = guardNames.get(i);
            final RecordInfo info = pendingGuards.get(name);

            if (i > 0) {
                output.append(NEWLINE);
            }

            generateTypeGuard(info, output);
        }

        return output.toString();
    }

    /**
     * Collects all nested Records that need type guard generation.
     *
     * @param recordInfo the record to process
     * @param pendingGuards map to collect guards (preserves order)
     * @param generatedGuards set of already processed guard names
     */
    private void collectNestedRecords(
            final RecordInfo recordInfo,
            final Map<String, RecordInfo> pendingGuards,
            final Set<String> generatedGuards
    ) {
        final String name = recordInfo.name();

        // Skip if already processed or is a circular reference placeholder
        if (generatedGuards.contains(name) || recordInfo.isCircularReference()) {
            return;
        }

        generatedGuards.add(name);
        pendingGuards.put(name, recordInfo);

        // Process nested records in fields
        for (final FieldInfo field : recordInfo.fields()) {
            collectNestedRecordsFromType(field.typeInfo(), pendingGuards, generatedGuards);
        }
    }

    /**
     * Recursively collects nested Records from a type.
     *
     * @param typeInfo the type to process
     * @param pendingGuards map to collect guards
     * @param generatedGuards set of already processed guard names
     */
    private void collectNestedRecordsFromType(
            final TypeInfo typeInfo,
            final Map<String, RecordInfo> pendingGuards,
            final Set<String> generatedGuards
    ) {
        // Handle nested Record
        if (typeInfo.kind() == TypeKind.RECORD && typeInfo.nestedRecordInfo() != null) {
            collectNestedRecords(typeInfo.nestedRecordInfo(), pendingGuards, generatedGuards);
        }

        // Handle element type (for Optional, List, Array)
        if (typeInfo.elementType() != null) {
            collectNestedRecordsFromType(typeInfo.elementType(), pendingGuards, generatedGuards);
        }

        // Handle Map key and value types
        if (typeInfo.keyType() != null) {
            collectNestedRecordsFromType(typeInfo.keyType(), pendingGuards, generatedGuards);
        }
        if (typeInfo.valueType() != null) {
            collectNestedRecordsFromType(typeInfo.valueType(), pendingGuards, generatedGuards);
        }
    }

    /**
     * Generates a single TypeScript type guard function.
     *
     * @param recordInfo the record to generate type guard for
     * @param output the StringBuilder to append to
     */
    private void generateTypeGuard(final RecordInfo recordInfo, final StringBuilder output) {
        final String typeName = recordInfo.name();
        final String guardName = "is" + typeName;

        // Add JSDoc comment if configured
        if (config.includeJsDoc()) {
            output.append("/**").append(NEWLINE);
            output.append(" * Type guard for ").append(typeName).append(NEWLINE);
            output.append(" * Generated from ").append(recordInfo.fullName()).append(NEWLINE);
            output.append(" */").append(NEWLINE);
        }

        // Function declaration
        if (config.exportFunctions()) {
            output.append("export ");
        }
        output.append("function ").append(guardName);
        output.append("(obj: unknown): obj is ").append(typeName).append(" {").append(NEWLINE);

        // Function body
        output.append(INDENT).append("return (").append(NEWLINE);

        // Base object check
        output.append(INDENT).append(INDENT).append("typeof obj === 'object' &&").append(NEWLINE);
        output.append(INDENT).append(INDENT).append("obj !== null");

        // Generate field checks
        for (final FieldInfo field : recordInfo.fields()) {
            output.append(" &&").append(NEWLINE);
            generateFieldCheck(field, typeName, output);
        }

        output.append(NEWLINE);
        output.append(INDENT).append(");").append(NEWLINE);
        output.append("}").append(NEWLINE);
    }

    /**
     * Generates the type check for a single field.
     *
     * @param field the field to generate check for
     * @param typeName the parent type name for casting
     * @param output the StringBuilder to append to
     */
    private void generateFieldCheck(final FieldInfo field, final String typeName, final StringBuilder output) {
        final String fieldName = field.name();
        final TypeInfo typeInfo = field.typeInfo();
        final String accessor = "(obj as " + typeName + ")." + fieldName;

        if (field.isOptional()) {
            // Optional field: check if present, then validate type
            output.append(INDENT).append(INDENT);
            output.append("(!('").append(fieldName).append("' in obj) || ");
            generateTypeCheck(typeInfo.elementType(), accessor, output);
            output.append(")");
        } else {
            // Required field: check presence and type
            output.append(INDENT).append(INDENT);
            output.append("'").append(fieldName).append("' in obj &&").append(NEWLINE);
            output.append(INDENT).append(INDENT);
            generateTypeCheck(typeInfo, accessor, output);
        }
    }

    /**
     * Generates the type check expression for a given type.
     *
     * @param typeInfo the type to check
     * @param accessor the accessor expression for the value
     * @param output the StringBuilder to append to
     */
    private void generateTypeCheck(final TypeInfo typeInfo, final String accessor, final StringBuilder output) {
        switch (typeInfo.kind()) {
            case PRIMITIVE -> generatePrimitiveCheck(typeInfo, accessor, output);
            case STRING -> output.append("typeof ").append(accessor).append(" === 'string'");
            case RECORD -> output.append("is").append(typeInfo.typeName()).append("(").append(accessor).append(")");
            case OPTIONAL -> generateOptionalCheck(typeInfo, accessor, output);
            case LIST, ARRAY -> generateArrayCheck(typeInfo, accessor, output);
            case MAP -> generateMapCheck(accessor, output);
            case ENUM -> generateEnumCheck(typeInfo, accessor, output);
            case OBJECT -> output.append("typeof ").append(accessor).append(" === 'object'");
        }
    }

    /**
     * Generates type check for primitive types.
     *
     * @param typeInfo the primitive type info
     * @param accessor the accessor expression
     * @param output the StringBuilder to append to
     */
    private void generatePrimitiveCheck(final TypeInfo typeInfo, final String accessor, final StringBuilder output) {
        final String tsType = mapPrimitiveToTypeScriptType(typeInfo.fullTypeName());
        output.append("typeof ").append(accessor).append(" === '").append(tsType).append("'");
    }

    /**
     * Maps a Java primitive type to TypeScript typeof result.
     *
     * @param fullTypeName the fully qualified Java type name
     * @return the TypeScript typeof result string
     */
    private String mapPrimitiveToTypeScriptType(final String fullTypeName) {
        return switch (fullTypeName) {
            case "int", "java.lang.Integer",
                 "long", "java.lang.Long",
                 "double", "java.lang.Double",
                 "float", "java.lang.Float",
                 "byte", "java.lang.Byte",
                 "short", "java.lang.Short" -> "number";
            case "boolean", "java.lang.Boolean" -> "boolean";
            case "char", "java.lang.Character" -> "string";
            default -> "object";
        };
    }

    /**
     * Generates type check for Optional types.
     *
     * @param typeInfo the Optional type info
     * @param accessor the accessor expression
     * @param output the StringBuilder to append to
     */
    private void generateOptionalCheck(final TypeInfo typeInfo, final String accessor, final StringBuilder output) {
        if (typeInfo.elementType() != null) {
            output.append("(").append(accessor).append(" === undefined || ");
            generateTypeCheck(typeInfo.elementType(), accessor, output);
            output.append(")");
        } else {
            output.append("true");
        }
    }

    /**
     * Generates type check for array/list types.
     *
     * @param typeInfo the array/list type info
     * @param accessor the accessor expression
     * @param output the StringBuilder to append to
     */
    private void generateArrayCheck(final TypeInfo typeInfo, final String accessor, final StringBuilder output) {
        output.append("Array.isArray(").append(accessor).append(")");

        // Add element type check if element type is a Record
        if (typeInfo.elementType() != null && typeInfo.elementType().kind() == TypeKind.RECORD) {
            output.append(" && ");
            output.append(accessor).append(".every(item => is");
            output.append(typeInfo.elementType().typeName()).append("(item))");
        }
    }

    /**
     * Generates type check for Map types.
     *
     * @param accessor the accessor expression
     * @param output the StringBuilder to append to
     */
    private void generateMapCheck(final String accessor, final StringBuilder output) {
        output.append("typeof ").append(accessor).append(" === 'object' && ");
        output.append(accessor).append(" !== null");
    }

    /**
     * Generates type check for Enum types.
     *
     * @param typeInfo the enum type info
     * @param accessor the accessor expression
     * @param output the StringBuilder to append to
     */
    private void generateEnumCheck(final TypeInfo typeInfo, final String accessor, final StringBuilder output) {
        // Try to get enum constants for union type check
        try {
            final Class<?> enumClass = Class.forName(typeInfo.fullTypeName());
            if (enumClass.isEnum()) {
                final Object[] constants = enumClass.getEnumConstants();
                if (constants != null && constants.length > 0) {
                    output.append("(");
                    for (int i = 0; i < constants.length; i++) {
                        if (i > 0) {
                            output.append(" || ");
                        }
                        output.append(accessor).append(" === '").append(constants[i].toString()).append("'");
                    }
                    output.append(")");
                    return;
                }
            }
        } catch (final ClassNotFoundException e) {
            // Fall back to string type check
        }
        output.append("typeof ").append(accessor).append(" === 'string'");
    }

    // ========== Configuration ==========

    /**
     * Configuration options for type guard generation.
     */
    public static class GeneratorConfig {

        private boolean exportFunctions = true;
        private boolean includeJsDoc = true;

        /**
         * Creates a GeneratorConfig with default settings.
         */
        public GeneratorConfig() {
        }

        /**
         * Sets whether to export functions (add 'export' keyword).
         *
         * @param exportFunctions true to export functions
         * @return this config for chaining
         */
        public GeneratorConfig setExportFunctions(final boolean exportFunctions) {
            this.exportFunctions = exportFunctions;
            return this;
        }

        /**
         * Gets whether functions should be exported.
         *
         * @return true if functions should be exported
         */
        public boolean exportFunctions() {
            return exportFunctions;
        }

        /**
         * Sets whether to include JSDoc comments.
         *
         * @param includeJsDoc true to include JSDoc
         * @return this config for chaining
         */
        public GeneratorConfig setIncludeJsDoc(final boolean includeJsDoc) {
            this.includeJsDoc = includeJsDoc;
            return this;
        }

        /**
         * Gets whether JSDoc comments should be included.
         *
         * @return true if JSDoc should be included
         */
        public boolean includeJsDoc() {
            return includeJsDoc;
        }
    }
}
