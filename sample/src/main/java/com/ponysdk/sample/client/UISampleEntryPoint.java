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

import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.server.concurrent.PScheduler.UIRunnable;
import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PKeyUpEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpHandler;
import com.ponysdk.core.ui.datagrid.ColumnDescriptor;
import com.ponysdk.core.ui.datagrid.DataGrid;
import com.ponysdk.core.ui.datagrid.impl.PLabelCellRenderer;
import com.ponysdk.core.ui.grid.AbstractGridWidget;
import com.ponysdk.core.ui.grid.GridTableWidget;
import com.ponysdk.core.ui.list.DataGridColumnDescriptor;
import com.ponysdk.core.ui.list.refreshable.Cell;
import com.ponysdk.core.ui.list.refreshable.RefreshableDataGrid;
import com.ponysdk.core.ui.list.renderer.cell.CellRenderer;
import com.ponysdk.core.ui.list.renderer.header.HeaderCellRenderer;
import com.ponysdk.core.ui.list.valueprovider.IdentityValueProvider;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.core.ui.model.PKeyCodes;
import com.ponysdk.core.ui.rich.PToolbar;
import com.ponysdk.core.ui.rich.PTwinListBox;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.sample.client.event.UserLoggedOutHandler;
import com.ponysdk.sample.client.page.addon.LoggerAddOn;
import com.ponysdk.sample.client.page.addon.SelectizeAddon;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class UISampleEntryPoint implements EntryPoint, UserLoggedOutHandler {

    private PLabel child2;

    // HighChartsStackedColumnAddOn highChartsStackedColumnAddOn;
    int a= 0;
    @Override
    public void start(final UIContext uiContext) {
        uiContext.setClientDataOutput((object, instruction) -> System.err.println(object + " : " + instruction));

        final PWindow a = Element.newPWindow(null, "Window 2", "resizable=yes,location=0,status=0,scrollbars=0");
        a.open();

        final PLabel b = Element.newPLabel();
        a.add(b);

        final AtomicInteger i = new AtomicInteger();
        PScheduler.scheduleWithFixedDelay(() -> {
            b.setText(i.incrementAndGet() + "");
        } , Duration.ofSeconds(1), Duration.ofSeconds(1));


        DataGrid<Integer> grid = new DataGrid();


        for (int cpt = 0; cpt < 20; cpt++) {
            final ColumnDescriptor<Integer> column = new ColumnDescriptor<>();
            final PAnchor anchor = Element.newPAnchor("Header " + i.incrementAndGet());
            anchor.addClickHandler(e -> grid.removeColumn(column));
            column.setCellRenderer(new PLabelCellRenderer<>(from -> a + ""));
            column.setHeaderRenderer(() -> anchor);
            grid.addColumnDescriptor(column);
        }
        PTextBox textBox = Element.newPTextBox();

        PButton add = Element.newPButton("add");
        add.addClickHandler(e -> {
            grid.setData(Integer.valueOf(textBox.getText()));
            /**final ColumnDescriptor<Integer> column = new ColumnDescriptor<>();
            final PAnchor anchor = new PAnchor("Header " + id.incrementAndGet());
            anchor.addClickHandler(click -> grid.removeColumn(column));
            column.setCellRenderer(new PLabelCellRenderer<>(from -> (int) (Math.random() * 1000) + ""));
            column.setHeaderRenderer(() -> anchor);
            grid.addColumnDescriptor(column);
             **/
        });

        PWindow.getMain().add(add);

        PWindow.getMain().add(textBox);
        PWindow.getMain().add(grid);

    /**
        PScheduler.scheduleAtFixedRate(() -> {
            grid.setData((int) (Math.random() * 50));
            grid.removeData((int) (Math.random() * 50));
            grid.removeColumn(grid.getColumns().get((int) (Math.random() * grid.getColumns().size() - 1)));

            final ColumnDescriptor<Integer> column = new ColumnDescriptor<>();
            final PAnchor anchor = new PAnchor("Header " + id.incrementAndGet());
            anchor.addClickHandler(click -> grid.removeColumn(column));
            column.setCellRenderer(new PLabelCellRenderer<>(from -> (int) (Math.random() * 1000) + ""));
            column.setHeaderRenderer(() -> anchor);
            grid.addColumnDescriptor(column);
        }, Duration.ofMillis(2000));
**/

        //final PWindow a = new PWindow(null, "Window 2", "resizable=yes,location=0,status=0,scrollbars=0");
        //a.open();

        //final PLabel b = new PLabel();
        //a.add(b);

        //        final AtomicInteger i = new AtomicInteger();
        //        PScheduler.scheduleWithFixedDelay(() -> {
        //            b.setText(i.incrementAndGet() + "");
        //        } , Duration.ofSeconds(1), Duration.ofSeconds(1));
        //
        //        testUIDelegator();

        //createGrid();

        if (true) return;

        // final LoggerAddOn addon = createPAddOn();
        // addon.attach(PWindow.getMain());

        // System.err.println(addon);

        // final PElementAddOn elementAddOn = new PElementAddOn();
        // elementAddOn.setInnerText("Coucou");
        // flowPanel.add(elementAddOn);

        // highChartsStackedColumnAddOn = new HighChartsStackedColumnAddOn();
        // PWindow.getMain().add(highChartsStackedColumnAddOn);
        // highChartsStackedColumnAddOn.setSeries("");

        // final HighChartsStackedColumnAddOn h2 = new
        // HighChartsStackedColumnAddOn();
        // a.add(h2);
        // h2.setSeries("");
        // final PElementAddOn elementAddOn2 = new PElementAddOn();
        // elementAddOn2.setInnerText("Coucou dans window");
        // a.add(elementAddOn2);

        final SelectizeAddon selectizeAddon = new SelectizeAddon();
        selectizeAddon.text("test");
        PWindow.getMain().add(selectizeAddon);

        final PRadioButton buy = Element.newPRadioButton("Buy");
        final PRadioButton sell = Element.newPRadioButton("Sell");
        buy.addValueChangeHandler((event) -> selectizeAddon.selectBuy(event.getValue()));
        sell.addValueChangeHandler((event) -> selectizeAddon.selectSell(event.getValue()));

        PWindow.getMain().add(buy);
        PWindow.getMain().add(sell);

        final PElement label1 = Element.newDiv();
        final PElement label2 = Element.newDiv();
        final PElement label3 = Element.newDiv();
        final PElement label4 = Element.newDiv();
        final PElement label5 = Element.newDiv();
        final PElement label6 = Element.newDiv();
        final PElement label7 = Element.newDiv();
        final PElement label8 = Element.newDiv();
        final PElement label9 = Element.newDiv();
        final PElement label10 = Element.newDiv();
        final PElement label11 = Element.newDiv();
        final PElement label12 = Element.newDiv();
        final PElement label13 = Element.newDiv();
        final PElement label14 = Element.newDiv();
        final PElement label15 = Element.newDiv();
        final PElement label16 = Element.newDiv();
        final PElement label17 = Element.newDiv();
        final PElement label18 = Element.newDiv();
        final PElement label19 = Element.newDiv();
        final PElement label20 = Element.newDiv();

        // flowPanel.add(label1);
        // flowPanel.add(label2);
        // flowPanel.add(label3);
        // flowPanel.add(label4);
        // flowPanel.add(label5);
        // flowPanel.add(label6);
        // flowPanel.add(label7);
        // flowPanel.add(label8);
        // flowPanel.add(label9);
        // flowPanel.add(label10);
        // flowPanel.add(label11);
        // flowPanel.add(label12);
        // flowPanel.add(label13);
        // flowPanel.add(label14);
        // flowPanel.add(label15);
        // flowPanel.add(label16);
        // flowPanel.add(label17);
        // flowPanel.add(label18);
        // flowPanel.add(label19);
        // flowPanel.add(label20);

        PScheduler.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                label1.setInnerHTML("<div style='color:red'>" + "Test avec des accents : &��{" + "</div>");
                label2.setInnerHTML("<div style='color:blue'>" + System.nanoTime() + "</div>");
                label3.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label4.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label5.setInnerHTML("<div style='color:blue'>" + System.nanoTime() + "</div>");
                label6.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label7.setInnerHTML("<div style='color:orange'>" + System.nanoTime() + "</div>");
                label8.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label9.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label10.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label11.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label12.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label13.setInnerHTML("<div style='color:green'>" + System.nanoTime() + "</div>");
                label14.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label15.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label16.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label17.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label18.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label19.setInnerHTML("<div style='color:red'>" + System.nanoTime() + "</div>");
                label20.setInnerHTML("<div style='color:yellow'>" + System.nanoTime() + "</div>");
            }
        }, Duration.ofMillis(5000));

        final PWindow window = Element.newPWindow(null, "a", null);
        window.open();
        final PWindow window1 = Element.newPWindow(null, "b", null);
        window1.open();
        final PWindow window2 = Element.newPWindow(null, "c", null);
        window2.open();
        final PWindow window3 = Element.newPWindow(null, "d", null);
        window3.open();

        final PFlowPanel boxContainer = Element.newPFlowPanel();

        PScript.execute(PWindow.getMain(), "alert('coucou Main');");

        final PWindow w1 = createWindow1();
        // final PWindow w2 = createWindow2();
        // final PWindow w3 = createWindow3();

        // boxContainer.add(new PHistory());
        // boxContainer.add(new PNotificationManager());
        // boxContainer.add(new PSuggestBox());

        boxContainer.add(createBlock(createAbsolutePanel()));
        // boxContainer.add(createPAddOn().asWidget());
        boxContainer.add(Element.newPAnchor());
        boxContainer.add(Element.newPAnchor("Anchor"));
        boxContainer.add(Element.newPAnchor("Anchor 1", "anchor2"));
        boxContainer.add(Element.newPButton());
        boxContainer.add(createButton());
        boxContainer.add(Element.newPCheckBox());
        boxContainer.add(Element.newPCheckBox("Checkbox"));
        final PCookies cookies = new PCookies();
        cookies.setCookie("Cook", "ies");
        boxContainer.add(createDateBox());
        boxContainer.add(Element.newPDateBox(new SimpleDateFormat("dd/MM/yyyy")));
        boxContainer.add(Element.newPDateBox(Element.newPDatePicker(), new SimpleDateFormat("yyyy/MM/dd")));
        boxContainer.add(Element.newPDatePicker());
        boxContainer.add(Element.newPDecoratedPopupPanel(PWindow.getMain().getID(), false));
        boxContainer.add(Element.newPDecoratedPopupPanel(PWindow.getMain().getID(), true));
        boxContainer.add(Element.newPDecoratorPanel());
        boxContainer.add(Element.newPDialogBox(PWindow.getMain().getID()));
        boxContainer.add(Element.newPDialogBox(PWindow.getMain().getID(), true));
        boxContainer.add(Element.newPDisclosurePanel("Disclosure"));
        boxContainer.add(createDockLayoutPanel());
        boxContainer.add(Element.newA());
        boxContainer.add(Element.newPFileUpload());
        boxContainer.add(Element.newPFlexTable());
        boxContainer.add(createPFlowPanel());
        boxContainer.add(Element.newPFocusPanel());
        boxContainer.add(Element.newPGrid());
        boxContainer.add(Element.newPGrid(2, 3));
        boxContainer.add(Element.newPHeaderPanel());
        // boxContainer.add(new PHistory());
        boxContainer.add(Element.newPHorizontalPanel());
        boxContainer.add(Element.newPHTML());
        boxContainer.add(Element.newPHTML("Html"));
        boxContainer.add(Element.newPHTML("Html 1", true));
        boxContainer.add(Element.newPImage()); // FIXME Test with image
        boxContainer.add(Element.newPLabel());
        boxContainer.add(Element.newPLabel("Label"));
        boxContainer.add(Element.newPLayoutPanel());
        boxContainer.add(Element.newPListBox());
        boxContainer.add(createListBox());
        boxContainer.add(Element.newPMenuBar());
        boxContainer.add(createMenu());
        // boxContainer.add(new PNotificationManager());
        boxContainer.add(Element.newPPasswordTextBox());
        boxContainer.add(Element.newPPasswordTextBox("Password"));

        boxContainer.add(Element.newPPopupPanel(PWindow.getMain().getID()));
        boxContainer.add(Element.newPPopupPanel(PWindow.getMain().getID(), true));

        boxContainer.add(Element.newPPushButton(Element.newPImage())); // FIXME Test with
        // image

        boxContainer.add(Element.newPRadioButton("RadioLabel"));
        boxContainer.add(Element.newPRadioButton("RadioLabel"));
        final PRichTextArea richTextArea = Element.newPRichTextArea();
        boxContainer.add(richTextArea);
        boxContainer.add(Element.newPRichTextToolbar(richTextArea));
        boxContainer.add(Element.newPScrollPanel());
        boxContainer.add(Element.newPSimpleLayoutPanel());
        boxContainer.add(Element.newPSimplePanel());
        boxContainer.add(Element.newPSplitLayoutPanel());
        boxContainer.add(createStackLayoutPanel());
        // boxContainer.add(new PSuggestBox());
        boxContainer.add(createTabLayoutPanel());
        boxContainer.add(Element.newPTabPanel());
        boxContainer.add(Element.newPTextArea());
        boxContainer.add(createPTextBox());
        boxContainer.add(new PToolbar());
        boxContainer.add(createTree());
        boxContainer.add(new PTwinListBox<>());
        boxContainer.add(Element.newPVerticalPanel());

        PWindow.getMain().add(boxContainer);

        child2 = Element.newPLabel("Label2");
        child2.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                System.out.println("bbbbb");
            }
        });
        boxContainer.add(child2);

        try {
            w1.add(child2);
        } catch (final Exception e) {

        }

        boxContainer.add(Element.newPLabel("Label3"));

        final PLabel label = Element.newPLabel("Label4");

        try {
            w1.add(label);

            boxContainer.add(label);
        } catch (final Exception e) {

        }

        // uiContext.getHistory().newItem("", false);
    }

    private class Data {

        protected Integer key;
        protected String value;

        public Data(final Integer key, final String value) {
            this.key = key;
            this.value = value;
        }
    }

    private void createGrid() {
        final AbstractGridWidget listView = new GridTableWidget();
        listView.setStyleProperty("table-layout", "fixed");
        final RefreshableDataGrid<Integer, Data> grid = new RefreshableDataGrid<>(listView);
        PWindow.getMain().add(grid);

        final DataGridColumnDescriptor<Data, Data> columnDescriptor1 = new DataGridColumnDescriptor<>();
        columnDescriptor1.setCellRenderer(new CellRenderer<UISampleEntryPoint.Data, PLabel>() {

            @Override
            public void update(final Data value, final Cell<Data, PLabel> current) {
                current.getWidget().setText(value.key + "");
            }

            @Override
            public PLabel render(final int row, final Data value) {
                return Element.newPLabel(value.key + "");
            }
        });
        columnDescriptor1.setHeaderCellRenderer(new HeaderCellRenderer() {

            @Override
            public IsPWidget render() {
                return Element.newPLabel("A");
            }
        });
        columnDescriptor1.setValueProvider(new IdentityValueProvider<>());
        grid.addDataGridColumnDescriptor(columnDescriptor1);
        grid.addDataGridColumnDescriptor(columnDescriptor1);
        grid.addDataGridColumnDescriptor(columnDescriptor1);
        grid.addDataGridColumnDescriptor(columnDescriptor1);
        grid.addDataGridColumnDescriptor(columnDescriptor1);
        grid.addDataGridColumnDescriptor(columnDescriptor1);
        grid.addDataGridColumnDescriptor(columnDescriptor1);

        for (int i = 0; i < 40; i++) {

            final DataGridColumnDescriptor<Data, Data> columnDescriptor3 = new DataGridColumnDescriptor<>();
            columnDescriptor3.setCellRenderer(new CellRenderer<UISampleEntryPoint.Data, PLabel>() {

                @Override
                public void update(final Data value, final Cell<Data, PLabel> current) {
                    current.getWidget().setText(value.value);
                }

                @Override
                public PLabel render(final int row, final Data value) {
                    return Element.newPLabel(value.value);
                }
            });
            columnDescriptor3.setHeaderCellRenderer(new HeaderCellRenderer() {

                @Override
                public IsPWidget render() {
                    return Element.newPLabel("B");
                }
            });
            columnDescriptor3.setValueProvider(new IdentityValueProvider<>());
            grid.addDataGridColumnDescriptor(columnDescriptor3);
        }

        grid.setData(0, 1, new Data(1, "AA"));
        grid.setData(1, 2, new Data(2, "BB"));
        final Data data = new Data(3, "CC");
        grid.setData(2, 3, data);

        final AtomicInteger i = new AtomicInteger();
        PScheduler.scheduleWithFixedDelay(() -> {
            for (int key = 1; key < 50; key++) {
                grid.setData(key - 1, key, new Data(key, "" + i.incrementAndGet()));
            }
        }, Duration.ofSeconds(1), Duration.ofMillis(100));
    }

    private void testUIDelegator() {
        final PLabel a = Element.newPLabel();
        PWindow.getMain().add(a);
        final AtomicInteger ai = new AtomicInteger();
        PScheduler.scheduleAtFixedRate(() -> {
            a.setText("a " + ai.incrementAndGet());
        }, Duration.ofMillis(0), Duration.ofMillis(10));

        final PLabel p = Element.newPLabel();
        PWindow.getMain().add(p);

        final boolean delegatorMode = true;

        if (delegatorMode) {
            //            final UIDelegator<String> delegator = PScheduler.delegate(new Callback<String>() {
            //
            //                @Override
            //                public void onSuccess(final String result) {
            //                    p.setText(result);
            //                }
            //
            //                @Override
            //                public void onError(final String result, final Exception exception) {
            //                }
            //            });

            new Thread() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(3000);
                    } catch (final InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    //                    delegator.onSuccess("Test");
                }
            }.start();

        }

    }

    public PWindow createWindow3() {
        final PWindow w3 = Element.newPWindow(null, "Window 3", "resizable=yes,location=0,status=0,scrollbars=0");
        final PFlowPanel windowContainer = Element.newPFlowPanel();
        w3.add(windowContainer);
        final PLabel child = Element.newPLabel("Window 3");
        windowContainer.add(child);

        final AtomicInteger i = new AtomicInteger(100);
        final UIRunnable scheduleAtFixedRate = PScheduler.scheduleAtFixedRate(() -> {
            final PLabel label = Element.newPLabel();
            windowContainer.add(label);
            label.setText("Window 3 " + i.incrementAndGet());
            windowContainer.add(Element.newPCheckBox("Checkbox"));
        }, Duration.ofSeconds(5), Duration.ofSeconds(5));

        w3.open();

        w3.addCloseHandler((event) -> scheduleAtFixedRate.cancel());

        return w3;
    }

    public PWindow createWindow2() {
        final PWindow w2 = Element.newPWindow(null, "Window 2", "resizable=yes,location=0,status=0,scrollbars=0");
        final PFlowPanel windowContainer = Element.newPFlowPanel();
        w2.add(windowContainer);
        final PLabel child = Element.newPLabel("Window 2");
        windowContainer.add(child);

        w2.open();

        final AtomicInteger i = new AtomicInteger();
        PScheduler.scheduleAtFixedRate(() -> {
            final PLabel label = Element.newPLabel();
            windowContainer.add(label);
            label.setText("Window 2 " + i.incrementAndGet());
            windowContainer.add(Element.newPCheckBox("Checkbox"));
        }, Duration.ofSeconds(5), Duration.ofSeconds(5));
        return w2;
    }

    public PWindow createWindow1() {
        final PWindow w = Element.newPWindow(null, "Window 1", null);
        w.open();

        PScript.execute(w.getID(), "alert('coucou Window1');");
        PScript.execute(w.getID(), "console.log('coucou Window1');");

        for (int i = 0; i < 2; i++) {
            Element.newPWindow(null, "Winddsqsdqdqs" + i, null).open();
        }
        final PFlowPanel windowContainer = Element.newPFlowPanel();
        w.add(windowContainer);
        final PLabel child = Element.newPLabel("Window 1");
        child.setText("Modified Window 1");

        child.addClickHandler((event) -> {
            child2.setText("Touched by God");
            PScript.execute(PWindow.getMain(), "alert('coucou');");
            child.setText("Clicked Window 1");
        });

        windowContainer.add(child);

        final AtomicInteger i = new AtomicInteger();
        PScheduler.scheduleAtFixedRate(() -> {
            final PLabel label = Element.newPLabel();
            label.setText("Window 1 " + i.incrementAndGet());
            windowContainer.add(label);
            windowContainer.add(Element.newPCheckBox("Checkbox"));
        }, Duration.ofSeconds(10), Duration.ofSeconds(10));
        return w;
    }

    @Override
    public void onUserLoggedOut(final UserLoggedOutEvent event) {
        UIContext.get().close();
    }

    private static final PStackLayoutPanel createStackLayoutPanel() {
        final PStackLayoutPanel child = Element.newPStackLayoutPanel(PUnit.CM);
        child.add(Element.newPLabel("Text"), "Text", false, 1.0);
        return child;
    }

    private static final PListBox createListBox() {
        final PListBox pListBox = Element.newPListBox(true);
        pListBox.addItem("A");
        pListBox.addItem("B");
        pListBox.insertItem("C", 1);
        return pListBox;
    }

    private static final PTabLayoutPanel createTabLayoutPanel() {
        final PTabLayoutPanel child = Element.newPTabLayoutPanel();
        child.add(Element.newPLabel("text"), "text");
        return child;
    }

    private static final PMenuBar createMenu() {
        final PMenuBar pMenuBar = Element.newPMenuBar(true);
        pMenuBar.addItem(Element.newPMenuItem("Menu 1", Element.newPMenuBar()));
        pMenuBar.addItem(Element.newPMenuItem("Menu 2", true, Element.newPMenuBar()));
        pMenuBar.addItem(Element.newPMenuItem("Menu 3", () -> System.err.println("Menu click")));
        pMenuBar.addSeparator();
        return pMenuBar;
    }

    private static final PFlowPanel createPFlowPanel() {
        final PFlowPanel panel1 = Element.newPFlowPanel();
        panel1.setAttribute("id", "panel1");
        final PFlowPanel panel2_1 = Element.newPFlowPanel();
        panel2_1.setAttribute("id", "panel2_1");
        final PFlowPanel panel3_1_1 = Element.newPFlowPanel();
        panel3_1_1.setAttribute("id", "panel3_1_1");
        final PFlowPanel panel3_1_2 = Element.newPFlowPanel();
        panel3_1_2.setAttribute("id", "panel3_1_2");
        final PFlowPanel panel2_2 = Element.newPFlowPanel();
        panel2_2.setAttribute("id", "panel2_2");
        final PFlowPanel panel3_2_1 = Element.newPFlowPanel();
        panel3_2_1.setAttribute("id", "panel3_2_1");
        final PFlowPanel panel3_2_2 = Element.newPFlowPanel();
        panel3_2_2.setAttribute("id", "panel3_2_2");

        panel1.add(panel2_1);
        panel2_1.add(panel3_1_1);
        panel2_1.add(panel3_1_2);
        panel1.add(panel2_2);
        panel2_2.add(panel3_2_1);
        panel2_2.add(panel3_2_2);

        return panel1;
    }

    private static final PWidget createBlock(final PWidget child) {
        final PFlowPanel panel = Element.newPFlowPanel();
        panel.add(child);
        return panel;
    }

    private static final PDockLayoutPanel createDockLayoutPanel() {
        final PDockLayoutPanel pDockLayoutPanel = Element.newPDockLayoutPanel(PUnit.CM);
        pDockLayoutPanel.addNorth(Element.newPLabel("LabelDock"), 1.5);
        return pDockLayoutPanel;
    }

    private static final PFlowPanel createDateBox() {
        final PFlowPanel flowPanel = Element.newPFlowPanel();
        final PDateBox dateBox = Element.newPDateBox();
        dateBox.setValue(new Date(0));
        flowPanel.add(dateBox);
        final PButton button = Element.newPButton("reset");
        button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                dateBox.setValue(null);
            }
        });
        flowPanel.add(button);
        return flowPanel;
    }

    private static final LoggerAddOn createPAddOn() {
        final LoggerAddOn labelPAddOn = new LoggerAddOn();
        labelPAddOn.log("addon logger test");
        return labelPAddOn;
    }

    private static final PTextBox createPTextBox() {
        final PTextBox pTextBox = Element.newPTextBox();

        pTextBox.addKeyUpHandler(new PKeyUpHandler() {

            @Override
            public void onKeyUp(final PKeyUpEvent keyUpEvent) {
                PScript.execute(PWindow.getMain(), "alert('" + keyUpEvent.getEventID() + "');");
            }

            @Override
            public PKeyCodes[] getFilteredKeys() {
                return new PKeyCodes[] { PKeyCodes.ENTER };
            }
        });
        return pTextBox;
    }

    private static final PTree createTree() {
        final PTree tree = Element.newPTree();
        tree.addItem("1");
        tree.addItem(Element.newPTreeItem("2"));
        return tree;
    }

    private static final PAbsolutePanel createAbsolutePanel() {
        final PAbsolutePanel pAbsolutePanel = Element.newPAbsolutePanel();
        pAbsolutePanel.add(Element.newDiv());
        pAbsolutePanel.add(Element.newP());
        return pAbsolutePanel;
    }

    private static final PButton createButton() {
        final PButton pButton = Element.newPButton("Button 1");
        pButton.addClickHandler(handler -> pButton.setText("Button 1 clicked"));
        return pButton;
    }

}
