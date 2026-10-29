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
package com.ponysdk.core.ui.datagrid2.controller;

import com.ponysdk.core.server.service.query.PResultSet;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.cell.Cell;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.data.DataGridFilter;
import com.ponysdk.core.ui.datagrid2.view.DataGridSnapshot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link DataGridControllerWrapper} and, through it, {@link SpyDataGridController}: every call reaches the
 * wrapped controller, and data changes notify the update handler only while the controller is bound.
 */
public class DataGridControllerWrapperTest {

    private DataGridController<String, String> wrapped;
    private DataGridControllerWrapper<String, String> wrapper;
    private int updates;
    private final List<String> removed = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        wrapped = Mockito.mock(DataGridController.class);
        updates = 0;
        removed.clear();
        wrapper = new DataGridControllerWrapper<>(wrapped, () -> updates++, removed::add);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void dataChangesNotifyWhenBound() {
        Mockito.when(wrapped.getBound()).thenReturn(true);
        Mockito.when(wrapped.removeData("a")).thenReturn("A");
        final Consumer<String> updater = s -> {
        };
        final Map<String, Consumer<String>> updaters = Map.of("a", updater);

        wrapper.setData("a");
        wrapper.setData(List.of("b"));
        wrapper.updateData("a", updater);
        wrapper.updateData(updaters);
        Assertions.assertEquals("A", wrapper.removeData("a"));

        Assertions.assertEquals(5, updates);
        Assertions.assertEquals(List.of("a"), removed);
        Mockito.verify(wrapped).setData("a");
        Mockito.verify(wrapped).setData(List.of("b"));
        Mockito.verify(wrapped).updateData("a", updater);
        Mockito.verify(wrapped).updateData(updaters);
        Mockito.verify(wrapped).removeData("a");
    }

    @Test
    public void dataChangesDoNotNotifyWhenUnbound() {
        Mockito.when(wrapped.getBound()).thenReturn(false);

        wrapper.setData("a");
        wrapper.setData(List.of("b"));
        wrapper.updateData("a", s -> {
        });
        wrapper.updateData(Map.of());
        wrapper.removeData("a");

        Assertions.assertEquals(0, updates);
        Assertions.assertEquals(List.of("a"), removed);
        Assertions.assertFalse(wrapper.getBound());
    }

    @Test
    public void rebindingNotifiesOnlyOnTransitionToBound() {
        Mockito.when(wrapped.getBound()).thenReturn(false, true);
        wrapper.setBound(true);
        Assertions.assertEquals(1, updates);

        Mockito.when(wrapped.getBound()).thenReturn(true, true);
        wrapper.setBound(true);
        Assertions.assertEquals(1, updates);

        Mockito.when(wrapped.getBound()).thenReturn(true, false);
        wrapper.setBound(false);
        Assertions.assertEquals(1, updates);
        Mockito.verify(wrapped, Mockito.times(2)).setBound(true);
        Mockito.verify(wrapped).setBound(false);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void queriesReturnWrappedValues() {
        final PResultSet<String> filtered = PResultSet.of(List.of("f"));
        final PResultSet<String> selected = PResultSet.of(List.of("s"));
        final PResultSet<String> last = PResultSet.of(List.of("l"));
        final Collection<String> live = List.of("x");
        Mockito.when(wrapped.getData("a")).thenReturn("A");
        Mockito.when(wrapped.isSelected("a")).thenReturn(true);
        Mockito.when(wrapped.isSelectable("a")).thenReturn(true);
        Mockito.when(wrapped.getFilteredData()).thenReturn(filtered);
        Mockito.when(wrapped.getLiveSelectedData()).thenReturn(selected);
        Mockito.when(wrapped.getLastRequestedData()).thenReturn(last);
        Mockito.when(wrapped.getLiveSelectedDataCount()).thenReturn(3);
        Mockito.when(wrapped.getLiveData(1, 2)).thenReturn(live);
        Mockito.when(wrapped.getRowCount()).thenReturn(7);
        Mockito.when(wrapped.get()).thenReturn(wrapped);

        Assertions.assertEquals("A", wrapper.getData("a"));
        Assertions.assertTrue(wrapper.isSelected("a"));
        Assertions.assertTrue(wrapper.isSelectable("a"));
        Assertions.assertSame(filtered, wrapper.getFilteredData());
        Assertions.assertSame(selected, wrapper.getLiveSelectedData());
        Assertions.assertSame(last, wrapper.getLastRequestedData());
        Assertions.assertEquals(3, wrapper.getLiveSelectedDataCount());
        Assertions.assertSame(live, wrapper.getLiveData(1, 2));
        Assertions.assertEquals(7, wrapper.getRowCount());
        Assertions.assertSame(wrapped, wrapper.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void commandsAreForwarded() {
        final ColumnDefinition<String> column = Mockito.mock(ColumnDefinition.class);
        final Cell<String, ?> cell = Mockito.mock(Cell.class);
        final Comparator<String> comparator = Comparator.naturalOrder();
        final BiPredicate<String, Supplier<Object>> predicate = (v, h) -> true;
        final DataGridFilter<String> filter = new DataGridFilter<>();
        final List<DataGridFilter<String>> filters = List.of(filter);
        final List<Object> keys = List.of("k");
        final DataGridConfig<String> config = new DataGridConfigBuilder<String>().build();
        final DataGridConfigBuilder<String> builder = new DataGridConfigBuilder<>();
        final DataGridAdapter<String, String> adapter = Mockito.mock(DataGridAdapter.class);
        final DataGridControllerListener<String> listener = Mockito.mock(DataGridControllerListener.class);
        final DataGridSnapshot snapshot = new DataGridSnapshot(0, 1, 0, new HashMap<>(), new HashSet<>());
        final Consumer<com.ponysdk.core.util.Pair<DefaultDataGridController<String, String>.DataSrcResult, Throwable>> consumer = p -> {
        };

        wrapper.renderCell(column, cell, "v");
        wrapper.select("a");
        wrapper.unselect("a");
        wrapper.selectKeys(List.of("a"));
        wrapper.unselectKeys(List.of("a"));
        wrapper.selectAllLiveData();
        wrapper.unselectAllData();
        wrapper.addSort(column, true);
        wrapper.addSort("k", comparator);
        wrapper.addPrimarySort("p", comparator);
        wrapper.clearSort("k");
        wrapper.clearSort(column);
        wrapper.clearSorts();
        wrapper.refreshWithSortingAndFilter();
        wrapper.setFilter("k", column, predicate, true, false);
        wrapper.setFilter(filter);
        wrapper.setFilters(filters);
        wrapper.clearFilter("k");
        wrapper.clearFilters(keys);
        wrapper.clearFilters(column);
        wrapper.clearFilters();
        wrapper.setConfig(config);
        wrapper.setAdapter(adapter);
        wrapper.setListener(listener);
        wrapper.clearRenderingHelpers(column);
        wrapper.clearRenderingHelper(column, "a");
        wrapper.enrichConfigBuilder(builder);
        wrapper.prepareLiveDataOnScreen(0, 1, snapshot, consumer);
        wrapper.refreshOnNextDraw();
        wrapper.refresh();
        wrapper.updateCurrentRows();

        Mockito.verify(wrapped).renderCell(column, cell, "v");
        Mockito.verify(wrapped).select("a");
        Mockito.verify(wrapped).unselect("a");
        Mockito.verify(wrapped).selectKeys(List.of("a"));
        Mockito.verify(wrapped).unselectKeys(List.of("a"));
        Mockito.verify(wrapped).selectAllLiveData();
        Mockito.verify(wrapped).unselectAllData();
        Mockito.verify(wrapped).addSort(column, true);
        Mockito.verify(wrapped).addSort("k", comparator);
        Mockito.verify(wrapped).addPrimarySort("p", comparator);
        Mockito.verify(wrapped).clearSort("k");
        Mockito.verify(wrapped).clearSort(column);
        Mockito.verify(wrapped).clearSorts();
        Mockito.verify(wrapped).refreshWithSortingAndFilter();
        Mockito.verify(wrapped).setFilter("k", column, predicate, true, false);
        Mockito.verify(wrapped).setFilter(filter);
        Mockito.verify(wrapped).setFilters(filters);
        Mockito.verify(wrapped).clearFilter("k");
        Mockito.verify(wrapped).clearFilters(keys);
        Mockito.verify(wrapped).clearFilters(column);
        Mockito.verify(wrapped).clearFilters();
        Mockito.verify(wrapped).setConfig(config);
        Mockito.verify(wrapped).setAdapter(adapter);
        Mockito.verify(wrapped).setListener(listener);
        Mockito.verify(wrapped).clearRenderingHelpers(column);
        Mockito.verify(wrapped).clearRenderingHelper(column, "a");
        Mockito.verify(wrapped).enrichConfigBuilder(builder);
        Mockito.verify(wrapped).prepareLiveDataOnScreen(0, 1, snapshot, consumer);
        Mockito.verify(wrapped).refreshOnNextDraw();
        Mockito.verify(wrapped).refresh();
        Mockito.verify(wrapped).updateCurrentRows();
        Assertions.assertEquals(0, updates);
    }

    @Test
    public void unbindingAnUnboundControllerDoesNotNotify() {
        Mockito.when(wrapped.getBound()).thenReturn(false, false);
        wrapper.setBound(false);
        Assertions.assertEquals(0, updates);
    }
}
