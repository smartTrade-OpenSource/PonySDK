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

import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Column;
import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.ColumnControllerSort;
import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.RenderingHelpersCache;
import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Row;

public abstract class SimpleDataSource<K, V> implements DataGridSource<K, V> {

    protected static final Logger log = LoggerFactory.getLogger(SimpleDataSource.class);
    protected final LinkedHashMap<Object, Comparator<Row<V>>> sorts = new LinkedHashMap<>();
    protected final Map<Object, AbstractFilter<V>> filters = new HashMap<>();
    protected final List<Row<V>> liveSelectedData = new ArrayList<>();
    protected final Set<K> selectedKeys = new HashSet<>();
    protected DataGridAdapter<K, V> adapter;
    protected RenderingHelpersCache<V> renderingHelpersCache; //FIXME : listener de dataSource
    protected int rowCounter = 0;

    //----------------------------------------------------------------------------------------------------------//
    //---------------------------------------- Adapter / RenderingCache ----------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    @Override
    public void setAdapter(final DataGridAdapter<K, V> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void sort() {
        liveSelectedData.sort(this::compare);
    }

    @Override
    public void setRenderingHelpersCache(final RenderingHelpersCache<V> renderingHelpersCache) {
        this.renderingHelpersCache = renderingHelpersCache;
    }

    //----------------------------------------------------------------------------------------------------------//
    //------------------------------- BinarySearch insertion, removal, findIndex -------------------------------//
    //----------------------------------------------------------------------------------------------------------//
    protected int insertRow(final List<Row<V>> rows, final Row<V> row) {
        if (rows.size() == 0) {
            rows.add(row);
            return 0;
        }
        if (compare(row, rows.get(0)) < 0) { //common case
            rows.add(0, row);
            return 0;
        }
        int left = 1;
        int right = rows.size() - 1;
        int index = left;
        int diff = 1;
        while (left <= right) {
            index = left + right >> 1;
            final Row<V> middleRow = rows.get(index);
            diff = compare(middleRow, row);
            if (diff < 0) left = index + 1;
            else if (diff > 0) right = index - 1;
            else throw new IllegalArgumentException(
                "Cannot insert an already existing row : existing=" + middleRow.data + ", new=" + row.data);
        }
        if (diff < 0) index++;
        rows.add(index, row);
        return index;
    }

    protected int removeRow(final List<Row<V>> rows, final Row<V> row) {
        final int rowIndex = findRowIndex(rows, row);
        if (rowIndex < 0) return rowIndex;
        rows.remove(rowIndex);
        return rowIndex;
    }

    private int findRowIndex(final List<Row<V>> rows, final Row<V> row) {
        int left = 0;
        int right = rows.size() - 1;
        while (left <= right) {
            final int middle = left + right >> 1;
            final Row<V> r = rows.get(middle);
            final int diff = compare(r, row);
            if (diff < 0) left = middle + 1;
            else if (diff > 0) right = middle - 1;
            else return middle;
        }
        return -1;
    }

    protected final int compare(final Row<V> r1, final Row<V> r2) {

        for (final Comparator<Row<V>> sort : sorts.values()) {
            final int diff = sort.compare(r1, r2);
            if (diff != 0) return diff;
        }
        final int diff = adapter.compareDefault(r1.data, r2.data);
        if (diff != 0) return diff;

        return adapter.isAscendingSortByInsertionOrder() ? r1.id - r2.id : r2.id - r1.id;
    }

    //----------------------------------------------------------------------------------------------------------//
    //------------------------------------------------ Sorting -------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    @Override
    public void addSort(final Column<V> column, final ColumnControllerSort colSort, final boolean asc) {
        final ColumnControllerSort tmpColSort = (ColumnControllerSort) sorts.get(column);
        if (tmpColSort != null && tmpColSort.asc == asc) return;
        sorts.put(column, colSort);
        sort();
    }

    @Override
    public void addSort(final Object key, final Comparator<Row<V>> comparator) {
        sorts.put(key, comparator);
        sort();
    }

    @Override
    public Comparator<Row<V>> clearSort(final Column<V> column) {
        return sorts.remove(column);
    }

    @Override
    public Comparator<Row<V>> clearSort(final Object key) {
        return sorts.remove(key);
    }

    @Override
    public void clearSorts() {
        sorts.clear();
        sort();
    }

    //    @Override
    //    public Collection<Comparator<Row<V>>> getSorts() {
    //        return sorts.values();
    //    }

    @Override
    public Set<Entry<Object, Comparator<Row<V>>>> getSortsEntry() {
        return sorts.entrySet();
    }

    //----------------------------------------------------------------------------------------------------------//
    //----------------------------------------------- Filtering ------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

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

    //----------------------------------------------------------------------------------------------------------//
    //----------------------------------------------- Selecting ------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

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
