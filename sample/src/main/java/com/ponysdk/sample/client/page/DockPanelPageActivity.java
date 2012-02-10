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

import com.ponysdk.ui.server.basic.PDockLayoutPanel;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class DockPanelPageActivity extends SamplePageActivity {

    public DockPanelPageActivity() {
        super("Dock Panel", "Panels");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        PDockLayoutPanel dockLayoutPanel = new PDockLayoutPanel();

        dockLayoutPanel.addNorth(buildNorthPanel(), 50);
        dockLayoutPanel.addEast(new PLabel("east"), 30);
        dockLayoutPanel.addLineEnd(new PLabel("Line end"), 30);
        dockLayoutPanel.addSouth(buildSouthPanel(), 50);
        dockLayoutPanel.addLineStart(new PLabel("Line start"), 30);

        PScrollPanel scrollPanel = buildCenterPanel();

        dockLayoutPanel.add(scrollPanel);

        examplePanel.setWidget(dockLayoutPanel);
    }

    private PScrollPanel buildCenterPanel() {
        PScrollPanel panel = new PScrollPanel();
        panel.setSizeFull();
        panel.setStyleProperty("backgroundColor", "#e8b6ea");
        panel.setWidget(new PLabel("center"));
        return panel;
    }

    private PHorizontalPanel buildNorthPanel() {
        PHorizontalPanel panel = new PHorizontalPanel();
        panel.setSizeFull();
        panel.setStyleProperty("backgroundColor", "#edede8");
        PLabel label = new PLabel("The north component");
        panel.add(label);
        panel.setCellHorizontalAlignment(label, PHorizontalAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(label, PVerticalAlignment.ALIGN_MIDDLE);
        return panel;
    }

    private PHorizontalPanel buildSouthPanel() {
        PHorizontalPanel panel = new PHorizontalPanel();
        panel.setSizeFull();
        panel.setStyleProperty("backgroundColor", "#336293");
        PLabel label = new PLabel("The south component");
        panel.add(label);
        panel.setCellHorizontalAlignment(label, PHorizontalAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(label, PVerticalAlignment.ALIGN_MIDDLE);
        return panel;
    }

}
