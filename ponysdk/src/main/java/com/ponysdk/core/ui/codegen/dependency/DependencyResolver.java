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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves component dependencies and determines generation order.
 * <p>
 * This class analyzes component definitions to extract type references from properties
 * and methods, builds a dependency graph, detects circular dependencies, and performs
 * topological sorting to determine the correct generation order.
 * </p>
 * <p>
 * Components must be generated in dependency order to ensure that all referenced types
 * are available during compilation.
 * </p>
 */
public class DependencyResolver {

    /**
     * Resolves dependencies and returns components in generation order.
     * <p>
     * This method:
     * </p>
     * <ol>
     *   <li>Builds a dependency graph from component definitions</li>
     *   <li>Detects circular dependencies using DFS</li>
     *   <li>Performs topological sort using Kahn's algorithm</li>
     *   <li>Returns components in dependency order</li>
     * </ol>
     *
     * @param components the list of component definitions to resolve
     * @return components ordered by dependencies (dependencies first)
     * @throws CircularDependencyException if circular dependencies are detected
     */
    public List<ComponentDefinition> resolve(final List<ComponentDefinition> components) {
        if (components == null || components.isEmpty()) {
            return List.of();
        }

        // Build dependency graph
        final DependencyGraph graph = buildDependencyGraph(components);

        // Detect circular dependencies
        final List<List<String>> cycles = detectCycles(graph);
        if (!cycles.isEmpty()) {
            throw new CircularDependencyException(formatCycles(cycles));
        }

        // Perform topological sort
        return topologicalSort(components, graph);
    }

    /**
     * Performs topological sort on components (public API for pipeline).
     * <p>
     * This is a convenience method that builds the dependency graph internally
     * and performs the sort. It's equivalent to calling {@link #resolve(List)}.
     * </p>
     *
     * @param components the list of component definitions to sort
     * @return components ordered by dependencies (dependencies first)
     * @throws CircularDependencyException if circular dependencies are detected
     */
    public List<ComponentDefinition> topologicalSort(final List<ComponentDefinition> components) {
        return resolve(components);
    }

    /**
     * Builds a dependency graph from component definitions.
     * <p>
     * Extracts type references from:
     * </p>
     * <ul>
     *   <li>Property types (javaType field)</li>
     *   <li>Method parameter types</li>
     *   <li>Method return types</li>
     * </ul>
     *
     * @param components the component definitions
     * @return the dependency graph
     */
    DependencyGraph buildDependencyGraph(final List<ComponentDefinition> components) {
        final Map<String, Set<String>> adjacencyList = new HashMap<>();
        final Map<String, ComponentDefinition> componentMap = new HashMap<>();

        // Initialize graph nodes
        for (final var component : components) {
            final String className = component.getWrapperClassName();
            adjacencyList.put(className, new HashSet<>());
            componentMap.put(className, component);
        }

        // Build edges by extracting type references
        for (final var component : components) {
            final String className = component.getWrapperClassName();
            final Set<String> dependencies = adjacencyList.get(className);

            // Extract dependencies from properties
            for (final var property : component.properties()) {
                extractTypeReferences(property.javaType(), componentMap.keySet())
                    .forEach(dependencies::add);
            }

            // Extract dependencies from method parameters
            for (final var method : component.methods()) {
                for (final var param : method.parameters()) {
                    extractTypeReferences(param.javaType(), componentMap.keySet())
                        .forEach(dependencies::add);
                }
            }
        }

        return new DependencyGraph(adjacencyList, componentMap);
    }

    /**
     * Extracts component type references from a Java type string.
     * <p>
     * Handles:
     * </p>
     * <ul>
     *   <li>Simple types: {@code "PButton"} → {@code ["PButton"]}</li>
     *   <li>Generic types: {@code "List<PButton>"} → {@code ["PButton"]}</li>
     *   <li>Optional types: {@code "Optional<PButton>"} → {@code ["PButton"]}</li>
     *   <li>Nested generics: {@code "List<Optional<PButton>>"} → {@code ["PButton"]}</li>
     * </ul>
     *
     * @param javaType       the Java type string
     * @param componentNames the set of known component class names
     * @return set of referenced component class names
     */
    Set<String> extractTypeReferences(final String javaType, final Set<String> componentNames) {
        final Set<String> references = new HashSet<>();

        if (javaType == null || javaType.isEmpty()) {
            return references;
        }

        // Remove generic type parameters and extract inner types
        final String cleanType = javaType.replaceAll("[<>]", " ");
        final String[] parts = cleanType.split("\\s+");

        for (final String part : parts) {
            if (componentNames.contains(part)) {
                references.add(part);
            }
        }

        return references;
    }

    /**
     * Detects circular dependencies using depth-first search.
     * <p>
     * Uses DFS with three colors:
     * </p>
     * <ul>
     *   <li>WHITE: unvisited</li>
     *   <li>GRAY: currently being visited (in the recursion stack)</li>
     *   <li>BLACK: fully visited</li>
     * </ul>
     * <p>
     * A back edge (edge to a GRAY node) indicates a cycle.
     * </p>
     *
     * @param graph the dependency graph
     * @return list of cycles, where each cycle is a list of component names
     */
    List<List<String>> detectCycles(final DependencyGraph graph) {
        final List<List<String>> cycles = new ArrayList<>();
        final Map<String, NodeColor> colors = new HashMap<>();
        final Deque<String> path = new ArrayDeque<>();

        // Initialize all nodes as WHITE
        for (final String node : graph.adjacencyList().keySet()) {
            colors.put(node, NodeColor.WHITE);
        }

        // Run DFS from each unvisited node
        for (final String node : graph.adjacencyList().keySet()) {
            if (colors.get(node) == NodeColor.WHITE) {
                dfsDetectCycle(node, graph, colors, path, cycles);
            }
        }

        return cycles;
    }

    /**
     * DFS helper for cycle detection.
     *
     * @param node   the current node
     * @param graph  the dependency graph
     * @param colors the color map for tracking visit status
     * @param path   the current DFS path
     * @param cycles the list to collect detected cycles
     */
    private void dfsDetectCycle(
        final String node,
        final DependencyGraph graph,
        final Map<String, NodeColor> colors,
        final Deque<String> path,
        final List<List<String>> cycles
    ) {
        colors.put(node, NodeColor.GRAY);
        path.addLast(node);

        for (final String neighbor : graph.adjacencyList().get(node)) {
            final NodeColor neighborColor = colors.get(neighbor);

            if (neighborColor == NodeColor.GRAY) {
                // Back edge detected - cycle found
                final List<String> cycle = extractCycle(path, neighbor);
                cycles.add(cycle);
            } else if (neighborColor == NodeColor.WHITE) {
                dfsDetectCycle(neighbor, graph, colors, path, cycles);
            }
        }

        path.removeLast();
        colors.put(node, NodeColor.BLACK);
    }

    /**
     * Extracts a cycle from the DFS path.
     *
     * @param path       the current DFS path
     * @param cycleStart the node where the cycle starts
     * @return the cycle as a list of node names
     */
    private List<String> extractCycle(final Deque<String> path, final String cycleStart) {
        final List<String> cycle = new ArrayList<>();
        boolean inCycle = false;

        for (final String node : path) {
            if (node.equals(cycleStart)) {
                inCycle = true;
            }
            if (inCycle) {
                cycle.add(node);
            }
        }

        cycle.add(cycleStart); // Close the cycle
        return cycle;
    }

    /**
     * Performs topological sort using Kahn's algorithm.
     * <p>
     * Algorithm:
     * </p>
     * <ol>
     *   <li>Calculate in-degree for each node</li>
     *   <li>Add all nodes with in-degree 0 to a queue</li>
     *   <li>While queue is not empty:
     *     <ul>
     *       <li>Remove a node from queue and add to result</li>
     *       <li>For each neighbor, decrement in-degree</li>
     *       <li>If neighbor's in-degree becomes 0, add to queue</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param components the original component list
     * @param graph      the dependency graph
     * @return components in topological order
     */
    List<ComponentDefinition> topologicalSort(
        final List<ComponentDefinition> components,
        final DependencyGraph graph
    ) {
        // Calculate in-degrees
        final Map<String, Integer> inDegree = new HashMap<>();
        for (final String node : graph.adjacencyList().keySet()) {
            inDegree.put(node, 0);
        }
        for (final Set<String> neighbors : graph.adjacencyList().values()) {
            for (final String neighbor : neighbors) {
                inDegree.merge(neighbor, 1, Integer::sum);
            }
        }

        // Initialize queue with nodes having in-degree 0
        final Queue<String> queue = new LinkedList<>();
        for (final Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        // Process nodes in topological order
        final List<String> sortedNames = new ArrayList<>();
        while (!queue.isEmpty()) {
            final String node = queue.poll();
            sortedNames.add(node);

            // Reduce in-degree for neighbors
            for (final String neighbor : graph.adjacencyList().get(node)) {
                final int newInDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newInDegree);

                if (newInDegree == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // Map sorted names back to ComponentDefinition objects
        return sortedNames.stream()
            .map(name -> graph.componentMap().get(name))
            .collect(Collectors.toList());
    }

    /**
     * Formats cycles for error message.
     *
     * @param cycles the list of cycles
     * @return formatted error message
     */
    private String formatCycles(final List<List<String>> cycles) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Circular dependencies detected:\n");

        for (int i = 0; i < cycles.size(); i++) {
            final List<String> cycle = cycles.get(i);
            sb.append("  Cycle ").append(i + 1).append(": ");
            sb.append(String.join(" -> ", cycle));
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Node colors for DFS cycle detection.
     */
    private enum NodeColor {
        WHITE,  // Unvisited
        GRAY,   // Currently being visited
        BLACK   // Fully visited
    }

    /**
     * Dependency graph representation.
     *
     * @param adjacencyList map from component name to set of dependencies
     * @param componentMap  map from component name to ComponentDefinition
     */
    record DependencyGraph(
        Map<String, Set<String>> adjacencyList,
        Map<String, ComponentDefinition> componentMap
    ) {}
}
