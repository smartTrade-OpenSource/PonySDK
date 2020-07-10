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

package com.ponysdk.sample.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PComplexPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.datagrid2.adapter.DataGridAdapter;
import com.ponysdk.core.ui.datagrid2.column.ColumnDefinition;
import com.ponysdk.core.ui.datagrid2.column.DefaultColumnDefinition;
import com.ponysdk.core.ui.datagrid2.controller.DataGridController;
import com.ponysdk.core.ui.datagrid2.data.RowAction;
import com.ponysdk.core.ui.datagrid2.view.ColumnVisibilitySelectorDataGridView;
import com.ponysdk.core.ui.datagrid2.view.ConfigSelectorDataGridView;
import com.ponysdk.core.ui.datagrid2.view.DataGridView;
import com.ponysdk.core.ui.datagrid2.view.DefaultDataGridView;
import com.ponysdk.core.ui.datagrid2.view.RowSelectorColumnDataGridView;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.sample.client.event.UserLoggedOutHandler;
import com.smarttrade.util.memory.MemoryCleaner;

public class UISampleTestPerformance implements EntryPoint, UserLoggedOutHandler {

    private static final Logger log = LoggerFactory.getLogger(UISampleTestPerformance.class);
    private PLabel mainLabel;
    int a = 0;
    private int rowCounter = 0;
    private int actionCounter = 0;
    private final Map<Character, DefaultColumnDefinition<MyRow>> colDefs = new HashMap<>();

    @Override
    public void start(final UIContext uiContext) {
        uiContext.setTerminalDataReceiver((object, instruction) -> System.err.println(object + " : " + instruction));

        createReconnectingPanel();

        mainLabel = Element.newPLabel("Can be modified by anybody : ₲ῳ₸");
        mainLabel.setAttributeLinkedToValue("data-title");
        mainLabel.setTitle("String ASCII");
        PWindow.getMain().add(mainLabel);

        testSimpleDataGridViewOnly();
        return;
    }

    private void testSimpleDataGridViewOnly() {
        final DataGridView<Integer, MyRow> simpleGridView = new DefaultDataGridView<>();
        final ColumnVisibilitySelectorDataGridView<Integer, MyRow> columnVisibilitySelectorDataGridView = new ColumnVisibilitySelectorDataGridView<>(
            simpleGridView);
        final RowSelectorColumnDataGridView<Integer, MyRow> rowSelectorColumnDataGridView = new RowSelectorColumnDataGridView<>(
            columnVisibilitySelectorDataGridView);
        final ConfigSelectorDataGridView<Integer, MyRow> configSelectorDataGridView = new ConfigSelectorDataGridView<>(
            rowSelectorColumnDataGridView, "DEFAULT");

        final DataGridView<Integer, MyRow> gridView = configSelectorDataGridView;
        gridView.setAdapter(new DataGridAdapter<Integer, MyRow>() {

            private final List<ColumnDefinition<MyRow>> columns = new ArrayList<>();

            {
                for (char c = 'a'; c <= 'z'; c++) {
                    final String ss = c + "";
                    final DefaultColumnDefinition<MyRow> colDef = new DefaultColumnDefinition<>(ss, v -> v.getValue(ss), (v, s) -> {
                        v.putValue(ss, s);
                    });
                    colDefs.put(c, colDef);
                    columns.add(colDef);
                }
            }

            @Override
            public void onUnselectRow(final IsPWidget rowWidget) {
                rowWidget.asWidget().removeStyleName("selected-row");
            }

            @Override
            public void onSelectRow(final IsPWidget rowWidget) {
                rowWidget.asWidget().addStyleName("selected-row");
            }

            @Override
            public boolean isAscendingSortByInsertionOrder() {
                return false;
            }

            @Override
            public Integer getKey(final MyRow v) {
                return v.id;
            }

            @Override
            public List<ColumnDefinition<MyRow>> getColumnDefinitions() {
                return columns;
            }

            @Override
            public int compareDefault(final MyRow v1, final MyRow v2) {
                return 0;
            }

            @Override
            public void onCreateHeaderRow(final IsPWidget rowWidget) {
                rowWidget.asWidget().getParent().asWidget().setStyleProperty("background", "aliceblue");
            }

            @Override
            public void onCreateFooterRow(final IsPWidget rowWidget) {
                rowWidget.asWidget().getParent().asWidget().setStyleProperty("background", "aliceblue");
            }

            @Override
            public void onCreateRow(final IsPWidget rowWidget) {
            }

            @Override
            public boolean hasHeader() {
                return true;
            }

            @Override
            public boolean hasFooter() {
                return false;
            }

            @Override
            public IsPWidget createLoadingDataWidget() {
                final PComplexPanel div = Element.newDiv();
                div.setWidth("100%");
                div.setHeight("100%");
                div.setStyleProperty("background-color", "#FFFFFF7F");
                return div;
            }

            @Override
            public void onCreateColumnResizer(final IsPWidget resizer) {
            }
        });

        // gridView.setPollingDelayMillis(250L);
        // gridView.setPollingDelayMillis(3600000L);
        final DataGridController<Integer, MyRow> controller = ((DefaultDataGridView<Integer, MyRow>) simpleGridView).getController();
        gridView.asWidget().setHeight("950px");
        gridView.asWidget().setWidth("1900px");
        gridView.asWidget().setStyleProperty("resize", "both");
        gridView.asWidget().setStyleProperty("overflow", "hidden");

        PWindow.getMain().add(gridView);
        PWindow.getMain().add(configSelectorDataGridView.getDecoratorWidget());

        controller.setBound(false);
        for (int i = 0; i < 10_000; i++) {
            if (i % 500_000 == 0) log.info("i: {}", i);
            controller.setData(createMyRow(i));
            rowCounter++;
        }
        controller.setBound(true);
        gridView.addRowAction(UISampleTestPerformance.class, new RowAction<>() {

            @Override
            public boolean testRow(final MyRow t, final int index) {
                return (index & 1) == 0;
            }

            @Override
            public void cancel(final IsPWidget row) {
                row.asWidget().removeStyleName("unpair-row");
            }

            @Override
            public void apply(final IsPWidget row) {
                row.asWidget().addStyleName("unpair-row");
            }

            @Override
            public boolean isActionApplied(final IsPWidget row) {
                return row.asWidget().hasStyleName("unpair-row");
            }
        });

        final TestAction addDataAction = (i) -> {
            i = rowCounter++;
            controller.setData(createMyRow(i));
        };

        final TestAction removeDataAction = (i) -> {
            i = --rowCounter;
            controller.removeData(i);
        };

        final TestAction updateDataAction_Version_1 = (i) -> {
            i %= rowCounter;
            if (i == 0) {
                actionCounter++;
            }
            final MyRow v = createMyRow(i);

            for (char column = 'a'; column < 'e'; column++) {
                if (column == 'a') {
                    int updatedValue = i;
                    for (int j = 0; j < actionCounter; j++) {
                        updatedValue *= 0.99;
                    }
                    v.putValue("a", String.format("a" + "%09d", updatedValue));
                } else if (column == 'b') {
                    int updatedValue = i;
                    for (int j = 0; j < actionCounter; j++) {
                        updatedValue *= 1.1;
                    }
                    v.putValue("b", String.format("b" + "%09d", updatedValue));
                } else if (column == 'c') {
                    int updatedValue = i;
                    for (int j = 0; j < actionCounter; j++) {
                        updatedValue *= 1.15;
                    }
                    v.putValue("c", String.format("c" + "%09d", updatedValue));
                } else if (column == 'd') {
                    int updatedValue = i;
                    for (int j = 0; j < actionCounter; j++) {
                        updatedValue *= 1.2;
                    }
                    v.putValue("d", String.format("d" + "%09d", updatedValue));
                }
            }
            controller.setData(v);
        };

        final TestAction updateDataAction_Version_2 = (i) -> {
            i %= rowCounter;
            if (i == 0) {
                actionCounter++;
            }
            final MyRow v = createMyRow(i);
            int updatedValue = i;
            updatedValue *= Math.random() + 1;
            v.putValue("b", String.format("b" + "%09d", updatedValue));
            controller.setData(v);
        };

        testPerformanceAfterWarmUp(rowCounter, 140, addDataAction, removeDataAction);

        // FIXME : somtimes the update action throws a concurrency exception
        // Test 1
        // addFilter(controller);
        // addSort(controller);
        // testPerformanceBench(rowCounter, 60, updateDataAction_Version_2);

        // Test 2

        // testPerformanceBench(rowCounter, 140, addDataAction);

        // Test 3
        // testPerformanceBench(rowCounter, 1, removeDataAction);
    }

    private void addFilter(final DataGridController<Integer, MyRow> controller) {
        controller.setFilter(0, "", (row) -> {
            if (row.getId() % 2 == 0) return true;
            return false;
        }, true);
    }

    private void addSort(final DataGridController<Integer, MyRow> controller) {
        // controller.addSort(colDefs.get('a'), true);
        // controller.addSort(colDefs.get('b'), false);
        // controller.addSort(colDefs.get('c'), true);
        controller.addSort(colDefs.get('b'), true);
    }

    private void testPerformanceAfterWarmUp(final int nbOfActionPerIteration, final int nbOfIterations, final TestAction addDataAction,
                                            final TestAction removeDataAction) {

        final Runnable addDataWarmUp = () -> {
            System.gc();
            for (int i = 0; i < 2; i++) {
                log.info("START testPerformanceWarmUp addData");
                int index = 0;
                while (index < nbOfActionPerIteration * nbOfIterations)
                    addDataAction.execute(index++);

                log.info("START testPerformanceWarmUp removeData");
                index = rowCounter;
                while (index > 10_000) {
                    removeDataAction.execute(index--);
                }
            }
            try {
                log.info("\n\n\n\n\n\n START Recording (4min record) ..... \n\n\n\n\n\n");
                Thread.sleep(20_000);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            testPerformanceBench(rowCounter, nbOfIterations, addDataAction);
        };
        final Thread thread = new Thread(addDataWarmUp, "Test thread");
        thread.start();
    }

    /**
     * This function tests an action by measuring the duration of the execution of this action.
     * It is destined to be called when the wanted action will dure all the test. The objectif is
     * not to measure the performance of an action alone but to compare it with another optimal case.
     *
     * @param nbOfActionPerIteration
     *            Define how many actions you need to perform, ex: how many updates if you want to update your rows.
     *            The maximum logical number is your rowCount in this case
     * @param nbOfIterations
     *            Define how many iterations you want to perform, ex: how many times you want to update each row.
     *            The greater this value the longer the test
     * @param testAction
     *            Lambda expression that defines what you will execute on a single row
     */
    private void testPerformanceBench(final int nbOfActionPerIteration, final int nbOfIterations, final TestAction testAction) {

        // final Runnable testPerfBench = () -> {
        // log.info("\nSTART testPerformanceBench (waiting 1 minute)");
        // try {
        // Thread.sleep(60000);
        // } catch (final Exception e) {
        // e.printStackTrace();
        // }
        log.info("START testPerformanceBench");
        int index = 0;
        System.gc();
        final long time_before = System.nanoTime();
        while (index < nbOfActionPerIteration * nbOfIterations)
            testAction.execute(index++);
        final long time_after = System.nanoTime();
        final long duration = time_after - time_before;
        log.info("TestDuration = " + duration);
        MemoryCleaner.cleanMemory(true, false);
        System.gc();
        // log.info("STOP testPerformanceBench (waiting 1 minute)");
        // try {
        // Thread.sleep(60000);
        // } catch (final Exception e) {
        // e.printStackTrace();
        // }
        log.info("STOP testPerformanceBench");
        // };
        // final Thread thread = new Thread(testPerfBench, "Test thread");
        // thread.start();
    }

    private static class MyRow {

        private final int id;
        private Map<String, String> map;
        private final String format;

        public MyRow(final int id) {
            super();
            this.id = id;
            format = toto(id);
        }

        private static String toto(final int id) {
            if (id < 10) return "00000000" + id;
            if (id < 100) return "0000000" + id;
            if (id < 1000) return "000000" + id;
            if (id < 10000) return "00000" + id;
            if (id < 100000) return "0000" + id;
            if (id < 1000000) return "000" + id;
            if (id < 10000000) return "00" + id;
            if (id < 100000000) return "0" + id;
            return Integer.toString(id);
        }

        int getId() {
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
        return new MyRow(index);
    }

    private void createReconnectingPanel() {
        final PSimplePanel reconnectionPanel = Element.newPSimplePanel();
        reconnectionPanel.setAttribute("id", "reconnection");
        final PSimplePanel reconnectingPanel = Element.newPSimplePanel();
        reconnectingPanel.setAttribute("id", "reconnecting");
        reconnectionPanel.setWidget(reconnectingPanel);
        PWindow.getMain().add(reconnectionPanel);
    }

    @Override
    public void onUserLoggedOut(final UserLoggedOutEvent event) {
        UIContext.get().close();
    }
}

interface TestAction {

    void execute(int id);
}
