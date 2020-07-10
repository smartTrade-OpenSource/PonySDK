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

package com.ponysdk.core.ui.datagrid2.datasource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.Column;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.controller.DefaultDataGridController;
import com.ponysdk.core.ui.datagrid2.controller.DefaultDataGridController.RenderingHelpersCache;
import com.ponysdk.core.ui.datagrid2.data.AbstractFilter;
import com.ponysdk.core.ui.datagrid2.data.DefaultRow;

/**
 * @author mabbas
 */
public abstract class AbstractDataSource<K, V> implements DataGridSource<K, V> {

    protected static final Logger log = LoggerFactory.getLogger(AbstractDataSource.class);
    protected final LinkedHashMap<Object, Comparator<DefaultRow<V>>> sorts = new LinkedHashMap<>();
    protected final Map<Object, AbstractFilter<V>> filters = new HashMap<>();
    protected final List<DefaultRow<V>> liveSelectedData = new ArrayList<>();
    protected final Set<K> selectedKeys = new HashSet<>();
    protected DataGridAdapter<K, V> adapter;
    protected RenderingHelpersCache<V> renderingHelpersCache;
    protected int rowCounter = 0;

    @Override
    public List<DefaultRow<V>> getLiveSelectedData() {
        return liveSelectedData;
    }

    @Override
    public void setAdapter(final DataGridAdapter<K, V> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void setRenderingHelpersCache(final RenderingHelpersCache<V> renderingHelpersCache) {
        this.renderingHelpersCache = renderingHelpersCache;
    }

    protected int insertRow(final List<DefaultRow<V>> rows, final DefaultRow<V> row) {
        if (rows.size() == 0) {
            rows.add(row);
            return 0;
        }
        if (compare(row, rows.get(0)) < 0) { // common case
            rows.add(0, row);
            return 0;
        }
        int left = 1;
        int right = rows.size() - 1;
        int index = left;
        int diff = 1;
        while (left <= right) {
            index = left + right >> 1;
            final DefaultRow<V> middleRow = rows.get(index);
            diff = compare(middleRow, row);
            if (diff < 0) left = index + 1;
            else if (diff > 0) right = index - 1;
            else throw new IllegalArgumentException(
                "Cannot insert an already existing row : existing=" + middleRow.getData() + ", new=" + row.getData());
        }
        if (diff < 0) index++;
        rows.add(index, row);
        return index;
    }

    protected int removeRow(final List<DefaultRow<V>> rows, final DefaultRow<V> row) {
        final int rowIndex = findRowIndex(rows, row);
        if (rowIndex < 0) return rowIndex;
        rows.remove(rowIndex);
        return rowIndex;
    }

    private int findRowIndex(final List<DefaultRow<V>> rows, final DefaultRow<V> row) {
        int left = 0;
        int right = rows.size() - 1;
        while (left <= right) {
            final int middle = left + right >> 1;
            final DefaultRow<V> r = rows.get(middle);
            final int diff = compare(r, row);
            if (diff < 0) left = middle + 1;
            else if (diff > 0) right = middle - 1;
            else return middle;
        }
        return -1;
    }

    protected final int compare(final DefaultRow<V> r1, final DefaultRow<V> r2) {

        for (final Comparator<DefaultRow<V>> sort : sorts.values()) {
            final int diff = sort.compare(r1, r2);
            if (diff != 0) return diff;
        }
        final int diff = adapter.compareDefault(r1.getData(), r2.getData());
        if (diff != 0) return diff;

        return adapter.isAscendingSortByInsertionOrder() ? r1.getID() - r2.getID() : r2.getID() - r1.getID();
    }

    @Override
    public void addSort(final Column<V> column, final DefaultDataGridController<K, V>.ColumnControllerSort colSort,
                        final boolean asc) {
        final DefaultDataGridController<K, V>.ColumnControllerSort tmpColSort = (DefaultDataGridController<K, V>.ColumnControllerSort) sorts
            .get(column);
        if (tmpColSort != null && tmpColSort.asc == asc) return;
        sorts.put(column, colSort);
        sort();
    }

    @Override
    public void addSort(final Object key, final Comparator<DefaultRow<V>> comparator) {
        sorts.put(key, comparator);
        sort();
    }

    @Override
    public Comparator<DefaultRow<V>> clearSort(final Column<V> column) {
        return sorts.remove(column);
    }

    @Override
    public Comparator<DefaultRow<V>> clearSort(final Object key) {
        return sorts.remove(key);
    }

    @Override
    public void clearSorts() {
        sorts.clear();
        sort();
    }

    @Override
    public void sort() {
        liveSelectedData.sort(this::compare);
    }

    @Override
    public Set<Entry<Object, Comparator<DefaultRow<V>>>> getSortsEntry() {
        return sorts.entrySet();
    }

    @Override
    public AbstractFilter<V> clearFilter(final Object key) {
        return filters.remove(key);
    }

    @Override
    public void clearFilters(final ColumnDefinition<V> column) {
        final Iterator<AbstractFilter<V>> iterator = filters.values().iterator();
        while (iterator.hasNext()) {
            final AbstractFilter<V> filter = iterator.next();
            final ColumnDefinition<V> filterColumn = filter.getColumnDefinition();
            if (filterColumn != null && filterColumn == column) iterator.remove();
        }
    }

    @Override
    public void clearFilters() {
        filters.clear();
    }

    @Override
    public boolean isSelected(final K k) {
        return selectedKeys.contains(k);
    }

    @Override
    public void unselectAllData() {
        liveSelectedData.clear();
        selectedKeys.clear();
    }
}
