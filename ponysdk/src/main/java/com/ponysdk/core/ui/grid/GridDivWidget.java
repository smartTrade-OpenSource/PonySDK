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

package com.ponysdk.core.ui.grid;

import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PFlowPanel;

public class GridDivWidget extends AbstractGridWidget {

    public GridDivWidget() {
        super("div");
        setStyleProperty("display", "table");
    }

    @Override
    protected PComplexPanel createTableHeader() {
        final PFlowPanel thead = new PFlowPanel();
        thead.setStyleProperty("display", "table-header-group");
        return thead;
    }

    @Override
    protected PComplexPanel createTableBody() {
        final PFlowPanel tbody = new PFlowPanel();
        tbody.setStyleProperty("display", "table-row-group");
        return tbody;
    }

    @Override
    public PComplexPanel createTableRow() {
        final PComplexPanel newRow = new PFlowPanel();
        newRow.setStyleProperty("display", "table-row");
        return newRow;
    }

    @Override
    protected PComplexPanel createTableCell() {
        final PComplexPanel newCell = new PFlowPanel();
        newCell.setStyleProperty("display", "table-cell");
        return newCell;
    }

    @Override
    protected PComplexPanel createTableHeaderCell() {
        final PComplexPanel newCell = new PFlowPanel();
        newCell.setStyleProperty("display", "table-cell");
        return createTableCell();
    }
}
