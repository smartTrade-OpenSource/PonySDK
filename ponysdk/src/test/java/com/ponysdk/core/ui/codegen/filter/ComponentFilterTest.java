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

package com.ponysdk.core.ui.codegen.filter;

import com.ponysdk.core.ui.codegen.model.ComponentDefinition;
import com.ponysdk.core.ui.codegen.model.FilterConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ComponentFilter}.
 * Tests component filtering with include/exclude patterns and skip lists.
 * Validates Requirements 9.5, 13.4.
 */
class ComponentFilterTest {

    @Test
    void filter_includesAllComponentsWhenNoFiltersConfigured() {
        final FilterConfig config = new FilterConfig(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList()
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("wa-input"),
            createComponent("sl-button")
        );

        final List<ComponentDefinition> filtered = filter.filter(components);

        assertEquals(3, filtered.size());
        assertTrue(filtered.containsAll(components));
    }

    @Test
    void filter_includesOnlyMatchingIncludePatterns() {
        final FilterConfig config = new FilterConfig(
            List.of("wa-*"),
            Collections.emptyList(),
            Collections.emptyList()
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("wa-input"),
            createComponent("sl-button")
        );

        final List<ComponentDefinition> filtered = filter.filter(components);

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("wa-button")));
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("wa-input")));
        assertFalse(filtered.stream().anyMatch(c -> c.tagName().equals("sl-button")));
    }

    @Test
    void filter_supportsMultipleIncludePatterns() {
        final FilterConfig config = new FilterConfig(
            List.of("wa-*", "sl-*"),
            Collections.emptyList(),
            Collections.emptyList()
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("sl-input"),
            createComponent("md-card")
        );

        final List<ComponentDefinition> filtered = filter.filter(components);

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("wa-button")));
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("sl-input")));
        assertFalse(filtered.stream().anyMatch(c -> c.tagName().equals("md-card")));
    }

    @Test
    void filter_excludesMatchingExcludePatterns() {
        final FilterConfig config = new FilterConfig(
            Collections.emptyList(),
            List.of("*-internal"),
            Collections.emptyList()
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("wa-button-internal"),
            createComponent("wa-input-internal")
        );

        final List<ComponentDefinition> filtered = filter.filter(components);

        assertEquals(1, filtered.size());
        assertEquals("wa-button", filtered.get(0).tagName());
    }

    @Test
    void filter_excludePatternsOverrideIncludePatterns() {
        final FilterConfig config = new FilterConfig(
            List.of("wa-*"),
            List.of("*-internal"),
            Collections.emptyList()
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("wa-button-internal"),
            createComponent("sl-input")
        );

        final List<ComponentDefinition> filtered = filter.filter(components);

        assertEquals(1, filtered.size());
        assertEquals("wa-button", filtered.get(0).tagName());
    }

    @Test
    void filter_skipsComponentsInSkipList() {
        final FilterConfig config = new FilterConfig(
            Collections.emptyList(),
            Collections.emptyList(),
            List.of("wa-deprecated-component", "wa-old-button")
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("wa-deprecated-component"),
            createComponent("wa-old-button"),
            createComponent("wa-input")
        );

        final List<ComponentDefinition> filtered = filter.filter(components);

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("wa-button")));
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("wa-input")));
        assertFalse(filtered.stream().anyMatch(c -> c.tagName().equals("wa-deprecated-component")));
        assertFalse(filtered.stream().anyMatch(c -> c.tagName().equals("wa-old-button")));
    }

    @Test
    void filter_skipListTakesPrecedenceOverIncludePatterns() {
        final FilterConfig config = new FilterConfig(
            List.of("wa-*"),
            Collections.emptyList(),
            List.of("wa-button")
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("wa-input")
        );

        final List<ComponentDefinition> filtered = filter.filter(components);

        assertEquals(1, filtered.size());
        assertEquals("wa-input", filtered.get(0).tagName());
    }

    @Test
    void filter_supportsQuestionMarkWildcard() {
        final FilterConfig config = new FilterConfig(
            List.of("wa-butto?"),
            Collections.emptyList(),
            Collections.emptyList()
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("wa-buttons"),
            createComponent("wa-input")
        );

        final List<ComponentDefinition> filtered = filter.filter(components);

        assertEquals(1, filtered.size());
        assertEquals("wa-button", filtered.get(0).tagName());
    }

    @Test
    void filter_handlesSpecialRegexCharactersInPatterns() {
        final FilterConfig config = new FilterConfig(
            List.of("wa-button.v2"),
            Collections.emptyList(),
            Collections.emptyList()
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button.v2"),
            createComponent("wa-buttonXv2")
        );

        final List<ComponentDefinition> filtered = filter.filter(components);

        assertEquals(1, filtered.size());
        assertEquals("wa-button.v2", filtered.get(0).tagName());
    }

    @Test
    void shouldInclude_returnsTrueForMatchingIncludePattern() {
        final FilterConfig config = new FilterConfig(
            List.of("wa-*"),
            Collections.emptyList(),
            Collections.emptyList()
        );
        final ComponentFilter filter = new ComponentFilter(config);

        assertTrue(filter.shouldInclude(createComponent("wa-button")));
        assertFalse(filter.shouldInclude(createComponent("sl-button")));
    }

    @Test
    void shouldInclude_returnsFalseForMatchingExcludePattern() {
        final FilterConfig config = new FilterConfig(
            Collections.emptyList(),
            List.of("*-internal"),
            Collections.emptyList()
        );
        final ComponentFilter filter = new ComponentFilter(config);

        assertTrue(filter.shouldInclude(createComponent("wa-button")));
        assertFalse(filter.shouldInclude(createComponent("wa-button-internal")));
    }

    @Test
    void shouldInclude_returnsFalseForSkippedComponent() {
        final FilterConfig config = new FilterConfig(
            Collections.emptyList(),
            Collections.emptyList(),
            List.of("wa-button")
        );
        final ComponentFilter filter = new ComponentFilter(config);

        assertFalse(filter.shouldInclude(createComponent("wa-button")));
        assertTrue(filter.shouldInclude(createComponent("wa-input")));
    }

    @Test
    void filter_handlesEmptyComponentList() {
        final FilterConfig config = new FilterConfig(
            List.of("wa-*"),
            Collections.emptyList(),
            Collections.emptyList()
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> filtered = filter.filter(Collections.emptyList());

        assertTrue(filtered.isEmpty());
    }

    @Test
    void filter_handlesComplexFilterCombination() {
        final FilterConfig config = new FilterConfig(
            List.of("wa-*", "sl-*"),
            List.of("*-internal", "*-deprecated"),
            List.of("wa-old-component")
        );
        final ComponentFilter filter = new ComponentFilter(config);

        final List<ComponentDefinition> components = List.of(
            createComponent("wa-button"),
            createComponent("wa-button-internal"),
            createComponent("wa-input-deprecated"),
            createComponent("wa-old-component"),
            createComponent("sl-card"),
            createComponent("sl-card-internal"),
            createComponent("md-button")
        );

        final List<ComponentDefinition> filtered = filter.filter(components);

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("wa-button")));
        assertTrue(filtered.stream().anyMatch(c -> c.tagName().equals("sl-card")));
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
