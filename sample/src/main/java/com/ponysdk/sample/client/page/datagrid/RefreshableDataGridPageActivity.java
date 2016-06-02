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

import java.util.Arrays;

import com.ponysdk.core.UIContext;
import com.ponysdk.sample.client.datamodel.PonyStock;
import com.ponysdk.sample.client.page.SamplePageActivity;
import com.ponysdk.ui.server.basic.DataListener;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.list.DataGridColumnDescriptor;
import com.ponysdk.ui.server.list.refreshable.Cell;
import com.ponysdk.ui.server.list.renderer.cell.CellRenderer;
import com.ponysdk.ui.server.list.renderer.header.StringHeaderCellRenderer;
import com.ponysdk.ui.server.list.valueprovider.IdentityValueProvider;

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

        scroll = new PScrollPanel();
        layout = new PFlowPanel();
        actions = new PFlexTable();
        listContainer = new PSimplePanel();

        scroll.setWidget(layout);
        layout.add(actions);
        layout.add(listContainer);

        actions.setStyleProperty("margin-left", "1em");
        actions.setStyleProperty("margin-right", "1em");

        examplePanel.setWidget(scroll);

        UIContext.get().addDataListener(this);
    }

    @Override
    public void onData(final Object data) {
        if (data instanceof PonyStock) {
            onPonyStock((PonyStock) data);
        }
    }

    protected void onPonyStock(final PonyStock data) {
    }

    protected DataGridColumnDescriptor<PonyStock, PonyStock, PHTML> newCountDescriptor() {
        final DataGridColumnDescriptor<PonyStock, PonyStock, PHTML> countDescriptor = new DataGridColumnDescriptor<>();
        countDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Stock"));
        countDescriptor.setValueProvider(new IdentityValueProvider<PonyStock>());
        countDescriptor.setCellRenderer(new CellRenderer<PonyStock, PHTML>() {

            @Override
            public PHTML render(final int row, final PonyStock value) {
                return new PHTML(value.getCount().toString());
            }

            @Override
            public void update(final PonyStock value, final Cell<PonyStock, PHTML> previous) {
                previous.getW().setText(value.getCount().toString());
            }

        });
        return countDescriptor;
    }

    protected DataGridColumnDescriptor<PonyStock, PonyStock, PHTML> newPriceDescriptor() {
        final DataGridColumnDescriptor<PonyStock, PonyStock, PHTML> priceDescriptor = new DataGridColumnDescriptor<>();
        priceDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Price"));
        priceDescriptor.setValueProvider(new IdentityValueProvider<PonyStock>());
        priceDescriptor.setCellRenderer(new CellRenderer<PonyStock, PHTML>() {

            @Override
            public PHTML render(final int row, final PonyStock value) {
                return new PHTML(value.getPrice().toString());
            }

            @Override
            public void update(final PonyStock value, final Cell<PonyStock, PHTML> previous) {
                previous.getW().setText(value.getPrice().toString());
            }

        });
        return priceDescriptor;
    }

    protected DataGridColumnDescriptor<PonyStock, PonyStock, PHTML> newRaceDescriptor() {
        final DataGridColumnDescriptor<PonyStock, PonyStock, PHTML> raceDescriptor = new DataGridColumnDescriptor<>();
        raceDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Race"));
        raceDescriptor.setValueProvider(new IdentityValueProvider<PonyStock>());
        raceDescriptor.setCellRenderer(new CellRenderer<PonyStock, PHTML>() {

            @Override
            public PHTML render(final int row, final PonyStock value) {
                return new PHTML(value.getRace());
            }

            @Override
            public void update(final PonyStock value, final Cell<PonyStock, PHTML> previous) {
                // no update
            }

        });
        return raceDescriptor;
    }

    protected DataGridColumnDescriptor<PonyStock, PonyStock, PHTML> newIDDescriptor() {
        final DataGridColumnDescriptor<PonyStock, PonyStock, PHTML> idDescriptor = new DataGridColumnDescriptor<>();
        idDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("ID"));
        idDescriptor.setValueProvider(new IdentityValueProvider<PonyStock>());
        idDescriptor.setCellRenderer(new CellRenderer<PonyStock, PHTML>() {

            @Override
            public PHTML render(final int row, final PonyStock value) {
                return new PHTML(value.getId().toString());
            }

            @Override
            public void update(final PonyStock value, final Cell<PonyStock, PHTML> previous) {
                // no update
            }

        });
        return idDescriptor;
    }

}
