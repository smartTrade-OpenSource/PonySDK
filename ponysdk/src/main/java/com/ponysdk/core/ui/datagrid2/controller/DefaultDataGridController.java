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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
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
import com.ponysdk.core.ui.datagrid2.data.DefaultRow;
import com.ponysdk.core.ui.datagrid2.data.Interval;
import com.ponysdk.core.ui.datagrid2.data.LiveDataView;
import com.ponysdk.core.ui.datagrid2.datasource.DataGridSource;
import com.ponysdk.core.ui.datagrid2.view.DataGridSnapshot;
import com.ponysdk.core.util.MappedList;

/**
 * @author mbagdouri
 */
public class DefaultDataGridController<K, V> implements DataGridController<K, V> {

    private static final Object NO_RENDERING_HELPER = new Object();
    private static final int RENDERING_HELPERS_CACHE_CAPACITY = 512;
    private int columnCounter = 0;
    private final RenderingHelpersCache<V> renderingHelpersCache = new RenderingHelpersCache<>();
    private final Map<ColumnDefinition<V>, Column<V>> columns = new HashMap<>();
    private DataGridControllerListener<V> listener;
    private DataGridAdapter<K, V> adapter;
    private boolean bound = true;
    private int from = Integer.MAX_VALUE;
    private int to = 0;
    private final RenderingHelperSupplier renderingHelperSupplier1 = new RenderingHelperSupplier();
    private final RenderingHelperSupplier renderingHelperSupplier2 = new RenderingHelperSupplier();
    private DataGridSource<K, V> dataSource;
    private DataGridSnapshot viewSnapshot;
    private final AtomicLong count = new AtomicLong();
    private long lastProcessedID;

    public DefaultDataGridController() {
        super();
    }

    public DefaultDataGridController(final DataGridSource<K, V> dataSource) {
        this.dataSource = dataSource;
        dataSource.setRenderingHelpersCache(renderingHelpersCache);
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

    private synchronized Object getRenderingHelper(final DefaultRow<V> row, final Column<V> column) {
        // FIXME : possible performance optimisation
        final Object[] renderingHelpers = renderingHelpersCache.computeIfAbsent(row, r -> new Object[columns.size()]);
        Object helper = renderingHelpers[column.getID()];
        if (helper == NO_RENDERING_HELPER) return null;
        if (helper == null) {
            helper = column.getColDef().getRenderingHelper(row.getData());
            renderingHelpers[column.getID()] = helper == null ? NO_RENDERING_HELPER : helper;
        }
        return helper;
    }

    private void clearRenderingHelpers(final DefaultRow<V> row) {
        renderingHelpersCache.remove(row);
    }

    private void clearRenderingHelper(final DefaultRow<V> row, final Column<V> column) {
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

    @Override
    public void refresh() {
        refreshRows(0, dataSource.getRowCount());
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
            final DefaultRow<V> row = dataSource.getRow(entry.getKey());
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
        final DefaultRow<V> row = dataSource.getRow(k);
        clearRenderingHelpers(row);
        final Interval interval = dataSource.updateData(k, updater);
        if (interval != null) refreshRows(interval.from, interval.to);
    }

    private void resetLiveData() {
        final int oldLiveDataSize = dataSource.getRowCount();
        dataSource.resetLiveData();
        // FIXME : this is to prevent not refreshing in a db filter case
        // refreshRows(0, Math.max(oldLiveDataSize, dataSource.getRowCount()));
        refreshRows(0, Math.max(oldLiveDataSize, 1));
    }

    @Override
    public final V removeData(final K k) {
        final DefaultRow<V> row = dataSource.getRow(k);
        if (row == null) return null;
        renderingHelpersCache.remove(row);
        final V v = dataSource.removeData(k);
        refresh();
        return v;
    }

    @Override
    public final V getData(final K k) {
        final DefaultRow<V> row = dataSource.getRow(k);
        if (row == null) return null;
        return row.getData();
    }

    @Override
    public void renderCell(final ColumnDefinition<V> colDef, final int row, final Cell<V> cell, final LiveDataView<V> result) {
        checkAdapter();
        final Column<V> column = getColumn(colDef);
        final DefaultRow<V> r = result.getLiveData().get(row);
        cell.render(r.getData(), getRenderingHelper(r, column));
    }

    @Override
    public void setValueOnExtendedCell(final int row, final ExtendedCell<V> extendedCell, final LiveDataView<V> result) {
        checkAdapter();
        final DefaultRow<V> r = result.getLiveData().get(row);
        extendedCell.setValue(r.getData());
    }

    @Override
    public void addSort(final ColumnDefinition<V> colDef, final boolean asc) {
        checkAdapter();
        final Column<V> column = getColumn(colDef);
        dataSource.addSort(column, new ColumnControllerSort(column, asc), asc);
        refresh();
    }

    @Override
    public void addSort(final Object key, final Comparator<V> comparator) {
        checkAdapter();
        dataSource.addSort(key, new GeneralControllerSort(comparator));
        refresh();
    }

    @Override
    public void addPrimarySort(final Object key, final Comparator<V> comparator) {
        checkAdapter();
        dataSource.addPrimarySort(key, new GeneralControllerSort(comparator));
        refresh();
    }

    @Override
    public void clearSort(final ColumnDefinition<V> colDef) {
        checkAdapter();
        final Column<V> column = getColumn(colDef);
        if (dataSource.clearSort(column) == null) return;
        dataSource.sort();
        refresh();
    }

    @Override
    public void clearSorts() {
        checkAdapter();
        dataSource.clearSorts();
        refresh();
    }

    @Override
    public void setFilter(final Object key, final ColumnDefinition<V> colDef, final BiPredicate<V, Supplier<Object>> biPredicate,
                          final boolean reinforcing) {
        checkAdapter();
        dataSource.setFilter(key, null, reinforcing, new ColumnFilter(colDef, biPredicate));
        resetLiveData();
    }

    @Override
    public void setFilter(final Object key, final String id, final Predicate<V> predicate, final boolean reinforcing) {
        checkAdapter();
        dataSource.setFilter(key, id, reinforcing, new GeneralFilter(predicate));
        resetLiveData();
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
        refresh();
    }

    private class DataGetterFromSrc implements Supplier<DataGetterFromSrc> {

        public long id = count.incrementAndGet();
        public LiveDataView<V> liveDataView;
        public int firstRowIndex;
        public int start;
        DataGridSnapshot threadSnapshot;
        Supplier<LiveDataView<V>> dataSupplier;

        DataGetterFromSrc(final Supplier<LiveDataView<V>> inner, final DataGridSnapshot threadSnapshot, final int firstRowIndex,
                final int start) {
            this.dataSupplier = inner;
            this.threadSnapshot = threadSnapshot;
            this.firstRowIndex = firstRowIndex;
            this.start = start;
        }

        @Override
        public DataGetterFromSrc get() {
            liveDataView = dataSupplier.get();
            return this;
        }
    }

    public class DataSrcResult {

        public LiveDataView<V> liveDataView;
        public int firstRowIndex;
        public int start;

        public DataSrcResult(final LiveDataView<V> liveDataView, final int firstRowIndex, final int start) {
            this.liveDataView = liveDataView;
            this.firstRowIndex = firstRowIndex;
            this.start = start;
        }
    }

    @Override
    public void prepareLiveDataOnScreen(final int dataSrcRowIndex, final int dataSize, final DataGridSnapshot threadSnapshot,
                                        final Consumer<DataSrcResult> consumer) {
        viewSnapshot = new DataGridSnapshot(threadSnapshot);
        final CompletableFuture<DataGetterFromSrc> completableFuture = CompletableFuture.supplyAsync(new DataGetterFromSrc(
            () -> dataSource.getRows(dataSrcRowIndex, dataSize), threadSnapshot, dataSrcRowIndex, threadSnapshot.start));

        final CompletableFuture<DataGetterFromSrc> threadAcceptanceByID = completableFuture.thenApply(dataResponse -> {
            if (dataResponse.id < lastProcessedID) {
                dataResponse.id = -1;
            } else {
                lastProcessedID = dataResponse.id;
            }
            return dataResponse;
        });

        threadAcceptanceByID.thenAccept(dataResponse -> {
            threadAcceptance(dataResponse, threadAcceptanceByID, consumer);
        });
    }

    private void threadAcceptance(final DataGetterFromSrc result, final CompletableFuture<DataGetterFromSrc> completableFuture,
                                  final Consumer<DataSrcResult> consumer) {
        if (viewSnapshot.equals(result.threadSnapshot) && result.id >= 0) {
            final CompletableFuture<DataSrcResult> passResultToView = completableFuture.thenApply(dataResponse -> {
                return new DataSrcResult(dataResponse.liveDataView, dataResponse.firstRowIndex, dataResponse.start);
            });
            passResultToView.thenAccept(consumer::accept);
        }
    }

    @Override
    public DataGridController<K, V> get() {
        return this;
    }

    @Override
    public void setListener(final DataGridControllerListener<V> listener) {
        this.listener = listener;
    }

    @Override
    public void clearRenderingHelper(final ColumnDefinition<V> colDef, final K key) {
        checkAdapter();
        final DefaultRow<V> row = dataSource.getRow(key);
        if (colDef != null && row != null) clearRenderingHelper(row, getColumn(colDef));
    }

    @Override
    public void clearRenderingHelpers(final ColumnDefinition<V> colDef) {
        checkAdapter();
        final Column<V> column = getColumn(colDef);
        for (final DefaultRow<V> row : dataSource.getRows()) {
            clearRenderingHelper(row, column);
        }
    }

    @Override
    public int getRowCount() {
        return dataSource.getRowCount();
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
        for (final Map.Entry<Object, Comparator<DefaultRow<V>>> entry : dataSource.getSortsEntry()) {
            if (entry.getValue() instanceof DefaultDataGridController.ColumnControllerSort) {
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
        final List<DefaultRow<V>> liveSelectedData = dataSource.getLiveSelectedData();
        return new MappedList<>(liveSelectedData, DefaultRow::getData);
    }

    @Override
    public Collection<V> getLiveData(final int from, final int dataSize) {
        final LiveDataView<V> liveData = dataSource.getRows(from, dataSize);
        return new MappedList<>(liveData.getLiveData(), DefaultRow::getData);
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

        private DefaultRow<V> row;
        private Column<V> column;

        private void set(final DefaultRow<V> row, final Column<V> column) {
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

    public static class RenderingHelpersCache<V> extends LinkedHashMap<DefaultRow<V>, Object[]> {

        @Override
        protected boolean removeEldestEntry(final Map.Entry<DefaultRow<V>, Object[]> eldest) {
            return size() > RENDERING_HELPERS_CACHE_CAPACITY;
        }
    }

    private class GeneralControllerSort implements Comparator<DefaultRow<V>> {

        private final Comparator<V> comparator;

        public GeneralControllerSort(final Comparator<V> comparator) {
            super();
            this.comparator = comparator;
        }

        @Override
        public int compare(final DefaultRow<V> r1, final DefaultRow<V> r2) {
            return comparator.compare(r1.getData(), r2.getData());
        }
    }

    public class ColumnControllerSort implements Comparator<DefaultRow<V>> {

        private final Column<V> column;
        private final boolean asc;

        public ColumnControllerSort(final Column<V> column, final boolean asc) {
            super();
            this.column = column;
            this.asc = asc;
        }

        @Override
        public int compare(final DefaultRow<V> r1, final DefaultRow<V> r2) {
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

        public Column<V> getColumn() {
            return column;
        }
    }

    private class GeneralFilter implements AbstractFilter<V> {

        private final Predicate<V> filter;

        public GeneralFilter(final Predicate<V> filter) {
            super();
            this.filter = filter;
        }

        @Override
        public boolean test(final DefaultRow<V> row) {
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
        public boolean test(final DefaultRow<V> row) {
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