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

import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PDockLayoutPanel;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.terminal.PUnit;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class DockPanelPageActivity extends SamplePageActivity implements PClickHandler {

    final PDockLayoutPanel dockLayoutPanel = new PDockLayoutPanel(PUnit.PX);
    private final PTextBox headerSize = new PTextBox();
    final PButton applySize = new PButton("Apply Header Size");
    private final PFlowPanel headerWidget = new PFlowPanel();

    public DockPanelPageActivity() {
        super("Dock Panel", "Panels");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        dockLayoutPanel.addNorth(buildAction(), 50);
        dockLayoutPanel.addSouth(buildComponent("south", "#75ffdc"), 50);
        dockLayoutPanel.addEast(buildComponent("east", "#b879fc"), 100);
        dockLayoutPanel.addWest(buildComponent("west", "#e8b6ea"), 100);
        dockLayoutPanel.add(buildCenterPanel());

        examplePanel.setWidget(dockLayoutPanel);
    }

    private PWidget buildAction() {
        applySize.addClickHandler(this);
        headerWidget.add(headerSize);
        headerWidget.add(applySize);
        return headerWidget;
    }

    private PScrollPanel buildCenterPanel() {
        final PScrollPanel panel = new PScrollPanel();
        panel.setSizeFull();
        panel.setStyleProperty("backgroundColor", "#c17d7d");
        panel.setWidget(new PLabel(
                "This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow."));
        return panel;
    }

    private PHorizontalPanel buildComponent(final String name, final String color) {
        final PHorizontalPanel panel = new PHorizontalPanel();
        panel.setSizeFull();
        panel.setStyleProperty("backgroundColor", color);
        final PLabel label = new PLabel("The " + name + " component");
        panel.add(label);
        panel.setCellHorizontalAlignment(label, PHorizontalAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(label, PVerticalAlignment.ALIGN_MIDDLE);
        return panel;
    }

    @Override
    public void onClick(final PClickEvent event) {
        final double size = Double.parseDouble(headerSize.getText());

        if (size < 50) {
            PNotificationManager.showHumanizedNotification("Size to small for the demo");
        } else {
            dockLayoutPanel.setWidgetSize(headerWidget, size);
        }

    }

}
