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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.Column;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.controller.DefaultDataGridController;
import com.ponysdk.core.ui.datagrid2.controller.DefaultDataGridController.RenderingHelpersCache;
import com.ponysdk.core.ui.datagrid2.data.AbstractFilter;
import com.ponysdk.core.ui.datagrid2.data.DefaultRow;
import com.ponysdk.core.ui.datagrid2.data.Interval;
import com.ponysdk.core.ui.datagrid2.data.ViewLiveData;

/**
 *
 */
public interface DataGridSource<K, V> {

    /**
     * @param k
     * @return the value to which the specified key is mapped or {@code null} if
     *         it doesn't exist in the Source
     */
    DefaultRow<V> getRow(final K k);

    /**
     * @return all rows in the dataSource
     */
    Collection<DefaultRow<V>> getRows();

    /**
     * @return ViewLiveData object that contains the demanded data and
     *         absoluteRowCount
     */
    ViewLiveData<V> getRows(final int dataSrcRowIndex, final int dataSize);

    /**
     * Returns the total number of rows that could be drawn in the view after
     * applying filters
     */
    int getRowCount();

    /**
     * Insert or replace the value in the source
     */
    Interval setData(final V v);

    /**
     * Update an existing value identified by key {@code k}, if it is present,
     * using the {@code updater}
     */
    Interval updateData(final K k, final Consumer<V> updater);

    /**
     * Removes data from the source
     */
    V removeData(final K k);

    /**
     * @return the list that contains selected data
     */
    List<DefaultRow<V>> getLiveSelectedData();

    /**
     * Adapter setter
     */
    void setAdapter(DataGridAdapter<K, V> adapter);

    /**
     * RenderingHelpersCache setter
     */
    void setRenderingHelpersCache(final RenderingHelpersCache<V> renderingHelpersCache);

    /**
     * Add a sort
     */
    void addSort(final Column<V> column, final DefaultDataGridController<K, V>.ColumnControllerSort colSort, final boolean asc);

    void addSort(Object key, Comparator<DefaultRow<V>> comparator);

    /**
     * Remove a sort
     */
    Comparator<DefaultRow<V>> clearSort(Column<V> column);

    Comparator<DefaultRow<V>> clearSort(Object key);

    /**
     * Clear all sorting
     */
    void clearSorts();

    /**
     * Sort the data
     */
    void sort();

    /**
     * Get sorts entry set
     */
    Set<Entry<Object, Comparator<DefaultRow<V>>>> getSortsEntry();

    /**
     * Set a filter
     */
    void setFilter(final Object key, String id, final boolean reinforcing, final AbstractFilter<V> filter);

    /**
     * Clear a filter
     */
    AbstractFilter<V> clearFilter(Object key);

    void clearFilters(ColumnDefinition<V> column);

    /**
     * Clear all filters
     */
    void clearFilters();

    /**
     * Performs the given action for each entry in the source until all entries
     * have been processed or the action throws an exception. There is no
     * guarantee on the order of elements
     */
    void forEach(BiConsumer<K, V> action);

    /**
     * Clears and resets liveData content
     */
    void resetLiveData();

    /**
     * Selects a row
     */
    void select(final K k);

    /**
     * Unselects a row
     */
    void unselect(final K k);

    /**
     * Selects all rows in the dataGrid
     */
    void selectAllLiveData();

    /**
     * Unselects all rows in the dataGrid
     */
    void unselectAllData();

    /**
     * @return if a row is selcted or not
     */
    boolean isSelected(final K k);
}
