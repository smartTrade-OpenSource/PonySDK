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

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PFlexTable;

public class DefaultSimpleListView extends PFlexTable implements SimpleListView {

    public DefaultSimpleListView() {
        super();
        setWidth("98%");
    }

    @Override
    public void addWidget(final IsPWidget widget, final int column, final int row) {
        setWidget(row, column, widget.asWidget());
    }

    @Override
    public void clearList() {
        final int rowCount = getRowCount();
        for (int i = rowCount; i > 0; i--) {
            removeRow(i);
        }
    }

    @Override
    public void selectRow(int row) {
        getRowFormatter().addStyleName(row, PonySDKTheme.SIMPLELIST_SELECTEDROW);
    }

    @Override
    public void unSelectRow(int row) {
        getRowFormatter().removeStyleName(row, PonySDKTheme.SIMPLELIST_SELECTEDROW);
    }

    @Override
    public void setColumns(int size) {

    }

    @Override
    public void addRowStyle(int row, String styleName) {
        getRowFormatter().addStyleName(row, styleName);
    }

    @Override
    public void addHeaderStyle(String styleName) {
        getRowFormatter().addStyleName(0, styleName);
    }

    @Override
    public void addCellStyle(int row, int col, String styleName) {
        getCellFormatter().addStyleName(row, col, styleName);

    }

    @Override
    public void removeCellStyle(int row, int column, String styleName) {
        getCellFormatter().removeStyleName(row, column, styleName);
    }

}
