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
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RegistrationClassGenerator}.
 * Tests registration class generation for component libraries.
 * Validates Requirements 8.1, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3, 9.4.
 */
class RegistrationClassGeneratorTest {

    @Test
    void generateRegistrationClass_generatesCorrectPackage() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final String source = generator.generateRegistrationClass(Collections.emptyList());

        assertTrue(source.contains("package com.ponysdk.core.ui.webawesome;"));
    }

    @Test
    void generateRegistrationClass_importsRequiredClasses() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final String source = generator.generateRegistrationClass(Collections.emptyList());

        assertTrue(source.contains("import java.util.HashMap;"));
        assertTrue(source.contains("import java.util.Map;"));
        assertTrue(source.contains("import java.util.function.Supplier;"));
        assertTrue(source.contains("import com.ponysdk.core.ui.component.PComponent;"));
    }

    @Test
    void generateRegistrationClass_generatesCorrectClassName() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final String source = generator.generateRegistrationClass(Collections.emptyList());

        assertTrue(source.contains("public final class WebawesomeComponentRegistry {"));
    }

    @Test
    void generateRegistrationClass_capitalizesLibraryNameInClassName() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.shoelace",
            "shoelace"
        );

        final String source = generator.generateRegistrationClass(Collections.emptyList());

        assertTrue(source.contains("public final class ShoelaceComponentRegistry {"));
    }

    @Test
    void generateRegistrationClass_includesJavadocWithLibraryName() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final String source = generator.generateRegistrationClass(Collections.emptyList());

        assertTrue(source.contains("/**"));
        assertTrue(source.contains("* Registration class for webawesome components."));
        assertTrue(source.contains("* Generated code - do not modify manually."));
        assertTrue(source.contains("*/"));
    }

    @Test
    void generateRegistrationClass_includesPrivateConstructor() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final String source = generator.generateRegistrationClass(Collections.emptyList());

        assertTrue(source.contains("private WebawesomeComponentRegistry() {"));
        assertTrue(source.contains("// Utility class"));
    }

    @Test
    void generateRegistrationClass_includesComponentFactoryMap() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final String source = generator.generateRegistrationClass(Collections.emptyList());

        assertTrue(source.contains("private static final Map<String, Supplier<PComponent<?>>> COMPONENT_FACTORIES = new HashMap<>();"));
    }

    @Test
    void generateRegistrationClass_registersComponentsInStaticInitializer() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("wa-input")
        );

        final String source = generator.generateRegistrationClass(components);

        assertTrue(source.contains("static {"));
        assertTrue(source.contains("COMPONENT_FACTORIES.put(\"wa-button\", PButton::new);"));
        assertTrue(source.contains("COMPONENT_FACTORIES.put(\"wa-input\", PInput::new);"));
    }

    @Test
    void generateRegistrationClass_handlesMultiWordComponentNames() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-tab-group"),
            createComponent("wa-color-picker")
        );

        final String source = generator.generateRegistrationClass(components);

        assertTrue(source.contains("COMPONENT_FACTORIES.put(\"wa-tab-group\", PTabGroup::new);"));
        assertTrue(source.contains("COMPONENT_FACTORIES.put(\"wa-color-picker\", PColorPicker::new);"));
    }

    @Test
    void generateRegistrationClass_includesRegisterAllMethod() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final String source = generator.generateRegistrationClass(Collections.emptyList());

        assertTrue(source.contains("/**"));
        assertTrue(source.contains("* Registers all webawesome components with the Component Terminal."));
        assertTrue(source.contains("public static void registerAll() {"));
        assertTrue(source.contains("// Component registration logic would go here"));
    }

    @Test
    void generateRegistrationClass_includesGetFactoryMethod() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final String source = generator.generateRegistrationClass(Collections.emptyList());

        assertTrue(source.contains("/**"));
        assertTrue(source.contains("* Gets a component factory for the specified tag name."));
        assertTrue(source.contains("@param tagName the component tag name"));
        assertTrue(source.contains("@return the component factory, or null if not found"));
        assertTrue(source.contains("public static Supplier<PComponent<?>> getFactory(final String tagName) {"));
        assertTrue(source.contains("return COMPONENT_FACTORIES.get(tagName);"));
    }

    @Test
    void generateRegistrationClass_includesGetTagNamesMethod() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final String source = generator.generateRegistrationClass(Collections.emptyList());

        assertTrue(source.contains("/**"));
        assertTrue(source.contains("* Gets all registered component tag names."));
        assertTrue(source.contains("@return set of tag names"));
        assertTrue(source.contains("public static java.util.Set<String> getTagNames() {"));
        assertTrue(source.contains("return COMPONENT_FACTORIES.keySet();"));
    }

    @Test
    void generateRegistrationClass_handlesEmptyComponentList() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final String source = generator.generateRegistrationClass(Collections.emptyList());

        assertTrue(source.contains("static {"));
        assertTrue(source.contains("}"));
        assertFalse(source.contains("COMPONENT_FACTORIES.put"));
    }

    @Test
    void generateRegistrationClass_handlesSingleComponent() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button")
        );

        final String source = generator.generateRegistrationClass(components);

        assertTrue(source.contains("COMPONENT_FACTORIES.put(\"wa-button\", PButton::new);"));
        assertEquals(1, countOccurrences(source, "COMPONENT_FACTORIES.put"));
    }

    @Test
    void generateRegistrationClass_handlesMultipleComponents() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("wa-input"),
            createComponent("wa-card"),
            createComponent("wa-dialog")
        );

        final String source = generator.generateRegistrationClass(components);

        assertTrue(source.contains("COMPONENT_FACTORIES.put(\"wa-button\", PButton::new);"));
        assertTrue(source.contains("COMPONENT_FACTORIES.put(\"wa-input\", PInput::new);"));
        assertTrue(source.contains("COMPONENT_FACTORIES.put(\"wa-card\", PCard::new);"));
        assertTrue(source.contains("COMPONENT_FACTORIES.put(\"wa-dialog\", PDialog::new);"));
        assertEquals(4, countOccurrences(source, "COMPONENT_FACTORIES.put"));
    }

    @Test
    void generateRegistrationClass_differentLibrariesGenerateDifferentPackages() {
        final RegistrationClassGenerator waGenerator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );
        final RegistrationClassGenerator slGenerator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.shoelace",
            "shoelace"
        );

        final List<ComponentDefinition> components = List.of(createComponent("button"));

        final String waSource = waGenerator.generateRegistrationClass(components);
        final String slSource = slGenerator.generateRegistrationClass(components);

        assertTrue(waSource.contains("package com.ponysdk.core.ui.webawesome;"));
        assertTrue(slSource.contains("package com.ponysdk.core.ui.shoelace;"));
        assertTrue(waSource.contains("public final class WebawesomeComponentRegistry {"));
        assertTrue(slSource.contains("public final class ShoelaceComponentRegistry {"));
    }

    @Test
    void generateRegistrationClass_usesUniqueComponentIdentifiers() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("wa-input")
        );

        final String source = generator.generateRegistrationClass(components);

        // Each component should have a unique tag name as identifier
        assertTrue(source.contains("\"wa-button\""));
        assertTrue(source.contains("\"wa-input\""));
        // Tag names should be used as keys in the factory map
        assertTrue(source.contains("COMPONENT_FACTORIES.put(\"wa-button\","));
        assertTrue(source.contains("COMPONENT_FACTORIES.put(\"wa-input\","));
    }

    @Test
    void generateRegistrationClass_supportsMultipleLibrariesWithSameComponentNames() {
        final RegistrationClassGenerator waGenerator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );
        final RegistrationClassGenerator slGenerator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.shoelace",
            "shoelace"
        );

        // Both libraries have a "button" component with different tag prefixes
        final List<ComponentDefinition> waComponents = List.of(createComponent("wa-button"));
        final List<ComponentDefinition> slComponents = List.of(createComponent("sl-button"));

        final String waSource = waGenerator.generateRegistrationClass(waComponents);
        final String slSource = slGenerator.generateRegistrationClass(slComponents);

        // Different packages prevent naming conflicts
        assertTrue(waSource.contains("package com.ponysdk.core.ui.webawesome;"));
        assertTrue(slSource.contains("package com.ponysdk.core.ui.shoelace;"));
        
        // Different tag names distinguish components
        assertTrue(waSource.contains("\"wa-button\""));
        assertTrue(slSource.contains("\"sl-button\""));
        
        // Both generate PButton but in different packages
        assertTrue(waSource.contains("PButton::new"));
        assertTrue(slSource.contains("PButton::new"));
    }

    @Test
    void generateRegistrationClass_generatesValidJavaCode() {
        final RegistrationClassGenerator generator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("wa-input")
        );

        final String source = generator.generateRegistrationClass(components);

        // Basic syntax checks
        assertTrue(source.contains("package "));
        assertTrue(source.contains("import "));
        assertTrue(source.contains("public final class "));
        assertTrue(source.contains("private "));
        assertTrue(source.contains("public static "));
        assertTrue(source.contains("static {"));
        
        // Verify proper closing braces
        int openBraces = countOccurrences(source, "{");
        int closeBraces = countOccurrences(source, "}");
        assertEquals(openBraces, closeBraces, "Braces should be balanced");
    }

    private ComponentDefinition createComponent(final String tagName) {
        return new ComponentDefinition(
            tagName,
            "Component",
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

    private int countOccurrences(final String text, final String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
