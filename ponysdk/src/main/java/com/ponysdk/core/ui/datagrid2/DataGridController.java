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

import java.util.Collection;
import java.util.Comparator;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author mbagdouri
 */
interface DataGridController<K, V> {

    void renderCell(ColumnDefinition<V> column, int row, Cell<V> widget);

    void setValueOnExtendedCell(int row, ExtendedCell<V> widget);

    V getRowData(int row);

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

    DataGridModel<K, V> getModel();

    void setListener(DataGridControllerListener<V> listener);

    void clearRenderingHelpers(ColumnDefinition<V> column);

    Collection<V> getLiveData();

    int getRowCount();

    void enrichConfigBuilder(DataGridConfigBuilder<V> builder);

    void prepareLiveDataOnScreen(int rowIndex, int size, boolean isHorizontalScroll);
}
