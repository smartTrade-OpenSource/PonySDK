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

package com.ponysdk.sample.client.page;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PGrid;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;

public class GridPageActivity extends SamplePageActivity implements PClickHandler {

    private PListBox rowListBox;
    private PListBox cellListBox;
    private PListBox actionListBox;
    private PListBox styleListBox;
    private PGrid table;

    public GridPageActivity() {
        super("Grid Table", "Table");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PFlowPanel container = Element.newPFlowPanel();

        table = Element.newPGrid(10, 10);
        table.setCellPadding(0);
        table.setCellSpacing(0);
        table.setSizeFull();

        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                table.setWidget(r, c, Element.newPLabel(r + "_" + c));
            }
        }

        container.add(table);

        final PLabel test = Element.newPLabel("Test style:");
        test.setStyleProperty("padding-top", "15px");
        container.add(test);

        rowListBox = buildIntListBox();
        cellListBox = buildIntListBox();
        actionListBox = buildActionListBox();
        styleListBox = buildStyleListBox();
        final PButton update = Element.newPButton("update");
        update.addClickHandler(this);

        container.add(rowListBox);
        container.add(cellListBox);
        container.add(actionListBox);
        container.add(styleListBox);
        container.add(update);

        final PScrollPanel scrollPanel = Element.newPScrollPanel();
        scrollPanel.setWidget(container);
        scrollPanel.setSizeFull();

        examplePanel.setWidget(scrollPanel);
    }

    private PListBox buildIntListBox() {
        final PListBox r = Element.newPListBox(false);
        for (int i = 0; i < 10; i++) {
            r.addItem("" + i, i);
        }
        r.selectIndex(0);
        return r;
    }

    private PListBox buildActionListBox() {
        final PListBox r = Element.newPListBox(false);
        r.addItem("add row style", 1);
        r.addItem("remove row style", 2);
        r.addItem("set row style", 3);
        r.addItem("add column style", 4);
        r.addItem("remove column style", 5);
        r.addItem("set column style", 6);
        r.addItem("add cell style", 7);
        r.addItem("remove cell style", 8);
        r.addItem("set cell style", 9);
        r.selectIndex(0);
        return r;
    }

    private PListBox buildStyleListBox() {
        final PListBox r = Element.newPListBox(false);
        r.addItem("orange", "orange");
        r.addItem("yellow", "yellow");
        r.addItem("bold", "bold");
        r.selectIndex(0);
        return r;
    }

    @Override
    public void onClick(final PClickEvent event) {
        final int row = (Integer) rowListBox.getSelectedValue();
        final int column = (Integer) cellListBox.getSelectedValue();
        final int action = (Integer) actionListBox.getSelectedValue();
        final String style = (String) styleListBox.getSelectedValue();

        switch (action) {
            case 1:
                table.getRowFormatter().addStyleName(row, style);
                break;
            case 2:
                table.getRowFormatter().removeStyleName(row, style);
                break;
            case 3:
                table.getRowFormatter().setStyleName(row, style);
                break;
            case 4:
                table.getColumnFormatter().addStyleName(column, style);
                break;
            case 5:
                table.getColumnFormatter().removeStyleName(column, style);
                break;
            case 6:
                table.getColumnFormatter().setStyleName(column, style);
                break;
            case 7:
                table.getCellFormatter().addStyleName(row, column, style);
                break;
            case 8:
                table.getCellFormatter().removeStyleName(row, column, style);
                break;
            case 9:
                table.getCellFormatter().setStyleName(row, column, style);
                break;
        }
    }
}
