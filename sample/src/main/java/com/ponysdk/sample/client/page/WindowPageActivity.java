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

package com.ponysdk.sample.client.page;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PRootLayoutPanel;
import com.ponysdk.ui.server.basic.PScript;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWindow;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PCloseEvent;
import com.ponysdk.ui.server.basic.event.PCloseHandler;

public class WindowPageActivity extends SamplePageActivity implements PCloseHandler {

    protected List<PWindow> windows = new ArrayList<PWindow>();

    private PTextBox urlTextBox;
    private PTextBox nameTextBox;
    private PTextBox featuresTextBox;

    private PTextBox popNameTextBox;
    private PTextBox popFeaturesTextBox;

    public WindowPageActivity() {
        super("Window", "Popup");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel verticalPanel = new PVerticalPanel();
        verticalPanel.setSpacing(10);

        // Open popup with custom URL
        final PFlexTable table = new PFlexTable();
        table.setWidget(0, 0, new PLabel("URL"));
        table.setWidget(0, 1, urlTextBox = new PTextBox("https://github.com/PonySDK/PonySDK"));
        table.setWidget(1, 0, new PLabel("Name"));
        table.setWidget(1, 1, nameTextBox = new PTextBox("PonySDK"));
        table.setWidget(2, 0, new PLabel("Features"));
        table.setWidget(2, 1, featuresTextBox = new PTextBox("width=1280,height=800,resizable,status=1"));

        final PButton open = new PButton("Open new window");
        open.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final String url = urlTextBox.getText();
                final String name = nameTextBox.getText();
                final String features = featuresTextBox.getText();
                final PWindow w = new PWindow(url, name, features);
                w.open();
            }
        });

        // Open popup that communicate with server
        final PFlexTable table2 = new PFlexTable();
        table2.setWidget(0, 0, new PLabel("Name"));
        table2.setWidget(0, 1, popNameTextBox = new PTextBox("Popup"));
        table2.setWidget(1, 0, new PLabel("Features"));
        table2.setWidget(1, 1, popFeaturesTextBox = new PTextBox("width=500,height=300,resizable"));

        final PButton open2 = new PButton("Open new window");
        open2.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final String disc = windows.size() == 0 ? "" : Integer.toString(windows.size());
                final String name = popNameTextBox.getText();
                final String features = popFeaturesTextBox.getText();
                final MyWindow window = new MyWindow(name + disc, features);
                window.open();
                window.addCloseHandler(WindowPageActivity.this);
                windows.add(window);
            }
        });

        final PButton postHello = new PButton("Post message");
        postHello.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                for (final PWindow window : windows) {
                    PNotificationManager.showHumanizedNotification(window, "Hello from opener");
                }
            }
        });

        final PButton closeAllWindow = new PButton("Close all windows");
        closeAllWindow.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                for (final PWindow window : windows) {
                    window.close();
                }
            }
        });

        verticalPanel.add(new PHTML("<b>Simple popup</b>"));
        verticalPanel.add(table);
        verticalPanel.add(open);

        verticalPanel.add(new PHTML("<br><br><b>Communicating popup</b>"));
        verticalPanel.add(table2);
        verticalPanel.add(open2);
        verticalPanel.add(postHello);
        verticalPanel.add(closeAllWindow);

        examplePanel.setWidget(verticalPanel);
    }

    private static class MyWindow extends PWindow {

        private int count = 1;
        private PRootLayoutPanel rootLayoutPanel;

        public MyWindow(final String name, final String features) {
            super(null, name, features);
        }

        protected void onLoad() {
            rootLayoutPanel = PRootLayoutPanel.get();

            final PFlowPanel flow = new PFlowPanel();
            final PButton addMessage = new PButton("Add message");
            addMessage.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent event) {
                    flow.add(new PLabel("Hello " + (count++)));
                }
            });
            final PButton clearMessage = new PButton("Clear message");
            clearMessage.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent event) {
                    for (int i = flow.getWidgetCount() - 1; i > 2; i--) {
                        flow.remove(i);
                    }
                }
            });
            final PButton postMessage = new PButton("Post message");
            postMessage.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent event) {
                    MyWindow.this.postOpenerCommand(new SayHelloCommand());
                }
            });
            final PButton execJs = new PButton("Exec javascript");
            execJs.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent event) {
                    PScript.execute("alert('from the popup');");
                }
            });
            flow.add(addMessage);
            flow.add(clearMessage);
            flow.add(postMessage);
            flow.add(execJs);
            rootLayoutPanel.add(flow);
        }

        private class SayHelloCommand implements Runnable {

            @Override
            public void run() {
                PNotificationManager.showHumanizedNotification("Hello from popup");
            }
        }

    }

    @Override
    public void onClose(final PCloseEvent closeEvent) {
        final PWindow source = (PWindow) closeEvent.getSource();
        final boolean remove = windows.remove(source);
        if (remove) {
            PNotificationManager.showTrayNotification("Window #" + source.getID() + " closed.");
        }
    }
}
