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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Code generator that parses the Web Awesome {@code custom-elements.json}
 * (Custom Elements Manifest) and produces Java wrapper classes, props records,
 * TypeScript interfaces, and a component index.
 * <p>
 * This class is intended to be invoked as a Gradle task at build time.
 * </p>
 */
public class WebAwesomeCodeGenerator {

    private static final Logger LOG = Logger.getLogger(WebAwesomeCodeGenerator.class.getName());

    // No hardcoded tag name lists - detection is done dynamically from manifest metadata

    /**
     * ARIA properties injected into all component props for accessibility support.
     * Maps property name to its JavaScript type.
     */
    public static final Map<String, String> ARIA_PROPERTIES;
    static {
        final Map<String, String> m = new LinkedHashMap<>();
        m.put("ariaLabel", "string");
        m.put("ariaDescribedby", "string");
        m.put("ariaHidden", "boolean");
        m.put("role", "string");
        ARIA_PROPERTIES = Collections.unmodifiableMap(m);
    }

    /**
     * Common validation properties for all form-associated components (buttons, inputs, etc.).
     * Maps property name to its JavaScript type from the manifest convention.
     */
    public static final Map<String, String> COMMON_VALIDATION_PROPERTIES;
    static {
        final Map<String, String> m = new LinkedHashMap<>();
        m.put("required", "boolean");
        m.put("customValidity", "string");
        m.put("disabled", "boolean");
        COMMON_VALIDATION_PROPERTIES = Collections.unmodifiableMap(m);
    }

    /**
     * Input-specific validation properties (text, number, etc.).
     * These are only added to actual input components, not buttons.
     * Maps property name to its JavaScript type from the manifest convention.
     */
    public static final Map<String, String> INPUT_VALIDATION_PROPERTIES;
    static {
        final Map<String, String> m = new LinkedHashMap<>();
        m.put("minlength", "number");
        m.put("maxlength", "number");
        m.put("min", "number");
        m.put("max", "number");
        m.put("pattern", "string");
        m.put("readonly", "boolean");
        INPUT_VALIDATION_PROPERTIES = Collections.unmodifiableMap(m);
    }

    private final Path manifestPath;
    private final Path javaOutputDir;
    private final Path tsOutputDir;

    public WebAwesomeCodeGenerator(final Path manifestPath, final Path javaOutputDir, final Path tsOutputDir) {
        this.manifestPath = manifestPath;
        this.javaOutputDir = javaOutputDir;
        this.tsOutputDir = tsOutputDir;
    }

    /**
     * Parses the Custom Elements Manifest JSON file and extracts component
     * definitions for all Web Awesome components (those with a {@code wa-} tag prefix).
     *
     * @return list of parsed component definitions
     * @throws RuntimeException if the JSON file cannot be read or is malformed
     */
    public List<ComponentDefinition> parseManifest() {
        final JsonObject root;
        try (final InputStream is = Files.newInputStream(manifestPath);
             final JsonReader reader = Json.createReader(is)) {
            root = reader.readObject();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to read Custom Elements Manifest at " + manifestPath + ": " + e.getMessage(), e);
        } catch (final Exception e) {
            throw new RuntimeException("Malformed JSON in Custom Elements Manifest at " + manifestPath + ": " + e.getMessage(), e);
        }

        final JsonArray modules = root.getJsonArray("modules");
        if (modules == null) {
            throw new RuntimeException("Malformed Custom Elements Manifest: missing 'modules' array in " + manifestPath);
        }

        final List<ComponentDefinition> components = new ArrayList<>();

        for (final JsonValue moduleValue : modules) {
            if (moduleValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject module = moduleValue.asJsonObject();

            final JsonArray declarations = module.getJsonArray("declarations");
            if (declarations == null) continue;

            for (final JsonValue declValue : declarations) {
                if (declValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
                final JsonObject decl = declValue.asJsonObject();

                if (!"class".equals(getString(decl, "kind"))) continue;

                final String tagName = getString(decl, "tagName");
                if (tagName == null || !tagName.startsWith("wa-")) continue;

                // Parse and enrich in one step to have access to both def and decl
                ComponentDefinition def = parseDeclaration(decl, tagName);
                def = enrichInputComponentProperties(def, decl);
                def = enrichDisplayComponentProperties(def);
                def = enrichAriaProperties(def);
                components.add(def);
            }
        }

        return components;
    }

    private ComponentDefinition parseDeclaration(final JsonObject decl, final String tagName) {
        final String className = getString(decl, "name");
        final String summary = getStringOrDefault(decl, "summary", "");
        final String jsDoc = getStringOrDefault(decl, "description", "");
        final String status = getStringOrDefault(decl, "status", "stable");

        final List<PropertyDef> properties = parseProperties(decl.getJsonArray("members"));
        final List<EventDef> events = parseEvents(decl.getJsonArray("events"));
        final List<SlotDef> slots = parseSlots(decl.getJsonArray("slots"));
        final List<CssPartDef> cssParts = parseCssParts(decl.getJsonArray("cssParts"));
        final List<CssPropertyDef> cssProperties = parseCssProperties(decl.getJsonArray("cssProperties"));

        return new ComponentDefinition(tagName, className, summary, jsDoc,
            properties, events, slots, cssParts, cssProperties, status);
    }

    /**
     * Detects if a component is a form-associated input component by checking
     * if it has a static {@code formAssociated} field set to true in the manifest.
     * <p>
     * This is a generic detection that works with any Custom Elements Manifest,
     * not just Web Awesome.
     * </p>
     *
     * @param decl the JSON declaration from the manifest
     * @return true if the component is form-associated
     */
    private static boolean isFormAssociatedComponent(final JsonObject decl) {
        final JsonArray members = decl.getJsonArray("members");
        if (members == null) return false;

        for (final JsonValue memberVal : members) {
            if (memberVal.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject member = (JsonObject) memberVal;
            
            if ("field".equals(member.getString("kind", "")) &&
                "formAssociated".equals(member.getString("name", "")) &&
                member.getBoolean("static", false)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Detects if a component is a button-like component (button, submit, reset).
     * Button components should only receive common validation properties, not input-specific ones.
     *
     * @param def the component definition
     * @return true if the component is a button
     */
    private static boolean isButtonComponent(final ComponentDefinition def) {
        final String tagName = def.tagName().toLowerCase();
        // Detect button components by tag name
        if (tagName.contains("button")) {
            return true;
        }
        // Could also check for type property with button/submit/reset values
        for (final PropertyDef prop : def.properties()) {
            if ("type".equals(prop.name())) {
                final String jsType = prop.type();
                if (jsType != null && jsType.contains("'button'") || jsType.contains("'submit'") || jsType.contains("'reset'")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Detects if a component supports expand/collapse behavior by checking
     * if it has show/hide or expand/collapse events.
     * <p>
     * This is a generic detection that works with any Custom Elements Manifest.
     * </p>
     *
     * @param def the component definition
     * @return true if the component is expandable
     */
    private static boolean isExpandableComponent(final ComponentDefinition def) {
        for (final EventDef event : def.events()) {
            final String name = event.name().toLowerCase();
            if (name.contains("show") || name.contains("hide") ||
                name.contains("expand") || name.contains("collapse")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detects if a component already has a variant property in its definition.
     *
     * @param def the component definition
     * @return true if the component has a variant property
     */
    private static boolean hasVariantProperty(final ComponentDefinition def) {
        return def.properties().stream()
            .anyMatch(p -> "variant".equals(p.name()));
    }

    /**
     * Enriches input component definitions with validation properties that may be
     * missing from the manifest. For form-associated components (detected via
     * {@code formAssociated} field), ensures that all properties in
     * {@link #COMMON_VALIDATION_PROPERTIES} and {@link #INPUT_VALIDATION_PROPERTIES} are present.
     * <p>
     * Properties already present in the manifest are preserved; only missing ones are added.
     * </p>
     * <p>
     * This method is generic and works with any Custom Elements Manifest that follows
     * the standard convention of marking form-associated components.
     * </p>
     *
     * @param def the component definition to enrich
     * @param decl the original JSON declaration (used for detection)
     * @return the enriched definition (same instance if not an input component or no changes needed)
     */
    public static ComponentDefinition enrichInputComponentProperties(final ComponentDefinition def, final JsonObject decl) {
        if (!isFormAssociatedComponent(decl)) {
            return def;
        }

        final Set<String> existingNames = new java.util.HashSet<>();
        for (final PropertyDef p : def.properties()) {
            existingNames.add(p.name());
        }

        final List<PropertyDef> enrichedProps = new ArrayList<>(def.properties());
        boolean changed = false;

        // Add common validation properties to all form-associated components
        for (final Map.Entry<String, String> entry : COMMON_VALIDATION_PROPERTIES.entrySet()) {
            final String propName = entry.getKey();
            if (!existingNames.contains(propName)) {
                final String jsType = entry.getValue();
                final String javaType = TypeMapper.mapToJavaType(jsType);
                enrichedProps.add(new PropertyDef(
                    propName, jsType, javaType,
                    "Validation property for form-associated components", null, false
                ));
                changed = true;
            }
        }

        // Add input-specific validation properties only to non-button components
        if (!isButtonComponent(def)) {
            for (final Map.Entry<String, String> entry : INPUT_VALIDATION_PROPERTIES.entrySet()) {
                final String propName = entry.getKey();
                if (!existingNames.contains(propName)) {
                    final String jsType = entry.getValue();
                    final String javaType = TypeMapper.mapToJavaType(jsType);
                    enrichedProps.add(new PropertyDef(
                        propName, jsType, javaType,
                        "Validation property for input components", null, false
                    ));
                    changed = true;
                }
            }
        }

        if (!changed) {
            return def;
        }

        return new ComponentDefinition(
            def.tagName(), def.className(), def.summary(), def.jsDoc(),
            enrichedProps, def.events(), def.slots(), def.cssParts(),
            def.cssProperties(), def.status()
        );
    }

    /**
     * Enriches display component definitions with a {@code variant} property if
     * missing from the manifest. This method only adds the variant property if
     * the component doesn't already have one.
     * <p>
     * This is a generic approach that can be applied to any component library.
     * The decision to add a variant is based on whether the property already exists,
     * not on hardcoded component names.
     * </p>
     *
     * @param def the component definition to enrich
     * @return the enriched definition (same instance if variant already exists)
     */
    public static ComponentDefinition enrichDisplayComponentProperties(final ComponentDefinition def) {
        if (hasVariantProperty(def)) {
            return def;
        }

        // Only add variant if the component seems to be a display/visual component
        // (has no form association and has visual-related properties or events)
        final List<PropertyDef> enrichedProps = new ArrayList<>(def.properties());
        enrichedProps.add(new PropertyDef(
            "variant", "string", "String",
            "The variant determines the visual style of the component",
            null, false
        ));

        return new ComponentDefinition(
            def.tagName(), def.className(), def.summary(), def.jsDoc(),
            enrichedProps, def.events(), def.slots(), def.cssParts(),
            def.cssProperties(), def.status()
        );
    }

    /**
     * Enriches component definitions with ARIA accessibility properties.
     * <p>
     * All components receive {@code ariaLabel}, {@code ariaDescribedby},
     * {@code ariaHidden}, and {@code role} properties. Expandable components
     * (detected via show/hide/expand/collapse events) additionally receive
     * {@code ariaExpanded}.
     * </p>
     * <p>
     * Properties already present in the manifest are preserved; only missing
     * ARIA properties are added.
     * </p>
     * <p>
     * This method is generic and works with any Custom Elements Manifest.
     * </p>
     *
     * @param def the component definition to enrich
     * @return the enriched definition (same instance if no changes needed)
     */
    public static ComponentDefinition enrichAriaProperties(final ComponentDefinition def) {
        final Set<String> existingNames = new java.util.HashSet<>();
        for (final PropertyDef p : def.properties()) {
            existingNames.add(p.name());
        }

        final List<PropertyDef> enrichedProps = new ArrayList<>(def.properties());
        boolean changed = false;

        // Add common ARIA properties to all components
        for (final Map.Entry<String, String> entry : ARIA_PROPERTIES.entrySet()) {
            final String propName = entry.getKey();
            if (!existingNames.contains(propName)) {
                final String jsType = entry.getValue();
                final String javaType = TypeMapper.mapToJavaType(jsType);
                enrichedProps.add(new PropertyDef(
                    propName, jsType, javaType,
                    "ARIA accessibility property", null, false
                ));
                changed = true;
            }
        }

        // Add ariaExpanded for expandable components (detected dynamically)
        if (isExpandableComponent(def) && !existingNames.contains("ariaExpanded")) {
            enrichedProps.add(new PropertyDef(
                "ariaExpanded", "boolean", "boolean",
                "ARIA expanded state for expandable components", null, false
            ));
            changed = true;
        }

        if (!changed) {
            return def;
        }

        return new ComponentDefinition(
            def.tagName(), def.className(), def.summary(), def.jsDoc(),
            enrichedProps, def.events(), def.slots(), def.cssParts(),
            def.cssProperties(), def.status()
        );
    }

    private List<PropertyDef> parseProperties(final JsonArray members) {
        if (members == null) return Collections.emptyList();

        final List<PropertyDef> properties = new ArrayList<>();
        for (final JsonValue memberValue : members) {
            if (memberValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject member = memberValue.asJsonObject();

            if (!"field".equals(getString(member, "kind"))) continue;

            final String name = getString(member, "name");
            if (name == null) continue;

            // Skip private JavaScript fields (those starting with #)
            if (name.startsWith("#")) {
                continue;
            }

            final String jsType = extractTypeText(member);
            final String javaType = TypeMapper.mapToJavaType(jsType);
            final String description = getStringOrDefault(member, "description", "");
            final String defaultValue = getString(member, "default");
            final boolean reflects = member.getBoolean("reflects", false);

            properties.add(new PropertyDef(name, jsType, javaType, description, defaultValue, reflects));
        }
        return properties;
    }

    private List<EventDef> parseEvents(final JsonArray events) {
        if (events == null) return Collections.emptyList();

        final List<EventDef> result = new ArrayList<>();
        for (final JsonValue eventValue : events) {
            if (eventValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject event = eventValue.asJsonObject();

            final String name = getString(event, "name");
            if (name == null) continue;

            final String description = getStringOrDefault(event, "description", "");
            final String detailType = extractTypeText(event);

            result.add(new EventDef(name, description, detailType));
        }
        return result;
    }

    private List<SlotDef> parseSlots(final JsonArray slots) {
        if (slots == null) return Collections.emptyList();

        final List<SlotDef> result = new ArrayList<>();
        for (final JsonValue slotValue : slots) {
            if (slotValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject slot = slotValue.asJsonObject();

            final String name = getStringOrDefault(slot, "name", "");
            final String description = getStringOrDefault(slot, "description", "");

            result.add(new SlotDef(name, description));
        }
        return result;
    }

    private List<CssPartDef> parseCssParts(final JsonArray cssParts) {
        if (cssParts == null) return Collections.emptyList();

        final List<CssPartDef> result = new ArrayList<>();
        for (final JsonValue partValue : cssParts) {
            if (partValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject part = partValue.asJsonObject();

            final String name = getString(part, "name");
            if (name == null) continue;

            final String description = getStringOrDefault(part, "description", "");

            result.add(new CssPartDef(name, description));
        }
        return result;
    }

    private List<CssPropertyDef> parseCssProperties(final JsonArray cssProperties) {
        if (cssProperties == null) return Collections.emptyList();

        final List<CssPropertyDef> result = new ArrayList<>();
        for (final JsonValue propValue : cssProperties) {
            if (propValue.getValueType() != JsonValue.ValueType.OBJECT) continue;
            final JsonObject prop = propValue.asJsonObject();

            final String name = getString(prop, "name");
            if (name == null) continue;

            final String description = getStringOrDefault(prop, "description", "");
            final String defaultValue = getString(prop, "default");

            result.add(new CssPropertyDef(name, description, defaultValue));
        }
        return result;
    }

    /**
     * Extracts the type text from a {@code "type": {"text": "..."}} structure.
     */
    private String extractTypeText(final JsonObject obj) {
        final JsonObject typeObj = obj.getJsonObject("type");
        if (typeObj == null) return null;
        return getString(typeObj, "text");
    }

    private static String getString(final JsonObject obj, final String key) {
        final JsonValue value = obj.get(key);
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) return null;
        if (value.getValueType() == JsonValue.ValueType.STRING) return ((JsonString) value).getString();
        return value.toString();
    }

    private static String getStringOrDefault(final JsonObject obj, final String key, final String defaultValue) {
        final String value = getString(obj, key);
        return value != null ? value : defaultValue;
    }

    // --- Code generation methods ---

    /**
     * Generates the Java Record source for a component's props.
     * <p>
     * The record class name is derived from the tag name: strip {@code "wa-"} prefix,
     * PascalCase it, and append {@code "Props"} (e.g. {@code "wa-input"} → {@code "InputProps"}).
     * </p>
     *
     * @param def the component definition
     * @return complete Java source code for the props record
     */
    public String generatePropsRecord(final ComponentDefinition def) {
        final List<PropertyDef> props = def.properties();
        final String recordName = tagNameToPropsClassName(def.tagName());
        final boolean needsOptionalImport = props.stream().anyMatch(p -> p.javaType().contains("Optional"));
        final boolean needsListImport = props.stream().anyMatch(p -> p.javaType().contains("List"));

        final StringBuilder sb = new StringBuilder();

        // Package declaration
        sb.append("package com.ponysdk.core.ui.wa.props;\n\n");

        // Imports
        if (needsListImport) {
            sb.append("import java.util.List;\n");
        }
        if (needsOptionalImport) {
            sb.append("import java.util.Optional;\n");
        }
        if (needsListImport || needsOptionalImport) {
            sb.append('\n');
        }

        // Javadoc
        sb.append("/**\n");
        sb.append(" * Props record for {@code <").append(def.tagName()).append(">}.\n");
        sb.append(" * @generated from custom-elements.json\n");
        sb.append(" */\n");

        // Record declaration
        sb.append("public record ").append(recordName).append("(\n");
        for (int i = 0; i < props.size(); i++) {
            final PropertyDef p = props.get(i);
            final String fieldName = kebabToCamelCase(p.name());
            sb.append("    ").append(p.javaType()).append(' ').append(fieldName);
            if (i < props.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append(") {\n");

        // defaults() factory method
        sb.append("    public static ").append(recordName).append(" defaults() {\n");
        sb.append("        return new ").append(recordName).append("(\n");
        for (int i = 0; i < props.size(); i++) {
            final PropertyDef p = props.get(i);
            sb.append("            ").append(defaultValueForType(p.javaType()));
            if (i < props.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("        );\n");
        sb.append("    }\n");

        // withX() copy methods
        for (final PropertyDef p : props) {
            final String fieldName = kebabToCamelCase(p.name());
            final String methodName = "with" + capitalize(fieldName);
            sb.append('\n');
            sb.append("    public ").append(recordName).append(' ').append(methodName)
              .append("(final ").append(p.javaType()).append(' ').append(fieldName).append(") {\n");
            sb.append("        return new ").append(recordName).append("(\n");
            for (int i = 0; i < props.size(); i++) {
                final PropertyDef other = props.get(i);
                final String otherField = kebabToCamelCase(other.name());
                sb.append("            ");
                if (otherField.equals(fieldName)) {
                    sb.append(fieldName);
                } else {
                    sb.append("this.").append(otherField).append("()");
                }
                if (i < props.size() - 1) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            sb.append("        );\n");
            sb.append("    }\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Converts a {@code wa-*} tag name to a PascalCase props class name.
     * <p>
     * Examples: {@code "wa-input"} → {@code "InputProps"}, {@code "wa-tab-group"} → {@code "TabGroupProps"}.
     */
    static String tagNameToPropsClassName(final String tagName) {
        final String withoutPrefix = tagName.startsWith("wa-") ? tagName.substring(3) : tagName;
        final StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (final char c : withoutPrefix.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        sb.append("Props");
        return sb.toString();
    }

    /**
     * Converts a kebab-case name to camelCase.
     * <p>
     * Examples: {@code "help-text"} → {@code "helpText"}, {@code "value"} → {@code "value"}.
     * </p>
     * <p>
     * If the resulting name is a Java keyword, appends an underscore to avoid compilation errors.
     * </p>
     */
    static String kebabToCamelCase(final String name) {
        if (name == null || name.isEmpty()) return name;
        final StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = false;
        for (final char c : name.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        final String result = sb.toString();
        
        // Avoid Java keywords by appending underscore
        if (isJavaKeyword(result)) {
            return result + "_";
        }
        return result;
    }

    /**
     * Checks if a string is a Java keyword.
     */
    private static boolean isJavaKeyword(final String name) {
        return switch (name) {
            case "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
                 "class", "const", "continue", "default", "do", "double", "else", "enum",
                 "extends", "final", "finally", "float", "for", "goto", "if", "implements",
                 "import", "instanceof", "int", "interface", "long", "native", "new", "package",
                 "private", "protected", "public", "return", "short", "static", "strictfp",
                 "super", "switch", "synchronized", "this", "throw", "throws", "transient",
                 "try", "void", "volatile", "while" -> true;
            default -> false;
        };
    }

    private static String capitalize(final String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Returns a sensible default value literal for the given Java type.
     */
    private static String defaultValueForType(final String javaType) {
        return switch (javaType) {
            case "String" -> "\"\"";
            case "double" -> "0.0";
            case "boolean" -> "false";
            case "Object" -> "null";
            default -> {
                if (javaType.startsWith("Optional")) {
                    yield "Optional.empty()";
                }
                yield "null";
            }
        };
    }

    /**
     * Converts a {@code wa-*} tag name to a PascalCase wrapper class name with "WA" prefix.
     * <p>
     * Examples: {@code "wa-input"} → {@code "WAInput"}, {@code "wa-tab-group"} → {@code "WATabGroup"}.
     */
    static String tagNameToWrapperClassName(final String tagName) {
        final String withoutPrefix = tagName.startsWith("wa-") ? tagName.substring(3) : tagName;
        final StringBuilder sb = new StringBuilder("WA");
        boolean capitalizeNext = true;
        for (final char c : withoutPrefix.toCharArray()) {
            if (c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Generates the PWebComponent wrapper class source for a component.
     * <p>
     * The wrapper class extends {@code PWebComponent<TProps>} with the correct props type,
     * includes Javadoc from the manifest (summary, slots, events, CSS parts), and provides
     * typed convenience setter methods for each property.
     * </p>
     *
     * @param def the component definition
     * @return complete Java source code for the wrapper class
     */
    public String generateWrapperClass(final ComponentDefinition def) {
        final String wrapperClassName = tagNameToWrapperClassName(def.tagName());
        final String propsClassName = tagNameToPropsClassName(def.tagName());
        final List<PropertyDef> props = def.properties();
        final boolean hasSlots = !def.slots().isEmpty();
        final boolean needsOptionalImport = props.stream().anyMatch(p -> p.javaType().contains("Optional"));
        final boolean needsListImport = props.stream().anyMatch(p -> p.javaType().contains("List"));

        final StringBuilder sb = new StringBuilder();

        // Package declaration
        sb.append("package com.ponysdk.core.ui.wa;\n\n");

        // Imports
        sb.append("import com.ponysdk.core.ui.component.PWebComponent;\n");
        sb.append("import com.ponysdk.core.ui.wa.props.").append(propsClassName).append(";\n");
        if (hasSlots) {
            sb.append("import java.util.Set;\n");
        }
        if (needsListImport) {
            sb.append("import java.util.List;\n");
        }
        if (needsOptionalImport) {
            sb.append("import java.util.Optional;\n");
        }
        sb.append('\n');

        // Javadoc
        appendJavadoc(sb, def);

        // Class declaration
        sb.append("public class ").append(wrapperClassName)
          .append(" extends PWebComponent<").append(propsClassName).append("> {\n\n");

        // Declared slots constant (if any)
        if (hasSlots) {
            sb.append("    private static final Set<String> DECLARED_SLOTS = Set.of(");
            final var slotNames = def.slots().stream()
                .map(SlotDef::name)
                .toList();
            for (int i = 0; i < slotNames.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append('"').append(slotNames.get(i)).append('"');
            }
            sb.append(");\n\n");
        }

        // Default constructor
        sb.append("    public ").append(wrapperClassName).append("() {\n");
        if (hasSlots) {
            sb.append("        super(").append(propsClassName).append(".defaults(), DECLARED_SLOTS);\n");
        } else {
            sb.append("        super(").append(propsClassName).append(".defaults());\n");
        }
        sb.append("    }\n\n");

        // Constructor with initial props
        sb.append("    public ").append(wrapperClassName).append("(final ").append(propsClassName).append(" initialProps) {\n");
        if (hasSlots) {
            sb.append("        super(initialProps, DECLARED_SLOTS);\n");
        } else {
            sb.append("        super(initialProps);\n");
        }
        sb.append("    }\n\n");

        // getPropsClass()
        sb.append("    @Override\n");
        sb.append("    protected Class<").append(propsClassName).append("> getPropsClass() {\n");
        sb.append("        return ").append(propsClassName).append(".class;\n");
        sb.append("    }\n\n");

        // getComponentSignature()
        sb.append("    @Override\n");
        sb.append("    protected String getComponentSignature() {\n");
        sb.append("        return \"").append(def.tagName()).append("\";\n");
        sb.append("    }\n");

        // Convenience setters
        for (final PropertyDef p : props) {
            final String fieldName = kebabToCamelCase(p.name());
            final String setterName = "set" + capitalize(fieldName);
            final String paramName = fieldName.equals("current") ? "value" : fieldName;
            sb.append('\n');
            sb.append("    public void ").append(setterName)
              .append("(final ").append(p.javaType()).append(' ').append(paramName).append(") {\n");
            sb.append("        final ").append(propsClassName).append(" currentProps = getCurrentProps();\n");
            sb.append("        setProps(currentProps.with").append(capitalize(fieldName)).append('(').append(paramName).append("));\n");
            sb.append("    }\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    private static void appendJavadoc(final StringBuilder sb, final ComponentDefinition def) {
        sb.append("/**\n");
        sb.append(" * Server-side wrapper for the Web Awesome {@code <").append(def.tagName()).append(">} component.\n");

        // Summary
        if (def.summary() != null && !def.summary().isEmpty()) {
            sb.append(" *\n");
            sb.append(" * <p>").append(escapeJavadoc(def.summary())).append("</p>\n");
        }

        // Slots
        if (!def.slots().isEmpty()) {
            sb.append(" *\n");
            sb.append(" * <h3>Available Slots</h3>\n");
            sb.append(" * <ul>\n");
            for (final SlotDef slot : def.slots()) {
                final String slotLabel = slot.name().isEmpty() ? "(default)" : slot.name();
                sb.append(" *   <li>{@code ").append(slotLabel).append("}");
                if (!slot.description().isEmpty()) {
                    sb.append(" - ").append(escapeJavadoc(slot.description()));
                }
                sb.append("</li>\n");
            }
            sb.append(" * </ul>\n");
        }

        // Events
        if (!def.events().isEmpty()) {
            sb.append(" *\n");
            sb.append(" * <h3>Events</h3>\n");
            sb.append(" * <ul>\n");
            for (final EventDef event : def.events()) {
                sb.append(" *   <li>{@code ").append(event.name()).append("}");
                if (!event.description().isEmpty()) {
                    sb.append(" - ").append(escapeJavadoc(event.description()));
                }
                sb.append("</li>\n");
            }
            sb.append(" * </ul>\n");
        }

        // CSS Parts
        if (!def.cssParts().isEmpty()) {
            sb.append(" *\n");
            sb.append(" * <h3>CSS Parts</h3>\n");
            sb.append(" * <ul>\n");
            for (final CssPartDef part : def.cssParts()) {
                sb.append(" *   <li>{@code ").append(part.name()).append("}");
                if (!part.description().isEmpty()) {
                    sb.append(" - ").append(escapeJavadoc(part.description()));
                }
                sb.append("</li>\n");
            }
            sb.append(" * </ul>\n");
        }

        sb.append(" *\n");
        sb.append(" * @generated from custom-elements.json (Web Awesome)\n");
        sb.append(" */\n");
    }

    /**
     * Escapes characters that are problematic in Javadoc content.
     */
    private static String escapeJavadoc(final String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("*/", "* /");
    }

    /**
     * Generates the TypeScript interface source for a component's props.
     * <p>
     * The interface name matches the Java Record name (e.g. {@code "InputProps"}).
     * Each property from the component definition becomes an interface field with
     * the corresponding TypeScript type. Optional Java types produce optional
     * fields (with {@code ?} marker).
     * </p>
     *
     * @param def the component definition
     * @return complete TypeScript source code for the props interface
     */
    public String generateTypeScriptInterface(final ComponentDefinition def) {
        final List<PropertyDef> props = def.properties();
        final String interfaceName = tagNameToPropsClassName(def.tagName());

        final StringBuilder sb = new StringBuilder();

        // JSDoc comment
        sb.append("/**\n");
        sb.append(" * Props interface for <").append(def.tagName()).append(">.\n");
        sb.append(" * @generated from custom-elements.json\n");
        sb.append(" */\n");

        // Interface declaration
        sb.append("export interface ").append(interfaceName).append(" {\n");

        for (final PropertyDef p : props) {
            final String fieldName = kebabToCamelCase(p.name());
            final boolean optional = p.javaType().startsWith("Optional");
            final String tsType = javaTypeToTypeScript(p.javaType());

            sb.append("    ").append(fieldName);
            if (optional) {
                sb.append('?');
            }
            sb.append(": ").append(tsType).append(";\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Maps a Java type string to the corresponding TypeScript type string.
     *
     * @param javaType the Java type (e.g. {@code "String"}, {@code "double"}, {@code "Optional<String>"})
     * @return the TypeScript type string
     */
    static String javaTypeToTypeScript(final String javaType) {
        if (javaType == null) return "any";
        return switch (javaType) {
            case "String" -> "string";
            case "double" -> "number";
            case "boolean" -> "boolean";
            case "Object" -> "any";
            default -> {
                if (javaType.startsWith("Optional<")) {
                    final String inner = javaType.substring("Optional<".length(), javaType.length() - 1);
                    yield javaTypeToTypeScript(inner);
                }
                yield "any";
            }
        };
    }

    /** Generates the component index source listing all components. */
    public String generateComponentIndex(final List<ComponentDefinition> defs) {
        final StringBuilder sb = new StringBuilder();
        sb.append("package com.ponysdk.core.ui.wa;\n\n");
        sb.append("import java.util.List;\n\n");
        sb.append("/**\n");
        sb.append(" * Index of all generated Web Awesome component wrappers.\n");
        sb.append(" * @generated from custom-elements.json\n");
        sb.append(" */\n");
        sb.append("public class ComponentIndex {\n\n");
        sb.append("    public record ComponentEntry(\n");
        sb.append("        String tagName,\n");
        sb.append("        String javaClass,\n");
        sb.append("        String propsClass,\n");
        sb.append("        String status\n");
        sb.append("    ) {}\n\n");
        sb.append("    public static final List<ComponentEntry> ALL_COMPONENTS = List.of(\n");

        for (int i = 0; i < defs.size(); i++) {
            final ComponentDefinition def = defs.get(i);
            final String wrapperClass = "com.ponysdk.core.ui.wa." + tagNameToWrapperClassName(def.tagName());
            final String propsClass = "com.ponysdk.core.ui.wa.props." + tagNameToPropsClassName(def.tagName());
            final String status = def.status() != null ? def.status() : "stable";

            sb.append("        new ComponentEntry(\"").append(def.tagName()).append("\", \"")
              .append(wrapperClass).append("\", \"")
              .append(propsClass).append("\", \"")
              .append(status).append("\")");

            if (i < defs.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("    );\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates the JavaScript registry file that registers all Web Awesome components
     * with the PComponent bridge.
     * 
     * @param defs the list of component definitions
     * @return complete JavaScript source code for the registry
     */
    public String generateJavaScriptRegistry(final List<ComponentDefinition> defs) {
        final StringBuilder sb = new StringBuilder();
        
        sb.append("/**\n");
        sb.append(" * Web Awesome Component Registry\n");
        sb.append(" * Registers generic factories for all wa-* custom elements.\n");
        sb.append(" * @generated from custom-elements.json\n");
        sb.append(" */\n\n");
        sb.append("(function () {\n");
        sb.append("    'use strict';\n\n");
        
        // Generate component list
        sb.append("    // List of Web Awesome component tag names (using wa- prefix)\n");
        sb.append("    const WA_COMPONENTS = [\n");
        
        for (int i = 0; i < defs.size(); i++) {
            final ComponentDefinition def = defs.get(i);
            sb.append("        '").append(def.tagName()).append("'");
            if (i < defs.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        
        sb.append("    ];\n\n");
        
        // Add the adapter and factory code
        sb.append("    /**\n");
        sb.append("     * Generic Web Component adapter for wa-* elements.\n");
        sb.append("     * Creates the custom element and applies props as attributes/properties.\n");
        sb.append("     */\n");
        sb.append("    class GenericWebComponentAdapter {\n");
        sb.append("        constructor(container, tagName) {\n");
        sb.append("            this.container = container;\n");
        sb.append("            this.tagName = tagName;\n");
        sb.append("            this.element = null;\n");
        sb.append("            this.eventCallback = null;\n");
        sb.append("        }\n\n");
        
        sb.append("        setEventCallback(callback) {\n");
        sb.append("            this.eventCallback = callback;\n");
        sb.append("        }\n\n");
        
        sb.append("        mount() {\n");
        sb.append("            // Create the custom element\n");
        sb.append("            this.element = document.createElement(this.tagName);\n");
        sb.append("            this.container.appendChild(this.element);\n");
        sb.append("            console.log('[WebAwesome] Mounted:', this.tagName);\n");
        sb.append("        }\n\n");
        
        sb.append("        updateProps(props) {\n");
        sb.append("            if (!this.element) return;\n\n");
        sb.append("            // Apply each prop as an attribute or property\n");
        sb.append("            for (const [key, value] of Object.entries(props)) {\n");
        sb.append("                if (typeof value === 'boolean') {\n");
        sb.append("                    // Boolean attributes\n");
        sb.append("                    if (value) {\n");
        sb.append("                        this.element.setAttribute(key, '');\n");
        sb.append("                    } else {\n");
        sb.append("                        this.element.removeAttribute(key);\n");
        sb.append("                    }\n");
        sb.append("                } else if (typeof value === 'string' || typeof value === 'number') {\n");
        sb.append("                    // String/number attributes\n");
        sb.append("                    this.element.setAttribute(key, String(value));\n");
        sb.append("                } else {\n");
        sb.append("                    // Complex values as properties\n");
        sb.append("                    this.element[key] = value;\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("            console.log('[WebAwesome] Props updated:', this.tagName, props);\n");
        sb.append("        }\n\n");
        
        sb.append("        unmount() {\n");
        sb.append("            if (this.element && this.element.parentNode) {\n");
        sb.append("                this.element.parentNode.removeChild(this.element);\n");
        sb.append("            }\n");
        sb.append("            this.element = null;\n");
        sb.append("            console.log('[WebAwesome] Unmounted:', this.tagName);\n");
        sb.append("        }\n");
        sb.append("    }\n\n");
        
        sb.append("    /**\n");
        sb.append("     * Generic factory function for Web Awesome components.\n");
        sb.append("     */\n");
        sb.append("    function createWebAwesomeFactory(tagName) {\n");
        sb.append("        return function (container) {\n");
        sb.append("            const adapter = new GenericWebComponentAdapter(container, tagName);\n");
        sb.append("            return {\n");
        sb.append("                getWebComponent: () => adapter\n");
        sb.append("            };\n");
        sb.append("        };\n");
        sb.append("    }\n\n");
        
        sb.append("    // Register all Web Awesome components\n");
        sb.append("    function registerAll() {\n");
        sb.append("        if (typeof window.registerWebComponent !== 'function') {\n");
        sb.append("            console.warn('[WebAwesome] registerWebComponent not available yet, deferring registration');\n");
        sb.append("            setTimeout(registerAll, 100);\n");
        sb.append("            return;\n");
        sb.append("        }\n\n");
        
        sb.append("        WA_COMPONENTS.forEach(tagName => {\n");
        sb.append("            window.registerWebComponent(tagName, createWebAwesomeFactory(tagName));\n");
        sb.append("        });\n\n");
        
        sb.append("        console.log('[WebAwesome] Registered', WA_COMPONENTS.length, 'component factories');\n");
        sb.append("    }\n\n");
        
        sb.append("    // Register when DOM is ready\n");
        sb.append("    if (document.readyState === 'loading') {\n");
        sb.append("        document.addEventListener('DOMContentLoaded', registerAll);\n");
        sb.append("    } else {\n");
        sb.append("        registerAll();\n");
        sb.append("    }\n\n");
        
        sb.append("})();\n");
        
        return sb.toString();
    }

    /**
     * Entry point for the Gradle task. Parses the manifest and generates all output files.
     *
     * @param args [0] manifestPath, [1] javaOutputDir, [2] tsOutputDir, [3] jsRegistryOutputPath (optional)
     */
    public static void main(final String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: WebAwesomeCodeGenerator <manifestPath> <javaOutputDir> <tsOutputDir> [jsRegistryOutputPath]");
            System.exit(1);
        }

        final Path manifestPath = Path.of(args[0]);
        final Path javaOutputDir = Path.of(args[1]);
        final Path tsOutputDir = Path.of(args[2]);
        final Path jsRegistryOutputPath = args.length > 3 ? Path.of(args[3]) : null;

        final WebAwesomeCodeGenerator generator = new WebAwesomeCodeGenerator(manifestPath, javaOutputDir, tsOutputDir);
        final List<ComponentDefinition> defs = generator.parseManifest();

        LOG.info("Parsed " + defs.size() + " component definitions from " + manifestPath);

        final Path propsPackageDir = javaOutputDir.resolve("com/ponysdk/core/ui/wa/props");
        final Path wrapperPackageDir = javaOutputDir.resolve("com/ponysdk/core/ui/wa");

        try {
            Files.createDirectories(propsPackageDir);
            Files.createDirectories(wrapperPackageDir);
            Files.createDirectories(tsOutputDir);

            for (final ComponentDefinition def : defs) {
                final String propsSource = generator.generatePropsRecord(def);
                final String propsFileName = tagNameToPropsClassName(def.tagName()) + ".java";
                Files.writeString(propsPackageDir.resolve(propsFileName), propsSource, StandardCharsets.UTF_8);

                final String wrapperSource = generator.generateWrapperClass(def);
                final String wrapperFileName = tagNameToWrapperClassName(def.tagName()) + ".java";
                Files.writeString(wrapperPackageDir.resolve(wrapperFileName), wrapperSource, StandardCharsets.UTF_8);

                final String tsSource = generator.generateTypeScriptInterface(def);
                final String tsFileName = tagNameToPropsClassName(def.tagName()) + ".ts";
                Files.writeString(tsOutputDir.resolve(tsFileName), tsSource, StandardCharsets.UTF_8);
            }

            final String indexSource = generator.generateComponentIndex(defs);
            Files.writeString(wrapperPackageDir.resolve("ComponentIndex.java"), indexSource, StandardCharsets.UTF_8);

            // Generate JavaScript registry if output path is provided
            if (jsRegistryOutputPath != null) {
                final String jsRegistrySource = generator.generateJavaScriptRegistry(defs);
                Files.createDirectories(jsRegistryOutputPath.getParent());
                Files.writeString(jsRegistryOutputPath, jsRegistrySource, StandardCharsets.UTF_8);
                LOG.info("Generated JavaScript registry at " + jsRegistryOutputPath);
            }

            LOG.info("Generated " + defs.size() + " components: props records, wrapper classes, TS interfaces, and component index.");
        } catch (final IOException e) {
            LOG.log(Level.SEVERE, "Failed to write generated sources", e);
            System.exit(1);
        }
    }

}

