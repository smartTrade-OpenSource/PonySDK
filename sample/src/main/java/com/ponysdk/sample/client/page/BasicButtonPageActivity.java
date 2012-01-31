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

import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class BasicButtonPageActivity extends SamplePageActivity {

    public BasicButtonPageActivity() {
        super("Basic Button", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PHorizontalPanel panel = new PHorizontalPanel();
        panel.setSpacing(10);

        PButton normalButton = new PButton("Normal Button");
        panel.add(normalButton);
        PButton disabledButton = new PButton("Disabled Button");
        disabledButton.setEnabled(false);
        panel.add(disabledButton);
        PButton disabledOnRequestButton = new PButton("Disabled on request");
        disabledOnRequestButton.showLoadingOnRequest(true);

        disabledOnRequestButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent event) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {}
            }
        });

        panel.add(disabledOnRequestButton);

        pageView.getBody().setWidget(panel);
    }
}
