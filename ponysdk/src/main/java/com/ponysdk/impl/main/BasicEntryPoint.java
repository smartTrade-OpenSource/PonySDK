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

package com.ponysdk.impl.main;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.ui.main.EntryPoint;

public class BasicEntryPoint implements EntryPoint {

    final PButton button = new PButton(" => Button");

    @Override
    public void start(final UIContext uiContext) {
        uiContext.setClientDataOutput((object, instruction) -> System.err.println(object + "" + instruction));

        // PRootPanel.get().clear(true);

        // final long start = System.currentTimeMillis();

        final PFlowPanel flowPanel = new PFlowPanel();
        //
        // final PGrid grid = new PGrid(200, 200);
        // PRootPanel.get().add(grid);

        PWindow.getMain().add(flowPanel);

        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                final PElement div = new PElement("input");
                div.setAttribute("type", "text");
                div.setAttribute("value", i + "-" + j);
                flowPanel.add(div);
            }
        }

        // final Map<Integer, PLabel> labels = new HashMap<>();
        //
        // for (int k = 0; k < 100; k++) {

        // final PButton button = new PButton(" => Button");

        // button.addClickHandler(new PClickHandler() {
        //
        // @Override
        // public void onClick(final PClickEvent eventbus) {
        // button.setText("" + System.currentTimeMillis());
        // }
        // });

        //

        // int i = 0;
        // while (true) {
        // final PElement child = new PElement("div");
        // child.setInnerText(i++ + " => Element ");
        // child.setStyleProperty("border", "1px solid red");
        // flowPanel.add(child);
        // }

        // final PTextBox textBox = new PTextBox(k + " => Button");
        //
        // textBox.addValueChangeHandler(new PValueChangeHandler<String>() {
        //
        // @Override
        // public void onValueChange(final PValueChangeEvent<String> eventbus) {
        // System.err.println("On value Changed : " + eventbus.getValue());
        // }
        // });

        // PRootPanel.get().add(textBox);

        // final PLabel label = new PLabel(k + " => Label");
        //
        // labels.put(k, label);
        //
        // PRootPanel.get().add(label);

        //
        // final PCheckBox checkBox = new PCheckBox("Check");
        // checkBox.setEnabled(false);
        // checkBox.setValue(true);
        // PRootPanel.get().add(checkBox);
        //
        // final PListBox listBox = new PListBox();
        // listBox.setWidth("150px");
        // listBox.setVisibleItemCount(50);
        // for (int j = 0; j < 50; j++) {
        // listBox.addItem("Item " + j);
        // }
        //
        // PRootPanel.get().add(listBox);

        // }

        // final PWindow window = new PWindow(null, "_blank", "");
        // window.open();
        //
        // window.addCloseHandler(new PCloseHandler() {
        //
        // @Override
        // public void onClose(final PCloseEvent closeEvent) {
        // System.err.println("On Close");
        // }
        // });
        // window.addOpenHandler(new POpenHandler() {
        //
        // @Override
        // public void onOpen(final POpenEvent openEvent) {
        // System.err.println("On Open");
        // }
        // });
        //
        // final PLabel label = new PLabel("Dans La window");
        // window.getPRootPanel().add(label);
        // UIScheduledThreadPoolExecutor.scheduleAtFixedRate(new Runnable() {
        //
        // @Override
        // public void run() {
        // final Random random = new Random();
        //
        // final String currentTimeMillis =
        // Long.toString(random.nextInt(500000));
        // for (int k = 0; k < 100; k++) {
        // labels.get(k).setText(currentTimeMillis);
        // }
        // }
        // }, 0, 200, TimeUnit.MILLISECONDS);

        // PScript.execute("window.alert('coucoucou' + (1 + 6));", new
        // ExecutionCallback() {
        //
        // @Override
        // public void onSuccess(final String msg) {}
        //
        // @Override
        // public void onFailure(final String msg) {
        // System.err.println("ERROR" + msg);
        // }
        // });
        // final PSplitLayoutPanel dockLayoutPanel = new PSplitLayoutPanel();
        //
        // final PLabel notrh = new PLabel("NORTH");
        // notrh.setSizeFull();
        // notrh.setStyleProperty("background", "red");
        //
        // final PLabel south = new PLabel("SOUTH");
        // south.setSizeFull();
        // south.setStyleProperty("background", "blue");
        //
        // final PLabel east = new PLabel("EAST");
        // east.setSizeFull();
        // east.setStyleProperty("background", "green");
        //
        // final PLabel west = new PLabel("WEST");
        // west.setSizeFull();
        // west.setStyleProperty("background", "orange");
        //
        // final PLabel centered = new PLabel("centered");
        // centered.setSizeFull();
        // centered.setStyleProperty("background", "yellow");
        //
        // dockLayoutPanel.addNorth(notrh, 50);
        // dockLayoutPanel.addSouth(south, 50);
        // dockLayoutPanel.addEast(east, 100);
        // dockLayoutPanel.addWest(west, 100);
        // dockLayoutPanel.add(centered);
        //
        // dockLayoutPanel.setSizeFull();
        //
        // PRootLayoutPanel.get().add(dockLayoutPanel);

        // final long stop = System.currentTimeMillis();
        //
        // System.err.println("Time to flush : " + (stop - start));

        // uiContext.getHistory().newItem("", false);
    }

}
