/*
 * Copyright (c) 2019 PonySDK Owners: Luciano Broussal <luciano.broussal AT
 * gmail.com> Mathieu Barbier <mathieu.barbier AT gmail.com> Nicolas Ciaravola
 * <nicolas.ciaravola.pro AT gmail.com>
 *
 * WebSite: http://code.google.com/p/pony-sdk/
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

package com.ponysdk.core.ui.datagrid2.data;

import com.ponysdk.core.ui.basic.IsPWidget;

/**
 * An action that will be applied to all rows that meet the condition of the
 * predicate on the value and the index
 *
 * @author mbagdouri
 */
public interface RowAction<V> {

    /**
     * @param value the value associated to the row
     * @param index the index of the row in the table
     * @return {@code true} if the action is to be applied on the row,
     *         {@code false} otherwise
     */
    boolean testRow(V value, int index);

    /**
     * Applies an action on the {@code row} widget.<br/>
     * <b>MUST NOT modify the dimensions (width or height) of the row since they
     * can be dynamically calculated.</b>
     */
    void apply(IsPWidget row);

    /**
     * Reverts the changes applied by {@link #apply(IsPWidget)} on the
     * {@code row} widget.<br/>
     */
    void cancel(IsPWidget row);

    /**
     * Returns a boolean that determines if an action is applied for a row
     */
    boolean isActionApplied(final IsPWidget row);
}
