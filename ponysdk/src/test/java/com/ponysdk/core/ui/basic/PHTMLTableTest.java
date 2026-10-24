/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.test.PSuite;

/** Cell bookkeeping of PFlexTable / PGrid: a sparse grid of widgets whose rows shift on insert / remove. */
public class PHTMLTableTest extends PSuite {

    private PFlexTable table;

    @BeforeEach
    public void setUp() {
        table = Element.newPFlexTable();
        PWindow.getMain().add(table);
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
}
