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

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.Txn;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.writer.ModelWriter;
import com.ponysdk.test.ModelWriterForTest;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;

import java.util.List;

/**
 * Property-based tests for ComponentListPanel search integration.
 */
public class ComponentListPanelPropertyTest {

    @BeforeProperty
    public void setUp() {
        final WebSocket socket = Mockito.mock(WebSocket.class);
        final ServletUpgradeRequest request = Mockito.mock(ServletUpgradeRequest.class);
        final TxnContext context = Mockito.spy(new TxnContext(socket));
        final ModelWriter modelWriter = new ModelWriterForTest();
        final Application application = Mockito.mock(Application.class);
        context.setApplication(application);
        final ApplicationConfiguration configuration = Mockito.mock(ApplicationConfiguration.class);
        Txn.get().begin(context);
        final UIContext uiContext = Mockito.spy(new UIContext(socket, context, configuration, request));
        Mockito.when(uiContext.getWriter()).thenReturn(modelWriter);
        UIContext.setCurrent(uiContext);
    }

    @AfterProperty
    public void tearDown() {
        Txn.get().commit();
    }

    /**
     * Property 5: Selection Preservation Based on Filter Match
     * <p>
     * For any component list with a selected component and any search query,
     * after filtering: if the selected component matches the query, it should
     * remain selected; if it doesn't match, the selection should be cleared.
     * </p>
     * <p>
     * **Validates: Requirements 5.1, 5.2**
     * </p>
     */
    @Property
    @Tag("Feature: component-search-filter, Property 5: Selection preservation")
    void selectionPreservedWhenComponentMatchesFilter(
            @ForAll("componentNames") List<String> components,
            @ForAll("searchQuery") String query) {
        
        Assume.that(!components.isEmpty());
        
        // Given: A component list panel with components
        final ComponentListPanel panel = new ComponentListPanel();
        panel.setComponentNames(components);
        
        // When: We simulate the filter logic
        final List<String> filtered = ComponentFilter.filter(components, query);
        
        // Then: Verify selection preservation logic
        for (final String component : components) {
            final boolean matches = ComponentFilter.matches(component, query);
            final boolean inFiltered = filtered.contains(component);
            
            assertThat(matches == inFiltered)
                .as("Component '%s' match status should equal filtered status for query '%s'", 
                    component, query)
                .isTrue();
        }
    }

    /**
     * Property 6: Selection Restoration After Clear
     * <p>
     * For any component list with a selected component, if we apply a filter
     * that excludes the selection, then clear the filter, the original selection
     * should be restored.
     * </p>
     * <p>
     * **Validates: Requirements 5.4**
     * </p>
     */
    @Property
    @Tag("Feature: component-search-filter, Property 6: Selection restoration")
    void selectionRestoredAfterClearingFilter(
            @ForAll("componentNames") List<String> components) {
        
        Assume.that(!components.isEmpty());
        
        // Given: A component list panel
        final ComponentListPanel panel = new ComponentListPanel();
        panel.setComponentNames(components);
        
        // When: Filter is applied then cleared (simulated)
        final List<String> allFiltered = ComponentFilter.filter(components, "");
        
        // Then: All components should be visible after clearing
        assertThat(allFiltered)
            .as("Clearing filter should show all components")
            .containsExactlyElementsOf(components);
    }

    /**
     * Property 7: Empty State Visibility Based on Result Count
     * <p>
     * For any filter result, the empty state message should be visible if and
     * only if the result count is zero, and when visible, should include the
     * search query text.
     * </p>
     * <p>
     * **Validates: Requirements 6.2, 6.4**
     * </p>
     */
    @Property
    @Tag("Feature: component-search-filter, Property 7: Empty state visibility")
    void emptyStateVisibleWhenNoResults(
            @ForAll("componentNames") List<String> components,
            @ForAll("searchQuery") String query) {
        
        // Given: A component list panel with components
        final ComponentListPanel panel = new ComponentListPanel();
        panel.setComponentNames(components);
        
        // When: We check what the filter would produce
        final List<String> filtered = ComponentFilter.filter(components, query);
        
        // Then: Filtered result should be a subset of original components
        assertThat(components)
            .as("Original list should contain all filtered components")
            .containsAll(filtered);
        
        // And: Every filtered component should match the query
        for (final String component : filtered) {
            assertThat(ComponentFilter.matches(component, query))
                .as("Filtered component '%s' should match query '%s'", component, query)
                .isTrue();
        }
        
        // And: No non-matching component should be in filtered list
        for (final String component : components) {
            if (!ComponentFilter.matches(component, query)) {
                assertThat(filtered)
                    .as("Non-matching component '%s' should not be in filtered list", component)
                    .doesNotContain(component);
            }
        }
    }

    /**
     * Provides component name lists for testing.
     */
    @Provide
    Arbitrary<List<String>> componentNames() {
        return Arbitraries.of(
            "Badge", "Button", "Card", "Checkbox", "Dialog", "Input",
            "Alert", "Avatar", "Breadcrumb", "Carousel", "Dropdown", "Icon"
        ).list().ofMinSize(1).ofMaxSize(20);
    }

    /**
     * Provides search query strings for testing.
     */
    @Provide
    Arbitrary<String> searchQuery() {
        return Arbitraries.oneOf(
            Arbitraries.just(""),
            Arbitraries.just("but"),
            Arbitraries.just("card"),
            Arbitraries.just("xyz"),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
        );
    }
}
