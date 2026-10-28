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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PBeforeSelectionEvent;
import com.ponysdk.core.ui.basic.event.PSelectionEvent;
import com.ponysdk.core.ui.basic.event.PSelectionHandler;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PTabLayoutPanelTest extends PSuite {

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
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        assertEquals(WidgetType.TAB_LAYOUT_PANEL, panel.getWidgetType());
        assertNull(panel.getSelectedItemIndex());
        assertNull(panel.getAnimationDuration());
        PWindow.getMain().add(panel);
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD_HANDLER).contains(panel.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.HANDLER_TYPE).contains(HandlerModel.HANDLER_SELECTION.getValue()));
    }

    @Test
    public void testAddWithTabText() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        PWindow.getMain().add(panel);

        writer.clear();
        final PLabel content = Element.newPLabel("content");
        panel.add(content, "tab");
        assertEquals(1, panel.getWidgetCount());
        assertSame(panel, content.getParent());
        assertTrue(content.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(content.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TAB_TEXT).contains("tab"));
        assertTrue(writer.valuesOf(ServerToClientModel.BEFORE_INDEX).contains(0));

        final PLabel second = Element.newPLabel("second");
        panel.add((IsPWidget) second, "tab2");
        assertSame(second, panel.getWidget(1));
        assertTrue(writer.valuesOf(ServerToClientModel.BEFORE_INDEX).contains(1));

        final PLabel first = Element.newPLabel("first");
        panel.insert((IsPWidget) first, "tab0", 0);
        assertSame(first, panel.getWidget(0));
        assertEquals(3, panel.getWidgetCount());
    }

    @Test
    public void testAddWithTabWidget() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        PWindow.getMain().add(panel);

        writer.clear();
        final PLabel content = Element.newPLabel("content");
        final PLabel tab = Element.newPLabel("tab");
        panel.add(content, tab);
        assertTrue(tab.isInitialized());
        assertTrue(content.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TAB_WIDGET).contains(tab.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.BEFORE_INDEX).contains(0));

        final PLabel content2 = Element.newPLabel("content2");
        panel.add((IsPWidget) content2, (IsPWidget) Element.newPLabel("tab2"));
        final PLabel content0 = Element.newPLabel("content0");
        panel.insert((IsPWidget) content0, (IsPWidget) Element.newPLabel("tab0"), 0);
        assertEquals(3, panel.getWidgetCount());
        assertEquals(0, panel.getWidgetIndex(content0));
        assertEquals(2, panel.getWidgetIndex(content2));
    }

    @Test
    public void testTabsAddedBeforeAttachAreCreatedWithThePanel() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        final PLabel content = Element.newPLabel("content");
        final PLabel tab = Element.newPLabel("tab");
        final PLabel textContent = Element.newPLabel("text");
        panel.add(content, tab);
        panel.add(textContent, "text");
        assertFalse(tab.isInitialized());
        assertFalse(content.isInitialized());

        PWindow.getMain().add(panel);
        assertTrue(tab.isInitialized());
        assertTrue(content.isInitialized());
        assertTrue(textContent.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TAB_WIDGET).contains(tab.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TAB_TEXT).contains("text"));
        // the index is only sent for an insertion into a created panel
        assertEquals(0, writer.count(ServerToClientModel.BEFORE_INDEX));
    }

    @Test
    public void testRemove() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        PWindow.getMain().add(panel);
        final PLabel content = Element.newPLabel("content");
        panel.add(content, Element.newPLabel("tab"));
        final PLabel other = Element.newPLabel("other");
        panel.add(other, "other");

        writer.clear();
        assertTrue(panel.remove(content));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_REMOVE).contains(content.getID()));
        assertNull(content.getParent());
        assertEquals(1, panel.getWidgetCount());
        assertTrue(panel.remove(other));
        assertFalse(panel.remove(other));
        assertEquals(0, panel.getWidgetCount());
    }

    @Test
    public void testInsertItselfFails() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        assertThrows(IllegalStateException.class, () -> panel.add(panel, Element.newPLabel("tab")));
    }

    @Test
    public void testInsertFromOtherWindowFails() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        PWindow.getMain().add(panel);
        final PWindow other = Element.newPWindow(null, null);
        other.open();
        final PLabel label = Element.newPLabel("l");
        other.add(label);
        assertThrows(IllegalAccessError.class, () -> panel.add(label, "tab"));
        assertThrows(IllegalAccessError.class, () -> panel.add(label, Element.newPLabel("tab")));
    }

    @Test
    public void testAddWithoutTabFails() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        assertThrows(UnsupportedOperationException.class, () -> panel.add(Element.newPLabel("l")));
    }

    @Test
    public void testSelectTab() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        PWindow.getMain().add(panel);
        panel.add(Element.newPLabel("a"), "a");
        panel.add(Element.newPLabel("b"), "b");

        writer.clear();
        panel.selectTab(1);
        assertEquals(Integer.valueOf(1), panel.getSelectedItemIndex());
        assertTrue(writer.valuesOf(ServerToClientModel.SELECTED_INDEX).contains(1));
        assertThrows(IndexOutOfBoundsException.class, () -> panel.selectTab(2));
    }

    @Test
    public void testClientSelection() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        PWindow.getMain().add(panel);
        panel.add(Element.newPLabel("a"), "a");
        panel.add(Element.newPLabel("b"), "b");

        final List<Integer> selected = new ArrayList<>();
        final PSelectionHandler<Integer> handler = event -> selected.add(event.getSelectedItem());
        panel.addSelectionHandler(handler);
        panel.addSelectionHandler(panel);

        panel.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_SELECTION.toStringValue(), 1).build());
        assertEquals(List.of(1), selected);
        assertEquals(Integer.valueOf(1), panel.getSelectedItemIndex());

        panel.removeSelectionHandler(handler);
        panel.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_SELECTION.toStringValue(), 0).build());
        assertEquals(List.of(1), selected);
        assertEquals(Integer.valueOf(0), panel.getSelectedItemIndex());
    }

    @Test
    public void testClientBeforeSelection() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        PWindow.getMain().add(panel);

        writer.clear();
        final List<Integer> before = new ArrayList<>();
        final PBeforeSelectionEvent.Handler<Integer> handler = event -> before.add(event.getSelectedItem());
        panel.addBeforeSelectionHandler(handler);
        assertTrue(writer.valuesOf(ServerToClientModel.HANDLER_TYPE).contains(HandlerModel.HANDLER_BEFORE_SELECTION.getValue()));

        panel.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_BEFORE_SELECTION.toStringValue(), 2).build());
        assertEquals(List.of(2), before);

        panel.removeBeforeSelectionHandler(handler);
        panel.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_BEFORE_SELECTION.toStringValue(), 3).build());
        assertEquals(List.of(2), before);
    }

    @Test
    public void testClientDataIgnoredWhenHidden() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        final List<PSelectionEvent<Integer>> events = new ArrayList<>();
        panel.addSelectionHandler(events::add);
        panel.setVisible(false);
        panel.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_SELECTION.toStringValue(), 0).build());
        assertTrue(events.isEmpty());
    }

    @Test
    public void testClientDataFallsBackToWidgetHandling() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        panel.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), true).build());
        assertTrue(panel.isShown());
    }

    @Test
    public void testAnimation() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        PWindow.getMain().add(panel);

        writer.clear();
        panel.setAnimationVertical(true);
        assertTrue(writer.valuesOf(ServerToClientModel.VERTICAL).contains(true));

        panel.animate(Duration.ofMillis(250));
        assertTrue(writer.valuesOf(ServerToClientModel.ANIMATE).contains(250));

        panel.setAnimationDuration(Duration.ofMillis(300));
        assertEquals(Duration.ofMillis(300), panel.getAnimationDuration());
        assertTrue(writer.valuesOf(ServerToClientModel.ANIMATION_DURATION).contains(300));

        writer.clear();
        panel.setAnimationDuration(Duration.ofMillis(300));
        assertTrue(writer.isEmpty());
    }

    @Test
    public void aWidgetOfTheSameWindowMovesIntoATab() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        PWindow.getMain().add(panel);
        final PLabel first = Element.newPLabel("1");
        final PLabel second = Element.newPLabel("2");
        PWindow.getMain().add(first);
        PWindow.getMain().add(second);

        panel.insert(first, Element.newPLabel("tab"), 0);
        panel.insert(second, "text", 1);

        assertSame(panel, first.getParent());
        assertSame(panel, second.getParent());
        assertTrue(panel.remove(second));
    }

    @Test
    public void aTextTabIsRemovedWithoutTabWidgets() {
        final PTabLayoutPanel panel = Element.newPTabLayoutPanel();
        final PLabel label = Element.newPLabel("1");
        panel.insert(label, "text", 0);
        assertTrue(panel.remove(label));
        assertNull(label.getParent());
    }
}
