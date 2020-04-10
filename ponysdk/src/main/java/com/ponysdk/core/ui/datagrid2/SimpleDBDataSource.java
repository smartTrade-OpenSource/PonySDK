/*
 * Copyright (c) 2020 PonySDK
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

package com.ponysdk.core.ui.datagrid2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ponysdk.core.server.service.query.Query.QueryMode;
import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Interval;
import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Row;

public abstract class SimpleDBDataSource<K, V> extends SimpleDataSource<K, V> {

    private final int defaultPageSize = 33;
    private final Map<K, Row<V>> liveDataOnScreen = new HashMap<>();
    protected final Map<Object, String> filterIDs = new HashMap<>();
    protected int fullSize = 0;

    // Must be implemented by each child class for the corresponding DB service
    public abstract List<V> getData(final int row, final int size, final QueryMode queryMode);

    @Override
    public List<Row<V>> getRows(final int rowIndex, final int size) {

        final List<Row<V>> rows = new ArrayList<>();
        final List<V> tmpData = getData(rowIndex, size, QueryMode.LIMIT);
        if (liveDataOnScreen != null) liveDataOnScreen.clear();
        rowCounter = 0;
        for (final V order : tmpData) {
            final Row<V> row = createRow(order);
            final K k = adapter.getKey(order);
            rows.add(row);
            liveDataOnScreen.put(k, row);
        }
        return rows;
    }

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    @Override
    public int getRowCount() {
        return fullSize;
    }

    @Override
    public void setFilter(final Object key, final String id, final boolean reinforcing, final AbstractFilter<V> filter) {
        filters.put(key, filter);
        if (id != null) filterIDs.put(key, id);
        getRows(0, 0); //FIXME : performance
    }

    @Override
    public void clearFilters() {
        super.clearFilters();
        getRows(0, 0); //FIXME : performance
    }

    @Override
    public void select(final K k) {
        final Row<V> row = liveDataOnScreen.get(k);
        if (row == null || !selectedKeys.add(k) || !row.accepted) return;
        insertRow(liveSelectedData, row);
    }

    @Override
    public void unselect(final K k) {
        final Row<V> row = liveDataOnScreen.get(k);
        if (row == null || !selectedKeys.remove(k) || !row.accepted) return;
        removeRow(liveSelectedData, row);
    }

    @Override
    public void selectAllLiveData() {
        // FIXME : Implement me plz, i am tired of the void
    }

    private Row<V> createRow(final V v) {
        final Row<V> row = new Row<>(rowCounter++, v);
        row.accepted = true;
        return row;
    }

    @Override
    public Row<V> getRow(final K k) {
        return null;
    }

    @Override
    public Collection<Row<V>> getRows() {
        return null;
    }

    @Override
    public Interval setData(final V v) {
        return null;
    }

    @Override
    public Interval updateData(final K k, final Consumer<V> updater) {
        return null;
    }

    @Override
    public V removeData(final K k) {
        return null;
    }

    @Override
    public void resetLiveData() {
        return;
    }

    @Override
    public void forEach(final BiConsumer<K, V> action) {
        return;
    }
}
