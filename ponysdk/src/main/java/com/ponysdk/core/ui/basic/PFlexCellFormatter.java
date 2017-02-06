/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ServerToClientModel;

public class PFlexCellFormatter extends PCellFormatter {

    protected PFlexCellFormatter(final PHTMLTable<PFlexCellFormatter> table) {
        super(table);
    }

    public void setColSpan(final int row, final int column, final int colSpan) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.SET_COL_SPAN, colSpan);
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }

    public void setRowSpan(final int row, final int column, final int rowSpan) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.SET_ROW_SPAN, rowSpan);
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }
}