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
package com.ponysdk.core.ui.datagrid2.column;

import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.datagrid2.cell.LabelCell;
import com.ponysdk.core.ui.datagrid2.cell.PrimaryCell;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import com.ponysdk.test.PEmulator;
import com.ponysdk.test.PSuite;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DefaultColumnDefinitionTest extends PSuite {

    private DefaultColumnDefinition<Integer> column;
    private ColumnController<Integer> controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        column = new DefaultColumnDefinition<>("price", v -> v * 10, (v, s) -> {
        }, "numbers");
        controller = Mockito.mock(ColumnController.class);
    }

    @Test
    public void exposesStaticProperties() {
        Assertions.assertEquals("price", column.getId());
        Assertions.assertEquals("numbers", column.getGroup());
        Assertions.assertEquals(State.UNPINNED_SHOWN, column.getDefaultState());
        Assertions.assertTrue(column.isVisibilitySwitchable());
        Assertions.assertTrue(column.isPinnable());
        Assertions.assertTrue(column.isFilterable());
        Assertions.assertTrue(column.isSortable());
        Assertions.assertTrue(column.isResizable());
        Assertions.assertEquals(140, column.getDefaultWidth());
        Assertions.assertEquals(50, column.getMinWidth());
        Assertions.assertEquals(Integer.MAX_VALUE, column.getMaxWidth());
        Assertions.assertNull(column.getFooter());
        Assertions.assertEquals("SimpleColumnDefinition [columnName=price]", column.toString());
        Assertions.assertNull(new DefaultColumnDefinition<Integer>("x", v -> v, null).getGroup());
    }

    @Test
    public void headerShowsNameAndPinAndHideLabels() {
        final PComplexPanel header = column.getHeader();

        Assertions.assertEquals(3, header.getWidgetCount());
        Assertions.assertEquals("price", ((PLabel) header.getWidget(0)).getText());
        Assertions.assertEquals("Pin", ((PLabel) header.getWidget(1)).getText());
        Assertions.assertEquals("Hide", ((PLabel) header.getWidget(2)).getText());
        Assertions.assertSame(header.getWidget(0), column.getDraggableHeaderElement());
    }

    @Test
    public void renderingHelperIsValueFunctionAsString() {
        Assertions.assertEquals("30", column.getRenderingHelper(3));
    }

    @Test
    public void compareUsesRenderingHelpersAsStrings() {
        Assertions.assertTrue(column.compare(1, () -> "a", 2, () -> "b") < 0);
        Assertions.assertTrue(column.compare(1, () -> "b", 2, () -> "a") > 0);
        Assertions.assertEquals(0, column.compare(1, () -> "a", 2, () -> "a"));
    }

    @Test
    public void createCellReturnsLabelCell() {
        final PrimaryCell<Integer> cell = column.createCell();

        Assertions.assertTrue(cell instanceof LabelCell);
    }

    @Test
    public void clickOnNameTogglesSortDirection() {
        column.setController(controller);
        Assertions.assertSame(controller, column.getController());
        final PLabel name = (PLabel) column.getHeader().getWidget(0);

        PEmulator.click(name);
        Mockito.verify(controller).sort(true);

        column.onSort(true);
        PEmulator.click(name);
        Mockito.verify(controller).sort(false);

        column.onClearSort();
        PEmulator.click(name);
        Mockito.verify(controller, Mockito.times(2)).sort(true);
    }

    @Test
    public void clicksWithoutControllerAreIgnored() {
        PEmulator.click(column.getHeader().getWidget(0));
        PEmulator.click(column.getHeader().getWidget(1));
        PEmulator.click(column.getHeader().getWidget(2));

        Mockito.verifyNoInteractions(controller);
    }

    @Test
    public void clickOnPinLabelPinsThenUnpins() {
        column.setController(controller);
        final PLabel pin = (PLabel) column.getHeader().getWidget(1);

        PEmulator.click(pin);
        Mockito.verify(controller).setState(State.PINNED_SHOWN);

        column.onStateChanged(State.PINNED_SHOWN);
        Assertions.assertEquals("Unpin", pin.getText());
        PEmulator.click(pin);
        Mockito.verify(controller).setState(State.UNPINNED_SHOWN);

        column.onStateChanged(State.UNPINNED_SHOWN);
        Assertions.assertEquals("Pin", pin.getText());
    }

    @Test
    public void clickOnHideLabelHidesThenShows() {
        column.setController(controller);
        final PLabel hide = (PLabel) column.getHeader().getWidget(2);

        PEmulator.click(hide);
        Mockito.verify(controller).setState(State.UNPINNED_HIDDEN);

        column.onStateChanged(State.UNPINNED_HIDDEN);
        PEmulator.click(hide);
        Mockito.verify(controller).setState(State.UNPINNED_SHOWN);
    }

    @Test
    public void listenerCallbacksWithoutEffectDoNotFail() {
        column.onFilter("k", (v, h) -> true, true);
        column.onClearFilter("other");
        column.onClearFilter(column);
        column.onClearFilters();
        column.onRedraw(true);
        column.onResized(10);
        column.onMoved();

        Assertions.assertEquals("price", ((PLabel) column.getHeader().getWidget(0)).getText());
    }
}
