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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.cell.Cell;
import com.ponysdk.core.ui.datagrid2.cell.ExtendedCell;
import com.ponysdk.core.ui.datagrid2.column.Column;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.ColumnSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.GeneralSort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig.Sort;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.data.AbstractFilter;
import com.ponysdk.core.ui.datagrid2.data.DataSrcResult;
import com.ponysdk.core.ui.datagrid2.data.Interval;
import com.ponysdk.core.ui.datagrid2.data.SimpleRow;
import com.ponysdk.core.ui.datagrid2.datasource.DataGridSource;
import com.ponysdk.core.ui.datagrid2.model.DataGridModel;
import com.ponysdk.core.util.MappedList;

/**
 * @author mbagdouri
 */

public class SimpleDataGridController<K, V> implements DataGridController<K, V>, DataGridModel<K, V> {

    private static final Object NO_RENDERING_HELPER = new Object();
    private static final int RENDERING_HELPERS_CACHE_CAPACITY = 512;
    private int columnCounter = 0;
    private final RenderingHelpersCache<V> renderingHelpersCache = new RenderingHelpersCache<>();
    //    public final List<Row<V>> liveDataOnScreen = new ArrayList<>();
    //FIXME
    public List<SimpleRow<V>> liveDataOnScreen = Collections.synchronizedList(new ArrayList<SimpleRow<V>>());
    private final Map<ColumnDefinition<V>, Column<V>> columns = new HashMap<>();
    private DataGridControllerListener<V> listener;
    private DataGridAdapter<K, V> adapter;
    private boolean bound = true;
    private int from = Integer.MAX_VALUE;
    private int to = 0;
    private final RenderingHelperSupplier renderingHelperSupplier1 = new RenderingHelperSupplier();
    private final RenderingHelperSupplier renderingHelperSupplier2 = new RenderingHelperSupplier();
    private final int absoluteIndex = 0; //Represents the position index of liveDataOnScreen in the overall dataGrid
    private DataGridSource<K, V> dataSource;

    public SimpleDataGridController() {
        super();
    }

    public SimpleDataGridController(final DataGridSource<K, V> dataSource) {
        this.dataSource = dataSource;
        dataSource.setRenderingHelpersCache(renderingHelpersCache);
    }

    public void setDataSource(final DataGridSource<K, V> dataSrc) {
        this.dataSource = dataSrc;
    }

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

    private Column<V> getColumn(final ColumnDefinition<V> colDef) {
        final Column<V> column = columns.get(colDef);
        if (column == null) throw new IllegalArgumentException("Unknown ColumnDefinition " + colDef);
        return column;
    }

    private Column<V> getColumn(final String columnId) {
        for (final Column<V> column : columns.values()) {
            if (columnId.equals(column.getColDef().getId())) return column;
        }
        return null;
    }

    private Object getRenderingHelper(final SimpleRow<V> row, final Column<V> column) {
        final Object[] renderingHelpers = renderingHelpersCache.computeIfAbsent(row, r -> new Object[columns.size()]);
        Object helper = renderingHelpers[column.getID()];
        if (helper == NO_RENDERING_HELPER) return null;
        if (helper == null) {
            helper = column.getColDef().getRenderingHelper(row.getData());
            renderingHelpers[column.getID()] = helper == null ? NO_RENDERING_HELPER : helper;
        }
        return helper;
    }

    private void clearRenderingHelpers(final SimpleRow<V> row) {
        renderingHelpersCache.remove(row);
    }

    private void clearRenderingHelper(final SimpleRow<V> row, final Column<V> column) {
        final Object[] renderingHelpers = renderingHelpersCache.get(row);
        if (renderingHelpers == null) return;
        renderingHelpers[column.getID()] = null;
    }

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
        final Interval interval = dataSource.setData(v);
        if (interval != null) refreshRows(interval.from, interval.to);
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
            final SimpleRow<V> row = dataSource.getRow(entry.getKey());
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
        final SimpleRow<V> row = dataSource.getRow(k);
        clearRenderingHelpers(row);
        final Interval interval = dataSource.updateData(k, updater);
        if (interval != null) refreshRows(interval.from, interval.to);
    }

    private void resetLiveData() {
        //FIXME
        final int oldLiveDataSize = dataSource.getRowCount();
        dataSource.resetLiveData();
        refreshRows(0, Math.max(oldLiveDataSize, dataSource.getRowCount()));
        //        refreshRows(0, Math.max(oldLiveDataSize, 1));
    }

    @Override
    public final V removeData(final K k) {
        final SimpleRow<V> row = dataSource.getRow(k);
        if (row == null) return null;
        renderingHelpersCache.remove(row);
        final V v = dataSource.removeData(k);
        refreshRows(0, dataSource.getRowCount());
        return v;
    }

    @Override
    public final V getData(final K k) {
        final SimpleRow<V> row = dataSource.getRow(k);
        if (row == null) return null;
        return row.getData();
    }

    @Override
    //FIXME
    //    public void renderCell(final ColumnDefinition<V> colDef, final int row, final Cell<V> cell) {
    //        checkAdapter();
    //        final Column<V> column = getColumn(colDef);
    //        final SimpleRow<V> r = liveDataOnScreen.get(row);
    //        cell.render(r.getData(), getRenderingHelper(r, column));
    //    }
    public void renderCell(final ColumnDefinition<V> colDef, final int row, final Cell<V> cell, final DataSrcResult<V> result) {
        checkAdapter();
        final Column<V> column = getColumn(colDef);
        final SimpleRow<V> r = result.liveData.get(row);
        cell.render(r.getData(), getRenderingHelper(r, column));
    }

    @Override
    public void setValueOnExtendedCell(final int row, final ExtendedCell<V> extendedCell) {
        checkAdapter();
        final SimpleRow<V> r = liveDataOnScreen.get(row);
        extendedCell.setValue(r.getData());
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
        dataSource.setFilter(key, null, reinforcing, new ColumnFilter(colDef, biPredicate));
        refreshRows(0, oldLiveDataSize);
    }

    @Override
    public void setFilter(final Object key, final String id, final Predicate<V> predicate, final boolean reinforcing) {
        checkAdapter();
        final int oldLiveDataSize = dataSource.getRowCount();
        dataSource.setFilter(key, id, reinforcing, new GeneralFilter(predicate));
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

    //    @Override
    //    public synchronized void prepareLiveDataOnScreen(final int rowIndex, final int size, final boolean isHorizontalScroll) {
    //        //If we have already the data in liveDataOnScreen then return (Horizontal Scroll/Pin/Unpin/Delete-Move column)
    //        if (absoluteIndex == rowIndex && size == liveDataOnScreen.size() && isHorizontalScroll) {
    //            System.out.println("#-Ctrl-# We prepare nothing");
    //            return;
    //        }
    //        //If the demanded data is different from what we have (Sort/Filter/Vertical Scroll)
    //        if (liveDataOnScreen != null) {
    //            //            liveDataOnScreen.clear();
    //            //            System.out.println("#-Ctrl-# clearing liveDataOnScreen in the thread: " + Thread.currentThread().getId());
    //            //            System.out.println("#-Ctrl-# We will prepare the data in the thread: " + Thread.currentThread().getId());
    //            //            liveDataOnScreen.addAll(dataSource.getRows(rowIndex, size));
    //            //            absoluteIndex = rowIndex;
    //            //            return;
    //
    //            List<Row<V>> myLiveData = new ArrayList<>(liveDataOnScreen);
    //            System.out.println("#-Ctrl-# We will prepare the data in the thread: " + Thread.currentThread().getId());
    //            myLiveData = dataSource.getRows(rowIndex, size);
    //            liveDataOnScreen = myLiveData;
    //            absoluteIndex = rowIndex;
    //            return;
    //        }
    //    }

    //    @Override
    //    public List<SimpleRow<V>> prepareLiveDataOnScreen(final int rowIndex, final int size, final boolean isHorizontalScroll) {
    //        return dataSource.getRows(rowIndex, size);
    //    }

    @Override
    public DataSrcResult<V> prepareLiveDataOnScreen(final DataSrcResult<V> dataSrcResult) {
        return dataSource.getRows(dataSrcResult);
    }

    @Override
    public V getRowData(final int rowIndex) {
        checkAdapter();
        final V v = rowIndex < liveDataOnScreen.size() ? liveDataOnScreen.get(rowIndex).getData() : null;
        if (v == null) {
            System.out.println("Row " + rowIndex + " is null ! in the thread: " + Thread.currentThread().getId());
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
        for (final SimpleRow<V> row : dataSource.getRows()) {
            clearRenderingHelper(row, column);
        }
    }

    @Override
    public int getRowCount() {
        return dataSource.getRowCount();
    }

    @Override
    public String toString() {
        return "DefaultDataGridController liveDataOnScreen=" + liveDataOnScreen + "]";
    }

    @Override
    public Collection<V> getLiveData() {
        //        return new MappedList<>(liveDataOnScreen, Row::getData);
        return null;
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
        for (final Map.Entry<Object, Comparator<SimpleRow<V>>> entry : dataSource.getSortsEntry()) {
            if (entry.getValue() instanceof SimpleDataGridController.ColumnControllerSort) {
                final ColumnControllerSort sort = (ColumnControllerSort) entry.getValue();
                builder.addSort(new ColumnSort<>(sort.column.getColDef().getId(), sort.asc));
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
        final List<SimpleRow<V>> liveSelectedData = dataSource.getLiveSelectedData();
        return new MappedList<>(liveSelectedData, SimpleRow::getData);
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

    private class RenderingHelperSupplier implements Supplier<Object> {

        private SimpleRow<V> row;
        private Column<V> column;

        private void set(final SimpleRow<V> row, final Column<V> column) {
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

    //    public static class RenderingHelpersCache<V> extends LinkedHashMap<SimpleRow<V>, Object[]> {
    //
    //        @Override
    //        protected boolean removeEldestEntry(final Map.Entry<SimpleRow<V>, Object[]> eldest) {
    //            return size() > RENDERING_HELPERS_CACHE_CAPACITY;
    //        }
    //    }
    public static class RenderingHelpersCache<V> extends LinkedHashMap<SimpleRow<V>, Object[]> {

        @Override
        protected boolean removeEldestEntry(final Map.Entry<SimpleRow<V>, Object[]> eldest) {
            return size() > RENDERING_HELPERS_CACHE_CAPACITY;
        }
    }

    private class GeneralControllerSort implements Comparator<SimpleRow<V>> {

        private final Comparator<V> comparator;

        public GeneralControllerSort(final Comparator<V> comparator) {
            super();
            this.comparator = comparator;
        }

        @Override
        public int compare(final SimpleRow<V> r1, final SimpleRow<V> r2) {
            return comparator.compare(r1.getData(), r2.getData());
        }
    }

    public class ColumnControllerSort implements Comparator<SimpleRow<V>> {

        private final Column<V> column;
        public final boolean asc;

        public ColumnControllerSort(final Column<V> column, final boolean asc) {
            super();
            this.column = column;
            this.asc = asc;
        }

        @Override
        public int compare(final SimpleRow<V> r1, final SimpleRow<V> r2) {
            try {
                renderingHelperSupplier1.set(r1, column);
                renderingHelperSupplier2.set(r2, column);
                final int diff = asc
                        ? column.getColDef().compare(r1.getData(), renderingHelperSupplier1, r2.getData(), renderingHelperSupplier2)
                        : column.getColDef().compare(r2.getData(), renderingHelperSupplier2, r1.getData(), renderingHelperSupplier1);
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

    private class GeneralFilter implements AbstractFilter<V> {

        private final Predicate<V> filter;

        public GeneralFilter(final Predicate<V> filter) {
            super();
            this.filter = filter;
        }

        @Override
        public boolean test(final SimpleRow<V> row) {
            return filter.test(row.getData());
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
        public boolean test(final SimpleRow<V> row) {
            try {
                renderingHelperSupplier1.set(row, column);
                return filter.test(row.getData(), renderingHelperSupplier1);
            } finally {
                renderingHelperSupplier1.clear();
            }
        }

        @Override
        public ColumnDefinition<V> getColumnDefinition() {
            return column.getColDef();
        }
    }
}