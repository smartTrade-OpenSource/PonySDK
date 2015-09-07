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

import javax.json.JsonObject;

import com.ponysdk.core.ClientDataOutput;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.main.EntryPoint;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PObject;
import com.ponysdk.ui.server.basic.PRootPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class BasicEntryPoint implements EntryPoint {

    @Override
    public void start(final UIContext uiContext) {
        uiContext.setClientDataOutput(new ClientDataOutput() {

            @Override
            public void onClientData(final PObject object, final JsonObject instruction) {
                System.err.println(object + "" + instruction);
            }
        });

        // PRootPanel.get().clear(true);

        final long start = System.currentTimeMillis();

        // final PFlowPanel flowPanel = new PFlowPanel();
        //
        // final PGrid grid = new PGrid(200, 50);
        // PRootPanel.get().add(grid);
        //
        // for (int i = 0; i < 200; i++) {
        // for (int j = 0; j < 50; j++) {
        // grid.setWidget(i, j, new PLabel(i + "-" + j));
        // }
        // }
        for (int k = 0; k < 1; k++) {

            // final PElement child = new PElement("div");
            // child.setInnerText(i + " => Element ");
            // child.setStyleProperty("border", "1px solid red");

            final PButton button = new PButton(k + " => Button");

            button.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent event) {
                    System.err.println("Click " + event.toString());
                }
            });

            PRootPanel.get().add(button);
            // final PTextBox textBox = new PTextBox(k + " => Button");
            // PRootPanel.get().add(textBox);

            // final PLabel label = new PLabel(i + " => Label");
            // PRootPanel.get().add(label);
            //
            // final PCheckBox checkBox = new PCheckBox("Check");
            // checkBox.setEnabled(false);
            // checkBox.setValue(true);
            //
            // final PListBox listBox = new PListBox();
            // listBox.setWidth("150px");
            // listBox.setVisibleItemCount(50);
            // for (int j = 0; j < 50; j++) {
            // listBox.addItem("Item " + j);
            // }
            //
            // PRootPanel.get().add(listBox);

        }
        // PScript.execute("window.alert('coucoucou' + (1 + 6));", new ExecutionCallback() {
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

        final long stop = System.currentTimeMillis();

        System.err.println("Time to flush : " + (stop - start));

        // uiContext.getHistory().newItem("", false);
    }

    @Override
    public void restart(final UIContext uiContext) {
        start(uiContext);
        // session.getHistory().newItem("", false);
    }

}
