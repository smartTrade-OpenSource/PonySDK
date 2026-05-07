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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
     * Requires at least 2 literal values.
     */
    private static final Pattern UNION_LITERAL_PATTERN = Pattern.compile("^'[^']+'(\\s*\\|\\s*'[^']+')+$");

    /**
     * Pattern matching optional union literal types such as {@code 'small' | 'medium' | 'large' | undefined}.
     * Each alternative is a single-quoted string, separated by {@code |}, with {@code undefined} at the end.
     * Requires at least 1 literal value before the {@code undefined}.
     */
    private static final Pattern OPTIONAL_UNION_LITERAL_PATTERN = Pattern.compile("^'[^']+'(\\s*\\|\\s*'[^']+')*\\s*\\|\\s*undefined$");

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
     * Cache of generated enums to avoid duplicate generation.
     * <p>
     * Key: signature of the enum (sorted literal values joined by "|", e.g., "brand|danger|neutral|success|warning")
     * Value: the enum name that was generated for those values
     * </p>
     * <p>
     * This ensures that if two properties have the same set of literal values (regardless of order),
     * they share the same enum.
     * </p>
     */
    private static final Map<String, String> ENUM_CACHE = new ConcurrentHashMap<>();

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
     * Detects if a CEM type is a pure union literal type.
     * <p>
     * A pure union literal type consists of two or more single-quoted string literals
     * separated by {@code |}, such as {@code 'small' | 'medium' | 'large'}.
     * </p>
     * <p>
     * This method ensures mutual exclusivity with {@link #isOptionalUnionLiteralType(String)}:
     * a type cannot be both a pure union literal and an optional union literal.
     * </p>
     *
     * @param cemType the CEM type string to check
     * @return {@code true} if the type is a pure union literal type, {@code false} otherwise
     *         (including for null or empty input)
     */
    public static boolean isUnionLiteralType(final String cemType) {
        if (cemType == null || cemType.isBlank()) {
            return false;
        }
        final String trimmed = cemType.trim();
        // Ensure mutual exclusivity: pure union literals must NOT match optional pattern
        return UNION_LITERAL_PATTERN.matcher(trimmed).matches() 
            && !OPTIONAL_UNION_LITERAL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Detects if a CEM type is an optional union literal type.
     * <p>
     * An optional union literal type consists of one or more single-quoted string literals
     * separated by {@code |}, followed by {@code | undefined}, such as
     * {@code 'small' | 'medium' | 'large' | undefined}.
     * </p>
     * <p>
     * This method ensures mutual exclusivity with {@link #isUnionLiteralType(String)}:
     * a type cannot be both a pure union literal and an optional union literal.
     * </p>
     *
     * @param cemType the CEM type string to check
     * @return {@code true} if the type is an optional union literal type, {@code false} otherwise
     *         (including for null or empty input)
     */
    public static boolean isOptionalUnionLiteralType(final String cemType) {
        if (cemType == null || cemType.isBlank()) {
            return false;
        }
        final String trimmed = cemType.trim();
        return OPTIONAL_UNION_LITERAL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Extracts the literal values from a union literal type string.
     * <p>
     * For example: {@code 'small' | 'medium' | 'large'} returns {@code ["small", "medium", "large"]}.
     * </p>
     * <p>
     * This method handles whitespace variations around the {@code |} separator and filters out
     * {@code undefined} from the result.
     * </p>
     *
     * @param cemType the union literal type string (e.g., {@code "'small' | 'medium' | 'large'"})
     * @return a list of literal values without quotes, or an empty list for null/empty input
     *         or non-union-literal types
     */
    public static List<String> extractLiteralValues(final String cemType) {
        if (cemType == null || cemType.isBlank()) {
            return List.of();
        }
        
        final String trimmed = cemType.trim();
        
        // Only process union literal types (pure or optional)
        if (!isUnionLiteralType(trimmed) && !isOptionalUnionLiteralType(trimmed)) {
            return List.of();
        }
        
        // Split by | with optional whitespace
        final String[] parts = trimmed.split("\\s*\\|\\s*");
        final List<String> values = new ArrayList<>();
        
        for (final String part : parts) {
            final String trimmedPart = part.trim();
            
            // Skip undefined
            if ("undefined".equals(trimmedPart)) {
                continue;
            }
            
            // Remove surrounding single quotes and add to list
            if (trimmedPart.startsWith("'") && trimmedPart.endsWith("'") && trimmedPart.length() >= 2) {
                values.add(trimmedPart.substring(1, trimmedPart.length() - 1));
            }
        }
        
        return values;
    }

    /**
     * Generates an enum name based on the component name and property name.
     * <p>
     * The enum name is derived by:
     * <ol>
     *   <li>Stripping the "wa-" prefix from the component name (if present)</li>
     *   <li>Converting the component name from kebab-case to PascalCase</li>
     *   <li>Converting the property name from kebab-case to PascalCase</li>
     *   <li>Concatenating both parts</li>
     * </ol>
     * </p>
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code ("wa-button", "variant")} → {@code "ButtonVariant"}</li>
     *   <li>{@code ("wa-icon", "size")} → {@code "IconSize"}</li>
     *   <li>{@code ("wa-progress-bar", "appearance")} → {@code "ProgressBarAppearance"}</li>
     * </ul>
     * </p>
     *
     * @param componentName the component name (e.g., "wa-button")
     * @param propertyName the property name (e.g., "variant")
     * @return the generated enum name in PascalCase (e.g., "ButtonVariant")
     * @throws IllegalArgumentException if componentName or propertyName is null or empty
     */
    public static String generateEnumName(final String componentName, final String propertyName) {
        if (componentName == null || componentName.isBlank()) {
            throw new IllegalArgumentException("Component name cannot be null or blank");
        }
        if (propertyName == null || propertyName.isBlank()) {
            throw new IllegalArgumentException("Property name cannot be null or blank");
        }
        
        // Strip "wa-" prefix from component name (case-insensitive)
        String strippedComponentName = componentName.trim();
        if (strippedComponentName.toLowerCase().startsWith("wa-")) {
            strippedComponentName = strippedComponentName.substring(3);
        }
        
        // Convert both parts to PascalCase and concatenate
        final String componentPart = kebabToPascalCase(strippedComponentName);
        final String propertyPart = kebabToPascalCase(propertyName.trim());
        
        return componentPart + propertyPart;
    }

    /**
     * Converts a literal value to a valid Java enum constant name in UPPER_SNAKE_CASE.
     * <p>
     * The conversion follows these rules:
     * <ol>
     *   <li>Convert all characters to uppercase</li>
     *   <li>Replace all non-alphanumeric characters with underscores</li>
     *   <li>Collapse consecutive underscores into a single underscore</li>
     *   <li>Remove leading and trailing underscores (unless needed for validity)</li>
     *   <li>If the result starts with a digit, prefix with underscore</li>
     *   <li>If the result is empty, return "_EMPTY_"</li>
     * </ol>
     * </p>
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code "neutral"} → {@code "NEUTRAL"}</li>
     *   <li>{@code "beat-fade"} → {@code "BEAT_FADE"}</li>
     *   <li>{@code "flip-both"} → {@code "FLIP_BOTH"}</li>
     *   <li>{@code "spin-pulse"} → {@code "SPIN_PULSE"}</li>
     *   <li>{@code "100%"} → {@code "_100_"}</li>
     *   <li>{@code "2xl"} → {@code "_2XL"}</li>
     *   <li>{@code "--special--"} → {@code "SPECIAL"}</li>
     *   <li>{@code ""} → {@code "_EMPTY_"}</li>
     * </ul>
     * </p>
     *
     * @param literal the literal value to convert (e.g., "beat-fade")
     * @return the UPPER_SNAKE_CASE constant name (e.g., "BEAT_FADE")
     * @throws IllegalArgumentException if literal is null
     */
    public static String literalToEnumConstant(final String literal) {
        if (literal == null) {
            throw new IllegalArgumentException("Literal cannot be null");
        }
        
        // Handle empty string
        if (literal.isEmpty()) {
            return "_EMPTY_";
        }
        
        // Convert to uppercase and replace non-alphanumeric with underscores
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < literal.length(); i++) {
            final char c = literal.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                result.append(Character.toUpperCase(c));
            } else {
                result.append('_');
            }
        }
        
        // Collapse consecutive underscores
        String collapsed = result.toString().replaceAll("_+", "_");
        
        // Remove leading underscores (we'll add back if needed for digit prefix)
        while (collapsed.startsWith("_") && collapsed.length() > 1) {
            collapsed = collapsed.substring(1);
        }
        
        // Remove trailing underscores
        while (collapsed.endsWith("_") && collapsed.length() > 1) {
            collapsed = collapsed.substring(0, collapsed.length() - 1);
        }
        
        // Handle case where result is just underscores or empty after trimming
        if (collapsed.isEmpty() || collapsed.equals("_")) {
            return "_EMPTY_";
        }
        
        // If starts with digit, prefix with underscore
        if (Character.isDigit(collapsed.charAt(0))) {
            collapsed = "_" + collapsed;
        }
        
        return collapsed;
    }

    /**
     * Gets an existing enum name from the cache or creates a new one.
     * <p>
     * This method ensures that if two properties have the same set of literal values
     * (regardless of order), they share the same enum. The cache key is computed by
     * sorting the literal values and joining them with "|".
     * </p>
     * <p>
     * Example:
     * <ul>
     *   <li>First call with ("wa-button", "variant", ["brand", "neutral", "success"]) → generates "ButtonVariant"</li>
     *   <li>Second call with ("wa-icon", "type", ["neutral", "success", "brand"]) → returns "ButtonVariant" (same values)</li>
     *   <li>Third call with ("wa-badge", "variant", ["info", "warning"]) → generates "BadgeVariant" (different values)</li>
     * </ul>
     * </p>
     *
     * @param componentName the component name (e.g., "wa-button")
     * @param propertyName the property name (e.g., "variant")
     * @param literals the list of literal values (e.g., ["brand", "neutral", "success"])
     * @return the enum name (either from cache or newly generated)
     * @throws IllegalArgumentException if any parameter is null or empty
     */
    public static String getOrCreateEnumName(final String componentName, final String propertyName, final List<String> literals) {
        if (componentName == null || componentName.isBlank()) {
            throw new IllegalArgumentException("Component name cannot be null or blank");
        }
        if (propertyName == null || propertyName.isBlank()) {
            throw new IllegalArgumentException("Property name cannot be null or blank");
        }
        if (literals == null || literals.isEmpty()) {
            throw new IllegalArgumentException("Literals list cannot be null or empty");
        }

        // Create cache key by sorting values and joining with "|"
        final String cacheKey = literals.stream()
            .sorted()
            .reduce((a, b) -> a + "|" + b)
            .orElse("");

        // Check cache first, generate new name if not found
        return ENUM_CACHE.computeIfAbsent(cacheKey, key -> generateEnumName(componentName, propertyName));
    }

    /**
     * Clears the enum cache.
     * <p>
     * This method is primarily intended for testing to ensure a clean state
     * between test cases.
     * </p>
     */
    public static void clearEnumCache() {
        ENUM_CACHE.clear();
        log.debug("Cleared enum cache");
    }

    /**
     * Converts a kebab-case string to PascalCase.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code "button"} → {@code "Button"}</li>
     *   <li>{@code "progress-bar"} → {@code "ProgressBar"}</li>
     *   <li>{@code "my-long-name"} → {@code "MyLongName"}</li>
     * </ul>
     * </p>
     *
     * @param kebabCase the kebab-case string to convert
     * @return the PascalCase string
     */
    private static String kebabToPascalCase(final String kebabCase) {
        if (kebabCase == null || kebabCase.isEmpty()) {
            return "";
        }
        
        final StringBuilder result = new StringBuilder();
        final String[] parts = kebabCase.split("-");
        
        for (final String part : parts) {
            if (!part.isEmpty()) {
                // Capitalize first letter, lowercase the rest
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1).toLowerCase());
                }
            }
        }
        
        return result.toString();
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
     * Maps a CEM type to Java with context for enum generation.
     * <p>
     * This method extends {@link #mapToJava(String)} by accepting component and property context,
     * enabling automatic enum generation for union literal types.
     * </p>
     * <p>
     * For union literal types (pure or optional), this method:
     * <ol>
     *   <li>Extracts the literal values from the type</li>
     *   <li>Gets or creates an enum name using the component and property context</li>
     *   <li>Generates the complete enum source code</li>
     *   <li>Returns a TypeMapping with enum metadata</li>
     * </ol>
     * </p>
     * <p>
     * For non-union-literal types, this method delegates to {@link #mapToJava(String)}.
     * </p>
     * <p>
     * Example:
     * <pre>
     * TypeMapping mapping = TypeMapper.mapToJavaWithContext(
     *     "'neutral' | 'brand' | 'success'",
     *     "wa-button",
     *     "variant"
     * );
     * // mapping.javaType() == "ButtonVariant"
     * // mapping.needsRecordGeneration() == true
     * // mapping.recordDefinition() contains the enum source
     * // mapping.needsImport() == true
     * // mapping.importPackage() == "com.ponysdk.core.ui.wa.enums"
     * </pre>
     * </p>
     *
     * @param cemType the CEM type string (e.g., {@code "'neutral' | 'brand' | 'success'"})
     * @param componentName the component name (e.g., {@code "wa-button"})
     * @param propertyName the property name (e.g., {@code "variant"})
     * @return TypeMapping with enum metadata if union literal, otherwise delegates to mapToJava()
     */
    public static TypeMapping mapToJavaWithContext(final String cemType, final String componentName, final String propertyName) {
        // Check if it's a union literal type (pure or optional)
        if (isUnionLiteralType(cemType) || isOptionalUnionLiteralType(cemType)) {
            // Extract literal values
            final List<String> literals = extractLiteralValues(cemType);
            
            if (literals.isEmpty()) {
                // Fallback if extraction fails
                log.warn("Failed to extract literals from union type '{}' — falling back to String", cemType);
                return TypeMapping.simple("String");
            }
            
            // Get or create enum name (uses cache to avoid duplicates)
            final String enumName = getOrCreateEnumName(componentName, propertyName, literals);
            
            // Generate enum source code
            final String packageName = "com.ponysdk.core.ui.wa.enums";
            final String enumSource = generateEnumSource(enumName, literals, packageName);
            
            // Return TypeMapping with enum metadata
            // needsRecordGeneration=true, recordDefinition=enumSource, needsImport=true, importPackage=packageName
            return new TypeMapping(enumName, true, packageName, true, enumSource);
        }
        
        // Not a union literal type, delegate to existing mapToJava()
        return mapToJava(cemType);
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
     * Generates complete Java enum source code for a union literal type.
     * <p>
     * The generated enum includes:
     * <ul>
     *   <li>Package declaration</li>
     *   <li>Javadoc with @generated annotation</li>
     *   <li>Enum constants with their original string values</li>
     *   <li>Private {@code value} field storing the original string</li>
     *   <li>Private constructor accepting the value</li>
     *   <li>Public {@code getValue()} method returning the original string</li>
     *   <li>Public static {@code fromValue(String)} method for parsing with error handling</li>
     * </ul>
     * </p>
     * <p>
     * Example output for {@code generateEnumSource("ButtonVariant", List.of("neutral", "brand", "success"), "com.ponysdk.core.ui.wa.enums")}:
     * <pre>
     * package com.ponysdk.core.ui.wa.enums;
     *
     * /**
     *  * Enum for ButtonVariant values.
     *  * @generated from custom-elements.json
     *  *&#47;
     * public enum ButtonVariant {
     *     NEUTRAL("neutral"),
     *     BRAND("brand"),
     *     SUCCESS("success");
     *
     *     private final String value;
     *
     *     ButtonVariant(String value) {
     *         this.value = value;
     *     }
     *
     *     public String getValue() {
     *         return value;
     *     }
     *
     *     public static ButtonVariant fromValue(String value) {
     *         for (ButtonVariant v : values()) {
     *             if (v.value.equals(value)) {
     *                 return v;
     *             }
     *         }
     *         throw new IllegalArgumentException(
     *             "Unknown ButtonVariant value: " + value +
     *             ". Valid values are: neutral, brand, success");
     *     }
     * }
     * </pre>
     * </p>
     *
     * @param enumName the name of the enum (e.g., "ButtonVariant")
     * @param literals the list of literal values (e.g., ["neutral", "brand", "success"])
     * @param packageName the package name (e.g., "com.ponysdk.core.ui.wa.enums")
     * @return the complete Java source code for the enum
     * @throws IllegalArgumentException if enumName, literals, or packageName is null or empty
     */
    public static String generateEnumSource(final String enumName, final List<String> literals, final String packageName) {
        if (enumName == null || enumName.isBlank()) {
            throw new IllegalArgumentException("Enum name cannot be null or blank");
        }
        if (literals == null || literals.isEmpty()) {
            throw new IllegalArgumentException("Literals list cannot be null or empty");
        }
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("Package name cannot be null or blank");
        }

        final StringBuilder sb = new StringBuilder();

        // Package declaration
        sb.append("package ").append(packageName).append(";\n\n");

        // Javadoc
        sb.append("/**\n");
        sb.append(" * Enum for ").append(enumName).append(" values.\n");
        sb.append(" * @generated from custom-elements.json\n");
        sb.append(" */\n");

        // Enum declaration
        sb.append("public enum ").append(enumName).append(" {\n");

        // Enum constants
        for (int i = 0; i < literals.size(); i++) {
            final String literal = literals.get(i);
            final String constant = literalToEnumConstant(literal);
            sb.append("    ").append(constant).append("(\"").append(escapeJavaString(literal)).append("\")");
            if (i < literals.size() - 1) {
                sb.append(",\n");
            } else {
                sb.append(";\n");
            }
        }

        sb.append("\n");

        // Private value field
        sb.append("    private final String value;\n\n");

        // Constructor
        sb.append("    ").append(enumName).append("(String value) {\n");
        sb.append("        this.value = value;\n");
        sb.append("    }\n\n");

        // getValue() method
        sb.append("    public String getValue() {\n");
        sb.append("        return value;\n");
        sb.append("    }\n\n");

        // fromValue() method with error handling
        sb.append("    public static ").append(enumName).append(" fromValue(String value) {\n");
        sb.append("        for (").append(enumName).append(" v : values()) {\n");
        sb.append("            if (v.value.equals(value)) {\n");
        sb.append("                return v;\n");
        sb.append("            }\n");
        sb.append("        }\n");
        sb.append("        throw new IllegalArgumentException(\n");
        sb.append("            \"Unknown ").append(enumName).append(" value: \" + value +\n");
        sb.append("            \". Valid values are: ").append(String.join(", ", literals)).append("\");\n");
        sb.append("    }\n");

        // Close enum
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Escapes special characters in a string for use in Java string literals.
     * <p>
     * Handles backslashes, double quotes, and common escape sequences.
     * </p>
     *
     * @param str the string to escape
     * @return the escaped string safe for use in Java source code
     */
    private static String escapeJavaString(final String str) {
        if (str == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            final char c = str.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
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
