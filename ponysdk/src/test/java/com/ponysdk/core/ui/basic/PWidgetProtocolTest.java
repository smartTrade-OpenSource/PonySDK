/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

/**
 * Wire-level tests for {@link PWidget} state set before the widget is created: it travels once, inside
 * the CREATE (enrichForUpdate), and is not stacked again as separate updates. After creation, every
 * change is sent as an update.
 */
public class PWidgetProtocolTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    @Test
    public void stateSetBeforeAttachIsSentOnceInsideTheCreate() {
        final PButton button = Element.newPButton("ok");
        button.setAttribute("title", "Confirm");
        button.setStyleProperty("color", "red");
        button.setProperty("tabIndex", "3");
        button.setVisible(false);
        button.setEnabled(false);
        writer.clear();

        PWindow.getMain().add(button);

        assertEquals(0, writer.count(ServerToClientModel.TYPE_UPDATE), "one CREATE, no extra update block");
        assertEquals(List.of("Confirm"), writer.valuesAfter(ServerToClientModel.PUT_ATTRIBUTE_KEY, "title"));
        assertEquals(List.of("red"), writer.valuesAfter(ServerToClientModel.PUT_STYLE_KEY, "color"));
        assertEquals(List.of("3"), writer.valuesAfter(ServerToClientModel.PUT_PROPERTY_KEY, "tabIndex"));
        assertEquals(List.of(false), writer.valuesOf(ServerToClientModel.WIDGET_VISIBLE));
        assertEquals(List.of(false), writer.valuesOf(ServerToClientModel.ENABLED));
    }

    @Test
    public void stateRemovedBeforeAttachIsNotSent() {
        final PLabel label = Element.newPLabel("x");
        label.setAttribute("data-x", "1");
        label.removeAttribute("data-x");
        label.setStyleProperty("color", "red");
        label.removeStyleProperty("color");
        writer.clear();

        PWindow.getMain().add(label);

        for (final ServerToClientModel m : new ServerToClientModel[] { ServerToClientModel.PUT_ATTRIBUTE_KEY,
                ServerToClientModel.REMOVE_ATTRIBUTE_KEY, ServerToClientModel.PUT_STYLE_KEY, ServerToClientModel.REMOVE_STYLE_KEY }) {
            assertEquals(0, writer.count(m), m + " must not be sent");
        }
    }

    @Test
    public void changesAfterAttachAreSentAsUpdates() {
        final PButton button = Element.newPButton("ok");
        PWindow.getMain().add(button);
        writer.clear();

        button.setAttribute("title", "Confirm");
        button.setStyleProperty("color", "red");
        button.setProperty("tabIndex", "3");
        button.setVisible(false);
        button.setEnabled(false);
        button.setAttribute("title", "Confirm"); // unchanged: nothing sent
        button.removeAttribute("title");
        button.removeStyleProperty("color");

        assertEquals(7, writer.count(ServerToClientModel.TYPE_UPDATE));
        assertEquals(List.of(button.getID(), button.getID(), button.getID(), button.getID(), button.getID(),
                button.getID(), button.getID()), writer.valuesOf(ServerToClientModel.TYPE_UPDATE));
        assertEquals(List.of("Confirm"), writer.valuesAfter(ServerToClientModel.PUT_ATTRIBUTE_KEY, "title"));
        assertEquals(List.of("red"), writer.valuesAfter(ServerToClientModel.PUT_STYLE_KEY, "color"));
        assertEquals(1, writer.count(ServerToClientModel.REMOVE_ATTRIBUTE_KEY));
        assertEquals(1, writer.count(ServerToClientModel.REMOVE_STYLE_KEY));
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_VISIBLE).contains(false));
    }

    @Test
    public void rarelyUsedStateIsStillReachable() {
        // these fields now live in a lazily allocated holder
        final PLabel label = Element.newPLabel("x");
        assertEquals(null, label.getProperty("p"));
        assertEquals(null, label.getAddons());
        label.setProperty("p", "v");
        assertEquals("v", label.getProperty("p"));
        final com.ponysdk.core.ui.basic.event.PVisibilityEvent.PVisibilityHandler handler = event -> {};
        label.addVisibilityHandler(handler);
        label.removeVisibilityHandler(handler);
        label.preventEvent(com.ponysdk.core.ui.model.PEventType.ONCLICK);
        label.stopEvent(com.ponysdk.core.ui.model.PEventType.ONCLICK);
        PWindow.getMain().add(label);
        assertEquals("v", label.getProperty("p"));
    }
}
