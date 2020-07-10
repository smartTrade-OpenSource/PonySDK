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

package com.ponysdk.core.ui.datagrid2.adapter;

import java.util.List;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.datagrid2.column.ColumnController;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.view.DataGridView;

/**
 * {@code DataGridAdapter} is used to set up and initialize the
 * {@link DataGridView}.
 *
 * @see DataGridView#setAdapter(DataGridAdapter)
 * @author mbagdouri
 */
public interface DataGridAdapter<K, V> {

    /**
     * Must always return the same immutable instance.
     *
     * @return the list of column definitions for this adapter.
     */
    List<ColumnDefinition<V>> getColumnDefinitions();

    /**
     * Defines how to retrieve the key from the value. Must always return the
     * same key for the same instance of the value.
     */
    K getKey(V v);

    /**
     * When two values are considered equal regarding all other criteria of
     * comparison, they will be compared according to a last criterion, which is
     * the order of insertion in the model. When we resort to this criterion,
     * this method determines whether data will be sorted in ascending or
     * descending (insertion) order.
     *
     * @see #compareDefault(Object, Object)
     * @see DataGridView#addSort(Object, java.util.Comparator)
     * @see ColumnController#sort(boolean)
     */
    boolean isAscendingSortByInsertionOrder();

    /**
     * Returns a negative integer, zero, or a positive integer as {@code v1} is
     * less than, equal to, or greater than {@code v2}.<br/>
     * Returns always zero if there is no default criterion of comparison.
     */
    int compareDefault(V v1, V v2);

    /**
     * If {@link #hasHeader()} is {@code true}, this method is called when the
     * header row widget of the {@link DataGridView} is created
     */
    void onCreateHeaderRow(IsPWidget rowWidget);

    /**
     * If {@link #hasFooter()} is {@code true}, this method is called when the
     * footer row widget of the {@link DataGridView} is created
     */
    void onCreateFooterRow(IsPWidget rowWidget);

    /**
     * Called when a row widget is created in the {@link DataGridView}
     */
    void onCreateRow(IsPWidget rowWidget);

    /**
     * Called when a row is selected in the {@link DataGridView}
     */
    void onSelectRow(IsPWidget rowWidget);

    /**
     * Called when a row is unselected in the {@link DataGridView}
     */
    void onUnselectRow(IsPWidget rowWidget);

    /**
     * Called when a column resizer is created in the {@link DataGridView}
     */
    void onCreateColumnResizer(IsPWidget resizer);

    /**
     * @return the widget that will cover the {@link DataGridView} when it is
     *         loading data (cannot be {@code null})
     */
    IsPWidget createLoadingDataWidget();

    /**
     * <i>N.B. The columns of a {@link DataGridView} without a header cannot be
     * resized nor dragged</i>
     *
     * @return {@code true} if the {@link DataGridView} must have a header,
     *         {@code false} otherwise
     */
    boolean hasHeader();

    /**
     * @return {@code true} if the {@link DataGridView} must have a footer,
     *         {@code false} otherwise
     */
    boolean hasFooter();
}