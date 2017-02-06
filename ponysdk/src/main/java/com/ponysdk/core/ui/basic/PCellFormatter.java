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

import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.model.ServerToClientModel;

public class PCellFormatter {

    protected final PHTMLTable<? extends PCellFormatter> table;

    protected PCellFormatter(final PHTMLTable<? extends PCellFormatter> table) {
        this.table = table;
    }

    public void addStyleName(final int row, final int column, final String styleName) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.CELL_FORMATTER_ADD_STYLE_NAME, styleName);
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }

    public void removeStyleName(final int row, final int column, final String styleName) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.CELL_FORMATTER_REMOVE_STYLE_NAME, styleName);
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }

    public void setStyleName(final int row, final int column, final String styleName) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.CELL_FORMATTER_SET_STYLE_NAME, styleName);
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }

    public void setVerticalAlignment(final int row, final int column, final PVerticalAlignment align) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.VERTICAL_ALIGNMENT, align.getValue());
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }

    public void setHorizontalAlignment(final int row, final int column, final PHorizontalAlignment align) {
        table.saveUpdate((writer) -> {
            writer.writeModel(ServerToClientModel.HORIZONTAL_ALIGNMENT, align.getValue());
            writer.writeModel(ServerToClientModel.ROW, row);
            writer.writeModel(ServerToClientModel.COLUMN, column);
        });
    }
}