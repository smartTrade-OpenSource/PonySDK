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

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public class PTGrid extends PTHTMLTable {

    private int rows = -1;
    private int columns = -1;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        final BinaryModel binaryModel = buffer.readBinaryModel();
        if (ServerToClientModel.ROW.equals(binaryModel.getModel())) {
            rows = binaryModel.getIntValue();
            columns = buffer.readBinaryModel().getIntValue();
        } else {
            buffer.rewind(binaryModel);
        }

        super.create(buffer, objectId, uiService);

        this.uiObject.addStyleName("pony-PGrid");
    }

    @Override
    protected HTMLTable createUIObject() {
        return rows != -1 ? new Grid(rows, columns) : new Grid();
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.CLEAR_ROW.ordinal() == modelOrdinal) {
            cast().removeRow(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.INSERT_ROW.ordinal() == modelOrdinal) {
            cast().insertRow(binaryModel.getIntValue());
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public Grid cast() {
        return (Grid) uiObject;
    }
}
