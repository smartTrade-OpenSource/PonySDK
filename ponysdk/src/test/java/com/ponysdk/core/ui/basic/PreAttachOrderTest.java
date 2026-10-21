/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

/**
 * What a table or a tab panel is given before it is on the terminal reaches the terminal as if it had been there:
 * the adds in order with the rows, the tab widgets created, the tabs in their order.
 */
public class PreAttachOrderTest extends PSuite {

    private CapturingModelWriter writer;

    @Before
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @After
    public void restoreWriter() {
        writer.uninstall();
    }

    /** The position of the n-th write of a model with a value. */
    private int indexOf(final ServerToClientModel model, final Object value) {
        final List<ServerToClientModel> models = writer.models();
        final List<Object> values = writer.valuesOf(model);
        int seen = 0;
        for (int i = 0; i < models.size(); i++) {
            if (models.get(i) != model) continue;
            if (values.get(seen++).equals(value)) return i;
        }
        throw new AssertionError(model + " " + value + " not written");
    }

    @Test
    public void aTableReplaysItsAddsInOrderWithItsRows() {
        final PFlexTable table = Element.newPFlexTable();
        final PLabel first = Element.newPLabel("first");
        final PLabel second = Element.newPLabel("second");
        table.setWidget(0, 0, first);
        table.insertRow(0); // first moves to row 1
        table.setWidget(0, 0, second);
        writer.clear();

        PWindow.getMain().add(table);

        final int addFirst = indexOf(ServerToClientModel.TYPE_ADD, first.getID());
        final int insert = indexOf(ServerToClientModel.INSERT_ROW, 0);
        final int addSecond = indexOf(ServerToClientModel.TYPE_ADD, second.getID());
        assertTrue("add, insert, add: " + writer.models(), addFirst < insert && insert < addSecond);
        assertEquals(2, table.getRowCount()); // the server's view: second at row 0, first at row 1
        assertEquals(first, table.getWidget(1, 0));
    }

    @Test
    public void aWidgetRemovedBeforehandIsAddedThenRemovedInItsTurn() {
        final PFlexTable table = Element.newPFlexTable();
        final PLabel gone = Element.newPLabel("gone");
        table.setWidget(0, 0, gone);
        table.remove(gone);
        table.setWidget(0, 0, Element.newPLabel("kept"));
        writer.clear();

        PWindow.getMain().add(table);

        assertTrue(indexOf(ServerToClientModel.TYPE_ADD, gone.getID()) < indexOf(ServerToClientModel.TYPE_REMOVE, gone.getID()));
    }

    @Test
    public void tabsGivenBeforehandHaveTheirWidgetsAndTheirOrder() {
        final PTabLayoutPanel tabs = Element.newPTabLayoutPanel();
        final PLabel a = Element.newPLabel("A");
        final PLabel b = Element.newPLabel("B");
        final PLabel c = Element.newPLabel("C");
        final PLabel tabOfA = Element.newPLabel("tab A");
        final PLabel tabOfC = Element.newPLabel("tab C");
        tabs.add(a, tabOfA);
        tabs.add(b, "tab B");
        tabs.insert(c, tabOfC, 0); // first
        writer.clear();

        PWindow.getMain().add(tabs);

        assertTrue("the tab widgets exist", tabOfA.isInitialized() && tabOfC.isInitialized());
        assertTrue(indexOf(ServerToClientModel.TYPE_CREATE, tabOfA.getID()) < indexOf(ServerToClientModel.TAB_WIDGET, tabOfA.getID()));
        assertTrue(indexOf(ServerToClientModel.TYPE_CREATE, tabOfC.getID()) < indexOf(ServerToClientModel.TAB_WIDGET, tabOfC.getID()));
        final List<Object> added = writer.valuesOf(ServerToClientModel.TYPE_ADD);
        final List<Object> tabsAdded = added.stream().filter(id -> List.of(a.getID(), b.getID(), c.getID()).contains(id)).toList();
        assertEquals("in their final order", List.of(c.getID(), a.getID(), b.getID()), tabsAdded);
        assertEquals("no index: the order is the adds'", 0, writer.count(ServerToClientModel.BEFORE_INDEX));
    }

    @Test
    public void tabsInsertedOnceAttachedKeepTheirIndex() {
        final PTabLayoutPanel tabs = Element.newPTabLayoutPanel();
        PWindow.getMain().add(tabs);
        tabs.add(Element.newPLabel("A"), "tab A");
        writer.clear();
        final PLabel tabOfB = Element.newPLabel("tab B");
        tabs.insert(Element.newPLabel("B"), tabOfB, 0);
        assertEquals(List.of(0), writer.valuesOf(ServerToClientModel.BEFORE_INDEX));
        assertTrue(tabOfB.isInitialized());
    }
}
