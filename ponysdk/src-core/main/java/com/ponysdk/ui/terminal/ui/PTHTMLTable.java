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
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.model.Model;

public class PTHTMLTable extends PTPanel<HTMLTable> {

    @Override
    public void add(final PTInstruction add, final UIService uiService) {
        final Widget w = asWidget(add.getObjectID(), uiService);

        final int row = add.getInt(Model.ROW);
        final int cell = add.getInt(Model.CELL);

        // uiObject.getCellFormatter().addStyleName(row, cell, "pony-PFlextable-Cell");
        uiObject.setWidget(row, cell, w);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(Model.CLEAR)) {
            uiObject.clear();
        } else if (update.containsKey(Model.BORDER_WIDTH)) {
            uiObject.setBorderWidth(update.getInt(Model.BORDER_WIDTH));
        } else if (update.containsKey(Model.CELL_SPACING)) {
            uiObject.setCellSpacing(update.getInt(Model.CELL_SPACING));
        } else if (update.containsKey(Model.CELL_PADDING)) {
            uiObject.setCellPadding(update.getInt(Model.CELL_PADDING));
        } else if (update.containsKey(Model.HTMLTABLE_ROW_STYLE)) {
            final int row = update.getInt(Model.ROW);
            if (update.containsKey(Model.ROW_FORMATTER_ADD_STYLE_NAME)) {
                uiObject.getRowFormatter().addStyleName(row, update.getString(Model.ROW_FORMATTER_ADD_STYLE_NAME));
            } else if (update.containsKey(Model.ROW_FORMATTER_SET_STYLE_NAME)) {
                uiObject.getRowFormatter().setStyleName(row, update.getString(Model.ROW_FORMATTER_SET_STYLE_NAME));
            } else if (update.containsKey(Model.ROW_FORMATTER_REMOVE_STYLE_NAME)) {
                uiObject.getRowFormatter().removeStyleName(row, update.getString(Model.ROW_FORMATTER_REMOVE_STYLE_NAME));
            }
        } else if (update.containsKey(Model.HTMLTABLE_CELL_STYLE)) {
            final int cellRow = update.getInt(Model.ROW);
            final int cellColumn = update.getInt(Model.COLUMN);
            if (update.containsKey(Model.CELL_FORMATTER_ADD_STYLE_NAME)) {
                uiObject.getCellFormatter().addStyleName(cellRow, cellColumn, update.getString(Model.CELL_FORMATTER_ADD_STYLE_NAME));
            } else if (update.containsKey(Model.CELL_FORMATTER_REMOVE_STYLE_NAME)) {
                uiObject.getCellFormatter().removeStyleName(cellRow, cellColumn, update.getString(Model.CELL_FORMATTER_REMOVE_STYLE_NAME));
            } else if (update.containsKey(Model.CELL_FORMATTER_SET_STYLE_NAME)) {
                uiObject.getCellFormatter().setStyleName(cellRow, cellColumn, update.getString(Model.CELL_FORMATTER_SET_STYLE_NAME));
            }
            if (update.containsKey(Model.CELL_VERTICAL_ALIGNMENT)) {
                final VerticalAlignmentConstant asVerticalAlignmentConstant = PVerticalAlignment.values()[update.getInt(Model.CELL_VERTICAL_ALIGNMENT)].asVerticalAlignmentConstant();
                uiObject.getCellFormatter().setVerticalAlignment(cellRow, cellColumn, asVerticalAlignmentConstant);
            }
            if (update.containsKey(Model.CELL_HORIZONTAL_ALIGNMENT)) {
                final HorizontalAlignmentConstant asHorizontalAlignmentConstant = PHorizontalAlignment.values()[update.getInt(Model.CELL_HORIZONTAL_ALIGNMENT)].asHorizontalAlignmentConstant();
                uiObject.getCellFormatter().setHorizontalAlignment(cellRow, cellColumn, asHorizontalAlignmentConstant);
            }
        } else if (update.containsKey(Model.HTMLTABLE_COLUMN_STYLE)) {
            final int column = update.getInt(Model.COLUMN);
            if (update.containsKey(Model.COLUMN_FORMATTER_ADD_STYLE_NAME)) {
                uiObject.getColumnFormatter().addStyleName(column, update.getString(Model.COLUMN_FORMATTER_ADD_STYLE_NAME));
            } else if (update.containsKey(Model.COLUMN_FORMATTER_REMOVE_STYLE_NAME)) {
                uiObject.getColumnFormatter().removeStyleName(column, update.getString(Model.COLUMN_FORMATTER_REMOVE_STYLE_NAME));
            } else if (update.containsKey(Model.COLUMN_FORMATTER_SET_STYLE_NAME)) {
                uiObject.getColumnFormatter().setStyleName(column, update.getString(Model.COLUMN_FORMATTER_SET_STYLE_NAME));
            }
            if (update.containsKey(Model.COLUMN_FORMATTER_COLUMN_WIDTH)) {
                uiObject.getColumnFormatter().setWidth(column, update.getString(Model.COLUMN_FORMATTER_COLUMN_WIDTH));
            }
        } else {
            super.update(update, uiService);
        }
    }
}
