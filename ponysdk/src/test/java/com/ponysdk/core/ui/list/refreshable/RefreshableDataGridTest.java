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

package com.ponysdk.core.ui.list.refreshable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.list.DataGridColumnDescriptor;
import com.ponysdk.core.ui.list.DefaultSimpleListView;
import com.ponysdk.core.ui.list.IdentityDataGridColumnDescriptor;
import com.ponysdk.core.ui.list.renderer.cell.LabelCellRenderer;
import com.ponysdk.core.ui.list.renderer.header.StringHeaderCellRenderer;
import com.ponysdk.test.PSuite;

public class RefreshableDataGridTest extends PSuite {

    private final DefaultSimpleListView view = new DefaultSimpleListView();
    private final RefreshableDataGrid<String, String> grid = new RefreshableDataGrid<>(view);
    private final IdentityDataGridColumnDescriptor<String> valueColumn = new IdentityDataGridColumnDescriptor<>();
    private final DataGridColumnDescriptor<String, Integer> lengthColumn = new DataGridColumnDescriptor<>();

    public RefreshableDataGridTest() {
        valueColumn.setHeaderCellRenderer(new StringHeaderCellRenderer("Value"));
        valueColumn.setCellRenderer(new LabelCellRenderer<>());
        grid.addDataGridColumnDescriptor(valueColumn);
        lengthColumn.setHeaderCellRenderer(new StringHeaderCellRenderer("Length"));
        lengthColumn.setCellRenderer(new LabelCellRenderer<>());
        lengthColumn.setValueProvider(String::length);
        grid.addDataGridColumnDescriptor(lengthColumn);
    }

    private String textAt(final int row, final int column) {
        return ((PLabel) view.getWidget(row, column)).getText();
    }

    @Test
    public void testAddAndUpdateInPlace() {
        grid.setData(0, "k1", "a");
        grid.setData(1, "k2", "bb");
        assertEquals(2, grid.getVisibleItemCount());
        assertEquals("a", textAt(1, 0));
        assertEquals("2", textAt(2, 1));
        assertEquals("a", grid.getViewData(0));
        assertEquals("bb", grid.getViewData(1));
        assertNull(grid.getViewData(5));
        assertEquals(1, grid.getViewRowDataIndex("bb"));

        final PLabel before = (PLabel) view.getWidget(1, 0);
        grid.setData(0, "k1", "aaa");
        // Same widget, refreshed content
        assertSame(before, view.getWidget(1, 0));
        assertEquals("aaa", textAt(1, 0));
        assertEquals("3", textAt(1, 1));
        assertEquals("aaa", grid.getViewData(0));
        assertEquals(2, grid.getRowCount());
    }

    @Test
    public void testUpdateWithMovedOrNewKey() {
        grid.setData(0, "k1", "a");
        grid.setData(1, "k2", "b");

        // k1 moves to row 1
        grid.setData(1, "k1", "a2");
        assertEquals("b", grid.getRow(0));
        assertEquals("a2", grid.getRow(1));
        assertEquals("a2", textAt(2, 0));
        assertEquals(1, grid.removeByKey("k1"));
        assertEquals(1, grid.getRowCount());

        // An unknown key replaces the row content
        grid.setData(0, "k3", "c");
        assertEquals("c", grid.getRow(0));
        assertEquals("c", textAt(1, 0));
        assertEquals(0, grid.removeByKey("k3"));
        assertEquals(-1, grid.removeByKey("k3"));
    }

    @Test
    public void testRemoveAndClear() {
        grid.setData(0, "k1", "a");
        grid.setData(1, "k2", "b");
        grid.setData(2, "k3", "c");

        assertEquals(1, grid.removeByKey("k2"));
        assertEquals(2, grid.getVisibleItemCount());
        assertEquals(List.of("a", "c"), List.of(grid.getRow(0), grid.getRow(1)));
        assertEquals("c", textAt(2, 0));

        grid.clear();
        assertEquals(0, grid.getRowCount());
        assertEquals(1, view.getRowCount());
        assertEquals("Value", textAt(0, 0));
        assertNull(grid.getViewData(0));
    }

    @Test
    public void testViewDataAfterRemove() {
        grid.setData(0, "k1", "a");
        grid.setData(1, "k2", "b");
        grid.removeByKey("k1");
        assertEquals("b", grid.getViewData(0));
    }

    @Test
    public void testMoveColumn() {
        grid.moveColumn(1, 1);
        assertEquals(List.of(valueColumn, lengthColumn), grid.getColumnDescriptors());
        // The default list view does not support moving columns, the model is reordered first
        assertThrows(UnsupportedOperationException.class, () -> grid.moveColumn(0, 1));
        assertEquals(List.of(lengthColumn, valueColumn), grid.getColumnDescriptors());
    }

    @Test
    public void testIndexBasedApiIsRejected() {
        assertThrows(RuntimeException.class, () -> grid.setData(0, "a"));
        assertThrows(RuntimeException.class, () -> grid.setData(List.of("a")));
        assertThrows(RuntimeException.class, () -> grid.remove("a"));
    }

    @Test
    public void testCell() {
        final Cell<String, PLabel> cell = new Cell<>();
        final PLabel label = Element.newPLabel();
        cell.setWidget(label);
        cell.setData("data");
        cell.setRow(2);
        cell.setCol(3);
        assertSame(label, cell.getWidget());
        assertEquals("data", cell.getData());
        assertEquals(2, cell.getRow());
        assertEquals(3, cell.getCol());
    }
}
