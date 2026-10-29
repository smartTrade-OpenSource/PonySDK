/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

/** Cell bookkeeping of PFlexTable / PGrid: a sparse grid of widgets whose rows shift on insert / remove. */
public class PHTMLTableTest extends PSuite {

    private PFlexTable table;
    private CapturingModelWriter writer;

    @BeforeEach
    public void setUp() {
        writer = CapturingModelWriter.install();
        table = Element.newPFlexTable();
        PWindow.getMain().add(table);
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private PLabel put(final int row, final int column) {
        final PLabel label = Element.newPLabel(row + "," + column);
        table.setWidget(row, column, label);
        return label;
    }

    private List<String> cells() {
        final List<String> out = new ArrayList<>();
        table.forEach(w -> out.add(((PLabel) w).getText()));
        return out;
    }

    @Test
    public void countsFollowTheHighestRowAndColumnHoldingAWidget() {
        assertEquals(0, table.getRowCount());
        put(0, 0);
        put(0, 2);
        final PLabel w = put(2, 1);
        assertEquals(3, table.getRowCount());
        assertEquals(3, table.getCellCount(0));
        assertEquals(0, table.getCellCount(1));
        assertEquals(2, table.getCellCount(2));
        assertSame(w, table.getWidget(2, 1));
        assertNull(table.getWidget(1, 1));
        assertNull(table.getWidget(9, 9));
        assertEquals(List.of("0,0", "0,2", "2,1"), cells());
    }

    @Test
    public void settingACellReplacesItsWidget() {
        final PLabel old = put(1, 1);
        final PLabel now = Element.newPLabel("new");
        table.setWidget(1, 1, now);
        assertNull(old.getParent());
        assertSame(table, now.getParent());
        assertSame(now, table.getWidget(1, 1));
        assertEquals(1, cells().size());
    }

    @Test
    public void clearingTheLastCellsShrinksTheCounts() {
        put(0, 0);
        put(3, 4);
        table.clearCell(3, 4);
        assertEquals(1, table.getRowCount());
        assertEquals(0, table.getCellCount(3));
        table.clearCell(0, 0);
        assertEquals(0, table.getRowCount());
        table.clearCell(5, 5); // nothing there
        assertEquals(0, table.getRowCount());
    }

    @Test
    public void insertingARowShiftsTheRowsBelow() {
        final PLabel a = put(0, 0);
        final PLabel b = put(1, 1);
        final PLabel c = put(2, 0);
        table.insertRow(1);
        assertSame(a, table.getWidget(0, 0));
        assertNull(table.getWidget(1, 1));
        assertSame(b, table.getWidget(2, 1));
        assertSame(c, table.getWidget(3, 0));
        assertEquals(4, table.getRowCount());
        assertTrue(table.remove(b)); // its new position is known
        assertNull(table.getWidget(2, 1));
    }

    @Test
    public void removingAnInsertedEmptyRowShiftsTheRowsBack() {
        final PLabel a = put(0, 0);
        final PLabel b = put(1, 1);
        table.insertRow(1);
        writer.clear();

        table.removeRow(1);
        assertEquals(List.of(1), writer.valuesOf(ServerToClientModel.CLEAR_ROW));
        assertSame(a, table.getWidget(0, 0));
        assertSame(b, table.getWidget(1, 1));
        assertNull(table.getWidget(2, 1));
        assertEquals(2, table.getRowCount());
        assertEquals(List.of("0,0", "1,1"), cells());
    }

    @Test
    public void removingARowDropsItsWidgetsAndShiftsTheRowsBelow() {
        final PLabel a = put(0, 0);
        final PLabel b = put(1, 0);
        final PLabel b2 = put(1, 3);
        final PLabel c = put(2, 2);
        table.removeRow(1);
        assertNull(b.getParent());
        assertNull(b2.getParent());
        assertSame(a, table.getWidget(0, 0));
        assertSame(c, table.getWidget(1, 2));
        assertEquals(2, table.getRowCount());
        assertEquals(List.of("0,0", "2,2"), cells());
        table.removeRow(7); // no row there: nothing happens
        assertEquals(2, table.getRowCount());
    }

    @Test
    public void removingAWidgetFreesItsCell() {
        final PLabel w = put(2, 3);
        assertFalse(table.remove(Element.newPLabel("stranger")));
        w.removeFromParent();
        assertNull(w.getParent());
        assertNull(table.getWidget(2, 3));
        assertEquals(0, table.getRowCount());
    }

    @Test
    public void clearRemovesEveryWidget() {
        final PLabel a = put(0, 0);
        final PLabel b = put(4, 2);
        table.clear();
        assertNull(a.getParent());
        assertNull(b.getParent());
        assertEquals(0, table.getRowCount());
        assertEquals(List.of(), cells());
        put(1, 1); // still usable
        assertEquals(2, table.getRowCount());
    }

    @Test
    public void cellsSetBeforeAttachAreCreatedWithTheTable() {
        final PFlexTable detached = Element.newPFlexTable();
        final PLabel w = Element.newPLabel("x");
        detached.setWidget(2, 5, w);
        PWindow.getMain().add(detached);
        assertSame(PWindow.getMain(), w.getWindow());
    }

    private Object last(final ServerToClientModel model) {
        final List<Object> values = writer.valuesOf(model);
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

    @Test
    public void settingAWidgetAddsItToItsCellOnTheTerminal() {
        writer.clear();
        final PLabel label = put(1, 2);
        assertSame(PWindow.getMain(), label.getWindow());
        assertEquals(List.of(label.getID()), writer.valuesOf(ServerToClientModel.TYPE_ADD));
        assertEquals(List.of(table.getID()), writer.valuesOf(ServerToClientModel.PARENT_OBJECT_ID));
        assertEquals(1, last(ServerToClientModel.ROW));
        assertEquals(2, last(ServerToClientModel.COLUMN));

        table.setWidget(0, 0, (IsPWidget) null);
        table.setWidget(0, 0, (PWidget) null);
        assertEquals(1, writer.count(ServerToClientModel.TYPE_ADD));
    }

    @Test
    public void removingAWidgetRemovesItFromTheTerminal() {
        final PLabel label = put(0, 0);
        writer.clear();
        assertTrue(table.remove(label));
        assertEquals(List.of(label.getID()), writer.valuesOf(ServerToClientModel.TYPE_REMOVE));
        assertFalse(table.remove(label));
        assertEquals(1, writer.count(ServerToClientModel.TYPE_REMOVE));
    }

    @Test
    public void clearSendsOneClearAndNoRemovePerWidget() {
        put(0, 0);
        put(1, 1);
        writer.clear();
        table.clear();
        assertEquals(1, writer.count(ServerToClientModel.CLEAR));
        assertEquals(0, writer.count(ServerToClientModel.TYPE_REMOVE));
    }

    @Test
    public void removingARowSendsItAndShiftsTheRowStyles() {
        put(0, 0);
        put(1, 0);
        put(2, 1);
        table.getRowFormatter().addStyleName(2, "third");
        writer.clear();

        table.removeRow(1);
        assertEquals(List.of(1), writer.valuesOf(ServerToClientModel.CLEAR_ROW));
        // The style of row 2 is now on row 1: adding it again there is a no-op
        writer.clear();
        table.getRowFormatter().addStyleName(1, "third");
        assertEquals(0, writer.count(ServerToClientModel.ROW_FORMATTER_ADD_STYLE_NAME));

        // The terminal has rows without widget (inserted, or emptied): their removal is sent too
        writer.clear();
        table.removeRow(9);
        assertEquals(List.of(9), writer.valuesOf(ServerToClientModel.CLEAR_ROW));
    }

    @Test
    public void removingARowWithoutWidgetShiftsTheRowStylesBelow() {
        final PLabel a = put(0, 0);
        table.insertRow(1);
        table.insertRow(2);
        table.getRowFormatter().addStyleName(0, "first");
        table.getRowFormatter().addStyleName(2, "third");
        writer.clear();

        table.removeRow(1);
        assertEquals(List.of(1), writer.valuesOf(ServerToClientModel.CLEAR_ROW));
        writer.clear();
        table.getRowFormatter().addStyleName(0, "first");
        table.getRowFormatter().addStyleName(1, "third");
        assertEquals(0, writer.count(ServerToClientModel.ROW_FORMATTER_ADD_STYLE_NAME), "styles moved up with their rows");
        assertSame(a, table.getWidget(0, 0));
        assertEquals(1, table.getRowCount());
    }

    @Test
    public void cellLookupOutsideTheTableAndWiderRows() {
        final PLabel a = put(0, 0);
        assertNull(table.getWidget(-1, 0));
        assertNull(table.getWidget(0, -1));
        assertNull(table.getWidget(0, 5));
        final PLabel far = put(0, 5);
        assertSame(far, table.getWidget(0, 5));
        assertSame(a, table.getWidget(0, 0));
        assertEquals(6, table.getCellCount(0));
        table.setWidget(0, 1, (IsPWidget) null);
        assertNull(table.getWidget(0, 1));
        assertEquals(0, table.getCellCount(9));
    }

    @Test
    public void insertingARowAboveAnEmptyRowShiftsTheWidgetsBelowIt() {
        final PLabel a = put(0, 0);
        final PLabel c = put(2, 0);
        table.insertRow(0);
        assertSame(a, table.getWidget(1, 0));
        assertSame(c, table.getWidget(3, 0));
        assertTrue(table.remove(c));
        assertEquals(2, table.getRowCount());
    }

    @Test
    public void insertingARowSendsItAndShiftsTheRowStyles() {
        put(0, 0);
        put(1, 0);
        table.getRowFormatter().addStyleName(0, "first");
        table.getRowFormatter().addStyleName(1, "second");
        writer.clear();

        table.insertRow(1);
        assertEquals(List.of(1), writer.valuesOf(ServerToClientModel.INSERT_ROW));
        writer.clear();
        table.getRowFormatter().addStyleName(0, "first");
        table.getRowFormatter().addStyleName(2, "second");
        assertEquals(0, writer.count(ServerToClientModel.ROW_FORMATTER_ADD_STYLE_NAME));
        table.getRowFormatter().addStyleName(1, "second");
        assertEquals(List.of("second"), writer.valuesOf(ServerToClientModel.ROW_FORMATTER_ADD_STYLE_NAME));

        // Past the last row: nothing to shift
        table.insertRow(10);
        assertEquals(3, table.getRowCount());
    }

    @Test
    public void paddingSpacingAndBorderAreSentOncePerChange() {
        writer.clear();
        table.setCellPadding(2);
        table.setCellPadding(2);
        table.setCellSpacing(3);
        table.setCellSpacing(3);
        table.setBorderWidth(4);
        table.setBorderWidth(4);
        assertEquals(2, table.getCellPadding());
        assertEquals(3, table.getCellSpacing());
        assertEquals(4, table.getBorderWidth());
        assertEquals(List.of(2), writer.valuesOf(ServerToClientModel.CELL_PADDING));
        assertEquals(List.of(3), writer.valuesOf(ServerToClientModel.CELL_SPACING));
        assertEquals(List.of(4), writer.valuesOf(ServerToClientModel.BORDER_WIDTH));
    }

    @Test
    public void rowFormatterSendsOnlyRealStyleChanges() {
        final PHTMLTable<PFlexCellFormatter>.PRowFormatter formatter = table.getRowFormatter();
        writer.clear();

        formatter.addStyleName(0, "a");
        formatter.addStyleName(0, "a");
        assertEquals(List.of("a"), writer.valuesOf(ServerToClientModel.ROW_FORMATTER_ADD_STYLE_NAME));
        assertEquals(0, last(ServerToClientModel.ROW));

        formatter.removeStyleName(0, "a");
        formatter.removeStyleName(0, "a");
        formatter.removeStyleName(5, "a");
        assertEquals(List.of("a"), writer.valuesOf(ServerToClientModel.ROW_FORMATTER_REMOVE_STYLE_NAME));

        formatter.setStyleName(1, "s");
        formatter.setStyleName(1, "t");
        assertEquals(List.of("s", "t"), writer.valuesOf(ServerToClientModel.ROW_FORMATTER_SET_STYLE_NAME));
        // setStyleName replaces the previous styles of the row
        writer.clear();
        formatter.removeStyleName(1, "s");
        assertEquals(0, writer.count(ServerToClientModel.ROW_FORMATTER_REMOVE_STYLE_NAME));
        formatter.removeStyleName(1, "t");
        assertEquals(List.of("t"), writer.valuesOf(ServerToClientModel.ROW_FORMATTER_REMOVE_STYLE_NAME));

        formatter.showRow(3);
        formatter.hideRow(4);
        assertEquals(List.of(3), writer.valuesOf(ServerToClientModel.ROW_FORMATTER_SHOW_ROW));
        assertEquals(List.of(4), writer.valuesOf(ServerToClientModel.ROW_FORMATTER_HIDE_ROW));
    }

    @Test
    public void columnFormatterSendsEachCall() {
        final PHTMLTable<PFlexCellFormatter>.PColumnFormatter formatter = table.getColumnFormatter();
        writer.clear();
        formatter.setWidth(1, "10px");
        assertEquals(List.of("10px"), writer.valuesOf(ServerToClientModel.COLUMN_FORMATTER_COLUMN_WIDTH));
        assertEquals(1, last(ServerToClientModel.COLUMN));
        formatter.addStyleName(2, "a");
        assertEquals(List.of("a"), writer.valuesOf(ServerToClientModel.COLUMN_FORMATTER_ADD_STYLE_NAME));
        assertEquals(2, last(ServerToClientModel.COLUMN));
        formatter.removeStyleName(3, "b");
        assertEquals(List.of("b"), writer.valuesOf(ServerToClientModel.COLUMN_FORMATTER_REMOVE_STYLE_NAME));
        formatter.setStyleName(4, "c");
        assertEquals(List.of("c"), writer.valuesOf(ServerToClientModel.COLUMN_FORMATTER_SET_STYLE_NAME));
        assertEquals(4, last(ServerToClientModel.COLUMN));
    }

    @Test
    public void cellFormatterSendsTheCellOfEachCall() {
        final PFlexCellFormatter formatter = table.getCellFormatter();
        writer.clear();

        formatter.addStyleName(1, 2, "a");
        assertEquals(List.of("a"), writer.valuesOf(ServerToClientModel.CELL_FORMATTER_ADD_STYLE_NAME));
        assertEquals(1, last(ServerToClientModel.ROW));
        assertEquals(2, last(ServerToClientModel.COLUMN));
        formatter.removeStyleName(1, 2, "b");
        assertEquals(List.of("b"), writer.valuesOf(ServerToClientModel.CELL_FORMATTER_REMOVE_STYLE_NAME));
        formatter.setStyleName(1, 2, "c");
        assertEquals(List.of("c"), writer.valuesOf(ServerToClientModel.CELL_FORMATTER_SET_STYLE_NAME));
        formatter.setVerticalAlignment(1, 2, PVerticalAlignment.ALIGN_MIDDLE);
        assertEquals(List.of(PVerticalAlignment.ALIGN_MIDDLE.getValue()), writer.valuesOf(ServerToClientModel.VERTICAL_ALIGNMENT));
        formatter.setHorizontalAlignment(1, 2, PHorizontalAlignment.ALIGN_RIGHT);
        assertEquals(List.of(PHorizontalAlignment.ALIGN_RIGHT.getValue()), writer.valuesOf(ServerToClientModel.HORIZONTAL_ALIGNMENT));

        formatter.setColSpan(3, 4, 2);
        assertEquals(List.of(2), writer.valuesOf(ServerToClientModel.SET_COL_SPAN));
        assertEquals(3, last(ServerToClientModel.ROW));
        assertEquals(4, last(ServerToClientModel.COLUMN));
        formatter.setRowSpan(5, 6, 3);
        assertEquals(List.of(3), writer.valuesOf(ServerToClientModel.SET_ROW_SPAN));
        assertEquals(5, last(ServerToClientModel.ROW));
    }

    @Test
    public void updatesBeforeAttachAreSentWithTheTable() {
        final PFlexTable detached = Element.newPFlexTable();
        detached.getCellFormatter().setColSpan(0, 0, 2);
        writer.clear();
        assertEquals(0, writer.count(ServerToClientModel.SET_COL_SPAN));
        PWindow.getMain().add(detached);
        assertEquals(List.of(2), writer.valuesOf(ServerToClientModel.SET_COL_SPAN));
    }

    @Test
    public void aWidgetRemovedBeforeAttachIsAddedThenRemoved() {
        final PFlexTable detached = Element.newPFlexTable();
        final PLabel label = Element.newPLabel("x");
        detached.setWidget(0, 0, label);
        assertTrue(detached.remove(label));
        writer.clear();
        PWindow.getMain().add(detached);
        // The add and the remove keep their order on the terminal
        final List<String> frames = writer.frames();
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(label.getID()));
        assertEquals(List.of(label.getID()), writer.valuesOf(ServerToClientModel.TYPE_REMOVE));
        assertTrue(frames.indexOf("TYPE_ADD=" + label.getID()) < frames.indexOf("TYPE_REMOVE=" + label.getID()));
        assertNull(detached.getWidget(0, 0));
    }

    @Test
    public void aWidgetMovedToAnotherTableBeforeAttachIsNotAddedHere() {
        final PFlexTable detached = Element.newPFlexTable();
        final PLabel label = Element.newPLabel("x");
        detached.setWidget(0, 0, label);
        final PFlexTable other = Element.newPFlexTable();
        other.setWidget(1, 1, label);
        assertSame(other, label.getParent());
        assertNull(detached.getWidget(0, 0));
        writer.clear();
        PWindow.getMain().add(detached);
        assertFalse(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(label.getID()));
        PWindow.getMain().add(other);
        assertTrue(writer.valuesOf(ServerToClientModel.TYPE_ADD).contains(label.getID()));
        assertEquals(List.of(other.getID()), writer.valuesAfter(ServerToClientModel.TYPE_ADD, label.getID()));
    }

    @Test
    public void dumpDomListsTheFilledCells() {
        final PLabel label = put(1, 1);
        assertEquals("<table pid=\"" + table.getID() + "\"><tr row = \"1\"><td col = \"1\">" + label.dumpDOM() + "</td></tr></table>",
            table.dumpDOM());
    }

    @Test
    public void theCellCountIgnoresTheEmptyEndOfARow() {
        put(0, 0);
        put(0, 1);
        final PLabel third = put(0, 2); // the row grows to 4 cells
        assertEquals(3, table.getCellCount(0));
        table.remove(third);
        assertEquals(2, table.getCellCount(0));
    }

    @Test
    public void aNullWidgetSetsNothing() {
        table.setWidget(0, 0, (IsPWidget) null);
        assertEquals(0, table.getRowCount());
    }

    @Test
    public void aWidgetMovedBeforeTheTableIsCreatedIsNotRemovedFromIt() {
        final PFlexTable detached = Element.newPFlexTable();
        final PLabel label = Element.newPLabel("moved");
        detached.setWidget(0, 0, label);
        final PFlowPanel elsewhere = Element.newPFlowPanel();
        elsewhere.add(label); // queued in the table: a remove; the label's add is the other panel's
        writer.clear();

        PWindow.getMain().add(detached);

        assertFalse(label.isInitialized());
        assertEquals(0, writer.count(ServerToClientModel.TYPE_REMOVE));
    }

    @Test
    public void aWidgetGivenAsIsPWidgetIsSet() {
        final PLabel label = Element.newPLabel("l");
        table.setWidget(0, 0, (IsPWidget) label);
        assertSame(label, table.getWidget(0, 0));
    }

    @Test
    public void aWidgetPutBackBeforeTheTableIsCreatedIsAddedOnly() {
        final PFlexTable detached = Element.newPFlexTable();
        final PLabel label = Element.newPLabel("back");
        detached.setWidget(0, 0, label);
        detached.remove(label);
        detached.setWidget(0, 0, label);
        writer.clear();

        PWindow.getMain().add(detached);

        assertTrue(label.isInitialized());
        assertEquals(0, writer.count(ServerToClientModel.TYPE_REMOVE));
        assertSame(label, detached.getWidget(0, 0));
    }
}
