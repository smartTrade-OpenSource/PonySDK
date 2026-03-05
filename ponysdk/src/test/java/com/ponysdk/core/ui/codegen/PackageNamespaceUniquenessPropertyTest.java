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

import com.ponysdk.core.ui.codegen.generator.CodeGeneratorImpl;
import com.ponysdk.core.ui.codegen.model.ComponentDefinition;
import net.jqwik.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for package namespace uniqueness across multiple libraries.
 * <p>
 * <b>Property 10: Package Namespace Uniqueness</b>
 * </p>
 * <p>
 * <b>Validates: Requirements 9.2, 9.3, 9.4</b>
 * </p>
 * <p>
 * For any two libraries with components having the same tag name, the generated
 * wrapper classes should be in different Java packages, preventing naming conflicts.
 * </p>
 */
@Tag("Feature: generic-webcomponent-wrapper, Property 10: Package Namespace Uniqueness")
public class PackageNamespaceUniquenessPropertyTest {

    /**
     * For any multiple libraries with overlapping component names, each library
     * should generate components in its own unique package namespace.
     * <p><b>Validates: Requirements 9.2, 9.3, 9.4</b></p>
     */
    @Property(tries = 100)
    void packageNamespaceUniqueness(
        @ForAll("libraryConfigurations") List<LibraryConfiguration> libraries,
        @ForAll("componentNames") List<String> componentNames
    ) {
        // Ensure we have at least 2 libraries to test namespace separation
        Assume.that(libraries.size() >= 2);
        
        // Map to store generated code by library and component
        final Map<String, Map<String, String>> generatedCodeByLibrary = new HashMap<>();
        
        // Generate wrapper classes for each library
        for (final LibraryConfiguration library : libraries) {
            final CodeGeneratorImpl generator = new CodeGeneratorImpl(library.javaPackage());
            final Map<String, String> libraryCode = new HashMap<>();
            
            for (final String componentName : componentNames) {
                final String tagName = library.prefix() + "-" + componentName;
                final ComponentDefinition component = createComponent(tagName, componentName);
                final String generatedCode = generator.generateWrapperClass(component);
                
                libraryCode.put(componentName, generatedCode);
            }
            
            generatedCodeByLibrary.put(library.name(), libraryCode);
        }
        
        // Verify package namespace uniqueness
        verifyPackageNamespaceUniqueness(libraries, generatedCodeByLibrary);
        
        // Verify components with same names are distinguished by package
        verifyComponentDistinction(libraries, componentNames, generatedCodeByLibrary);
        
        // Verify tag names distinguish components
        verifyTagNameDistinction(libraries, componentNames, generatedCodeByLibrary);
    }

    /**
     * Verifies that each library generates code in its own unique package.
     */
    private void verifyPackageNamespaceUniqueness(
        final List<LibraryConfiguration> libraries,
        final Map<String, Map<String, String>> generatedCodeByLibrary
    ) {
        for (final LibraryConfiguration library : libraries) {
            final Map<String, String> libraryCode = generatedCodeByLibrary.get(library.name());
            
            for (final String code : libraryCode.values()) {
                assertTrue(
                    code.contains("package " + library.javaPackage() + ";"),
                    String.format("Library '%s' should generate code in package '%s'",
                        library.name(), library.javaPackage())
                );
            }
        }
    }

    /**
     * Verifies that components with the same name in different libraries
     * are distinguished by their package namespace.
     */
    private void verifyComponentDistinction(
        final List<LibraryConfiguration> libraries,
        final List<String> componentNames,
        final Map<String, Map<String, String>> generatedCodeByLibrary
    ) {
        // For each component name, collect all packages that generated it
        for (final String componentName : componentNames) {
            final Set<String> packagesForComponent = new HashSet<>();
            
            for (final LibraryConfiguration library : libraries) {
                final Map<String, String> libraryCode = generatedCodeByLibrary.get(library.name());
                final String code = libraryCode.get(componentName);
                
                if (code != null) {
                    packagesForComponent.add(library.javaPackage());
                }
            }
            
            // All packages should be unique (no duplicates)
            assertEquals(
                libraries.size(),
                packagesForComponent.size(),
                String.format("Component '%s' should be generated in %d different packages, one per library",
                    componentName, libraries.size())
            );
        }
    }

    /**
     * Verifies that components are distinguished by their tag names,
     * which include the library prefix.
     */
    private void verifyTagNameDistinction(
        final List<LibraryConfiguration> libraries,
        final List<String> componentNames,
        final Map<String, Map<String, String>> generatedCodeByLibrary
    ) {
        // Collect all tag names across all libraries
        final Set<String> allTagNames = new HashSet<>();
        
        for (final LibraryConfiguration library : libraries) {
            final Map<String, String> libraryCode = generatedCodeByLibrary.get(library.name());
            
            for (final String componentName : componentNames) {
                final String code = libraryCode.get(componentName);
                final String expectedTagName = library.prefix() + "-" + componentName;
                
                assertTrue(
                    code.contains("TAG_NAME = \"" + expectedTagName + "\""),
                    String.format("Component '%s' in library '%s' should have tag name '%s'",
                        componentName, library.name(), expectedTagName)
                );
                
                allTagNames.add(expectedTagName);
            }
        }
        
        // All tag names should be unique
        final int expectedTagNameCount = libraries.size() * componentNames.size();
        assertEquals(
            expectedTagNameCount,
            allTagNames.size(),
            String.format("Expected %d unique tag names across all libraries and components",
                expectedTagNameCount)
        );
    }

    /**
     * Additional property: Verify that the same component name in different
     * libraries generates the same class name but in different packages.
     */
    @Property(tries = 100)
    void sameComponentNameDifferentPackages(
        @ForAll("libraryConfigurations") List<LibraryConfiguration> libraries,
        @ForAll("singleComponentName") String componentName
    ) {
        Assume.that(libraries.size() >= 2);
        
        final Set<String> packages = new HashSet<>();
        final Set<String> classNames = new HashSet<>();
        final Set<String> tagNames = new HashSet<>();
        
        for (final LibraryConfiguration library : libraries) {
            final CodeGeneratorImpl generator = new CodeGeneratorImpl(library.javaPackage());
            final String tagName = library.prefix() + "-" + componentName;
            final ComponentDefinition component = createComponent(tagName, componentName);
            final String code = generator.generateWrapperClass(component);
            
            // Extract package
            assertTrue(code.contains("package " + library.javaPackage() + ";"));
            packages.add(library.javaPackage());
            
            // Extract class name (should be the same across libraries)
            final String expectedClassName = "P" + capitalize(componentName);
            assertTrue(code.contains("public class " + expectedClassName + " extends"));
            classNames.add(expectedClassName);
            
            // Extract tag name (should be different due to prefix)
            assertTrue(code.contains("TAG_NAME = \"" + tagName + "\""));
            tagNames.add(tagName);
        }
        
        // All packages should be different
        assertEquals(libraries.size(), packages.size(),
            "Each library should use a different package");
        
        // All class names should be the same (but in different packages)
        assertEquals(1, classNames.size(),
            "All libraries should generate the same class name for the same component");
        
        // All tag names should be different (due to library prefix)
        assertEquals(libraries.size(), tagNames.size(),
            "Each library should use a different tag name (with prefix)");
    }

    /**
     * Property: Verify that deep package hierarchies work correctly.
     */
    @Property(tries = 50)
    void deepPackageHierarchiesPreventConflicts(
        @ForAll("deepPackageNames") List<String> packages,
        @ForAll("singleComponentName") String componentName
    ) {
        Assume.that(packages.size() >= 2);
        
        final Set<String> generatedPackages = new HashSet<>();
        
        for (final String packageName : packages) {
            final CodeGeneratorImpl generator = new CodeGeneratorImpl(packageName);
            final ComponentDefinition component = createComponent("lib-" + componentName, componentName);
            final String code = generator.generateWrapperClass(component);
            
            assertTrue(code.contains("package " + packageName + ";"),
                "Generated code should use the specified package: " + packageName);
            
            generatedPackages.add(packageName);
        }
        
        // All packages should be unique
        assertEquals(packages.size(), generatedPackages.size(),
            "All packages should be unique");
    }

    // ========== Helper Methods ==========

    private ComponentDefinition createComponent(final String tagName, final String className) {
        return new ComponentDefinition(
            tagName,
            capitalize(className),
            "A " + className + " component",
            "",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "stable"
        );
    }

    private String capitalize(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<List<LibraryConfiguration>> libraryConfigurations() {
        return Arbitraries.integers().between(2, 4)
            .flatMap(count -> {
                final List<Arbitrary<LibraryConfiguration>> libraryArbitraries = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    libraryArbitraries.add(libraryConfiguration(i));
                }
                return Combinators.combine(libraryArbitraries).as(libs -> libs);
            });
    }

    @Provide
    Arbitrary<LibraryConfiguration> libraryConfiguration(final int index) {
        final List<String> libraryNames = List.of("webawesome", "shoelace", "material", "carbon");
        final List<String> prefixes = List.of("wa", "sl", "md", "cb");
        final List<String> basePackages = List.of(
            "com.ponysdk.core.ui.webawesome",
            "com.ponysdk.core.ui.shoelace",
            "com.ponysdk.core.ui.material",
            "com.ponysdk.core.ui.carbon"
        );
        
        final int idx = index % libraryNames.size();
        return Arbitraries.just(new LibraryConfiguration(
            libraryNames.get(idx),
            prefixes.get(idx),
            basePackages.get(idx)
        ));
    }

    @Provide
    Arbitrary<List<String>> componentNames() {
        final Arbitrary<String> componentName = Arbitraries.of(
            "button", "input", "card", "dialog", "menu",
            "table", "form", "select", "checkbox", "radio"
        );
        
        return componentName.list().ofMinSize(2).ofMaxSize(5).uniqueElements();
    }

    @Provide
    Arbitrary<String> singleComponentName() {
        return Arbitraries.of(
            "button", "input", "card", "dialog", "menu",
            "table", "form", "select", "checkbox", "radio"
        );
    }

    @Provide
    Arbitrary<List<String>> deepPackageNames() {
        final Arbitrary<String> basePackage = Arbitraries.of(
            "com.example.ui",
            "org.company.components",
            "io.framework.widgets"
        );
        
        final Arbitrary<String> subPackage = Arbitraries.of(
            "lib1", "lib2", "lib3", "library.v1", "library.v2"
        );
        
        return Combinators.combine(basePackage, subPackage)
            .as((base, sub) -> base + "." + sub)
            .list()
            .ofMinSize(2)
            .ofMaxSize(4)
            .uniqueElements();
    }

    // ========== Data Classes ==========

    /**
     * Configuration for a component library.
     *
     * @param name        the library name (e.g., "webawesome")
     * @param prefix      the tag name prefix (e.g., "wa")
     * @param javaPackage the Java package for generated code
     */
    record LibraryConfiguration(
        String name,
        String prefix,
        String javaPackage
    ) {}
}
