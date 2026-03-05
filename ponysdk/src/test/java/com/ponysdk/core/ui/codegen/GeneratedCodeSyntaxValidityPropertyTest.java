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

package com.ponysdk.core.ui.codegen;

import com.ponysdk.core.ui.codegen.generator.CodeGenerator;
import com.ponysdk.core.ui.codegen.generator.CodeGeneratorImpl;
import com.ponysdk.core.ui.codegen.model.*;
import com.ponysdk.core.ui.codegen.validation.CodeValidator;
import net.jqwik.api.*;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for generated code syntax validity.
 * <p>
 * Feature: generic-webcomponent-wrapper, Property 13: Generated Code Syntax Validity
 * </p>
 * <p>
 * **Validates: Requirements 16.1, 16.4**
 * </p>
 * <p>
 * For any component definition, the generated Java code should be syntactically valid
 * and compile without errors.
 * </p>
 */
public class GeneratedCodeSyntaxValidityPropertyTest {

    private final CodeGenerator generator = new CodeGeneratorImpl("com.ponysdk.core.ui.test");
    private final CodeValidator validator = new CodeValidator();

    /**
     * Property 13: Generated Code Syntax Validity - Wrapper Class
     * <p>
     * **Validates: Requirements 16.1, 16.4**
     * </p>
     * <p>
     * For any component definition, the generated wrapper class should be syntactically
     * valid Java code that compiles without errors.
     * </p>
     */
    @Property(tries = 100)
    @Label("Feature: generic-webcomponent-wrapper, Property 13: Generated Code Syntax Validity - Wrapper Class")
    void generatedWrapperClassSyntaxValidity(@ForAll("componentDefinition") final ComponentDefinition def) {
        // Generate wrapper class
        final String wrapperCode = generator.generateWrapperClass(def);

        // Validate syntax
        final CodeValidator.ValidationResult result = validator.validateJava(
            "com.ponysdk.core.ui.test." + def.getWrapperClassName(),
            wrapperCode
        );

        // Assert valid
        assertTrue(result.valid(),
            () -> "Generated wrapper class should be syntactically valid. Errors: " +
                formatErrors(result.errors()));
    }

    /**
     * Property 13: Generated Code Syntax Validity - Props Record
     * <p>
     * **Validates: Requirements 16.1, 16.4**
     * </p>
     * <p>
     * For any component definition, the generated props record should be syntactically
     * valid Java code that compiles without errors.
     * </p>
     */
    @Property(tries = 100)
    @Label("Feature: generic-webcomponent-wrapper, Property 13: Generated Code Syntax Validity - Props Record")
    void generatedPropsRecordSyntaxValidity(@ForAll("componentDefinition") final ComponentDefinition def) {
        // Generate props record
        final String propsCode = generator.generatePropsRecord(def);

        // Validate syntax
        final CodeValidator.ValidationResult result = validator.validateJava(
            "com.ponysdk.core.ui.test." + def.getPropsClassName(),
            propsCode
        );

        // Assert valid
        assertTrue(result.valid(),
            () -> "Generated props record should be syntactically valid. Errors: " +
                formatErrors(result.errors()));
    }

    /**
     * Property 13: Generated Code Syntax Validity - TypeScript Interface
     * <p>
     * **Validates: Requirements 16.1, 16.4**
     * </p>
     * <p>
     * For any component definition, the generated TypeScript interface should be
     * syntactically valid TypeScript code.
     * </p>
     */
    @Property(tries = 100)
    @Label("Feature: generic-webcomponent-wrapper, Property 13: Generated Code Syntax Validity - TypeScript Interface")
    void generatedTypeScriptInterfaceSyntaxValidity(@ForAll("componentDefinition") final ComponentDefinition def) {
        // Generate TypeScript interface
        final String tsCode = generator.generateTypeScriptInterface(def);

        // Validate syntax (basic validation)
        final CodeValidator.ValidationResult result = validator.validateTypeScript(tsCode);

        // Assert valid
        assertTrue(result.valid(),
            () -> "Generated TypeScript interface should be syntactically valid. Errors: " +
                formatErrors(result.errors()));
    }

    /**
     * Property 13: Generated Code Syntax Validity - Complex Component
     * <p>
     * **Validates: Requirements 16.1, 16.4**
     * </p>
     * <p>
     * For any component definition with properties, events, slots, and methods,
     * all generated code should be syntactically valid.
     * </p>
     */
    @Property(tries = 100)
    @Label("Feature: generic-webcomponent-wrapper, Property 13: Generated Code Syntax Validity - Complex Component")
    void generatedComplexComponentSyntaxValidity(@ForAll("complexComponentDefinition") final ComponentDefinition def) {
        // Generate all code
        final String wrapperCode = generator.generateWrapperClass(def);
        final String propsCode = generator.generatePropsRecord(def);
        final String tsCode = generator.generateTypeScriptInterface(def);

        // Validate wrapper class
        final CodeValidator.ValidationResult wrapperResult = validator.validateJava(
            "com.ponysdk.core.ui.test." + def.getWrapperClassName(),
            wrapperCode
        );
        assertTrue(wrapperResult.valid(),
            () -> "Generated wrapper class should be syntactically valid. Errors: " +
                formatErrors(wrapperResult.errors()));

        // Validate props record
        final CodeValidator.ValidationResult propsResult = validator.validateJava(
            "com.ponysdk.core.ui.test." + def.getPropsClassName(),
            propsCode
        );
        assertTrue(propsResult.valid(),
            () -> "Generated props record should be syntactically valid. Errors: " +
                formatErrors(propsResult.errors()));

        // Validate TypeScript interface
        final CodeValidator.ValidationResult tsResult = validator.validateTypeScript(tsCode);
        assertTrue(tsResult.valid(),
            () -> "Generated TypeScript interface should be syntactically valid. Errors: " +
                formatErrors(tsResult.errors()));
    }

    // ========== Generators ==========

    /**
     * Generates random component definitions with varying complexity.
     */
    @Provide
    Arbitrary<ComponentDefinition> componentDefinition() {
        return Combinators.combine(
            tagNames(),
            Arbitraries.integers().between(0, 5),
            Arbitraries.integers().between(0, 3),
            Arbitraries.integers().between(0, 3),
            Arbitraries.integers().between(0, 3)
        ).as((tagName, propCount, eventCount, slotCount, methodCount) ->
            createComponentDefinition(tagName, propCount, eventCount, slotCount, methodCount)
        );
    }

    /**
     * Generates complex component definitions with all features.
     */
    @Provide
    Arbitrary<ComponentDefinition> complexComponentDefinition() {
        return Combinators.combine(
            tagNames(),
            Arbitraries.integers().between(3, 10),
            Arbitraries.integers().between(2, 5),
            Arbitraries.integers().between(1, 4),
            Arbitraries.integers().between(2, 5)
        ).as((tagName, propCount, eventCount, slotCount, methodCount) ->
            createComponentDefinition(tagName, propCount, eventCount, slotCount, methodCount)
        );
    }

    /**
     * Generates valid web component tag names.
     */
    @Provide
    Arbitrary<String> tagNames() {
        return Arbitraries.of(
            "wa-button", "wa-input", "wa-dialog", "wa-card", "wa-tab-group",
            "wa-select", "wa-checkbox", "wa-radio", "wa-switch", "wa-textarea",
            "sl-button", "sl-input", "sl-dialog", "sl-card", "sl-tab-group"
        );
    }

    /**
     * Generates property definitions.
     */
    @Provide
    Arbitrary<PropertyDef> propertyDefinition() {
        return Combinators.combine(
            Arbitraries.of("variant", "size", "disabled", "loading", "value", "label", "placeholder"),
            Arbitraries.of("string", "boolean", "number", "string | undefined"),
            Arbitraries.of(true, false),
            Arbitraries.of("public", "protected")
        ).as((name, cemType, required, privacy) -> {
            final String javaType = mapCemTypeToJava(cemType);
            final String tsType = cemType;
            final String defaultValue = getDefaultValue(cemType);
            return new PropertyDef(name, cemType, javaType, tsType,
                "Property: " + name, defaultValue, required, privacy);
        });
    }

    /**
     * Generates event definitions.
     */
    @Provide
    Arbitrary<EventDef> eventDefinition() {
        return Combinators.combine(
            Arbitraries.of("click", "focus", "blur", "change", "input"),
            Arbitraries.of(true, false),
            Arbitraries.of(true, false)
        ).as((eventName, bubbles, cancelable) -> {
            final String fullEventName = "wa-" + eventName;
            return new EventDef(fullEventName, "Emitted on " + eventName,
                null, bubbles, cancelable);
        });
    }

    /**
     * Generates slot definitions.
     */
    @Provide
    Arbitrary<SlotDef> slotDefinition() {
        return Arbitraries.of("prefix", "suffix", "header", "footer", "")
            .map(slotName -> new SlotDef(slotName, "Slot: " + slotName));
    }

    /**
     * Generates method definitions.
     */
    @Provide
    Arbitrary<MethodDef> methodDefinition() {
        return Combinators.combine(
            Arbitraries.of("focus", "blur", "show", "hide", "checkValidity"),
            Arbitraries.of(true, false)
        ).as((methodName, async) -> {
            final String returnType = async ? "Promise<void>" : "void";
            return new MethodDef(methodName, "Method: " + methodName,
                Collections.emptyList(), returnType, async);
        });
    }

    // ========== Helper Methods ==========

    /**
     * Creates a component definition with specified counts of features.
     */
    private ComponentDefinition createComponentDefinition(
        final String tagName,
        final int propCount,
        final int eventCount,
        final int slotCount,
        final int methodCount
    ) {
        final List<PropertyDef> properties = generateProperties(propCount);
        final List<EventDef> events = generateEvents(eventCount);
        final List<SlotDef> slots = generateSlots(slotCount);
        final List<MethodDef> methods = generateMethods(methodCount);

        return new ComponentDefinition(
            tagName,
            toClassName(tagName),
            "A test component",
            "Test component for property-based testing",
            properties,
            events,
            slots,
            methods,
            Collections.emptyList(),
            "stable"
        );
    }

    /**
     * Generates a list of property definitions.
     */
    private List<PropertyDef> generateProperties(final int count) {
        final String[] names = {"variant", "size", "disabled", "loading", "value", "label", "placeholder", "required", "readonly", "autofocus"};
        final String[] types = {"string", "boolean", "number", "string | undefined", "boolean"};

        return java.util.stream.IntStream.range(0, Math.min(count, names.length))
            .mapToObj(i -> {
                final String name = names[i];
                final String cemType = types[i % types.length];
                final String javaType = mapCemTypeToJava(cemType);
                final boolean required = i % 3 == 0;
                return new PropertyDef(name, cemType, javaType, cemType,
                    "Property: " + name, getDefaultValue(cemType), required, "public");
            })
            .toList();
    }

    /**
     * Generates a list of event definitions.
     */
    private List<EventDef> generateEvents(final int count) {
        final String[] eventNames = {"click", "focus", "blur", "change", "input"};

        return java.util.stream.IntStream.range(0, Math.min(count, eventNames.length))
            .mapToObj(i -> {
                final String eventName = "wa-" + eventNames[i];
                return new EventDef(eventName, "Emitted on " + eventNames[i],
                    null, true, false);
            })
            .toList();
    }

    /**
     * Generates a list of slot definitions.
     */
    private List<SlotDef> generateSlots(final int count) {
        final String[] slotNames = {"prefix", "suffix", "header", "footer"};

        return java.util.stream.IntStream.range(0, Math.min(count, slotNames.length))
            .mapToObj(i -> new SlotDef(slotNames[i], "Slot: " + slotNames[i]))
            .toList();
    }

    /**
     * Generates a list of method definitions.
     */
    private List<MethodDef> generateMethods(final int count) {
        final String[] methodNames = {"focus", "blur", "show", "hide", "checkValidity"};

        return java.util.stream.IntStream.range(0, Math.min(count, methodNames.length))
            .mapToObj(i -> {
                final boolean async = i % 2 == 0;
                final String returnType = async ? "Promise<void>" : "void";
                return new MethodDef(methodNames[i], "Method: " + methodNames[i],
                    Collections.emptyList(), returnType, async);
            })
            .toList();
    }

    /**
     * Maps CEM type to Java type.
     */
    private String mapCemTypeToJava(final String cemType) {
        return switch (cemType) {
            case "string" -> "String";
            case "boolean" -> "boolean";
            case "number" -> "double";
            case "string | undefined" -> "Optional<String>";
            default -> "String";
        };
    }

    /**
     * Gets default value for a CEM type.
     */
    private String getDefaultValue(final String cemType) {
        return switch (cemType) {
            case "string", "string | undefined" -> "\"\"";
            case "boolean" -> "false";
            case "number" -> "0.0";
            default -> null;
        };
    }

    /**
     * Converts tag name to class name.
     */
    private String toClassName(final String tagName) {
        final String[] parts = tagName.split("-");
        final StringBuilder className = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            className.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                className.append(parts[i].substring(1));
            }
        }
        return className.toString();
    }

    /**
     * Formats validation errors for display.
     */
    private String formatErrors(final List<CodeValidator.ValidationError> errors) {
        if (errors.isEmpty()) {
            return "No errors";
        }

        final StringBuilder sb = new StringBuilder("\n");
        for (final CodeValidator.ValidationError error : errors) {
            sb.append("  Line ").append(error.lineNumber())
              .append(", Column ").append(error.columnNumber())
              .append(": ").append(error.message())
              .append("\n");
        }
        return sb.toString();
    }
}
