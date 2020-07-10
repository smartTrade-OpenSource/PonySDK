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
public interface ColumnController<V> {

    /**
     * Adds/replaces a sorting criterion on the view based on this column
     *
     * @param asc
     */
    void sort(boolean asc);

    /**
     * Cancels the sorting criterion of this column from the view
     */
    void clearSort();

    /**
     * Adds/replaces a filter that accepts only data that meet the condition of
     * the {@link BiPredicate} {@code filter}.
     *
     * @param key an object that can be used to uniquely identify a
     *            filter, so that it can be replaced or removed
     * @param filter a {@code BiPredicate} that decides whether a value is
     *            accepted or filtered. The {@code BiPredicate}
     *            arguments are the model value and a {@code Supplier}
     *            that provides the rendering helper that was supplied
     *            by {@link ColumnDefinition#getRenderingHelper(Object)}
     * @param reinforcing {@code true} if the predicate is at least as
     *            intolerant as the replaced predicate of the same key
     *            (i.e. the predicate doesn't accept any value that was
     *            not accepted by the replaced predicate), {@code false}
     *            otherwise. This is an optimization that allows us to
     *            avoid applying the predicate on values that we already
     *            know will not be accepted. If this filter is not
     *            replacing an existing one, the value of the
     *            {@code reinforcing} argument has no impact.
     */
    void filter(Object key, BiPredicate<V, Supplier<Object>> filter, boolean reinforcing);

    /**
     * Cancels the filter, corresponding to {@code key}, from the view
     */
    void clearFilter(Object key);

    /**
     * Cancels all filters, corresponding to this column, from the view
     */
    void clearFilters();

    /**
     * Redraws all the cells of this column
     *
     * @param clearRenderingHelpers {@code true} if all the rendering helpers
     *            for this column must be cleared and thus
     *            recalculated before drawing, {@code false}
     *            otherwise
     */
    void redraw(boolean clearRenderingHelpers);

    /**
     * Changes the current state of this column
     */
    void setState(ColumnDefinition.State state);
}