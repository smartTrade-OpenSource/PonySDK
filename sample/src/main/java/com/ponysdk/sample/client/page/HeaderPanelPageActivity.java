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

import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PHeaderPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PScrollPanel;
import com.ponysdk.core.ui.basic.PWidget;

public class HeaderPanelPageActivity extends SamplePageActivity {

    public HeaderPanelPageActivity() {
        super("Header Panel", "Panels");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PHeaderPanel headerPanel = new PHeaderPanel();
        headerPanel.setHeaderWidget(getDatas("Header", 3));
        headerPanel.setFooterWidget(getDatas("Footer", 2));

        final PScrollPanel scroll = new PScrollPanel();
        scroll.setWidget(getDatas("Content", 100));
        scroll.setHeight("100%");
        scroll.setWidth("100%");
        headerPanel.setContentWidget(scroll);

        headerPanel.getHeaderWidget().setStyleProperty("border-bottom", "1px solid #EEE");
        headerPanel.getFooterWidget().setStyleProperty("border-top", "1px solid #EEE");

        examplePanel.setWidget(headerPanel);
    }

    private PWidget getDatas(final String label, final int rowCount) {
        final PFlowPanel flow = new PFlowPanel();
        for (int i = 0; i < rowCount; i++) {
            flow.add(new PLabel(label + " - line " + i));
        }
        return flow;
    }

}
