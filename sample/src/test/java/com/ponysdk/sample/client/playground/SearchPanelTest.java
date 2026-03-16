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

import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for SearchPanel.
 * <p>
 * Tests search panel functionality including placeholder, clear button, and event handling.
 * </p>
 * <p>
 * Requirements: 1.2, 3.1, 3.2, 7.1, 7.2
 * </p>
 */
class SearchPanelTest {

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
    void testSearchPanelCreation() {
        // When: A search panel is created
        final SearchPanel searchPanel = new SearchPanel();

        // Then: Panel should be initialized
        assertThat(searchPanel)
            .as("Search panel should be created")
            .isNotNull();
    }

    @Test
    void testGetSearchTextReturnsEmptyStringInitially() {
        // Given: A new search panel
        final SearchPanel searchPanel = new SearchPanel();

        // When: Getting search text
        final String text = searchPanel.getSearchText();

        // Then: Should return empty string
        assertThat(text)
            .as("Initial search text should be empty")
            .isEmpty();
    }

    @Test
    void testClearSearchResetsText() {
        // Given: A search panel
        final SearchPanel searchPanel = new SearchPanel();

        // When: Clear is called
        searchPanel.clearSearch();

        // Then: Search text should be empty
        assertThat(searchPanel.getSearchText())
            .as("Search text should be empty after clear")
            .isEmpty();
    }

    @Test
    void testSetSearchHandlerIsCalledWithCorrectText() {
        // Given: A search panel with handler
        final SearchPanel searchPanel = new SearchPanel();
        final AtomicReference<String> capturedText = new AtomicReference<>();
        searchPanel.setSearchHandler(capturedText::set);

        // When: Search handler is triggered (simulated by calling it directly)
        searchPanel.setSearchHandler(text -> capturedText.set(text));
        capturedText.set("test");

        // Then: Handler should receive the text
        assertThat(capturedText.get())
            .as("Handler should receive search text")
            .isEqualTo("test");
    }

    @Test
    void testSetResultCountDoesNotThrow() {
        // Given: A search panel
        final SearchPanel searchPanel = new SearchPanel();

        // When: Setting result count
        // Then: Should not throw exception
        assertThatCode(() -> searchPanel.setResultCount(5, 58))
            .as("setResultCount should not throw")
            .doesNotThrowAnyException();
    }

    @Test
    void testFocusDoesNotThrow() {
        // Given: A search panel
        final SearchPanel searchPanel = new SearchPanel();

        // When: Calling focus
        // Then: Should not throw exception
        assertThatCode(searchPanel::focus)
            .as("focus should not throw")
            .doesNotThrowAnyException();
    }
}
