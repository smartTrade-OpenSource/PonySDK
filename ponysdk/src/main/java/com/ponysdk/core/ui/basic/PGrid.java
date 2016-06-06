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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.server.application.Parser;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;

/**
 * A rectangular grid that can contain text, html, or a child {@link PWidget}
 * within its cells. It must be resized explicitly to the desired number of rows
 * and columns.
 */
public class PGrid extends PHTMLTable<PCellFormatter> {

    private final int columns;
    private final int rows;

    public PGrid() {
        this(0, 0);
    }

    public PGrid(final int rows, final int columns) {
        super();
        this.rows = rows;
        this.columns = columns;
    }

    @Override
    protected void init0() {
        super.init0();
        setCellFormatter(new PCellFormatter(this));
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);

        if (rows != 0 && columns != 0) {
            parser.parse(ServerToClientModel.ROW, rows);
            parser.parse(ServerToClientModel.COLUMN, columns);
        }
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