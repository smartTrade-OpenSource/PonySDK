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

package com.ponysdk.core.ui.datagrid2.adapter;

import java.util.List;
import java.util.Objects;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;

/**
 * @author mbagdouri
 */
public class DecoratorDataGridAdapter<K, V> implements DataGridAdapter<K, V> {

    private final DataGridAdapter<K, V> adapter;

    protected DecoratorDataGridAdapter(final DataGridAdapter<K, V> adapter) {
        super();
        Objects.requireNonNull(adapter);
        this.adapter = adapter;
    }

    @Override
    public List<ColumnDefinition<V>> getColumnDefinitions() {
        return adapter.getColumnDefinitions();
    }

    @Override
    public K getKey(final V v) {
        return adapter.getKey(v);
    }

    @Override
    public boolean isAscendingSortByInsertionOrder() {
        return adapter.isAscendingSortByInsertionOrder();
    }

    @Override
    public int compareDefault(final V v1, final V v2) {
        return adapter.compareDefault(v1, v2);
    }

    @Override
    public void onCreateHeaderRow(final IsPWidget rowWidget) {
        adapter.onCreateHeaderRow(rowWidget);
    }

    @Override
    public void onCreateFooterRow(final IsPWidget rowWidget) {
        adapter.onCreateFooterRow(rowWidget);
    }

    @Override
    public void onCreateRow(final IsPWidget rowWidget) {
        adapter.onCreateRow(rowWidget);
    }

    @Override
    public void onSelectRow(final IsPWidget rowWidget) {
        adapter.onSelectRow(rowWidget);
    }

    @Override
    public void onUnselectRow(final IsPWidget rowWidget) {
        adapter.onUnselectRow(rowWidget);
    }

    @Override
    public boolean hasHeader() {
        return adapter.hasHeader();
    }

    @Override
    public boolean hasFooter() {
        return adapter.hasFooter();
    }

    protected DataGridAdapter<K, V> getAdapter() {
        return adapter;
    }

    @Override
    public IsPWidget createLoadingDataWidget() {
        return adapter.createLoadingDataWidget();
    }

    @Override
    public void onCreateColumnResizer(final IsPWidget resizer) {
        adapter.onCreateColumnResizer(resizer);
    }
}