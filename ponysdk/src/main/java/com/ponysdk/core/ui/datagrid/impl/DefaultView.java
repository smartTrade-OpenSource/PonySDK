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

package com.ponysdk.core.ui.datagrid.impl;

import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.ui.datagrid.View;

import java.util.HashMap;
import java.util.Map;

public class DefaultView implements View {

    private final PFlowPanel table = Element.newPFlowPanel();
    private final PFlowPanel headers = Element.newPFlowPanel();
    private final PFlowPanel rows = Element.newPFlowPanel();


    public DefaultView() {
        table.add(headers);
        table.add(rows);
    }

    @Override
    public PWidget asWidget() {
        return table;
    }

    @Override
    public PSimplePanel addHeader() {
        PSimplePanel th = Element.newPSimplePanel();
        th.addStyleName("th");
        headers.add(th);
        return th;
    }

    @Override
    public PWidget getHeader(final int c) {
        return headers.getWidget(c);
    }

    @Override
    public PWidget getCell(final int r, final int c) {
        PFlowPanel row = (PFlowPanel) rows.getWidget(r);
        return row.getWidget(c);
    }

    @Override
    public int getRowCount() {
        return rows.getWidgetCount();
    }

    @Override
    public void addRow() {
        PFlowPanel row = Element.newPFlowPanel();
        row.addStyleName("r");
        rows.add(row);
    }

    @Override
    public PSimplePanel addCell(int r) {
        PFlowPanel row = (PFlowPanel) rows.getWidget(r);
        PSimplePanel cell = Element.newPSimplePanel();
        row.add(cell);
        return cell;
    }

}