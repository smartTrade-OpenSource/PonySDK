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

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTFlexTable extends PTHTMLTable {

    private static final String PONY_FLEX_TABLE_STYLE_NAME = "pony-PFlexTable";

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        super.create(buffer, objectId, uiService);

        this.uiObject.addStyleName(PONY_FLEX_TABLE_STYLE_NAME);
    }

    @Override
    protected HTMLTable createUIObject() {
        return new FlexTable();
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.CLEAR_ROW.equals(binaryModel.getModel())) {
            cast().removeRow(binaryModel.getIntValue());
            return true;
        }
        if (ServerToClientModel.INSERT_ROW.equals(binaryModel.getModel())) {
            cast().insertRow(binaryModel.getIntValue());
            return true;
        }
        if (ServerToClientModel.SET_COL_SPAN.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int cellFormatterRow = buffer.readBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellFormatterColumn = buffer.readBinaryModel().getIntValue();
            cast().getFlexCellFormatter().setColSpan(cellFormatterRow, cellFormatterColumn, binaryModel.getIntValue());
        }
        if (ServerToClientModel.SET_ROW_SPAN.equals(binaryModel.getModel())) {
            // ServerToClientModel.ROW
            final int cellFormatterRow = buffer.readBinaryModel().getIntValue();
            // ServerToClientModel.COLUMN
            final int cellFormatterColumn = buffer.readBinaryModel().getIntValue();
            cast().getFlexCellFormatter().setRowSpan(cellFormatterRow, cellFormatterColumn, binaryModel.getIntValue());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    @Override
    public FlexTable cast() {
        return (FlexTable) uiObject;
    }
}
