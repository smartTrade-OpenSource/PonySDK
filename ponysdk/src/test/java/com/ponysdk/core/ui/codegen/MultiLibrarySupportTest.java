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

import com.ponysdk.core.ui.codegen.filter.ComponentFilter;
import com.ponysdk.core.ui.codegen.generator.CodeGeneratorImpl;
import com.ponysdk.core.ui.codegen.generator.RegistrationClassGenerator;
import com.ponysdk.core.ui.codegen.model.ComponentDefinition;
import com.ponysdk.core.ui.codegen.model.FilterConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for multi-library support.
 * Tests package namespacing, component filtering, and registration class generation
 * across multiple component libraries.
 * Validates Requirements 9.1, 9.2, 9.3, 9.4, 9.5.
 */
class MultiLibrarySupportTest {

    @Test
    void packageNamespacing_preventsNamingConflictsBetweenLibraries() {
        // Create generators for two different libraries
        final CodeGeneratorImpl waGenerator = new CodeGeneratorImpl("com.ponysdk.core.ui.webawesome");
        final CodeGeneratorImpl slGenerator = new CodeGeneratorImpl("com.ponysdk.core.ui.shoelace");

        // Both libraries have a "button" component
        final ComponentDefinition waButton = createComponent("wa-button");
        final ComponentDefinition slButton = createComponent("sl-button");

        final String waSource = waGenerator.generateWrapperClass(waButton);
        final String slSource = slGenerator.generateWrapperClass(slButton);

        // Verify different packages
        assertTrue(waSource.contains("package com.ponysdk.core.ui.webawesome;"));
        assertTrue(slSource.contains("package com.ponysdk.core.ui.shoelace;"));

        // Both generate PButton class but in different packages
        assertTrue(waSource.contains("public class PButton extends PWebComponent<ButtonProps>"));
        assertTrue(slSource.contains("public class PButton extends PWebComponent<ButtonProps>"));

        // Different tag names distinguish the components
        assertTrue(waSource.contains("private static final String TAG_NAME = \"wa-button\";"));
        assertTrue(slSource.contains("private static final String TAG_NAME = \"sl-button\";"));
    }

    @Test
    void packageNamespacing_allowsSameComponentNamesInDifferentLibraries() {
        final CodeGeneratorImpl waGenerator = new CodeGeneratorImpl("com.ponysdk.core.ui.webawesome");
        final CodeGeneratorImpl slGenerator = new CodeGeneratorImpl("com.ponysdk.core.ui.shoelace");
        final CodeGeneratorImpl mdGenerator = new CodeGeneratorImpl("com.ponysdk.core.ui.material");

        // All three libraries have button, input, and card components
        final List<String> componentNames = List.of("button", "input", "card");

        for (final String name : componentNames) {
            final ComponentDefinition waComp = createComponent("wa-" + name);
            final ComponentDefinition slComp = createComponent("sl-" + name);
            final ComponentDefinition mdComp = createComponent("md-" + name);

            final String waSource = waGenerator.generateWrapperClass(waComp);
            final String slSource = slGenerator.generateWrapperClass(slComp);
            final String mdSource = mdGenerator.generateWrapperClass(mdComp);

            // All three should generate successfully with different packages
            assertTrue(waSource.contains("package com.ponysdk.core.ui.webawesome;"));
            assertTrue(slSource.contains("package com.ponysdk.core.ui.shoelace;"));
            assertTrue(mdSource.contains("package com.ponysdk.core.ui.material;"));
        }
    }

    @Test
    void componentFiltering_supportsLibrarySpecificPatterns() {
        // Create filter that only includes Web Awesome components
        final FilterConfig waConfig = new FilterConfig(
            List.of("wa-*"),
            Collections.emptyList(),
            Collections.emptyList()
        );
        final ComponentFilter waFilter = new ComponentFilter(waConfig);

        // Create filter that only includes Shoelace components
        final FilterConfig slConfig = new FilterConfig(
            List.of("sl-*"),
            Collections.emptyList(),
            Collections.emptyList()
        );
        final ComponentFilter slFilter = new ComponentFilter(slConfig);

        final List<ComponentDefinition> allComponents = List.of(
            createComponent("wa-button"),
            createComponent("wa-input"),
            createComponent("sl-button"),
            createComponent("sl-input"),
            createComponent("md-button")
        );

        final List<ComponentDefinition> waFiltered = waFilter.filter(allComponents);
        final List<ComponentDefinition> slFiltered = slFilter.filter(allComponents);

        // Web Awesome filter should only include wa-* components
        assertEquals(2, waFiltered.size());
        assertTrue(waFiltered.stream().allMatch(c -> c.tagName().startsWith("wa-")));

        // Shoelace filter should only include sl-* components
        assertEquals(2, slFiltered.size());
        assertTrue(slFiltered.stream().allMatch(c -> c.tagName().startsWith("sl-")));
    }

    @Test
    void componentFiltering_supportsMultipleLibrariesSimultaneously() {
        final FilterConfig config = new FilterConfig(
            List.of("wa-*", "sl-*"),
            Collections.emptyList(),
            Collections.emptyList()
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> allComponents = List.of(
            createComponent("wa-button"),
            createComponent("sl-input"),
            createComponent("md-card"),
            createComponent("wa-dialog"),
            createComponent("sl-menu")
        );

        final List<ComponentDefinition> filtered = filter.filter(allComponents);

        assertEquals(4, filtered.size());
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("wa-button")));
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("sl-input")));
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("wa-dialog")));
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("sl-menu")));
        assertFalse(filtered.stream().anyMatch(c -> c.tagName().equals("md-card")));
    }

    @Test
    void registrationClass_generatesUniqueRegistryPerLibrary() {
        final RegistrationClassGenerator waGenerator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );
        final RegistrationClassGenerator slGenerator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.shoelace",
            "shoelace"
        );

        final List<ComponentDefinition> waComponents = List.of(
            createComponent("wa-button"),
            createComponent("wa-input")
        );
        final List<ComponentDefinition> slComponents = List.of(
            createComponent("sl-button"),
            createComponent("sl-card")
        );

        final String waRegistry = waGenerator.generateRegistrationClass(waComponents);
        final String slRegistry = slGenerator.generateRegistrationClass(slComponents);

        // Different packages
        assertTrue(waRegistry.contains("package com.ponysdk.core.ui.webawesome;"));
        assertTrue(slRegistry.contains("package com.ponysdk.core.ui.shoelace;"));

        // Different class names
        assertTrue(waRegistry.contains("public final class WebawesomeComponentRegistry {"));
        assertTrue(slRegistry.contains("public final class ShoelaceComponentRegistry {"));

        // Different component registrations
        assertTrue(waRegistry.contains("COMPONENT_FACTORIES.put(\"wa-button\", PButton::new);"));
        assertTrue(waRegistry.contains("COMPONENT_FACTORIES.put(\"wa-input\", PInput::new);"));
        assertTrue(slRegistry.contains("COMPONENT_FACTORIES.put(\"sl-button\", PButton::new);"));
        assertTrue(slRegistry.contains("COMPONENT_FACTORIES.put(\"sl-card\", PCard::new);"));

        // Web Awesome registry should not contain Shoelace components
        assertFalse(waRegistry.contains("sl-button"));
        assertFalse(waRegistry.contains("sl-card"));

        // Shoelace registry should not contain Web Awesome components
        assertFalse(slRegistry.contains("wa-button"));
        assertFalse(slRegistry.contains("wa-input"));
    }

    @Test
    void registrationClass_distinguishesComponentsByTagName() {
        final RegistrationClassGenerator waGenerator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );
        final RegistrationClassGenerator slGenerator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.shoelace",
            "shoelace"
        );

        // Both libraries have a button component
        final List<ComponentDefinition> waComponents = List.of(createComponent("wa-button"));
        final List<ComponentDefinition> slComponents = List.of(createComponent("sl-button"));

        final String waRegistry = waGenerator.generateRegistrationClass(waComponents);
        final String slRegistry = slGenerator.generateRegistrationClass(slComponents);

        // Each registry uses the full tag name as the unique identifier
        assertTrue(waRegistry.contains("COMPONENT_FACTORIES.put(\"wa-button\","));
        assertTrue(slRegistry.contains("COMPONENT_FACTORIES.put(\"sl-button\","));

        // Tag names are different, preventing conflicts
        assertNotEquals("wa-button", "sl-button");
    }

    @Test
    void multiLibraryWorkflow_completeIntegration() {
        // Simulate a complete multi-library workflow

        // Step 1: Define components from multiple libraries
        final List<ComponentDefinition> allComponents = List.of(
            createComponent("wa-button"),
            createComponent("wa-input"),
            createComponent("wa-internal-component"),
            createComponent("sl-button"),
            createComponent("sl-card"),
            createComponent("md-button")
        );

        // Step 2: Filter components for Web Awesome library
        final FilterConfig waFilterConfig = new FilterConfig(
            List.of("wa-*"),
            List.of("*-internal*"),
            Collections.emptyList()
        );
        final ComponentFilter waFilter = new ComponentFilter(waFilterConfig);
        final List<ComponentDefinition> waComponents = waFilter.filter(allComponents);

        // Step 3: Filter components for Shoelace library
        final FilterConfig slFilterConfig = new FilterConfig(
            List.of("sl-*"),
            Collections.emptyList(),
            Collections.emptyList()
        );
        final ComponentFilter slFilter = new ComponentFilter(slFilterConfig);
        final List<ComponentDefinition> slComponents = slFilter.filter(allComponents);

        // Step 4: Generate wrapper classes for each library
        final CodeGeneratorImpl waGenerator = new CodeGeneratorImpl("com.ponysdk.core.ui.webawesome");
        final CodeGeneratorImpl slGenerator = new CodeGeneratorImpl("com.ponysdk.core.ui.shoelace");

        for (final ComponentDefinition comp : waComponents) {
            final String source = waGenerator.generateWrapperClass(comp);
            assertTrue(source.contains("package com.ponysdk.core.ui.webawesome;"));
        }

        for (final ComponentDefinition comp : slComponents) {
            final String source = slGenerator.generateWrapperClass(comp);
            assertTrue(source.contains("package com.ponysdk.core.ui.shoelace;"));
        }

        // Step 5: Generate registration classes
        final RegistrationClassGenerator waRegGenerator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.webawesome",
            "webawesome"
        );
        final RegistrationClassGenerator slRegGenerator = new RegistrationClassGenerator(
            "com.ponysdk.core.ui.shoelace",
            "shoelace"
        );

        final String waRegistry = waRegGenerator.generateRegistrationClass(waComponents);
        final String slRegistry = slRegGenerator.generateRegistrationClass(slComponents);

        // Verify results
        assertEquals(2, waComponents.size()); // wa-button, wa-input (internal excluded)
        assertEquals(2, slComponents.size()); // sl-button, sl-card

        assertTrue(waRegistry.contains("WebawesomeComponentRegistry"));
        assertTrue(slRegistry.contains("ShoelaceComponentRegistry"));

        assertTrue(waRegistry.contains("\"wa-button\""));
        assertTrue(waRegistry.contains("\"wa-input\""));
        assertFalse(waRegistry.contains("\"wa-internal-component\""));

        assertTrue(slRegistry.contains("\"sl-button\""));
        assertTrue(slRegistry.contains("\"sl-card\""));
    }

    @Test
    void packageNamespacing_supportsDeepPackageHierarchy() {
        final CodeGeneratorImpl generator1 = new CodeGeneratorImpl("com.example.ui.lib1");
        final CodeGeneratorImpl generator2 = new CodeGeneratorImpl("com.example.ui.lib2.subpackage");

        final ComponentDefinition comp1 = createComponent("lib1-button");
        final ComponentDefinition comp2 = createComponent("lib2-button");

        final String source1 = generator1.generateWrapperClass(comp1);
        final String source2 = generator2.generateWrapperClass(comp2);

        assertTrue(source1.contains("package com.example.ui.lib1;"));
        assertTrue(source2.contains("package com.example.ui.lib2.subpackage;"));
    }

    @Test
    void componentFiltering_excludesInternalComponentsAcrossLibraries() {
        final FilterConfig config = new FilterConfig(
            List.of("wa-*", "sl-*"),
            List.of("*-internal"),
            Collections.emptyList()
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> allComponents = List.of(
            createComponent("wa-button"),
            createComponent("wa-button-internal"),
            createComponent("sl-card"),
            createComponent("sl-card-internal")
        );

        final List<ComponentDefinition> filtered = filter.filter(allComponents);

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("wa-button")));
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("sl-card")));
        assertFalse(filtered.stream().anyMatch(c -> c.tagName().contains("-internal")));
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
}
