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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Column;
import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.ColumnControllerSort;
import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Interval;
import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Row;

public abstract class SimpleDataSource<K, V> implements DataGridSource<K, V> {

    protected static final Logger log = LoggerFactory.getLogger(SimpleDataSource.class);
    protected final Map<K, Row<V>> cache = new HashMap<>();
    protected final LinkedHashMap<Object, Comparator<Row<V>>> sorts = new LinkedHashMap<>();
    protected final Map<Object, AbstractFilter<V>> filters = new HashMap<>();
    protected final List<Row<V>> liveData = new ArrayList<>();
    protected final List<Row<V>> liveSelectedData = new ArrayList<>();
    protected final Set<K> selectedKeys = new HashSet<>();
    protected DataGridAdapter<K, V> adapter;
    protected int rowCounter = 0;

    //----------------------------------------------------------------------------------------------------------//
    //----------------------------------------- Cache getters --------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    @Override
    public Row<V> getRow(final K k) {
        return cache.get(k);
    }

    @Override
    public Collection<Row<V>> getRows() {
        return cache.values();
    }

    //----------------------------------------------------------------------------------------------------------//
    //------------------------------------------------ Adapter -------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    @Override
    public void setAdapter(final DataGridAdapter<K, V> adapter) {
        this.adapter = adapter;
    }

    //----------------------------------------------------------------------------------------------------------//
    //----------------------------------------- Gestion de liveData --------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    // Insert data in the cache or update it if it already exists
    @Override
    public Interval setData(final V v) {
        Objects.requireNonNull(v);
        final K k = adapter.getKey(v);
        final Row<V> row = cache.get(k);
        Interval interval;
        if (row != null) {
            if (row.data == v) return null;
            interval = updateData(k, row, v);
        } else {
            interval = insertData(k, v);
        }
        return interval;
    }

    // Rows are updated when they already exist
    private Interval updateData(final K k, final Row<V> row, final V newV) {
        if (row.accepted) {
            final int oldLiveDataSize = liveData.size();
            final int oldRowIndex = removeRow(liveData, row);
            final boolean selected = selectedKeys.contains(k);
            if (selected) removeRow(liveSelectedData, row);
            row.data = newV;
            return onWasAcceptedAndRemoved(selected, row, oldLiveDataSize, oldRowIndex);
        } else {
            row.data = newV;
            return onWasNotAccepted(k, row);
        }
    }

    @Override
    public Interval updateData(final K k, final Consumer<V> updater) {
        final Row<V> row = cache.get(k);
        if (row == null) return null;
        if (row.accepted) {
            final int oldLiveDataSize = liveData.size();
            final int oldRowIndex = removeRow(liveData, row);
            final boolean selected = selectedKeys.contains(k);
            if (selected) removeRow(liveSelectedData, row);
            updater.accept(row.data);
            return onWasAcceptedAndRemoved(selected, row, oldLiveDataSize, oldRowIndex);
        } else {
            updater.accept(row.data);
            return onWasNotAccepted(k, row);
        }
    }

    @Override
    public V removeData(final K k) {

        final Row<V> row = cache.remove(k);
        final boolean selected = selectedKeys.remove(k);
        if (row.accepted) {
            //            final int oldLiveDataSize = liveData.size();
            //            final int rowIndex = removeRow(liveData, row);
            if (selected) {
                removeRow(liveSelectedData, row);
            }
            //            refreshRows(rowIndex, oldLiveDataSize);
        }
        return row.data;
    }

    // Here rows are created and inserted in liveData
    private Interval insertData(final K k, final V data) {
        final Row<V> row = new Row<>(rowCounter++, data);
        row.accepted = accept(row);
        cache.put(k, row);
        if (!row.accepted) return null;
        final int rowIndex = insertRow(liveData, row);
        return new Interval(rowIndex, liveData.size());
    }

    private Interval onWasAcceptedAndRemoved(final boolean selected, final Row<V> row, final int oldLiveDataSize,
                                             final int oldRowIndex) {
        //        clearRenderingHelpers(row);
        if (accept(row)) {
            final int rowIndex = insertRow(liveData, row);
            if (selected) insertRow(liveSelectedData, row);
            if (oldRowIndex <= rowIndex) {
                return new Interval(oldRowIndex, rowIndex + 1);
            } else {
                return new Interval(rowIndex, oldRowIndex + 1);
            }
        } else {
            row.accepted = false;
            return new Interval(oldRowIndex, oldLiveDataSize);
        }
    }

    private Interval onWasNotAccepted(final K k, final Row<V> row) {
        //        clearRenderingHelpers(row);
        if (accept(row)) {
            row.accepted = true;
            final int rowIndex = insertRow(liveData, row);
            if (selectedKeys.contains(k)) insertRow(liveSelectedData, row);
            return new Interval(rowIndex, liveData.size());
        } //else do nothing
        return null;
    }

    private boolean accept(final Row<V> row) {
        for (final AbstractFilter<V> filter : filters.values()) {
            if (!filter.test(row)) return false;
        }
        return true;
    }

    @Override
    public void resetLiveData() {
        liveSelectedData.clear();
        liveData.clear();
        for (final Row<V> row : cache.values()) {
            row.accepted = accept(row);
            if (row.accepted) {
                insertRow(liveData, row);
                if (selectedKeys.contains(adapter.getKey(row.data))) {
                    insertRow(liveSelectedData, row);
                }
            }
        }
    }

    //    public void refreshRows(final int from, final int to) {
    //        this.from = Math.min(this.from, from);
    //        this.to = Math.max(this.to, to);
    //        if (SimpleDataGridController.bound) doRefreshRows(); //FIXME
    //    }
    //
    //    public void doRefreshRows() {
    //        try {
    //            if (listener != null) {
    //                listener.onUpdateRows(from, to);
    //            }
    //        } finally {
    //            from = Integer.MAX_VALUE;
    //            to = 0;
    //        }
    //    }

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

        for (final Comparator<Row<V>> sort : getSorts()) {
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

    @Override
    public Collection<Comparator<Row<V>>> getSorts() {
        return sorts.values();
    }

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

    //    @Override
    //    public AbstractFilter<V> putFilter(final Object key, final AbstractFilter<V> filter) {
    //        return filters.put(key, filter);
    //    }

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

    //    @Override
    //    public Collection<AbstractFilter<V>> getFilters() {
    //        return filters.values();
    //    }

    //----------------------------------------------------------------------------------------------------------//
    //----------------------------------------------- Selecting ------------------------------------------------//
    //----------------------------------------------------------------------------------------------------------//

    @Override
    public boolean isSelected(final K k) {
        return selectedKeys.contains(k);
    }

    @Override
    public void select(final K k) {
        final Row<V> row = cache.get(k);
        if (row == null || !selectedKeys.add(k) || !row.accepted) return;
        insertRow(liveSelectedData, row);
    }

    @Override
    public void unselect(final K k) {
        final Row<V> row = cache.get(k);
        if (row == null || !selectedKeys.remove(k) || !row.accepted) return;
        removeRow(liveSelectedData, row);
    }
}
