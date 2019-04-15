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

package com.ponysdk.sample.client.page.datagrid;

import com.ponysdk.core.server.application.DataListener;
import com.ponysdk.core.server.context.UIContextImpl;
import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.ui.list.IdentityDataGridColumnDescriptor;
import com.ponysdk.core.ui.list.renderer.cell.HtmlCellRenderer;
import com.ponysdk.core.ui.list.renderer.header.StringHeaderCellRenderer;
import com.ponysdk.sample.client.datamodel.PonyStock;
import com.ponysdk.sample.client.page.SamplePageActivity;

import java.util.Arrays;

public class RefreshableDataGridPageActivity extends SamplePageActivity implements DataListener {

    protected PScrollPanel scroll;
    protected PFlowPanel layout;
    protected PFlexTable actions;
    protected PSimplePanel listContainer;

    public RefreshableDataGridPageActivity(final String pageName) {
        super(pageName, Arrays.asList("Rich UI Components", "Refreshable Data Grid"));
    }

    @Override
    protected void onFirstShowPage() {

        super.onFirstShowPage();

        scroll = Element.newPScrollPanel();
        layout = Element.newPFlowPanel();
        actions = Element.newPFlexTable();
        listContainer = Element.newPSimplePanel();

        scroll.setWidget(layout);
        layout.add(actions);
        layout.add(listContainer);

        actions.setStyleProperty("margin-left", "1em");
        actions.setStyleProperty("margin-right", "1em");

        examplePanel.setWidget(scroll);

        UIContextImpl.get().addDataListener(this);
    }

    @Override
    public void onData(final Object data) {
        if (data instanceof PonyStock) {
            onPonyStock((PonyStock) data);
        }
    }

    protected void onPonyStock(final PonyStock data) {
        // Nothing to do
    }

    protected IdentityDataGridColumnDescriptor<PonyStock> newCountDescriptor() {
        final IdentityDataGridColumnDescriptor<PonyStock> countDescriptor = new IdentityDataGridColumnDescriptor<>();
        countDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Stock"));
        countDescriptor.setCellRenderer(new HtmlCellRenderer<PonyStock>() {

            @Override
            protected String getValue(final PonyStock value) {
                return value.getCount().toString();
            }
        });
        return countDescriptor;
    }

    protected IdentityDataGridColumnDescriptor<PonyStock> newPriceDescriptor() {
        final IdentityDataGridColumnDescriptor<PonyStock> priceDescriptor = new IdentityDataGridColumnDescriptor<>();
        priceDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Price"));
        priceDescriptor.setCellRenderer(new HtmlCellRenderer<PonyStock>() {

            @Override
            protected String getValue(final PonyStock value) {
                return value.getPrice().toString();
            }
        });
        return priceDescriptor;
    }

    protected IdentityDataGridColumnDescriptor<PonyStock> newRaceDescriptor() {
        final IdentityDataGridColumnDescriptor<PonyStock> raceDescriptor = new IdentityDataGridColumnDescriptor<>();
        raceDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Race"));
        raceDescriptor.setCellRenderer(new HtmlCellRenderer<PonyStock>() {

            @Override
            protected String getValue(final PonyStock value) {
                return value.getRace();
            }

        });
        return raceDescriptor;
    }

    protected IdentityDataGridColumnDescriptor<PonyStock> newIDDescriptor() {
        final IdentityDataGridColumnDescriptor<PonyStock> idDescriptor = new IdentityDataGridColumnDescriptor<>();
        idDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("ID"));
        idDescriptor.setCellRenderer(new HtmlCellRenderer<PonyStock>() {

            @Override
            protected String getValue(final PonyStock value) {
                return value.getId().toString();
            }
        });
        return idDescriptor;
    }

}
