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

import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PRadioButton;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;

public class RadioButtonPageActivity extends SamplePageActivity implements PValueChangeHandler<Boolean> {

    public RadioButtonPageActivity() {
        super("Radio Button", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panelTop = new PVerticalPanel();
        panelTop.setSpacing(10);

        panelTop.add(new PLabel("Select your favorite color:"));

        final PVerticalPanel panelBottom = new PVerticalPanel();
        panelBottom.setSpacing(10);

        panelBottom.add(new PLabel("Select your favorite sport:"));
        panelBottom.add(newPRadioButton("sport", "Polo"));
        panelBottom.add(newPRadioButton("sport", "Vaulting"));

        panelTop.add(panelBottom);

        final PVerticalPanel panel = new PVerticalPanel();
        panel.add(panelTop);
        panel.add(panelBottom);

        examplePanel.setWidget(panel);
    }

    private PRadioButton newPRadioButton(final String name, final String label) {
        final PRadioButton radioButton = new PRadioButton(name, label);
        if (label.equals("Polo")) radioButton.setValue(true);
        radioButton.addValueChangeHandler(this);
        return radioButton;
    }

    @Override
    public void onValueChange(final PValueChangeEvent<Boolean> event) {
        final PRadioButton radioButton = (PRadioButton) event.getSource();
        PNotificationManager.showTrayNotification("Name = " + radioButton.getName() + " Text = " + radioButton.getText() + " Value = " + radioButton.getValue());
    }
}
