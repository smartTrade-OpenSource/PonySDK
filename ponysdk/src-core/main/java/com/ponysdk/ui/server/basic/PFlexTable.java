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

import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;

/**
 * A flexible table that creates cells on demand. It can be jagged (that is, each row can contain a different
 * number of cells) and individual cells can be set to span multiple rows or columns.
 * <p>
 * <img class='gallery' src='doc-files/PFlexTable.png'/>
 * </p>
 */
public class PFlexTable extends PHTMLTable {

    public PFlexTable() {
        // TODO nciaravola must be moved in terminal classes
        addStyleName("pony-PFlexTable");
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
            final Update update = new Update(ID);
            // TODO lbroussal decide what value put if key mandatory but no value tested on the terminal side
            update.put(PROPERTY.FLEXTABLE_CELL_FORMATTER, -1);
            update.put(PROPERTY.ROW, row);
            update.put(PROPERTY.COLUMN, column);
            update.put(PROPERTY.SET_COL_SPAN, colSpan);
            getPonySession().stackInstruction(update);
        }

        public void setRowSpan(final int row, final int column, final int rowSpan) {
            final Update update = new Update(ID);
            update.put(PROPERTY.FLEXTABLE_CELL_FORMATTER, -1);
            update.put(PROPERTY.ROW, row);
            update.put(PROPERTY.COLUMN, column);
            update.put(PROPERTY.SET_ROW_SPAN, rowSpan);
            getPonySession().stackInstruction(update);
        }
    }
}