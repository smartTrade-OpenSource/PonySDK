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

import com.ponysdk.core.ui.codegen.dependency.CircularDependencyException;
import com.ponysdk.core.ui.codegen.dependency.DependencyResolver;
import com.ponysdk.core.ui.codegen.model.ComponentDefinition;
import com.ponysdk.core.ui.codegen.model.MethodDef;
import com.ponysdk.core.ui.codegen.model.ParameterDef;
import com.ponysdk.core.ui.codegen.model.PropertyDef;
import net.jqwik.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for dependency order generation.
 * <p>
 * <b>Property 15: Dependency Order Generation</b>
 * </p>
 * <p>
 * <b>Validates: Requirements 19.4</b>
 * </p>
 * <p>
 * For any set of component definitions with dependencies, components should be
 * generated in dependency order such that all referenced types are available
 * during compilation.
 * </p>
 */
@Tag("Feature: generic-webcomponent-wrapper, Property 15: Dependency Order Generation")
public class DependencyOrderGenerationPropertyTest {

    private final DependencyResolver resolver = new DependencyResolver();

    /**
     * For any set of components with dependencies, the topological sort should
     * produce an order where all dependencies come before their dependents.
     * <p><b>Validates: Requirements 19.4</b></p>
     */
    @Property(tries = 100)
    void dependencyOrderAllowsCompilation(
        @ForAll("componentGraphs") ComponentGraph graph
    ) throws CircularDependencyException {
        // Sort components by dependency order
        final List<ComponentDefinition> sorted = resolver.topologicalSort(graph.components());
        
        // Verify all components are present
        assertEquals(graph.components().size(), sorted.size(),
            "All components should be in the sorted list");
        
        // Build a set of already-seen component names as we iterate
        final Set<String> availableTypes = new HashSet<>();
        
        // Verify dependency order: each component's dependencies must appear before it
        for (final ComponentDefinition component : sorted) {
            final Set<String> dependencies = extractDependencies(component, graph.componentNames());
            
            for (final String dependency : dependencies) {
                assertTrue(
                    availableTypes.contains(dependency),
                    String.format(
                        "Component '%s' depends on '%s', but '%s' has not been generated yet. " +
                        "Generation order is invalid.",
                        component.className(), dependency, dependency
                    )
                );
            }
            
            // Mark this component as available for subsequent components
            availableTypes.add(component.className());
        }
    }

    /**
     * For any acyclic dependency graph, the topological sort should succeed
     * and produce a valid ordering.
     * <p><b>Validates: Requirements 19.4</b></p>
     */
    @Property(tries = 100)
    void acyclicGraphsProduceValidOrdering(
        @ForAll("acyclicComponentGraphs") ComponentGraph graph
    ) {
        assertDoesNotThrow(
            () -> {
                final List<ComponentDefinition> sorted = resolver.topologicalSort(graph.components());
                
                // Verify the ordering is valid
                verifyValidOrdering(sorted, graph);
            },
            "Acyclic graphs should always produce a valid topological ordering"
        );
    }

    /**
     * For any linear dependency chain (A -> B -> C), the sort should produce
     * the correct order (C, B, A).
     * <p><b>Validates: Requirements 19.4</b></p>
     */
    @Property(tries = 100)
    void linearDependencyChainProducesCorrectOrder(
        @ForAll("linearChainLength") int chainLength
    ) throws CircularDependencyException {
        Assume.that(chainLength >= 2 && chainLength <= 10);
        
        // Create a linear chain: Component0 -> Component1 -> ... -> ComponentN
        final List<ComponentDefinition> components = new ArrayList<>();
        
        for (int i = 0; i < chainLength; i++) {
            final String className = "Component" + i;
            final List<PropertyDef> properties = new ArrayList<>();
            
            // Each component depends on the next one (except the last)
            if (i < chainLength - 1) {
                final String nextClassName = "Component" + (i + 1);
                properties.add(createProperty("dependency", nextClassName));
            }
            
            components.add(createComponent(className, properties, Collections.emptyList()));
        }
        
        // Sort the components
        final List<ComponentDefinition> sorted = resolver.topologicalSort(components);
        
        // Verify the order: last component should come first, first component should come last
        assertEquals("Component" + (chainLength - 1), sorted.get(0).className(),
            "Last component in chain should be generated first (no dependencies)");
        assertEquals("Component0", sorted.get(chainLength - 1).className(),
            "First component in chain should be generated last (depends on all others)");
        
        // Verify complete ordering
        for (int i = 0; i < chainLength; i++) {
            assertEquals("Component" + (chainLength - 1 - i), sorted.get(i).className(),
                "Components should be in reverse order of dependency chain");
        }
    }

    /**
     * For any diamond dependency graph (D -> B, D -> C, B -> A, C -> A),
     * the sort should ensure A comes before both B and C, and both B and C
     * come before D.
     * <p><b>Validates: Requirements 19.4</b></p>
     */
    @Property(tries = 100)
    void diamondDependencyProducesValidOrder(
        @ForAll("componentNames") List<String> names
    ) throws CircularDependencyException {
        Assume.that(names.size() >= 4);
        
        final String nameA = names.get(0);
        final String nameB = names.get(1);
        final String nameC = names.get(2);
        final String nameD = names.get(3);
        
        // Create diamond: D -> B, D -> C, B -> A, C -> A
        final ComponentDefinition a = createComponent(nameA, Collections.emptyList(), Collections.emptyList());
        final ComponentDefinition b = createComponent(nameB, List.of(createProperty("a", nameA)), Collections.emptyList());
        final ComponentDefinition c = createComponent(nameC, List.of(createProperty("a", nameA)), Collections.emptyList());
        final ComponentDefinition d = createComponent(nameD, 
            List.of(createProperty("b", nameB), createProperty("c", nameC)), 
            Collections.emptyList());
        
        final List<ComponentDefinition> sorted = resolver.topologicalSort(List.of(d, c, b, a));
        
        // Find indices
        final int indexA = findIndex(sorted, nameA);
        final int indexB = findIndex(sorted, nameB);
        final int indexC = findIndex(sorted, nameC);
        final int indexD = findIndex(sorted, nameD);
        
        // Verify ordering constraints
        assertTrue(indexA < indexB, "A should come before B");
        assertTrue(indexA < indexC, "A should come before C");
        assertTrue(indexB < indexD, "B should come before D");
        assertTrue(indexC < indexD, "C should come before D");
    }

    /**
     * For any set of independent components (no dependencies), any ordering
     * should be valid.
     * <p><b>Validates: Requirements 19.4</b></p>
     */
    @Property(tries = 100)
    void independentComponentsProduceAnyValidOrder(
        @ForAll("independentComponents") List<ComponentDefinition> components
    ) throws CircularDependencyException {
        Assume.that(components.size() >= 2);
        
        final List<ComponentDefinition> sorted = resolver.topologicalSort(components);
        
        // All components should be present
        assertEquals(components.size(), sorted.size());
        
        // All original components should be in the sorted list
        final Set<String> originalNames = components.stream()
            .map(ComponentDefinition::className)
            .collect(Collectors.toSet());
        final Set<String> sortedNames = sorted.stream()
            .map(ComponentDefinition::className)
            .collect(Collectors.toSet());
        
        assertEquals(originalNames, sortedNames,
            "Sorted list should contain exactly the same components");
    }

    /**
     * For any component graph with method dependencies, the sort should
     * respect dependencies in method parameters and return types.
     * <p><b>Validates: Requirements 19.4</b></p>
     */
    @Property(tries = 100)
    void methodDependenciesRespected(
        @ForAll("componentNames") List<String> names
    ) throws CircularDependencyException {
        Assume.that(names.size() >= 3);
        
        final String nameA = names.get(0);
        final String nameB = names.get(1);
        final String nameC = names.get(2);
        
        // A has no dependencies
        final ComponentDefinition a = createComponent(nameA, Collections.emptyList(), Collections.emptyList());
        
        // B has a method that returns A
        final MethodDef methodReturningA = new MethodDef(
            "getA",
            "Returns an A",
            Collections.emptyList(),
            nameA,
            false
        );
        final ComponentDefinition b = createComponent(nameB, Collections.emptyList(), List.of(methodReturningA));
        
        // C has a method that takes B as parameter
        final ParameterDef paramB = new ParameterDef("b", nameB, nameB, "B parameter");
        final MethodDef methodTakingB = new MethodDef(
            "processB",
            "Processes B",
            List.of(paramB),
            "void",
            false
        );
        final ComponentDefinition c = createComponent(nameC, Collections.emptyList(), List.of(methodTakingB));
        
        final List<ComponentDefinition> sorted = resolver.topologicalSort(List.of(c, b, a));
        
        // Find indices
        final int indexA = findIndex(sorted, nameA);
        final int indexB = findIndex(sorted, nameB);
        final int indexC = findIndex(sorted, nameC);
        
        // Verify ordering: A < B < C
        assertTrue(indexA < indexB, "A should come before B (B's method returns A)");
        assertTrue(indexB < indexC, "B should come before C (C's method takes B)");
    }

    // ========== Helper Methods ==========

    /**
     * Extracts all dependencies from a component definition.
     */
    private Set<String> extractDependencies(
        final ComponentDefinition component,
        final Set<String> allComponentNames
    ) {
        final Set<String> dependencies = new HashSet<>();
        
        // Extract from properties
        for (final PropertyDef prop : component.properties()) {
            if (allComponentNames.contains(prop.javaType())) {
                dependencies.add(prop.javaType());
            }
            // Handle generic types like List<Component>
            if (prop.javaType().contains("<") && prop.javaType().contains(">")) {
                final String genericType = extractGenericType(prop.javaType());
                if (allComponentNames.contains(genericType)) {
                    dependencies.add(genericType);
                }
            }
        }
        
        // Extract from methods
        for (final MethodDef method : component.methods()) {
            if (allComponentNames.contains(method.returnType())) {
                dependencies.add(method.returnType());
            }
            for (final ParameterDef param : method.parameters()) {
                if (allComponentNames.contains(param.javaType())) {
                    dependencies.add(param.javaType());
                }
            }
        }
        
        return dependencies;
    }

    /**
     * Extracts generic type from parameterized type string.
     */
    private String extractGenericType(final String type) {
        final int start = type.indexOf('<');
        final int end = type.lastIndexOf('>');
        if (start != -1 && end != -1 && end > start) {
            return type.substring(start + 1, end).trim();
        }
        return "";
    }

    /**
     * Verifies that the sorted list represents a valid topological ordering.
     */
    private void verifyValidOrdering(
        final List<ComponentDefinition> sorted,
        final ComponentGraph graph
    ) {
        final Set<String> seen = new HashSet<>();
        
        for (final ComponentDefinition component : sorted) {
            final Set<String> deps = extractDependencies(component, graph.componentNames());
            
            for (final String dep : deps) {
                assertTrue(seen.contains(dep),
                    String.format("Dependency '%s' of component '%s' should appear earlier in the ordering",
                        dep, component.className()));
            }
            
            seen.add(component.className());
        }
    }

    /**
     * Finds the index of a component by name in the sorted list.
     */
    private int findIndex(final List<ComponentDefinition> components, final String className) {
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i).className().equals(className)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Creates a component definition for testing.
     */
    private ComponentDefinition createComponent(
        final String className,
        final List<PropertyDef> properties,
        final List<MethodDef> methods
    ) {
        return new ComponentDefinition(
            "lib-" + className.toLowerCase(),
            className,
            "A " + className + " component",
            "",
            properties,
            Collections.emptyList(),
            Collections.emptyList(),
            methods,
            Collections.emptyList(),
            "stable"
        );
    }

    /**
     * Creates a property definition for testing.
     */
    private PropertyDef createProperty(final String name, final String javaType) {
        return new PropertyDef(
            name,
            javaType,
            javaType,
            javaType.toLowerCase(),
            "A " + name + " property",
            null,
            false,
            "public"
        );
    }

    // ========== Arbitraries ==========

    @Provide
    Arbitrary<ComponentGraph> componentGraphs() {
        return Combinators.combine(
            Arbitraries.integers().between(3, 8),
            Arbitraries.doubles().between(0.0, 0.5)
        ).flatAs((componentCount, dependencyProbability) -> {
            // Generate component names
            final List<String> names = new ArrayList<>();
            for (int i = 0; i < componentCount; i++) {
                names.add("Component" + i);
            }
            
            // Generate components with random dependencies
            final List<ComponentDefinition> components = new ArrayList<>();
            final Random random = new Random();
            
            for (int i = 0; i < componentCount; i++) {
                final String className = names.get(i);
                final List<PropertyDef> properties = new ArrayList<>();
                
                // Add dependencies to earlier components (to avoid cycles)
                for (int j = i + 1; j < componentCount; j++) {
                    if (random.nextDouble() < dependencyProbability) {
                        properties.add(createProperty("dep" + j, names.get(j)));
                    }
                }
                
                components.add(createComponent(className, properties, Collections.emptyList()));
            }
            
            return Arbitraries.just(new ComponentGraph(components, new HashSet<>(names)));
        });
    }

    @Provide
    Arbitrary<ComponentGraph> acyclicComponentGraphs() {
        // Same as componentGraphs but guaranteed acyclic by construction
        // (only allowing dependencies to components with higher indices)
        return componentGraphs();
    }

    @Provide
    Arbitrary<Integer> linearChainLength() {
        return Arbitraries.integers().between(2, 10);
    }

    @Provide
    Arbitrary<List<String>> componentNames() {
        return Arbitraries.of(
            "Button", "Input", "Card", "Dialog", "Menu",
            "Table", "Form", "Select", "Checkbox", "Radio",
            "Badge", "Icon", "Tooltip", "Modal", "Drawer"
        ).list().ofMinSize(2).ofMaxSize(8).uniqueElements();
    }

    @Provide
    Arbitrary<List<ComponentDefinition>> independentComponents() {
        return Arbitraries.integers().between(2, 10)
            .flatMap(count -> {
                final List<ComponentDefinition> components = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    components.add(createComponent(
                        "Component" + i,
                        Collections.emptyList(),
                        Collections.emptyList()
                    ));
                }
                return Arbitraries.just(components);
            });
    }

    // ========== Data Classes ==========

    /**
     * Represents a component dependency graph for testing.
     *
     * @param components     the list of components
     * @param componentNames set of all component names for dependency checking
     */
    record ComponentGraph(
        List<ComponentDefinition> components,
        Set<String> componentNames
    ) {}
}
