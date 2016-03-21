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

package com.ponysdk.ui.server.list;

import com.google.gwt.user.cellview.client.DataGrid;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.list.renderer.header.HeaderCellRenderer;

/**
 * Defines a {@link DataGrid} column
 * 
 * @param <D>
 */
public class ColumnDescriptor<D, W extends IsPWidget> {

    protected HeaderCellRenderer headerCellRenderer;
    protected CellRenderer<D, W> cellRenderer;

    public void setHeaderCellRenderer(final HeaderCellRenderer headerCellRender) {
        this.headerCellRenderer = headerCellRender;
    }

    public HeaderCellRenderer getHeaderCellRenderer() {
        return headerCellRenderer;
    }

    public void setCellRenderer(final CellRenderer<D, W> cellRenderer) {
        this.cellRenderer = cellRenderer;
    }

    public CellRenderer<D, W> getCellRenderer() {
        return cellRenderer;
    }

    public W renderCell(final D data) {
        return cellRenderer.render(data);
    }
}