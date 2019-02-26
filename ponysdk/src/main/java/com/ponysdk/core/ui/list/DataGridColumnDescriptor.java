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

package com.ponysdk.core.ui.list;

import java.util.function.Function;

import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.list.renderer.cell.CellRenderer;
import com.ponysdk.core.ui.list.renderer.header.HeaderCellRenderer;

public class DataGridColumnDescriptor<D, V> {

    protected HeaderCellRenderer headerCellRenderer;
    protected CellRenderer<V, ? extends IsPWidget> cellRenderer;
    protected CellRenderer<V, ? extends IsPWidget> subCellRenderer;
    protected Function<D, V> valueProvider;

    public HeaderCellRenderer getHeaderCellRenderer() {
        return headerCellRenderer;
    }

    public void setHeaderCellRenderer(final HeaderCellRenderer headerCellRender) {
        this.headerCellRenderer = headerCellRender;
    }

    public CellRenderer<V, ? extends IsPWidget> getCellRenderer() {
        return cellRenderer;
    }

    public void setCellRenderer(final CellRenderer<V, ? extends IsPWidget> cellRenderer) {
        this.cellRenderer = cellRenderer;
    }

    public Function<D, V> getValueProvider() {
        return valueProvider;
    }

    public void setValueProvider(final Function<D, V> valueProvider) {
        this.valueProvider = valueProvider;
    }

    public CellRenderer<V, ? extends IsPWidget> getSubCellRenderer() {
        return subCellRenderer;
    }

    public void setSubCellRenderer(final CellRenderer<V, ? extends IsPWidget> subCellRenderer) {
        this.subCellRenderer = subCellRenderer;
    }

    public IsPWidget renderCell(final int row, final D data) {
        if (cellRenderer == null) throw new IllegalArgumentException("CellRenderer is required");
        if (valueProvider == null) throw new IllegalArgumentException("ValueProvider is required");
        return cellRenderer.render(row, valueProvider.apply(data));
    }

    public IsPWidget renderSubCell(final int row, final D data) {
        if (subCellRenderer == null) throw new IllegalArgumentException("SubCellRenderer is required");
        if (valueProvider == null) throw new IllegalArgumentException("ValueProvider is required");
        return subCellRenderer.render(row, valueProvider.apply(data));
    }

}
