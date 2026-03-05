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

package com.ponysdk.core.ui.codegen.generator;

import com.ponysdk.core.ui.codegen.model.ComponentDefinition;
import com.ponysdk.core.ui.codegen.model.CssPropertyDef;
import com.ponysdk.core.ui.codegen.model.EventDef;
import com.ponysdk.core.ui.codegen.model.MethodDef;
import com.ponysdk.core.ui.codegen.model.PropertyDef;
import com.ponysdk.core.ui.codegen.model.SlotDef;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generic implementation of {@link CodeGenerator} for generating Java and TypeScript
 * wrapper code from web component definitions.
 * <p>
 * This implementation is library-agnostic and works with any Custom Elements Manifest.
 * It generates code that integrates with the PonySDK infrastructure including
 * {@code PWebComponent}, {@code PropsDiffer}, and {@code Component Terminal}.
 * </p>
 */
public class CodeGeneratorImpl implements CodeGenerator {

    private final String javaPackage;

    /**
     * Creates a new code generator for the specified Java package.
     *
     * @param javaPackage the Java package for generated classes (e.g. {@code "com.ponysdk.core.ui.webawesome"})
     */
    public CodeGeneratorImpl(final String javaPackage) {
        this.javaPackage = javaPackage;
    }

    @Override
        public String generateWrapperClass(final ComponentDefinition def) {
            if (def == null) {
                throw new IllegalArgumentException("Component definition cannot be null");
            }

            final String wrapperClassName = def.getWrapperClassName();
            final String propsClassName = def.getPropsClassName();
            final boolean hasSlots = !def.slots().isEmpty();
            final boolean hasEvents = !def.events().isEmpty();
            final boolean hasCssProperties = !def.cssProperties().isEmpty();

            final StringBuilder sb = new StringBuilder();

            // Package declaration
            sb.append("package ").append(javaPackage).append(";\n\n");

            // Imports
            sb.append("import com.ponysdk.core.ui.component.PWebComponent;\n");
            if (hasSlots) {
                sb.append("import com.ponysdk.core.ui.component.PComponent;\n");
                sb.append("import java.util.Set;\n");
            }
            if (hasEvents) {
                sb.append("import java.util.function.Consumer;\n");
                sb.append("import javax.json.JsonObject;\n");
            }
            if (!def.methods().isEmpty()) {
                // Check if any method is async
                final boolean hasAsyncMethods = def.methods().stream().anyMatch(MethodDef::async);
                if (hasAsyncMethods) {
                    sb.append("import java.util.concurrent.CompletableFuture;\n");
                }
                // Check if any method parameter uses List
                final boolean hasListParams = def.methods().stream()
                    .flatMap(m -> m.parameters().stream())
                    .anyMatch(p -> p.javaType().startsWith("List<"));
                if (hasListParams) {
                    sb.append("import java.util.List;\n");
                }
                // Check if any method parameter uses Optional
                final boolean hasOptionalParams = def.methods().stream()
                    .flatMap(m -> m.parameters().stream())
                    .anyMatch(p -> p.javaType().startsWith("Optional<"));
                if (hasOptionalParams) {
                    sb.append("import java.util.Optional;\n");
                }
            }
            sb.append('\n');

            // JavaDoc
            appendJavadoc(sb, def);

            // Class declaration
            sb.append("public class ").append(wrapperClassName)
              .append(" extends PWebComponent<").append(propsClassName).append("> {\n\n");

            // TAG_NAME constant
            sb.append("    private static final String TAG_NAME = \"").append(def.tagName()).append("\";\n");

            // CSS custom property constants (if any)
            if (hasCssProperties) {
                for (final var cssProp : def.cssProperties()) {
                    sb.append("    private static final String ").append(cssProp.getConstantName())
                      .append(" = \"").append(cssProp.name()).append("\";\n");
                }
            }

            // DECLARED_SLOTS constant (if any)
            if (hasSlots) {
                sb.append("    private static final Set<String> DECLARED_SLOTS = Set.of(");
                final String slotNames = def.slots().stream()
                    .map(SlotDef::name)
                    .map(name -> "\"" + name + "\"")
                    .collect(Collectors.joining(", "));
                sb.append(slotNames);
                sb.append(");\n");
            }
            sb.append('\n');

            // Default constructor
            sb.append("    public ").append(wrapperClassName).append("() {\n");
            sb.append("        this(").append(propsClassName).append(".defaults());\n");
            sb.append("    }\n\n");

            // Constructor with initial props
            sb.append("    public ").append(wrapperClassName)
              .append("(final ").append(propsClassName).append(" initialProps) {\n");
            if (hasSlots) {
                sb.append("        super(initialProps, DECLARED_SLOTS);\n");
            } else {
                sb.append("        super(initialProps);\n");
            }
            sb.append("    }\n\n");

            // getPropsClass() override
            sb.append("    @Override\n");
            sb.append("    protected Class<").append(propsClassName).append("> getPropsClass() {\n");
            sb.append("        return ").append(propsClassName).append(".class;\n");
            sb.append("    }\n\n");

            // getComponentSignature() override
            sb.append("    @Override\n");
            sb.append("    protected String getComponentSignature() {\n");
            sb.append("        return TAG_NAME;\n");
            sb.append("    }\n");

            // Generate event listener methods
            if (hasEvents) {
                sb.append('\n');
                sb.append("    // ========== GENERATED EVENT HANDLERS ==========\n\n");
                appendEventListenerMethods(sb, def);
                sb.append("    // ========== END GENERATED CODE ==========\n");
            }

            // Generate slot management methods
            if (hasSlots) {
                sb.append('\n');
                sb.append("    // ========== GENERATED SLOT METHODS ==========\n\n");
                appendSlotMethods(sb, def);
                sb.append("    // ========== END GENERATED CODE ==========\n");
            }

            // Generate component method proxies
            if (!def.methods().isEmpty()) {
                sb.append('\n');
                sb.append("    // ========== GENERATED METHOD PROXIES ==========\n\n");
                appendMethodProxies(sb, def);
                sb.append("    // ========== END GENERATED CODE ==========\n");
            }

            // Generate CSS custom property setters
            if (hasCssProperties) {
                sb.append('\n');
                sb.append("    // ========== GENERATED CSS PROPERTY SETTERS ==========\n\n");
                appendCssPropertySetters(sb, def);
                sb.append("    // ========== END GENERATED CODE ==========\n");
            }

            // Placeholder for manual extensions
            sb.append('\n');
            sb.append("    // Manual extensions can be added below this line\n");

            sb.append("}\n");

            return sb.toString();
        }


    @Override
    public String generatePropsRecord(final ComponentDefinition def) {
        if (def == null) {
            throw new IllegalArgumentException("Component definition cannot be null");
        }

        final String propsClassName = def.getPropsClassName();
        final var publicProperties = def.getPublicProperties();
        
        final StringBuilder sb = new StringBuilder();

        // Package declaration
        sb.append("package ").append(javaPackage).append(";\n\n");

        // Imports
        final boolean hasOptional = publicProperties.stream().anyMatch(p -> p.javaType().startsWith("Optional<"));
        final boolean hasList = publicProperties.stream().anyMatch(p -> p.javaType().startsWith("List<"));
        
        if (hasOptional) {
            sb.append("import java.util.Optional;\n");
        }
        if (hasList) {
            sb.append("import java.util.List;\n");
        }
        if (hasOptional || hasList) {
            sb.append('\n');
        }

        // Record JavaDoc
        sb.append("/**\n");
        sb.append(" * Props for the ").append(def.className()).append(" component.\n");
        
        if (def.description() != null && !def.description().isEmpty()) {
            sb.append(" * <p>\n");
            final String[] lines = def.description().split("\n");
            for (final String line : lines) {
                sb.append(" * ").append(escapeJavadoc(line.trim())).append("\n");
            }
            sb.append(" * </p>\n");
        }
        
        // Document each parameter
        if (!publicProperties.isEmpty()) {
            sb.append(" *\n");
            for (final var prop : publicProperties) {
                sb.append(" * @param ").append(prop.name());
                if (prop.description() != null && !prop.description().isEmpty()) {
                    sb.append(" ").append(escapeJavadoc(prop.description()));
                }
                sb.append("\n");
            }
        }
        
        sb.append(" */\n");

        // Record declaration
        sb.append("public record ").append(propsClassName).append("(\n");
        
        // Record components
        for (int i = 0; i < publicProperties.size(); i++) {
            final var prop = publicProperties.get(i);
            sb.append("    ").append(prop.javaType()).append(" ").append(prop.name());
            if (i < publicProperties.size() - 1) {
                sb.append(",\n");
            } else {
                sb.append("\n");
            }
        }
        
        sb.append(") {\n\n");

        // defaults() static method
        sb.append("    /**\n");
        sb.append("     * Returns a new instance with default values.\n");
        sb.append("     *\n");
        sb.append("     * @return props with default values\n");
        sb.append("     */\n");
        sb.append("    public static ").append(propsClassName).append(" defaults() {\n");
        sb.append("        return new ").append(propsClassName).append("(");
        
        if (!publicProperties.isEmpty()) {
            sb.append("\n");
            for (int i = 0; i < publicProperties.size(); i++) {
                final var prop = publicProperties.get(i);
                sb.append("            ");
                
                // Use default value if available, otherwise use type-appropriate default
                if (prop.defaultValue() != null && !prop.defaultValue().isEmpty()) {
                    sb.append(formatDefaultValue(prop));
                } else {
                    sb.append(getTypeDefault(prop.javaType()));
                }
                
                if (i < publicProperties.size() - 1) {
                    sb.append(",\n");
                } else {
                    sb.append("\n");
                }
            }
            sb.append("        ");
        }
        
        sb.append(");\n");
        sb.append("    }\n");

        // Builder methods (withXxx)
        for (final var prop : publicProperties) {
            sb.append("\n");
            sb.append("    /**\n");
            sb.append("     * Returns a new instance with the specified ").append(prop.name()).append(".\n");
            sb.append("     *\n");
            sb.append("     * @param ").append(prop.name()).append(" the new value\n");
            sb.append("     * @return new props instance\n");
            sb.append("     */\n");
            
            // Method name: withXxx
            final String methodName = "with" + Character.toUpperCase(prop.name().charAt(0)) + 
                                     (prop.name().length() > 1 ? prop.name().substring(1) : "");
            
            sb.append("    public ").append(propsClassName).append(" ").append(methodName)
              .append("(final ").append(prop.javaType()).append(" ").append(prop.name()).append(") {\n");
            
            // Return new instance with updated value
            sb.append("        return new ").append(propsClassName).append("(");
            
            if (publicProperties.size() == 1) {
                sb.append(prop.name());
            } else {
                sb.append("\n");
                for (int i = 0; i < publicProperties.size(); i++) {
                    final var p = publicProperties.get(i);
                    sb.append("            ");
                    if (p.name().equals(prop.name())) {
                        sb.append(prop.name());
                    } else {
                        sb.append("this.").append(p.name()).append("()");
                    }
                    if (i < publicProperties.size() - 1) {
                        sb.append(",\n");
                    } else {
                        sb.append("\n");
                    }
                }
                sb.append("        ");
            }
            
            sb.append(");\n");
            sb.append("    }\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Formats a default value for use in generated code.
     *
     * @param prop the property definition
     * @return formatted default value
     */
    private static String formatDefaultValue(final PropertyDef prop) {
        final String defaultValue = prop.defaultValue();
        final String javaType = prop.javaType();
        
        // Handle string literals
        if (javaType.equals("String")) {
            if (!defaultValue.startsWith("\"")) {
                return "\"" + defaultValue + "\"";
            }
            return defaultValue;
        }
        
        // Handle boolean
        if (javaType.equals("boolean")) {
            return defaultValue.toLowerCase();
        }
        
        // Handle Optional
        if (javaType.startsWith("Optional<")) {
            if ("null".equals(defaultValue) || defaultValue.isEmpty()) {
                return "Optional.empty()";
            }
            // Extract inner type and wrap value
            final String innerType = javaType.substring(9, javaType.length() - 1);
            if (innerType.equals("String")) {
                return "Optional.of(\"" + defaultValue + "\")";
            }
            return "Optional.of(" + defaultValue + ")";
        }
        
        // Handle List
        if (javaType.startsWith("List<")) {
            if ("null".equals(defaultValue) || defaultValue.isEmpty()) {
                return "List.of()";
            }
            return defaultValue;
        }
        
        // Default: use as-is
        return defaultValue;
    }

    /**
     * Returns a type-appropriate default value for a Java type.
     *
     * @param javaType the Java type
     * @return default value
     */
    private static String getTypeDefault(final String javaType) {
        return switch (javaType) {
            case "boolean" -> "false";
            case "int", "long", "short", "byte" -> "0";
            case "double", "float" -> "0.0";
            case "char" -> "'\\0'";
            default -> {
                if (javaType.startsWith("Optional<")) {
                    yield "Optional.empty()";
                } else if (javaType.startsWith("List<")) {
                    yield "List.of()";
                } else {
                    yield "null";
                }
            }
        };
    }

    @Override
    public String generateTypeScriptInterface(final ComponentDefinition def) {
        if (def == null) {
            throw new IllegalArgumentException("Component definition cannot be null");
        }

        final String propsInterfaceName = def.getPropsClassName();
        final var publicProperties = def.getPublicProperties();
        
        final StringBuilder sb = new StringBuilder();

        // JSDoc for interface
        sb.append("/**\n");
        sb.append(" * Props for the ").append(def.className()).append(" component.\n");
        
        if (def.description() != null && !def.description().isEmpty()) {
            sb.append(" * \n");
            final String[] lines = def.description().split("\n");
            for (final String line : lines) {
                sb.append(" * ").append(escapeJSDoc(line.trim())).append("\n");
            }
        }
        
        sb.append(" */\n");

        // Interface declaration
        sb.append("export interface ").append(propsInterfaceName).append(" {\n");
        
        // Interface properties
        for (final var prop : publicProperties) {
            // JSDoc for property
            if (prop.description() != null && !prop.description().isEmpty()) {
                sb.append("    /** ").append(escapeJSDoc(prop.description())).append(" */\n");
            }
            
            // Property declaration
            sb.append("    ").append(prop.name());
            
            // Optional marker if not required
            if (!prop.required()) {
                sb.append("?");
            }
            
            sb.append(": ").append(prop.tsType()).append(";\n");
        }
        
        sb.append("}\n\n");

        // Type guard function
        sb.append("/**\n");
        sb.append(" * Type guard for ").append(propsInterfaceName).append(".\n");
        sb.append(" * \n");
        sb.append(" * @param obj the object to check\n");
        sb.append(" * @returns true if obj is ").append(propsInterfaceName).append("\n");
        sb.append(" */\n");
        sb.append("export function is").append(propsInterfaceName)
          .append("(obj: unknown): obj is ").append(propsInterfaceName).append(" {\n");
        
        // Check if object
        sb.append("    if (typeof obj !== 'object' || obj === null) return false;\n");
        sb.append("    const props = obj as Record<string, unknown>;\n");
        sb.append("    \n");
        
        // Check each property
        for (final var prop : publicProperties) {
            final String propName = prop.name();
            final String tsType = prop.tsType();
            
            // Skip check if property is optional
            if (!prop.required()) {
                sb.append("    if (props.").append(propName).append(" !== undefined && ");
                sb.append(generateTypeCheck(propName, tsType));
                sb.append(") return false;\n");
            } else {
                // Required property must exist and have correct type
                sb.append("    if (").append(generateTypeCheck(propName, tsType));
                sb.append(") return false;\n");
            }
        }
        
        sb.append("    \n");
        sb.append("    return true;\n");
        sb.append("}\n");

        // Generate event detail interfaces for events with custom detail types
        for (final var event : def.events()) {
            if (event.needsDetailClass()) {
                sb.append("\n");
                appendEventDetailInterface(sb, event, def.className());
            }
        }

        return sb.toString();
    }

    /**
     * Appends a TypeScript interface for an event detail type.
     *
     * @param sb        the string builder to append to
     * @param event     the event definition
     * @param className the component class name
     */
    private static void appendEventDetailInterface(final StringBuilder sb, final EventDef event, final String className) {
        final String eventName = toPascalCase(event.name().replaceFirst("^[a-z]+-", ""));
        final String interfaceName = className + eventName + "Detail";
        
        // JSDoc
        sb.append("/**\n");
        sb.append(" * Event detail for the '").append(event.name()).append("' event.\n");
        
        if (event.description() != null && !event.description().isEmpty()) {
            sb.append(" * \n");
            sb.append(" * ").append(escapeJSDoc(event.description())).append("\n");
        }
        
        sb.append(" */\n");
        
        // Parse detail type and generate interface
        final String detailType = event.detailType();
        
        if (detailType.startsWith("{") && detailType.endsWith("}")) {
            // Structured object type
            sb.append("export interface ").append(interfaceName).append(" {\n");
            
            // Parse fields from object type (simplified parsing)
            final String fieldsStr = detailType.substring(1, detailType.length() - 1).trim();
            if (!fieldsStr.isEmpty()) {
                final String[] fields = fieldsStr.split(",");
                for (final String field : fields) {
                    final String[] parts = field.trim().split(":");
                    if (parts.length == 2) {
                        final String fieldName = parts[0].trim();
                        final String fieldType = parts[1].trim();
                        sb.append("    ").append(fieldName).append(": ").append(fieldType).append(";\n");
                    }
                }
            }
            
            sb.append("}\n");
        } else {
            // Simple type alias
            sb.append("export type ").append(interfaceName).append(" = ").append(detailType).append(";\n");
        }
    }

    /**
     * Converts a kebab-case or snake_case string to PascalCase.
     *
     * @param str the string to convert
     * @return the PascalCase string
     */
    private static String toPascalCase(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        final String[] parts = str.split("[-_]");
        final StringBuilder result = new StringBuilder();
        
        for (final String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1));
                }
            }
        }
        
        return result.toString();
    }

    /**
     * Generates a TypeScript type check expression for a property.
     *
     * @param propName the property name
     * @param tsType   the TypeScript type
     * @return the type check expression
     */
    private static String generateTypeCheck(final String propName, final String tsType) {
        // Handle primitive types
        if ("string".equals(tsType)) {
            return "typeof props." + propName + " !== 'string'";
        } else if ("number".equals(tsType)) {
            return "typeof props." + propName + " !== 'number'";
        } else if ("boolean".equals(tsType)) {
            return "typeof props." + propName + " !== 'boolean'";
        }
        
        // Handle array types
        if (tsType.endsWith("[]")) {
            return "!Array.isArray(props." + propName + ")";
        }
        
        // Handle union types with undefined
        if (tsType.contains(" | undefined")) {
            final String baseType = tsType.replace(" | undefined", "").trim();
            return generateTypeCheck(propName, baseType);
        }
        
        // Handle string literal unions (e.g., 'small' | 'medium' | 'large')
        if (tsType.contains("'") && tsType.contains("|")) {
            final String[] literals = tsType.split("\\|");
            final StringBuilder check = new StringBuilder();
            check.append("!(");
            for (int i = 0; i < literals.length; i++) {
                if (i > 0) {
                    check.append(" || ");
                }
                final String literal = literals[i].trim().replace("'", "");
                check.append("props.").append(propName).append(" === '").append(literal).append("'");
            }
            check.append(")");
            return check.toString();
        }
        
        // Handle object types
        if (tsType.startsWith("{") || tsType.equals("object")) {
            return "typeof props." + propName + " !== 'object' || props." + propName + " === null";
        }
        
        // Default: check for object type
        return "typeof props." + propName + " !== 'object'";
    }

    @Override
    public Map<String, String> generateEventDetailClasses(final ComponentDefinition def) {
        if (def == null) {
            throw new IllegalArgumentException("Component definition cannot be null");
        }

        final Map<String, String> eventDetailClasses = new java.util.HashMap<>();
        
        // Generate a class for each event with custom detail type
        for (final var event : def.events()) {
            if (event.needsDetailClass()) {
                final String eventName = toPascalCase(event.name().replaceFirst("^[a-z]+-", ""));
                final String className = def.className() + eventName + "Detail";
                final String code = generateEventDetailClass(className, event, def.className());
                eventDetailClasses.put(className, code);
            }
        }
        
        return eventDetailClasses;
    }

    /**
     * Generates a Java record class for an event detail type.
     *
     * @param className     the class name for the detail record
     * @param event         the event definition
     * @param componentName the component class name
     * @return the generated Java source code
     */
    private String generateEventDetailClass(final String className, final EventDef event, final String componentName) {
        final StringBuilder sb = new StringBuilder();
        
        // Package declaration
        sb.append("package ").append(javaPackage).append(".events;\n\n");
        
        // Imports (if needed)
        final String detailType = event.detailType();
        if (detailType.contains("List<") || detailType.contains("[]")) {
            sb.append("import java.util.List;\n");
        }
        if (detailType.contains("Optional<")) {
            sb.append("import java.util.Optional;\n");
        }
        if (detailType.contains("List<") || detailType.contains("Optional<")) {
            sb.append("\n");
        }
        
        // JavaDoc
        sb.append("/**\n");
        sb.append(" * Event detail for the '").append(event.name()).append("' event of ")
          .append(componentName).append(" component.\n");
        
        if (event.description() != null && !event.description().isEmpty()) {
            sb.append(" * <p>\n");
            sb.append(" * ").append(escapeJavadoc(event.description())).append("\n");
            sb.append(" * </p>\n");
        }
        
        sb.append(" */\n");
        
        // Parse detail type and generate record
        if (detailType.startsWith("{") && detailType.endsWith("}")) {
            // Structured object type - generate record
            sb.append("public record ").append(className).append("(\n");
            
            // Parse fields from object type
            final String fieldsStr = detailType.substring(1, detailType.length() - 1).trim();
            if (!fieldsStr.isEmpty()) {
                final String[] fields = fieldsStr.split(",");
                for (int i = 0; i < fields.length; i++) {
                    final String field = fields[i].trim();
                    final String[] parts = field.split(":");
                    if (parts.length == 2) {
                        final String fieldName = parts[0].trim();
                        final String fieldType = parts[1].trim();
                        
                        // Map TypeScript type to Java type
                        final String javaType = mapTypeScriptToJava(fieldType);
                        
                        sb.append("    ").append(javaType).append(" ").append(fieldName);
                        if (i < fields.length - 1) {
                            sb.append(",\n");
                        } else {
                            sb.append("\n");
                        }
                    }
                }
            }
            
            sb.append(") {}\n");
        } else {
            // Simple type - generate type alias as record with single field
            final String javaType = mapTypeScriptToJava(detailType);
            sb.append("public record ").append(className).append("(\n");
            sb.append("    ").append(javaType).append(" value\n");
            sb.append(") {}\n");
        }
        
        return sb.toString();
    }

    /**
     * Maps a TypeScript type to a Java type.
     * This is a simplified mapping for event detail types.
     *
     * @param tsType the TypeScript type
     * @return the Java type
     */
    private static String mapTypeScriptToJava(final String tsType) {
        final String trimmed = tsType.trim();
        
        // Handle primitive types
        if ("string".equals(trimmed)) {
            return "String";
        } else if ("number".equals(trimmed)) {
            return "double";
        } else if ("boolean".equals(trimmed)) {
            return "boolean";
        }
        
        // Handle array types
        if (trimmed.endsWith("[]")) {
            final String elementType = trimmed.substring(0, trimmed.length() - 2);
            return "List<" + mapTypeScriptToJava(elementType) + ">";
        }
        
        // Handle union with undefined (make Optional)
        if (trimmed.contains(" | undefined")) {
            final String baseType = trimmed.replace(" | undefined", "").trim();
            final String javaBaseType = mapTypeScriptToJava(baseType);
            // Don't wrap primitives in Optional, use boxed types instead
            if ("boolean".equals(javaBaseType)) {
                return "Boolean";
            } else if ("double".equals(javaBaseType)) {
                return "Double";
            }
            return "Optional<" + javaBaseType + ">";
        }
        
        // Handle object types
        if (trimmed.startsWith("{") || "object".equals(trimmed)) {
            return "Object";
        }
        
        // Default: use Object
        return "Object";
    }

    /**
     * Appends JavaDoc comment for the wrapper class.
     *
     * @param sb  the string builder to append to
     * @param def the component definition
     */
    private static void appendJavadoc(final StringBuilder sb, final ComponentDefinition def) {
        sb.append("/**\n");

        // Summary
        if (def.summary() != null && !def.summary().isEmpty()) {
            sb.append(" * ").append(escapeJavadoc(def.summary())).append("\n");
        }

        // Description
        if (def.description() != null && !def.description().isEmpty()) {
            sb.append(" * <p>\n");
            final String[] lines = def.description().split("\n");
            for (final String line : lines) {
                sb.append(" * ").append(escapeJavadoc(line.trim())).append("\n");
            }
            sb.append(" * </p>\n");
        }

        // Status warning
        if ("experimental".equals(def.status())) {
            sb.append(" * <p>\n");
            sb.append(" * <strong>Warning:</strong> This component is experimental and may change in future versions.\n");
            sb.append(" * </p>\n");
        } else if ("deprecated".equals(def.status())) {
            sb.append(" * <p>\n");
            sb.append(" * <strong>Deprecated:</strong> This component is deprecated and may be removed in future versions.\n");
            sb.append(" * </p>\n");
        }

        // See also
        sb.append(" *\n");
        sb.append(" * @see ").append(def.getPropsClassName()).append("\n");

        sb.append(" */\n");
    }

    /**
     * Appends event listener methods for all events defined in the component.
     *
     * @param sb  the string builder to append to
     * @param def the component definition
     */
    private static void appendEventListenerMethods(final StringBuilder sb, final ComponentDefinition def) {
        for (final var event : def.events()) {
            // JavaDoc
            sb.append("    /**\n");
            sb.append("     * Registers a handler for the '").append(event.name()).append("' event.\n");
            
            if (event.description() != null && !event.description().isEmpty()) {
                sb.append("     * <p>\n");
                sb.append("     * ").append(escapeJavadoc(event.description())).append("\n");
                sb.append("     * </p>\n");
            }
            
            sb.append("     *\n");
            sb.append("     * @param handler the event handler\n");
            sb.append("     */\n");
            
            // Method signature
            sb.append("    public void ").append(event.getListenerMethodName())
              .append("(final Consumer<JsonObject> handler) {\n");
            
            // Method body - call onEvent
            sb.append("        onEvent(\"").append(event.name()).append("\", handler);\n");
            sb.append("    }\n\n");
        }
    }

    /**
     * Appends slot management methods for all slots defined in the component.
     *
     * @param sb  the string builder to append to
     * @param def the component definition
     */
    private static void appendSlotMethods(final StringBuilder sb, final ComponentDefinition def) {
        for (final var slot : def.slots()) {
            // JavaDoc
            sb.append("    /**\n");
            if (slot.isDefaultSlot()) {
                sb.append("     * Adds a component to the default slot.\n");
            } else {
                sb.append("     * Adds a component to the '").append(slot.name()).append("' slot.\n");
            }
            
            if (slot.description() != null && !slot.description().isEmpty()) {
                sb.append("     * <p>\n");
                sb.append("     * ").append(escapeJavadoc(slot.description())).append("\n");
                sb.append("     * </p>\n");
            }
            
            sb.append("     *\n");
            sb.append("     * @param child the component to add\n");
            sb.append("     */\n");
            
            // Method signature
            sb.append("    public void ").append(slot.getSlotMethodName())
              .append("(final PComponent<?> child) {\n");
            
            // Method body - call addToSlot
            if (slot.isDefaultSlot()) {
                sb.append("        addToSlot(\"\", child);\n");
            } else {
                sb.append("        addToSlot(\"").append(slot.name()).append("\", child);\n");
            }
            sb.append("    }\n\n");
        }
    }

    /**
     * Appends component method proxy methods for all methods defined in the component.
     *
     * @param sb  the string builder to append to
     * @param def the component definition
     */
    private static void appendMethodProxies(final StringBuilder sb, final ComponentDefinition def) {
        for (final var method : def.methods()) {
            // JavaDoc
            sb.append("    /**\n");
            
            if (method.description() != null && !method.description().isEmpty()) {
                sb.append("     * ").append(escapeJavadoc(method.description())).append("\n");
                sb.append("     *\n");
            }
            
            // Parameter descriptions
            for (final var param : method.parameters()) {
                sb.append("     * @param ").append(param.name());
                if (param.description() != null && !param.description().isEmpty()) {
                    sb.append(" ").append(escapeJavadoc(param.description()));
                }
                sb.append("\n");
            }
            
            // Return description
            if (method.async()) {
                sb.append("     * @return future containing the result\n");
            } else if (!"void".equals(method.returnType())) {
                sb.append("     * @return the result\n");
            }
            
            sb.append("     */\n");
            
            // Method signature
            sb.append("    public ");
            
            // Return type
            if (method.async()) {
                // Map return type and wrap in CompletableFuture
                final String mappedReturnType = mapReturnType(method.returnType());
                sb.append("CompletableFuture<").append(mappedReturnType).append(">");
            } else {
                sb.append(mapReturnType(method.returnType()));
            }
            
            sb.append(" ").append(method.name()).append("(");
            
            // Parameters
            if (!method.parameters().isEmpty()) {
                for (int i = 0; i < method.parameters().size(); i++) {
                    final var param = method.parameters().get(i);
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append("final ").append(param.javaType()).append(" ").append(param.name());
                }
            }
            
            sb.append(") {\n");
            
            // Method body - bridge call
            if (method.async()) {
                sb.append("        return callComponentMethodAsync(\"").append(method.name()).append("\"");
            } else {
                if ("void".equals(method.returnType())) {
                    sb.append("        callComponentMethod(\"").append(method.name()).append("\"");
                } else {
                    sb.append("        return callComponentMethod(\"").append(method.name()).append("\"");
                }
            }
            
            // Add parameters to bridge call
            for (final var param : method.parameters()) {
                sb.append(", ").append(param.name());
            }
            
            sb.append(");\n");
            sb.append("    }\n\n");
        }
    }

    /**
     * Appends CSS custom property setter methods for all CSS properties defined in the component.
     *
     * @param sb  the string builder to append to
     * @param def the component definition
     */
    private static void appendCssPropertySetters(final StringBuilder sb, final ComponentDefinition def) {
        for (final var cssProp : def.cssProperties()) {
            // Convert CSS property name to method name
            // e.g., "--button-background" -> "setButtonBackground"
            final String propertyName = cssProp.name().replaceFirst("^--", "");
            final String[] parts = propertyName.split("-");
            final StringBuilder methodNameBuilder = new StringBuilder("set");
            for (final String part : parts) {
                if (!part.isEmpty()) {
                    methodNameBuilder.append(Character.toUpperCase(part.charAt(0)));
                    if (part.length() > 1) {
                        methodNameBuilder.append(part.substring(1));
                    }
                }
            }
            final String methodName = methodNameBuilder.toString();
            
            // JavaDoc
            sb.append("    /**\n");
            sb.append("     * Sets the '").append(cssProp.name()).append("' CSS custom property.\n");
            
            if (cssProp.description() != null && !cssProp.description().isEmpty()) {
                sb.append("     * <p>\n");
                sb.append("     * ").append(escapeJavadoc(cssProp.description())).append("\n");
                sb.append("     * </p>\n");
            }
            
            if (cssProp.syntax() != null && !cssProp.syntax().isEmpty()) {
                sb.append("     * <p>\n");
                sb.append("     * Syntax: ").append(escapeJavadoc(cssProp.syntax())).append("\n");
                sb.append("     * </p>\n");
            }
            
            if (cssProp.defaultValue() != null && !cssProp.defaultValue().isEmpty()) {
                sb.append("     * <p>\n");
                sb.append("     * Default: ").append(escapeJavadoc(cssProp.defaultValue())).append("\n");
                sb.append("     * </p>\n");
            }
            
            sb.append("     *\n");
            sb.append("     * @param value the CSS property value\n");
            sb.append("     */\n");
            
            // Method signature
            sb.append("    public void ").append(methodName).append("(final String value) {\n");
            
            // Method body - call setCssProperty
            sb.append("        setCssProperty(").append(cssProp.getConstantName()).append(", value);\n");
            sb.append("    }\n\n");
        }
    }

    /**
     * Maps a CEM return type to Java type.
     * Handles void, Promise types, and delegates to TypeMapper for other types.
     *
     * @param cemReturnType the CEM return type
     * @return the Java return type
     */
    private static String mapReturnType(final String cemReturnType) {
        if (cemReturnType == null || cemReturnType.isEmpty() || "void".equals(cemReturnType)) {
            return "void";
        }
        
        // Handle Promise<T> types - extract T
        if (cemReturnType.startsWith("Promise<") && cemReturnType.endsWith(">")) {
            final String innerType = cemReturnType.substring(8, cemReturnType.length() - 1);
            // Map void to Void for CompletableFuture
            if ("void".equals(innerType)) {
                return "Void";
            }
            return mapReturnType(innerType);
        }
        
        // Use TypeMapper for other types
        return com.ponysdk.core.ui.wa.codegen.TypeMapper.mapToJava(cemReturnType).javaType();
    }

    /**
     * Escapes special characters in JavaDoc text.
     *
     * @param text the text to escape
     * @return the escaped text
     */
    private static String escapeJavadoc(final String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("@", "&#64;");
    }

    /**
     * Escapes special characters in JSDoc text.
     *
     * @param text the text to escape
     * @return the escaped text
     */
    private static String escapeJSDoc(final String text) {
        if (text == null) {
            return "";
        }
        return text.replace("*/", "*\\/")
                   .replace("\\", "\\\\");
    }
}
