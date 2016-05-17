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

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.JsonObject;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.concurrent.UIScheduledThreadPoolExecutor;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.statistic.TerminalDataReceiver;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.sample.client.event.UserLoggedOutHandler;
import com.ponysdk.sample.client.page.addon.LabelPAddOn;
import com.ponysdk.ui.server.basic.PAbsolutePanel;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PDateBox;
import com.ponysdk.ui.server.basic.PDockLayoutPanel;
import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PKeyCodes;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PMenuBar;
import com.ponysdk.ui.server.basic.PMenuItem;
import com.ponysdk.ui.server.basic.PMenuItemSeparator;
import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.server.basic.PRootPanel;
import com.ponysdk.ui.server.basic.PScript;
import com.ponysdk.ui.server.basic.PStackLayoutPanel;
import com.ponysdk.ui.server.basic.PTabLayoutPanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PTree;
import com.ponysdk.ui.server.basic.PTreeItem;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.PWindow;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;
import com.ponysdk.ui.server.basic.event.POpenEvent;
import com.ponysdk.ui.server.basic.event.POpenHandler;
import com.ponysdk.ui.terminal.PUnit;

public class UISampleEntryPoint implements EntryPoint, UserLoggedOutHandler {

    @Override
    public void start(final UIContext uiContext) {
        uiContext.setClientDataOutput(new TerminalDataReceiver() {

            @Override
            public void onDataReceived(final PObject object, final JsonObject instruction) {
                System.err.println(object + "" + instruction);
            }
        });

        /*
         * final PWindow w = new PWindow(null, "Window 1", null);
         * w.addOpenHandler(new POpenHandler() {
         *
         * @Override
         * public void onOpen(final POpenEvent openEvent) {
         *
         * final PFlowPanel windowContainer = new PFlowPanel();
         * w.addWidget(windowContainer);
         * final PLabel child = new PLabel("Window 1");
         * child.setText("Modified Window 1");
         * windowContainer.add(child);
         *
         * final AtomicInteger i = new AtomicInteger();
         * UIScheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
         *
         * @Override
         * public void run() {
         * final PLabel label = new PLabel();
         * label.setText("Window 1 " + i.incrementAndGet());
         * final Consumer<String> a = label::setText;
         * windowContainer.add(label);
         * windowContainer.add(new PCheckBox("Checkbox"));
         * }
         * }, 10, 10, TimeUnit.SECONDS);
         * }
         * });
         * w.open();
         */

        final PWindow w2 = new PWindow(null, "Window 2", null);
        w2.addOpenHandler(new POpenHandler() {

            @Override
            public void onOpen(final POpenEvent openEvent) {
                final PFlowPanel windowContainer = new PFlowPanel();
                w2.addWidget(windowContainer);
                final PLabel child = new PLabel("Window 2");
                windowContainer.add(child);

                final AtomicInteger i = new AtomicInteger();
                UIScheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {

                    @Override
                    public void run() {
                        final PLabel label = new PLabel();
                        windowContainer.add(label);
                        label.setText("Window 2 " + i.incrementAndGet());
                        windowContainer.add(new PCheckBox("Checkbox"));
                    }
                }, 5, 5, TimeUnit.SECONDS);
            }
        });
        w2.open();

        /*
         * final PWindow w3 = new PWindow(null, "Window 3",
         * "resizable=yes,location=0,status=0,scrollbars=0");
         * w3.addOpenHandler(new POpenHandler() {
         *
         * @Override
         * public void onOpen(final POpenEvent openEvent) {
         * final PFlowPanel windowContainer = new PFlowPanel();
         * w3.addWidget(windowContainer);
         * final PLabel child = new PLabel("Window 3");
         * windowContainer.add(child);
         *
         * final AtomicInteger i = new AtomicInteger(100);
         * UIScheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
         *
         * @Override
         * public void run() {
         * final PLabel label = new PLabel();
         * windowContainer.add(label);
         * label.setText("Window 3 " + i.incrementAndGet());
         * windowContainer.add(new PCheckBox("Checkbox"));
         * }
         * }, 5, 5, TimeUnit.SECONDS);
         * }
         * });
         * w3.open();
         */
        final PFlowPanel boxContainer = new PFlowPanel();
        /*
         * // boxContainer.add(new PHistory());
         * // boxContainer.add(new PNotificationManager());
         * // boxContainer.add(new PSuggestBox());
         *
         * boxContainer.add(createBlock(createAbsolutePanel()));
         * //boxContainer.add(createPAddOn().asWidget());
         * boxContainer.add(new PAnchor());
         *
         * boxContainer.add(new PAnchor("Anchor"));
         * boxContainer.add(new PAnchor("Anchor 1", "anchor2"));
         * boxContainer.add(new PButton());
         * boxContainer.add(createButton());
         * boxContainer.add(new PCheckBox());
         * boxContainer.add(new PCheckBox("Checkbox"));
         * final PCookies cookies = new PCookies();
         * cookies.setCookie("Cook", "ies");
         * boxContainer.add(createDateBox());
         * boxContainer.add(new PDateBox(new SimpleDateFormat("dd/MM/yyyy")));
         * boxContainer.add(new PDateBox(new PDatePicker(), new SimpleDateFormat("yyyy/MM/dd")));
         * boxContainer.add(new PDatePicker());
         * boxContainer.add(new PDecoratedPopupPanel(false));
         * boxContainer.add(new PDecoratedPopupPanel(true));
         * boxContainer.add(new PDecoratorPanel());
         * boxContainer.add(new PDialogBox());
         * boxContainer.add(new PDialogBox(true));
         * boxContainer.add(new PDisclosurePanel("Disclosure"));
         * boxContainer.add(createDockLayoutPanel());
         * boxContainer.add(new PElement("a"));
         * boxContainer.add(new PFileUpload());
         * boxContainer.add(new PFlexTable());
         * boxContainer.add(createPFlowPanel());
         * boxContainer.add(new PFocusPanel());
         * boxContainer.add(new PGrid());
         * boxContainer.add(new PGrid(2, 3));
         * boxContainer.add(new PHeaderPanel());
         * // boxContainer.add(new PHistory());
         * boxContainer.add(new PHorizontalPanel());
         * boxContainer.add(new PHTML());
         * boxContainer.add(new PHTML("Html"));
         * boxContainer.add(new PHTML("Html 1", true));
         * boxContainer.add(new PImage()); // FIXME Test with image
         * boxContainer.add(new PLabel());
         * boxContainer.add(new PLabel("Label"));
         * boxContainer.add(new PLayoutPanel());
         * boxContainer.add(new PListBox());
         * boxContainer.add(createListBox());
         * boxContainer.add(new PMenuBar());
         * boxContainer.add(createMenu());
         * // boxContainer.add(new PNotificationManager());
         * boxContainer.add(new PPasswordTextBox());
         * boxContainer.add(new PPasswordTextBox("Password"));
         * boxContainer.add(new PPopupPanel());
         * boxContainer.add(new PPopupPanel(true));
         * boxContainer.add(new PPushButton(new PImage())); // FIXME Test with image
         * boxContainer.add(new PRadioButton("RadioLabel"));
         * boxContainer.add(new PRadioButton("RadioName", "RadioLabel"));
         * final PRichTextArea richTextArea = new PRichTextArea();
         * boxContainer.add(richTextArea);
         * boxContainer.add(new PRichTextToolbar(richTextArea));
         * PScript.execute("alert('coucou');");
         * boxContainer.add(new PScrollPanel());
         * boxContainer.add(new PSimpleLayoutPanel());
         * boxContainer.add(new PSimplePanel());
         * boxContainer.add(new PSplitLayoutPanel());
         * boxContainer.add(createStackLayoutPanel());
         * // boxContainer.add(new PSuggestBox());
         * boxContainer.add(createTabLayoutPanel());
         * boxContainer.add(new PTabPanel());
         * boxContainer.add(new PTextArea());
         * boxContainer.add(createPTextBox());
         * boxContainer.add(new PToolbar());
         * boxContainer.add(createTree());
         * boxContainer.add(new PTwinListBox());
         * boxContainer.add(new PVerticalPanel());
         * // boxContainer.add(new PWindow());
         */

        final PLabel child = new PLabel("Label1");
        child.setStyleName("aaaa");
        child.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                System.out.println("aaaaa");
            }
        });
        boxContainer.add(child);
        PRootPanel.get().add(boxContainer);
        final PLabel child2 = new PLabel("Label2");
        boxContainer.add(child2);
        child2.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                System.out.println("bbbbb");
            }
        });

        // uiContext.getHistory().newItem("", false);
    }

    @Override
    public void restart(final UIContext uiContext) {
        start(uiContext);
        // session.getHistory().newItem("", false);
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
        pMenuBar.addSeparator(new PMenuItemSeparator());
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
        final LabelPAddOn labelPAddOn = new LabelPAddOn(label);
        final StringBuilder sb = new StringBuilder(70000);
        sb.append("é");
        for (int i = 0; i < 1; i++)
            sb.append("a");
        labelPAddOn.log(sb.append("z").toString());
        return labelPAddOn;
    }

    private static final PTextBox createPTextBox() {
        final PTextBox pTextBox = new PTextBox();

        final PKeyUpFilterHandler keyUpHandler = new PKeyUpFilterHandler(PKeyCodes.ENTER) {

            @Override
            public void onKeyUp(final PKeyUpEvent keyUpEvent) {
                PScript.get().executeScript("alert('" + keyUpEvent.getEventID() + "');");
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
