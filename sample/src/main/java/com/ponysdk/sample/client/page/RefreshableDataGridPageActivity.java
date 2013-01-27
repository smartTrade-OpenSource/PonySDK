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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ponysdk.sample.client.datamodel.PonyStock;
import com.ponysdk.ui.server.basic.DataListener;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PPusher;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.celltable.SimpleTableView;
import com.ponysdk.ui.server.list2.refreshable.Cell;
import com.ponysdk.ui.server.list2.refreshable.RefreshableCellRenderer;
import com.ponysdk.ui.server.list2.refreshable.RefreshableDataGrid;
import com.ponysdk.ui.server.list2.refreshable.RefreshableDataGridColumnDescriptor;
import com.ponysdk.ui.server.list2.renderer.header.StringHeaderCellRenderer;
import com.ponysdk.ui.server.list2.valueprovider.IdentityValueProvider;

public class RefreshableDataGridPageActivity extends SamplePageActivity implements DataListener {

    private SortableRefreshableDataGrid<Long, PonyStock> dataGrid;

    public RefreshableDataGridPageActivity() {
        super("Refreshable Data Grid", "Rich UI Components");
    }

    @Override
    protected void onFirstShowPage() {

        super.onFirstShowPage();

        final PScrollPanel scroll = new PScrollPanel();
        final PFlowPanel layout = new PFlowPanel();
        final PFlexTable formContainer = new PFlexTable();
        final PSimplePanel listContainer = new PSimplePanel();
        layout.add(formContainer);
        layout.add(listContainer);
        scroll.setWidget(layout);
        examplePanel.setWidget(scroll);

        dataGrid = new SortableRefreshableDataGrid<Long, PonyStock>(new Comparator<PonyStock>() {

            @Override
            public int compare(final PonyStock o1, final PonyStock o2) {
                return o1.getPrice().compareTo(o2.getPrice());
            }
        });

        final RefreshableDataGridColumnDescriptor<PonyStock, PonyStock, PHTML> idDescriptor = new RefreshableDataGridColumnDescriptor<PonyStock, PonyStock, PHTML>();
        idDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("ID"));
        idDescriptor.setValueProvider(new IdentityValueProvider<PonyStock>());
        idDescriptor.setCellRenderer(new RefreshableCellRenderer<PonyStock, PHTML>() {

            @Override
            public PHTML render(final int row, final PonyStock value) {
                return new PHTML(value.getId().toString());
            }

            @Override
            public void update(final PonyStock value, final Cell<PonyStock, PHTML> previous) {
                // no update
            }

        });

        final RefreshableDataGridColumnDescriptor<PonyStock, PonyStock, PHTML> raceDescriptor = new RefreshableDataGridColumnDescriptor<PonyStock, PonyStock, PHTML>();
        raceDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Race"));
        raceDescriptor.setValueProvider(new IdentityValueProvider<PonyStock>());
        raceDescriptor.setCellRenderer(new RefreshableCellRenderer<PonyStock, PHTML>() {

            @Override
            public PHTML render(final int row, final PonyStock value) {
                return new PHTML(value.getRace());
            }

            @Override
            public void update(final PonyStock value, final Cell<PonyStock, PHTML> previous) {
                // no update
            }

        });

        final RefreshableDataGridColumnDescriptor<PonyStock, PonyStock, PHTML> priceDescriptor = new RefreshableDataGridColumnDescriptor<PonyStock, PonyStock, PHTML>();
        priceDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Price"));
        priceDescriptor.setValueProvider(new IdentityValueProvider<PonyStock>());
        priceDescriptor.setCellRenderer(new RefreshableCellRenderer<PonyStock, PHTML>() {

            @Override
            public PHTML render(final int row, final PonyStock value) {
                return new PHTML(value.getPrice().toString());
            }

            @Override
            public void update(final PonyStock value, final Cell<PonyStock, PHTML> previous) {
                previous.getW().setText(value.getPrice().toString());
            }

        });

        final RefreshableDataGridColumnDescriptor<PonyStock, PonyStock, PHTML> countDescriptor = new RefreshableDataGridColumnDescriptor<PonyStock, PonyStock, PHTML>();
        countDescriptor.setHeaderCellRenderer(new StringHeaderCellRenderer("Stock"));
        countDescriptor.setValueProvider(new IdentityValueProvider<PonyStock>());
        countDescriptor.setCellRenderer(new RefreshableCellRenderer<PonyStock, PHTML>() {

            @Override
            public PHTML render(final int row, final PonyStock value) {
                return new PHTML(value.getCount().toString());
            }

            @Override
            public void update(final PonyStock value, final Cell<PonyStock, PHTML> previous) {
                previous.getW().setText(value.getCount().toString());
            }

        });

        dataGrid.addDataGridColumnDescriptor(idDescriptor);
        dataGrid.addDataGridColumnDescriptor(raceDescriptor);
        dataGrid.addDataGridColumnDescriptor(priceDescriptor);
        dataGrid.addDataGridColumnDescriptor(countDescriptor);

        dataGrid.getListView().setStyleProperty("width", "100%");
        dataGrid.getListView().setColumnWidth(0, "15%");
        dataGrid.getListView().setColumnWidth(1, "25%");
        dataGrid.getListView().setColumnWidth(2, "25%");
        dataGrid.getListView().setColumnWidth(3, "25%");

        listContainer.setWidget(dataGrid);

        PPusher.initialize();
        PPusher.get().addDataListener(this);
    }

    @Override
    public void onData(final Object data) {
        if (data instanceof PonyStock) {
            final PonyStock ponyStock = (PonyStock) data;
            dataGrid.setData(ponyStock.getId(), ponyStock);
        }
    }

    private class SortableRefreshableDataGrid<K, D> extends RefreshableDataGrid<K, D> {

        private final List<K> keys = new ArrayList<K>();
        private final List<D> datas = new ArrayList<D>();

        private final Comparator<D> comparator;

        public SortableRefreshableDataGrid(final Comparator<D> comparator) {
            super(new SimpleTableView());
            this.comparator = comparator;
        }

        @Override
        public SimpleTableView getListView() {
            return (SimpleTableView) super.getListView();
        }

        @Override
        public void setData(final K key, final D data) {
            int previousIndex = keys.indexOf(key);
            if (previousIndex == -1) {
                previousIndex = datas.size();
                datas.add(data);
                keys.add(key);
            } else {
                datas.remove(previousIndex);
                datas.add(previousIndex, data);
            }

            Collections.sort(datas, comparator);

            final int newIndex = datas.indexOf(data);

            super.setData(key, data);

            if (newIndex != previousIndex) {
                getListView().moveRow(previousIndex + 1, newIndex + 1);
                keys.remove(previousIndex);
                keys.add(newIndex, key);
            }
        }
    }

}
