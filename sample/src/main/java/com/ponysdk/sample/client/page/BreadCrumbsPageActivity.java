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

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;
import com.ponysdk.ui.server.breadcrumbs.PBreadCrumbs;
import com.ponysdk.ui.server.breadcrumbs.PBreadCrumbs.ItemLevel;

public class BreadCrumbsPageActivity extends SamplePageActivity {

    protected int level = 6;

    public BreadCrumbsPageActivity() {
        super("BreadCrumbs", "Rich UI Components");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PFlowPanel panel = new PFlowPanel();

        final PBreadCrumbs breadCrumbs = new PBreadCrumbs();

        breadCrumbs.addItem("level 1");
        breadCrumbs.addItem("level 2");
        breadCrumbs.addItem("level 3");
        breadCrumbs.addItem("level 4");
        breadCrumbs.addItem("level 5");
        breadCrumbs.addItem("location");

        breadCrumbs.addSelectionHandler(new PSelectionHandler<PBreadCrumbs.ItemLevel>() {

            @Override
            public void onSelection(final PSelectionEvent<ItemLevel> event) {
                level = event.getSelectedItem().getLevel();
                PNotificationManager.showHumanizedNotification("Selected level : " + level);
            }
        });

        final PFlowPanel inputPanel = new PFlowPanel();
        final PTextBox input = new PTextBox();
        final PButton add = new PButton("Add Level");
        add.setStyleName(PonySDKTheme.BUTTON_BLUE);
        add.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                if (input.getText().isEmpty()) breadCrumbs.addItem("level " + ++level);
                else breadCrumbs.addItem(input.getText());
                input.setText("");
            }
        });
        inputPanel.add(input);
        inputPanel.add(add);

        panel.add(breadCrumbs);
        panel.add(inputPanel);

        examplePanel.setWidget(panel);

    }
}
