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
 * Generates TypeScript interfaces from parsed Java Record definitions.
 * <p>
 * The TypeScriptGenerator takes {@link RecordInfo} objects produced by {@link RecordParser}
 * and generates corresponding TypeScript interface code. It handles:
 * <ul>
 *   <li>Primitive type mapping (int→number, String→string, boolean→boolean)</li>
 *   <li>Optional fields as optional TypeScript properties (?)</li>
 *   <li>Nested Records as nested interfaces</li>
 *   <li>List/Collection types as arrays (T[])</li>
 *   <li>Map types as Record&lt;K, V&gt;</li>
 *   <li>Enum types as string union types</li>
 * </ul>
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * RecordParser parser = new RecordParser();
 * RecordInfo info = parser.parse(ChartProps.class);
 * 
 * TypeScriptGenerator generator = new TypeScriptGenerator();
 * String tsCode = generator.generate(info);
 * 
 * // Output:
 * // export interface ChartProps {
 * //   title: string;
 * //   data: DataPoint[];
 * //   options: ChartOptions;
 * // }
 * }</pre>
 *
 * <h2>Type Mappings</h2>
 * <table>
 *   <tr><th>Java Type</th><th>TypeScript Type</th></tr>
 *   <tr><td>int, long, double, float, byte, short</td><td>number</td></tr>
 *   <tr><td>boolean</td><td>boolean</td></tr>
 *   <tr><td>String</td><td>string</td></tr>
 *   <tr><td>char, Character</td><td>string</td></tr>
 *   <tr><td>Optional&lt;T&gt;</td><td>T | undefined (with ? modifier)</td></tr>
 *   <tr><td>List&lt;T&gt;</td><td>T[]</td></tr>
 *   <tr><td>Map&lt;K, V&gt;</td><td>Record&lt;K, V&gt;</td></tr>
 *   <tr><td>Record (nested)</td><td>interface</td></tr>
 *   <tr><td>Enum</td><td>string (union type if values known)</td></tr>
 * </table>
 *
 * @see RecordParser
 * @see RecordInfo
 */
public class TypeScriptGenerator {

    private static final String INDENT = "  ";
    private static final String NEWLINE = "\n";

    /**
     * Configuration options for the generator.
     */
    private final GeneratorConfig config;

    /**
     * Creates a new TypeScriptGenerator with default configuration.
     */
    public TypeScriptGenerator() {
        this(new GeneratorConfig());
    }

    /**
     * Creates a new TypeScriptGenerator with the specified configuration.
     *
     * @param config the generator configuration
     */
    public TypeScriptGenerator(final GeneratorConfig config) {
        this.config = Objects.requireNonNull(config, "Config must not be null");
    }

    /**
     * Generates TypeScript interface code from a RecordInfo.
     * <p>
     * This method generates the main interface and all nested interfaces
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
        final Set<String> generatedInterfaces = new HashSet<>();
        final Map<String, RecordInfo> pendingInterfaces = new LinkedHashMap<>();

        // Collect all interfaces that need to be generated
        collectNestedRecords(recordInfo, pendingInterfaces, generatedInterfaces);

        // Generate all interfaces
        final List<String> interfaceNames = new ArrayList<>(pendingInterfaces.keySet());
        for (int i = 0; i < interfaceNames.size(); i++) {
            final String name = interfaceNames.get(i);
            final RecordInfo info = pendingInterfaces.get(name);

            if (i > 0) {
                output.append(NEWLINE);
            }

            generateInterface(info, output);
        }

        return output.toString();
    }

    /**
     * Generates TypeScript interfaces for multiple Records.
     * <p>
     * This method generates all interfaces in a single output, avoiding
     * duplicate interface definitions for shared nested types.
     * </p>
     *
     * @param recordInfos the list of parsed Record information
     * @return the generated TypeScript code as a String
     * @throws NullPointerException if recordInfos is null
     */
    public String generateAll(final List<RecordInfo> recordInfos) {
        Objects.requireNonNull(recordInfos, "RecordInfos must not be null");

        final StringBuilder output = new StringBuilder();
        final Set<String> generatedInterfaces = new HashSet<>();
        final Map<String, RecordInfo> pendingInterfaces = new LinkedHashMap<>();

        // Collect all interfaces from all records
        for (final RecordInfo recordInfo : recordInfos) {
            collectNestedRecords(recordInfo, pendingInterfaces, generatedInterfaces);
        }

        // Generate all interfaces
        final List<String> interfaceNames = new ArrayList<>(pendingInterfaces.keySet());
        for (int i = 0; i < interfaceNames.size(); i++) {
            final String name = interfaceNames.get(i);
            final RecordInfo info = pendingInterfaces.get(name);

            if (i > 0) {
                output.append(NEWLINE);
            }

            generateInterface(info, output);
        }

        return output.toString();
    }

    /**
     * Collects all nested Records that need interface generation.
     *
     * @param recordInfo the record to process
     * @param pendingInterfaces map to collect interfaces (preserves order)
     * @param generatedInterfaces set of already processed interface names
     */
    private void collectNestedRecords(
            final RecordInfo recordInfo,
            final Map<String, RecordInfo> pendingInterfaces,
            final Set<String> generatedInterfaces
    ) {
        final String name = recordInfo.name();

        // Skip if already processed or is a circular reference placeholder
        if (generatedInterfaces.contains(name) || recordInfo.isCircularReference()) {
            return;
        }

        generatedInterfaces.add(name);
        pendingInterfaces.put(name, recordInfo);

        // Process nested records in fields
        for (final FieldInfo field : recordInfo.fields()) {
            collectNestedRecordsFromType(field.typeInfo(), pendingInterfaces, generatedInterfaces);
        }
    }

    /**
     * Recursively collects nested Records from a type.
     *
     * @param typeInfo the type to process
     * @param pendingInterfaces map to collect interfaces
     * @param generatedInterfaces set of already processed interface names
     */
    private void collectNestedRecordsFromType(
            final TypeInfo typeInfo,
            final Map<String, RecordInfo> pendingInterfaces,
            final Set<String> generatedInterfaces
    ) {
        // Handle nested Record
        if (typeInfo.kind() == TypeKind.RECORD && typeInfo.nestedRecordInfo() != null) {
            collectNestedRecords(typeInfo.nestedRecordInfo(), pendingInterfaces, generatedInterfaces);
        }

        // Handle element type (for Optional, List, Array)
        if (typeInfo.elementType() != null) {
            collectNestedRecordsFromType(typeInfo.elementType(), pendingInterfaces, generatedInterfaces);
        }

        // Handle Map key and value types
        if (typeInfo.keyType() != null) {
            collectNestedRecordsFromType(typeInfo.keyType(), pendingInterfaces, generatedInterfaces);
        }
        if (typeInfo.valueType() != null) {
            collectNestedRecordsFromType(typeInfo.valueType(), pendingInterfaces, generatedInterfaces);
        }
    }

    /**
     * Generates a single TypeScript interface.
     *
     * @param recordInfo the record to generate interface for
     * @param output the StringBuilder to append to
     */
    private void generateInterface(final RecordInfo recordInfo, final StringBuilder output) {
        // Add JSDoc comment if configured
        if (config.includeJsDoc()) {
            output.append("/**").append(NEWLINE);
            output.append(" * Generated from ").append(recordInfo.fullName()).append(NEWLINE);
            output.append(" */").append(NEWLINE);
        }

        // Interface declaration
        if (config.exportInterfaces()) {
            output.append("export ");
        }
        output.append("interface ").append(recordInfo.name()).append(" {").append(NEWLINE);

        // Generate fields
        for (final FieldInfo field : recordInfo.fields()) {
            generateField(field, output);
        }

        output.append("}").append(NEWLINE);
    }

    /**
     * Generates a single field in the interface.
     *
     * @param field the field to generate
     * @param output the StringBuilder to append to
     */
    private void generateField(final FieldInfo field, final StringBuilder output) {
        output.append(INDENT);
        output.append(field.name());

        // Add optional modifier for Optional types
        if (field.isOptional()) {
            output.append("?");
        }

        output.append(": ");
        output.append(mapTypeToTypeScript(field.typeInfo()));
        output.append(";").append(NEWLINE);
    }

    /**
     * Maps a Java type to its TypeScript equivalent.
     *
     * @param typeInfo the type information
     * @return the TypeScript type string
     */
    private String mapTypeToTypeScript(final TypeInfo typeInfo) {
        return switch (typeInfo.kind()) {
            case PRIMITIVE -> mapPrimitiveToTypeScript(typeInfo.fullTypeName());
            case STRING -> "string";
            case RECORD -> typeInfo.typeName();
            case OPTIONAL -> mapOptionalToTypeScript(typeInfo);
            case LIST, ARRAY -> mapListToTypeScript(typeInfo);
            case MAP -> mapMapToTypeScript(typeInfo);
            case ENUM -> mapEnumToTypeScript(typeInfo);
            case OBJECT -> "unknown";
        };
    }

    /**
     * Maps a Java primitive type to TypeScript.
     *
     * @param fullTypeName the fully qualified Java type name
     * @return the TypeScript type
     */
    private String mapPrimitiveToTypeScript(final String fullTypeName) {
        return switch (fullTypeName) {
            case "int", "java.lang.Integer",
                 "long", "java.lang.Long",
                 "double", "java.lang.Double",
                 "float", "java.lang.Float",
                 "byte", "java.lang.Byte",
                 "short", "java.lang.Short" -> "number";
            case "boolean", "java.lang.Boolean" -> "boolean";
            case "char", "java.lang.Character" -> "string";
            default -> "unknown";
        };
    }

    /**
     * Maps an Optional type to TypeScript.
     * <p>
     * Optional&lt;T&gt; becomes T | undefined in TypeScript.
     * The field itself gets the ? modifier.
     * </p>
     *
     * @param typeInfo the Optional type info
     * @return the TypeScript type
     */
    private String mapOptionalToTypeScript(final TypeInfo typeInfo) {
        if (typeInfo.elementType() != null) {
            return mapTypeToTypeScript(typeInfo.elementType());
        }
        return "unknown";
    }

    /**
     * Maps a List/Array type to TypeScript.
     * <p>
     * List&lt;T&gt; becomes T[] in TypeScript.
     * </p>
     *
     * @param typeInfo the List/Array type info
     * @return the TypeScript type
     */
    private String mapListToTypeScript(final TypeInfo typeInfo) {
        if (typeInfo.elementType() != null) {
            final String elementType = mapTypeToTypeScript(typeInfo.elementType());
            // Wrap complex types in parentheses for array notation
            if (elementType.contains("|") || elementType.contains("&")) {
                return "(" + elementType + ")[]";
            }
            return elementType + "[]";
        }
        return "unknown[]";
    }

    /**
     * Maps a Map type to TypeScript.
     * <p>
     * Map&lt;K, V&gt; becomes Record&lt;K, V&gt; in TypeScript.
     * For non-string keys, uses { [key: K]: V } syntax.
     * </p>
     *
     * @param typeInfo the Map type info
     * @return the TypeScript type
     */
    private String mapMapToTypeScript(final TypeInfo typeInfo) {
        final String keyType = typeInfo.keyType() != null
                ? mapTypeToTypeScript(typeInfo.keyType())
                : "string";
        final String valueType = typeInfo.valueType() != null
                ? mapTypeToTypeScript(typeInfo.valueType())
                : "unknown";

        // Use Record<K, V> for string/number keys
        if ("string".equals(keyType) || "number".equals(keyType)) {
            return "Record<" + keyType + ", " + valueType + ">";
        }

        // Use index signature for other key types
        return "{ [key: " + keyType + "]: " + valueType + " }";
    }

    /**
     * Maps an Enum type to TypeScript.
     * <p>
     * Enums are mapped to string type. If enum values are known,
     * they could be mapped to a union type.
     * </p>
     *
     * @param typeInfo the Enum type info
     * @return the TypeScript type
     */
    private String mapEnumToTypeScript(final TypeInfo typeInfo) {
        // Try to get enum constants for union type
        try {
            final Class<?> enumClass = Class.forName(typeInfo.fullTypeName());
            if (enumClass.isEnum()) {
                final Object[] constants = enumClass.getEnumConstants();
                if (constants != null && constants.length > 0) {
                    final StringBuilder union = new StringBuilder();
                    for (int i = 0; i < constants.length; i++) {
                        if (i > 0) {
                            union.append(" | ");
                        }
                        union.append("'").append(constants[i].toString()).append("'");
                    }
                    return union.toString();
                }
            }
        } catch (final ClassNotFoundException e) {
            // Fall back to string type
        }
        return "string";
    }

    // ========== Configuration ==========

    /**
     * Configuration options for TypeScript generation.
     */
    public static class GeneratorConfig {

        private boolean exportInterfaces = true;
        private boolean includeJsDoc = true;

        /**
         * Creates a GeneratorConfig with default settings.
         */
        public GeneratorConfig() {
        }

        /**
         * Sets whether to export interfaces (add 'export' keyword).
         *
         * @param exportInterfaces true to export interfaces
         * @return this config for chaining
         */
        public GeneratorConfig setExportInterfaces(final boolean exportInterfaces) {
            this.exportInterfaces = exportInterfaces;
            return this;
        }

        /**
         * Gets whether interfaces should be exported.
         *
         * @return true if interfaces should be exported
         */
        public boolean exportInterfaces() {
            return exportInterfaces;
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
