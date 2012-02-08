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
import com.ponysdk.ui.server.addon.PFloatablePanel;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PScrollPanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;

public class PositionContainerPageActivity extends PageActivity {

    public PositionContainerPageActivity() {
        super("PositionContainer", "Category");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onShowPage(Place place) {
        final PScrollPanel scrollPanel = new PScrollPanel();
        scrollPanel.setHeight("500px");
        scrollPanel.setWidth("500px");
        final PVerticalPanel content = new PVerticalPanel();
        for (int i = 0; i < 5; i++) {
            content.add(new PLabel("Label " + i));
        }
        final PHorizontalPanel controlBar = new PHorizontalPanel();
        controlBar.add(new PButton("Button 1"));
        controlBar.add(new PButton("Button 2"));
        final PFloatablePanel positionContainer = new PFloatablePanel();
        positionContainer.setWidget(controlBar);

        content.add(positionContainer);
        for (int i = 0; i < 50; i++) {
            String label = new String();
            for (int j = 0; j < 100; j++) {
                label += "a";
            }
            content.add(new PLabel(label));
        }

        scrollPanel.setWidget(content);
        pageView.getBody().setWidget(scrollPanel);

        positionContainer.setLinkedScrollPanel(scrollPanel);
    }

    @Override
    protected void onFirstShowPage() {

    }
}
