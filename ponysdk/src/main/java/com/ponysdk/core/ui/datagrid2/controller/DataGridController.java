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
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.ponysdk.core.server.service.query.PResultSet;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.cell.Cell;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.view.DataGridSnapshot;
import com.ponysdk.core.ui.datagrid2.view.DataGridView;
import com.ponysdk.core.util.Pair;

/**
 * @author mbagdouri
 */

public interface DataGridController<K, V> {

    void renderCell(ColumnDefinition<V> column, Cell<V, ?> widget, V data);

    boolean isSelected(K k);

    boolean isSelectable(K k);

    public PResultSet<V> getFilteredData();

    public PResultSet<V> getLiveSelectedData();

    /**
     *
     * @return the number of selected data
     */
    int getLiveSelectedDataCount();

    Collection<V> getLiveData(int from, int dataSize);

    void select(K k);

    void unselect(K k);

    void selectAllLiveData();

    void unselectAllData();

    void addSort(ColumnDefinition<V> column, boolean asc);

    void addSort(Object key, Comparator<V> comparator);

    // FIXME a better solution can be an insert with a specified index
    void addPrimarySort(Object key, Comparator<V> comparator);

    void clearSort(Object key);

    void clearSort(ColumnDefinition<V> column);

    void clearSorts();

    void sort();

    void setFilter(Object key, ColumnDefinition<V> column, BiPredicate<V, Supplier<Object>> filter, boolean reinforcing);

    void setFilter(Object key, String id, Predicate<V> filter, boolean reinforcing);

    void clearFilter(Object key);

    void clearFilters(ColumnDefinition<V> column);

    void clearFilters();

    void setConfig(DataGridConfig<V> config);

    void setAdapter(DataGridAdapter<K, V> adapter);

    DataGridController<K, V> get();

    void setListener(DataGridControllerListener<V> listener);

    void clearRenderingHelpers(ColumnDefinition<V> column);

    void clearRenderingHelper(ColumnDefinition<V> column, final K key);

    int getRowCount();

    void enrichConfigBuilder(DataGridConfigBuilder<V> builder);

    /**
     * Performs a full refresh; i.e. same as {@link #refreshOnNextDraw()} but with a forced redraw on
     * {@link DataGridView} afterwards.
     */
    void refresh();

    /**
     * Ensures that all data will be refreshed during the next draw performed on {@link DataGridView} but without
     * performing it immediately.<br>
     * <br>
     * Indeed, a performance mechanism limits the rows which gets refreshed during a draw and this refresh ensures the
     * whole dataset will be refreshed.<br>
     * <br>
     *
     * splits the data in 2 zones: a constant one and a variable one.
     * The variable zone is the only one that gets updated during a draw and it is constantly narrowed down for better
     * performance.<br>
     * <br>
     * This refresh will grow the variable zone on the whole dataset so that it gets updated next time but it will not
     * force any redraw.
     */
    void refreshOnNextDraw();

    /**
     * Send a partially filled object to the dataSource. The object will return
     * filled with the needed data for the view, and with updated fields
     * sometimes
     */
    void prepareLiveDataOnScreen(int dataSrcRowIndex, int dataSize, DataGridSnapshot threadSnapshot,
                                 Consumer<Pair<DefaultDataGridController<K, V>.DataSrcResult, Throwable>> consumer);

    /**
     * Insert or replace the value
     */
    void setData(V v);

    /**
     * Insert or replace the collection of values
     *
     * @param c
     */
    void setData(Collection<V> c);

    /**
     * Update an existing value identified by key {@code k}, if it is present,
     * using the {@code updater}
     */
    void updateData(K k, Consumer<V> updater);

    /**
     * Update existing values identified by the key, if they are present, using
     * the {@code updaters}
     */
    void updateData(Map<K, Consumer<V>> updaters);

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this model contains no mapping for the key.
     */
    V getData(K k);

    /**
     * Removes the value identified by the key {@code k} from this model if it
     * is present
     */
    V removeData(K k);

    /**
     * If {@code bound} is true (default), the view is notified when the model
     * is updated. Otherwise, the view is not notified but it will continue to
     * see the most recent version of the model
     *
     * @param bound
     */
    void setBound(boolean bound);

    /**
     * Whether the model is bound to the view.
     */
    boolean getBound();

}
