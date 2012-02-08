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

import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.ui.server.addon.PNotificationManager;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PTabPanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PBeforeSelectionHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;

public class TabPanelPageActivity extends PageActivity {

    private final PTabPanel tabPanel = new PTabPanel();
    private int tabCount = 0;

    public TabPanelPageActivity() {
        super("TabPanel", "Fast UI Components");
        tabPanel.setWidth("400px");
        tabPanel.setHeight("400px");
        tabPanel.setAnimationEnabled(true);
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
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onShowPage(final Place place) {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onFirstShowPage() {
        final PVerticalPanel verticalPanel = new PVerticalPanel();
        verticalPanel.setSizeFull();

        final PSimplePanel layoutPanel = new PSimplePanel();
        layoutPanel.setStyleProperty("border", "2px solid red");
        layoutPanel.setSizeFull();

        final PButton button = new PButton("Add Tab");
        button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final PVerticalPanel tabContent = new PVerticalPanel();
                tabContent.setSizeFull();

                final int tabIndex = tabCount;
                final PLabel label = new PLabel("content-" + tabIndex);
                tabContent.add(label);
                tabContent.setStyleProperty("border", "2px solid red");
                tabPanel.add(tabContent, "Tab-" + tabIndex);
                tabCount++;
            }
        });

        layoutPanel.setWidget(tabPanel);
        verticalPanel.add(button);
        verticalPanel.add(layoutPanel);

        pageView.getBody().setWidget(verticalPanel);
    }
}
