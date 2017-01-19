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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.grid.GridTableWidget;
import com.ponysdk.core.ui.list.refreshable.RefreshableDataGrid;
import com.ponysdk.sample.client.datamodel.PonyStock;

public class SortableRefreshableDataGridPageActivity extends RefreshableDataGridPageActivity {

    private SortableRefreshableDataGrid<Long, PonyStock> dataGrid;
    private boolean added = false;

    public SortableRefreshableDataGridPageActivity() {
        super("Sortable");
    }

    @Override
    protected void onFirstShowPage() {

        super.onFirstShowPage();

        final PButton addRow = new PButton("Insert row");
        addRow.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                if (added) return;
                insertColspanRow();
                added = true;
            }
        });

        actions.setWidget(0, 0, addRow);

        dataGrid = new SortableRefreshableDataGrid<>(new Comparator<PonyStock>() {

            @Override
            public int compare(final PonyStock o1, final PonyStock o2) {
                return o1.getPrice().compareTo(o2.getPrice());
            }
        });

        dataGrid.addDataGridColumnDescriptor(newIDDescriptor());
        dataGrid.addDataGridColumnDescriptor(newRaceDescriptor());
        dataGrid.addDataGridColumnDescriptor(newPriceDescriptor());
        dataGrid.addDataGridColumnDescriptor(newCountDescriptor());

        dataGrid.getListView().setStyleProperty("width", "100%");
        dataGrid.getListView().setColumnWidth(0, "15%");
        dataGrid.getListView().setColumnWidth(1, "25%");
        dataGrid.getListView().setColumnWidth(2, "25%");
        dataGrid.getListView().setColumnWidth(3, "25%");

        listContainer.setWidget(dataGrid);
    }

    @Override
    protected void onPonyStock(final PonyStock data) {
        //        dataGrid.setData(data.getId(), data);
    }

    private class SortableRefreshableDataGrid<K, D> extends RefreshableDataGrid<K, D> {

        private final List<D> datas = new ArrayList<>();

        private final Comparator<D> comparator;

        public SortableRefreshableDataGrid(final Comparator<D> comparator) {
            super(new GridTableWidget());
            this.comparator = comparator;
        }

        @Override
        public GridTableWidget getListView() {
            return (GridTableWidget) super.getListView();
        }

        public void setData(final K key, final D data) {

            if (added) {
                removeColspanRow();
            }

            //            final int previousRow = getViewRowIndex(key);
            //            if (previousRow == -1) {
            //                // add new row
            //                datas.add(data);
            //            }

            //            super.setData(key, data);

            Collections.sort(datas, comparator);

            final int newRow = datas.indexOf(data);
            //            if (previousRow != newRow) {
            //                moveRow(key, newRow);
            //            }

            if (added) {
                insertColspanRow();
            }
        }
    }

    protected void removeColspanRow() {
        dataGrid.remove(5);
    }

    protected void insertColspanRow() {
        dataGrid.insertRow(5, 0, 4, new PLabel("Hello " + System.currentTimeMillis()));
    }

}
