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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that maps JavaScript/TypeScript type strings from the
 * Custom Elements Manifest to their corresponding Java and TypeScript types.
 * <p>
 * Used by the Code Generator when producing Java Records and wrapper
 * classes from {@code custom-elements.json}.
 * </p>
 * <p>
 * Mapping rules:
 * <ul>
 *   <li>{@code string} → {@code String} (Java) / {@code string} (TypeScript)</li>
 *   <li>{@code number} → {@code double} (Java) / {@code number} (TypeScript)</li>
 *   <li>{@code boolean} → {@code boolean} (Java) / {@code boolean} (TypeScript)</li>
 *   <li>{@code string | undefined} → {@code Optional<String>} (Java) / {@code string | undefined} (TypeScript)</li>
 *   <li>{@code string[]} → {@code List<String>} (Java) / {@code string[]} (TypeScript)</li>
 *   <li>{@code Date} → {@code String} (Java) / {@code string} (TypeScript) - ISO 8601 representation</li>
 *   <li>Union literal types (e.g. {@code 'primary' | 'success' | ...}) → {@code String} (Java) / preserved (TypeScript)</li>
 *   <li>Any unmappable type → {@code Object} (Java) / {@code unknown} (TypeScript) with a logged warning</li>
 * </ul>
 */
public final class TypeMapper {

    private static final Logger log = LoggerFactory.getLogger(TypeMapper.class);

    /**
     * Pattern matching union literal types such as {@code 'primary' | 'success' | 'neutral'}.
     * Each alternative is a single-quoted string, separated by {@code |}.
     */
    private static final Pattern UNION_LITERAL_PATTERN = Pattern.compile("^'[^']+'(\\s*\\|\\s*'[^']+')+$");

    /**
     * Pattern matching object types such as {@code { x: number, y: number }}.
     */
    private static final Pattern OBJECT_PATTERN = Pattern.compile("^\\{\\s*.+\\s*\\}$");

    /**
     * Pattern matching array types such as {@code string[]}, {@code number[]}.
     */
    private static final Pattern ARRAY_PATTERN = Pattern.compile("^(.+)\\[\\]$");

    /**
     * Pattern matching optional types such as {@code string | undefined}, {@code number | undefined}.
     */
    private static final Pattern OPTIONAL_PATTERN = Pattern.compile("^(.+)\\s*\\|\\s*undefined$");

    private static final Map<String, String> DIRECT_JAVA_MAPPINGS = Map.of(
        "string", "String",
        "number", "double",
        "boolean", "boolean",
        "Date", "String"
    );

    private static final Map<String, String> DIRECT_TS_MAPPINGS = Map.of(
        "string", "string",
        "number", "number",
        "boolean", "boolean",
        "Date", "string"
    );

    /**
     * Registry for exact custom type mappings.
     * Maps CEM type strings to custom Java and TypeScript types.
     * Thread-safe for concurrent access.
     */
    private static final Map<String, CustomTypeMapping> CUSTOM_MAPPINGS = new ConcurrentHashMap<>();

    /**
     * Registry for pattern-based custom type mappings.
     * Maps regex patterns to custom Java and TypeScript types.
     * Thread-safe for concurrent access.
     */
    private static final Map<Pattern, CustomTypeMapping> PATTERN_MAPPINGS = new ConcurrentHashMap<>();

    /**
     * Represents a custom type mapping with both Java and TypeScript types.
     */
    public record CustomTypeMapping(String javaType, String typescriptType) {}

    private TypeMapper() {
        // utility class
    }

    /**
     * Registers a custom type mapping for an exact CEM type.
     * <p>
     * Custom mappings override default mapping rules. This allows library-specific
     * or project-specific type mappings to be configured.
     * </p>
     * <p>
     * Example: Map "HTMLElement" to "IsPWidget" instead of "Object"
     * </p>
     *
     * @param cemType the exact CEM type string to match
     * @param javaType the Java type to map to
     * @param tsType the TypeScript type to map to
     */
    public static void registerCustomMapping(final String cemType, final String javaType, final String tsType) {
        if (cemType == null || cemType.isBlank()) {
            throw new IllegalArgumentException("CEM type cannot be null or blank");
        }
        if (javaType == null || javaType.isBlank()) {
            throw new IllegalArgumentException("Java type cannot be null or blank");
        }
        if (tsType == null || tsType.isBlank()) {
            throw new IllegalArgumentException("TypeScript type cannot be null or blank");
        }
        
        CUSTOM_MAPPINGS.put(cemType.trim(), new CustomTypeMapping(javaType, tsType));
        log.debug("Registered custom mapping: '{}' -> Java: '{}', TypeScript: '{}'", cemType, javaType, tsType);
    }

    /**
     * Registers a pattern-based custom type mapping.
     * <p>
     * Pattern mappings use regex to match multiple CEM types. They are checked
     * after exact custom mappings but before default mappings.
     * </p>
     * <p>
     * Example: Map all types matching "HTML.*Element" to "IsPWidget"
     * </p>
     *
     * @param pattern the regex pattern to match CEM types
     * @param javaType the Java type to map to
     * @param tsType the TypeScript type to map to
     */
    public static void registerPatternMapping(final String pattern, final String javaType, final String tsType) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("Pattern cannot be null or blank");
        }
        if (javaType == null || javaType.isBlank()) {
            throw new IllegalArgumentException("Java type cannot be null or blank");
        }
        if (tsType == null || tsType.isBlank()) {
            throw new IllegalArgumentException("TypeScript type cannot be null or blank");
        }
        
        final Pattern compiledPattern = Pattern.compile(pattern);
        PATTERN_MAPPINGS.put(compiledPattern, new CustomTypeMapping(javaType, tsType));
        log.debug("Registered pattern mapping: '{}' -> Java: '{}', TypeScript: '{}'", pattern, javaType, tsType);
    }

    /**
     * Loads custom type mappings from a configuration map.
     * <p>
     * This method is designed to integrate with the Configuration Manager (Task 5).
     * Each entry in the map should have a CEM type as the key and a CustomTypeMapping
     * as the value.
     * </p>
     * <p>
     * Pattern-based mappings can be identified by checking if the key contains regex
     * metacharacters (e.g., ".*", "^", "$", "[", "]").
     * </p>
     *
     * @param customMappings map of CEM types to custom type mappings
     */
    public static void loadCustomMappings(final Map<String, CustomTypeMapping> customMappings) {
        if (customMappings == null || customMappings.isEmpty()) {
            log.debug("No custom mappings to load");
            return;
        }
        
        for (final Map.Entry<String, CustomTypeMapping> entry : customMappings.entrySet()) {
            final String cemType = entry.getKey();
            final CustomTypeMapping mapping = entry.getValue();
            
            // Check if this looks like a regex pattern
            if (isRegexPattern(cemType)) {
                registerPatternMapping(cemType, mapping.javaType(), mapping.typescriptType());
            } else {
                registerCustomMapping(cemType, mapping.javaType(), mapping.typescriptType());
            }
        }
        
        log.info("Loaded {} custom type mappings", customMappings.size());
    }

    /**
     * Clears all custom type mappings.
     * <p>
     * This method is primarily intended for testing to ensure a clean state
     * between test cases.
     * </p>
     */
    public static void clearCustomMappings() {
        CUSTOM_MAPPINGS.clear();
        PATTERN_MAPPINGS.clear();
        log.debug("Cleared all custom type mappings");
    }

    /**
     * Checks if a string appears to be a regex pattern.
     * <p>
     * This is a heuristic check that looks for common regex metacharacters.
     * </p>
     *
     * @param str the string to check
     * @return true if the string contains regex metacharacters
     */
    private static boolean isRegexPattern(final String str) {
        return str.contains(".*") || str.contains(".+") || str.contains("^") || 
               str.contains("$") || str.contains("[") || str.contains("]") ||
               str.contains("(") || str.contains(")") || str.contains("|");
    }

    /**
     * Boxes primitive types for use in generics.
     * <p>
     * Java generics don't support primitive types, so we need to box them:
     * {@code double} → {@code Double}, {@code boolean} → {@code Boolean}, etc.
     * </p>
     *
     * @param javaType the Java type (possibly primitive)
     * @return the boxed type if primitive, otherwise the original type
     */
    private static String boxPrimitiveType(final String javaType) {
        return switch (javaType) {
            case "double" -> "Double";
            case "boolean" -> "Boolean";
            case "int" -> "Integer";
            case "long" -> "Long";
            case "float" -> "Float";
            case "byte" -> "Byte";
            case "short" -> "Short";
            case "char" -> "Character";
            default -> javaType;
        };
    }

    /**
     * Maps a JavaScript type string from the Custom Elements Manifest to
     * the corresponding Java type with metadata.
     * <p>
     * The mapping is deterministic: the same input always produces the same output.
     * Custom mappings are checked first, followed by default mapping rules.
     *
     * @param cemType the CEM type string (e.g. {@code "string"}, {@code "number"},
     *                {@code "'primary' | 'success'"}, {@code "string[]"})
     * @return the TypeMapping containing Java type and metadata
     */
    public static TypeMapping mapToJava(final String cemType) {
        if (cemType == null || cemType.isBlank()) {
            log.warn("Unmappable type: empty or null type string — falling back to Object");
            return TypeMapping.simple("Object");
        }

        final String trimmed = cemType.trim();

        // Check exact custom mappings first
        final CustomTypeMapping exactMapping = CUSTOM_MAPPINGS.get(trimmed);
        if (exactMapping != null) {
            log.debug("Using exact custom mapping for '{}': {}", trimmed, exactMapping.javaType());
            return TypeMapping.simple(exactMapping.javaType());
        }

        // Check pattern-based custom mappings
        for (final Map.Entry<Pattern, CustomTypeMapping> entry : PATTERN_MAPPINGS.entrySet()) {
            if (entry.getKey().matcher(trimmed).matches()) {
                final CustomTypeMapping patternMapping = entry.getValue();
                log.debug("Using pattern custom mapping for '{}': {}", trimmed, patternMapping.javaType());
                return TypeMapping.simple(patternMapping.javaType());
            }
        }

        // Continue with default mapping rules
        // Check for optional types (e.g., "string | undefined")
        final var optionalMatcher = OPTIONAL_PATTERN.matcher(trimmed);
        if (optionalMatcher.matches()) {
            final String baseType = optionalMatcher.group(1).trim();
            final TypeMapping baseMapping = mapToJava(baseType);
            return TypeMapping.withImport("Optional<" + baseMapping.javaType() + ">", "java.util");
        }

        // Check for array types (e.g., "string[]")
        final var arrayMatcher = ARRAY_PATTERN.matcher(trimmed);
        if (arrayMatcher.matches()) {
            final String elementType = arrayMatcher.group(1).trim();
            final TypeMapping elementMapping = mapToJava(elementType);
            // Box primitive types for use in generics (List<Double> not List<double>)
            final String boxedType = boxPrimitiveType(elementMapping.javaType());
            return TypeMapping.withImport("List<" + boxedType + ">", "java.util");
        }

        // Check direct mappings
        final String direct = DIRECT_JAVA_MAPPINGS.get(trimmed);
        if (direct != null) {
            return TypeMapping.simple(direct);
        }

        // Check for union literal types like 'primary' | 'success' | 'neutral'
        if (UNION_LITERAL_PATTERN.matcher(trimmed).matches()) {
            // For now, map to String until code generator supports enum generation
            // The enum definition is available via handleUnionLiterals() for future use
            return TypeMapping.simple("String");
        }

        // Check for object types like { x: number, y: number }
        if (OBJECT_PATTERN.matcher(trimmed).matches()) {
            // For now, map to Object until code generator supports record generation
            // The record definition is available via handleObjectType() for future use
            log.warn("Object type '{}' detected but record generation not yet integrated — falling back to Object", trimmed);
            return TypeMapping.simple("Object");
        }

        // Check for complex union types (not just literals)
        if (trimmed.contains("|")) {
            return handleComplexUnion(trimmed);
        }

        // Unmappable type — fallback to Object
        log.warn("Unmappable type: '{}' — falling back to Object", trimmed);
        return TypeMapping.simple("Object");
    }

    /**
     * Handles union literal types by generating an enum definition.
     * <p>
     * For example: {@code 'small' | 'medium' | 'large'} generates a Size enum.
     * <p>
     * This method is public for testing and future use by the code generator.
     * The main mapToJava method currently falls back to String until the code
     * generator is ready to integrate enum generation.
     *
     * @param cemType the union literal type string
     * @return TypeMapping with enum generation metadata
     */
    public static TypeMapping handleUnionLiterals(final String cemType) {
        // Extract literal values
        final String[] literals = cemType.split("\\s*\\|\\s*");
        final StringBuilder enumDef = new StringBuilder();
        
        // Generate enum name (would need context from property name in real implementation)
        // For now, we'll use a placeholder that the code generator will replace
        final String enumName = "GeneratedEnum";
        
        enumDef.append("public enum ").append(enumName).append(" {\n");
        
        for (int i = 0; i < literals.length; i++) {
            final String literal = literals[i].trim().replaceAll("^'|'$", "");
            final String enumConstant = literal.toUpperCase().replaceAll("[^A-Z0-9_]", "_");
            
            enumDef.append("    ").append(enumConstant).append("(\"").append(literal).append("\")");
            if (i < literals.length - 1) {
                enumDef.append(",\n");
            } else {
                enumDef.append(";\n");
            }
        }
        
        enumDef.append("\n");
        enumDef.append("    private final String value;\n");
        enumDef.append("    ").append(enumName).append("(String value) { this.value = value; }\n");
        enumDef.append("    public String getValue() { return value; }\n");
        enumDef.append("}");
        
        return TypeMapping.withRecordGeneration(enumName, enumDef.toString());
    }

    /**
     * Handles object types by generating a record definition.
     * <p>
     * For example: {@code { x: number, y: number }} generates a PositionData record.
     * <p>
     * This method is public for testing and future use by the code generator.
     * The main mapToJava method currently falls back to Object until the code
     * generator is ready to integrate record generation.
     *
     * @param cemType the object type string
     * @return TypeMapping with record generation metadata
     */
    public static TypeMapping handleObjectType(final String cemType) {
        // Parse object structure
        final String objectContent = cemType.substring(1, cemType.length() - 1).trim();
        
        // Split by comma (simple parsing, doesn't handle nested objects)
        final String[] fields = objectContent.split(",");
        
        final StringBuilder recordDef = new StringBuilder();
        final String recordName = "GeneratedRecord"; // Placeholder
        
        recordDef.append("public record ").append(recordName).append("(");
        
        for (int i = 0; i < fields.length; i++) {
            final String field = fields[i].trim();
            final String[] parts = field.split(":");
            
            if (parts.length == 2) {
                final String fieldName = parts[0].trim();
                final String fieldType = parts[1].trim();
                
                // Recursively map field type
                final TypeMapping fieldMapping = mapToJava(fieldType);
                
                recordDef.append(fieldMapping.javaType()).append(" ").append(fieldName);
                if (i < fields.length - 1) {
                    recordDef.append(", ");
                }
            }
        }
        
        recordDef.append(") {}");
        
        return TypeMapping.withRecordGeneration(recordName, recordDef.toString());
    }

    /**
     * Handles complex union types that cannot be cleanly mapped.
     * <p>
     * For example: {@code string | number | { custom: boolean }} falls back to Object with TODO.
     *
     * @param cemType the complex union type string
     * @return TypeMapping with fallback and TODO comment
     */
    private static TypeMapping handleComplexUnion(final String cemType) {
        log.warn("Complex union type '{}' cannot be cleanly mapped — falling back to Object with TODO", cemType);
        return TypeMapping.fallbackWithTodo(cemType, "Complex union");
    }

    /**
     * Maps a JavaScript type string from the Custom Elements Manifest to
     * the corresponding TypeScript type.
     * <p>
     * The mapping is deterministic: the same input always produces the same output.
     * Custom mappings are checked first, followed by default mapping rules.
     *
     * @param cemType the CEM type string (e.g. {@code "string"}, {@code "number"},
     *                {@code "'primary' | 'success'"}, {@code "string[]"})
     * @return the TypeScript type string
     */
    public static String mapToTypeScript(final String cemType) {
        if (cemType == null || cemType.isBlank()) {
            log.warn("Unmappable type: empty or null type string — falling back to unknown");
            return "unknown";
        }

        final String trimmed = cemType.trim();

        // Check exact custom mappings first
        final CustomTypeMapping exactMapping = CUSTOM_MAPPINGS.get(trimmed);
        if (exactMapping != null) {
            log.debug("Using exact custom mapping for '{}': {}", trimmed, exactMapping.typescriptType());
            return exactMapping.typescriptType();
        }

        // Check pattern-based custom mappings
        for (final Map.Entry<Pattern, CustomTypeMapping> entry : PATTERN_MAPPINGS.entrySet()) {
            if (entry.getKey().matcher(trimmed).matches()) {
                final CustomTypeMapping patternMapping = entry.getValue();
                log.debug("Using pattern custom mapping for '{}': {}", trimmed, patternMapping.typescriptType());
                return patternMapping.typescriptType();
            }
        }

        // Continue with default mapping rules
        // Check for optional types (e.g., "string | undefined") - preserve as-is
        if (OPTIONAL_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }

        // Check for array types (e.g., "string[]") - preserve as-is
        if (ARRAY_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }

        // Check direct mappings
        final String direct = DIRECT_TS_MAPPINGS.get(trimmed);
        if (direct != null) {
            return direct;
        }

        // Check for union literal types - preserve as-is
        if (UNION_LITERAL_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }

        // Check for object types - preserve as-is
        if (OBJECT_PATTERN.matcher(trimmed).matches()) {
            return trimmed;
        }

        // Check for complex union types - preserve as-is
        if (trimmed.contains("|")) {
            return trimmed;
        }

        // Unmappable type — fallback to unknown
        log.warn("Unmappable type: '{}' — falling back to unknown", trimmed);
        return "unknown";
    }

    /**
     * Maps a JavaScript type string from the Custom Elements Manifest to
     * the corresponding Java type string (legacy method for backward compatibility).
     * <p>
     * The mapping is deterministic: the same input always produces the same output.
     *
     * @param jsType the JavaScript type string (e.g. {@code "string"}, {@code "number"},
     *               {@code "'primary' | 'success'"})
     * @return the corresponding Java type string
     * @deprecated Use {@link #mapToJava(String)} instead for richer type information
     */
    @Deprecated
    public static String mapToJavaType(final String jsType) {
        return mapToJava(jsType).javaType();
    }
}
