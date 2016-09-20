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

import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;
import com.ponysdk.core.ui.basic.event.PValueChangeHandler;
import com.ponysdk.core.ui.rich.PNotificationManager;

public class CheckBoxPageActivity extends SamplePageActivity implements PValueChangeHandler<Boolean> {

    public CheckBoxPageActivity() {
        super("CheckBox", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = new PVerticalPanel();
        panel.setSpacing(10);

        panel.add(new PLabel("Check all days that you are available:"));

        final PCheckBox monday = new PCheckBox("Monday");
        monday.addValueChangeHandler(this);
        panel.add(monday);

        final PCheckBox tuesday = new PCheckBox("Tuesday");
        tuesday.addValueChangeHandler(this);
        panel.add(tuesday);

        final PCheckBox wednesday = new PCheckBox("Wednesday");
        wednesday.addValueChangeHandler(this);
        panel.add(wednesday);

        final PCheckBox thursday = new PCheckBox("Thursday");
        thursday.addValueChangeHandler(this);
        panel.add(thursday);

        final PCheckBox friday = new PCheckBox("Friday");
        friday.addValueChangeHandler(this);
        panel.add(friday);

        final PCheckBox saturday = new PCheckBox("Saturday");
        saturday.addValueChangeHandler(this);
        saturday.setEnabled(false);
        panel.add(saturday);

        final PCheckBox sunday = new PCheckBox("Sunday");
        sunday.addValueChangeHandler(this);
        sunday.setEnabled(false);
        panel.add(sunday);

        examplePanel.setWidget(panel);
    }

    @Override
    public void onValueChange(final PValueChangeEvent<Boolean> event) {
        final PCheckBox checkBox = (PCheckBox) event.getSource();
        PNotificationManager.showTrayNotification(getView().asWidget().getWindowID(),
                checkBox.getText() + (event.getValue() ? " is checked" : " is unchecked"));
    }
}
