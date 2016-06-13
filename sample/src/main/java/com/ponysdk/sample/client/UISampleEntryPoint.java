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

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.JsonObject;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.PScheduler;
import com.ponysdk.core.server.concurrent.PScheduler.UIRunnable;
import com.ponysdk.core.terminal.PUnit;
import com.ponysdk.core.ui.basic.PAbsolutePanel;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PCookies;
import com.ponysdk.core.ui.basic.PDateBox;
import com.ponysdk.core.ui.basic.PDatePicker;
import com.ponysdk.core.ui.basic.PDecoratedPopupPanel;
import com.ponysdk.core.ui.basic.PDecoratorPanel;
import com.ponysdk.core.ui.basic.PDialogBox;
import com.ponysdk.core.ui.basic.PDisclosurePanel;
import com.ponysdk.core.ui.basic.PDockLayoutPanel;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PFileUpload;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PFocusPanel;
import com.ponysdk.core.ui.basic.PGrid;
import com.ponysdk.core.ui.basic.PHTML;
import com.ponysdk.core.ui.basic.PHeaderPanel;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PImage;
import com.ponysdk.core.ui.basic.PKeyCodes;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PLayoutPanel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PMenuBar;
import com.ponysdk.core.ui.basic.PMenuItem;
import com.ponysdk.core.ui.basic.PObject;
import com.ponysdk.core.ui.basic.PPasswordTextBox;
import com.ponysdk.core.ui.basic.PPopupPanel;
import com.ponysdk.core.ui.basic.PPushButton;
import com.ponysdk.core.ui.basic.PRadioButton;
import com.ponysdk.core.ui.basic.PRichTextArea;
import com.ponysdk.core.ui.basic.PRichTextToolbar;
import com.ponysdk.core.ui.basic.PRootPanel;
import com.ponysdk.core.ui.basic.PScript;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSimpleLayoutPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PSplitLayoutPanel;
import com.ponysdk.core.ui.basic.PStackLayoutPanel;
import com.ponysdk.core.ui.basic.PTabLayoutPanel;
import com.ponysdk.core.ui.basic.PTabPanel;
import com.ponysdk.core.ui.basic.PTextArea;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PTree;
import com.ponysdk.core.ui.basic.PTreeItem;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.basic.event.PKeyUpEvent;
import com.ponysdk.core.ui.basic.event.PKeyUpFilterHandler;
import com.ponysdk.core.ui.main.EntryPoint;
import com.ponysdk.core.ui.rich.PToolbar;
import com.ponysdk.core.ui.rich.PTwinListBox;
import com.ponysdk.core.ui.statistic.TerminalDataReceiver;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.sample.client.event.UserLoggedOutHandler;
import com.ponysdk.sample.client.page.addon.LabelPAddOn;

public class UISampleEntryPoint implements EntryPoint, UserLoggedOutHandler {

    private PLabel child2;

    @Override
    public void start(final UIContext uiContext) {
        uiContext.setClientDataOutput((object, instruction) -> System.err.println(object + " : " + instruction));

        final PFlowPanel flowPanel = new PFlowPanel();
        PRootPanel.get().add(flowPanel);


        createPAddOn();

        if(true) return;

        final PElement label1 = new PElement("div");
        final PElement label2 = new PElement("div");
        final PElement label3 = new PElement("div");
        final PElement label4 = new PElement("div");
        final PElement label5 = new PElement("div");
        final PElement label6 = new PElement("div");
        final PElement label7 = new PElement("div");
        final PElement label8 = new PElement("div");
        final PElement label9 = new PElement("div");
        final PElement label10 = new PElement("div");
        final PElement label11 = new PElement("div");
        final PElement label12 = new PElement("div");
        final PElement label13 = new PElement("div");
        final PElement label14 = new PElement("div");
        final PElement label15 = new PElement("div");
        final PElement label16 = new PElement("div");
        final PElement label17 = new PElement("div");
        final PElement label18 = new PElement("div");
        final PElement label19 = new PElement("div");
        final PElement label20 = new PElement("div");

        flowPanel.add(label1);
        flowPanel.add(label2);
        flowPanel.add(label3);
        flowPanel.add(label4);
        flowPanel.add(label5);
        flowPanel.add(label6);
        flowPanel.add(label7);
        flowPanel.add(label8);
        flowPanel.add(label9);
        flowPanel.add(label10);
        flowPanel.add(label11);
        flowPanel.add(label12);
        flowPanel.add(label13);
        flowPanel.add(label14);
        flowPanel.add(label15);
        flowPanel.add(label16);
        flowPanel.add(label17);
        flowPanel.add(label18);
        flowPanel.add(label19);
        flowPanel.add(label20);

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

        final PWindow window = new PWindow(null, "a", null);
        window.open();
        final PWindow window1 = new PWindow(null, "b", null);
        window1.open();
        final PWindow window2 = new PWindow(null, "c", null);
        window2.open();
        final PWindow window3 = new PWindow(null, "d", null);
        window3.open();

        final PFlowPanel boxContainer = new PFlowPanel();

        PScript.execute("alert('coucou Main');");

        final PWindow w1 = createWindow1();
        // final PWindow w2 = createWindow2();
        // final PWindow w3 = createWindow3();

        // boxContainer.add(new PHistory());
        // boxContainer.add(new PNotificationManager());
        // boxContainer.add(new PSuggestBox());

        boxContainer.add(createBlock(createAbsolutePanel()));
        // boxContainer.add(createPAddOn().asWidget());
        boxContainer.add(new PAnchor());
        boxContainer.add(new PAnchor("Anchor"));
        boxContainer.add(new PAnchor("Anchor 1", "anchor2"));
        boxContainer.add(new PButton());
        boxContainer.add(createButton());
        boxContainer.add(new PCheckBox());
        boxContainer.add(new PCheckBox("Checkbox"));
        final PCookies cookies = new PCookies();
        cookies.setCookie("Cook", "ies");
        boxContainer.add(createDateBox());
        boxContainer.add(new PDateBox(new SimpleDateFormat("dd/MM/yyyy")));
        boxContainer.add(new PDateBox(new PDatePicker(), new SimpleDateFormat("yyyy/MM/dd")));
        boxContainer.add(new PDatePicker());
        boxContainer.add(new PDecoratedPopupPanel(false));
        boxContainer.add(new PDecoratedPopupPanel(true));
        boxContainer.add(new PDecoratorPanel());
        boxContainer.add(new PDialogBox());
        boxContainer.add(new PDialogBox(true));
        boxContainer.add(new PDisclosurePanel("Disclosure"));
        boxContainer.add(createDockLayoutPanel());
        boxContainer.add(new PElement("a"));
        boxContainer.add(new PFileUpload());
        boxContainer.add(new PFlexTable());
        boxContainer.add(createPFlowPanel());
        boxContainer.add(new PFocusPanel());
        boxContainer.add(new PGrid());
        boxContainer.add(new PGrid(2, 3));
        boxContainer.add(new PHeaderPanel());
        // boxContainer.add(new PHistory());
        boxContainer.add(new PHorizontalPanel());
        boxContainer.add(new PHTML());
        boxContainer.add(new PHTML("Html"));
        boxContainer.add(new PHTML("Html 1", true));
        boxContainer.add(new PImage()); // FIXME Test with image
        boxContainer.add(new PLabel());
        boxContainer.add(new PLabel("Label"));
        boxContainer.add(new PLayoutPanel());
        boxContainer.add(new PListBox());
        boxContainer.add(createListBox());
        boxContainer.add(new PMenuBar());
        boxContainer.add(createMenu());
        // boxContainer.add(new PNotificationManager());
        boxContainer.add(new PPasswordTextBox());
        boxContainer.add(new PPasswordTextBox("Password"));

        boxContainer.add(new PPopupPanel());
        boxContainer.add(new PPopupPanel(true));

        boxContainer.add(new PPushButton(new PImage())); // FIXME Test with
                                                         // image

        boxContainer.add(new PRadioButton("RadioLabel"));
        boxContainer.add(new PRadioButton("RadioName", "RadioLabel"));
        final PRichTextArea richTextArea = new PRichTextArea();
        boxContainer.add(richTextArea);
        boxContainer.add(new PRichTextToolbar(richTextArea));
        boxContainer.add(new PScrollPanel());
        boxContainer.add(new PSimpleLayoutPanel());
        boxContainer.add(new PSimplePanel());
        boxContainer.add(new PSplitLayoutPanel());
        boxContainer.add(createStackLayoutPanel());
        // boxContainer.add(new PSuggestBox());
        boxContainer.add(createTabLayoutPanel());
        boxContainer.add(new PTabPanel());
        boxContainer.add(new PTextArea());
        boxContainer.add(createPTextBox());
        boxContainer.add(new PToolbar());
        boxContainer.add(createTree());
        boxContainer.add(new PTwinListBox<Object>());
        boxContainer.add(new PVerticalPanel());

        PRootPanel.get().add(boxContainer);

        child2 = new PLabel("Label2");
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

        boxContainer.add(new PLabel("Label3"));

        final PLabel label = new PLabel("Label4");

        try {
            w1.add(label);

            boxContainer.add(label);
        } catch (final Exception e) {

        }

        // uiContext.getHistory().newItem("", false);
    }

    public PWindow createWindow3() {
        final PWindow w3 = new PWindow(null, "Window 3", "resizable=yes,location=0,status=0,scrollbars=0");
        final PFlowPanel windowContainer = new PFlowPanel();
        w3.add(windowContainer);
        final PLabel child = new PLabel("Window 3");
        windowContainer.add(child);

        final AtomicInteger i = new AtomicInteger(100);
        final UIRunnable scheduleAtFixedRate = PScheduler.scheduleAtFixedRate(() -> {
            final PLabel label = new PLabel();
            windowContainer.add(label);
            label.setText("Window 3 " + i.incrementAndGet());
            windowContainer.add(new PCheckBox("Checkbox"));
        } , Duration.ofSeconds(5), Duration.ofSeconds(5));

        w3.open();

        w3.addCloseHandler((event) -> scheduleAtFixedRate.cancel());

        return w3;
    }

    public PWindow createWindow2() {
        final PWindow w2 = new PWindow(null, "Window 2", "resizable=yes,location=0,status=0,scrollbars=0");
        final PFlowPanel windowContainer = new PFlowPanel();
        w2.add(windowContainer);
        final PLabel child = new PLabel("Window 2");
        windowContainer.add(child);

        w2.open();

        final AtomicInteger i = new AtomicInteger();
        PScheduler.scheduleAtFixedRate(() -> {
            final PLabel label = new PLabel();
            windowContainer.add(label);
            label.setText("Window 2 " + i.incrementAndGet());
            windowContainer.add(new PCheckBox("Checkbox"));
        } , Duration.ofSeconds(5), Duration.ofSeconds(5));
        return w2;
    }

    public PWindow createWindow1() {
        final PWindow w = new PWindow(null, "Window 1", null);
        w.open();

        PScript.execute(w.getID(), "alert('coucou Window1');");
        PScript.execute(w.getID(), "console.log('coucou Window1');");

        for (int i = 0; i < 2; i++) {
            new PWindow(null, "Winddsqsdqdqs" + i, null).open();
        }
        final PFlowPanel windowContainer = new PFlowPanel();
        w.add(windowContainer);
        final PLabel child = new PLabel("Window 1");
        child.setText("Modified Window 1");

        child.addClickHandler((event) -> {
            child2.setText("Touched by God");
            PScript.execute("alert('coucou');");
            child.setText("Clicked Window 1");
        });

        windowContainer.add(child);

        final AtomicInteger i = new AtomicInteger();
        PScheduler.scheduleAtFixedRate(() -> {
            final PLabel label = new PLabel();
            label.setText("Window 1 " + i.incrementAndGet());
            windowContainer.add(label);
            windowContainer.add(new PCheckBox("Checkbox"));
        } , Duration.ofSeconds(10), Duration.ofSeconds(10));
        return w;
    }

    @Override
    public void onUserLoggedOut(final UserLoggedOutEvent event) {
        UIContext.get().close();
    }

    private static final PStackLayoutPanel createStackLayoutPanel() {
        final PStackLayoutPanel child = new PStackLayoutPanel(PUnit.CM);
        child.add(new PLabel("Text"), "Text", false, 1.0);
        return child;
    }

    private static final PListBox createListBox() {
        final PListBox pListBox = new PListBox(true);
        pListBox.addItem("A");
        pListBox.addItem("B");
        pListBox.insertItem("C", 1);
        return pListBox;
    }

    private static final PTabLayoutPanel createTabLayoutPanel() {
        final PTabLayoutPanel child = new PTabLayoutPanel();
        child.add(new PLabel("text"), "text");
        return child;
    }

    private static final PMenuBar createMenu() {
        final PMenuBar pMenuBar = new PMenuBar(true);
        pMenuBar.addItem(new PMenuItem("Menu 1", new PMenuBar()));
        pMenuBar.addItem(new PMenuItem("Menu 2", true, new PMenuBar()));
        pMenuBar.addItem(new PMenuItem("Menu 3", () -> System.err.println("Menu click")));
        pMenuBar.addSeparator();
        return pMenuBar;
    }

    private static final PFlowPanel createPFlowPanel() {
        final PFlowPanel panel1 = new PFlowPanel();
        panel1.setAttribute("id", "panel1");
        final PFlowPanel panel2_1 = new PFlowPanel();
        panel2_1.setAttribute("id", "panel2_1");
        final PFlowPanel panel3_1_1 = new PFlowPanel();
        panel3_1_1.setAttribute("id", "panel3_1_1");
        final PFlowPanel panel3_1_2 = new PFlowPanel();
        panel3_1_2.setAttribute("id", "panel3_1_2");
        final PFlowPanel panel2_2 = new PFlowPanel();
        panel2_2.setAttribute("id", "panel2_2");
        final PFlowPanel panel3_2_1 = new PFlowPanel();
        panel3_2_1.setAttribute("id", "panel3_2_1");
        final PFlowPanel panel3_2_2 = new PFlowPanel();
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
        final PFlowPanel panel = new PFlowPanel();
        panel.add(child);
        return panel;
    }

    private static final PDockLayoutPanel createDockLayoutPanel() {
        final PDockLayoutPanel pDockLayoutPanel = new PDockLayoutPanel(PUnit.CM);
        pDockLayoutPanel.addNorth(new PLabel("LabelDock"), 1.5);
        return pDockLayoutPanel;
    }

    private static final PFlowPanel createDateBox() {
        final PFlowPanel flowPanel = new PFlowPanel();
        final PDateBox dateBox = new PDateBox();
        dateBox.setValue(new Date(0));
        flowPanel.add(dateBox);
        final PButton button = new PButton("reset");
        button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                dateBox.setValue(null);
            }
        });
        flowPanel.add(button);
        return flowPanel;
    }

    private static final LabelPAddOn createPAddOn() {
        final PElement label = new PElement("div");
        PRootPanel.get().add(label);
        final LabelPAddOn labelPAddOn = new LabelPAddOn(label);
        labelPAddOn.log("addon logger test");
        return labelPAddOn;
    }

    private static final PTextBox createPTextBox() {
        final PTextBox pTextBox = new PTextBox();

        final PKeyUpFilterHandler keyUpHandler = new PKeyUpFilterHandler(PKeyCodes.ENTER) {

            @Override
            public void onKeyUp(final PKeyUpEvent keyUpEvent) {
                PScript.execute("alert('" + keyUpEvent.getEventID() + "');");
            }
        };

        pTextBox.addDomHandler(keyUpHandler, PKeyUpEvent.TYPE);
        return pTextBox;
    }

    private static final PTree createTree() {
        final PTree tree = new PTree();
        tree.addItem("1");
        tree.addItem(new PTreeItem("2"));
        return tree;
    }

    private static final PAbsolutePanel createAbsolutePanel() {
        final PAbsolutePanel pAbsolutePanel = new PAbsolutePanel();
        pAbsolutePanel.add(new PElement("div"));
        pAbsolutePanel.add(new PElement("p"));
        return pAbsolutePanel;
    }

    private static final PButton createButton() {
        final PButton pButton = new PButton("Button 1");
        pButton.addClickHandler(handler -> pButton.setText("Button 1 clicked"));
        return pButton;
    }

}
