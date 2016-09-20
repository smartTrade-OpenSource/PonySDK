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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment.HorizontalAlignmentConstant;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.ui.alignment.PTHorizontalAlignment;
import com.ponysdk.core.terminal.ui.alignment.PTVerticalAlignment;

public abstract class PTHTMLTable extends PTPanel<HTMLTable> {

    @Override
    public void add(final ReaderBuffer buffer, final PTObject ptObject) {
        final Widget w = asWidget(ptObject);

        // ServerToClientModel.ROW
        final int row = buffer.readBinaryModel().getIntValue();
        // ServerToClientModel.COLUMN
        final int cell = buffer.readBinaryModel().getIntValue();

        uiObject.getCellFormatter().addStyleName(row, cell, "ptc");
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
            final int row = buffer.readBinaryModel().getIntValue();
            uiObject.getRowFormatter().addStyleName(row, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.ROW_FORMATTER_SET_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int row = buffer.readBinaryModel().getIntValue();
            uiObject.getRowFormatter().setStyleName(row, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.ROW_FORMATTER_REMOVE_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int row = buffer.readBinaryModel().getIntValue();
            uiObject.getRowFormatter().removeStyleName(row, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.CELL_FORMATTER_ADD_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int cellRow = buffer.readBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.readBinaryModel().getIntValue();
            uiObject.getCellFormatter().addStyleName(cellRow, cellColumn, binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.CELL_FORMATTER_REMOVE_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int cellRow = buffer.readBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.readBinaryModel().getIntValue();
            uiObject.getCellFormatter().removeStyleName(cellRow, cellColumn, binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.CELL_FORMATTER_SET_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int cellRow = buffer.readBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.readBinaryModel().getIntValue();
            uiObject.getCellFormatter().setStyleName(cellRow, cellColumn, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.VERTICAL_ALIGNMENT.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int cellRow = buffer.readBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.readBinaryModel().getIntValue();
            final VerticalAlignmentConstant asVerticalAlignmentConstant = PTVerticalAlignment.values()[binaryModel
                    .getByteValue()].asVerticalAlignmentConstant();
            uiObject.getCellFormatter().setVerticalAlignment(cellRow, cellColumn, asVerticalAlignmentConstant);
            return true;
        }
        if (ServerToClientModel.HORIZONTAL_ALIGNMENT.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int cellRow = buffer.readBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.readBinaryModel().getIntValue();
            final HorizontalAlignmentConstant asHorizontalAlignmentConstant = PTHorizontalAlignment.values()[binaryModel
                    .getByteValue()].asHorizontalAlignmentConstant();
            uiObject.getCellFormatter().setHorizontalAlignment(cellRow, cellColumn, asHorizontalAlignmentConstant);
            return true;
        }
        if (ServerToClientModel.COLUMN_FORMATTER_ADD_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.COLUMN
            final int column = buffer.readBinaryModel().getIntValue();
            uiObject.getColumnFormatter().addStyleName(column, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.COLUMN_FORMATTER_REMOVE_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.COLUMN
            final int column = buffer.readBinaryModel().getIntValue();
            uiObject.getColumnFormatter().removeStyleName(column, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.COLUMN_FORMATTER_SET_STYLE_NAME.equals(binaryModel.getModel())) {
            // ServerToClientModel.COLUMN
            final int column = buffer.readBinaryModel().getIntValue();
            uiObject.getColumnFormatter().setStyleName(column, binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.COLUMN_FORMATTER_COLUMN_WIDTH.equals(binaryModel.getModel())) {
            // ServerToClientModel.COLUMN
            final int column = buffer.readBinaryModel().getIntValue();
            uiObject.getColumnFormatter().setWidth(column, binaryModel.getStringValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }
}
