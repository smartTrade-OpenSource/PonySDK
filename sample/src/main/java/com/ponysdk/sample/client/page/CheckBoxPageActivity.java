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

import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PVerticalPanel;

public class CheckBoxPageActivity extends SamplePageActivity {

    public CheckBoxPageActivity() {
        super("CheckBox", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel verticalPanel = new PVerticalPanel();

        verticalPanel.add(new PLabel("Check all days that you are available:"));

        verticalPanel.add(new PCheckBox("Monday"));
        verticalPanel.add(new PCheckBox("Tuesday"));
        verticalPanel.add(new PCheckBox("Wednesday"));
        verticalPanel.add(new PCheckBox("Thursday"));
        verticalPanel.add(new PCheckBox("Friday"));
        PCheckBox saturday = new PCheckBox("Saturday");
        saturday.setEnabled(false);
        verticalPanel.add(saturday);
        PCheckBox sunday = new PCheckBox("Sunday");
        sunday.setEnabled(false);
        verticalPanel.add(sunday);

        examplePanel.setWidget(verticalPanel);
    }
}
