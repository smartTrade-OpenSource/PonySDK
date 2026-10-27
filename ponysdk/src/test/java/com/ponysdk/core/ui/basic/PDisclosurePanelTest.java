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

import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PCloseEvent;
import com.ponysdk.core.ui.basic.event.POpenEvent;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PDisclosurePanelTest extends PSuite {

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
    public void testInit() {
        final PDisclosurePanel panel = Element.newPDisclosurePanel("header");
        assertEquals(WidgetType.DISCLOSURE_PANEL, panel.getWidgetType());
        assertNull(panel.getContent());
        assertFalse(panel.isOpen());
        assertFalse(panel.isAnimationEnabled());
        PWindow.getMain().add(panel);
        assertTrue(writer.valuesOf(ServerToClientModel.TEXT).contains("header"));
    }

    @Test
    public void testContentSetBeforeAttachIsCreatedWithThePanel() {
        final PDisclosurePanel panel = Element.newPDisclosurePanel("header");
        final PLabel content = Element.newPLabel("c");
        panel.setWidget(content);
        assertSame(content, panel.getContent());
        assertSame(panel, content.getParent());
        assertFalse(content.isInitialized());

        PWindow.getMain().add(panel);
        assertTrue(content.isInitialized());
        assertEquals(List.of(panel.getID()), writer.valuesAfter(ServerToClientModel.TYPE_ADD, content.getID()));
    }

    @Test
    public void testAddAfterAttach() {
        final PDisclosurePanel panel = Element.newPDisclosurePanel("header");
        PWindow.getMain().add(panel);
        final PLabel content = Element.newPLabel("c");
        panel.add((IsPWidget) content);
        assertTrue(content.isInitialized());
        assertSame(content, panel.iterator().next());
        assertThrows(IllegalStateException.class, () -> panel.add(Element.newPLabel("other")));

        writer.clear();
        panel.setContent(content);
        assertTrue(writer.isEmpty());
        assertEquals("<div pid=\"" + panel.getID() + "\"><div>header</div>" + content.dumpDOM() + "</div>", panel.dumpDOM());
    }

    @Test
    public void testClearWithoutContent() {
        final PDisclosurePanel panel = Element.newPDisclosurePanel("header");
        panel.clear();
        assertNull(panel.getContent());
        assertFalse(panel.remove(Element.newPLabel("stranger")));
    }

    @Test
    public void testRemoveContent() {
        final PDisclosurePanel panel = Element.newPDisclosurePanel("header");
        final PLabel content = Element.newPLabel("c");
        panel.setContent(content);
        assertTrue(panel.remove(content));
        assertNull(panel.getContent());
        assertNull(content.getParent());
    }

    @Test
    public void testReplaceContent() {
        final PDisclosurePanel panel = Element.newPDisclosurePanel("header");
        final PLabel first = Element.newPLabel("a");
        final PLabel second = Element.newPLabel("b");
        panel.setContent(first);
        panel.setContent(second);
        assertSame(second, panel.getContent());
        assertNull(first.getParent());
    }

    @Test
    public void testSetOpen() {
        final PDisclosurePanel panel = Element.newPDisclosurePanel("header");
        PWindow.getMain().add(panel);

        writer.clear();
        panel.setOpen(true);
        assertTrue(panel.isOpen());
        assertTrue(writer.valuesOf(ServerToClientModel.OPEN).contains(true));

        writer.clear();
        panel.setOpen(true);
        assertTrue(writer.isEmpty());

        panel.setOpen(false);
        assertFalse(panel.isOpen());
        assertTrue(writer.valuesOf(ServerToClientModel.CLOSE).contains(false));
    }

    @Test
    public void testAnimationEnabled() {
        final PDisclosurePanel panel = Element.newPDisclosurePanel("header");
        PWindow.getMain().add(panel);

        writer.clear();
        panel.setAnimationEnabled(true);
        assertTrue(panel.isAnimationEnabled());
        assertTrue(writer.valuesOf(ServerToClientModel.ANIMATION).contains(true));

        writer.clear();
        panel.setAnimationEnabled(true);
        assertTrue(writer.isEmpty());
    }

    @Test
    public void testClientOpenAndClose() {
        final PDisclosurePanel panel = Element.newPDisclosurePanel("header");
        PWindow.getMain().add(panel);
        final List<POpenEvent> opened = new ArrayList<>();
        final List<PCloseEvent> closed = new ArrayList<>();
        panel.addOpenHandler(opened::add);
        panel.addCloseHandler(closed::add);

        panel.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_OPEN.toStringValue(), true).build());
        assertTrue(panel.isOpen());
        assertEquals(1, opened.size());
        assertSame(panel, opened.get(0).getSource());

        panel.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_CLOSE.toStringValue(), true).build());
        assertFalse(panel.isOpen());
        assertEquals(1, closed.size());
    }

    @Test
    public void testClientDataIgnoredWhenHidden() {
        final PDisclosurePanel panel = Element.newPDisclosurePanel("header");
        panel.setVisible(false);
        panel.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_OPEN.toStringValue(), true).build());
        assertFalse(panel.isOpen());
    }

    @Test
    public void testClientDataFallsBackToWidgetHandling() {
        final PDisclosurePanel panel = Element.newPDisclosurePanel("header");
        panel.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), true).build());
        assertTrue(panel.isShown());
    }
}
