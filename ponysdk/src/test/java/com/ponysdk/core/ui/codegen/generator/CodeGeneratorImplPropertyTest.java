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
import com.ponysdk.core.ui.codegen.model.EventDef;
import com.ponysdk.core.ui.codegen.model.MethodDef;
import com.ponysdk.core.ui.codegen.model.ParameterDef;
import com.ponysdk.core.ui.codegen.model.SlotDef;
import net.jqwik.api.*;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for {@link CodeGeneratorImpl}.
 */
class CodeGeneratorImplPropertyTest {

    private final CodeGeneratorImpl generator = new CodeGeneratorImpl("com.ponysdk.core.ui.test");

    /**
     * Property 3: Wrapper Class Generation Structure
     * 
     * **Validates: Requirements 2.1, 2.2, 2.4, 12.1**
     * 
     * For any component definition, the generated Java wrapper class should extend PWebComponent
     * with the correct type parameter, include the tag name constant, and have a constructor
     * accepting the Props record.
     */
    @Property
    @Label("Feature: generic-webcomponent-wrapper, Property 3: Wrapper Class Generation Structure")
    void wrapperClassGenerationStructure(@ForAll("componentDefinition") final ComponentDefinition def) {
        final String source = generator.generateWrapperClass(def);

        // Verify extends PWebComponent with correct type parameter
        final String expectedClassDeclaration = "public class " + def.getWrapperClassName() + 
            " extends PWebComponent<" + def.getPropsClassName() + ">";
        assertTrue(source.contains(expectedClassDeclaration),
            "Generated class should extend PWebComponent with correct type parameter");

        // Verify TAG_NAME constant
        final String expectedTagName = "private static final String TAG_NAME = \"" + def.tagName() + "\";";
        assertTrue(source.contains(expectedTagName),
            "Generated class should include TAG_NAME constant");

        // Verify constructor accepting Props
        final String expectedConstructor = "public " + def.getWrapperClassName() + 
            "(final " + def.getPropsClassName() + " initialProps)";
        assertTrue(source.contains(expectedConstructor),
            "Generated class should have constructor accepting Props record");

        // Verify getPropsClass() override
        assertTrue(source.contains("protected Class<" + def.getPropsClassName() + "> getPropsClass()"),
            "Generated class should override getPropsClass()");

        // Verify getComponentSignature() override
        assertTrue(source.contains("protected String getComponentSignature()"),
            "Generated class should override getComponentSignature()");
    }

    /**
     * Property 4: Method Generation Completeness
     * 
     * **Validates: Requirements 2.3, 17.1, 17.2, 17.3**
     * 
     * For any component definition with methods, all methods defined in the CEM should appear
     * in the generated wrapper class with correctly mapped parameter and return types.
     */
    @Property
    @Label("Feature: generic-webcomponent-wrapper, Property 4: Method Generation Completeness")
    void methodGenerationCompleteness(@ForAll("componentWithMethods") final ComponentDefinition def) {
        final String source = generator.generateWrapperClass(def);

        // Verify all methods are generated
        for (final MethodDef method : def.methods()) {
            final String methodSignature = "public " + (method.async() ? "CompletableFuture" : "") + 
                " " + method.name() + "(";
            assertTrue(source.contains(methodSignature),
                "Generated class should include method: " + method.name());

            // Verify parameters are included
            for (final ParameterDef param : method.parameters()) {
                assertTrue(source.contains(param.javaType() + " " + param.name()),
                    "Method " + method.name() + " should include parameter: " + param.name());
            }
        }

        // Verify method proxies section marker
        if (!def.methods().isEmpty()) {
            assertTrue(source.contains("// ========== GENERATED METHOD PROXIES =========="),
                "Generated class should include method proxies section marker");
        }
    }

    /**
     * Property 8: Event Listener Generation
     * 
     * **Validates: Requirements 6.1, 6.2, 6.3, 12.3**
     * 
     * For any component definition with events, the generated wrapper class should include
     * an event listener method for each event that calls the inherited onEvent() method
     * with the correct event name.
     */
    @Property
    @Label("Feature: generic-webcomponent-wrapper, Property 8: Event Listener Generation")
    void eventListenerGeneration(@ForAll("componentWithEvents") final ComponentDefinition def) {
        final String source = generator.generateWrapperClass(def);

        // Verify all event listeners are generated
        for (final EventDef event : def.events()) {
            final String listenerMethod = "public void " + event.getListenerMethodName() + 
                "(final Consumer<JsonObject> handler)";
            assertTrue(source.contains(listenerMethod),
                "Generated class should include listener method: " + event.getListenerMethodName());

            // Verify onEvent call with correct event name
            final String onEventCall = "onEvent(\"" + event.name() + "\", handler);";
            assertTrue(source.contains(onEventCall),
                "Listener method should call onEvent with correct event name: " + event.name());
        }

        // Verify event handlers section marker
        if (!def.events().isEmpty()) {
            assertTrue(source.contains("// ========== GENERATED EVENT HANDLERS =========="),
                "Generated class should include event handlers section marker");
        }
    }

    /**
     * Property 9: Slot Method Generation
     * 
     * **Validates: Requirements 7.1, 7.2**
     * 
     * For any component definition with slots, the generated wrapper class should include
     * a method for each named slot that calls addToSlot() with the correct slot name.
     */
    @Property
    @Label("Feature: generic-webcomponent-wrapper, Property 9: Slot Method Generation")
    void slotMethodGeneration(@ForAll("componentWithSlots") final ComponentDefinition def) {
        final String source = generator.generateWrapperClass(def);

        // Verify all slot methods are generated
        for (final SlotDef slot : def.slots()) {
            final String slotMethod = "public void " + slot.getSlotMethodName() + 
                "(final PComponent<?> child)";
            assertTrue(source.contains(slotMethod),
                "Generated class should include slot method: " + slot.getSlotMethodName());

            // Verify addToSlot call with correct slot name
            final String slotName = slot.isDefaultSlot() ? "" : slot.name();
            final String addToSlotCall = "addToSlot(\"" + slotName + "\", child);";
            assertTrue(source.contains(addToSlotCall),
                "Slot method should call addToSlot with correct slot name: " + slotName);
        }

        // Verify slot methods section marker
        if (!def.slots().isEmpty()) {
            assertTrue(source.contains("// ========== GENERATED SLOT METHODS =========="),
                "Generated class should include slot methods section marker");
        }
    }

    /**
     * Property 14: Async Method Return Type
     * 
     * **Validates: Requirements 17.5**
     * 
     * For any component method marked as async in the CEM, the generated Java method
     * should return CompletableFuture.
     */
    @Property
    @Label("Feature: generic-webcomponent-wrapper, Property 14: Async Method Return Type")
    void asyncMethodReturnType(@ForAll("componentWithAsyncMethods") final ComponentDefinition def) {
        final String source = generator.generateWrapperClass(def);

        // Verify all async methods return CompletableFuture
        for (final MethodDef method : def.methods()) {
            if (method.async()) {
                final String asyncMethodSignature = "public CompletableFuture<";
                assertTrue(source.contains(asyncMethodSignature + "Void> " + method.name() + "(") ||
                          source.contains(asyncMethodSignature + "String> " + method.name() + "(") ||
                          source.contains(asyncMethodSignature + "Boolean> " + method.name() + "(") ||
                          source.contains(asyncMethodSignature + "Double> " + method.name() + "("),
                    "Async method " + method.name() + " should return CompletableFuture");

                // Verify callComponentMethodAsync is used
                final String asyncCall = "callComponentMethodAsync(\"" + method.name() + "\"";
                assertTrue(source.contains(asyncCall),
                    "Async method should call callComponentMethodAsync");
            }
        }
    }

    // ========== Generators ==========

    @Provide
    Arbitrary<ComponentDefinition> componentDefinition() {
        return Arbitraries.of(
            createMinimalComponent("wa-button"),
            createMinimalComponent("wa-input"),
            createMinimalComponent("wa-dialog"),
            createMinimalComponent("wa-tab-group"),
            createMinimalComponent("wa-card")
        );
    }

    @Provide
    Arbitrary<ComponentDefinition> componentWithMethods() {
        return Combinators.combine(
            Arbitraries.of("wa-button", "wa-input", "wa-dialog"),
            Arbitraries.integers().between(1, 3)
        ).as((tagName, methodCount) -> {
            final List<MethodDef> methods = List.of(
                new MethodDef("focus", "Focuses the component", Collections.emptyList(), "void", false),
                new MethodDef("blur", "Blurs the component", Collections.emptyList(), "void", false),
                new MethodDef("checkValidity", "Checks validity", Collections.emptyList(), "boolean", false)
            ).subList(0, methodCount);

            return new ComponentDefinition(
                tagName,
                toClassName(tagName),
                "A component",
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                methods,
                Collections.emptyList(),
                "stable"
            );
        });
    }

    @Provide
    Arbitrary<ComponentDefinition> componentWithEvents() {
        return Combinators.combine(
            Arbitraries.of("wa-button", "wa-input", "wa-dialog"),
            Arbitraries.integers().between(1, 3)
        ).as((tagName, eventCount) -> {
            final List<EventDef> events = List.of(
                new EventDef("wa-click", "Emitted on click", null, true, false),
                new EventDef("wa-focus", "Emitted on focus", null, true, false),
                new EventDef("wa-blur", "Emitted on blur", null, true, false)
            ).subList(0, eventCount);

            return new ComponentDefinition(
                tagName,
                toClassName(tagName),
                "A component",
                "",
                Collections.emptyList(),
                events,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "stable"
            );
        });
    }

    @Provide
    Arbitrary<ComponentDefinition> componentWithSlots() {
        return Combinators.combine(
            Arbitraries.of("wa-button", "wa-card", "wa-dialog"),
            Arbitraries.integers().between(1, 3)
        ).as((tagName, slotCount) -> {
            final List<SlotDef> slots = List.of(
                new SlotDef("prefix", "Prefix slot"),
                new SlotDef("suffix", "Suffix slot"),
                new SlotDef("", "Default slot")
            ).subList(0, slotCount);

            return new ComponentDefinition(
                tagName,
                toClassName(tagName),
                "A component",
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                slots,
                Collections.emptyList(),
                Collections.emptyList(),
                "stable"
            );
        });
    }

    @Provide
    Arbitrary<ComponentDefinition> componentWithAsyncMethods() {
        return Arbitraries.of("wa-dialog", "wa-drawer", "wa-popup").map(tagName -> {
            final List<MethodDef> methods = List.of(
                new MethodDef("show", "Shows the component", Collections.emptyList(), "Promise<void>", true),
                new MethodDef("hide", "Hides the component", Collections.emptyList(), "Promise<void>", true),
                new MethodDef("getValue", "Gets value", Collections.emptyList(), "Promise<string>", true)
            );

            return new ComponentDefinition(
                tagName,
                toClassName(tagName),
                "A component",
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                methods,
                Collections.emptyList(),
                "stable"
            );
        });
    }

    private ComponentDefinition createMinimalComponent(final String tagName) {
        return new ComponentDefinition(
            tagName,
            toClassName(tagName),
            "A component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );
    }

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
}
