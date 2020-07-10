/*
 * Copyright (c) 2019 PonySDK
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

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.cell.Cell;
import com.ponysdk.core.ui.datagrid2.cell.ExtendedCell;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.data.ViewLiveData;
import com.ponysdk.core.ui.datagrid2.view.DataGridSnapshot;

/**
 * @author mbagdouri
 */
public abstract class SpyDataGridController<K, V> implements DataGridController<K, V> {

    private final DataGridController<K, V> controller;

    public SpyDataGridController(final DataGridController<K, V> controller) {
        super();
        this.controller = controller;
    }

    @Override
    public void setData(final V v) {
        controller.setData(v);
        if (controller.getBound()) {
            onDataUpdate();
        }
    }

    @Override
    public void setData(final Collection<V> c) {
        controller.setData(c);
        if (controller.getBound()) onDataUpdate();
    }

    @Override
    public void updateData(final K k, final Consumer<V> updater) {
        controller.updateData(k, updater);
        if (controller.getBound()) onDataUpdate();
    }

    @Override
    public void updateData(final Map<K, Consumer<V>> updaters) {
        controller.updateData(updaters);
        if (controller.getBound()) onDataUpdate();
    }

    @Override
    public V getData(final K k) {
        return controller.getData(k);
    }

    @Override
    public V removeData(final K k) {
        final V v = controller.removeData(k);
        if (controller.getBound()) onDataUpdate();
        return v;
    }

    @Override
    public void setBound(final boolean bound) {
        final boolean wasBound = controller.getBound();
        controller.setBound(bound);
        if (!wasBound && controller.getBound()) onDataUpdate();
    }

    @Override
    public boolean getBound() {
        return controller.getBound();
    }

    protected abstract void onDataUpdate();

    @Override
    public void renderCell(final ColumnDefinition<V> column, final int row, final Cell<V> widget, final ViewLiveData<V> result) {
        controller.renderCell(column, row, widget, result);
    }

    @Override
    public void setValueOnExtendedCell(final int row, final ExtendedCell<V> widget, final ViewLiveData<V> result) {
        setValueOnExtendedCell(row, widget, result);
    }

    @Override
    public boolean isSelected(final K k) {
        return controller.isSelected(k);
    }

    @Override
    public Collection<V> getLiveSelectedData() {
        return controller.getLiveSelectedData();
    }

    @Override
    public void select(final K k) {
        controller.select(k);
    }

    @Override
    public void unselect(final K k) {
        controller.unselect(k);
    }

    @Override
    public void selectAllLiveData() {
        controller.selectAllLiveData();
    }

    @Override
    public void unselectAllData() {
        controller.unselectAllData();
    }

    @Override
    public void addSort(final ColumnDefinition<V> column, final boolean asc) {
        controller.addSort(column, asc);
    }

    @Override
    public void addSort(final Object key, final Comparator<V> comparator) {
        controller.addSort(key, comparator);
    }

    @Override
    public void clearSort(final Object key) {
        controller.clearSort(key);
    }

    @Override
    public void clearSort(final ColumnDefinition<V> column) {
        controller.clearSort(column);
    }

    @Override
    public void clearSorts() {
        controller.clearSorts();
    }

    @Override
    public void setFilter(final Object key, final ColumnDefinition<V> column, final BiPredicate<V, Supplier<Object>> filter,
                          final boolean reinforcing) {
        controller.setFilter(key, column, filter, reinforcing);
    }

    @Override
    public void setFilter(final Object key, final String id, final Predicate<V> filter, final boolean reinforcing) {
        controller.setFilter(key, id, filter, reinforcing);
    }

    @Override
    public void clearFilter(final Object key) {
        controller.clearFilter(key);
    }

    @Override
    public void clearFilters(final ColumnDefinition<V> column) {
        controller.clearFilters(column);
    }

    @Override
    public void clearFilters() {
        controller.clearFilters();
    }

    @Override
    public void setConfig(final DataGridConfig<V> config) {
        controller.setConfig(config);
    }

    @Override
    public void setAdapter(final DataGridAdapter<K, V> adapter) {
        controller.setAdapter(adapter);
    }

    @Override
    public DataGridController<K, V> get() {
        return controller.get();
    }

    @Override
    public void setListener(final DataGridControllerListener<V> listener) {
        controller.setListener(listener);
    }

    @Override
    public void clearRenderingHelpers(final ColumnDefinition<V> column) {
        controller.clearRenderingHelpers(column);
    }

    @Override
    public int getRowCount() {
        return controller.getRowCount();
    }

    @Override
    public void enrichConfigBuilder(final DataGridConfigBuilder<V> builder) {
        controller.enrichConfigBuilder(builder);
    }

    @Override
    public void prepareLiveDataOnScreen(final int dataSrcRowIndex, final int dataSize, final DataGridSnapshot threadSnapshot,
                                        final Consumer<DefaultDataGridController<K, V>.DataSrcResult> consumer) {
        controller.prepareLiveDataOnScreen(dataSrcRowIndex, dataSize, threadSnapshot, consumer);
    }
}
