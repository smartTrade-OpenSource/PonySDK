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
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.ui.server.addon.PNotificationManager;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PRadioButton;
import com.ponysdk.ui.server.basic.PRadioButtonGroup;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class RadioButtonPageActivity extends PageActivity {

    public RadioButtonPageActivity() {
        super("RadioButton", "Basics UI Components");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onShowPage(Place place) {
        final PVerticalPanel verticalPanel = new PVerticalPanel();
        verticalPanel.add(new PLabel("Group1"));
        final PHorizontalPanel group1Layout = new PHorizontalPanel();
        group1Layout.add(new PRadioButton("group1", "poney"));
        group1Layout.add(new PRadioButton("group1", "horse"));
        group1Layout.add(new PRadioButton("group1", "unicorn"));
        verticalPanel.add(group1Layout);

        verticalPanel.add(new PLabel("Group2"));
        final PHorizontalPanel group2Layout = new PHorizontalPanel();
        group2Layout.add(new PRadioButton("group2", "ford"));
        group2Layout.add(new PRadioButton("group2", "ferrari"));
        group2Layout.add(new PRadioButton("group2", "porche"));
        verticalPanel.add(group2Layout);

        verticalPanel.add(new PLabel("Colors"));
        final PHorizontalPanel group3Layout = new PHorizontalPanel();
        final PRadioButton red = new PRadioButton("red");
        group3Layout.add(red);
        final PRadioButton blue = new PRadioButton("blue");
        group3Layout.add(blue);
        final PRadioButton white = new PRadioButton("white");
        group3Layout.add(white);
        verticalPanel.add(group3Layout);

        final PRadioButtonGroup radioButtonGroup = new PRadioButtonGroup("group3");
        radioButtonGroup.registerRadioButton(red);
        radioButtonGroup.registerRadioButton(blue);
        radioButtonGroup.registerRadioButton(white);

        final PButton group3Button = new PButton("PClick!");
        verticalPanel.add(group3Button);
        group3Button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                final PRadioButton radioButton = radioButtonGroup.getValue();
                if (radioButton != null) {
                    PNotificationManager.notify("Color selected", radioButton.getText());
                } else {
                    PNotificationManager.notify("No Color selected ", "");
                }

            }
        });

        final PButton group4Button = new PButton("Enable button");
        group4Button.setStyleName(PonySDKTheme.BUTTON_GRAY);

        final PButton group5Button = new PButton("Disabled button");
        group5Button.setEnabled(false);
        group5Button.setStyleName(PonySDKTheme.BUTTON_GRAY);

        verticalPanel.add(group4Button);
        verticalPanel.add(group5Button);

        pageView.getBody().setWidget(verticalPanel);
    }

    @Override
    protected void onFirstShowPage() {

    }
}
