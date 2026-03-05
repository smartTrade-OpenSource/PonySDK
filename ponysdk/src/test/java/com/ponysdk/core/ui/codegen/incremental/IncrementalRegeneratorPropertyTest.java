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

package com.ponysdk.core.ui.codegen.incremental;

import com.ponysdk.core.ui.codegen.generator.CodeGenerator;
import com.ponysdk.core.ui.codegen.generator.CodeGeneratorImpl;
import com.ponysdk.core.ui.codegen.model.*;
import net.jqwik.api.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for incremental regeneration preservation.
 * <p>
 * Feature: generic-webcomponent-wrapper
 * </p>
 * <p>
 * <b>Validates: Requirements 11.2, 11.3</b>
 * </p>
 */
public class IncrementalRegeneratorPropertyTest {

    // ========== Property 11: Incremental Regeneration Preservation ==========

    /**
     * Property 11: Incremental Regeneration Preservation
     * <p>
     * <b>Validates: Requirements 11.2, 11.3</b>
     * </p>
     * <p>
     * For any wrapper class with manual code outside generated regions,
     * regenerating the class should preserve all manual code while updating
     * only the generated regions.
     * </p>
     */
    @Property(tries = 100)
    @Label("Feature: generic-webcomponent-wrapper, Property 11: Incremental Regeneration Preservation")
    void incrementalRegenerationPreservesManualCode(
            @ForAll("componentDefinition") ComponentDefinition initialDef,
            @ForAll("manualCodeSnippets") List<String> manualCodeSnippets,
            @ForAll("componentDefinition") ComponentDefinition updatedDef
    ) {
        // Arrange: Generate initial wrapper
        final CodeGenerator generator = new CodeGeneratorImpl("com.ponysdk.core.ui.test");
        final String initialGenerated = generator.generateWrapperClass(initialDef);
        
        // Add manual code to the initial generated wrapper
        final String wrapperWithManualCode = addManualCode(initialGenerated, manualCodeSnippets);
        
        // Act: Regenerate with updated component definition
        final String newGenerated = generator.generateWrapperClass(updatedDef);
        final IncrementalRegenerator regenerator = new IncrementalRegenerator();
        final IncrementalRegenerator.RegenerationResult result = 
            regenerator.regenerate(wrapperWithManualCode, newGenerated);
        
        // Assert: Manual code should be preserved
        for (final String manualCode : manualCodeSnippets) {
            if (!manualCode.trim().isEmpty()) {
                assertTrue(
                    result.updatedCode().contains(manualCode),
                    "Manual code should be preserved: " + manualCode
                );
            }
        }
        
        // Assert: Generated regions should be updated
        assertGeneratedRegionsUpdated(initialGenerated, newGenerated, result.updatedCode());
    }

    /**
     * Property 11 (variant): Manual code after generated regions is preserved
     * <p>
     * <b>Validates: Requirements 11.2</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Feature: generic-webcomponent-wrapper, Property 11: Manual code after generated regions preserved")
    void manualCodeAfterGeneratedRegionsPreserved(
            @ForAll("componentDefinition") ComponentDefinition def,
            @ForAll("manualMethods") List<String> manualMethods
    ) {
        // Arrange: Generate wrapper
        final CodeGenerator generator = new CodeGeneratorImpl("com.ponysdk.core.ui.test");
        final String generated = generator.generateWrapperClass(def);
        
        // Add manual methods after generated regions
        final String withManualCode = addManualMethodsAfterGenerated(generated, manualMethods);
        
        // Act: Regenerate with same definition (simulating update)
        final String newGenerated = generator.generateWrapperClass(def);
        final IncrementalRegenerator regenerator = new IncrementalRegenerator();
        final IncrementalRegenerator.RegenerationResult result = 
            regenerator.regenerate(withManualCode, newGenerated);
        
        // Assert: All manual methods should be preserved
        for (final String method : manualMethods) {
            assertTrue(
                result.updatedCode().contains(method),
                "Manual method should be preserved: " + method
            );
        }
    }

    /**
     * Property 11 (variant): Generated regions are updated while manual code preserved
     * <p>
     * <b>Validates: Requirements 11.3</b>
     * </p>
     */
    @Property(tries = 100)
    @Label("Feature: generic-webcomponent-wrapper, Property 11: Generated regions updated, manual preserved")
    void generatedRegionsUpdatedManualPreserved(
            @ForAll("componentWithEvents") ComponentDefinition initialDef,
            @ForAll("manualField") String manualField,
            @ForAll("additionalEvent") EventDef newEvent
    ) {
        // Arrange: Generate initial wrapper
        final CodeGenerator generator = new CodeGeneratorImpl("com.ponysdk.core.ui.test");
        final String initialGenerated = generator.generateWrapperClass(initialDef);
        
        // Add manual field
        final String withManualField = addManualFieldAfterGenerated(initialGenerated, manualField);
        
        // Create updated definition with additional event
        final List<EventDef> updatedEvents = new java.util.ArrayList<>(initialDef.events());
        updatedEvents.add(newEvent);
        final ComponentDefinition updatedDef = new ComponentDefinition(
            initialDef.tagName(),
            initialDef.className(),
            initialDef.summary(),
            initialDef.description(),
            initialDef.properties(),
            updatedEvents,
            initialDef.slots(),
            initialDef.methods(),
            initialDef.cssProperties(),
            initialDef.status()
        );
        
        // Act: Regenerate
        final String newGenerated = generator.generateWrapperClass(updatedDef);
        final IncrementalRegenerator regenerator = new IncrementalRegenerator();
        final IncrementalRegenerator.RegenerationResult result = 
            regenerator.regenerate(withManualField, newGenerated);
        
        // Assert: Manual field preserved
        assertTrue(
            result.updatedCode().contains(manualField),
            "Manual field should be preserved"
        );
        
        // Assert: New event listener method present
        final String newEventMethod = "add" + capitalize(newEvent.name()) + "Listener";
        assertTrue(
            result.updatedCode().contains(newEventMethod),
            "New event listener method should be present: " + newEventMethod
        );
    }

    // ========== Helper Methods ==========

    /**
     * Adds manual code snippets after the last generated region.
     */
    private String addManualCode(final String generated, final List<String> manualCodeSnippets) {
        if (manualCodeSnippets.isEmpty()) {
            return generated;
        }
        
        // Find the last occurrence of "END GENERATED CODE"
        final int lastEndMarker = generated.lastIndexOf("// ========== END GENERATED CODE ==========");
        if (lastEndMarker == -1) {
            // No generated regions, add at end before closing brace
            final int lastBrace = generated.lastIndexOf("}");
            final StringBuilder sb = new StringBuilder(generated.substring(0, lastBrace));
            sb.append("\n    // Manual code added\n");
            for (final String snippet : manualCodeSnippets) {
                sb.append("    ").append(snippet).append("\n");
            }
            sb.append("}");
            return sb.toString();
        }
        
        // Add after the last generated region
        final int insertPoint = generated.indexOf("\n", lastEndMarker) + 1;
        final StringBuilder sb = new StringBuilder(generated.substring(0, insertPoint));
        sb.append("\n    // Manual code added\n");
        for (final String snippet : manualCodeSnippets) {
            sb.append("    ").append(snippet).append("\n");
        }
        sb.append(generated.substring(insertPoint));
        return sb.toString();
    }

    /**
     * Adds manual methods after generated regions.
     */
    private String addManualMethodsAfterGenerated(final String generated, final List<String> methods) {
        if (methods.isEmpty()) {
            return generated;
        }
        
        final int lastEndMarker = generated.lastIndexOf("// ========== END GENERATED CODE ==========");
        if (lastEndMarker == -1) {
            return generated;
        }
        
        final int insertPoint = generated.indexOf("\n", lastEndMarker) + 1;
        final StringBuilder sb = new StringBuilder(generated.substring(0, insertPoint));
        sb.append("\n    // Manual methods\n");
        for (final String method : methods) {
            sb.append("    ").append(method).append("\n\n");
        }
        sb.append(generated.substring(insertPoint));
        return sb.toString();
    }

    /**
     * Adds a manual field after generated regions.
     */
    private String addManualFieldAfterGenerated(final String generated, final String field) {
        final int lastEndMarker = generated.lastIndexOf("// ========== END GENERATED CODE ==========");
        if (lastEndMarker == -1) {
            return generated;
        }
        
        final int insertPoint = generated.indexOf("\n", lastEndMarker) + 1;
        final StringBuilder sb = new StringBuilder(generated.substring(0, insertPoint));
        sb.append("\n    // Manual field\n");
        sb.append("    ").append(field).append("\n");
        sb.append(generated.substring(insertPoint));
        return sb.toString();
    }

    /**
     * Asserts that generated regions have been updated.
     */
    private void assertGeneratedRegionsUpdated(
            final String initial,
            final String newGenerated,
            final String result
    ) {
        // Check that result contains generated region markers
        assertTrue(
            result.contains("// ========== GENERATED"),
            "Result should contain generated region markers"
        );
        assertTrue(
            result.contains("// ========== END GENERATED CODE =========="),
            "Result should contain end markers"
        );
    }

    /**
     * Capitalizes the first letter of a string.
     */
    private String capitalize(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    // ========== Arbitraries (Generators) ==========

    /**
     * Generates random component definitions.
     */
    @Provide
    Arbitrary<ComponentDefinition> componentDefinition() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15).map(s -> "wa-" + s),
                Arbitraries.integers().between(0, 3),
                Arbitraries.integers().between(0, 3),
                Arbitraries.integers().between(0, 2)
        ).as((tagName, propCount, eventCount, slotCount) -> {
            final String className = tagNameToClassName(tagName);
            
            final List<PropertyDef> properties = generateProperties(propCount);
            final List<EventDef> events = generateEvents(eventCount);
            final List<SlotDef> slots = generateSlots(slotCount);
            
            return new ComponentDefinition(
                tagName,
                className,
                "Test component",
                "A test component for property testing",
                properties,
                events,
                slots,
                List.of(),
                List.of(),
                "stable"
            );
        });
    }

    /**
     * Generates component definitions with events.
     */
    @Provide
    Arbitrary<ComponentDefinition> componentWithEvents() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(15).map(s -> "wa-" + s),
                Arbitraries.integers().between(1, 3)
        ).as((tagName, eventCount) -> {
            final String className = tagNameToClassName(tagName);
            final List<EventDef> events = generateEvents(eventCount);
            
            return new ComponentDefinition(
                tagName,
                className,
                "Test component",
                "A test component with events",
                List.of(),
                events,
                List.of(),
                List.of(),
                List.of(),
                "stable"
            );
        });
    }

    /**
     * Generates manual code snippets.
     */
    @Provide
    Arbitrary<List<String>> manualCodeSnippets() {
        return Arbitraries.of(
                "private final Logger logger = LoggerFactory.getLogger(getClass());",
                "private String customField = \"custom\";",
                "public void customMethod() { /* custom logic */ }",
                "// Custom comment",
                "private static final int CUSTOM_CONSTANT = 42;"
        ).list().ofMinSize(1).ofMaxSize(3);
    }

    /**
     * Generates manual methods.
     */
    @Provide
    Arbitrary<List<String>> manualMethods() {
        return Arbitraries.of(
                "public void customMethod() {\n        // Custom implementation\n    }",
                "private void helperMethod() {\n        // Helper logic\n    }",
                "protected void onCustomEvent() {\n        // Event handler\n    }"
        ).list().ofMinSize(1).ofMaxSize(2);
    }

    /**
     * Generates a manual field.
     */
    @Provide
    Arbitrary<String> manualField() {
        return Arbitraries.of(
                "private final String customField = \"value\";",
                "private int counter = 0;",
                "private static final Logger LOG = LoggerFactory.getLogger(PButton.class);"
        );
    }

    /**
     * Generates an additional event.
     */
    @Provide
    Arbitrary<EventDef> additionalEvent() {
        return Arbitraries.of(
                new EventDef("blur", "Blur event", null, true, false),
                new EventDef("change", "Change event", null, true, false),
                new EventDef("input", "Input event", null, true, false)
        );
    }

    // ========== Helper Methods for Generation ==========

    private String tagNameToClassName(final String tagName) {
        final String withoutPrefix = tagName.replaceFirst("^[a-z]+-", "");
        return capitalize(kebabToCamelCase(withoutPrefix));
    }

    private String kebabToCamelCase(final String kebab) {
        final String[] parts = kebab.split("-");
        return java.util.stream.Stream.of(parts)
                .map(this::capitalize)
                .collect(Collectors.joining());
    }

    private List<PropertyDef> generateProperties(final int count) {
        final List<PropertyDef> properties = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            properties.add(new PropertyDef(
                "prop" + i,
                "string",
                "String",
                "string",
                "Property " + i,
                null,
                false,
                "public"
            ));
        }
        return properties;
    }

    private List<EventDef> generateEvents(final int count) {
        final String[] eventNames = {"click", "focus", "hover", "change", "input"};
        final List<EventDef> events = new java.util.ArrayList<>();
        for (int i = 0; i < count && i < eventNames.length; i++) {
            events.add(new EventDef(
                eventNames[i],
                "Event " + eventNames[i],
                null,
                true,
                false
            ));
        }
        return events;
    }

    private List<SlotDef> generateSlots(final int count) {
        final String[] slotNames = {"prefix", "suffix", "icon"};
        final List<SlotDef> slots = new java.util.ArrayList<>();
        for (int i = 0; i < count && i < slotNames.length; i++) {
            slots.add(new SlotDef(
                slotNames[i],
                "Slot " + slotNames[i]
            ));
        }
        return slots;
    }
}
