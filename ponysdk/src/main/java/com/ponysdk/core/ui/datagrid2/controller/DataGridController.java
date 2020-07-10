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

import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.cell.Cell;
import com.ponysdk.core.ui.datagrid2.cell.ExtendedCell;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfig;
import com.ponysdk.core.ui.datagrid2.config.DataGridConfigBuilder;
import com.ponysdk.core.ui.datagrid2.data.ViewLiveData;
import com.ponysdk.core.ui.datagrid2.view.DataGridSnapshot;

/**
 * @author mbagdouri
 */

public interface DataGridController<K, V> {

    void renderCell(ColumnDefinition<V> column, int row, Cell<V> widget, ViewLiveData<V> result);

    void setValueOnExtendedCell(int row, ExtendedCell<V> widget, ViewLiveData<V> result);

    boolean isSelected(K k);

    Collection<V> getLiveSelectedData();

    void select(K k);

    void unselect(K k);

    void selectAllLiveData();

    void unselectAllData();

    void addSort(ColumnDefinition<V> column, boolean asc);

    void addSort(Object key, Comparator<V> comparator);

    void clearSort(Object key);

    void clearSort(ColumnDefinition<V> column);

    void clearSorts();

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

    int getRowCount();

    void enrichConfigBuilder(DataGridConfigBuilder<V> builder);

    /**
     * Send a partially filled object to the dataSource. The object will return
     * filled with the needed data for the view, and with updated fields
     * sometimes
     */
    void prepareLiveDataOnScreen(int dataSrcRowIndex, int dataSize, DataGridSnapshot threadSnapshot,
                                 Consumer<DefaultDataGridController<K, V>.DataSrcResult> consumer);

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
