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
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.ponysdk.core.ui.datagrid2.DataGridConfig.ColumnSort;
import com.ponysdk.core.ui.datagrid2.DataGridConfig.GeneralSort;
import com.ponysdk.core.ui.datagrid2.DataGridConfig.Sort;
import com.ponysdk.core.util.MappedList;

/**
 * @author mbagdouri
 */
class SimpleDataGridController<K, V> implements DataGridController<K, V>, DataGridModel<K, V> {

    private static final Object NO_RENDERING_HELPER = new Object();
    private static final int RENDERING_HELPERS_CACHE_CAPACITY = 512;
    private int rowCounter = 0;
    private int columnCounter = 0;
    private final RenderingHelpersCache<V> renderingHelpersCache = new RenderingHelpersCache<>();
    private final Map<K, Row<V>> cache = new HashMap<>();
    private final List<Row<V>> liveData = new ArrayList<>();
    private final List<Row<V>> liveSelectedData = new ArrayList<>();
    private final Set<K> selectedKeys = new HashSet<>();
    private final Map<Object, AbstractFilter<V>> filters = new HashMap<>();
    private final LinkedHashMap<Object, Comparator<Row<V>>> sorts = new LinkedHashMap<>();
    private final Map<ColumnDefinition<V>, Column<V>> columns = new HashMap<>();
    private DataGridControllerListener<V> listener;
    private DataGridAdapter<K, V> adapter;
    private boolean bound = true;
    private int from = Integer.MAX_VALUE;
    private int to = 0;
    private final RenderingHelperSupplier renderingHelperSupplier1 = new RenderingHelperSupplier();
    private final RenderingHelperSupplier renderingHelperSupplier2 = new RenderingHelperSupplier();

    SimpleDataGridController() {
        super();
    }

    @Override
    public void setAdapter(final DataGridAdapter<K, V> adapter) {
        if (this.adapter != null) throw new IllegalStateException("DataGridAdapter is already set");
        this.adapter = adapter;
        for (final ColumnDefinition<V> column : adapter.getColumnDefinitions()) {
            columns.put(column, new Column<>(columnCounter++, column));
        }
    }

    private void checkAdapter() {
        if (adapter == null) throw new IllegalStateException("A DataGridAdapter must be set");
    }

    private Column<V> getColumn(final ColumnDefinition<V> colDef) {
        final Column<V> column = columns.get(colDef);
        if (column == null) throw new IllegalArgumentException("Unknown ColumnDefinition " + colDef);
        return column;
    }

    private Column<V> getColumn(final String columnId) {
        for (final Column<V> column : columns.values()) {
            if (columnId.equals(column.def.getId())) return column;
        }
        return null;
    }

    Object getRenderingHelper(final Row<V> row, final Column<V> column) {
        final Object[] renderingHelpers = renderingHelpersCache.computeIfAbsent(row, r -> new Object[columns.size()]);
        Object helper = renderingHelpers[column.id];
        if (helper == NO_RENDERING_HELPER) return null;
        if (helper == null) {
            helper = column.def.getRenderingHelper(row.data);
            renderingHelpers[column.id] = helper == null ? NO_RENDERING_HELPER : helper;
        }
        return helper;
    }

    void clearRenderingHelpers(final Row<V> row) {
        renderingHelpersCache.remove(row);
    }

    void clearRenderingHelper(final Row<V> row, final Column<V> column) {
        final Object[] renderingHelpers = renderingHelpersCache.get(row);
        if (renderingHelpers == null) return;
        renderingHelpers[column.id] = null;
    }

    @Override
    public void clearFilters(final ColumnDefinition<V> column) {
        checkAdapter();
        Objects.requireNonNull(column);
        final Iterator<AbstractFilter<V>> iterator = filters.values().iterator();
        while (iterator.hasNext()) {
            final AbstractFilter<V> filter = iterator.next();
            final ColumnDefinition<V> filterColumn = filter.getColumnDefinition();
            if (filterColumn != null && filterColumn == column) iterator.remove();
        }
        resetLiveData();
    }

    @Override
    public final void clearFilters() {
        checkAdapter();
        filters.clear();
        resetLiveData();
    }

    private void refreshRows(final int from, final int to) {
        this.from = Math.min(this.from, from);
        this.to = Math.max(this.to, to);
        if (bound) doRefreshRows();
    }

    private void doRefreshRows() {
        try {
            if (listener != null) listener.onUpdateRows(from, to);
        } finally {
            from = Integer.MAX_VALUE;
            to = 0;
        }
    }

    @Override
    public final void setData(final V v) {
        final Interval interval = setData0(v);
        if (interval != null) refreshRows(interval.from, interval.to);
    }

    @Override
    public void setData(final Collection<V> c) {
        int from = Integer.MAX_VALUE;
        int to = 0;
        for (final V v : c) {
            final Interval interval = setData0(v);
            if (interval == null) continue;
            from = Math.min(from, interval.from);
            to = Math.max(to, interval.to);
        }
        if (from < to) refreshRows(from, to);
    }

    private Interval setData0(final V v) {
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

    private Interval insertData(final K k, final V data) {
        final Row<V> row = new Row<>(rowCounter++, data);
        row.accepted = accept(row);
        cache.put(k, row);
        if (!row.accepted) return null;
        final int rowIndex = insertRow(liveData, row);
        return new Interval(rowIndex, liveData.size());
    }

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

    private Interval onWasAcceptedAndRemoved(final boolean selected, final Row<V> row, final int oldLiveDataSize,
                                             final int oldRowIndex) {
        clearRenderingHelpers(row);
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
        clearRenderingHelpers(row);
        if (accept(row)) {
            row.accepted = true;
            final int rowIndex = insertRow(liveData, row);
            if (selectedKeys.contains(k)) insertRow(liveSelectedData, row);
            return new Interval(rowIndex, liveData.size());
        } //else do nothing
        return null;
    }

    @Override
    public void updateData(final K k, final Consumer<V> updater) {
        final Interval interval = updateData0(k, updater);
        if (interval != null) refreshRows(interval.from, interval.to);
    }

    private Interval updateData0(final K k, final Consumer<V> updater) {
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
    public void updateData(final Map<K, Consumer<V>> updaters) {
        int from = Integer.MAX_VALUE;
        int to = 0;
        for (final Map.Entry<K, Consumer<V>> entry : updaters.entrySet()) {
            final Interval interval = updateData0(entry.getKey(), entry.getValue());
            if (interval == null) continue;
            from = Math.min(from, interval.from);
            to = Math.max(to, interval.to);
        }
        if (from < to) refreshRows(from, to);
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

    private int insertRow(final List<Row<V>> rows, final Row<V> row) {
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

    private int removeRow(final List<Row<V>> rows, final Row<V> row) {
        final int rowIndex = findRowIndex(rows, row);
        if (rowIndex < 0) return rowIndex;
        rows.remove(rowIndex);
        return rowIndex;
    }

    private void resetLiveData() {
        final int oldLiveDataSize = liveData.size();
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
        refreshRows(0, Math.max(oldLiveDataSize, liveData.size()));
    }

    private boolean accept(final Row<V> row) {
        for (final AbstractFilter<V> filter : filters.values()) {
            if (!filter.test(row)) return false;
        }
        return true;
    }

    @Override
    public final V removeData(final K k) {
        final Row<V> row = cache.remove(k);
        if (row == null) return null;
        renderingHelpersCache.remove(row);
        final boolean selected = selectedKeys.remove(k);
        if (row.accepted) {
            final int oldLiveDataSize = liveData.size();
            final int rowIndex = removeRow(liveData, row);
            if (selected) {
                removeRow(liveSelectedData, row);
            }
            refreshRows(rowIndex, oldLiveDataSize);
        }
        return row.data;
    }

    @Override
    public final V getData(final K k) {
        final Row<V> row = cache.get(k);
        if (row == null) return null;
        return row.data;
    }

    private final int compare(final Row<V> r1, final Row<V> r2) {
        for (final Comparator<Row<V>> sort : sorts.values()) {
            final int diff = sort.compare(r1, r2);
            if (diff != 0) return diff;
        }
        final int diff = adapter.compareDefault(r1.data, r2.data);
        if (diff != 0) return diff;
        return adapter.isAscendingSortByInsertionOrder() ? r1.id - r2.id : r2.id - r1.id;
    }

    @Override
    public void renderCell(final ColumnDefinition<V> colDef, final int row, final Cell<V> cell) {
        checkAdapter();
        final Column<V> column = getColumn(colDef);
        final Row<V> r = liveData.get(row);
        cell.render(r.data, getRenderingHelper(r, column));
    }

    @Override
    public void setValueOnExtendedCell(final int row, final ExtendedCell<V> extendedCell) {
        checkAdapter();
        final Row<V> r = liveData.get(row);
        extendedCell.setValue(r.data);
    }

    private void sort() {
        liveSelectedData.sort(this::compare);
        liveData.sort(this::compare);
        refreshRows(0, liveData.size());
    }

    @Override
    public void addSort(final ColumnDefinition<V> colDef, final boolean asc) {
        checkAdapter();
        final Column<V> column = getColumn(colDef);
        final ColumnControllerSort colSort = (ColumnControllerSort) sorts.get(column);
        if (colSort != null && colSort.asc == asc) return;
        sorts.put(column, new ColumnControllerSort(column, asc));
        sort();
    }

    @Override
    public void clearSort(final ColumnDefinition<V> colDef) {
        checkAdapter();
        final Column<V> column = getColumn(colDef);
        if (sorts.remove(column) == null) return;
        sort();
    }

    @Override
    public void clearSorts() {
        checkAdapter();
        sorts.clear();
        sort();
    }

    @Override
    public void setFilter(final Object key, final ColumnDefinition<V> colDef, final BiPredicate<V, Supplier<Object>> biPredicate,
                          final boolean reinforcing) {
        checkAdapter();
        setFilter(key, reinforcing, new ColumnFilter(colDef, biPredicate));
    }

    @Override
    public void setFilter(final Object key, final Predicate<V> predicate, final boolean reinforcing) {
        checkAdapter();
        setFilter(key, reinforcing, new GeneralFilter(predicate));
    }

    private void setFilter(final Object key, final boolean reinforcing, final AbstractFilter<V> filter) {
        final AbstractFilter<V> oldFilter = filters.put(key, filter);
        if (oldFilter == null || reinforcing) {
            final int oldLiveDataSize = liveData.size();
            final int from = reinforceFilter(liveData, filter);
            reinforceFilter(liveSelectedData, filter);
            if (from >= 0) refreshRows(from, oldLiveDataSize);
        } else {
            resetLiveData();
        }
    }

    private int reinforceFilter(final List<Row<V>> rows, final AbstractFilter<V> filter) {
        final Iterator<Row<V>> iterator = rows.iterator();
        int from = -1;
        for (int i = 0; iterator.hasNext(); i++) {
            final Row<V> row = iterator.next();
            if (!filter.test(row)) {
                row.accepted = false;
                iterator.remove();
                if (from < 0) from = i;
            }
        }
        return from;
    }

    @Override
    public void clearFilter(final Object key) {
        checkAdapter();
        final AbstractFilter<V> oldFilter = filters.remove(key);
        if (oldFilter == null) return;
        resetLiveData();
    }

    @Override
    public void addSort(final Object key, final Comparator<V> comparator) {
        checkAdapter();
        sorts.put(key, new GeneralControllerSort(comparator));
        sort();
    }

    @Override
    public void clearSort(final Object key) {
        checkAdapter();
        if (sorts.remove(key) == null) return;
        sort();
    }

    @Override
    public V getRowData(final int row) {
        checkAdapter();
        return row < liveData.size() ? liveData.get(row).data : null;
    }

    @Override
    public DataGridModel<K, V> getModel() {
        return this;
    }

    @Override
    public void setListener(final DataGridControllerListener<V> listener) {
        this.listener = listener;
    }

    @Override
    public void clearRenderingHelpers(final ColumnDefinition<V> colDef) {
        checkAdapter();
        final Column<V> column = getColumn(colDef);
        for (final Row<V> row : cache.values()) {
            clearRenderingHelper(row, column);
        }
    }

    @Override
    public void forEach(final BiConsumer<K, V> action) {
        cache.forEach((k, r) -> action.accept(k, r.data));
    }

    @Override
    public int getRowCount() {
        return liveData.size();
    }

    @Override
    public String toString() {
        return "DefaultDataGridController [cache=" + cache + ", liveData=" + liveData + "]";
    }

    @Override
    public Collection<V> getLiveData() {
        return new MappedList<>(liveData, Row::getData);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public void setConfig(final DataGridConfig<V> config) {
        checkAdapter();
        sorts.clear();
        filters.clear();
        for (final Sort<V> s : config.getSorts()) {
            if (s == null) continue;
            if (s instanceof ColumnSort) {
                final ColumnSort<V> sort = (ColumnSort<V>) s;
                final Column<V> column = getColumn(sort.getColumnId());
                if (column == null) continue;
                sorts.put(column, new ColumnControllerSort(column, sort.isAsc()));
            } else { // s instanceof GeneralSort
                final GeneralSort<V> sort = (GeneralSort<V>) s;
                sorts.put(sort.getKey(), new GeneralControllerSort(sort.getComparator()));
            }
        }
        resetLiveData();
    }

    @Override
    public void enrichConfigBuilder(final DataGridConfigBuilder<V> builder) {
        checkAdapter();
        for (final Map.Entry<Object, Comparator<Row<V>>> entry : sorts.entrySet()) {
            if (entry.getValue() instanceof SimpleDataGridController.ColumnControllerSort) {
                final ColumnControllerSort sort = (ColumnControllerSort) entry.getValue();
                builder.addSort(new ColumnSort<>(sort.column.def.getId(), sort.asc));
            } else { // instanceof GeneralControllerSort
                final GeneralControllerSort sort = (GeneralControllerSort) entry.getValue();
                builder.addSort(new GeneralSort<>(entry.getKey(), sort.comparator));
            }
        }
    }

    @Override
    public void setBound(final boolean bound) {
        if (this.bound == bound) return;
        this.bound = bound;
        if (!bound || from >= to) return;
        doRefreshRows();
    }

    @Override
    public boolean getBound() {
        return bound;
    }

    @Override
    public boolean isSelected(final K k) {
        return selectedKeys.contains(k);
    }

    @Override
    public Collection<V> getLiveSelectedData() {
        return new MappedList<>(liveSelectedData, Row::getData);
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

    @Override
    public void selectAllLiveData() {
        liveSelectedData.clear();
        for (final Row<V> row : liveData) {
            liveSelectedData.add(row);
            selectedKeys.add(adapter.getKey(row.data));
        }
    }

    @Override
    public void unselectAllData() {
        liveSelectedData.clear();
        selectedKeys.clear();
    }

    private static class Interval {

        private final int from;
        private final int to;

        Interval(final int from, final int to) {
            super();
            this.from = from;
            this.to = to;
        }

    }

    private class RenderingHelperSupplier implements Supplier<Object> {

        private Row<V> row;
        private Column<V> column;

        private void set(final Row<V> row, final Column<V> column) {
            this.row = row;
            this.column = column;
        }

        private void clear() {
            this.row = null;
            this.column = null;
        }

        @Override
        public Object get() {
            return getRenderingHelper(row, column);
        }

    }

    private static class RenderingHelpersCache<V> extends LinkedHashMap<Row<V>, Object[]> {

        @Override
        protected boolean removeEldestEntry(final Entry<Row<V>, Object[]> eldest) {
            return size() > RENDERING_HELPERS_CACHE_CAPACITY;
        }
    }

    private static class Column<V> {

        private final ColumnDefinition<V> def;
        private final int id;

        private Column(final int id, final ColumnDefinition<V> def) {
            super();
            this.id = id;
            this.def = def;
        }

    }

    private static class Row<V> {

        private final int id;
        private V data;
        private boolean accepted;

        Row(final int id, final V data) {
            super();
            this.id = id;
            this.data = data;
        }

        V getData() {
            return data;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return "Row [id=" + id + ", data=" + data + ", accepted=" + accepted + "]";
        }

    }

    private class GeneralControllerSort implements Comparator<Row<V>> {

        private final Comparator<V> comparator;

        public GeneralControllerSort(final Comparator<V> comparator) {
            super();
            this.comparator = comparator;
        }

        @Override
        public int compare(final Row<V> r1, final Row<V> r2) {
            return comparator.compare(r1.data, r2.data);
        }

    }

    private class ColumnControllerSort implements Comparator<Row<V>> {

        private final Column<V> column;
        private final boolean asc;

        ColumnControllerSort(final Column<V> column, final boolean asc) {
            super();
            this.column = column;
            this.asc = asc;
        }

        @Override
        public int compare(final Row<V> r1, final Row<V> r2) {
            try {
                renderingHelperSupplier1.set(r1, column);
                renderingHelperSupplier2.set(r2, column);
                final int diff = asc ? column.def.compare(r1.data, renderingHelperSupplier1, r2.data, renderingHelperSupplier2)
                        : column.def.compare(r2.data, renderingHelperSupplier2, r1.data, renderingHelperSupplier1);
                return diff;
            } finally {
                renderingHelperSupplier1.clear();
                renderingHelperSupplier2.clear();
            }
        }

    }

    private static interface AbstractFilter<V> extends Predicate<Row<V>> {

        abstract ColumnDefinition<V> getColumnDefinition();

    }

    private class GeneralFilter implements AbstractFilter<V> {

        private final Predicate<V> filter;

        public GeneralFilter(final Predicate<V> filter) {
            super();
            this.filter = filter;
        }

        @Override
        public boolean test(final Row<V> row) {
            return filter.test(row.data);
        }

        @Override
        public ColumnDefinition<V> getColumnDefinition() {
            return null;
        }
    }

    private class ColumnFilter implements AbstractFilter<V> {

        private final Column<V> column;
        private final BiPredicate<V, Supplier<Object>> filter;

        ColumnFilter(final ColumnDefinition<V> colDef, final BiPredicate<V, Supplier<Object>> filter) {
            super();
            this.column = getColumn(colDef);
            this.filter = filter;
        }

        @Override
        public boolean test(final Row<V> row) {
            try {
                renderingHelperSupplier1.set(row, column);
                return filter.test(row.data, renderingHelperSupplier1);
            } finally {
                renderingHelperSupplier1.clear();
            }
        }

        @Override
        public ColumnDefinition<V> getColumnDefinition() {
            return column.def;
        }
    }
}
