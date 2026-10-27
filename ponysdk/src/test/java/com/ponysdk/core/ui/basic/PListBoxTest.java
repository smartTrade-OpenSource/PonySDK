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
import java.util.Arrays;
import java.util.List;

import jakarta.json.Json;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.PListBox.ListItem;
import com.ponysdk.core.ui.basic.event.PChangeEvent;
import com.ponysdk.core.ui.basic.event.PChangeHandler;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class PListBoxTest extends PSuite {

    private CapturingModelWriter writer;

    @BeforeEach
    public void captureWrites() {
        writer = CapturingModelWriter.install();
    }

    @AfterEach
    public void restoreWriter() {
        writer.uninstall();
    }

    private Object last(final ServerToClientModel model) {
        final List<Object> values = writer.valuesOf(model);
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

    private static PListBox attachedListBox() {
        final PListBox listBox = Element.newPListBox();
        PWindow.getMain().add(listBox);
        return listBox;
    }

    @Test
    public void testInit() {
        final PListBox listBox = Element.newPListBox();
        assertEquals(WidgetType.LISTBOX, listBox.getWidgetType());
        assertFalse(listBox.isEmptySelection());
        assertEquals(0, listBox.getItemCount());
        assertEquals(-1, listBox.getSelectedIndex());
        assertNull(listBox.getSelectedItem());
        assertNull(listBox.getSelectedValue());
    }

    @Test
    public void testEmptyItem() {
        final PListBox listBox = Element.newPListBox(true);
        assertTrue(listBox.isEmptySelection());
        assertEquals(1, listBox.getItemCount());
        assertEquals("", listBox.getItem(0));
        assertNull(listBox.getValue(0));

        listBox.addItem("a");
        listBox.clear();
        assertEquals(1, listBox.getItemCount());
        assertEquals("", listBox.getItem(0));
    }

    @Test
    public void testAddItem() {
        final PListBox listBox = attachedListBox();
        listBox.addItem("a");
        listBox.addItem("b", 2);
        assertEquals(2, listBox.getItemCount());
        assertEquals("a", listBox.getValue(0));
        assertEquals(2, listBox.getValue(1));
        assertEquals(List.of("a", "b"), writer.valuesOf(ServerToClientModel.ITEM_INSERTED));
    }

    @Test
    public void testAddNullItem() {
        final PListBox listBox = Element.newPListBox();
        assertThrows(NullPointerException.class, () -> listBox.addItem(null));
    }

    @Test
    public void testInsertItem() {
        final PListBox listBox = attachedListBox();
        listBox.addItem("a");
        listBox.insertItem("first", 0);
        assertEquals("first", listBox.getItem(0));
        assertEquals(0, last(ServerToClientModel.INDEX));

        // Out of range index appends
        listBox.insertItem("last", "lastValue", 99);
        assertEquals("last", listBox.getItem(2));
        assertEquals("lastValue", listBox.getValue(2));
        assertEquals(2, last(ServerToClientModel.INDEX));

        listBox.insertItem("neg", -1);
        assertEquals("neg", listBox.getItem(3));
    }

    @Test
    public void testAddItemsInGroup() {
        final PListBox listBox = attachedListBox();
        listBox.addItemsInGroup("group", "a", "b");
        // The group itself is an item
        assertEquals(3, listBox.getItemCount());
        assertEquals("group", listBox.getItem(0));
        assertNull(listBox.getValue(0));
        assertEquals("b", listBox.getValue(2));
        assertArrayEquals(new String[] { "a", "b" }, (String[]) last(ServerToClientModel.ITEM_ADD));
        assertEquals("group", last(ServerToClientModel.ITEM_GROUP));
    }

    @Test
    public void testSetItemTextAndValue() {
        final PListBox listBox = attachedListBox();
        listBox.addItem("a");
        listBox.setItemText(0, "b");
        listBox.setValue(0, 5);
        assertEquals("b", listBox.getItem(0));
        assertEquals(5, listBox.getValue(0));
        assertEquals(List.of("b"), writer.valuesOf(ServerToClientModel.ITEM_UPDATED));
    }

    @Test
    public void testSetItemTextOutOfBounds() {
        final PListBox listBox = Element.newPListBox();
        assertThrows(IndexOutOfBoundsException.class, () -> listBox.setItemText(0, "a"));
    }

    @Test
    public void testRemoveItemByIndex() {
        final PListBox listBox = attachedListBox();
        listBox.addItem("a");
        listBox.addItem("b");
        listBox.addItem("c");
        listBox.setSelectedIndex(2);

        final ListItem removed = listBox.removeItem(0);
        assertEquals("a", removed.getLabel());
        assertEquals("a", removed.getValue());
        assertEquals(2, listBox.getItemCount());
        assertEquals(0, last(ServerToClientModel.ITEM_REMOVED));
        // The selection follows the selected item
        assertEquals(1, listBox.getSelectedIndex());
        assertEquals("c", listBox.getSelectedItem());
    }

    @Test
    public void testRemoveSelectedItemSelectsNoOtherItem() {
        final PListBox listBox = attachedListBox();
        listBox.setMultipleSelect(true);
        listBox.addItem("a");
        listBox.addItem("b");
        listBox.addItem("c");
        listBox.addItem("d");
        listBox.setSelectedIndex(1, true);
        listBox.setSelectedIndex(3, true);

        writer.clear();
        listBox.removeItem(3);
        assertEquals(List.of(1), listBox.getSelectedIndexes());
        assertEquals(1, listBox.getSelectedIndex());
        // The terminal removes the option itself: no selection is sent
        assertEquals(0, writer.count(ServerToClientModel.SELECTED));

        listBox.removeItem(0);
        // The selection follows the selected item
        assertEquals(List.of(0), listBox.getSelectedIndexes());
        assertEquals("b", listBox.getSelectedItem());

        listBox.removeItem(0);
        assertTrue(listBox.getSelectedIndexes().isEmpty());
        assertEquals(-1, listBox.getSelectedIndex());
        assertNull(listBox.getSelectedItem());
        assertEquals(0, writer.count(ServerToClientModel.SELECTED));
    }

    @Test
    public void testRemoveItemByLabel() {
        final PListBox listBox = attachedListBox();
        listBox.addItem("a");
        listBox.addItem("b");
        listBox.addItem("a");
        listBox.removeItem("a");
        assertEquals(1, listBox.getItemCount());
        assertEquals("b", listBox.getItem(0));
        assertEquals(2, writer.count(ServerToClientModel.ITEM_REMOVED));
    }

    @Test
    public void testRemoveValue() {
        final PListBox listBox = attachedListBox();
        listBox.addItem("a", 1);
        listBox.addItem("b", 2);
        listBox.addItem("c", 1);
        listBox.removeValue(1);
        assertEquals(1, listBox.getItemCount());
        assertEquals("b", listBox.getItem(0));
        listBox.removeValue(42);
        assertEquals(1, listBox.getItemCount());
    }

    @Test
    public void testClear() {
        final PListBox listBox = attachedListBox();
        listBox.addItem("a");
        listBox.setSelectedIndex(0);
        listBox.clear();
        assertEquals(0, listBox.getItemCount());
        assertEquals(-1, listBox.getSelectedIndex());
        assertTrue(listBox.getSelectedIndexes().isEmpty());
        assertEquals(1, writer.count(ServerToClientModel.CLEAR));
    }

    @Test
    public void testSelection() {
        final PListBox listBox = attachedListBox();
        listBox.addItem("a", 1);
        listBox.addItem("b", 2);
        listBox.addItem("c", 3);

        listBox.setSelectedItem("b");
        assertEquals(1, listBox.getSelectedIndex());
        assertEquals("b", listBox.getSelectedItem());
        assertEquals(2, listBox.getSelectedValue());
        assertEquals(List.of(true), writer.valuesOf(ServerToClientModel.SELECTED));
        assertEquals(1, last(ServerToClientModel.INDEX));

        listBox.setSelectedValue(3);
        assertEquals(2, listBox.getSelectedIndex());

        listBox.setSelectedItem("unknown");
        listBox.setSelectedValue("unknown");
        assertEquals(2, listBox.getSelectedIndex());

        // Single selection never fills the selected indexes
        assertFalse(listBox.isItemSelected(2));
        assertTrue(listBox.getSelectedIndexes().isEmpty());
    }

    @Test
    public void testMultipleSelection() {
        final PListBox listBox = attachedListBox();
        assertFalse(listBox.isMultipleSelect());
        listBox.setMultipleSelect(true);
        listBox.setMultipleSelect(true);
        assertTrue(listBox.isMultipleSelect());
        assertEquals(List.of(true), writer.valuesOf(ServerToClientModel.MULTISELECT));

        listBox.addItem("a", 1);
        listBox.addItem("b", 2);
        listBox.addItem("c", 3);
        listBox.setSelectedIndex(0);
        listBox.setSelectedItem("c", true);
        assertEquals(Arrays.asList(0, 2), listBox.getSelectedIndexes());
        assertEquals(Arrays.asList("a", "c"), listBox.getSelectedItems());
        assertEquals(Arrays.asList(1, 3), listBox.getSelectedValues());
        assertTrue(listBox.isItemSelected(2));

        listBox.setSelectedValue(1, false);
        assertEquals(List.of(2), listBox.getSelectedIndexes());
        assertEquals(false, last(ServerToClientModel.SELECTED));
    }

    @Test
    public void testVisibleItemCount() {
        final PListBox listBox = attachedListBox();
        listBox.setVisibleItemCount(5);
        listBox.setVisibleItemCount(5);
        assertEquals(5, listBox.getVisibleItemCount());
        assertEquals(List.of(5), writer.valuesOf(ServerToClientModel.VISIBLE_ITEM_COUNT));
    }

    @Test
    public void testChangeFromClient() {
        final PListBox listBox = attachedListBox();
        listBox.addItem("a");
        listBox.addItem("b");
        listBox.addItem("c");
        final List<PChangeEvent> events = new ArrayList<>();
        final PChangeHandler handler = events::add;
        listBox.addChangeHandler(handler);

        listBox.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_CHANGE.toStringValue(), "1,2").build());
        assertEquals(1, events.size());
        assertSame(listBox, events.get(0).getSource());
        assertEquals(1, listBox.getSelectedIndex());
        assertEquals(Arrays.asList(1, 2), listBox.getSelectedIndexes());

        assertTrue(listBox.removeChangeHandler(handler));
        assertFalse(listBox.removeChangeHandler(handler));
        listBox.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_CHANGE.toStringValue(), "0").build());
        assertEquals(1, events.size());
        assertEquals(0, listBox.getSelectedIndex());
    }

    @Test
    public void testChangeIgnoredWhenDisabledOrHidden() {
        final PListBox listBox = attachedListBox();
        listBox.addItem("a");
        listBox.addItem("b");
        listBox.setEnabled(false);
        listBox.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_CHANGE.toStringValue(), "1").build());
        assertEquals(-1, listBox.getSelectedIndex());

        listBox.setEnabled(true);
        listBox.setVisible(false);
        listBox.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_CHANGE.toStringValue(), "1").build());
        assertEquals(-1, listBox.getSelectedIndex());
    }

    @Test
    public void testOtherClientDataDelegatesToWidget() {
        final PListBox listBox = attachedListBox();
        listBox.onClientData(Json.createObjectBuilder().add(ClientToServerModel.HANDLER_WIDGET_VISIBILITY.toStringValue(), true).build());
        assertTrue(listBox.isShown());
    }

    @Test
    public void testIterator() {
        final PListBox listBox = Element.newPListBox();
        listBox.addItem("a");
        listBox.addItem("b");
        final List<String> labels = new ArrayList<>();
        listBox.forEach(item -> labels.add(item.getLabel()));
        assertEquals(Arrays.asList("a", "b"), labels);
    }

    @Test
    public void testDumpDOM() {
        final PListBox listBox = Element.newPListBox();
        listBox.addItem("a");
        listBox.addItem("b");
        listBox.setSelectedIndex(1);
        final String id = String.valueOf(listBox.getID());
        assertEquals("<select pid=\"" + id + "\"><option value=\"0\" >a</option><option value=\"1\" selected>b</option></select>",
            listBox.dumpDOM());
    }

    @Test
    public void testDumpDOMEmpty() {
        final PListBox listBox = Element.newPListBox();
        assertEquals("<select pid=\"" + listBox.getID() + "\"></select>", listBox.dumpDOM());
    }
}
