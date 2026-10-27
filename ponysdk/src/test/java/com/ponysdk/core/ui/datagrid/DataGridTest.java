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

package com.ponysdk.core.ui.datagrid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid.impl.DefaultView;
import com.ponysdk.core.ui.datagrid.impl.PHTMLCellRenderer;
import com.ponysdk.core.ui.datagrid.impl.PLabelCellRenderer;
import com.ponysdk.core.ui.datagrid.impl.PLabelHeaderRenderer;
import com.ponysdk.test.PSuite;

public class DataGridTest extends PSuite {

    record Item(String key, int rank, String label) {
    }

    private static final Comparator<Item> BY_RANK = Comparator.comparingInt(Item::rank);

    private static String cellText(final View view, final int row, final int column) {
        return ((PLabel) view.getCell(row, column)).getText();
    }

    private static List<String> column(final View view, final int column) {
        final List<String> texts = new ArrayList<>();
        for (int r = 0; r < view.getRowCount(); r++) {
            texts.add(cellText(view, r, column));
        }
        return texts;
    }

    @Test
    public void testSortedRendering() {
        final DefaultView view = new DefaultView();
        final DataGrid<String> grid = new DataGrid<>(view, Function.identity());
        assertSame(view.asWidget(), grid.asWidget());
        final ColumnDescriptor<String> valueColumn = ColumnDescriptor.newDefault("Value", s -> s);
        grid.addColumnDescriptor(valueColumn);
        assertEquals("Value", ((PLabel) view.getHeader(0)).getText());

        grid.addData("b");
        grid.addData(List.of("c", "a"));
        grid.newConsumer().accept("d");
        assertEquals(List.of("a", "b", "c", "d"), column(view, 0));
        assertEquals(List.of("a", "b", "c", "d"), new ArrayList<>(grid.getData()));
        assertThrows(UnsupportedOperationException.class, () -> grid.getData().clear());

        // Adding a column draws the existing rows
        grid.addColumnDescriptor(ColumnDescriptor.newDefault("Upper", String::toUpperCase));
        assertEquals(List.of("A", "B", "C", "D"), column(view, 1));
        assertEquals(2, grid.getColumns().size());

        // Same data again: drawn in place
        final PWidget before = view.getCell(1, 0);
        grid.addData("b");
        assertSame(before, view.getCell(1, 0));
        assertEquals(4, grid.getData().size());

        grid.removeData("b");
        assertEquals(List.of("a", "c", "d", ""), column(view, 0));
        assertEquals(List.of("A", "C", "D", ""), column(view, 1));
        assertEquals(3, grid.getData().size());

        grid.removeColumn(valueColumn);
        assertEquals(List.of(grid.getColumns().get(0)), grid.getColumns());
        assertEquals("Upper", ((PLabel) view.getHeader(0)).getText());
        assertNull(view.getHeader(1));
        assertEquals(List.of("A", "C", "D", ""), column(view, 0));
        assertEquals("", cellText(view, 0, 1));

        grid.clear();
        assertTrue(grid.getData().isEmpty());
        assertEquals(List.of("", "", "", ""), column(view, 0));

        // Removing a column that is not displayed is a no-op
        grid.removeColumn(valueColumn);
        assertEquals(1, grid.getColumns().size());
    }

    @Test
    public void testRemoveColumnOnEmptyGrid() {
        final DataGrid<String> grid = new DataGrid<>();
        grid.removeColumn(ColumnDescriptor.newDefault("x", s -> s));
        assertTrue(grid.getColumns().isEmpty());
        assertTrue(grid.asWidget() instanceof PFlexTable);
    }

    @Test
    public void testComparatorAndKeyProvider() {
        final DefaultView view = new DefaultView();
        final DataGrid<Item> grid = new DataGrid<>(view, Item::key, BY_RANK);
        grid.addColumnDescriptor(ColumnDescriptor.newDefault("Label", Item::label));
        grid.addColumnDescriptor(ColumnDescriptor.newDefault("Rank", Item::rank, String::valueOf));
        grid.addData(new Item("k2", 2, "two"));
        grid.addData(new Item("k1", 1, "one"));
        assertEquals(List.of("one", "two"), column(view, 0));
        assertEquals(List.of("1", "2"), column(view, 1));

        // Same key, same rank: the row is refreshed in place
        grid.addData(new Item("k1", 1, "uno"));
        assertEquals(List.of("uno", "two"), column(view, 0));
        assertEquals(2, grid.getData().size());

        grid.update(new Item("k2", 2, "two"), item -> new Item(item.key(), item.rank(), "dos"));
        assertEquals(2, grid.getData().size());
        assertEquals("dos", grid.getData().stream().filter(i -> i.key().equals("k2")).findFirst().orElseThrow().label());

        grid.update(new Item("k3", 3, "three"), Function.identity());
        assertEquals(3, view.getRowCount());
        assertEquals("three", cellText(view, 2, 0));
    }

    @Test
    public void testUpdateDrawsMergedValue() {
        final DefaultView view = new DefaultView();
        final DataGrid<Item> grid = new DataGrid<>(view, Item::key, BY_RANK);
        grid.addColumnDescriptor(ColumnDescriptor.newDefault("Label", Item::label));
        grid.addData(new Item("k1", 1, "one"));
        grid.update(new Item("k1", 1, "one"), item -> new Item(item.key(), item.rank(), "uno"));
        assertEquals("uno", cellText(view, 0, 0));
    }

    @Test
    public void testUpdatedSortValueMovesTheRow() {
        final DefaultView view = new DefaultView();
        final DataGrid<Item> grid = new DataGrid<>(view, Item::key, BY_RANK);
        grid.addColumnDescriptor(ColumnDescriptor.newDefault("Label", Item::label));
        grid.addData(new Item("k1", 1, "one"));
        grid.addData(new Item("k2", 2, "two"));
        grid.addData(new Item("k1", 3, "three"));
        assertEquals(2, grid.getData().size());
        assertEquals(List.of("two", "three"), column(view, 0));
    }

    @Test
    public void testUpdatedSortValueMovesTheRowUp() {
        final DefaultView view = new DefaultView();
        final DataGrid<Item> grid = new DataGrid<>(view, Item::key, BY_RANK);
        grid.addColumnDescriptor(ColumnDescriptor.newDefault("Label", Item::label));
        grid.addData(new Item("k1", 1, "one"));
        grid.addData(new Item("k2", 2, "two"));
        grid.addData(new Item("k3", 3, "three"));
        grid.addData(new Item("k3", 0, "zero"));
        assertEquals(List.of("zero", "one", "two"), column(view, 0));
    }

    @Test
    public void testRemoveUnknownDataKeepsRows() {
        final DefaultView view = new DefaultView();
        final DataGrid<String> grid = new DataGrid<>(view, Function.identity());
        grid.addColumnDescriptor(ColumnDescriptor.newDefault("Value", s -> s));
        grid.addData(List.of("a", "c"));
        grid.removeData("b");
        assertEquals(List.of("a", "c"), column(view, 0));
        grid.removeData("c");
        assertEquals(List.of("a", ""), column(view, 0));
    }

    @Test
    public void testChainedTransforms() {
        final ColumnDescriptor<Item> twoSteps = ColumnDescriptor.newDefault("Label length", Item::label, l -> l.length() + "");
        final ColumnDescriptor<Item> threeSteps = ColumnDescriptor.newDefault("Twice", Item::label, String::length,
            l -> String.valueOf(l * 2));
        final Item item = new Item("k", 1, "abc");
        final Item noLabel = new Item("k", 1, null);
        assertEquals("3", ((PLabel) twoSteps.getCellRenderer().render(item)).getText());
        assertEquals("6", ((PLabel) threeSteps.getCellRenderer().render(item)).getText());
        assertNull(((PLabel) twoSteps.getCellRenderer().render(noLabel)).getText());
        assertNull(((PLabel) threeSteps.getCellRenderer().render(noLabel)).getText());

        final ColumnDescriptor<String> nullSecondStep = ColumnDescriptor.newDefault("x", s -> s, s -> (String) null, s -> "never");
        assertNull(((PLabel) nullSecondStep.getCellRenderer().render("a")).getText());
        assertEquals("Twice", ((PLabel) threeSteps.getHeaderRenderer().render()).getText());
    }

    @Test
    public void testCellRenderers() {
        final PLabelCellRenderer<Integer> label = new PLabelCellRenderer<>();
        final PLabel l = label.render(5);
        assertEquals("5", l.getText());
        assertSame(l, label.update(6, l));
        assertEquals("6", l.getText());
        label.reset(l);
        assertEquals("", l.getText());
        label.reset(null);

        final PHTMLCellRenderer<Integer> html = new PHTMLCellRenderer<>();
        final PHTML h = html.render(1);
        assertEquals("1", h.getHTML());
        final PHTMLCellRenderer<String> bold = new PHTMLCellRenderer<>(s -> "<b>" + s + "</b>");
        final PHTML b = bold.render("a");
        assertEquals("<b>a</b>", b.getHTML());
        assertSame(b, bold.update("c", b));
        assertEquals("<b>c</b>", b.getHTML());
        bold.reset(b);
        assertEquals("", b.getHTML());
        bold.reset(null);

        final PLabelHeaderRenderer header = new PLabelHeaderRenderer("H");
        assertSame(header.render(), header.render());

        final ColumnDescriptor<String> descriptor = new ColumnDescriptor<>();
        descriptor.setHeaderRenderer(header);
        descriptor.setCellRenderer(bold);
        assertSame(header, descriptor.getHeaderRenderer());
        assertSame(bold, descriptor.getCellRenderer());
    }

    @Test
    public void testDefaultView() {
        final DefaultView view = new DefaultView();
        assertEquals(-1, view.getRowCount());
        final PWidget header = new PLabelHeaderRenderer("h").render();
        view.setHeader(0, header);
        view.setCell(1, 0, new PLabelCellRenderer<String>().render("c"));
        assertSame(header, view.getHeader(0));
        assertEquals("c", cellText(view, 1, 0));
        assertEquals(2, view.getRowCount());
        assertNull(view.getCell(0, 0));
    }

    @Test
    public void testTreeSet() {
        final DataGridTreeSet<Item> set = new DataGridTreeSet<>(BY_RANK, Item::key);
        final Item a = new Item("a", 2, "");
        final Item b = new Item("b", 1, "");
        set.add(a);
        set.add(b);
        assertEquals(1, set.getPosition(a));
        assertEquals(0, set.getPosition(b));
        assertTrue(set.containsData(new Item("a", 9, "other")));
        assertTrue(set.remove(b));
        assertFalse(set.containsData(b));
        set.clear();
        assertTrue(set.isEmpty());
        assertFalse(set.containsData(a));

        final DataGrid<String> sortedDesc = new DataGrid<>(Comparator.<String> reverseOrder());
        sortedDesc.addColumnDescriptor(ColumnDescriptor.newDefault("v", s -> s));
        sortedDesc.addData(List.of("a", "b"));
        assertEquals(List.of("b", "a"), new ArrayList<>(sortedDesc.getData()));

        final DataGrid<String> byLength = new DataGrid<>((Function<String, ?>) String::length);
        byLength.addData(List.of("a", "bb"));
        assertEquals(2, byLength.getData().size());
    }
}
