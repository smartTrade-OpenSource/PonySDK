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

package com.ponysdk.core.ui.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;
import static org.junit.jupiter.api.Assertions.*;

public class GridWidgetTest extends PSuite {

    private static final List<Supplier<AbstractGridWidget>> FACTORIES = List.of(GridTableWidget::new, GridDivWidget::new);

    /**
     * Header row with "h0", "h1" and two body rows "r0c0", "r0c1" / "r1c0", "r1c1".
     */
    private static AbstractGridWidget newFilledGrid(final Supplier<AbstractGridWidget> factory) {
        final AbstractGridWidget grid = factory.get();
        grid.addWidget(Element.newPLabel("h0"), 0, 0, 1);
        grid.addWidget(Element.newPLabel("h1"), 1, 0, 1);
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                grid.addWidget(Element.newPLabel("r" + r + "c" + c), c, r + 1, 1);
            }
        }
        return grid;
    }

    private static String text(final PComplexPanel cell) {
        return ((PLabel) cell.getWidget(0)).getText();
    }

    private static List<String> rowTexts(final PComplexPanel row) {
        final List<String> texts = new ArrayList<>();
        for (int i = 0; i < row.getWidgetCount(); i++) {
            texts.add(text((PComplexPanel) row.getWidget(i)));
        }
        return texts;
    }

    @Test
    public void testStructure() {
        final GridTableWidget table = new GridTableWidget();
        assertEquals("table", table.getTagName());
        assertEquals("thead", ((PElement) table.getHead()).getTagName());
        assertEquals("tbody", ((PElement) table.getBody()).getTagName());
        assertTrue(table.hasStyleName("pony-SimpleList"));
        assertEquals("tr", ((PElement) table.createTableRow()).getTagName());

        final GridDivWidget div = new GridDivWidget();
        assertEquals("div", div.getTagName());
        div.addWidget(Element.newPLabel("h"), 0, 0, 1);
        div.addWidget(Element.newPLabel("c"), 0, 1, 1);
        final CapturingModelWriter writer = CapturingModelWriter.install();
        try {
            PWindow.getMain().add(div);
            assertEquals(List.of("table", "table-header-group", "table-row", "table-cell", "table-row-group", "table-row", "table-cell"),
                writer.valuesAfter(ServerToClientModel.PUT_STYLE_KEY, "display"));
        } finally {
            writer.uninstall();
            div.removeFromParent();
        }
    }

    @Test
    public void testAddWidgets() {
        for (final Supplier<AbstractGridWidget> factory : FACTORIES) {
            final AbstractGridWidget grid = newFilledGrid(factory);
            assertEquals(1, grid.getHead().getWidgetCount());
            assertEquals(2, grid.getBody().getWidgetCount());
            assertEquals(List.of("h0", "h1"), rowTexts(grid.getRow(0)));
            assertEquals(List.of("r1c0", "r1c1"), rowTexts(grid.getRow(2)));
            assertEquals("r0c1", text(grid.getCell(1, 1)));
            assertTrue(grid.getCell(1, 1).hasStyleName("ptc"));

            // Replacing a cell content
            grid.addWidget(Element.newPLabel("new"), 1, 1, 3);
            assertEquals("new", text(grid.getCell(1, 1)));
            assertEquals(1, grid.getCell(1, 1).getWidgetCount());
            assertEquals("3", grid.getCell(1, 1).getAttribute("colspan"));
            grid.addHeadWidget(Element.newPLabel("H1"), 1, 0, 2);
            assertEquals("H1", text(grid.getCell(0, 1)));
            assertEquals("2", grid.getCell(0, 1).getAttribute("colspan"));

            assertThrows(IndexOutOfBoundsException.class, () -> grid.addWidget(Element.newPLabel(), 0, -1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> grid.addWidget(Element.newPLabel(), -1, 0, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> grid.addBodyWidget(Element.newPLabel(), 0, -1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> grid.addBodyWidget(Element.newPLabel(), -1, 0, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> grid.addHeadWidget(Element.newPLabel(), 0, -1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> grid.addHeadWidget(Element.newPLabel(), -1, 0, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> grid.getRow(3));
            assertThrows(IndexOutOfBoundsException.class, () -> grid.getRow(-1));
        }
    }

    @Test
    public void testClearAndRemoveRows() {
        for (final Supplier<AbstractGridWidget> factory : FACTORIES) {
            final AbstractGridWidget grid = newFilledGrid(factory);
            grid.clear(2);
            assertEquals(1, grid.getBody().getWidgetCount());
            assertEquals(1, grid.getHead().getWidgetCount());
            grid.clear(1);
            assertEquals(0, grid.getBody().getWidgetCount());
            assertThrows(IndexOutOfBoundsException.class, () -> grid.clear(-1));

            final AbstractGridWidget grid2 = newFilledGrid(factory);
            grid2.removeRow(1);
            assertEquals(List.of("r1c0", "r1c1"), rowTexts(grid2.getRow(1)));
            assertTrue(grid2.remove(0));
            assertEquals(0, grid2.getHead().getWidgetCount());

            final AbstractGridWidget grid3 = newFilledGrid(factory);
            grid3.clearList();
            assertEquals(0, grid3.getHead().getWidgetCount());
            assertEquals(0, grid3.getBody().getWidgetCount());

            final AbstractGridWidget grid4 = newFilledGrid(factory);
            grid4.clear();
            assertEquals(0, grid4.getHead().getWidgetCount());
            assertEquals(0, grid4.getBody().getWidgetCount());
        }
    }

    @Test
    public void testInsertRow() {
        for (final Supplier<AbstractGridWidget> factory : FACTORIES) {
            final AbstractGridWidget grid = newFilledGrid(factory);
            grid.insertRow(0);
            assertEquals(2, grid.getHead().getWidgetCount());
            grid.insertRow(1);
            assertEquals(3, grid.getBody().getWidgetCount());
        }
    }

    @Test
    public void testMoveRow() {
        for (final Supplier<AbstractGridWidget> factory : FACTORIES) {
            final AbstractGridWidget grid = newFilledGrid(factory);
            grid.moveRow(1, 2);
            assertEquals(List.of("r1c0", "r1c1"), rowTexts(grid.getRow(1)));
            assertEquals(List.of("r0c0", "r0c1"), rowTexts(grid.getRow(2)));
        }
    }

    @Test
    public void testMoveBodyColumn() {
        for (final Supplier<AbstractGridWidget> factory : FACTORIES) {
            final AbstractGridWidget grid = newFilledGrid(factory);
            grid.moveColumn(1, 0);
            assertEquals(List.of("r0c1", "r0c0"), rowTexts(grid.getRow(1)));
            assertEquals(List.of("r1c1", "r1c0"), rowTexts(grid.getRow(2)));
        }
    }

    @Test
    public void testMoveColumnMovesHeader() {
        final AbstractGridWidget grid = newFilledGrid(GridTableWidget::new);
        grid.moveColumn(1, 0);
        assertEquals(List.of("h1", "h0"), rowTexts(grid.getRow(0)));
        assertEquals(List.of("r0c1", "r0c0"), rowTexts(grid.getRow(1)));
    }

    @Test
    public void testColumnIterator() {
        final AbstractGridWidget grid = newFilledGrid(GridTableWidget::new);
        final Iterator<PComplexPanel> iterator = grid.getColumnIterator(1);
        assertTrue(iterator.hasNext());
        assertEquals("r0c1", text(iterator.next()));
        assertEquals("r1c1", text(iterator.next()));
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    public void testColumnIteratorRemove() {
        final AbstractGridWidget grid = newFilledGrid(GridTableWidget::new);
        final Iterator<PComplexPanel> iterator = grid.getColumnIterator(1);
        iterator.next();
        iterator.remove();
        assertEquals(List.of("r0c0"), rowTexts(grid.getRow(1)));
        assertEquals(List.of("r1c0", "r1c1"), rowTexts(grid.getRow(2)));
    }

    @Test
    public void testStyles() {
        for (final Supplier<AbstractGridWidget> factory : FACTORIES) {
            final AbstractGridWidget grid = newFilledGrid(factory);
            grid.addRowStyle(1, "row");
            assertTrue(grid.getRow(1).hasStyleName("row"));
            grid.removeRowStyle(1, "row");
            assertFalse(grid.getRow(1).hasStyleName("row"));

            grid.addHeaderStyle("header");
            assertTrue(grid.getRow(0).hasStyleName("header"));

            grid.addCellStyle(2, 1, "cell");
            assertTrue(grid.getCell(2, 1).hasStyleName("cell"));
            grid.removeCellStyle(2, 1, "cell");
            assertFalse(grid.getCell(2, 1).hasStyleName("cell"));

            grid.addColumnStyle(0, "column");
            assertTrue(grid.getCell(1, 0).hasStyleName("column"));
            assertTrue(grid.getCell(2, 0).hasStyleName("column"));
            assertFalse(grid.getCell(0, 0).hasStyleName("column"));
            grid.removeColumnStyle(0, "column");
            assertFalse(grid.getCell(1, 0).hasStyleName("column"));
        }
    }

    @Test
    public void testColumnWidth() {
        final AbstractGridWidget grid = newFilledGrid(GridTableWidget::new);
        PWindow.getMain().add(grid);
        final CapturingModelWriter writer = CapturingModelWriter.install();
        try {
            grid.setColumnWidth(1, "50px");
            assertEquals(List.of("50px"), writer.valuesAfter(ServerToClientModel.PUT_STYLE_KEY, "width"));
            assertEquals(List.of(grid.getCell(0, 1).getID()), writer.valuesOf(ServerToClientModel.TYPE_UPDATE));
        } finally {
            writer.uninstall();
            grid.removeFromParent();
        }
    }

    @Test
    public void testRemoveColumn() {
        assertColumnRemoved(GridTableWidget::new);
    }

    @Test
    public void testRemoveColumnOnDivGrid() {
        assertColumnRemoved(GridDivWidget::new);
    }

    private static void assertColumnRemoved(final Supplier<AbstractGridWidget> factory) {
        final AbstractGridWidget grid = newFilledGrid(factory);
        grid.removeColumn(0);
        assertEquals(List.of("h1"), rowTexts(grid.getRow(0)));
        assertEquals(List.of("r0c1"), rowTexts(grid.getRow(1)));
        assertEquals(List.of("r1c1"), rowTexts(grid.getRow(2)));
    }

    @Test
    public void testInsertGoesToHeadOrBody() {
        final AbstractGridWidget grid = new GridTableWidget();
        final PComplexPanel headRow = grid.createTableRow();
        final PComplexPanel bodyRow = grid.createTableRow();
        grid.insert(headRow, 0);
        grid.insert(bodyRow, 3);
        assertSame(headRow, grid.getHead().getWidget(0));
        assertSame(bodyRow, grid.getBody().getWidget(0));
    }

    @Test
    public void testUnsupportedOperations() {
        final AbstractGridWidget grid = new GridDivWidget();
        assertThrows(IllegalAccessError.class, () -> grid.setColumns(2));
        assertThrows(IllegalAccessError.class, () -> grid.selectRow(0));
        assertThrows(IllegalAccessError.class, () -> grid.unSelectRow(0));
    }

    @Test
    public void testMoveTheHeadRowOntoItself() {
        for (final Supplier<AbstractGridWidget> factory : FACTORIES) {
            final AbstractGridWidget grid = newFilledGrid(factory);
            final List<String> head = rowTexts(grid.getRow(0));
            grid.moveRow(0, 0);
            assertEquals(head, rowTexts(grid.getRow(0)));
        }
    }
}
