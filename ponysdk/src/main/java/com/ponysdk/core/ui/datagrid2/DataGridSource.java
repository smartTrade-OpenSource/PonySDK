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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Column;
import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.ColumnControllerSort;
import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Interval;
import com.ponysdk.core.ui.datagrid2.SimpleDataGridController.Row;

/**
 *
 */
public interface DataGridSource<K, V> {

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this model contains no mapping for the key.
     */
    Row<V> getRow(final K k);

    /**
     * Insert or replace the value
     */
    void putRow(final K k, final Row<V> row);

    /**
     * Removes the value identified by the key {@code k} from this model if it is present
     */
    Row<V> removeRow(final K k);

    /**
     * Returns all values
     */
    Collection<Row<V>> getRows();

    /**
     * Returns all rows rows needed for the scroll
     */
    //    List<V> getNeededRowsForScroll(int index, int size);
    List<Row<V>> getRows(int index, int size);

    /**
     * Returns the first page of data for the new sort
     */
    //    List<V> getNewSortData();

    /**
     * Returns filtered number of row that could be drawn
     */
    int getRowCount();

    /**
     * Resets liveData and draw data corresponding to sort
     */
    void sort();

    /**
     * Adapter setter
     */
    void setAdapter(DataGridAdapter<K, V> adapter);

    /**
     * Adds a sort
     */
    void addSort(final Column<V> column, final ColumnControllerSort colSort, final boolean asc);

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
     * Get sorts values
     */
    Collection<Comparator<Row<V>>> getSorts();

    /**
     * Get sorts entry set
     */
    Set<Entry<Object, Comparator<Row<V>>>> getSortsEntry();

    /**
     * Sets a filter
     */
    void setFilter(final Object key, final boolean reinforcing, final AbstractFilter<V> filter);

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
     * Get filters values
     */
    Collection<AbstractFilter<V>> getFilters();

    /**
     * Put a filter in filters
     */
    public AbstractFilter<V> putFilter(final Object key, final AbstractFilter<V> filter);

    /**
     * Performs the given action for each entry in this map until all entries
     * have been processed or the action throws an exception. There is no guarantee on the order of elements
     */
    void forEach(BiConsumer<K, V> action);

    /**
     * Returns data to be added to liveSelectedData
     */
    List<V> onSelectAllLiveData();

    /**
     * Insert or updates data in cache
     */
    Interval setData(final V v);

    Interval updateData(final K k, final Consumer<V> updater);

    void resetLiveData();

    V removeData(final K k);

    //    V getRowData(final int row);

    boolean isSelected(final K k);

    void select(final K k);

    void unselect(final K k);

    void selectAllLiveData();

    void unselectAllData();
}
