/*
 * Copyright (c) 2019 PonySDK Owners: Luciano Broussal <luciano.broussal AT
 * gmail.com> Mathieu Barbier <mathieu.barbier AT gmail.com> Nicolas Ciaravola
 * <nicolas.ciaravola.pro AT gmail.com>
 *
 * WebSite: http://code.google.com/p/pony-sdk/
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

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnActionListener;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
import com.ponysdk.core.ui.datagrid2.data.RowAction;

/**
 * @author mbagdouri
 */
public class DecoratorDataGridView<K, V> implements DataGridView<K, V> {

    private final DataGridView<K, V> view;

    protected DecoratorDataGridView(final DataGridView<K, V> view) {
        super();
        Objects.requireNonNull(view);
        this.view = view;
    }

    protected final DataGridView<K, V> getView() {
        return view;
    }

    @Override
    public PWidget asWidget() {
        return view.asWidget();
    }

    @Override
    public DataGridController<K, V> getController() {
        return view.getController();
    }

    @Override
    public void setAdapter(final DataGridAdapter<K, V> adapter) {
        view.setAdapter(adapter);
    }

    @Override
    public void setPollingDelayMillis(final long pollingDelayMillis) {
        view.setPollingDelayMillis(pollingDelayMillis);
    }

    @Override
    public Collection<V> getLiveSelectedData() {
        return view.getLiveSelectedData();
    }

    @Override
    public void unselectAllData() {
        view.unselectAllData();
    }

    @Override
    public void selectAllLiveData() {
        view.selectAllLiveData();
    }

    @Override
    public void setFilter(final Object key, final String id, final Predicate<V> filter, final boolean reinforcing) {
        view.setFilter(key, id, filter, reinforcing);
    }

    @Override
    public void clearFilter(final Object key) {
        view.clearFilter(key);
    }

    @Override
    public void clearFilters() {
        view.clearFilters();
    }

    @Override
    public void clearSorts() {
        view.clearSorts();
    }

    @Override
    public void addColumnActionListener(final ColumnDefinition<V> column, final ColumnActionListener<V> listener) {
        view.addColumnActionListener(column, listener);
    }

    @Override
    public void removeColumnActionListener(final ColumnDefinition<V> column, final ColumnActionListener<V> listener) {
        view.removeColumnActionListener(column, listener);
    }

    @Override
    public String encodeConfigCustomValue(final String key, final Object value) {
        return view.encodeConfigCustomValue(key, value);
    }

    @Override
    public Object decodeConfigCustomValue(final String key, final String value) throws DecodeException {
        return view.decodeConfigCustomValue(key, value);
    }

    @Override
    public DataGridConfig<V> getConfig() {
        return view.getConfig();
    }

    @Override
    public void setConfig(final DataGridConfig<V> config) {
        view.setConfig(config);
    }

    @Override
    public void addDrawListener(final DrawListener drawListener) {
        view.addDrawListener(drawListener);
    }

    @Override
    public void addSort(final Object key, final Comparator<V> comparator) {
        view.addSort(key, comparator);
    }

    @Override
    public void clearSort(final Object key) {
        view.clearSort(key);
    }

    @Override
    public void addRowAction(final Object key, final RowAction<V> rowHighlighter) {
        view.addRowAction(key, rowHighlighter);
    }

    @Override
    public void clearRowAction(final Object key) {
        view.clearRowAction(key);
    }

    @Override
    public void removeDrawListener(final DrawListener drawListener) {
        view.removeDrawListener(drawListener);
    }

    @Override
    public int getLiveDataRowCount() {
        return view.getLiveDataRowCount();
    }
}
