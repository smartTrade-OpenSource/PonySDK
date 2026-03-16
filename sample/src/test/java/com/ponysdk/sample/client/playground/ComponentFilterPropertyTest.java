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

package com.ponysdk.sample.client.playground;

import static org.assertj.core.api.Assertions.*;

import net.jqwik.api.*;
import org.junit.jupiter.api.Tag;

import java.util.List;

/**
 * Property-based tests for ComponentFilter.
 * <p>
 * Tests universal properties of the filter logic across randomly generated inputs.
 * </p>
 */
public class ComponentFilterPropertyTest {

    /**
     * Property 1: Filter Returns Only Matching Components
     * <p>
     * For any list of component names and any search query, the filtered result
     * should contain only components where the component name contains the query
     * as a case-insensitive substring.
     * </p>
     * <p>
     * **Validates: Requirements 2.1, 2.4, 2.5**
     * </p>
     */
    @Property
    @Tag("Feature: component-search-filter, Property 1: Filter subset")
    void filterReturnsOnlyMatchingComponents(
            @ForAll("componentNames") List<String> components,
            @ForAll("searchQuery") String query) {
        
        // When: Filter is applied
        final List<String> filtered = ComponentFilter.filter(components, query);
        
        // Then: All filtered components should match the query
        for (final String component : filtered) {
            assertThat(ComponentFilter.matches(component, query))
                .as("Component '%s' should match query '%s'", component, query)
                .isTrue();
        }
        
        // And: All matching components from original list should be in filtered list
        for (final String component : components) {
            if (ComponentFilter.matches(component, query)) {
                assertThat(filtered)
                    .as("Filtered list should contain matching component '%s'", component)
                    .contains(component);
            }
        }
        
        // And: Filtered list should not contain non-matching components
        for (final String component : filtered) {
            assertThat(components)
                .as("Filtered component '%s' should be from original list", component)
                .contains(component);
        }
    }

    /**
     * Property 2: Empty Query Returns All Components
     * <p>
     * For any list of component names, when the search query is empty or contains
     * only whitespace, the filtered result should equal the original list.
     * </p>
     * <p>
     * **Validates: Requirements 2.6, 3.4**
     * </p>
     */
    @Property
    @Tag("Feature: component-search-filter, Property 2: Empty query identity")
    void emptyQueryReturnsAllComponents(@ForAll("componentNames") List<String> components) {
        
        // When: Filter is applied with empty query
        final List<String> filteredEmpty = ComponentFilter.filter(components, "");
        
        // Then: Result should equal original list
        assertThat(filteredEmpty)
            .as("Empty query should return all components")
            .containsExactlyElementsOf(components);
        
        // When: Filter is applied with whitespace query
        final List<String> filteredWhitespace = ComponentFilter.filter(components, "   ");
        
        // Then: Result should equal original list
        assertThat(filteredWhitespace)
            .as("Whitespace query should return all components")
            .containsExactlyElementsOf(components);
        
        // When: Filter is applied with null query
        final List<String> filteredNull = ComponentFilter.filter(components, null);
        
        // Then: Result should equal original list
        assertThat(filteredNull)
            .as("Null query should return all components")
            .containsExactlyElementsOf(components);
    }

    /**
     * Provides component name lists for testing.
     * <p>
     * Generates lists of realistic component names similar to Web Awesome components.
     * </p>
     */
    @Provide
    Arbitrary<List<String>> componentNames() {
        return Arbitraries.of(
            "Badge", "Button", "Card", "Checkbox", "Dialog", "Input",
            "Alert", "Avatar", "Breadcrumb", "Carousel", "Dropdown", "Icon",
            "Menu", "Modal", "Progress", "Radio", "Select", "Slider",
            "Spinner", "Switch", "Tab", "Tag", "Textarea", "Tooltip"
        ).list().ofMinSize(0).ofMaxSize(30);
    }

    /**
     * Provides search query strings for testing.
     * <p>
     * Generates various query patterns including:
     * - Empty strings
     * - Single characters
     * - Partial component names
     * - Full component names
     * - Mixed case queries
     * </p>
     */
    @Provide
    Arbitrary<String> searchQuery() {
        return Arbitraries.oneOf(
            Arbitraries.just(""),
            Arbitraries.just("but"),
            Arbitraries.just("BUT"),
            Arbitraries.just("Button"),
            Arbitraries.just("badge"),
            Arbitraries.just("car"),
            Arbitraries.just("xyz"),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
        );
    }
}
