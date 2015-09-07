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

package com.ponysdk.ui.server.basic;

import com.ponysdk.core.Parser;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

/**
 * A flexible table that creates cells on demand. It can be jagged (that is, each row can contain a different
 * number of cells) and individual cells can be set to span multiple rows or columns.
 */
public class PFlexTable extends PHTMLTable {

    public PFlexTable() {
        setCellFormatter(new PFlexCellFormatter());
    }

    public PFlexCellFormatter getFlexCellFormatter() {
        return (PFlexCellFormatter) getCellFormatter();
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.FLEX_TABLE;
    }

    public class PFlexCellFormatter extends PCellFormatter {

        public void setColSpan(final int row, final int column, final int colSpan) {
            final Parser parser = Txn.get().getTxnContext().getParser();
            parser.beginObject();
            parser.parse(Model.TYPE_UPDATE);
            parser.comma();
            parser.parse(Model.OBJECT_ID, ID);
            parser.comma();
            parser.parse(Model.FLEXTABLE_CELL_FORMATTER, -1);
            parser.comma();
            parser.parse(Model.ROW, row);
            parser.comma();
            parser.parse(Model.COLUMN, column);
            parser.comma();
            parser.parse(Model.SET_COL_SPAN, colSpan);
            parser.endObject();
        }

        public void setRowSpan(final int row, final int column, final int rowSpan) {
            final Parser parser = Txn.get().getTxnContext().getParser();
            parser.beginObject();
            parser.parse(Model.TYPE_UPDATE);
            parser.comma();
            parser.parse(Model.OBJECT_ID, ID);
            parser.comma();
            parser.parse(Model.FLEXTABLE_CELL_FORMATTER, -1);
            parser.comma();
            parser.parse(Model.ROW, row);
            parser.comma();
            parser.parse(Model.COLUMN, column);
            parser.comma();
            parser.parse(Model.SET_ROW_SPAN, rowSpan);
            parser.endObject();
        }
    }
}