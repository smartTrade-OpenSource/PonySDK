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

import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.ui.basic.event.PCloseEvent;
import com.ponysdk.core.ui.basic.event.PCloseHandler;
import com.ponysdk.core.ui.rich.PNotificationManager;

import java.util.ArrayList;
import java.util.List;

public class WindowPageActivity extends SamplePageActivity implements PCloseHandler {

    protected List<PWindow> windows = new ArrayList<>();

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

        final PVerticalPanel verticalPanel = Element.newPVerticalPanel();
        verticalPanel.setSpacing(10);

        // Open popup with custom URL
        final PFlexTable table = Element.newPFlexTable();
        table.setWidget(0, 0, Element.newPLabel("URL"));
        table.setWidget(0, 1, urlTextBox = Element.newPTextBox("https://github.com/PonySDK/PonySDK"));
        table.setWidget(1, 0, Element.newPLabel("Name"));
        table.setWidget(1, 1, nameTextBox = Element.newPTextBox("PonySDK"));
        table.setWidget(2, 0, Element.newPLabel("Features"));
        table.setWidget(2, 1, featuresTextBox = Element.newPTextBox("width=1280,height=800,resizable,status=1"));

        final PButton open = Element.newPButton("Open new window");
        open.addClickHandler(event -> {
            final String url = urlTextBox.getText();
            final String name = nameTextBox.getText();
            final String features = featuresTextBox.getText();
            final PWindow w = Element.newPWindow(url, name, features);
            w.open();
        });

        // Open popup that communicate with server
        final PFlexTable table2 = Element.newPFlexTable();
        table2.setWidget(0, 0, Element.newPLabel("Name"));
        table2.setWidget(0, 1, popNameTextBox = Element.newPTextBox("Popup"));
        table2.setWidget(1, 0, Element.newPLabel("Features"));
        table2.setWidget(1, 1, popFeaturesTextBox = Element.newPTextBox("width=500,height=300,resizable"));

        final PButton open2 = Element.newPButton("Open new window");
        open2.addClickHandler(event -> {
            final String disc = windows.isEmpty() ? "" : Integer.toString(windows.size());
            final String name = popNameTextBox.getText();
            final String features = popFeaturesTextBox.getText();
            final MyWindow window = new MyWindow(name + disc, features);
            window.open();
            window.addCloseHandler(WindowPageActivity.this);
            windows.add(window);
        });

        final PButton postHello = Element.newPButton("Post message");
        postHello.addClickHandler(event -> {
            for (final PWindow window : windows) {
                PNotificationManager.showHumanizedNotification(window, "Hello from opener");
            }
        });

        final PButton closeAllWindow = Element.newPButton("Close all windows");
        closeAllWindow.addClickHandler(event -> {
            for (final PWindow window : windows) {
                window.close();
            }
        });

        verticalPanel.add(Element.newPHTML("<b>Simple popup</b>"));
        verticalPanel.add(table);
        verticalPanel.add(open);

        verticalPanel.add(Element.newPHTML("<br><br><b>Communicating popup</b>"));
        verticalPanel.add(table2);
        verticalPanel.add(open2);
        verticalPanel.add(postHello);
        verticalPanel.add(closeAllWindow);

        examplePanel.setWidget(verticalPanel);
    }

    @Override
    public void onClose(final PCloseEvent closeEvent) {
        final PWindow source = (PWindow) closeEvent.getSource();
        final boolean remove = windows.remove(source);
        if (remove) {
            PNotificationManager.showTrayNotification(getView().asWidget().getWindow(), "Window #" + source.getID() + " closed.");
        }
    }

    private static class MyWindow extends PWindow {

        private int count = 1;

        public MyWindow(final String name, final String features) {
            super(false, null, name, features);
        }

        // @Override
        protected void onLoad() {
            final PFlowPanel flow = Element.newPFlowPanel();
            final PButton addMessage = Element.newPButton("Add message");
            addMessage.addClickHandler(event -> flow.add(Element.newPLabel("Hello " + count++)));
            final PButton clearMessage = Element.newPButton("Clear message");
            clearMessage.addClickHandler(event -> {
                for (int i = flow.getWidgetCount() - 1; i > 2; i--) {
                    flow.remove(i);
                }
            });
            final PButton execJs = Element.newPButton("Exec javascript");
            execJs.addClickHandler(event -> PScript.execute(PWindow.getMain(), "alert('from the popup');"));
            flow.add(addMessage);
            flow.add(clearMessage);
            flow.add(execJs);
            add(flow);
        }

    }
}
