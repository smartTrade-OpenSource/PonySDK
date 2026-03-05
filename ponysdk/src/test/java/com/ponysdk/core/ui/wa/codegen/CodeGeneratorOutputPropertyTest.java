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

import net.jqwik.api.*;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Code Generator output completeness.
 * <p>
 * Feature: ui-library-wrapper, Property 14: Code Generator Output Completeness
 * </p>
 * <p>
 * <b>Validates: Requirements 12.1, 12.3, 13.2, 13.3, 13.4, 13.5, 13.8</b>
 * </p>
 */
@Tag("Feature: ui-library-wrapper, Property 14: Code Generator Output Completeness")
public class CodeGeneratorOutputPropertyTest {

    private final WebAwesomeCodeGenerator generator = new WebAwesomeCodeGenerator(
        Path.of("dummy"), Path.of("dummy"), Path.of("dummy")
    );

    // ========== Property 14a: Generated Java Record has exactly P fields ==========

    /**
     * For any ComponentDefinition with P properties, the generated Java Record
     * SHALL have exactly P fields (one per property).
     * <p><b>Validates: Requirements 13.2</b></p>
     */
    @Property(tries = 100)
    void generatedRecordHasExactlyPFields(@ForAll("componentDefinition") ComponentDefinition def) {
        final String record = generator.generatePropsRecord(def);
        final String recordName = WebAwesomeCodeGenerator.tagNameToPropsClassName(def.tagName());

        // Extract the field block between the record declaration parens
        final int declStart = record.indexOf("public record " + recordName + "(");
        assertTrue(declStart >= 0, "Record declaration should exist");
        final int fieldsStart = record.indexOf("(", declStart) + 1;
        final int fieldsEnd = record.indexOf(")", fieldsStart);
        final String fieldsBlock = record.substring(fieldsStart, fieldsEnd).trim();

        int fieldCount = 0;
        for (final String line : fieldsBlock.split("\n")) {
            if (!line.trim().isEmpty()) {
                fieldCount++;
            }
        }

        assertEquals(def.properties().size(), fieldCount,
            "Record should have exactly " + def.properties().size() + " fields for tag " + def.tagName());
    }

    // ========== Property 14b: Wrapper class extends PWebComponent with correct signature ==========

    /**
     * For any ComponentDefinition, the generated wrapper class SHALL extend
     * PWebComponent with the correct props type and getComponentSignature()
     * SHALL return the wa-* tag name.
     * <p><b>Validates: Requirements 12.1, 12.3, 13.3</b></p>
     */
    @Property(tries = 100)
    void wrapperClassExtendsCorrectlyAndHasSignature(@ForAll("componentDefinition") ComponentDefinition def) {
        final String wrapper = generator.generateWrapperClass(def);
        final String propsClassName = WebAwesomeCodeGenerator.tagNameToPropsClassName(def.tagName());
        final String wrapperClassName = WebAwesomeCodeGenerator.tagNameToWrapperClassName(def.tagName());

        assertTrue(wrapper.contains("extends PWebComponent<" + propsClassName + ">"),
            "Wrapper should extend PWebComponent<" + propsClassName + ">");
        assertTrue(wrapper.contains("return \"" + def.tagName() + "\""),
            "getComponentSignature() should return '" + def.tagName() + "'");
        assertTrue(wrapper.contains("public class " + wrapperClassName),
            "Wrapper should declare class " + wrapperClassName);
    }

    // ========== Property 14c: Javadoc contains all events ==========

    /**
     * For any ComponentDefinition with E events, the generated Javadoc
     * SHALL contain all E event names.
     * <p><b>Validates: Requirements 13.4</b></p>
     */
    @Property(tries = 100)
    void javadocContainsAllEvents(@ForAll("componentDefinitionWithEvents") ComponentDefinition def) {
        final String wrapper = generator.generateWrapperClass(def);

        for (final EventDef event : def.events()) {
            assertTrue(wrapper.contains(event.name()),
                "Javadoc should contain event '" + event.name() + "'");
        }
    }

    // ========== Property 14d: Javadoc contains all slot names ==========

    /**
     * For any ComponentDefinition with S slots, the generated Javadoc
     * SHALL contain all S slot names (or "(default)" for empty slot names).
     * <p><b>Validates: Requirements 13.4</b></p>
     */
    @Property(tries = 100)
    void javadocContainsAllSlots(@ForAll("componentDefinitionWithSlots") ComponentDefinition def) {
        final String wrapper = generator.generateWrapperClass(def);

        for (final SlotDef slot : def.slots()) {
            final String expected = slot.name().isEmpty() ? "(default)" : slot.name();
            assertTrue(wrapper.contains(expected),
                "Javadoc should contain slot '" + expected + "'");
        }
    }

    // ========== Property 14e: Javadoc contains all CSS part names ==========

    /**
     * For any ComponentDefinition with C CSS parts, the generated Javadoc
     * SHALL contain all C CSS part names.
     * <p><b>Validates: Requirements 13.4</b></p>
     */
    @Property(tries = 100)
    void javadocContainsAllCssParts(@ForAll("componentDefinitionWithCssParts") ComponentDefinition def) {
        final String wrapper = generator.generateWrapperClass(def);

        for (final CssPartDef part : def.cssParts()) {
            assertTrue(wrapper.contains(part.name()),
                "Javadoc should contain CSS part '" + part.name() + "'");
        }
    }

    // ========== Property 14f: TS interface has exactly P fields ==========

    /**
     * For any ComponentDefinition with P properties, the generated TypeScript
     * interface SHALL have exactly P fields.
     * <p><b>Validates: Requirements 13.5</b></p>
     */
    @Property(tries = 100)
    void tsInterfaceHasExactlyPFields(@ForAll("componentDefinition") ComponentDefinition def) {
        final String tsInterface = generator.generateTypeScriptInterface(def);

        final int bodyStart = tsInterface.indexOf("{") + 1;
        final int bodyEnd = tsInterface.lastIndexOf("}");
        final String body = tsInterface.substring(bodyStart, bodyEnd);

        int fieldCount = 0;
        for (final String line : body.split("\n")) {
            final String trimmed = line.trim();
            if (!trimmed.isEmpty() && trimmed.contains(":") && trimmed.endsWith(";")) {
                fieldCount++;
            }
        }

        assertEquals(def.properties().size(), fieldCount,
            "TS interface should have exactly " + def.properties().size() + " fields");
    }

    // ========== Property 14g: Component index entry contains tag, wrapper, and props ==========

    /**
     * For any ComponentDefinition, the generated component index entry SHALL
     * contain the tag name, wrapper class name, and props class name.
     * <p><b>Validates: Requirements 13.8</b></p>
     */
    @Property(tries = 100)
    void indexEntryContainsAllInfo(@ForAll("componentDefinition") ComponentDefinition def) {
        final String index = generator.generateComponentIndex(List.of(def));
        final String wrapperClassName = WebAwesomeCodeGenerator.tagNameToWrapperClassName(def.tagName());
        final String propsClassName = WebAwesomeCodeGenerator.tagNameToPropsClassName(def.tagName());

        assertTrue(index.contains(def.tagName()),
            "Index should contain tag name '" + def.tagName() + "'");
        assertTrue(index.contains(wrapperClassName),
            "Index should contain wrapper class '" + wrapperClassName + "'");
        assertTrue(index.contains(propsClassName),
            "Index should contain props class '" + propsClassName + "'");
    }

    // ========== Arbitraries ==========

    /**
     * Builds a ComponentDefinition arbitrary by combining two groups
     * (jqwik Combinators.combine supports max 8 parameters).
     */
    @Provide
    Arbitrary<ComponentDefinition> componentDefinition() {
        // Group 1: tag, className, summary, properties, status (5 params)
        final Arbitrary<Object[]> group1 = Combinators.combine(
            tagNames(),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
            Arbitraries.strings().ofMaxLength(200),
            propertyDefs().list().ofMinSize(1).ofMaxSize(30),
            Arbitraries.of("stable", "experimental", "deprecated")
        ).as((tag, cls, summary, props, status) -> new Object[]{tag, cls, summary, props, status});

        // Group 2: events, slots, cssParts, cssProperties (4 params)
        final Arbitrary<Object[]> group2 = Combinators.combine(
            eventDefs().list().ofMaxSize(10),
            slotDefs().list().ofMaxSize(10),
            cssPartDefs().list().ofMaxSize(10),
            cssPropertyDefs().list().ofMaxSize(5)
        ).as((events, slots, cssParts, cssProps) -> new Object[]{events, slots, cssParts, cssProps});

        return Combinators.combine(group1, group2).as((g1, g2) -> buildDef(g1, g2));
    }

    @Provide
    Arbitrary<ComponentDefinition> componentDefinitionWithEvents() {
        final Arbitrary<Object[]> group1 = Combinators.combine(
            tagNames(),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
            Arbitraries.strings().ofMaxLength(200),
            propertyDefs().list().ofMinSize(1).ofMaxSize(10),
            Arbitraries.of("stable", "experimental", "deprecated")
        ).as((tag, cls, summary, props, status) -> new Object[]{tag, cls, summary, props, status});

        final Arbitrary<Object[]> group2 = Combinators.combine(
            eventDefs().list().ofMinSize(1).ofMaxSize(10),
            slotDefs().list().ofMaxSize(5),
            cssPartDefs().list().ofMaxSize(5),
            cssPropertyDefs().list().ofMaxSize(3)
        ).as((events, slots, cssParts, cssProps) -> new Object[]{events, slots, cssParts, cssProps});

        return Combinators.combine(group1, group2).as((g1, g2) -> buildDef(g1, g2));
    }

    @Provide
    Arbitrary<ComponentDefinition> componentDefinitionWithSlots() {
        final Arbitrary<Object[]> group1 = Combinators.combine(
            tagNames(),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
            Arbitraries.strings().ofMaxLength(200),
            propertyDefs().list().ofMinSize(1).ofMaxSize(10),
            Arbitraries.of("stable", "experimental", "deprecated")
        ).as((tag, cls, summary, props, status) -> new Object[]{tag, cls, summary, props, status});

        final Arbitrary<Object[]> group2 = Combinators.combine(
            eventDefs().list().ofMaxSize(5),
            slotDefs().list().ofMinSize(1).ofMaxSize(10),
            cssPartDefs().list().ofMaxSize(5),
            cssPropertyDefs().list().ofMaxSize(3)
        ).as((events, slots, cssParts, cssProps) -> new Object[]{events, slots, cssParts, cssProps});

        return Combinators.combine(group1, group2).as((g1, g2) -> buildDef(g1, g2));
    }

    @Provide
    Arbitrary<ComponentDefinition> componentDefinitionWithCssParts() {
        final Arbitrary<Object[]> group1 = Combinators.combine(
            tagNames(),
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(20),
            Arbitraries.strings().ofMaxLength(200),
            propertyDefs().list().ofMinSize(1).ofMaxSize(10),
            Arbitraries.of("stable", "experimental", "deprecated")
        ).as((tag, cls, summary, props, status) -> new Object[]{tag, cls, summary, props, status});

        final Arbitrary<Object[]> group2 = Combinators.combine(
            eventDefs().list().ofMaxSize(5),
            slotDefs().list().ofMaxSize(5),
            cssPartDefs().list().ofMinSize(1).ofMaxSize(10),
            cssPropertyDefs().list().ofMaxSize(3)
        ).as((events, slots, cssParts, cssProps) -> new Object[]{events, slots, cssParts, cssProps});

        return Combinators.combine(group1, group2).as((g1, g2) -> buildDef(g1, g2));
    }

    @SuppressWarnings("unchecked")
    private static ComponentDefinition buildDef(final Object[] g1, final Object[] g2) {
        return new ComponentDefinition(
            (String) g1[0],
            (String) g1[1],
            (String) g1[2],
            "",
            (List<PropertyDef>) g1[3],
            (List<EventDef>) g2[0],
            (List<SlotDef>) g2[1],
            (List<CssPartDef>) g2[2],
            (List<CssPropertyDef>) g2[3],
            (String) g1[4]
        );
    }

    private Arbitrary<String> tagNames() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(2).ofMaxLength(20)
            .map(s -> "wa-" + s);
    }

    private Arbitrary<PropertyDef> propertyDefs() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
            Arbitraries.of("string", "number", "boolean", "string | undefined"),
            Arbitraries.strings().ofMaxLength(100),
            Arbitraries.strings().ofMaxLength(20),
            Arbitraries.of(true, false)
        ).as((name, type, desc, def, reflects) ->
            new PropertyDef(name, type, TypeMapper.mapToJavaType(type), desc, def, reflects));
    }

    private Arbitrary<EventDef> eventDefs() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(30)
                .map(s -> "wa-" + s),
            Arbitraries.strings().ofMaxLength(100),
            Arbitraries.of("void", "string", "number", "")
        ).as(EventDef::new);
    }

    private Arbitrary<SlotDef> slotDefs() {
        return Combinators.combine(
            Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
            ),
            Arbitraries.strings().ofMaxLength(100)
        ).as(SlotDef::new);
    }

    private Arbitrary<CssPartDef> cssPartDefs() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
            Arbitraries.strings().ofMaxLength(100)
        ).as(CssPartDef::new);
    }

    private Arbitrary<CssPropertyDef> cssPropertyDefs() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
                .map(s -> "--wa-" + s),
            Arbitraries.strings().ofMaxLength(100),
            Arbitraries.strings().ofMaxLength(20)
        ).as(CssPropertyDef::new);
    }
}
