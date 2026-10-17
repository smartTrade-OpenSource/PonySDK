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

package com.ponysdk.sample.client;

import static com.ponysdk.sample.client.Ui.*;
import static com.ponysdk.sample.client.Costs.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PDisclosurePanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
import com.ponysdk.core.ui.datagrid2.data.RowAction;
import com.ponysdk.core.ui.datagrid2.view.ColumnFilterFooterDataGridView;
import com.ponysdk.core.ui.datagrid2.view.ColumnVisibilitySelectorDataGridView;
import com.ponysdk.core.ui.datagrid2.view.ConfigSelectorDataGridView;
import com.ponysdk.core.ui.datagrid2.view.DataGridView;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridView;
import com.ponysdk.core.ui.datagrid2.view.RowSelectorColumnDataGridView;

/** DataGrid: 52 columns, up to 10 000 rows, the data and the logic on the server. */
final class DataGridPage {

    private DataGridPage() {
    }

    // ── Tab: DataGrid ────────────────────────────────────────────────────────

    static PFlowPanel build(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();
        pageTitle(p, "DataGrid", "52 columns, up to 10 000 rows. Sort, filter, resize, pin and select: the data and "
            + "the logic stay on the server, only the visible window of rows is sent to the browser.");
        buildDataGridInto(p);
        return p;
    }

    static void buildDataGridInto(final PFlowPanel container) {
        final DefaultDataGridView<Integer, MyRow> simpleGridView = new DefaultDataGridView<>();
        simpleGridView.setRefreshOnColumnVisibilityChanged(true);

        final ColumnVisibilitySelectorDataGridView<Integer, MyRow> columnVisibilitySelectorDataGridView = new ColumnVisibilitySelectorDataGridView<>(simpleGridView);
        final RowSelectorColumnDataGridView<Integer, MyRow> rowSelectorColumnDataGridView = new RowSelectorColumnDataGridView<>(columnVisibilitySelectorDataGridView);
        final ColumnFilterFooterDataGridView<Integer, MyRow> columnFilterFooterDataGridView = new ColumnFilterFooterDataGridView<>(rowSelectorColumnDataGridView);
        final ConfigSelectorDataGridView<Integer, MyRow> configSelectorDataGridView = new ConfigSelectorDataGridView<>(columnFilterFooterDataGridView, "DEFAULT");

        final DataGridView<Integer, MyRow> gridView = configSelectorDataGridView;
        gridView.setAdapter(new DataGridAdapter<>() {
            private final List<ColumnDefinition<MyRow>> columns = new ArrayList<>();
            {
                for (char c = 'a'; c <= 'z'; c++) {
                    final String ss = c + "";
                    columns.add(new DefaultColumnDefinition<>(ss, v -> v.getValue(ss), (v, s) -> v.putValue(ss, s)));
                }
                for (char c = 'A'; c <= 'Z'; c++) {
                    final String ss = c + "";
                    columns.add(new DefaultColumnDefinition<>(ss, v -> v.getValue(ss), (v, s) -> v.putValue(ss, s)));
                }
            }
            @Override public void onUnselectRow(final IsPWidget r) { r.asWidget().removeStyleName("selected-row"); }
            @Override public void onSelectRow(final IsPWidget r) { r.asWidget().addStyleName("selected-row"); }
            @Override public boolean isAscendingSortByInsertionOrder() { return false; }
            @Override public Integer getKey(final MyRow v) { return v.id; }
            @Override public List<ColumnDefinition<MyRow>> getColumnDefinitions() { return columns; }
            @Override public int compareDefault(final MyRow v1, final MyRow v2) { return 0; }
            @Override public void onCreateHeaderRow(final IsPWidget r) { r.asWidget().getParent().asWidget().addStyleName("dg-head"); }
            @Override public void onCreateFooterRow(final IsPWidget r) { r.asWidget().getParent().asWidget().addStyleName("dg-head"); }
            @Override public void onCreateRow(final IsPWidget r) {}
            @Override public boolean hasHeader() { return true; }
            @Override public boolean hasFooter() { return false; }
            @Override public IsPWidget createLoadingDataWidget() {
                final PComplexPanel div = Element.newDiv();
                div.setWidth("100%"); div.setHeight("100%");
                div.setStyleProperty("background-color", "rgba(255,255,255,.7)");
                return div;
            }
            @Override public void onCreateColumnResizer(final IsPWidget r) {}
            @Override public boolean isSelectionEnabled() { return true; }
        });
        gridView.setPollingDelayMillis(250L);

        final DataGridController<Integer, MyRow> controller = gridView.getController();
        gridView.asWidget().setHeight("500px");
        gridView.asWidget().setWidth("100%");
        gridView.asWidget().setStyleProperty("resize", "vertical");
        gridView.asWidget().addStyleName("dg-frame");
        gridView.asWidget().setStyleProperty("overflow", "hidden");

        // Controls row
        final PFlowPanel controls = Element.newPFlowPanel();
        controls.addStyleName("dg-controls");

        final PTextBox pollingDelay = Element.newPTextBox();
        pollingDelay.setPlaceholder("Polling delay ms");
        pollingDelay.setStyleProperty("width", "140px");
        final PButton changePollingDelay = Element.newPButton("Set polling delay");
        changePollingDelay.addClickHandler(e -> gridView.setPollingDelayMillis(Integer.parseInt(pollingDelay.getText().trim())));

        final PButton clearSortsButton = Element.newPButton("Clear sorts");
        clearSortsButton.addClickHandler(e -> gridView.clearSorts(false));

        controls.add(pollingDelay, changePollingDelay, clearSortsButton);
        controls.add(configSelectorDataGridView.getDecoratorWidget());
        // 52 column checkboxes: collapsed by default
        final PDisclosurePanel columnsPicker = Element.newPDisclosurePanel("Columns…");
        columnsPicker.addStyleName("dg-columns");
        columnsPicker.setContent(columnVisibilitySelectorDataGridView.getDecoratorWidget());
        controls.add(columnsPicker);

        container.add(controls);
        container.add(gridView);

        gridView.addRowAction(UISampleEntryPoint.class, new RowAction<>() {
            @Override public boolean testRow(final MyRow t, final int index) { return (index & 1) == 0; }
            @Override public void cancel(final IsPWidget row) { row.asWidget().removeStyleName("unpair-row"); }
            @Override public void apply(final IsPWidget row) { row.asWidget().addStyleName("unpair-row"); }
            @Override public boolean isActionApplied(final IsPWidget row) { return row.asWidget().hasStyleName("unpair-row"); }
        });

        controller.setBound(false);
        final int PAGE_SIZE = 200;
        final int TOTAL_ROWS = 10_000;
        final int[] loaded = {0};

        // Load first page
        for (int i = 0; i < PAGE_SIZE; i++) {
            controller.setData(createMyRow(i));
        }
        loaded[0] = PAGE_SIZE;
        controller.setBound(true);

        // "Load more" button
        final PButton loadMoreButton = Element.newPButton("Load more (" + PAGE_SIZE + " rows)");
        loadMoreButton.setStyleProperty("margin-top", "8px");
        loadMoreButton.addClickHandler(e -> {
            if (loaded[0] >= TOTAL_ROWS) {
                loadMoreButton.setEnabled(false);
                loadMoreButton.setText("All rows loaded");
                return;
            }
            controller.setBound(false);
            final int end = Math.min(loaded[0] + PAGE_SIZE, TOTAL_ROWS);
            for (int i = loaded[0]; i < end; i++) {
                controller.setData(createMyRow(i));
            }
            loaded[0] = end;
            controller.setBound(true);
            if (loaded[0] >= TOTAL_ROWS) {
                loadMoreButton.setEnabled(false);
                loadMoreButton.setText("All rows loaded (" + TOTAL_ROWS + ")");
            } else {
                loadMoreButton.setText("Load more (" + loaded[0] + "/" + TOTAL_ROWS + ")");
            }
        });
        container.add(loadMoreButton);
    }

    static class MyRow {

        final int id;
        private Map<String, String> map;
        private final String format;

        public MyRow(final int id) {
            super();
            this.id = id;
            format = String.format("%09d", id);
        }

        public void putValue(final String key, final String value) {
            if (map == null) map = new ConcurrentHashMap<>();
            map.put(key, value);
        }

        public String getValue(final String key) {
            if (map != null) {
                final String v = map.get(key);
                if (v != null) return v;
            }
            return key + format;
        }

        @Override
        public MyRow clone() {
            final MyRow model = new MyRow(id);
            if (map == null) return model;
            model.map = new HashMap<>(map);
            return model;
        }

        @Override
        public String toString() {
            return "SampleModel [id=" + id + "]";
        }

    }

    static MyRow createMyRow(final int index) {
        return new MyRow(index);
    }
}
