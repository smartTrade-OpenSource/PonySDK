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

import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PSplitLayoutPanel;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class SplitPanelPageActivity extends SamplePageActivity {

    public SplitPanelPageActivity() {
        super("Split Panel", "Panels");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        PSplitLayoutPanel dockLayoutPanel = new PSplitLayoutPanel();

        dockLayoutPanel.addNorth(buildComponent("north", "#f2a45c"), 50);
        dockLayoutPanel.addSouth(buildComponent("south", "#75ffdc"), 50);
        dockLayoutPanel.addEast(buildComponent("east", "#b879fc"), 100);
        dockLayoutPanel.addWest(buildComponent("west", "#e8b6ea"), 100);
        dockLayoutPanel.add(buildCenterPanel());

        examplePanel.setWidget(dockLayoutPanel);
    }

    private PScrollPanel buildCenterPanel() {
        PScrollPanel panel = new PScrollPanel();
        panel.setSizeFull();
        panel.setStyleProperty("backgroundColor", "#c17d7d");
        panel.setWidget(new PLabel(
                "This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow. This is some text to show how the contents on either side of the splitter flow."));
        return panel;
    }

    private PHorizontalPanel buildComponent(final String name, final String color) {
        PHorizontalPanel panel = new PHorizontalPanel();
        panel.setSizeFull();
        panel.setStyleProperty("backgroundColor", color);
        PLabel label = new PLabel("The " + name + " component");
        panel.add(label);
        panel.setCellHorizontalAlignment(label, PHorizontalAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(label, PVerticalAlignment.ALIGN_MIDDLE);
        return panel;
    }

}
