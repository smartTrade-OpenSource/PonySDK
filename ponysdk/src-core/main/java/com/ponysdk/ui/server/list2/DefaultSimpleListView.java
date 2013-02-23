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

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PFlexTable;

public class DefaultSimpleListView extends PFlexTable implements SimpleListView {

    public DefaultSimpleListView() {
        super();
        addStyleName(PonySDKTheme.SIMPLELIST);
    }

    @Override
    public void addWidget(final IsPWidget widget, final int column, final int row) {
        setWidget(row, column, widget.asWidget());
    }

    @Override
    public void clearList() {
        clear(0);
    }

    @Override
    public void clear(final int from) {
        final int rowCount = getRowCount();
        for (int i = rowCount; i >= from; i--) {
            removeRow(i);
        }
    }

    @Override
    public void selectRow(final int row) {
        getRowFormatter().addStyleName(row + 1, PonySDKTheme.SIMPLELIST_SELECTEDROW);
    }

    @Override
    public void unSelectRow(final int row) {
        getRowFormatter().removeStyleName(row + 1, PonySDKTheme.SIMPLELIST_SELECTEDROW);
    }

    @Override
    public void setColumns(final int size) {

    }

    @Override
    public void addRowStyle(final int row, final String styleName) {
        getRowFormatter().addStyleName(row, styleName);
    }

    @Override
    public void removeRowStyle(final int row, final String styleName) {
        getRowFormatter().removeStyleName(row, styleName);
    }

    @Override
    public void addHeaderStyle(final String styleName) {
        getRowFormatter().addStyleName(0, styleName);
    }

    @Override
    public void addCellStyle(final int row, final int col, final String styleName) {
        getCellFormatter().addStyleName(row, col, styleName);

    }

    @Override
    public void removeCellStyle(final int row, final int column, final String styleName) {
        getCellFormatter().removeStyleName(row, column, styleName);
    }

    @Override
    public void addColumnStyle(final int column, final String styleName) {
        getColumnFormatter().addStyleName(column, styleName);
    }

    @Override
    public void removeColumnStyle(final int column, final String styleName) {
        getColumnFormatter().removeStyleName(column, styleName);
    }

    @Override
    public void setColumnWidth(final int column, final String width) {
        getColumnFormatter().setWidth(column, width);
    }

}
