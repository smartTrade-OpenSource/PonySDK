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

import com.ponysdk.core.UIContext;
import com.ponysdk.core.concurrent.PScheduler;
import com.ponysdk.core.concurrent.PScheduler.UIRunnable;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.core.statistic.TerminalDataReceiver;
import com.ponysdk.sample.client.event.UserLoggedOutEvent;
import com.ponysdk.sample.client.event.UserLoggedOutHandler;
import com.ponysdk.sample.client.page.addon.LabelPAddOn;
import com.ponysdk.ui.server.basic.PAbsolutePanel;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PCookies;
import com.ponysdk.ui.server.basic.PDateBox;
import com.ponysdk.ui.server.basic.PDatePicker;
import com.ponysdk.ui.server.basic.PDecoratedPopupPanel;
import com.ponysdk.ui.server.basic.PDecoratorPanel;
import com.ponysdk.ui.server.basic.PDialogBox;
import com.ponysdk.ui.server.basic.PDisclosurePanel;
import com.ponysdk.ui.server.basic.PDockLayoutPanel;
import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.basic.PFileUpload;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PFocusPanel;
import com.ponysdk.ui.server.basic.PGrid;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PHeaderPanel;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PImage;
import com.ponysdk.ui.server.basic.PKeyCodes;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PLayoutPanel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PMenuBar;
import com.ponysdk.ui.server.basic.PMenuItem;
import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.server.basic.PPasswordTextBox;
import com.ponysdk.ui.server.basic.PPopupPanel;
import com.ponysdk.ui.server.basic.PPushButton;
import com.ponysdk.ui.server.basic.PRadioButton;
import com.ponysdk.ui.server.basic.PRichTextArea;
import com.ponysdk.ui.server.basic.PRichTextToolbar;
import com.ponysdk.ui.server.basic.PRootPanel;
import com.ponysdk.ui.server.basic.PScript;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PSplitLayoutPanel;
import com.ponysdk.ui.server.basic.PStackLayoutPanel;
import com.ponysdk.ui.server.basic.PTabLayoutPanel;
import com.ponysdk.ui.server.basic.PTabPanel;
import com.ponysdk.ui.server.basic.PTextArea;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PTree;
import com.ponysdk.ui.server.basic.PTreeItem;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.PWindow;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpFilterHandler;
import com.ponysdk.ui.server.rich.PToolbar;
import com.ponysdk.ui.server.rich.PTwinListBox;
import com.ponysdk.ui.terminal.PUnit;

public class UISampleEntryPoint implements EntryPoint, UserLoggedOutHandler {

    private PLabel child2;

    @Override
    public void start(final UIContext uiContext) {
        uiContext.setClientDataOutput(new TerminalDataReceiver() {

            @Override
            public void onDataReceived(final PObject object, final JsonObject instruction) {
                System.err.println(object + " : " + instruction);
            }
        });

        final PFlowPanel boxContainer = new PFlowPanel();

        System.err.println(PClickEvent.TYPE);

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
        PScript.execute("alert('coucou');");
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
        }, Duration.ofSeconds(5), Duration.ofSeconds(5));

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
        }, Duration.ofSeconds(5), Duration.ofSeconds(5));
        return w2;
    }

    public PWindow createWindow1() {
        final PWindow w = new PWindow(null, "Window 1", null);
        w.open();

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
        }, Duration.ofSeconds(10), Duration.ofSeconds(10));
        return w;
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
        final LabelPAddOn labelPAddOn = new LabelPAddOn(label);
        final StringBuilder sb = new StringBuilder(70000);
        sb.append("aa");
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
