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

import com.ponysdk.core.instruction.EntryInstruction;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

/**
 * A rectangular grid that can contain text, html, or a child {@link PWidget} within its cells. It must be
 * resized explicitly to the desired number of rows and columns.
 */
public class PGrid extends PHTMLTable {

    private final int columns;
    private final int rows;

    public PGrid(final int rows, final int columns) {
        super(new EntryInstruction(Model.ROW, rows), new EntryInstruction(Model.COLUMN, columns));

        this.rows = rows;
        this.columns = columns;

        setCellFormatter(new PCellFormatter());
    }

    public PGrid() {
        this(0, 0);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.GRID;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

}