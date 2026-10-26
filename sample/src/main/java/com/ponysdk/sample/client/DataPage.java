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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PGrid;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PTree;
import com.ponysdk.core.ui.basic.PTreeItem;
import com.ponysdk.core.ui.grid.AbstractGridWidget;
import com.ponysdk.core.ui.grid.GridTableWidget;
import com.ponysdk.core.ui.list.DataGridColumnDescriptor;
import com.ponysdk.core.ui.list.refreshable.Cell;
import com.ponysdk.core.ui.list.refreshable.RefreshableDataGrid;
import com.ponysdk.core.ui.list.renderer.cell.CellRenderer;

/** Data: trees and tables whose content is pushed from the server. */
final class DataPage {

    private DataPage() {
    }

    // ── Tab: Data ────────────────────────────────────────────────────────────

    static PFlowPanel build(final List<PScheduler.UIRunnable> schedulers) {
        final PFlowPanel p = section();

        pageTitle(p, "Data", "Trees and tables whose content is pushed from the server.");

        p.add(exampleCard("PTree", "Items added, inserted and renamed from Java; a widget as an item.", createTree(),
            Source.code(DataPage.class, "tree")));

        final PScrollPanel gridFrame = Element.newPScrollPanel();
        gridFrame.addStyleName("lay-frame");
        gridFrame.addStyleName("data-table");
        gridFrame.setHeight("280px");
        gridFrame.setWidget(createGrid(schedulers));
        p.add(exampleCard("RefreshableDataGrid", "50 rows refreshed every 200 ms by a server task: only the changed cells travel.",
            gridFrame, Source.code(DataPage.class, "refreshablegrid")));

        // region flextable
        final PFlexTable flex = Element.newPFlexTable();
        flex.addStyleName("data-table");
        final String[][] fx = { { "Pair", "Bid", "Ask", "Spread" }, { "EUR/USD", "1.0851", "1.0853", "0.2" },
                                { "GBP/USD", "1.2710", "1.2713", "0.3" }, { "USD/JPY", "149.21", "149.24", "0.3" } };
        for (int row = 0; row < fx.length; row++) {
            for (int col = 0; col < fx[row].length; col++) {
                final PLabel cell = Element.newPLabel(fx[row][col]);
                if (row == 0) cell.addStyleName("data-th");
                flex.setWidget(row, col, cell);
            }
        }
        // endregion
        p.add(exampleCard("PFlexTable", "A table whose cells are widgets, set by row and column.", flex,
            Source.code(DataPage.class, "flextable")));

        // region grid
        final PGrid grid = Element.newPGrid(2, 3);
        grid.addStyleName("data-table");
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) grid.setWidget(row, col, chip("r" + row + " · c" + col));
        }
        // endregion
        p.add(exampleCard("PGrid 2×3", "A table of a fixed size.", grid, Source.code(DataPage.class, "grid")));

        return p;
    }

    static final class Data {

        protected Integer key;
        protected String value;

        public Data(final Integer key, final String value) {
            this.key = key;
            this.value = value;
        }
    }

    static RefreshableDataGrid<Integer, Data> createGrid(final List<PScheduler.UIRunnable> schedulers) {
        // region refreshablegrid
        final AbstractGridWidget listView = new GridTableWidget();
        listView.setStyleProperty("table-layout", "fixed");
        final RefreshableDataGrid<Integer, Data> grid = new RefreshableDataGrid<>(listView);

        final DataGridColumnDescriptor<Data, Data> columnDescriptor1 = new DataGridColumnDescriptor<>();
        columnDescriptor1.setCellRenderer(new CellRenderer<Data, PLabel>() {

            @Override
            public void update(final Data value, final Cell<Data, PLabel> current) {
                current.getWidget().setText(value.key + "");
            }

            @Override
            public PLabel render(final int row, final Data value) {
                return Element.newPLabel(value.key + "");
            }
        });
        columnDescriptor1.setHeaderCellRenderer(() -> Element.newPLabel("Key"));
        columnDescriptor1.setValueProvider(Function.identity());
        grid.addDataGridColumnDescriptor(columnDescriptor1);

        for (int i = 0; i < 6; i++) {
            final String header = "Value " + (i + 1);

            final DataGridColumnDescriptor<Data, Data> columnDescriptor3 = new DataGridColumnDescriptor<>();
            columnDescriptor3.setCellRenderer(new CellRenderer<Data, PLabel>() {

                @Override
                public void update(final Data value, final Cell<Data, PLabel> current) {
                    current.getWidget().setText(value.value);
                }

                @Override
                public PLabel render(final int row, final Data value) {
                    return Element.newPLabel(value.value);
                }
            });
            columnDescriptor3.setHeaderCellRenderer(() -> Element.newPLabel(header));
            columnDescriptor3.setValueProvider(Function.identity());
            grid.addDataGridColumnDescriptor(columnDescriptor3);
        }

        grid.setData(0, 1, new Data(1, "AA"));
        grid.setData(1, 2, new Data(2, "BB"));
        final Data data = new Data(3, "CC");
        grid.setData(2, 3, data);

        final AtomicInteger i = new AtomicInteger();
        schedulers.add(PScheduler.scheduleWithFixedDelay(() -> {
            if (idle()) return;
            for (int key = 1; key <= 50; key++) {
                grid.setData(key - 1, key, new Data(key, "" + i.incrementAndGet()));
            }
        }, Duration.ofMillis(500), Duration.ofMillis(200)));
        // endregion
        return grid;
    }

    static final PTree createTree() {
        // region tree
        final PTree tree = Element.newPTree();

        tree.addStyleName("data-tree");
        final PTreeItem fx = tree.add("FX desk");
        fx.add("EUR/USD");
        fx.add(0, Element.newPTreeItem("GBP/USD"));   // inserted before
        fx.add("USD/JPY");
        fx.setState(true);

        final PTreeItem rates = Element.newPTreeItem("Rates desk");
        final PTreeItem bund = rates.add(Element.newPTreeItem());
        bund.setText("Bund 10Y");                    // text set after creation
        rates.add(Element.newPTreeItem(Element.newPLabel("UST 10Y (widget item)")));
        tree.add(rates);
        rates.setState(true);

        bund.setSelected(true);
        // endregion
        return tree;
    }
}
