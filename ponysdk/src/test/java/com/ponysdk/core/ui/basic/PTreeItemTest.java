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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PTreeItemTest extends PSuite {

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
        final PTreeItem item = Element.newPTreeItem("text");
        assertEquals(WidgetType.TREE_ITEM, item.getWidgetType());
        assertNull(item.getWidget());
        assertEquals(0, item.size());
        assertNull(item.get(0));
        assertFalse(item.iterator().hasNext());
        assertFalse(Element.newPTreeItem().getState());
    }

    @Test
    public void testSetTextBeforeAndAfterInit() {
        final PTree tree = Element.newPTree();
        final PTreeItem item = tree.add("a");
        item.setText("b");
        PWindow.getMain().add(tree);
        assertTrue(writer.valuesOf(ServerToClientModel.TEXT).contains("b"));
        assertFalse(writer.valuesOf(ServerToClientModel.TEXT).contains("a"));

        writer.clear();
        item.setText("c");
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_UPDATE).contains(item.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.TEXT).contains("c"));

        writer.clear();
        item.setText("c");
        assertTrue(writer.isEmpty());
    }

    @Test
    public void testSetState() {
        final PTree tree = Element.newPTree();
        final PTreeItem item = tree.add("a");
        PWindow.getMain().add(tree);

        writer.clear();
        item.setState(true);
        assertTrue(item.getState());
        assertTrue(writer.valuesOf(ServerToClientModel.OPEN).contains(true));

        writer.clear();
        item.setState(true);
        assertTrue(writer.isEmpty());

        item.setState(false);
        assertFalse(item.getState());
        assertTrue(writer.valuesOf(ServerToClientModel.CLOSE).contains(false));
    }

    @Test
    public void testSelection() {
        final PTree tree = Element.newPTree();
        final PTreeItem a = tree.add("a");
        final PTreeItem b = tree.add("b");
        assertFalse(a.isSelected());

        a.setSelected(true);
        assertTrue(a.isSelected());
        assertSame(a, tree.getSelectedItem());

        a.setSelected(true);
        assertSame(a, tree.getSelectedItem());

        b.setSelected(false);
        assertSame(a, tree.getSelectedItem());

        a.setSelected(false);
        assertFalse(a.isSelected());
        assertNull(tree.getSelectedItem());
    }

    @Test
    public void testAddChildrenAfterInit() {
        final PTree tree = Element.newPTree();
        final PTreeItem parent = tree.add("parent");
        PWindow.getMain().add(tree);

        writer.clear();
        final PTreeItem last = parent.add("last");
        assertTrue(last.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(last.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.PARENT_OBJECT_ID).contains(parent.getID()));

        writer.clear();
        final PTreeItem first = parent.add(0, Element.newPTreeItem("first"));
        assertTrue(first.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(first.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.INDEX).contains(0));
        assertSame(first, parent.get(0));
        assertSame(last, parent.get(1));
        assertEquals(2, parent.size());
    }

    @Test
    public void testAddAtIndexBeforeInit() {
        final PTree tree = Element.newPTree();
        final PTreeItem parent = tree.add("parent");
        final PTreeItem child = parent.add(0, Element.newPTreeItem("child"));
        assertFalse(child.isInitialized());

        PWindow.getMain().add(tree);
        assertTrue(child.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(child.getID()));
        assertFalse((writer.count(ServerToClientModel.INDEX) > 0));
        // children inherit the tree of their parent: selection goes through it
        child.setSelected(true);
        assertSame(child, tree.getSelectedItem());
    }

    @Test
    public void testRemoveAndRemoveFromParent() {
        final PTree tree = Element.newPTree();
        final PTreeItem parent = tree.add("parent");
        final PTreeItem a = parent.add("a");
        final PTreeItem b = parent.add("b");
        PWindow.getMain().add(tree);

        writer.clear();
        assertTrue(parent.remove(a));
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_REMOVE).contains(a.getID()));
        assertEquals(1, parent.size());

        b.removeFromParent();
        assertEquals(0, parent.size());
    }

    @Test
    public void testRootRemoveFromParentRemovesTree() {
        final PTree tree = Element.newPTree();
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.add(tree);
        assertEquals(1, panel.getWidgetCount());
        tree.getRoot().removeFromParent();
        assertEquals(0, panel.getWidgetCount());
    }

    @Test
    public void testWidgetItem() {
        final PLabel label = Element.newPLabel("label");
        final PTreeItem item = Element.newPTreeItem(label);
        assertSame(label, item.getWidget());

        final PTree tree = Element.newPTree();
        tree.add(item);
        PWindow.getMain().add(tree);

        assertSame(tree, label.getParent());
        assertTrue(label.isInitialized());
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(label.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.PARENT_OBJECT_ID).contains(item.getID()));
        assertTrue(writer.valuesOf(ServerToClientModel.WIDGET).contains(null));
    }

    @Test
    public void testWidgetItemDetachedFromPreviousParent() {
        final PLabel label = Element.newPLabel("label");
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.add(label);

        final PTree tree = Element.newPTree();
        tree.add(Element.newPTreeItem(label));
        PWindow.getMain().add(tree);

        assertEquals(0, panel.getWidgetCount());
        assertSame(tree, label.getParent());
    }

    @Test
    public void testDestroyPropagatesToChildren() {
        final PTree tree = Element.newPTree();
        final PTreeItem parent = tree.add("parent");
        final PTreeItem child = parent.add("child");
        PWindow.getMain().add(tree);
        parent.onDestroy();
        assertNull(parent.getWindow());
        assertNull(child.getWindow());
    }

    @Test
    public void testDumpDOM() {
        final PTreeItem lone = Element.newPTreeItem("x");
        final PTreeItem tmp = lone.add("y");
        lone.remove(tmp);
        assertEquals("<div item=\"x\"></div>", lone.dumpDOM());
    }

    @Test
    public void testRemoveFromLeafReturnsFalse() {
        final PTree tree = Element.newPTree();
        final PTreeItem leaf = tree.add("leaf");
        final PTreeItem other = tree.add("other");
        PWindow.getMain().add(tree);

        writer.clear();
        assertFalse(leaf.remove(other));
        assertEquals(0, writer.count(ServerToClientModel.TYPE_REMOVE));
    }

    @Test
    public void testRemoveNonChildReturnsFalse() {
        final PTree tree = Element.newPTree();
        final PTreeItem parent = tree.add("parent");
        parent.add("child");
        final PTreeItem other = tree.add("other");
        PWindow.getMain().add(tree);

        writer.clear();
        assertFalse(parent.remove(other));
        assertEquals(1, parent.size());
        assertEquals(0, writer.count(ServerToClientModel.TYPE_REMOVE));
    }
}
