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

import com.ponysdk.ui.server.addon.PNotificationManager;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PDockLayoutPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PSimpleLayoutPanel;
import com.ponysdk.ui.server.basic.PTabPanel;
import com.ponysdk.ui.server.basic.event.PBeforeSelectionHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;

public class TabPanelPageActivity extends SamplePageActivity {

    protected int tabCount = 0;

    public TabPanelPageActivity() {
        super("Tab Panel", "Panels");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        PDockLayoutPanel dockLayoutPanel = new PDockLayoutPanel();
        dockLayoutPanel.setSizeFull();

        final PTabPanel tabPanel = new PTabPanel();
        tabPanel.setSizeFull();

        tabPanel.addBeforeSelectionHandler(new PBeforeSelectionHandler<Integer>() {

            @Override
            public void onBeforeSelection(final Integer index) {
                PNotificationManager.showTrayNotification("onBeforeSelection, tab index : " + index);
            }
        });
        tabPanel.addSelectionHandler(new PSelectionHandler<Integer>() {

            @Override
            public void onSelection(final PSelectionEvent<Integer> event) {
                PNotificationManager.showTrayNotification("onSelection, tab index : " + event.getSelectedItem());
            }
        });

        final PButton button = new PButton("Add Tab");
        button.setWidth("100px");
        button.setStyleProperty("margin", "10px");

        button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                addTabContent(tabPanel);
            }
        });

        dockLayoutPanel.addNorth(button, 50);
        dockLayoutPanel.add(tabPanel);

        addTabContent(tabPanel);

        examplePanel.setWidget(dockLayoutPanel);
    }

    protected void addTabContent(final PTabPanel tabPanel) {
        final PSimpleLayoutPanel tabContent = new PSimpleLayoutPanel();

        final int tabIndex = tabCount;
        final PLabel label = new PLabel("content-" + tabIndex);
        tabContent.setWidget(label);
        tabPanel.add(tabContent, "Tab-" + tabIndex);
        tabCount++;
    }
}
