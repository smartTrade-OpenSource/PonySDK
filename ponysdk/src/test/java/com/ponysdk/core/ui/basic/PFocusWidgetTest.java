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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.eventbus.Event;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PFocusWidgetTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static JsonObject domEvent(final DomHandlerType type) {
        return Json.createObjectBuilder().add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), type.getValue()).build();
    }

    private static PButton attachedButton() {
        final PButton button = Element.newPButton();
        PWindow.getMain().add(button);
        return button;
    }

    @Test
    public void tabindexOnlyOnFormFieldsMakesTheWidgetFocusable() {
        Mockito.when(UIContext.get().getConfiguration().isTabindexOnlyFormField()).thenReturn(true);
        try {
            assertEquals(PWidget.TabindexMode.FOCUSABLE.getTabIndex(), Element.newPButton().getTabindex());
        } finally {
            Mockito.when(UIContext.get().getConfiguration().isTabindexOnlyFormField()).thenReturn(false);
        }
        assertEquals(-Integer.MAX_VALUE, Element.newPButton().getTabindex());
    }

    @Test
    public void disabledBeforeAttachIsSentInTheCreate() {
        final PButton button = Element.newPButton();
        button.setEnabled(false);
        assertTrue(writer.isEmpty());
        PWindow.getMain().add(button);
        assertEquals(List.of(false), writer.valuesOf(ServerToClientModel.ENABLED));
        assertEquals(0, writer.count(ServerToClientModel.TYPE_UPDATE));
    }

    @Test
    public void flagsAreSentOnlyWhenChanged() {
        final PButton button = attachedButton();
        writer.clear();
        button.setEnabled(true);
        button.setEnabled(false);
        button.setEnabledOnRequest(true);
        button.setEnabledOnRequest(true);
        button.showLoadingOnRequest(true);
        button.showLoadingOnRequest(true);
        assertEquals(List.of(false), writer.valuesOf(ServerToClientModel.ENABLED));
        assertEquals(List.of(true), writer.valuesOf(ServerToClientModel.ENABLED_ON_REQUEST));
        assertEquals(List.of(true), writer.valuesOf(ServerToClientModel.LOADING_ON_REQUEST));
        assertTrue(button.isEnabledOnRequest());
        assertTrue(button.isShowLoadingOnRequest());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void focusOnlyWhenVisibleAndEnabled() {
        final PButton button = attachedButton();
        writer.clear();
        button.setFocus(true);
        button.setFocus(false);
        assertEquals(List.of(true, false), writer.valuesOf(ServerToClientModel.FOCUS));

        writer.clear();
        button.setEnabled(false);
        button.focus();
        button.blur();
        button.setEnabled(true);
        button.setVisible(false);
        button.focus();
        button.blur();
        assertEquals(0, writer.count(ServerToClientModel.FOCUS));
    }

    @Test
    public void handlersReceiveTheirEvents() {
        final PButton button = attachedButton();
        final List<Event<?>> events = new ArrayList<>();
        button.addMouseOverHandler(events::add);
        button.addFocusHandler(events::add);
        button.addBlurHandler(events::add);
        button.addDoubleClickHandler(events::add);
        button.onClientData(domEvent(DomHandlerType.MOUSE_OVER));
        button.onClientData(domEvent(DomHandlerType.FOCUS));
        button.onClientData(domEvent(DomHandlerType.BLUR));
        button.onClientData(domEvent(DomHandlerType.DOUBLE_CLICK));
        assertEquals(4, events.size());
    }

    @Test
    public void disabledWidgetOnlyAcceptsVisibilityChanges() {
        final PButton button = attachedButton();
        final List<Event<?>> clicks = new ArrayList<>();
        button.addClickHandler(clicks::add);
        button.setEnabled(false);
        button.onClientData(domEvent(DomHandlerType.CLICK));
        assertTrue(clicks.isEmpty());

        button.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), true).build());
        assertTrue(button.isShown());

        // not a click: no end of processing
        writer.clear();
        button.onClientData(Json.createObjectBuilder().add("other", 1).build());
        button.onClientData(domEvent(DomHandlerType.FOCUS));
        assertEquals(0, writer.count(ServerToClientModel.END_OF_PROCESSING));
        assertFalse(button.isEnabled());
    }
}
