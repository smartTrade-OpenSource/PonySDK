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
package com.ponysdk.core.ui.datagrid2.adapter;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class DecoratorDataGridAdapterTest {

    private DataGridAdapter<String, String> wrapped;
    private DecoratorDataGridAdapter<String, String> decorator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        wrapped = Mockito.mock(DataGridAdapter.class);
        decorator = new DecoratorDataGridAdapter<>(wrapped);
    }

    @Test
    public void rejectsNullAdapter() {
        Assertions.assertThrows(NullPointerException.class, () -> new DecoratorDataGridAdapter<String, String>(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void returnsValuesOfWrappedAdapter() {
        final List<ColumnDefinition<String>> columns = List.of(Mockito.mock(ColumnDefinition.class));
        final IsPWidget loading = Mockito.mock(IsPWidget.class);
        Mockito.when(wrapped.getColumnDefinitions()).thenReturn(columns);
        Mockito.when(wrapped.getKey("value")).thenReturn("key");
        Mockito.when(wrapped.isAscendingSortByInsertionOrder()).thenReturn(true);
        Mockito.when(wrapped.compareDefault("a", "b")).thenReturn(-3);
        Mockito.when(wrapped.hasHeader()).thenReturn(true);
        Mockito.when(wrapped.hasFooter()).thenReturn(true);
        Mockito.when(wrapped.isSelectionEnabled()).thenReturn(true);
        Mockito.when(wrapped.createLoadingDataWidget()).thenReturn(loading);

        Assertions.assertSame(wrapped, decorator.getAdapter());
        Assertions.assertSame(columns, decorator.getColumnDefinitions());
        Assertions.assertEquals("key", decorator.getKey("value"));
        Assertions.assertTrue(decorator.isAscendingSortByInsertionOrder());
        Assertions.assertEquals(-3, decorator.compareDefault("a", "b"));
        Assertions.assertTrue(decorator.hasHeader());
        Assertions.assertTrue(decorator.hasFooter());
        Assertions.assertTrue(decorator.isSelectionEnabled());
        Assertions.assertSame(loading, decorator.createLoadingDataWidget());
    }

    @Test
    public void forwardsRowCallbacksToWrappedAdapter() {
        final IsPWidget widget = Mockito.mock(IsPWidget.class);

        decorator.onCreateHeaderRow(widget);
        decorator.onCreateFooterRow(widget);
        decorator.onCreateRow(widget);
        decorator.onSelectRow(widget);
        decorator.onUnselectRow(widget);
        decorator.onCreateColumnResizer(widget);

        Mockito.verify(wrapped).onCreateHeaderRow(widget);
        Mockito.verify(wrapped).onCreateFooterRow(widget);
        Mockito.verify(wrapped).onCreateRow(widget);
        Mockito.verify(wrapped).onSelectRow(widget);
        Mockito.verify(wrapped).onUnselectRow(widget);
        Mockito.verify(wrapped).onCreateColumnResizer(widget);
    }
}
