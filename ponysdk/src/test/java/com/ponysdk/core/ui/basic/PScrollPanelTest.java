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
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PScrollEvent;
import com.ponysdk.core.ui.basic.event.PScrollEvent.PScrollHandler;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PScrollPanelTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private static PScrollPanel attached() {
        final PScrollPanel panel = Element.newPScrollPanel();
        PWindow.getMain().add(panel);
        return panel;
    }

    private static JsonObject scroll(final int height, final int width, final int vertical, final int horizontal) {
        return Json.createObjectBuilder()
            .add(ClientToServerModel.HANDLER_SCROLL.toStringValue(), 0)
            .add(ClientToServerModel.HANDLER_SCROLL_HEIGHT.toStringValue(), height)
            .add(ClientToServerModel.HANDLER_SCROLL_WIDTH.toStringValue(), width)
            .add(ClientToServerModel.HANDLER_SCROLL_VERTICAL.toStringValue(), vertical)
            .add(ClientToServerModel.HANDLER_SCROLL_HORIZONTAL.toStringValue(), horizontal)
            .build();
    }

    @Test
    public void testInit() {
        assertEquals(WidgetType.SCROLL_PANEL, Element.newPScrollPanel().getWidgetType());
    }

    @Test
    public void testScrollPositions() {
        final PScrollPanel panel = attached();
        writer.clear();
        panel.setHorizontalScrollPosition(12);
        panel.setVerticalScrollPosition(34);
        assertEquals(List.of(12), writer.valuesOf(ServerToClientModel.HORIZONTAL_SCROLL_POSITION));
        assertEquals(List.of(34), writer.valuesOf(ServerToClientModel.VERTICAL_SCROLL_POSITION));
    }

    @Test
    public void testScrollToSendsTheScrollTypeOrdinal() {
        final PScrollPanel panel = attached();
        writer.clear();
        panel.scrollToBottom();
        panel.scrollToLeft();
        panel.scrollToRight();
        panel.scrollToTop();
        assertEquals(List.of(0, 1, 2, 3), writer.valuesOf(ServerToClientModel.SCROLL_TO));
    }

    @Test
    public void testScrollHandlerLifecycle() {
        final PScrollPanel panel = attached();
        writer.clear();
        final List<PScrollEvent> events = new ArrayList<>();
        final PScrollHandler first = events::add;
        final PScrollHandler second = events::add;
        panel.addScrollHandler(first);
        panel.addScrollHandler(second);
        assertEquals(List.of(HandlerModel.HANDLER_SCROLL.getValue()), writer.valuesOf(ServerToClientModel.HANDLER_TYPE));
        assertEquals(1, writer.count(ServerToClientModel.TYPE_ADD_HANDLER));

        panel.onClientData(scroll(100, 200, 30, 40));
        assertEquals(2, events.size());
        final PScrollEvent event = events.get(0);
        assertSame(panel, event.getSource());
        assertEquals(100, event.getHeight());
        assertEquals(200, event.getWidth());
        assertEquals(30, event.getVerticalPostion());
        assertEquals(40, event.getHorizontalPosition());

        writer.clear();
        panel.removeScrollHandler(first);
        assertEquals(0, writer.count(ServerToClientModel.TYPE_REMOVE_HANDLER));
        panel.removeScrollHandler(second);
        assertEquals(1, writer.count(ServerToClientModel.TYPE_REMOVE_HANDLER));
        assertEquals(List.of(HandlerModel.HANDLER_SCROLL.getValue()), writer.valuesOf(ServerToClientModel.HANDLER_TYPE));
    }

    @Test
    public void testRemoveHandlerWithoutAnyHandler() {
        final PScrollPanel panel = attached();
        writer.clear();
        panel.removeScrollHandler(event -> {
        });
        assertTrue(writer.isEmpty());
    }

    @Test
    public void testScrollWithoutHandlerAndWhenHidden() {
        final PScrollPanel panel = attached();
        assertDoesNotThrow(() -> panel.onClientData(scroll(1, 2, 3, 4)));

        final List<PScrollEvent> events = new ArrayList<>();
        panel.addScrollHandler(events::add);
        panel.setVisible(false);
        panel.onClientData(scroll(1, 2, 3, 4));
        assertTrue(events.isEmpty());
    }

    @Test
    public void testOtherClientDataDelegated() {
        final PScrollPanel panel = attached();
        final List<PScrollEvent> events = new ArrayList<>();
        panel.addScrollHandler(events::add);
        panel.onClientData(Json.createObjectBuilder().add("unknown", 1).build());
        assertTrue(events.isEmpty());
    }

}
