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
import jakarta.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PLayoutResizeEvent;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PSplitLayoutPanelTest extends PSuite {

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
        final PSplitLayoutPanel panel = Element.newPSplitLayoutPanel();
        assertEquals(WidgetType.SPLIT_LAYOUT_PANEL, panel.getWidgetType());
        assertEquals(PUnit.PX, panel.getUnit());
        assertTrue(panel.getResizeHandlers().isEmpty());
    }

    @Test
    public void testDefaultsForUnknownWidget() {
        final PSplitLayoutPanel panel = Element.newPSplitLayoutPanel();
        final PLabel label = Element.newPLabel("l");
        assertEquals(-1, panel.getMinSize(label));
        assertEquals(-1, panel.getSnapClosedSize(label));
        assertFalse(panel.isToggleDisplayAllowed(label));
    }

    @Test
    public void testWidgetSplitSettings() {
        final PSplitLayoutPanel panel = Element.newPSplitLayoutPanel();
        PWindow.getMain().add(panel);
        final PLabel west = Element.newPLabel("w");
        panel.addWest(west, 100);

        writer.clear();
        panel.setWidgetMinSize(west, 50);
        assertEquals(50, panel.getMinSize(west));
        assertTrue(writer.valuesOf(ServerToClientModel.MIN_SIZE).contains(50));
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET_ID).contains(west.getID()));

        panel.setWidgetSnapClosedSize(west, 20);
        assertEquals(20, panel.getSnapClosedSize(west));
        assertTrue(writer.valuesOf(ServerToClientModel.SNAP_CLOSED_SIZE).contains(20));

        panel.setWidgetToggleDisplayAllowed(west, true);
        assertTrue(panel.isToggleDisplayAllowed(west));
        assertTrue(writer.valuesOf(ServerToClientModel.TOGGLE_DISPLAY_ALLOWED).contains(true));

        writer.clear();
        panel.setWidgetMinSize(west, 50);
        panel.setWidgetSnapClosedSize(west, 20);
        panel.setWidgetToggleDisplayAllowed(west, true);
        assertTrue(writer.isEmpty());
    }

    @Test
    public void testSettingsRequireChild() {
        final PSplitLayoutPanel panel = Element.newPSplitLayoutPanel();
        final PLabel stranger = Element.newPLabel("s");
        assertThrows(IllegalStateException.class, () -> panel.setWidgetMinSize(stranger, 1));
        assertThrows(IllegalStateException.class, () -> panel.setWidgetSnapClosedSize(stranger, 1));
        assertThrows(IllegalStateException.class, () -> panel.setWidgetToggleDisplayAllowed(stranger, true));
    }

    @Test
    public void testRemoveForgetsSplitInfo() {
        final PSplitLayoutPanel panel = Element.newPSplitLayoutPanel();
        final PLabel west = Element.newPLabel("w");
        panel.addWest(west, 100);
        panel.setWidgetMinSize(west, 50);

        assertTrue(panel.remove(west));
        assertEquals(-1, panel.getMinSize(west));
        assertNull(west.getParent());
        assertFalse(panel.remove(west));
    }

    @Test
    public void testResizeHandlers() {
        final PSplitLayoutPanel panel = Element.newPSplitLayoutPanel();
        PWindow.getMain().add(panel);
        final PLabel west = Element.newPLabel("w");
        panel.addWest(west, 100);

        writer.clear();
        final List<PLayoutResizeEvent> events = new ArrayList<>();
        final PLayoutResizeEvent.Handler handler = events::add;
        panel.addLayoutResizeHandler(handler);
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD_HANDLER).contains(panel.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.HANDLER_TYPE).contains(HandlerModel.HANDLER_RESIZE.getValue()));
        assertEquals(1, panel.getResizeHandlers().size());

        writer.clear();
        panel.addLayoutResizeHandler(e -> {});
        assertFalse((writer.count(ServerToClientModel.TYPE_ADD_HANDLER) > 0));

        final JsonObject resize = Json.createObjectBuilder()
            .add(ClientToServerModel.HANDLER_RESIZE.toStringValue(), Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add(ClientToServerModel.OBJECT_ID.toStringValue(), west.getID())
                    .add(ClientToServerModel.SIZE.toStringValue(), 123.5))
                .add(Json.createObjectBuilder()
                    .add(ClientToServerModel.OBJECT_ID.toStringValue(), -42)
                    .add(ClientToServerModel.SIZE.toStringValue(), 1.0)))
            .build();
        panel.onClientData(resize);

        assertEquals(1, events.size());
        final List<PLayoutResizeEvent.LayoutResizeData> data = events.get(0).getLayoutResizeData();
        assertEquals(1, data.size());
        assertSame(west, data.get(0).getWidget());
        assertEquals(123.5, data.get(0).getSize(), 0.0);

        panel.removeLayoutResizeHandler(handler);
        assertEquals(1, panel.getResizeHandlers().size());
        assertFalse((writer.count(ServerToClientModel.TYPE_REMOVE_HANDLER) > 0));
        panel.removeLayoutResizeHandler(panel.getResizeHandlers().iterator().next());
        assertEquals(1, writer.count(ServerToClientModel.TYPE_REMOVE_HANDLER));
        assertEquals(HandlerModel.HANDLER_RESIZE.getValue(), writer.valuesOf(ServerToClientModel.HANDLER_TYPE).getLast());
    }

    @Test
    public void testClientDataIgnoredWhenHidden() {
        final PSplitLayoutPanel panel = Element.newPSplitLayoutPanel();
        final List<PLayoutResizeEvent> events = new ArrayList<>();
        panel.addLayoutResizeHandler(events::add);
        panel.setVisible(false);
        panel.onClientData(Json.createObjectBuilder()
            .add(ClientToServerModel.HANDLER_RESIZE.toStringValue(), Json.createArrayBuilder()).build());
        assertTrue(events.isEmpty());
    }

    @Test
    public void testClientDataFallsBackToWidgetHandling() {
        final PSplitLayoutPanel panel = Element.newPSplitLayoutPanel();
        panel.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), true).build());
        assertTrue(panel.isShown());
    }
}
