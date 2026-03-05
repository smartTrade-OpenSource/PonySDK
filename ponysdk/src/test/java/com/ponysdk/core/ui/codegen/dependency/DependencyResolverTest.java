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

package com.ponysdk.core.ui.codegen.dependency;

import com.ponysdk.core.ui.codegen.model.ComponentDefinition;
import com.ponysdk.core.ui.codegen.model.MethodDef;
import com.ponysdk.core.ui.codegen.model.ParameterDef;
import com.ponysdk.core.ui.codegen.model.PropertyDef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link DependencyResolver}.
 */
class DependencyResolverTest {

    private final DependencyResolver resolver = new DependencyResolver();

    @Test
    void testResolveEmptyList() {
        final List<ComponentDefinition> result = resolver.resolve(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void testResolveNullList() {
        final List<ComponentDefinition> result = resolver.resolve(null);
        assertThat(result).isEmpty();
    }

    @Test
    void testResolveSingleComponent() {
        final ComponentDefinition button = createComponent("wa-button", List.of(), List.of());
        
        final List<ComponentDefinition> result = resolver.resolve(List.of(button));
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).tagName()).isEqualTo("wa-button");
    }

    @Test
    void testResolveNoDependencies() {
        final ComponentDefinition button = createComponent("wa-button", List.of(), List.of());
        final ComponentDefinition input = createComponent("wa-input", List.of(), List.of());
        final ComponentDefinition checkbox = createComponent("wa-checkbox", List.of(), List.of());
        
        final List<ComponentDefinition> result = resolver.resolve(List.of(button, input, checkbox));
        
        assertThat(result).hasSize(3);
        assertThat(result).extracting(ComponentDefinition::tagName)
            .containsExactlyInAnyOrder("wa-button", "wa-input", "wa-checkbox");
    }

    @Test
    void testResolveLinearDependency() {
        // PDialog depends on PButton
        final ComponentDefinition button = createComponent("wa-button", List.of(), List.of());
        final ComponentDefinition dialog = createComponent(
            "wa-dialog",
            List.of(createProperty("closeButton", "PButton")),
            List.of()
        );
        
        final List<ComponentDefinition> result = resolver.resolve(List.of(dialog, button));
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).tagName()).isEqualTo("wa-button");
        assertThat(result.get(1).tagName()).isEqualTo("wa-dialog");
    }

    @Test
    void testResolveChainedDependencies() {
        // PForm depends on PInput, PInput depends on PButton
        final ComponentDefinition button = createComponent("wa-button", List.of(), List.of());
        final ComponentDefinition input = createComponent(
            "wa-input",
            List.of(createProperty("clearButton", "PButton")),
            List.of()
        );
        final ComponentDefinition form = createComponent(
            "wa-form",
            List.of(createProperty("submitInput", "PInput")),
            List.of()
        );
        
        final List<ComponentDefinition> result = resolver.resolve(List.of(form, input, button));
        
        assertThat(result).hasSize(3);
        assertThat(result.get(0).tagName()).isEqualTo("wa-button");
        assertThat(result.get(1).tagName()).isEqualTo("wa-input");
        assertThat(result.get(2).tagName()).isEqualTo("wa-form");
    }

    @Test
    void testResolveDiamondDependency() {
        // PForm depends on PButton and PInput
        // PInput depends on PButton
        final ComponentDefinition button = createComponent("wa-button", List.of(), List.of());
        final ComponentDefinition input = createComponent(
            "wa-input",
            List.of(createProperty("clearButton", "PButton")),
            List.of()
        );
        final ComponentDefinition form = createComponent(
            "wa-form",
            List.of(
                createProperty("submitButton", "PButton"),
                createProperty("emailInput", "PInput")
            ),
            List.of()
        );
        
        final List<ComponentDefinition> result = resolver.resolve(List.of(form, input, button));
        
        assertThat(result).hasSize(3);
        assertThat(result.get(0).tagName()).isEqualTo("wa-button");
        assertThat(result.get(1).tagName()).isEqualTo("wa-input");
        assertThat(result.get(2).tagName()).isEqualTo("wa-form");
    }

    @Test
    void testResolveMethodParameterDependency() {
        final ComponentDefinition button = createComponent("wa-button", List.of(), List.of());
        final ComponentDefinition dialog = createComponent(
            "wa-dialog",
            List.of(),
            List.of(createMethod("addButton", List.of(createParameter("button", "PButton"))))
        );
        
        final List<ComponentDefinition> result = resolver.resolve(List.of(dialog, button));
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).tagName()).isEqualTo("wa-button");
        assertThat(result.get(1).tagName()).isEqualTo("wa-dialog");
    }

    @Test
    void testResolveGenericTypeDependency() {
        final ComponentDefinition button = createComponent("wa-button", List.of(), List.of());
        final ComponentDefinition toolbar = createComponent(
            "wa-toolbar",
            List.of(createProperty("buttons", "List<PButton>")),
            List.of()
        );
        
        final List<ComponentDefinition> result = resolver.resolve(List.of(toolbar, button));
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).tagName()).isEqualTo("wa-button");
        assertThat(result.get(1).tagName()).isEqualTo("wa-toolbar");
    }

    @Test
    void testResolveOptionalTypeDependency() {
        final ComponentDefinition button = createComponent("wa-button", List.of(), List.of());
        final ComponentDefinition dialog = createComponent(
            "wa-dialog",
            List.of(createProperty("closeButton", "Optional<PButton>")),
            List.of()
        );
        
        final List<ComponentDefinition> result = resolver.resolve(List.of(dialog, button));
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).tagName()).isEqualTo("wa-button");
        assertThat(result.get(1).tagName()).isEqualTo("wa-dialog");
    }

    @Test
    void testResolveNestedGenericDependency() {
        final ComponentDefinition button = createComponent("wa-button", List.of(), List.of());
        final ComponentDefinition toolbar = createComponent(
            "wa-toolbar",
            List.of(createProperty("buttons", "List<Optional<PButton>>")),
            List.of()
        );
        
        final List<ComponentDefinition> result = resolver.resolve(List.of(toolbar, button));
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).tagName()).isEqualTo("wa-button");
        assertThat(result.get(1).tagName()).isEqualTo("wa-toolbar");
    }

    @Test
    void testDetectSimpleCircularDependency() {
        // PComponentA depends on PComponentB
        // PComponentB depends on PComponentA
        final ComponentDefinition componentA = createComponent(
            "wa-component-a",
            List.of(createProperty("componentB", "PComponentB")),
            List.of()
        );
        final ComponentDefinition componentB = createComponent(
            "wa-component-b",
            List.of(createProperty("componentA", "PComponentA")),
            List.of()
        );
        
        assertThatThrownBy(() -> resolver.resolve(List.of(componentA, componentB)))
            .isInstanceOf(CircularDependencyException.class)
            .hasMessageContaining("Circular dependencies detected")
            .hasMessageContaining("PComponentA")
            .hasMessageContaining("PComponentB");
    }

    @Test
    void testDetectThreeWayCircularDependency() {
        // A -> B -> C -> A
        final ComponentDefinition componentA = createComponent(
            "wa-component-a",
            List.of(createProperty("componentB", "PComponentB")),
            List.of()
        );
        final ComponentDefinition componentB = createComponent(
            "wa-component-b",
            List.of(createProperty("componentC", "PComponentC")),
            List.of()
        );
        final ComponentDefinition componentC = createComponent(
            "wa-component-c",
            List.of(createProperty("componentA", "PComponentA")),
            List.of()
        );
        
        assertThatThrownBy(() -> resolver.resolve(List.of(componentA, componentB, componentC)))
            .isInstanceOf(CircularDependencyException.class)
            .hasMessageContaining("Circular dependencies detected");
    }

    @Test
    void testDetectSelfDependency() {
        // Component depends on itself
        final ComponentDefinition component = createComponent(
            "wa-tree-node",
            List.of(createProperty("children", "List<PTreeNode>")),
            List.of()
        );
        
        assertThatThrownBy(() -> resolver.resolve(List.of(component)))
            .isInstanceOf(CircularDependencyException.class)
            .hasMessageContaining("Circular dependencies detected")
            .hasMessageContaining("PTreeNode");
    }

    @Test
    void testBuildDependencyGraph() {
        final ComponentDefinition button = createComponent("wa-button", List.of(), List.of());
        final ComponentDefinition dialog = createComponent(
            "wa-dialog",
            List.of(createProperty("closeButton", "PButton")),
            List.of()
        );
        
        final DependencyResolver.DependencyGraph graph = resolver.buildDependencyGraph(
            List.of(button, dialog)
        );
        
        assertThat(graph.adjacencyList()).containsKeys("PButton", "PDialog");
        assertThat(graph.adjacencyList().get("PButton")).isEmpty();
        assertThat(graph.adjacencyList().get("PDialog")).containsExactly("PButton");
    }

    @Test
    void testExtractTypeReferences() {
        final Set<String> componentNames = Set.of("PButton", "PInput", "PDialog");
        
        // Simple type
        assertThat(resolver.extractTypeReferences("PButton", componentNames))
            .containsExactly("PButton");
        
        // Generic type
        assertThat(resolver.extractTypeReferences("List<PButton>", componentNames))
            .containsExactly("PButton");
        
        // Optional type
        assertThat(resolver.extractTypeReferences("Optional<PInput>", componentNames))
            .containsExactly("PInput");
        
        // Nested generic
        assertThat(resolver.extractTypeReferences("List<Optional<PDialog>>", componentNames))
            .containsExactly("PDialog");
        
        // Multiple references
        assertThat(resolver.extractTypeReferences("Map<PButton, PInput>", componentNames))
            .containsExactlyInAnyOrder("PButton", "PInput");
        
        // Non-component type
        assertThat(resolver.extractTypeReferences("String", componentNames))
            .isEmpty();
        
        // Primitive type
        assertThat(resolver.extractTypeReferences("boolean", componentNames))
            .isEmpty();
        
        // Null/empty
        assertThat(resolver.extractTypeReferences(null, componentNames))
            .isEmpty();
        assertThat(resolver.extractTypeReferences("", componentNames))
            .isEmpty();
    }

    @Test
    void testDetectCycles() {
        // Create a graph with a cycle: A -> B -> C -> A
        final ComponentDefinition componentA = createComponent(
            "wa-component-a",
            List.of(createProperty("componentB", "PComponentB")),
            List.of()
        );
        final ComponentDefinition componentB = createComponent(
            "wa-component-b",
            List.of(createProperty("componentC", "PComponentC")),
            List.of()
        );
        final ComponentDefinition componentC = createComponent(
            "wa-component-c",
            List.of(createProperty("componentA", "PComponentA")),
            List.of()
        );
        
        final DependencyResolver.DependencyGraph graph = resolver.buildDependencyGraph(
            List.of(componentA, componentB, componentC)
        );
        
        final List<List<String>> cycles = resolver.detectCycles(graph);
        
        assertThat(cycles).isNotEmpty();
        assertThat(cycles.get(0)).contains("PComponentA", "PComponentB", "PComponentC");
    }

    @Test
    void testTopologicalSort() {
        // Create components: Button, Input (depends on Button), Form (depends on Input)
        final ComponentDefinition button = createComponent("wa-button", List.of(), List.of());
        final ComponentDefinition input = createComponent(
            "wa-input",
            List.of(createProperty("clearButton", "PButton")),
            List.of()
        );
        final ComponentDefinition form = createComponent(
            "wa-form",
            List.of(createProperty("submitInput", "PInput")),
            List.of()
        );
        
        final List<ComponentDefinition> components = List.of(form, input, button);
        final DependencyResolver.DependencyGraph graph = resolver.buildDependencyGraph(components);
        
        final List<ComponentDefinition> sorted = resolver.topologicalSort(components, graph);
        
        assertThat(sorted).hasSize(3);
        
        // Button should come before Input
        final int buttonIndex = findIndex(sorted, "wa-button");
        final int inputIndex = findIndex(sorted, "wa-input");
        final int formIndex = findIndex(sorted, "wa-form");
        
        assertThat(buttonIndex).isLessThan(inputIndex);
        assertThat(inputIndex).isLessThan(formIndex);
    }

    // Helper methods

    private ComponentDefinition createComponent(
        final String tagName,
        final List<PropertyDef> properties,
        final List<MethodDef> methods
    ) {
        return new ComponentDefinition(
            tagName,
            tagName.replaceFirst("^[a-z]+-", ""),
            "Summary",
            "Description",
            properties,
            List.of(),
            List.of(),
            methods,
            List.of(),
            "stable"
        );
    }

    private PropertyDef createProperty(final String name, final String javaType) {
        return new PropertyDef(
            name,
            javaType,
            javaType,
            javaType,
            "Description",
            null,
            false,
            "public"
        );
    }

    private MethodDef createMethod(final String name, final List<ParameterDef> parameters) {
        return new MethodDef(
            name,
            "Description",
            parameters,
            "void",
            false
        );
    }

    private ParameterDef createParameter(final String name, final String javaType) {
        return new ParameterDef(
            name,
            javaType,
            javaType,
            "Description"
        );
    }

    private int findIndex(final List<ComponentDefinition> list, final String tagName) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).tagName().equals(tagName)) {
                return i;
            }
        }
        return -1;
    }
}
