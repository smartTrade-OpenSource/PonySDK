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

package com.ponysdk.ui.terminal.ui;

import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.Update;

public class PTFlexTable extends PTHTMLTable {

    @Override
    public void create(final Create create, final UIService uiService) {
        init(create, uiService, new com.google.gwt.user.client.ui.FlexTable());
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();

        if (PropertyKey.CLEAR.equals(propertyKey)) {
            cast().clear();
        } else if (PropertyKey.CLEAR_ROW.equals(propertyKey)) {
            cast().removeRow(property.getIntValue());
        } else if (PropertyKey.INSERT_ROW.equals(propertyKey)) {
            cast().insertRow(property.getIntValue());
        } else if (PropertyKey.HTMLTABLE_ROW_STYLE.equals(propertyKey)) {
            final int row = property.getChildProperty(PropertyKey.ROW).getIntValue();
            final Property addProperty = property.getChildProperty(PropertyKey.ROW_FORMATTER_ADD_STYLE_NAME);
            final Property removeProperty = property.getChildProperty(PropertyKey.ROW_FORMATTER_REMOVE_STYLE_NAME);
            if (addProperty != null) {
                cast().getRowFormatter().addStyleName(row, addProperty.getValue());
            } else {
                cast().getRowFormatter().removeStyleName(row, removeProperty.getValue());
            }
        } else if (PropertyKey.HTMLTABLE_CELL_STYLE.equals(propertyKey)) {
            final int cellRow = property.getChildProperty(PropertyKey.ROW).getIntValue();
            final int cellColumn = property.getChildProperty(PropertyKey.COLUMN).getIntValue();
            final Property addCellProperty = property.getChildProperty(PropertyKey.CELL_FORMATTER_ADD_STYLE_NAME);
            final Property removeCellProperty = property.getChildProperty(PropertyKey.CELL_FORMATTER_REMOVE_STYLE_NAME);
            if (addCellProperty != null) {
                cast().getCellFormatter().addStyleName(cellRow, cellColumn, addCellProperty.getValue());
            } else {
                cast().getCellFormatter().removeStyleName(cellRow, cellColumn, removeCellProperty.getValue());
            }
        } else if (PropertyKey.FLEXTABLE_CELL_FORMATTER.equals(propertyKey)) {
            final int cellFormatterRow = property.getChildProperty(PropertyKey.ROW).getIntValue();
            final int cellFormatterColumn = property.getChildProperty(PropertyKey.COLUMN).getIntValue();
            final Property colSpanProperty = property.getChildProperty(PropertyKey.SET_COL_SPAN);
            final Property rowSpanProperty = property.getChildProperty(PropertyKey.SET_ROW_SPAN);
            if (colSpanProperty != null) {
                cast().getFlexCellFormatter().setColSpan(cellFormatterRow, cellFormatterColumn, colSpanProperty.getIntValue());
            } else if (rowSpanProperty != null) {
                cast().getFlexCellFormatter().setRowSpan(cellFormatterRow, cellFormatterColumn, rowSpanProperty.getIntValue());
            }
        } else {
            super.update(update, uiService);
        }

    }

    @Override
    public com.google.gwt.user.client.ui.FlexTable cast() {
        return (com.google.gwt.user.client.ui.FlexTable) uiObject;
    }
}
