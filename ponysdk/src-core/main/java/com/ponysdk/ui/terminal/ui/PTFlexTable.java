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

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.Model;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTFlexTable extends PTHTMLTable {

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIService uiService) {
        super.create(buffer, objectId, uiService);

        this.uiObject.addStyleName("pony-PFlexTable");
    }

    @Override
    protected HTMLTable createUIObject() {
        return new FlexTable();
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (Model.CLEAR_ROW.equals(binaryModel.getModel())) {
            cast().removeRow(binaryModel.getIntValue());
            return true;
        }
        if (Model.INSERT_ROW.equals(binaryModel.getModel())) {
            cast().insertRow(binaryModel.getIntValue());
            return true;
        }
        if (Model.SET_COL_SPAN.equals(binaryModel.getModel())) {
            // Model.ROW
            final int cellFormatterRow = buffer.getBinaryModel().getIntValue();
            // Model.COLUMN
            final int cellFormatterColumn = buffer.getBinaryModel().getIntValue();
            cast().getFlexCellFormatter().setColSpan(cellFormatterRow, cellFormatterColumn, binaryModel.getIntValue());
        }
        if (Model.SET_ROW_SPAN.equals(binaryModel.getModel())) {
            // Model.ROW
            final int cellFormatterRow = buffer.getBinaryModel().getIntValue();
            // Model.COLUMN
            final int cellFormatterColumn = buffer.getBinaryModel().getIntValue();
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
