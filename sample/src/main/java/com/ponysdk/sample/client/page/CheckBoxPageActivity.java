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

        final PVerticalPanel panel = new PVerticalPanel();
        panel.setSpacing(10);

        panel.add(new PLabel("Check all days that you are available:"));

        panel.add(new PCheckBox("Monday"));
        panel.add(new PCheckBox("Tuesday"));
        panel.add(new PCheckBox("Wednesday"));
        panel.add(new PCheckBox("Thursday"));
        panel.add(new PCheckBox("Friday"));
        final PCheckBox saturday = new PCheckBox("Saturday");
        saturday.setEnabled(false);
        panel.add(saturday);
        final PCheckBox sunday = new PCheckBox("Sunday");
        sunday.setEnabled(false);
        panel.add(sunday);

        examplePanel.setWidget(panel);
    }
}
