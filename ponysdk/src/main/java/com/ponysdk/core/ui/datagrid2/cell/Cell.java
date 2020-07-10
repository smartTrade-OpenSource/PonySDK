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
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;

/**
 * {@code Cell} is a widget that will be part of a {@code DataGridView} column
 * body. It can be recycled to be used with different model values.<br/>
 * The {@link IsPWidget#asWidget()} method must always return the same instance
 * of the main widget that will be used for the rendering of the value.
 *
 * @author mbagdouri
 */
public interface Cell<V> extends IsPWidget {

    /**
     * Must always return the same instance and cannot be {@code null} or the
     * same as the main widget.
     *
     * @return a widget that will replace the main widget when the value for
     *         this cell is not available
     */
    PWidget asPendingWidget();

    /**
     * Renders the value in the main widget.
     *
     * @param data the value to be rendered
     * @param renderingHelper the intermediate object supplied in
     *            {@link ColumnDefinition#getRenderingHelper(Object)}
     */
    void render(V data, Object renderingHelper);

    /**
     * Sets a {@link CellController} that can be used to make cell/row related
     * actions. It will be set as soon as the
     * {@link ColumnDefinition#createCell()} is called.
     *
     * @param cellController
     */
    void setController(CellController<V> cellController);

    /**
     * Called when the row that this cell belongs to is selected
     */
    void select();

    /**
     * Called when the row that this cell belongs to is unselected
     */
    void unselect();
}