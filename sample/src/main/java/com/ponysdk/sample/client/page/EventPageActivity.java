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

import com.ponysdk.core.event.BusinessEvent.Level;
import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.sample.client.event.DemoBusinessEvent;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class EventPageActivity extends PageActivity {

    public EventPageActivity() {
        super("PNotificationManager by event", "PNotificationManager");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onShowPage(Place place) {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onFirstShowPage() {
        final PVerticalPanel panel = new PVerticalPanel();

        final PHorizontalPanel infoPanel = new PHorizontalPanel();
        final PTextBox textField = new PTextBox();
        final PButton ok = new PButton("send [INFO]");
        ok.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                final DemoBusinessEvent businessEvent = new DemoBusinessEvent(EventPageActivity.this);
                businessEvent.setBusinessMessage(textField.getText());
                fireEvent(businessEvent);
            }
        });

        infoPanel.add(textField);
        infoPanel.add(ok);

        final PHorizontalPanel warningPanel = new PHorizontalPanel();
        final PTextBox textField2 = new PTextBox();
        final PButton ok2 = new PButton("send [WARN]");

        ok2.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                final DemoBusinessEvent businessEvent = new DemoBusinessEvent(EventPageActivity.this);
                businessEvent.setLevel(Level.WARNING);
                businessEvent.setBusinessMessage(textField2.getText());
                fireEvent(businessEvent);
            }
        });

        warningPanel.add(textField2);
        warningPanel.add(ok2);

        final PHorizontalPanel errorPanel = new PHorizontalPanel();
        final PTextBox textField3 = new PTextBox();
        final PButton ok3 = new PButton("send [ERROR]");

        ok3.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                final DemoBusinessEvent businessEvent = new DemoBusinessEvent(EventPageActivity.this);
                businessEvent.setLevel(Level.ERROR);
                businessEvent.setBusinessMessage(textField3.getText());
                fireEvent(businessEvent);
            }
        });

        errorPanel.add(textField3);
        errorPanel.add(ok3);

        panel.add(infoPanel);
        panel.add(warningPanel);
        panel.add(errorPanel);

        pageView.getBody().setWidget(panel);
    }
}
