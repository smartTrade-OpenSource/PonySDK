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

import java.util.Arrays;

import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PDockLayoutPanel;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PTabLayoutPanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.event.PBeforeSelectionEvent;
import com.ponysdk.ui.server.basic.event.PBeforeSelectionHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;
import com.ponysdk.ui.terminal.PUnit;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class TabLayoutPanelPageActivity extends SamplePageActivity {

    private final String[] colors = { "#F34A53", "#FAE3B4", "#AAC789", "#437356", "#1E4147" };
    protected int tabCount = 0;

    public TabLayoutPanelPageActivity() {
        super("Tab Layout Panel", Arrays.asList("Panels", "Tab"));
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PDockLayoutPanel dockLayoutPanel = new PDockLayoutPanel(PUnit.PX);
        dockLayoutPanel.setSizeFull();

        final PTabLayoutPanel tabPanel = new PTabLayoutPanel();
        tabPanel.setSizeFull();
        tabPanel.setAnimationVertical(false);
        tabPanel.setAnimationDuration(1000);

        tabPanel.addBeforeSelectionHandler(new PBeforeSelectionHandler<Integer>() {

            @Override
            public void onBeforeSelection(final PBeforeSelectionEvent<Integer> event) {
                PNotificationManager.showTrayNotification("onBeforeSelection, tab index : " + event.getSelectedItem());
            }
        });
        tabPanel.addSelectionHandler(new PSelectionHandler<Integer>() {

            @Override
            public void onSelection(final PSelectionEvent<Integer> event) {
                PNotificationManager.showTrayNotification("onSelection, tab index : " + event.getSelectedItem());
            }
        });

        final PButton button = new PButton("Add Tab");
        button.setStyleProperty("margin", "10px");
        button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                addTabContent(tabPanel);
            }
        });

        final PButton addCustomTabButton = new PButton("Add Tab (custom tab)");
        addCustomTabButton.setStyleProperty("margin", "10px");
        addCustomTabButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                addCustomTabContent(tabPanel);
            }
        });

        final PTextBox indexTextBox = new PTextBox();
        final PButton selectButton = new PButton("Select Tab");
        selectButton.setStyleProperty("margin", "10px");
        selectButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final String text = indexTextBox.getText();
                tabPanel.selectTab(Integer.valueOf(text));
            }
        });

        final PHorizontalPanel horizontalPanel = new PHorizontalPanel();
        horizontalPanel.setVerticalAlignment(PVerticalAlignment.ALIGN_MIDDLE);
        horizontalPanel.add(button);
        horizontalPanel.add(addCustomTabButton);
        horizontalPanel.add(indexTextBox);
        horizontalPanel.add(selectButton);

        dockLayoutPanel.addNorth(horizontalPanel, 50);
        dockLayoutPanel.add(tabPanel);

        addTabContent(tabPanel);

        examplePanel.setWidget(dockLayoutPanel);
    }

    protected void addTabContent(final PTabLayoutPanel tabPanel) {
        final PSimpleLayoutPanel tabContent = new PSimpleLayoutPanel();
        tabContent.setStyleProperty("background-color", colors[tabCount % colors.length]);

        final int tabIndex = tabCount;
        final PLabel label = new PLabel("content-" + tabIndex);
        label.setStyleProperty("color", "white");
        tabContent.setWidget(label);
        tabPanel.add(tabContent, "Tab-" + tabIndex);
        tabCount++;
    }

    protected void addCustomTabContent(final PTabLayoutPanel tabPanel) {
        final PSimplePanel tabContent = new PSimplePanel();

        final int tabIndex = tabCount;
        final PLabel tabLabel = new PLabel("CustomTab-" + tabIndex);
        tabLabel.setStyleProperty("color", "blue");
        tabLabel.setStyleProperty("whiteSpace", "nowrap");
        final PLabel label = new PLabel("content-" + tabIndex);
        tabContent.setWidget(label);
        tabPanel.add(tabContent, tabLabel);
        tabCount++;
    }
}
