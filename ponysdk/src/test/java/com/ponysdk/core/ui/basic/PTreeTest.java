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
import com.ponysdk.core.ui.basic.event.PSelectionEvent;
import com.ponysdk.core.ui.basic.event.PSelectionHandler;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PTreeTest extends PSuite {

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
        final PTree tree = Element.newPTree();
        assertEquals(WidgetType.TREE, tree.getWidgetType());
        assertNotNull(tree.getRoot());
        assertEquals(0, tree.size());
        assertFalse(tree.iterator().hasNext());
    }

    @Test
    public void testCreationWritesRootItem() {
        final PTree tree = Element.newPTree();
        final PTreeItem item = tree.add("a");
        PWindow.getMain().add(tree);

        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(tree.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(tree.getRoot().getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TREE_ROOT).contains(tree.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(tree.getRoot().getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_CREATE).contains(item.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TEXT).contains("a"));
        assertTrue(item.isInitialized());
    }

    @Test
    public void testAddGetRemove() {
        final PTree tree = Element.newPTree();
        final PTreeItem a = tree.add("a");
        final PTreeItem b = tree.add(Element.newPTreeItem("b"));
        assertEquals(2, tree.size());
        assertSame(a, tree.get(0));
        assertSame(b, tree.get(1));

        final List<PTreeItem> items = new ArrayList<>();
        tree.forEach(items::add);
        assertEquals(List.of(a, b), items);

        tree.remove(a);
        assertEquals(1, tree.size());
        assertSame(b, tree.get(0));
    }

    @Test
    public void testAnimationEnabled() {
        final PTree tree = Element.newPTree();
        PWindow.getMain().add(tree);
        assertFalse(tree.isAnimationEnabled());

        writer.clear();
        tree.setAnimationEnabled(true);
        assertTrue(tree.isAnimationEnabled());
        assertTrue(writer.valuesOf(ServerToClientModel.ANIMATION).contains(true));

        writer.clear();
        tree.setAnimationEnabled(true);
        assertTrue(writer.isEmpty());
    }

    @Test
    public void testSetSelectedItemFiresHandlersAndWrites() {
        final PTree tree = Element.newPTree();
        final PTreeItem item = tree.add("a");
        PWindow.getMain().add(tree);

        final List<PTreeItem> selected = new ArrayList<>();
        final PSelectionHandler<PTreeItem> handler = event -> selected.add(event.getSelectedItem());
        tree.addSelectionHandler(handler);

        writer.clear();
        tree.setSelectedItem(item);
        assertSame(item, tree.getSelectedItem());
        assertTrue(writer.valuesOf(ServerToClientModel.SELECTED_INDEX).contains(item.getID()));
        assertEquals(List.of(item), selected);

        writer.clear();
        tree.setSelectedItem(item);
        assertTrue(writer.isEmpty());
        assertEquals(1, selected.size());

        tree.setSelectedItem(null);
        assertNull(tree.getSelectedItem());
        assertTrue(writer.valuesOf(ServerToClientModel.SELECTED_INDEX).contains(-1));

        tree.removeSelectionHandler(handler);
        tree.setSelectedItem(item);
        assertEquals(2, selected.size());
    }

    @Test
    public void testClientSelectionOpenClose() {
        final PTree tree = Element.newPTree();
        final PTreeItem item = tree.add("a");
        PWindow.getMain().add(tree);

        final List<PSelectionEvent<PTreeItem>> events = new ArrayList<>();
        tree.addSelectionHandler(events::add);

        tree.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_SELECTION.toStringValue(), item.getID()).build());
        assertSame(item, tree.getSelectedItem());
        assertEquals(1, events.size());
        assertSame(item, events.get(0).getSelectedItem());

        tree.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_OPEN.toStringValue(), item.getID()).build());
        assertTrue(item.getState());

        tree.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_CLOSE.toStringValue(), item.getID()).build());
        assertFalse(item.getState());
    }

    @Test
    public void testClientSelectionWithoutHandler() {
        final PTree tree = Element.newPTree();
        final PTreeItem item = tree.add("a");
        PWindow.getMain().add(tree);
        tree.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_SELECTION.toStringValue(), item.getID()).build());
        assertSame(item, tree.getSelectedItem());
    }

    @Test
    public void testClientDataIgnoredWhenHidden() {
        final PTree tree = Element.newPTree();
        final PTreeItem item = tree.add("a");
        PWindow.getMain().add(tree);
        tree.setVisible(false);
        tree.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_SELECTION.toStringValue(), item.getID()).build());
        assertNull(tree.getSelectedItem());
    }

    @Test
    public void testClientDataFallsBackToWidgetHandling() {
        final PTree tree = Element.newPTree();
        PWindow.getMain().add(tree);
        tree.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), true).build());
        assertTrue(tree.isShown());
    }

    @Test
    public void testClear() {
        final PTree tree = Element.newPTree();
        tree.add("a");
        tree.add("b");
        PWindow.getMain().add(tree);

        writer.clear();
        tree.clear();
        assertEquals(0, tree.size());
        assertTrue((writer.count(ServerToClientModel.CLEAR) > 0));
    }

    @Test
    public void testClearEmptyTree() {
        final PTree tree = Element.newPTree();
        tree.clear();
        assertEquals(0, tree.size());
    }

    @Test
    public void testAdoptAndOrphan() {
        final PTree tree = Element.newPTree();
        final PLabel label = Element.newPLabel("l");
        final PTreeItem item = Element.newPTreeItem(label);

        tree.adopt(label, item);
        assertSame(tree, label.getParent());
        assertThrows(IllegalStateException.class, () -> tree.adopt(label, item));

        tree.orphan(label);
        assertNull(label.getParent());

        final PFlowPanel other = Element.newPFlowPanel();
        other.add(label);
        assertThrows(IllegalStateException.class, () -> tree.orphan(label));
    }

    @Test
    public void testDestroyPropagatesToItems() {
        final PTree tree = Element.newPTree();
        final PTreeItem item = tree.add("a");
        PWindow.getMain().add(tree);
        tree.onDestroy();
        assertNull(tree.getWindow());
        assertNull(tree.getRoot().getWindow());
        assertNull(item.getWindow());
    }

    @Test
    public void testDumpDOMWithLeaf() {
        final PTree tree = Element.newPTree();
        tree.add("a");
        assertEquals("<div><div item=\"null\"><div item=\"a\"></div></div></div>", tree.dumpDOM());
    }
}
