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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.list.renderer.CellRenderer;
import com.ponysdk.ui.server.list.renderer.HeaderCellRenderer;
import com.ponysdk.ui.server.list.renderer.ObjectCellRenderer;
import com.ponysdk.ui.server.list.renderer.StringHeaderCellRenderer;
import com.ponysdk.ui.server.list.valueprovider.ValueProvider;

public class ListColumnDescriptor<D, V> {

    private static final Logger log = LoggerFactory.getLogger(ListColumnDescriptor.class);

    private String caption;

    private HeaderCellRenderer headerCellRenderer;
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private CellRenderer<D, V> cellRenderer = new ObjectCellRenderer();
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private CellRenderer<D, V> subCellRenderer = new ObjectCellRenderer();
    private ValueProvider<D, V> valueProvider;

    public ListColumnDescriptor() {
    }

    public ListColumnDescriptor(String caption) {
        this.caption = caption;
    }

    public IsPWidget renderCell(int row, D data) {
        return renderCell(row, data, cellRenderer);
    }

    public IsPWidget renderSubCell(int row, D data) {
        return renderCell(row, data, subCellRenderer);
    }

    private IsPWidget renderCell(int row, D data, CellRenderer<D, V> renderer) {
        V value;
        try {
            value = valueProvider.getValue(data);
        } catch (final Exception e) {
            log.error("cannot get value", e);
            return new PLabel("failed");
        }
        return renderer.render(row, data, value);
    }

    public IsPWidget renderHeader() {
        if (headerCellRenderer == null)
            headerCellRenderer = new StringHeaderCellRenderer(caption);
        return headerCellRenderer.render();
    }

    public void setHeaderCellRenderer(HeaderCellRenderer headerCellRender) {
        this.headerCellRenderer = headerCellRender;
    }

    public void setCellRenderer(CellRenderer<D, V> cellRenderer) {
        if (cellRenderer == null)
            throw new RuntimeException("cellRender cannot be null");
        this.cellRenderer = cellRenderer;
    }

    public void setValueProvider(ValueProvider<D, V> valueProvider) {
        this.valueProvider = valueProvider;
    }

    public HeaderCellRenderer getHeaderCellRenderer() {
        return headerCellRenderer;
    }

    public CellRenderer<D, V> getCellRenderer() {
        return cellRenderer;
    }

    public CellRenderer<D, V> getSubCellRenderer() {
        return subCellRenderer;
    }

    public void setSubCellRenderer(CellRenderer<D, V> subCellRenderer) {
        this.subCellRenderer = subCellRenderer;
    }

}
