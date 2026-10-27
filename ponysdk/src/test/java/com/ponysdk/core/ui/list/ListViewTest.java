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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import com.ponysdk.testutil.CapturingModelWriter;

public class ListViewTest extends PSuite {

    private CapturingModelWriter writer;

    @AfterEach
    public void tearDown() {
        if (writer != null) writer.uninstall();
    }

    @Test
    public void testSimpleListViewWidgets() {
        final DefaultSimpleListView view = new DefaultSimpleListView();
        final PLabel label = Element.newPLabel("a");
        view.addWidget(label, 1, 2, 1);
        assertSame(label, view.getWidget(2, 1));
        assertEquals(3, view.getRowCount());

        view.addWidget(Element.newPLabel("b"), 0, 3, 2);
        view.clear(3);
        assertNull(view.getWidget(3, 0));
        assertSame(label, view.getWidget(2, 1));

        view.clearList();
        assertEquals(0, view.getRowCount());

        assertThrows(UnsupportedOperationException.class, () -> view.setColumns(2));
        assertThrows(UnsupportedOperationException.class, () -> view.moveRow(0, 1));
        assertThrows(UnsupportedOperationException.class, () -> view.moveColumn(0, 1));
    }

    @Test
    public void testSimpleListViewStyles() {
        final DefaultSimpleListView view = new DefaultSimpleListView();
        view.addWidget(Element.newPLabel("a"), 0, 0, 1);
        view.addWidget(Element.newPLabel("b"), 0, 1, 1);
        PWindow.getMain().add(view);
        writer = CapturingModelWriter.install();

        view.addRowStyle(1, "row");
        view.removeRowStyle(1, "row");
        view.addHeaderStyle("header");
        assertEquals(List.of("row", "header"), writer.valuesOf(ServerToClientModel.ROW_FORMATTER_ADD_STYLE_NAME));
        assertEquals(List.of("row"), writer.valuesOf(ServerToClientModel.ROW_FORMATTER_REMOVE_STYLE_NAME));
        assertEquals(List.of(1, 1, 0), writer.valuesOf(ServerToClientModel.ROW));

        view.addCellStyle(1, 0, "cell");
        view.removeCellStyle(1, 0, "cell");
        view.addColumnStyle(0, "column");
        view.removeColumnStyle(0, "column");
        view.setColumnWidth(0, "10px");
        assertEquals(List.of("column"), writer.valuesOf(ServerToClientModel.COLUMN_FORMATTER_ADD_STYLE_NAME));
        assertEquals(List.of("column"), writer.valuesOf(ServerToClientModel.COLUMN_FORMATTER_REMOVE_STYLE_NAME));
        assertEquals(List.of("10px"), writer.valuesOf(ServerToClientModel.COLUMN_FORMATTER_COLUMN_WIDTH));
        view.removeFromParent();
    }

    @Test
    public void testComplexListViewDelegatesToSimpleList() {
        final DefaultComplexListView view = new DefaultComplexListView();
        // position panel, simple list, bottom layout and custom information
        assertEquals(4, view.getWidgetCount());
        final DefaultSimpleListView simpleList = (DefaultSimpleListView) view.getWidget(1);

        final PLabel header = Element.newPLabel("header");
        final PLabel cell = Element.newPLabel("cell");
        view.addWidget(header, 0, 0, 1);
        view.addWidget(cell, 0, 1, 1);
        assertSame(header, simpleList.getWidget(0, 0));
        assertSame(cell, simpleList.getWidget(1, 0));

        view.insertRow(1);
        assertSame(cell, simpleList.getWidget(2, 0));
        view.addWidget(Element.newPLabel("inserted"), 0, 1, 1);
        view.removeRow(1);
        assertSame(cell, simpleList.getWidget(1, 0));

        view.clearList();
        assertSame(header, simpleList.getWidget(0, 0));
        assertNull(simpleList.getWidget(1, 0));

        view.addWidget(cell, 0, 1, 1);
        view.clear(0);
        assertEquals(0, simpleList.getRowCount());

        assertThrows(UnsupportedOperationException.class, () -> view.setColumns(1));
        assertThrows(UnsupportedOperationException.class, () -> view.moveRow(0, 1));
        assertThrows(UnsupportedOperationException.class, () -> view.moveColumn(0, 1));
    }

    @Test
    public void testComplexListViewStylesAreForwarded() {
        final DefaultComplexListView view = new DefaultComplexListView();
        final DefaultSimpleListView simpleList = (DefaultSimpleListView) view.getWidget(1);
        view.addWidget(Element.newPLabel("header"), 0, 0, 1);
        view.addWidget(Element.newPLabel("cell"), 0, 1, 1);
        PWindow.getMain().add(view);
        writer = CapturingModelWriter.install();

        view.selectRow(0);
        view.unSelectRow(0);
        view.addRowStyle(1, "row");
        view.removeRowStyle(1, "row");
        view.addHeaderStyle("header");
        view.addCellStyle(1, 0, "cell");
        view.removeCellStyle(1, 0, "cell");
        view.addColumnStyle(0, "column");
        view.removeColumnStyle(0, "column");
        view.setColumnWidth(0, "5em");

        assertEquals(List.of("pony-SimpleList-Row-Selected", "row", "header"),
            writer.valuesOf(ServerToClientModel.ROW_FORMATTER_ADD_STYLE_NAME));
        assertEquals(List.of("pony-SimpleList-Row-Selected", "row"),
            writer.valuesOf(ServerToClientModel.ROW_FORMATTER_REMOVE_STYLE_NAME));
        assertEquals(List.of("column"), writer.valuesOf(ServerToClientModel.COLUMN_FORMATTER_ADD_STYLE_NAME));
        assertEquals(List.of("column"), writer.valuesOf(ServerToClientModel.COLUMN_FORMATTER_REMOVE_STYLE_NAME));
        assertEquals(List.of("5em"), writer.valuesOf(ServerToClientModel.COLUMN_FORMATTER_COLUMN_WIDTH));
        view.removeFromParent();
    }

    @Test
    public void testComplexListViewLayouts() {
        final DefaultComplexListView view = new DefaultComplexListView();
        assertNotNull(view.getFormLayout());
        assertNotNull(view.getTopListLayout());
        assertNotNull(view.getBottomListLayout());
        assertNotNull(view.getPagingLayout());
        assertNotNull(view.getPreferencesLayout());

        view.setSearchResultInformation("3 results");
        final PLabel searchResult = (PLabel) ((com.ponysdk.core.ui.basic.PSimplePanel) view.getBottomListLayout()).getWidget();
        assertEquals("3 results", searchResult.getText());

        view.addCustomInformation("info");
        final com.ponysdk.core.ui.basic.PVerticalPanel custom = (com.ponysdk.core.ui.basic.PVerticalPanel) view.getWidget(3);
        assertEquals("info", ((PLabel) custom.getWidget(0)).getText());

        final boolean[] clicked = { false };
        view.addAction("Action", event -> clicked[0] = true);
        assertEquals(1, view.getToolbarLayout().getWidgetCount());
        final PButton button = (PButton) view.getToolbarLayout().getWidget(0);
        assertEquals("Action", button.getText());
        PEmulator.click(button);
        assertTrue(clicked[0]);

        // No-op hooks
        view.setFloatableToolBar(null);
        view.updateView();
    }

    @Test
    public void testFieldDescriptor() {
        final FieldDescriptor descriptor = new FieldDescriptor("id", "key", String.class, "list", "criteria", "export");
        assertEquals("id", descriptor.getID());
        assertEquals("key", descriptor.getKey());
        assertEquals(String.class, descriptor.getType());
        assertEquals("list", descriptor.getListCaption());
        assertEquals("criteria", descriptor.getCriteriaKey());
        assertEquals("export", descriptor.getExportCaption());

        descriptor.setID("id2");
        descriptor.setKey("key2");
        descriptor.setType(Integer.class);
        descriptor.setListCaption("list2");
        descriptor.setCriteriaKey("criteria2");
        descriptor.setExportCaption("export2");
        assertEquals("id2", descriptor.getID());
        assertEquals("key2", descriptor.getKey());
        assertEquals(Integer.class, descriptor.getType());
        assertEquals("list2", descriptor.getListCaption());
        assertEquals("criteria2", descriptor.getCriteriaKey());
        assertEquals("export2", descriptor.getExportCaption());
    }
}
