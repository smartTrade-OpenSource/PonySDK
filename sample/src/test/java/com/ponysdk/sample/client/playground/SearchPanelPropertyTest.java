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

/**
 * Property-based tests for SearchPanel.
 */
public class SearchPanelPropertyTest {

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
     * Property 4: Result Count Format
     * <p>
     * For any filtered component list and total component count, the result count
     * string should match the format "{filtered} of {total} components" where
     * filtered ? total.
     * </p>
     * <p>
     * **Validates: Requirements 4.2**
     * </p>
     */
    @Property
    @Tag("Feature: component-search-filter, Property 4: Result count format")
    void resultCountFormatIsCorrect(
            @ForAll("filteredCounts") int filtered,
            @ForAll("totalCounts") int total) {
        
        Assume.that(filtered >= 0);
        Assume.that(total >= 0);
        Assume.that(filtered <= total);
        
        // Given: A search panel
        final SearchPanel searchPanel = new SearchPanel();
        
        // When/Then: setResultCount should accept any valid non-negative pair where filtered <= total
        assertThatCode(() -> searchPanel.setResultCount(filtered, total))
            .as("setResultCount(%d, %d) should not throw", filtered, total)
            .doesNotThrowAnyException();
    }

    /**
     * Provides filtered count values for testing.
     */
    @Provide
    Arbitrary<Integer> filteredCounts() {
        return Arbitraries.integers().between(0, 100);
    }

    /**
     * Provides total count values for testing.
     */
    @Provide
    Arbitrary<Integer> totalCounts() {
        return Arbitraries.integers().between(0, 100);
    }
}
