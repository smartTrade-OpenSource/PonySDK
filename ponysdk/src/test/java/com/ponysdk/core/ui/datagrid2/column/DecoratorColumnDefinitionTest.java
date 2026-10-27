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

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.datagrid2.cell.PrimaryCell;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class DecoratorColumnDefinitionTest {

    private ColumnDefinition<String> wrapped;
    private DecoratorColumnDefinition<String> decorator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        wrapped = Mockito.mock(ColumnDefinition.class);
        decorator = new DecoratorColumnDefinition<>(wrapped) {
        };
    }

    @Test
    @SuppressWarnings("unchecked")
    public void returnsValuesOfWrappedColumn() {
        final IsPWidget header = Mockito.mock(IsPWidget.class);
        final IsPWidget footer = Mockito.mock(IsPWidget.class);
        final IsPWidget draggable = Mockito.mock(IsPWidget.class);
        final PrimaryCell<String> cell = Mockito.mock(PrimaryCell.class);
        final ColumnController<String> controller = Mockito.mock(ColumnController.class);
        final Supplier<Object> h1 = () -> "x";
        final Supplier<Object> h2 = () -> "y";
        Mockito.when(wrapped.getHeader()).thenReturn(header);
        Mockito.when(wrapped.getFooter()).thenReturn(footer);
        Mockito.when(wrapped.getDraggableHeaderElement()).thenReturn(draggable);
        Mockito.when(wrapped.createCell()).thenReturn(cell);
        Mockito.when(wrapped.getRenderingHelper("v")).thenReturn("helper");
        Mockito.when(wrapped.compare("a", h1, "b", h2)).thenReturn(5);
        Mockito.when(wrapped.getDefaultState()).thenReturn(State.PINNED_SHOWN);
        Mockito.when(wrapped.isVisibilitySwitchable()).thenReturn(true);
        Mockito.when(wrapped.isPinnable()).thenReturn(true);
        Mockito.when(wrapped.isFilterable()).thenReturn(true);
        Mockito.when(wrapped.isSortable()).thenReturn(true);
        Mockito.when(wrapped.isResizable()).thenReturn(true);
        Mockito.when(wrapped.getId()).thenReturn("id");
        Mockito.when(wrapped.getDefaultWidth()).thenReturn(10);
        Mockito.when(wrapped.getMinWidth()).thenReturn(5);
        Mockito.when(wrapped.getMaxWidth()).thenReturn(50);
        Mockito.when(wrapped.getGroup()).thenReturn("group");
        Mockito.when(wrapped.getController()).thenReturn(controller);

        Assertions.assertSame(wrapped, decorator.getColumn());
        Assertions.assertSame(header, decorator.getHeader());
        Assertions.assertSame(footer, decorator.getFooter());
        Assertions.assertSame(draggable, decorator.getDraggableHeaderElement());
        Assertions.assertSame(cell, decorator.createCell());
        Assertions.assertEquals("helper", decorator.getRenderingHelper("v"));
        Assertions.assertEquals(5, decorator.compare("a", h1, "b", h2));
        Assertions.assertEquals(State.PINNED_SHOWN, decorator.getDefaultState());
        Assertions.assertTrue(decorator.isVisibilitySwitchable());
        Assertions.assertTrue(decorator.isPinnable());
        Assertions.assertTrue(decorator.isFilterable());
        Assertions.assertTrue(decorator.isSortable());
        Assertions.assertTrue(decorator.isResizable());
        Assertions.assertEquals("id", decorator.getId());
        Assertions.assertEquals(10, decorator.getDefaultWidth());
        Assertions.assertEquals(5, decorator.getMinWidth());
        Assertions.assertEquals(50, decorator.getMaxWidth());
        Assertions.assertEquals("group", decorator.getGroup());
        Assertions.assertSame(controller, decorator.getController());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void forwardsActionsToWrappedColumn() {
        final ColumnController<String> controller = Mockito.mock(ColumnController.class);
        final BiPredicate<String, Supplier<Object>> filter = (v, h) -> true;

        decorator.setController(controller);
        decorator.onSort(true);
        decorator.onClearSort();
        decorator.onFilter("k", filter, true);
        decorator.onClearFilter("k");
        decorator.onClearFilters();
        decorator.onRedraw(true);
        decorator.onStateChanged(State.UNPINNED_HIDDEN);
        decorator.onResized(33);
        decorator.onMoved();

        Mockito.verify(wrapped).setController(controller);
        Mockito.verify(wrapped).onSort(true);
        Mockito.verify(wrapped).onClearSort();
        Mockito.verify(wrapped).onFilter("k", filter, true);
        Mockito.verify(wrapped).onClearFilter("k");
        Mockito.verify(wrapped).onClearFilters();
        Mockito.verify(wrapped).onRedraw(true);
        Mockito.verify(wrapped).onStateChanged(State.UNPINNED_HIDDEN);
        Mockito.verify(wrapped).onResized(33);
        Mockito.verify(wrapped).onMoved();
    }
}
