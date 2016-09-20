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

import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PRadioButton;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.rich.PNotificationManager;

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

        panelTop.add(newPRadioButton("color", "blue"));
        panelTop.add(newPRadioButton("color", "red"));

        final PRadioButton yellow = newPRadioButton("color", "yellow");
        yellow.setEnabled(false);
        panelTop.add(yellow);

        panelTop.add(newPRadioButton("color", "green"));

        final PVerticalPanel panelBottom = new PVerticalPanel();
        panelBottom.setSpacing(10);

        panelBottom.add(new PLabel("Select your favorite sport:"));
        panelBottom.add(newPRadioButton("sport", "Polo"));
        panelBottom.add(newPRadioButton("sport", "Rodeo"));
        panelBottom.add(newPRadioButton("sport", "Horse racing"));
        panelBottom.add(newPRadioButton("sport", "Dressage"));
        panelBottom.add(newPRadioButton("sport", "Endurance riding"));
        panelBottom.add(newPRadioButton("sport", "Eventing"));
        panelBottom.add(newPRadioButton("sport", "Reining"));
        panelBottom.add(newPRadioButton("sport", "Show jumping"));
        panelBottom.add(newPRadioButton("sport", "Tent pegging"));
        panelBottom.add(newPRadioButton("sport", "Vaulting"));

        panelTop.add(panelBottom);

        final PVerticalPanel panel = new PVerticalPanel();
        panel.add(panelTop);
        panel.add(panelBottom);

        examplePanel.setWidget(panel);
    }

    private PRadioButton newPRadioButton(final String name, final String label) {
        final PRadioButton radioButton = new PRadioButton(name, label);
        radioButton.addValueChangeHandler(this);
        return radioButton;
    }

    @Override
    public void onValueChange(final PValueChangeEvent<Boolean> event) {
        final PRadioButton radioButton = (PRadioButton) event.getSource();
        PNotificationManager.showTrayNotification(getView().asWidget().getWindowID(),
                "Name = " + radioButton.getName() + " Text = " + radioButton.getText() + " Value = " + radioButton.getValue());
    }
}
