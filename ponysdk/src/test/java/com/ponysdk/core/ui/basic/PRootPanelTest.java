/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PRootPanelTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static PWindow openedWindow() {
        final PWindow window = Element.newPWindow(null, null);
        window.open();
        return window;
    }

    private static JsonObject visibility(final boolean shown) {
        return Json.createObjectBuilder().add(ClientToServerModel.HANDLER_DOCUMENT_VISIBILITY.toStringValue(), shown).build();
    }

    @Test
    public void testDefaultRootPanelHasNoRootId() {
        final PWindow window = Element.newPWindow(null, null);
        final PRootPanel panel = window.getPRootPanel();
        assertEquals(WidgetType.ROOT_PANEL, panel.getWidgetType());
        writer.clear();
        window.open();
        assertTrue(panel.isInitialized());
        assertEquals(0, writer.count(ServerToClientModel.ROOT_ID));
    }

    @Test
    public void testZoneRootPanelSendsItsRootId() {
        final PWindow window = openedWindow();
        writer.clear();
        final PRootPanel panel = window.getPRootPanel("zone");
        assertTrue(panel.isInitialized());
        assertSame(panel, window.getPRootPanel("zone"));
        assertEquals(List.of("zone"), writer.valuesOf(ServerToClientModel.ROOT_ID));
    }

    @Test
    public void testDocumentVisibilityForwardedToTheWindowByTheDefaultPanel() {
        final PWindow window = openedWindow();
        assertTrue(window.isShown());
        window.getPRootPanel().onClientData(visibility(false));
        assertFalse(window.isShown());
    }

    @Test
    public void testDocumentVisibilityIgnoredByAZonePanel() {
        final PWindow window = openedWindow();
        window.getPRootPanel("zone").onClientData(visibility(false));
        assertTrue(window.isShown());
    }

    @Test
    public void testClientDataIgnoredOnceDestroyed() {
        final PWindow window = openedWindow();
        final PRootPanel panel = window.getPRootPanel();
        panel.onDestroy();
        panel.onClientData(visibility(false));
        assertTrue(window.isShown());
    }

    @Test
    public void testOtherClientDataDelegated() {
        final PWindow window = openedWindow();
        final PRootPanel panel = window.getPRootPanel();
        assertDoesNotThrow(() -> panel.onClientData(Json.createObjectBuilder().add("unknown", 1).build()));
        assertTrue(window.isShown());
    }

}
