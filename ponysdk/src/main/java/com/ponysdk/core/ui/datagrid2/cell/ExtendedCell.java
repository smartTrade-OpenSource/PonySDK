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

package com.ponysdk.core.ui.datagrid2.cell;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;

/**
 * {@code ExtendedCell} is created when a {@link Cell} is switched to mode
 * extended. {@code ExtendedCell} will be always bound to the same key, but its
 * data might change in case the model is updated.<br/>
 * The {@link IsPWidget#asWidget()} method must always return the same instance
 * of the main widget that will be used for the rendering of the value.
 *
 * @author mbagdouri
 * @see CellController#extendedMode(ExtendedCell)
 */
public interface ExtendedCell<V> extends IsPWidget {

    /**
     * Sets the most recent value on this cell. This value can be the same as
     * the value previously set (i.e. when this method is called, it is not
     * guaranteed that the value has changed)
     */
    void setValue(V v);

    /**
     * Sets a {@link ExtendedCellController} that can be used to make cell/row
     * related actions. It will be set as soon as the
     * {@link ColumnDefinition#createCell()} is called.
     *
     * @param cellController
     */
    void setController(ExtendedCellController<V> extendedCellController);

    /**
     * Called when the row that this cell belongs to is selected
     */
    void select();

    /**
     * Called when the row that this cell belongs to is unselected
     */
    void unselect();

    /**
     * Called before each time this cell is removed from its parent
     */
    void beforeRemove();

    /**
     * Called after each time this cell is added to a parent widget
     */
    void afterAdd();
}