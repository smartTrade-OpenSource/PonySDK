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

import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.celltable.SimpleTableView;
import com.ponysdk.core.ui.form.formfield.IntegerTextBoxFormField;
import com.ponysdk.core.ui.form.formfield.LongTextBoxFormField;
import com.ponysdk.core.ui.list.refreshable.RefreshableDataGrid;
import com.ponysdk.sample.client.datamodel.PonyStock;

public class SimpleRefreshableDataGridPageActivity extends RefreshableDataGridPageActivity {

    private RefreshableDataGrid<Long, PonyStock> dataGrid;

    public SimpleRefreshableDataGridPageActivity() {
        super("Simple");
    }

    @Override
    protected void onFirstShowPage() {

        super.onFirstShowPage();

        addForm();

        dataGrid = new RefreshableDataGrid<>(new SimpleTableView());

        dataGrid.addDataGridColumnDescriptor(newIDDescriptor());
        dataGrid.addDataGridColumnDescriptor(newRaceDescriptor());
        dataGrid.addDataGridColumnDescriptor(newPriceDescriptor());
        dataGrid.addDataGridColumnDescriptor(newCountDescriptor());

        dataGrid.getListView().asWidget().setStyleProperty("width", "100%");
        dataGrid.getListView().setColumnWidth(0, "15%");
        dataGrid.getListView().setColumnWidth(1, "25%");
        dataGrid.getListView().setColumnWidth(2, "25%");
        dataGrid.getListView().setColumnWidth(3, "25%");

        listContainer.setWidget(dataGrid);
    }

    private void addForm() {
        final IntegerTextBoxFormField addRowFormField = new IntegerTextBoxFormField();
        final IntegerTextBoxFormField removeRowFormField = new IntegerTextBoxFormField();
        final LongTextBoxFormField removeRowByKeyFormField = new LongTextBoxFormField();
        final LongTextBoxFormField keyRowFormField = new LongTextBoxFormField();
        final IntegerTextBoxFormField toRowFormField = new IntegerTextBoxFormField();

        addRowFormField.asWidget().setPlaceholder("Row index (add)");
        removeRowFormField.asWidget().setPlaceholder("Row index (remove)");
        removeRowByKeyFormField.asWidget().setPlaceholder("Row key (remove)");
        keyRowFormField.asWidget().setPlaceholder("Row key (move)");
        toRowFormField.asWidget().setPlaceholder("Row index (move)");

        final PButton addButton = new PButton("Add a row");
        addButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final Integer index = addRowFormField.getValue();
                if (index != null) {
                    dataGrid.insertRow(index, 0, 4, new PLabel("Colspan 1 inserted"));
                }
            }
        });

        final PButton removeByKeyButton = new PButton("Remove a row (by key)");
        removeByKeyButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final Long k = removeRowByKeyFormField.getValue();
                if (k != null) {
                    dataGrid.removeByKey(k);
                }
            }
        });

        final PButton removeButton = new PButton("Remove a row (by index)");
        removeButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final Integer v = removeRowFormField.getValue();
                if (v != null) {
                    dataGrid.remove(v);
                }
            }
        });

        final PButton moveButton = new PButton("Move a row");
        moveButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final Long k = keyRowFormField.getValue();
                final Integer v = toRowFormField.getValue();
                if (v != null && k != null) {
                    dataGrid.moveRow(k, v);
                }
            }
        });

        actions.setWidget(0, 0, addRowFormField.asWidget());
        actions.setWidget(0, 1, addButton);
        actions.setWidget(1, 0, removeRowByKeyFormField.asWidget());
        actions.setWidget(1, 1, removeByKeyButton);
        actions.setWidget(2, 0, removeRowFormField.asWidget());
        actions.setWidget(2, 1, removeButton);

        actions.setWidget(0, 2, keyRowFormField.asWidget());
        actions.setWidget(0, 3, toRowFormField.asWidget());
        actions.setWidget(0, 4, moveButton);
    }

    @Override
    protected void onPonyStock(final PonyStock data) {
        dataGrid.setData(data.getId(), data);
    }

}
