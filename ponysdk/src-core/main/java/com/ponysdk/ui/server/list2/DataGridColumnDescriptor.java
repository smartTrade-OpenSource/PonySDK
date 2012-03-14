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

package com.ponysdk.ui.server.list2;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.list.renderer.cell.CellRenderer;
import com.ponysdk.ui.server.list.valueprovider.ValueProvider;
import com.ponysdk.ui.server.list2.header.HeaderCellRenderer;

public class DataGridColumnDescriptor<D, V> {

    private HeaderCellRenderer headerCellRenderer;
    private CellRenderer<D, V> cellRenderer;
    private CellRenderer<D, V> subCellRenderer;
    private ValueProvider<D, V> valueProvider;

    public void setHeaderCellRenderer(final HeaderCellRenderer headerCellRender) {
        this.headerCellRenderer = headerCellRender;
    }

    public HeaderCellRenderer getHeaderCellRenderer() {
        return headerCellRenderer;
    }

    public void setCellRenderer(final CellRenderer<D, V> cellRenderer) {
        this.cellRenderer = cellRenderer;
    }

    public CellRenderer<D, V> getCellRenderer() {
        return cellRenderer;
    }

    public void setValueProvider(final ValueProvider<D, V> valueProvider) {
        this.valueProvider = valueProvider;
    }

    public ValueProvider<D, V> getValueProvider() {
        return valueProvider;
    }

    public CellRenderer<D, V> getSubCellRenderer() {
        return subCellRenderer;
    }

    public void setSubCellRenderer(final CellRenderer<D, V> subCellRenderer) {
        this.subCellRenderer = subCellRenderer;
    }

    public IsPWidget renderCell(final int row, final D data) {
        if (cellRenderer == null) throw new IllegalArgumentException("CellRenderer is required");
        if (valueProvider == null) throw new IllegalArgumentException("ValueProvider is required");
        return cellRenderer.render(row, data, valueProvider.getValue(data));
    }

    public IsPWidget renderSubCell(final int row, final D data) {
        if (subCellRenderer == null) throw new IllegalArgumentException("SubCellRenderer is required");
        if (valueProvider == null) throw new IllegalArgumentException("ValueProvider is required");
        return subCellRenderer.render(row, data, valueProvider.getValue(data));
    }
}
