/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.ui.server.list.refreshable;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.list.DataGridColumnDescriptor;
import com.ponysdk.ui.server.list.renderer.cell.CellRenderer;

public class RefreshableDataGridColumnDescriptor<D, V, W extends IsPWidget> extends DataGridColumnDescriptor<D, V> {

    @Override
    public void setCellRenderer(final CellRenderer<V> cellRenderer) {
        throw new IllegalArgumentException("use RefreshableCellRenderer instead of CellRenderer");
    }

    public void setCellRenderer(final RefreshableCellRenderer<V, W> cellRenderer) {
        super.setCellRenderer(cellRenderer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public W renderCell(final int row, final D data) {
        return (W) super.renderCell(row, data);
    }

    @SuppressWarnings("unchecked")
    @Override
    public RefreshableCellRenderer<V, W> getCellRenderer() {
        return (RefreshableCellRenderer<V, W>) cellRenderer;
    }

}