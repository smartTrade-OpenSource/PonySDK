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

public interface SimpleListView extends IsPWidget {

    void clearList();

    void addWidget(IsPWidget widget, int column, int row, int colspan);

    void insertRow(int row);

    void removeRow(int row);

    void setColumns(int size);

    void selectRow(int row);

    void unSelectRow(int row);

    void addRowStyle(int row, String styleName);

    void removeRowStyle(int row, String styleName);

    void addColumnStyle(int column, String styleName);

    void removeColumnStyle(int column, String styleName);

    void setColumnWidth(int column, String width);

    void addHeaderStyle(String styleName);

    void addCellStyle(int row, int col, String styleName);

    void removeCellStyle(int row, int column, String styleName);

    void moveRow(final int index, final int beforeIndex);

    void moveColumn(final int index, final int beforeIndex);

    void clear(int from);

}
