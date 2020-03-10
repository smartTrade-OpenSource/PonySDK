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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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

public class SimpleDataGridController<K, V> implements DataGridController<K, V>, DataGridModel<K, V> {

    private static final Object NO_RENDERING_HELPER = new Object();
    private static final int RENDERING_HELPERS_CACHE_CAPACITY = 512;
    private int columnCounter = 0;
    private final RenderingHelpersCache<V> renderingHelpersCache = new RenderingHelpersCache<>();
    private final List<Row<V>> liveDataOnScreen = new ArrayList<>();
    private final Map<ColumnDefinition<V>, Column<V>> columns = new HashMap<>();
    private DataGridControllerListener<V> listener;
    private DataGridAdapter<K, V> adapter;
    private boolean bound = true;
    private int from = Integer.MAX_VALUE;
    private int to = 0;
    private final RenderingHelperSupplier renderingHelperSupplier1 = new RenderingHelperSupplier();
    private final RenderingHelperSupplier renderingHelperSupplier2 = new RenderingHelperSupplier();
    private int absoluteIndex = 0; //Represents the position index of liveDataOnScreen in the overall dataGrid
    private DataGridSource<K, V> dataSource = new SimpleCacheDataSource<>(); // ToDo datasource has to be injected by spring
    private boolean isHorizontalScroll = false;

    SimpleDataGridController() {
        super();
        setDataSource(dataSource);
    }

    public void setDataSource(final DataGridSource<K, V> dataSrc) {
        dataSource = dataSrc;
        dataSource.setRenderingHelpersCache(renderingHelpersCache);
    }

    // The adapter is used to set up and initialize the DataGridView
    @Override
    public void setAdapter(final DataGridAdapter<K, V> adapter) {
        if (this.adapter != null) throw new IllegalStateException("DataGridAdapter is already set");
        this.adapter = adapter;
        dataSource.setAdapter(adapter);
        for (final ColumnDefinition<V> column : adapter.getColumnDefinitions()) {
            columns.put(column, new Column<>(columnCounter++, column));
        }
    }

    private void checkAdapter() {
        if (adapter == null) throw new IllegalStateException("A DataGridAdapter must be set");
    }

    // Get the Column
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

    // To prevent recalculating on some data we stock them in renderingHelpersCache
    private Object getRenderingHelper(final Row<V> row, final Column<V> column) {
        final Object[] renderingHelpers = renderingHelpersCache.computeIfAbsent(row, r -> new Object[columns.size()]);
        Object helper = renderingHelpers[column.id];
        if (helper == NO_RENDERING_HELPER) return null;
        if (helper == null) {
            helper = column.def.getRenderingHelper(row.data);
            renderingHelpers[column.id] = helper == null ? NO_RENDERING_HELPER : helper;
        }
        return helper;
    }

    private void clearRenderingHelpers(final Row<V> row) {
        renderingHelpersCache.remove(row);
    }

    private void clearRenderingHelper(final Row<V> row, final Column<V> column) {
        final Object[] renderingHelpers = renderingHelpersCache.get(row);
        if (renderingHelpers == null) return;
        renderingHelpers[column.id] = null;
    }

    // clears the Map filters then resets data
    @Override
    public void clearFilters(final ColumnDefinition<V> column) {
        checkAdapter();
        Objects.requireNonNull(column);
        dataSource.clearFilters(column);
        resetLiveData();
    }

    @Override
    public final void clearFilters() {
        checkAdapter();
        dataSource.clearFilters();
        resetLiveData();
    }

    // refresh and update the rows (from-to)
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

    /*
     * If the row corresponding to this value is found in the dataSource update it, if not found
     * insert it
     */
    @Override
    public final void setData(final V v) {
        //        addDefaultSort();
        final Interval interval = dataSource.setData(v);
        if (interval != null) refreshRows(interval.from, interval.to);
        //        if (interval != null) refreshRows(0, dataSource.getRowCount());
    }

    @Override
    public void setData(final Collection<V> c) {
        int from = Integer.MAX_VALUE;
        int to = 0;
        for (final V v : c) {
            final Interval interval = dataSource.setData(v);
            if (interval == null) continue;
            from = Math.min(from, interval.from);
            to = Math.max(to, interval.to);
        }
        if (from < to) refreshRows(from, to);
    }

    @Override
    public void updateData(final Map<K, Consumer<V>> updaters) {
        int from = Integer.MAX_VALUE;
        int to = 0;
        for (final Map.Entry<K, Consumer<V>> entry : updaters.entrySet()) {
            final Row<V> row = dataSource.getRow(entry.getKey());
            clearRenderingHelpers(row);
            final Interval interval = dataSource.updateData(entry.getKey(), entry.getValue());
            if (interval == null) continue;
            from = Math.min(from, interval.from);
            to = Math.max(to, interval.to);
        }
        if (from < to) refreshRows(from, to);
    }

    @Override
    public void updateData(final K k, final Consumer<V> updater) {
        final Row<V> row = dataSource.getRow(k);
        clearRenderingHelpers(row);
        final Interval interval = dataSource.updateData(k, updater);
        if (interval != null) refreshRows(interval.from, interval.to);
    }

    private void resetLiveData() {
        final int oldLiveDataSize = dataSource.getRowCount();
        dataSource.resetLiveData();
        refreshRows(0, Math.max(oldLiveDataSize, dataSource.getRowCount()));
    }

    @Override
    public final V removeData(final K k) {
        final Row<V> row = dataSource.getRow(k);
        if (row == null) return null;
        renderingHelpersCache.remove(row);
        final V v = dataSource.removeData(k);
        refreshRows(0, dataSource.getRowCount());
        return v;
    }

    @Override
    public final V getData(final K k) {
        final Row<V> row = dataSource.getRow(k);
        if (row == null) return null;
        return row.data;
    }

    @Override
    public void renderCell(final ColumnDefinition<V> colDef, final int row, final Cell<V> cell) {
        checkAdapter();
        final Column<V> column = getColumn(colDef);
        final Row<V> r = liveDataOnScreen.get(row);
        cell.render(r.data, getRenderingHelper(r, column));
    }

    @Override
    public void setValueOnExtendedCell(final int row, final ExtendedCell<V> extendedCell) {
        checkAdapter();
        final Row<V> r = liveDataOnScreen.get(row);
        extendedCell.setValue(r.data);
    }

    @Override
    public void addSort(final ColumnDefinition<V> colDef, final boolean asc) {

        checkAdapter();
        final Column<V> column = getColumn(colDef);
        dataSource.addSort(column, new ColumnControllerSort(column, asc), asc);
        refreshRows(0, dataSource.getRowCount());
    }

    @Override
    public void addSort(final Object key, final Comparator<V> comparator) {
        checkAdapter();
        dataSource.addSort(key, new GeneralControllerSort(comparator));
        refreshRows(0, dataSource.getRowCount());
    }

    @Override
    public void clearSort(final ColumnDefinition<V> colDef) {
        checkAdapter();
        final Column<V> column = getColumn(colDef);
        if (dataSource.clearSort(column) == null) return;
        dataSource.sort();
        refreshRows(0, dataSource.getRowCount());
    }

    @Override
    public void clearSorts() {
        checkAdapter();
        dataSource.clearSorts();
        refreshRows(0, dataSource.getRowCount());
    }

    @Override
    public void setFilter(final Object key, final ColumnDefinition<V> colDef, final BiPredicate<V, Supplier<Object>> biPredicate,
                          final boolean reinforcing) {
        final int oldLiveDataSize = dataSource.getRowCount();
        checkAdapter();
        dataSource.setFilter(key, reinforcing, new ColumnFilter(colDef, biPredicate));
        refreshRows(0, oldLiveDataSize);
    }

    @Override
    public void setFilter(final Object key, final Predicate<V> predicate, final boolean reinforcing) {
        checkAdapter();
        final int oldLiveDataSize = dataSource.getRowCount();
        dataSource.setFilter(key, reinforcing, new GeneralFilter(predicate));
        refreshRows(0, oldLiveDataSize);
    }

    @Override
    public void clearFilter(final Object key) {
        checkAdapter();
        final AbstractFilter<V> oldFilter = dataSource.clearFilter(key);
        if (oldFilter == null) return;
        resetLiveData();
    }

    @Override
    public void clearSort(final Object key) {
        checkAdapter();
        if (dataSource.clearSort(key) == null) return;
        dataSource.sort();
        refreshRows(0, dataSource.getRowCount());
    }

    @Override
    public void setHorizontalScroll(final boolean isHorizontalScroll) {
        this.isHorizontalScroll = isHorizontalScroll;
    }

    @Override
    public void prepareLiveDataOnScreen(final int rowIndex, final int size) {
        /*
         * If we have already the data in liveDataOnScreen then return
         * Use Case: Horizontal Scroll / Pin / Unpin / Delete-Move column
         */
        if (absoluteIndex == rowIndex && size == liveDataOnScreen.size() && isHorizontalScroll) {
            System.out.println("#-Ctrl-# We prepare nothing");
            return;
        }
        /*
         * If we have already some of the data then ask only for what we don't have
         * Use Case: Window resize
         */ //FIXME
        //        if (absoluteIndex == rowIndex && size > liveDataOnScreen.size() && !liveDataOnScreen.isEmpty()) {
        //            System.out.println("#-Ctrl-# We prepare in+ -> row: " + (rowIndex + liveDataOnScreen.size()) + "   size: "
        //                    + (size - liveDataOnScreen.size()));
        //            final List<Row<V>> tmp = dataSource.getRows(rowIndex + liveDataOnScreen.size(), size - liveDataOnScreen.size());
        //            liveDataOnScreen.addAll(tmp);
        //            absoluteIndex = rowIndex;
        //            return;
        //        }
        /*
         * If the demanded data is different from what we have then overwite it with the new data
         * Use Case: Sort / Filter / Scroll Vertical
         */
        if (liveDataOnScreen != null) {
            liveDataOnScreen.clear();
            System.out.println("#-Ctrl-# We clean then prepare -> row: " + (rowIndex + liveDataOnScreen.size()) + "   size: "
                    + (size - liveDataOnScreen.size()));
            liveDataOnScreen.addAll(dataSource.getRows(rowIndex, size));
            absoluteIndex = rowIndex;
            return;
        }
    }

    @Override
    public V getRowData(final int rowIndex) {
        checkAdapter();
        final V v = rowIndex < liveDataOnScreen.size() ? liveDataOnScreen.get(rowIndex).getData() : null;
        if (v == null) {
            System.out.println("this is null !! ");
            return dataSource.getRows(rowIndex, 1).get(0).getData();
        }
        return v;
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
        //Modified
        //        for (final Row<V> row : cache.values()) {
        for (final Row<V> row : dataSource.getRows()) {
            clearRenderingHelper(row, column);
        }
    }

    @Override
    public int getRowCount() {
        return dataSource.getRowCount();
    }

    //    @Override
    //    public String toString() {
    //        //Modified
    //        //        return "DefaultDataGridController [cache=" + cache + ", liveData=" + liveData + "]";
    //
    ////        return "DefaultDataGridController [cache=" + dataSource + ", liveData=" + liveData + "]";
    //    }

    @Override
    public Collection<V> getLiveData() {
        //        return new MappedList<>(liveData, Row::getData);
        return new MappedList<>(liveDataOnScreen, Row::getData);
    }

    @Override
    public void setConfig(final DataGridConfig<V> config) {
        checkAdapter();

        dataSource.clearSorts();
        dataSource.clearFilters();

        for (final Sort<V> s : config.getSorts()) {
            if (s == null) continue;
            if (s instanceof ColumnSort) {
                final ColumnSort<V> sort = (ColumnSort<V>) s;
                final Column<V> column = getColumn(sort.getColumnId());
                if (column == null) continue;
                dataSource.addSort(column, new ColumnControllerSort(column, sort.isAsc()));

            } else { // s instanceof GeneralSort
                final GeneralSort<V> sort = (GeneralSort<V>) s;
                dataSource.addSort(sort.getKey(), new GeneralControllerSort(sort.getComparator()));
            }
        }
        resetLiveData();
    }

    @Override
    public void enrichConfigBuilder(final DataGridConfigBuilder<V> builder) {
        checkAdapter();
        for (final Map.Entry<Object, Comparator<Row<V>>> entry : dataSource.getSortsEntry()) {
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
        return dataSource.isSelected(k);
    }

    @Override
    public Collection<V> getLiveSelectedData() {
        final List<Row<V>> liveSelectedData = ((SimpleDataSource) dataSource).liveSelectedData; //FIXME
        return new MappedList<>(liveSelectedData, Row::getData);
    }

    @Override
    public void select(final K k) {
        dataSource.select(k);
    }

    @Override
    public void unselect(final K k) {
        dataSource.unselect(k);
    }

    @Override
    public void selectAllLiveData() {
        dataSource.selectAllLiveData();
    }

    @Override
    public void unselectAllData() {
        dataSource.unselectAllData();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////// Nested Classes /////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    public static class Interval {

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

    public static class RenderingHelpersCache<V> extends LinkedHashMap<Row<V>, Object[]> {

        @Override
        protected boolean removeEldestEntry(final Entry<Row<V>, Object[]> eldest) {
            return size() > RENDERING_HELPERS_CACHE_CAPACITY;
        }
    }

    // Modified private
    public static class Column<V> {

        private final ColumnDefinition<V> def;
        private final int id;

        private Column(final int id, final ColumnDefinition<V> def) {
            super();
            this.id = id;
            this.def = def;
        }

        //Added
        public ColumnDefinition<V> getColDef() {
            return def;
        }

    }

    // Modified accessor
    public static class Row<V> {

        final int id;
        public V data;
        public boolean accepted;

        public Row(final int id, final V data) {
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

    //Modified private
    public class ColumnControllerSort implements Comparator<Row<V>> {

        private final Column<V> column;
        //Modified private
        public final boolean asc;

        public ColumnControllerSort(final Column<V> column, final boolean asc) {
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

        public boolean getSortAsc() {
            return asc;
        }

    }

    //    private static interface AbstractFilter<V> extends Predicate<Row<V>> {
    //
    //        abstract ColumnDefinition<V> getColumnDefinition();
    //
    //    }

    public class GeneralFilter implements AbstractFilter<V> {

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

    public class ColumnFilter implements AbstractFilter<V> {

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