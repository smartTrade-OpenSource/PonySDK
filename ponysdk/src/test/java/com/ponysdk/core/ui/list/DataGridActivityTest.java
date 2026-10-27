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

package com.ponysdk.core.ui.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.list.renderer.cell.LabelCellRenderer;
import com.ponysdk.core.ui.list.renderer.header.StringHeaderCellRenderer;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class DataGridActivityTest extends PSuite {

    private CapturingModelWriter writer;

    private final DefaultSimpleListView view = new DefaultSimpleListView();
    private final DataGridActivity<String> grid = new DataGridActivity<>(view);

    @AfterEach
    public void tearDown() {
        if (writer != null) writer.uninstall();
    }

    private void addColumns() {
        final IdentityDataGridColumnDescriptor<String> identity = new IdentityDataGridColumnDescriptor<>();
        identity.setHeaderCellRenderer(new StringHeaderCellRenderer("Value"));
        identity.setCellRenderer(new LabelCellRenderer<>());
        identity.setSubCellRenderer(new LabelCellRenderer<>("none"));
        grid.addDataGridColumnDescriptor(identity);

        final DataGridColumnDescriptor<String, Integer> length = new DataGridColumnDescriptor<>();
        length.setHeaderCellRenderer(new StringHeaderCellRenderer("Length"));
        length.setCellRenderer(new LabelCellRenderer<>());
        length.setSubCellRenderer(new LabelCellRenderer<>());
        length.setValueProvider(String::length);
        grid.addDataGridColumnDescriptor(length);
    }

    private String textAt(final int row, final int column) {
        return ((PLabel) view.getWidget(row, column)).getText();
    }

    @Test
    public void testColumnDescriptorsRenderHeaders() {
        addColumns();
        assertEquals(2, grid.getColumnDescriptors().size());
        assertEquals("Value", textAt(0, 0));
        assertEquals("Length", textAt(0, 1));
        assertSame(view, grid.getListView());
        assertSame(view, grid.asWidget());
    }

    @Test
    public void testSetData() {
        addColumns();
        grid.setData(Arrays.asList("a", "bbb"));

        assertEquals(2, grid.getRowCount());
        assertEquals(2, grid.getVisibleItemCount());
        assertEquals("a", textAt(1, 0));
        assertEquals("1", textAt(1, 1));
        assertEquals("bbb", textAt(2, 0));
        assertEquals("3", textAt(2, 1));
        assertTrue(view.getWidget(1, 2) instanceof PSimplePanel);
        assertEquals("bbb", grid.getRow(1));
        assertEquals(1, grid.getModelRowIndex("bbb"));
        assertEquals(-1, grid.getModelRowIndex("zzz"));

        grid.setData(List.of("c"));
        assertEquals(1, grid.getRowCount());
        assertEquals("c", textAt(1, 0));
        assertNull(view.getWidget(2, 0));
    }

    @Test
    public void testSubList() {
        addColumns();
        grid.setData(Arrays.asList("a", "b"));

        grid.insertSubList("a", Arrays.asList("a1", "a22"));
        assertEquals(4, grid.getRowCount());
        assertEquals(Arrays.asList("a", "a1", "a22", "b"), toList(grid.getVisibleItems()));
        assertEquals("a1", textAt(2, 0));
        assertEquals("3", textAt(3, 1));
        assertEquals("b", textAt(4, 0));

        grid.removeSubList("a");
        assertEquals(Arrays.asList("a", "b"), toList(grid.getVisibleItems()));
        assertEquals("b", textAt(2, 0));

        // Nothing registered for this father anymore
        grid.removeSubList("a");
        assertEquals(2, grid.getRowCount());

        grid.insertSubList("b", new ArrayList<>());
        assertEquals(2, grid.getRowCount());
    }

    @Test
    public void testSubCellRendererIsRequired() {
        final DataGridColumnDescriptor<String, String> descriptor = new DataGridColumnDescriptor<>();
        assertThrows(IllegalArgumentException.class, () -> descriptor.renderCell(0, "a"));
        assertThrows(IllegalArgumentException.class, () -> descriptor.renderSubCell(0, "a"));
        descriptor.setCellRenderer(new LabelCellRenderer<>());
        descriptor.setSubCellRenderer(new LabelCellRenderer<>());
        assertThrows(IllegalArgumentException.class, () -> descriptor.renderCell(0, "a"));
        assertThrows(IllegalArgumentException.class, () -> descriptor.renderSubCell(0, "a"));
        descriptor.setValueProvider(String::toUpperCase);
        assertEquals("A", ((PLabel) descriptor.renderCell(0, "a")).getText());
        assertEquals("A", ((PLabel) descriptor.renderSubCell(0, "a")).getText());
        assertTrue(descriptor.getCellRenderer() instanceof LabelCellRenderer);
        assertTrue(descriptor.getSubCellRenderer() instanceof LabelCellRenderer);
        assertEquals("B", descriptor.getValueProvider().apply("b"));
    }

    @Test
    public void testInsertRowAndRemove() {
        addColumns();
        grid.setData(Arrays.asList("a", "b"));

        final PLabel separator = Element.newPLabel("separator");
        grid.insertRow(1, 0, 2, separator);
        assertEquals(3, grid.getRowCount());
        assertSame(separator, view.getWidget(2, 0));
        // An inserted row carries no data, so it is not a visible item
        assertEquals(Arrays.asList("a", "b"), toList(grid.getVisibleItems()));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.insertRow(10, 0, 1, Element.newPLabel()));

        grid.remove(1);
        assertEquals(2, grid.getVisibleItemCount());
        grid.remove("a");
        assertEquals(1, grid.getVisibleItemCount());
        assertEquals("b", grid.getRow(0));
        assertEquals("b", textAt(1, 0));

        final PLabel extra = Element.newPLabel("extra");
        grid.insertWidget(1, 3, 1, extra);
        assertSame(extra, view.getWidget(1, 3));
    }

    @Test
    public void testVisibleItemsIteratorCannotRemove() {
        grid.setData(List.of("a"));
        final Iterator<String> iterator = grid.getVisibleItems().iterator();
        assertTrue(iterator.hasNext());
        assertEquals("a", iterator.next());
        assertFalse(iterator.hasNext());
        assertThrows(IllegalAccessError.class, iterator::remove);
    }

    @Test
    public void testClearAndSelection() {
        addColumns();
        PWindow.getMain().add(view);
        grid.setData(Arrays.asList("a", "b"));
        writer = CapturingModelWriter.install();

        grid.selectRow("b");
        assertEquals(List.of("pony-SimpleList-Row-Selected"), writer.valuesOf(ServerToClientModel.ROW_FORMATTER_ADD_STYLE_NAME));
        assertEquals(List.of(2), writer.valuesOf(ServerToClientModel.ROW));

        grid.unSelectRow("b");
        assertEquals(List.of("pony-SimpleList-Row-Selected"),
            writer.valuesOf(ServerToClientModel.ROW_FORMATTER_REMOVE_STYLE_NAME));

        // DefaultSimpleListView.clearList() also drops the header row
        grid.clear();
        assertEquals(0, view.getRowCount());
        view.removeFromParent();
    }

    private static List<String> toList(final Iterable<String> iterable) {
        final List<String> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list;
    }
}
