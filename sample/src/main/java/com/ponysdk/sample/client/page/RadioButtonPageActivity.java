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
import com.ponysdk.ui.server.basic.PRadioButton;
import com.ponysdk.ui.server.basic.PVerticalPanel;

public class RadioButtonPageActivity extends SamplePageActivity {

    public RadioButtonPageActivity() {
        super("Radio Button", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panelTop = new PVerticalPanel();
        panelTop.setSpacing(10);

        panelTop.add(new PLabel("Select your favorite color:"));
        panelTop.add(new PRadioButton("color", "blue"));
        panelTop.add(new PRadioButton("color", "red"));

        PRadioButton yellow = new PRadioButton("color", "yellow");
        yellow.setEnabled(false);
        panelTop.add(yellow);
        panelTop.add(new PRadioButton("color", "green"));

        final PVerticalPanel panelBottom = new PVerticalPanel();
        panelBottom.setSpacing(10);

        panelBottom.add(new PLabel("Select your favorite sport:"));
        panelBottom.add(new PRadioButton("sport", "Baseball"));
        panelBottom.add(new PRadioButton("sport", "Basketball"));
        panelBottom.add(new PRadioButton("sport", "Football"));
        panelBottom.add(new PRadioButton("sport", "Hockey"));
        panelBottom.add(new PRadioButton("sport", "Soccer"));
        panelBottom.add(new PRadioButton("sport", "Water Polo"));

        panelTop.add(panelBottom);

        final PVerticalPanel panel = new PVerticalPanel();
        panel.add(panelTop);
        panel.add(panelBottom);

        examplePanel.setWidget(panel);
    }
}
