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

import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTHTMLTable extends PTPanel<HTMLTable> {

    @Override
    public void add(final PTInstruction add, final UIService uiService) {
        final Widget w = asWidget(add.getObjectID(), uiService);

        final int row = add.getInt(PROPERTY.ROW);
        final int cell = add.getInt(PROPERTY.CELL);

        uiObject.getCellFormatter().addStyleName(row, cell, "pony-PFlextable-Cell");
        uiObject.setWidget(row, cell, w);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.CLEAR)) {
            uiObject.clear();
        } else if (update.containsKey(PROPERTY.BORDER_WIDTH)) {
            uiObject.setBorderWidth(update.getInt(PROPERTY.BORDER_WIDTH));
        } else if (update.containsKey(PROPERTY.CELL_SPACING)) {
            uiObject.setCellSpacing(update.getInt(PROPERTY.CELL_SPACING));
        } else if (update.containsKey(PROPERTY.CELL_PADDING)) {
            uiObject.setCellPadding(update.getInt(PROPERTY.CELL_PADDING));
        } else if (update.containsKey(PROPERTY.HTMLTABLE_ROW_STYLE)) {
            final int row = update.getInt(PROPERTY.ROW);
            if (update.containsKey(PROPERTY.ROW_FORMATTER_ADD_STYLE_NAME)) {
                uiObject.getRowFormatter().addStyleName(row, update.getString(PROPERTY.ROW_FORMATTER_ADD_STYLE_NAME));
            } else {
                uiObject.getRowFormatter().removeStyleName(row, update.getString(PROPERTY.ROW_FORMATTER_REMOVE_STYLE_NAME));
            }
        } else if (update.containsKey(PROPERTY.HTMLTABLE_CELL_STYLE)) {
            final int cellRow = update.getInt(PROPERTY.ROW);
            final int cellColumn = update.getInt(PROPERTY.COLUMN);
            if (update.containsKey(PROPERTY.CELL_FORMATTER_ADD_STYLE_NAME)) {
                uiObject.getCellFormatter().addStyleName(cellRow, cellColumn, update.getString(PROPERTY.CELL_FORMATTER_ADD_STYLE_NAME));
            }
            if (update.containsKey(PROPERTY.CELL_FORMATTER_REMOVE_STYLE_NAME)) {
                uiObject.getCellFormatter().removeStyleName(cellRow, cellColumn, update.getString(PROPERTY.CELL_FORMATTER_REMOVE_STYLE_NAME));
            }
            if (update.containsKey(PROPERTY.CELL_VERTICAL_ALIGNMENT)) {
                final VerticalAlignmentConstant asVerticalAlignmentConstant = PVerticalAlignment.values()[update.getInt(PROPERTY.CELL_VERTICAL_ALIGNMENT)].asVerticalAlignmentConstant();
                uiObject.getCellFormatter().setVerticalAlignment(cellRow, cellColumn, asVerticalAlignmentConstant);
            }
            if (update.containsKey(PROPERTY.CELL_HORIZONTAL_ALIGNMENT)) {
                final HorizontalAlignmentConstant asHorizontalAlignmentConstant = PHorizontalAlignment.values()[update.getInt(PROPERTY.CELL_HORIZONTAL_ALIGNMENT)].asHorizontalAlignmentConstant();
                uiObject.getCellFormatter().setHorizontalAlignment(cellRow, cellColumn, asHorizontalAlignmentConstant);
            }
        } else if (update.containsKey(PROPERTY.HTMLTABLE_COLUMN_STYLE)) {
            final int column = update.getInt(PROPERTY.COLUMN);
            if (update.containsKey(PROPERTY.COLUMN_FORMATTER_ADD_STYLE_NAME)) {
                uiObject.getColumnFormatter().addStyleName(column, update.getString(PROPERTY.COLUMN_FORMATTER_ADD_STYLE_NAME));
            }
            if (update.containsKey(PROPERTY.COLUMN_FORMATTER_REMOVE_STYLE_NAME)) {
                uiObject.getColumnFormatter().removeStyleName(column, update.getString(PROPERTY.COLUMN_FORMATTER_REMOVE_STYLE_NAME));
            }
            if (update.containsKey(PROPERTY.COLUMN_FORMATTER_COLUMN_WIDTH)) {
                uiObject.getColumnFormatter().setWidth(column, update.getString(PROPERTY.COLUMN_FORMATTER_COLUMN_WIDTH));
            }
        } else {
            super.update(update, uiService);
        }
    }
}
