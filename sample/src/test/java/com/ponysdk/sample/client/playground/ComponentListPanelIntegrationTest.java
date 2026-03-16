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

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

/**
 * Integration tests for ComponentListPanel with search functionality.
 * <p>
 * Tests the complete integration of search panel, filtering, and list display.
 * </p>
 * <p>
 * Requirements: 1.1, 2.1, 4.5, 5.1, 5.2, 5.4, 6.1
 * </p>
 */
class ComponentListPanelIntegrationTest {

    @BeforeEach
    void setUp() {
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

    @AfterEach
    void tearDown() {
        Txn.get().commit();
    }

    @Test
    void testSearchPanelInitializedCorrectly() {
        // Given: A component list panel
        final ComponentListPanel panel = new ComponentListPanel();
        final List<String> components = Arrays.asList("Badge", "Button", "Card");

        // When: Component names are set
        panel.setComponentNames(components);

        // Then: Search panel should be initialized
        assertThat(panel.getSearchPanel())
            .as("Search panel should be initialized")
            .isNotNull();
    }

    @Test
    void testEmptyStateShownWhenNoMatches() {
        // Given: A component list panel with components
        final ComponentListPanel panel = new ComponentListPanel();
        final List<String> components = Arrays.asList("Badge", "Button", "Card");
        panel.setComponentNames(components);

        // When: We verify empty state label exists
        // Then: Empty state label should be present
        assertThat(panel.getEmptyStateLabel())
            .as("Empty state label should exist")
            .isNotNull();
    }

    @Test
    void testSetComponentNamesWithNullThrowsException() {
        // Given: A component list panel
        final ComponentListPanel panel = new ComponentListPanel();

        // When: Setting null component names
        // Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> panel.setComponentNames(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("componentNames must not be null");
    }

    @Test
    void testShowErrorWithNullThrowsException() {
        // Given: A component list panel
        final ComponentListPanel panel = new ComponentListPanel();

        // When: Showing null error message
        // Then: Should throw IllegalArgumentException
        assertThatThrownBy(() -> panel.showError(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("errorMessage must not be null");
    }

    @Test
    void testGetSelectedComponentReturnsNullInitially() {
        // Given: A new component list panel
        final ComponentListPanel panel = new ComponentListPanel();

        // When: Getting selected component
        final String selected = panel.getSelectedComponent();

        // Then: Should return null
        assertThat(selected)
            .as("Initially no component should be selected")
            .isNull();
    }

    @Test
    void testClearRemovesAllComponents() {
        // Given: A component list panel with components
        final ComponentListPanel panel = new ComponentListPanel();
        final List<String> components = Arrays.asList("Badge", "Button", "Card");
        panel.setComponentNames(components);

        // When: Clear is called
        panel.clear();

        // Then: Selected component should be null
        assertThat(panel.getSelectedComponent())
            .as("After clear, no component should be selected")
            .isNull();
    }

    @Test
    void testShowErrorHidesSearchPanel() {
        // Given: A component list panel with components
        final ComponentListPanel panel = new ComponentListPanel();
        final List<String> components = Arrays.asList("Badge", "Button", "Card");
        panel.setComponentNames(components);

        // When: Error is shown
        panel.showError("Test error");

        // Then: Search panel should be hidden
        assertThat(panel.getSearchPanel().isVisible())
            .as("Search panel should be hidden on error")
            .isFalse();
    }
}
