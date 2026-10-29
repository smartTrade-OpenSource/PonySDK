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
package com.ponysdk.core.ui.datagrid2.view;

import com.ponysdk.core.server.service.query.PResultSet;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnActionListener;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
import com.ponysdk.core.ui.datagrid2.data.DataGridFilter;
import com.ponysdk.core.ui.datagrid2.data.RowAction;
import com.ponysdk.core.ui.datagrid2.view.DataGridView.DecodeException;
import com.ponysdk.core.ui.datagrid2.view.DataGridView.DrawListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class DecoratorDataGridViewTest {

    private DataGridView<String, String> wrapped;
    private DecoratorDataGridView<String, String> decorator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        wrapped = Mockito.mock(DataGridView.class);
        decorator = new DecoratorDataGridView<>(wrapped);
    }

    @Test
    public void rejectsNullView() {
        Assertions.assertThrows(NullPointerException.class, () -> new DecoratorDataGridView<String, String>(null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void queriesReturnWrappedValues() throws Exception {
        final PWidget widget = Mockito.mock(PWidget.class);
        final DataGridController<String, String> controller = Mockito.mock(DataGridController.class);
        final DataGridAdapter<String, String> adapter = Mockito.mock(DataGridAdapter.class);
        final PResultSet<String> filtered = PResultSet.of(List.of("f"));
        final PResultSet<String> selected = PResultSet.of(List.of("s"));
        final Collection<String> live = List.of("l");
        final DataGridConfig<String> config = new DataGridConfigBuilder<String>().build();
        Mockito.when(wrapped.asWidget()).thenReturn(widget);
        Mockito.when(wrapped.getController()).thenReturn(controller);
        Mockito.when(wrapped.getAdapter()).thenReturn(adapter);
        Mockito.when(wrapped.getFilteredData()).thenReturn(filtered);
        Mockito.when(wrapped.getLiveSelectedData()).thenReturn(selected);
        Mockito.when(wrapped.getLiveSelectedDataCount()).thenReturn(4);
        Mockito.when(wrapped.getLiveData(1, 2)).thenReturn(live);
        Mockito.when(wrapped.getLiveDataRowCount()).thenReturn(30);
        Mockito.when(wrapped.getConfig()).thenReturn(config);
        Mockito.when(wrapped.encodeConfigCustomValue("k", 1)).thenReturn("one");
        Mockito.when(wrapped.decodeConfigCustomValue("k", "one")).thenReturn(1);

        Assertions.assertSame(wrapped, decorator.getView());
        Assertions.assertSame(widget, decorator.asWidget());
        Assertions.assertSame(controller, decorator.getController());
        Assertions.assertSame(adapter, decorator.getAdapter());
        Assertions.assertSame(filtered, decorator.getFilteredData());
        Assertions.assertSame(selected, decorator.getLiveSelectedData());
        Assertions.assertEquals(4, decorator.getLiveSelectedDataCount());
        Assertions.assertSame(live, decorator.getLiveData(1, 2));
        Assertions.assertEquals(30, decorator.getLiveDataRowCount());
        Assertions.assertSame(config, decorator.getConfig());
        Assertions.assertEquals("one", decorator.encodeConfigCustomValue("k", 1));
        Assertions.assertEquals(1, decorator.decodeConfigCustomValue("k", "one"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void commandsAreForwarded() {
        final DataGridAdapter<String, String> adapter = Mockito.mock(DataGridAdapter.class);
        final DataGridFilter<String> filter = new DataGridFilter<>();
        final List<DataGridFilter<String>> filters = List.of(filter);
        final List<Object> keys = List.of("k");
        final ColumnDefinition<String> column = Mockito.mock(ColumnDefinition.class);
        final ColumnActionListener<String> listener = Mockito.mock(ColumnActionListener.class);
        final DataGridConfig<String> config = new DataGridConfigBuilder<String>().build();
        final DrawListener drawListener = count -> {
        };
        final Comparator<String> comparator = Comparator.naturalOrder();
        final RowAction<String> rowAction = Mockito.mock(RowAction.class);
        final Function<Throwable, String> handler = Throwable::getMessage;

        decorator.setAdapter(adapter);
        decorator.setPollingDelayMillis(10);
        decorator.unselectAllData();
        decorator.selectAllLiveData();
        decorator.setFilter(filter);
        decorator.setFilters(filters);
        decorator.clearFilter("k");
        decorator.clearFilters(keys);
        decorator.clearFilters();
        decorator.clearSorts(true);
        decorator.addColumnActionListener(column, listener);
        decorator.removeColumnActionListener(column, listener);
        decorator.setConfig(config);
        decorator.addDrawListener(drawListener);
        decorator.removeDrawListener(drawListener);
        decorator.addSort("k", comparator);
        decorator.clearSort("k");
        decorator.addRowAction("r", rowAction);
        decorator.clearRowAction("r");
        decorator.scrollTo(3);
        decorator.resume();
        decorator.pause();
        decorator.setExceptionHandler(handler);

        Mockito.verify(wrapped).setAdapter(adapter);
        Mockito.verify(wrapped).setPollingDelayMillis(10);
        Mockito.verify(wrapped).unselectAllData();
        Mockito.verify(wrapped).selectAllLiveData();
        Mockito.verify(wrapped).setFilter(filter);
        Mockito.verify(wrapped).setFilters(filters);
        Mockito.verify(wrapped).clearFilter("k");
        Mockito.verify(wrapped).clearFilters(keys);
        Mockito.verify(wrapped).clearFilters();
        Mockito.verify(wrapped).clearSorts(true);
        Mockito.verify(wrapped).addColumnActionListener(column, listener);
        Mockito.verify(wrapped).removeColumnActionListener(column, listener);
        Mockito.verify(wrapped).setConfig(config);
        Mockito.verify(wrapped).addDrawListener(drawListener);
        Mockito.verify(wrapped).removeDrawListener(drawListener);
        Mockito.verify(wrapped).addSort("k", comparator);
        Mockito.verify(wrapped).clearSort("k");
        Mockito.verify(wrapped).addRowAction("r", rowAction);
        Mockito.verify(wrapped).clearRowAction("r");
        Mockito.verify(wrapped).scrollTo(3);
        Mockito.verify(wrapped).resume();
        Mockito.verify(wrapped).pause();
        Mockito.verify(wrapped).setExceptionHandler(handler);
    }

    @Test
    public void decodeExceptionKeepsMessageAndCause() {
        final Throwable cause = new IllegalStateException("cause");

        Assertions.assertNull(new DecodeException().getMessage());
        Assertions.assertSame(cause, new DecodeException(cause).getCause());
        Assertions.assertEquals("m", new DecodeException("m").getMessage());
        final DecodeException withCause = new DecodeException("m", cause);
        Assertions.assertEquals("m", withCause.getMessage());
        Assertions.assertSame(cause, withCause.getCause());
        final DecodeException full = new DecodeException("m", cause, false, false);
        Assertions.assertSame(cause, full.getCause());
        Assertions.assertEquals(0, full.getStackTrace().length);
    }
}
