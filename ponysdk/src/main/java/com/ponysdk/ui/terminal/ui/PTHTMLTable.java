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
import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public abstract class PTHTMLTable extends PTPanel<HTMLTable> {

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final Widget w = asWidget(ptObject);

        // ServerToClientModel.ROW
        final int row = buffer.getBinaryModel().getIntValue();
        // ServerToClientModel.COLUMN
        final int cell = buffer.getBinaryModel().getIntValue();

        // uiObject.getCellFormatter().addStyleName(row, cell,
        // "pony-PFlextable-Cell");
        uiObject.setWidget(row, cell, w);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.CLEAR.equals(binaryModel.getModel())) {
            uiObject.clear();
            return true;
        }
        if (ServerToClientModel.BORDER_WIDTH.equals(binaryModel.getModel())) {
            uiObject.setBorderWidth(binaryModel.getIntValue());
            return true;
        }
        if (ServerToClientModel.CELL_SPACING.equals(binaryModel.getModel())) {
            uiObject.setCellSpacing(binaryModel.getIntValue());
            return true;
        }
        if (ServerToClientModel.CELL_PADDING.equals(binaryModel.getModel())) {
            uiObject.setCellPadding(binaryModel.getIntValue());
            return true;
        }
        if (ServerToClientModel.ROW_FORMATTER_ADD_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int row = buffer.getBinaryModel().getIntValue();
            uiObject.getRowFormatter().addStyleName(row, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.ROW_FORMATTER_SET_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int row = buffer.getBinaryModel().getIntValue();
            uiObject.getRowFormatter().setStyleName(row, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.ROW_FORMATTER_REMOVE_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int row = buffer.getBinaryModel().getIntValue();
            uiObject.getRowFormatter().removeStyleName(row, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.CELL_FORMATTER_ADD_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int cellRow = buffer.getBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.getBinaryModel().getIntValue();
            uiObject.getCellFormatter().addStyleName(cellRow, cellColumn, binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.CELL_FORMATTER_REMOVE_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int cellRow = buffer.getBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.getBinaryModel().getIntValue();
            uiObject.getCellFormatter().removeStyleName(cellRow, cellColumn, binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.CELL_FORMATTER_SET_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int cellRow = buffer.getBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.getBinaryModel().getIntValue();
            uiObject.getCellFormatter().setStyleName(cellRow, cellColumn, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.VERTICAL_ALIGNMENT.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int cellRow = buffer.getBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.getBinaryModel().getIntValue();
            final VerticalAlignmentConstant asVerticalAlignmentConstant = PVerticalAlignment.values()[binaryModel
                    .getByteValue()].asVerticalAlignmentConstant();
            uiObject.getCellFormatter().setVerticalAlignment(cellRow, cellColumn, asVerticalAlignmentConstant);
            return true;
        }
        if (ServerToClientModel.HORIZONTAL_ALIGNMENT.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int cellRow = buffer.getBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.getBinaryModel().getIntValue();
            final HorizontalAlignmentConstant asHorizontalAlignmentConstant = PHorizontalAlignment.values()[binaryModel
                    .getByteValue()].asHorizontalAlignmentConstant();
            uiObject.getCellFormatter().setHorizontalAlignment(cellRow, cellColumn, asHorizontalAlignmentConstant);
            return true;
        }
        if (ServerToClientModel.COLUMN_FORMATTER_ADD_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.COLUMN
            final int column = buffer.getBinaryModel().getIntValue();
            uiObject.getColumnFormatter().addStyleName(column, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.COLUMN_FORMATTER_REMOVE_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.COLUMN
            final int column = buffer.getBinaryModel().getIntValue();
            uiObject.getColumnFormatter().removeStyleName(column, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.COLUMN_FORMATTER_SET_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.COLUMN
            final int column = buffer.getBinaryModel().getIntValue();
            uiObject.getColumnFormatter().setStyleName(column, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.COLUMN_FORMATTER_COLUMN_WIDTH.equals(binaryModel.getModel())) {
            // ServerToClientModel.COLUMN
            final int column = buffer.getBinaryModel().getIntValue();
            uiObject.getColumnFormatter().setWidth(column, binaryModel.getStringValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }
}
