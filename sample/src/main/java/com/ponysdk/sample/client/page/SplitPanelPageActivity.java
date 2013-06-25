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

import com.ponysdk.impl.webapplication.page.DefaultPageView;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSplitLayoutPanel;
import com.ponysdk.ui.server.basic.event.PLayoutResizeEvent;
import com.ponysdk.ui.server.basic.event.PLayoutResizeEvent.LayoutResizeData;
import com.ponysdk.ui.server.basic.event.PLayoutResizeHandler;
import com.ponysdk.ui.terminal.PUnit;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class SplitPanelPageActivity extends SamplePageActivity {

    private PHorizontalPanel south;

    public SplitPanelPageActivity() {
        super("Split Panel", "Panels");
        final DefaultPageView pageView = new DefaultPageView("Split Panel");
        pageView.setHeaderHeight(20);
        setPageView(pageView);
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PSplitLayoutPanel splitLayoutPanel = new PSplitLayoutPanel(PUnit.PX);

        splitLayoutPanel.addNorth(buildComponent("north", "#f2a45c"), 50);
        splitLayoutPanel.addSouth(south = buildComponent("south", "#75ffdc"), 50);
        splitLayoutPanel.addEast(buildComponent("east", "#b879fc"), 100);
        splitLayoutPanel.addWest(buildComponent("west", "#e8b6ea"), 100);
        splitLayoutPanel.add(buildCenterPanel());

        splitLayoutPanel.setWidgetSnapClosedSize(south, 40);
        splitLayoutPanel.setWidgetToggleDisplayAllowed(south, true);

        splitLayoutPanel.addLayoutResizeHandler(new PLayoutResizeHandler() {

            @Override
            public void onLayoutResize(final PLayoutResizeEvent resizeEvent) {
                for (final LayoutResizeData data : resizeEvent.getLayoutResizeData()) {
                    if (data.w == south) {
                        PNotificationManager.showTrayNotification("South size: " + data.size);
                    }
                }
            }
        });

        examplePanel.setWidget(splitLayoutPanel);
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

}
