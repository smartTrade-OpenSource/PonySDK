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
import com.ponysdk.core.ui.codegen.model.PropertyDef;
import net.jqwik.api.*;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for props record generation completeness.
 * <p>
 * Feature: generic-webcomponent-wrapper, Property 5: Props Record Generation Completeness
 * </p>
 * <p>
 * <b>Validates: Requirements 3.1, 3.2</b>
 * </p>
 */
@Tag("Feature: generic-webcomponent-wrapper, Property 5: Props Record Generation Completeness")
public class PropsRecordGenerationCompletenessPropertyTest {

    private final CodeGeneratorImpl generator = new CodeGeneratorImpl("com.ponysdk.core.ui.test");

    /**
     * Property 5: Props Record Generation Completeness
     * 
     * **Validates: Requirements 3.1, 3.2**
     * 
     * For any component definition, the generated Props record should contain all public
     * properties (excluding private members) with correctly mapped Java types.
     */
    @Property(tries = 100)
    @Label("Feature: generic-webcomponent-wrapper, Property 5: Props Record Generation Completeness")
    void propsRecordContainsAllPublicPropertiesWithCorrectTypes(
        @ForAll("componentWithVariousProperties") final ComponentDefinition def
    ) {
        final String propsRecord = generator.generatePropsRecord(def);
        final List<PropertyDef> publicProperties = def.getPublicProperties();

        // Verify record declaration exists
        final String recordDeclaration = "public record " + def.getPropsClassName() + "(";
        assertTrue(propsRecord.contains(recordDeclaration),
            "Props record should have correct declaration");

        // Extract the record components section (between the parentheses)
        final Pattern recordPattern = Pattern.compile(
            "public record " + Pattern.quote(def.getPropsClassName()) + "\\s*\\(([^)]+)\\)",
            Pattern.DOTALL
        );
        final Matcher matcher = recordPattern.matcher(propsRecord);
        assertTrue(matcher.find(), "Should find record declaration with components");

        final String recordComponents = matcher.group(1);

        // Verify each public property appears in the record with correct type
        for (final PropertyDef prop : publicProperties) {
            // Check that the property appears with its Java type
            final String propertyDeclaration = prop.javaType() + " " + prop.name();
            assertTrue(recordComponents.contains(propertyDeclaration),
                "Props record should contain property '" + prop.name() + 
                "' with type '" + prop.javaType() + "'");
        }

        // Verify the count of properties matches
        // Count non-empty lines in the record components section
        final String[] lines = recordComponents.split("\n");
        int propertyCount = 0;
        for (final String line : lines) {
            final String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//") && !trimmed.startsWith("/*")) {
                propertyCount++;
            }
        }

        assertEquals(publicProperties.size(), propertyCount,
            "Props record should have exactly " + publicProperties.size() + 
            " properties (all public properties)");

        // Verify private properties are NOT included
        for (final PropertyDef prop : def.properties()) {
            if ("private".equals(prop.privacy())) {
                final String privatePropertyDeclaration = prop.javaType() + " " + prop.name();
                assertFalse(recordComponents.contains(privatePropertyDeclaration),
                    "Props record should NOT contain private property '" + prop.name() + "'");
            }
        }
    }

    // ========== Generators ==========

    /**
     * Generates component definitions with various property configurations including:
     * - Different numbers of properties (1-10)
     * - Mix of public and private properties
     * - Various Java types (String, boolean, int, Optional, List)
     */
    @Provide
    Arbitrary<ComponentDefinition> componentWithVariousProperties() {
        return Combinators.combine(
            tagNames(),
            publicProperties(),
            privateProperties()
        ).as((tagName, publicProps, privateProps) -> {
            // Combine public and private properties
            final List<PropertyDef> allProperties = new java.util.ArrayList<>(publicProps);
            allProperties.addAll(privateProps);

            return new ComponentDefinition(
                tagName,
                toClassName(tagName),
                "A test component",
                "Component for testing props record generation",
                allProperties,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                "stable"
            );
        });
    }

    @Provide
    Arbitrary<String> tagNames() {
        return Arbitraries.of(
            "wa-button",
            "wa-input",
            "wa-dialog",
            "wa-card",
            "wa-tab-group",
            "wa-select",
            "wa-checkbox",
            "wa-radio",
            "wa-textarea",
            "wa-switch"
        );
    }

    @Provide
    Arbitrary<List<PropertyDef>> publicProperties() {
        return propertyDefs("public").list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<List<PropertyDef>> privateProperties() {
        return propertyDefs("private").list().ofMaxSize(3);
    }

    private Arbitrary<PropertyDef> propertyDefs(final String privacy) {
        return Combinators.combine(
            propertyNames(),
            javaTypes(),
            Arbitraries.strings().ofMaxLength(100),
            Arbitraries.strings().ofMaxLength(20),
            Arbitraries.of(true, false)
        ).as((name, typeInfo, desc, defaultVal, required) ->
            new PropertyDef(
                name,
                typeInfo.cemType,
                typeInfo.javaType,
                typeInfo.tsType,
                desc,
                defaultVal,
                required,
                privacy
            )
        );
    }

    @Provide
    Arbitrary<String> propertyNames() {
        return Arbitraries.of(
            "variant",
            "size",
            "disabled",
            "loading",
            "outline",
            "value",
            "placeholder",
            "label",
            "name",
            "required",
            "readonly",
            "checked",
            "open",
            "closable",
            "placement",
            "trigger",
            "distance",
            "skidding",
            "hoist",
            "flip",
            "shift",
            "autoSize",
            "sync"
        );
    }

    @Provide
    Arbitrary<TypeInfo> javaTypes() {
        return Arbitraries.of(
            new TypeInfo("string", "String", "string"),
            new TypeInfo("boolean", "boolean", "boolean"),
            new TypeInfo("number", "int", "number"),
            new TypeInfo("number", "double", "number"),
            new TypeInfo("string | undefined", "Optional<String>", "string | undefined"),
            new TypeInfo("number | undefined", "Optional<Integer>", "number | undefined"),
            new TypeInfo("boolean | undefined", "Optional<Boolean>", "boolean | undefined"),
            new TypeInfo("string[]", "List<String>", "string[]"),
            new TypeInfo("number[]", "List<Integer>", "number[]"),
            new TypeInfo("'small' | 'medium' | 'large'", "String", "'small' | 'medium' | 'large'"),
            new TypeInfo("'top' | 'bottom' | 'left' | 'right'", "String", "'top' | 'bottom' | 'left' | 'right'")
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

    /**
     * Helper record to group type information together.
     */
    private record TypeInfo(String cemType, String javaType, String tsType) {}
}
