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

import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.model.PVerticalAlignment;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PSplitLayoutPanel;
import com.ponysdk.core.ui.basic.event.PLayoutResizeEvent.LayoutResizeData;
import com.ponysdk.core.ui.rich.PNotificationManager;
import com.ponysdk.impl.webapplication.page.DefaultPageView;

public class SplitPanelPageActivity extends SamplePageActivity {

    private PHorizontalPanel south;
    private PHorizontalPanel east;

    public SplitPanelPageActivity() {
        super("Split Panel", "Panels");
        final DefaultPageView pageView = new DefaultPageView("Split Panel");
        pageView.setHeaderHeight(20);
        setPageView(pageView);
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PSplitLayoutPanel splitLayoutPanel = Element.newPSplitLayoutPanel();

        splitLayoutPanel.addNorth(buildComponent("north", "#f2a45c"), 50);
        splitLayoutPanel.addSouth(south = buildComponent("south", "#75ffdc"), 50);
        splitLayoutPanel.addEast(east = buildComponent("east", "#b879fc"), 100);
        PHorizontalPanel west;
        splitLayoutPanel.addWest(west = buildComponent("west", "#e8b6ea"), 100);
        splitLayoutPanel.add(buildCenterPanel());

        splitLayoutPanel.setWidgetToggleDisplayAllowed(east, true);
        splitLayoutPanel.setWidgetToggleDisplayAllowed(west, true);
        splitLayoutPanel.setWidgetSnapClosedSize(south, 40);
        splitLayoutPanel.setWidgetToggleDisplayAllowed(south, true);

        splitLayoutPanel.addLayoutResizeHandler(resizeEvent -> {
            for (final LayoutResizeData data : resizeEvent.getLayoutResizeData()) {
                if (data.getWidget() == south) {
                    PNotificationManager.showTrayNotification(getView().asWidget().getWindow(), "South size: " + data.getSize());
                } else if (data.getWidget() == east) {
                    PNotificationManager.showTrayNotification(getView().asWidget().getWindow(), "East size: " + data.getSize());
                }
            }
        });

        examplePanel.setWidget(splitLayoutPanel);
    }

    private PScrollPanel buildCenterPanel() {
        final PScrollPanel panel = Element.newPScrollPanel();
        panel.setSizeFull();
        panel.setStyleProperty("backgroundColor", "#c17d7d");
        panel.setWidget(Element.newPLabel(
                "This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow."));
        return panel;
    }

    private PHorizontalPanel buildComponent(final String name, final String color) {
        final PHorizontalPanel panel = Element.newPHorizontalPanel();
        panel.setSizeFull();
        panel.setStyleProperty("backgroundColor", color);
        final PLabel label = Element.newPLabel("The " + name + " component");
        panel.add(label);
        panel.setCellHorizontalAlignment(label, PHorizontalAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(label, PVerticalAlignment.ALIGN_MIDDLE);
        return panel;
    }

}
