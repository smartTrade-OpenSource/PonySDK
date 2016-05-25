/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.ui.server.celltable;

import java.util.Iterator;

import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.list.SimpleListView;

public class SimpleTableView extends SimpleTable implements SimpleListView {

    public SimpleTableView() {
        addStyleName("pony-PFlexTable");
        addStyleName("pony-SimpleList");
    }

    @Override
    public void clearList() {
        clear(0);
    }

    @Override
    public void clear(final int from) {
        super.clear(from);
    }

    @Override
    public void insertRow(final int row) {
        insert(new PElement("tr"), row);
    }

    @Override
    public void setColumns(final int size) {
        throw new IllegalAccessError("Not implemented");
    }

    @Override
    public void selectRow(final int row) {
        throw new IllegalAccessError("Not implemented");
    }

    @Override
    public void unSelectRow(final int row) {
        throw new IllegalAccessError("Not implemented");
    }

    @Override
    public void addRowStyle(final int row, final String styleName) {
        getRow(row).addStyleName(styleName);
    }

    @Override
    public void removeRowStyle(final int row, final String styleName) {
        getRow(row).removeStyleName(styleName);
    }

    @Override
    public void addColumnStyle(final int column, final String styleName) {
        final Iterator<PElement> iterator = getColumnIterator(column);
        while (iterator.hasNext()) {
            final PElement e = iterator.next();
            e.addStyleName(styleName);
        }
    }

    @Override
    public void removeColumnStyle(final int column, final String styleName) {
        final Iterator<PElement> iterator = getColumnIterator(column);
        while (iterator.hasNext()) {
            final PElement e = iterator.next();
            e.removeStyleName(styleName);
        }
    }

    @Override
    public void setColumnWidth(final int column, final String width) {
        final PElement cell = getCell(0, column);
        cell.setStyleProperty("width", width);
    }

    @Override
    public void addHeaderStyle(final String styleName) {
        getRow(0).addStyleName(styleName);
    }

    @Override
    public void addCellStyle(final int row, final int col, final String styleName) {
        getCell(row, col).addStyleName(styleName);
    }

    @Override
    public void removeCellStyle(final int row, final int col, final String styleName) {
        getCell(row, col).removeStyleName(styleName);
    }

}
