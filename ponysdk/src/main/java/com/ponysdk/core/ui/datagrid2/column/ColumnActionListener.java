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

package com.ponysdk.core.ui.datagrid2.column;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

/**
 * @author mbagdouri
 */
public interface ColumnActionListener<V> {

    /**
     * Called when a sorting criterion for this column is added/replaced
     *
     * @see ColumnController#sort(boolean)
     */
    void onSort(boolean asc);

    /**
     * Called when a sorting criterion for this column is cancelled
     *
     * @see ColumnController#clearSort()
     */
    void onClearSort();

    /**
     * Called when this column is filtered
     *
     * @see ColumnController#filter(Object, BiPredicate, boolean)
     */
    void onFilter(Object key, BiPredicate<V, Supplier<Object>> filter, boolean reinforcing);

    /**
     * Called when this column's filter with {@code key} is cancelled
     *
     * @see ColumnController#clearFilter(Object)
     */
    void onClearFilter(Object key);

    /**
     * Called when all this column filters are cleared
     *
     * @see ColumnController#clearFilters()
     */
    void onClearFilters();

    /**
     * Called when this column is redrawn
     *
     * @see ColumnController#redraw(boolean)
     */
    void onRedraw(boolean clearRenderingHelpers);

    /**
     * Called when this column's state is changed
     *
     * @see ColumnController#setState(com.ponysdk.core.ui.datagrid2.column.ColumnDefinition.State)
     */
    void onStateChanged(ColumnDefinition.State state);

    /**
     * Called when this column's width is changed
     */
    void onResized(int width);
}