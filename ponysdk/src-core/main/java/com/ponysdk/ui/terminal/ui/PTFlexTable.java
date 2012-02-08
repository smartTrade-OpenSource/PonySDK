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
        init(new com.google.gwt.user.client.ui.FlexTable());
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();

        if (PropertyKey.CLEAR_ROW.equals(propertyKey)) {
            cast().removeRow(property.getIntValue());
        } else if (PropertyKey.INSERT_ROW.equals(propertyKey)) {
            cast().insertRow(property.getIntValue());
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
