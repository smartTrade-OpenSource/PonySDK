/*
 * Copyright (c) 2020 PonySDK
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

package com.ponysdk.core.ui.datagrid2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
import com.ponysdk.core.ui.datagrid2.controller.DefaultDataGridController;
import com.ponysdk.core.ui.datagrid2.data.ViewLiveData;
import com.ponysdk.core.ui.datagrid2.view.DataGridView;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridView;

/**
 * @author mabbas
 */

class DataGridUnitTest {

    private static DataGridView<Integer, MyRow> simpleGridView;
    private static DataGridController<Integer, MyRow> controller;
    private static final Map<Character, DefaultColumnDefinition<MyRow>> colDefs = new HashMap<>();
    private static List<DefaultDataGridView<Integer, MyRow>.Row> rows;
    MyRow myRow;

    //    @BeforeAll
    //    static void createDataGrid() {
    //        System.out.println("In method : createDataGrid - getting in");
    //        System.out.println("In method : createDataGrid - before view instanciation");
    //        try {
    //            simpleGridView = new DefaultDataGridView<>();
    //        } catch (final Exception e) {
    //            e.printStackTrace();
    //        }
    //        System.out.println("In method : createDataGrid - after view instanciation");
    //        final ColumnVisibilitySelectorDataGridView<Integer, MyRow> columnVisibilitySelectorDataGridView = new ColumnVisibilitySelectorDataGridView<>(
    //            simpleGridView);
    //        final RowSelectorColumnDataGridView<Integer, MyRow> rowSelectorColumnDataGridView = new RowSelectorColumnDataGridView<>(
    //            columnVisibilitySelectorDataGridView);
    //        final ConfigSelectorDataGridView<Integer, MyRow> configSelectorDataGridView = new ConfigSelectorDataGridView<>(
    //            rowSelectorColumnDataGridView, "DEFAULT");
    //
    //        final DataGridView<Integer, MyRow> gridView = configSelectorDataGridView;
    //        rows = ((DefaultDataGridView<Integer, MyRow>) simpleGridView).getRows();
    //        gridView.setAdapter(new DataGridAdapter<Integer, MyRow>() {
    //
    //            private final List<ColumnDefinition<MyRow>> columns = new ArrayList<>();
    //
    //            {
    //                for (char c = 'a'; c <= 'z'; c++) {
    //                    final String ss = c + "";
    //                    final DefaultColumnDefinition<MyRow> colDef = new DefaultColumnDefinition<>(ss, v -> v.getValue(ss), (v, s) -> {
    //                        v.putValue(ss, s);
    //                    });
    //                    colDefs.put(c, colDef);
    //                    columns.add(colDef);
    //                }
    //            }
    //
    //            @Override
    //            public void onUnselectRow(final IsPWidget rowWidget) {
    //                rowWidget.asWidget().removeStyleName("selected-row");
    //            }
    //
    //            @Override
    //            public void onSelectRow(final IsPWidget rowWidget) {
    //                rowWidget.asWidget().addStyleName("selected-row");
    //            }
    //
    //            @Override
    //            public boolean isAscendingSortByInsertionOrder() {
    //                return false;
    //            }
    //
    //            @Override
    //            public Integer getKey(final MyRow v) {
    //                return v.id;
    //            }
    //
    //            @Override
    //            public List<ColumnDefinition<MyRow>> getColumnDefinitions() {
    //                return columns;
    //            }
    //
    //            @Override
    //            public int compareDefault(final MyRow v1, final MyRow v2) {
    //                return 0;
    //            }
    //
    //            @Override
    //            public void onCreateHeaderRow(final IsPWidget rowWidget) {
    //                rowWidget.asWidget().getParent().asWidget().setStyleProperty("background", "aliceblue");
    //            }
    //
    //            @Override
    //            public void onCreateFooterRow(final IsPWidget rowWidget) {
    //                rowWidget.asWidget().getParent().asWidget().setStyleProperty("background", "aliceblue");
    //            }
    //
    //            @Override
    //            public void onCreateRow(final IsPWidget rowWidget) {
    //            }
    //
    //            @Override
    //            public boolean hasHeader() {
    //                return true;
    //            }
    //
    //            @Override
    //            public boolean hasFooter() {
    //                return false;
    //            }
    //
    //            @Override
    //            public IsPWidget createLoadingDataWidget() {
    //                final PComplexPanel div = Element.newDiv();
    //                div.setWidth("100%");
    //                div.setHeight("100%");
    //                div.setStyleProperty("background-color", "#FFFFFF7F");
    //                return div;
    //            }
    //
    //            @Override
    //            public void onCreateColumnResizer(final IsPWidget resizer) {
    //            }
    //        });
    //
    //        gridView.setPollingDelayMillis(250L);
    //        controller = ((DefaultDataGridView<Integer,MyRow>)simpleGridView).getController();
    //        gridView.asWidget().setHeight("950px");
    //        gridView.asWidget().setWidth("1900px");
    //        gridView.asWidget().setStyleProperty("resize", "both");
    //        gridView.asWidget().setStyleProperty("overflow", "hidden");
    //
    //        PWindow.getMain().add(gridView);
    //        PWindow.getMain().add(configSelectorDataGridView.getDecoratorWidget());
    //
    //        controller.setBound(false);
    //        for (int i = 0; i < 1_000; i++) {
    //            controller.setData(createMyRow(i));
    //        }
    //        controller.setBound(true);
    //        gridView.addRowAction(DataGridUnitTest.class, new RowAction<>() {
    //
    //            @Override
    //            public boolean testRow(final MyRow t, final int index) {
    //                return (index & 1) == 0;
    //            }
    //
    //            @Override
    //            public void cancel(final IsPWidget row) {
    //                row.asWidget().removeStyleName("unpair-row");
    //            }
    //
    //            @Override
    //            public void apply(final IsPWidget row) {
    //                row.asWidget().addStyleName("unpair-row");
    //            }
    //        });
    //        System.out.println("In method : createDataGrid - getting out");
    //    }

    @BeforeAll
    static void beforeAll() {
        controller = new DefaultDataGridController<>();
    }

    private static class MyRow {

        private final int id;
        private Map<String, String> map;
        private final String format;

        public MyRow(final int id) {
            super();
            System.out.println("In method : MyRow");
            this.id = id;
            format = String.format("%09d", id);
        }

        public int getId() {
            return id;
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

    private static MyRow createMyRow(final int index) {
        System.out.println("In method : createMyRow");
        return new MyRow(index);
    }

    @Test
    void addData() {
        System.out.println("In method : addData");
        final Object object = PWindow.getMain().getData();
        System.out.println();
        //        myRow = createMyRow(1000);
        //        controller.setData(myRow);
        //        final DataGridSnapshot viewStateSnapshot = new DataGridSnapshot(0, 10, new HashMap<>(), new HashSet<>());
        //        final ViewLiveData<MyRow> viewLiveData = new ViewLiveData<>(0, 10, new ArrayList<DefaultRow<MyRow>>(),
        //            new DataGridSnapshot(viewStateSnapshot));
        //        //        final Consumer<ViewLiveData<MyRow>> consumer = PScheduler.delegate(this::updateView);
        //        final Consumer<DefaultDataGridController<Integer, MyRow>.MySupplier> consumer = PScheduler.delegate(this::updateView);
        //        controller.prepareLiveDataOnScreen(viewLiveData, consumer);
    }

    //    private boolean updateView(final ViewLiveData<MyRow> result) {
    private synchronized void updateView(final DefaultDataGridController<Integer, MyRow>.MySupplier supplierRresult) {
        final ViewLiveData<MyRow> result = supplierRresult.get();
        final int expectedID = myRow.getId();
        final int actualID = result.liveData.get(0).getID();
        assertEquals(expectedID, actualID);
    }

    //    @Test
    //    void removeData() {
    //        System.out.println("In method : removeData");
    //        controller.removeData(1000);
    //    }
    //
    //    @Test
    //    void updateData() {
    //        System.out.println("In method : updateData");
    //        final MyRow v = createMyRow(999);
    //        v.putValue("a", String.format("a" + "%09d", 0.5));
    //        controller.setData(v);
    //    }
    //
    //    @Test
    //    void sortData() {
    //        System.out.println("In method : sortData");
    //        controller.addSort(colDefs.get('a'), true);
    //    }
    //
    //    @Test
    //    void filterData() {
    //        System.out.println("In method : filterData");
    //        controller.setFilter(0, "", (row) -> {
    //            if (row.getId() % 2 == 0) return true;
    //            return false;
    //        }, true);
    //    }
}
