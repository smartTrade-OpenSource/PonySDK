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
import com.ponysdk.core.ui.datagrid2.controller.SimpleDataGridController;
import com.ponysdk.core.ui.datagrid2.controller.SimpleDataGridController.RenderingHelpersCache;
import com.ponysdk.core.ui.datagrid2.data.AbstractFilter;
import com.ponysdk.core.ui.datagrid2.data.Interval;
import com.ponysdk.core.ui.datagrid2.data.Row;

/**
 *
 */
public interface DataGridSource<K, V> {

    /**
     * @param k
     * @return the value to which the specified key is mapped
     *         or {@code null} if it doesn't exist in the dataSource
     */
    Row<V> getRow(final K k);

    /**
     * @return all rows in the dataSource
     */
    Collection<Row<V>> getRows();

    /**
     * @return the needed rows for the view
     */
    List<Row<V>> getRows(int index, int size);

    /**
     * Returns the total number of rows that could be drawn in the view
     * after applying filters
     */
    int getRowCount();

    /**
     * Returns liveSelecteData
     */
    List<Row<V>> getLiveSelectedData();

    /**
     * Adapter setter
     */
    void setAdapter(DataGridAdapter<K, V> adapter);

    /**
     * RenderingHelpersCache setter
     */
    void setRenderingHelpersCache(final RenderingHelpersCache<V> renderingHelpersCache);

    /**
     * Sorts our data when necessary
     */
    void sort();

    /**
     * Adds a sort
     */
    void addSort(final Column<V> column, final SimpleDataGridController<K, V>.ColumnControllerSort colSort, final boolean asc);

    void addSort(Object key, Comparator<Row<V>> comparator);

    /**
     * Removes a sort
     */
    Comparator<Row<V>> clearSort(Column<V> column);

    Comparator<Row<V>> clearSort(Object key);

    /**
     * Clears all sorting
     */
    void clearSorts();

    /**
     * Get sorts entry set
     */
    Set<Entry<Object, Comparator<Row<V>>>> getSortsEntry();

    /**
     * Sets a filter
     */
    void setFilter(final Object key, String id, final boolean reinforcing, final AbstractFilter<V> filter);

    /**
     * Clears a filter
     */
    AbstractFilter<V> clearFilter(Object key);

    void clearFilters(ColumnDefinition<V> column);

    /**
     * Clears all filters
     */
    void clearFilters();

    /**
     * Performs the given action for each entry in this map until all entries
     * have been processed or the action throws an exception. There is no guarantee on the order of elements
     */
    void forEach(BiConsumer<K, V> action);

    /**
     * Insert or updates data in cash and liveData
     */
    Interval setData(final V v);

    /**
     * Updates data in cash and liveData
     */
    Interval updateData(final K k, final Consumer<V> updater);

    /**
     * Removes data from cash and liveData
     */
    V removeData(final K k);

    /**
     * Clears and resets liveData content
     */
    void resetLiveData();

    /**
     * Selects a row from the view
     */
    void select(final K k);

    /**
     * Unselects a row from the view
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
     * Returns if a row is selcted or not
     */
    boolean isSelected(final K k);
}
