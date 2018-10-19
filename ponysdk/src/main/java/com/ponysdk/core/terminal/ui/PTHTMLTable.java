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
import com.ponysdk.core.terminal.ui.converter.GWTConverter;

public abstract class PTHTMLTable<T extends HTMLTable> extends PTPanel<T> {

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
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.CLEAR == model) {
            uiObject.clear();
            return true;
        } else if (ServerToClientModel.BORDER_WIDTH == model) {
            uiObject.setBorderWidth(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.CELL_SPACING == model) {
            uiObject.setCellSpacing(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.CELL_PADDING == model) {
            uiObject.setCellPadding(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.ROW_FORMATTER_ADD_STYLE_NAME == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.ROW
            final int row = buffer.readBinaryModel().getIntValue();
            uiObject.getRowFormatter().addStyleName(row, value);
            return true;
        } else if (ServerToClientModel.ROW_FORMATTER_SET_STYLE_NAME == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.ROW
            final int row = buffer.readBinaryModel().getIntValue();
            uiObject.getRowFormatter().setStyleName(row, value);
            return true;
        } else if (ServerToClientModel.ROW_FORMATTER_REMOVE_STYLE_NAME == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.ROW
            final int row = buffer.readBinaryModel().getIntValue();
            uiObject.getRowFormatter().removeStyleName(row, value);
            return true;
        } else if (ServerToClientModel.CELL_FORMATTER_ADD_STYLE_NAME == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.ROW
            final int cellRow = buffer.readBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.readBinaryModel().getIntValue();
            uiObject.getCellFormatter().addStyleName(cellRow, cellColumn, value);
            return true;
        } else if (ServerToClientModel.CELL_FORMATTER_REMOVE_STYLE_NAME == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.ROW
            final int cellRow = buffer.readBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.readBinaryModel().getIntValue();
            uiObject.getCellFormatter().removeStyleName(cellRow, cellColumn, value);
            return true;
        } else if (ServerToClientModel.CELL_FORMATTER_SET_STYLE_NAME == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.ROW
            final int cellRow = buffer.readBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.readBinaryModel().getIntValue();
            uiObject.getCellFormatter().setStyleName(cellRow, cellColumn, value);
            return true;
        } else if (ServerToClientModel.VERTICAL_ALIGNMENT == model) {
            final VerticalAlignmentConstant value = GWTConverter.asVerticalAlignmentConstant(binaryModel.getIntValue());
            // ServerToClientModel.ROW
            final int cellRow = buffer.readBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.readBinaryModel().getIntValue();
            uiObject.getCellFormatter().setVerticalAlignment(cellRow, cellColumn, value);
            return true;
        } else if (ServerToClientModel.HORIZONTAL_ALIGNMENT == model) {
            final HorizontalAlignmentConstant value = GWTConverter.asHorizontalAlignmentConstant(binaryModel.getIntValue());
            // ServerToClientModel.ROW
            final int cellRow = buffer.readBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellColumn = buffer.readBinaryModel().getIntValue();
            uiObject.getCellFormatter().setHorizontalAlignment(cellRow, cellColumn, value);
            return true;
        } else if (ServerToClientModel.COLUMN_FORMATTER_ADD_STYLE_NAME == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.COLUMN
            final int column = buffer.readBinaryModel().getIntValue();
            uiObject.getColumnFormatter().addStyleName(column, value);
            return true;
        } else if (ServerToClientModel.COLUMN_FORMATTER_REMOVE_STYLE_NAME == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.COLUMN
            final int column = buffer.readBinaryModel().getIntValue();
            uiObject.getColumnFormatter().removeStyleName(column, value);
            return true;
        } else if (ServerToClientModel.COLUMN_FORMATTER_SET_STYLE_NAME == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.COLUMN
            final int column = buffer.readBinaryModel().getIntValue();
            uiObject.getColumnFormatter().setStyleName(column, value);
            return true;
        } else if (ServerToClientModel.COLUMN_FORMATTER_COLUMN_WIDTH == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.COLUMN
            final int column = buffer.readBinaryModel().getIntValue();
            uiObject.getColumnFormatter().setWidth(column, value);
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

}
