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

import com.ponysdk.core.event.PBusinessEvent.Level;
import com.ponysdk.sample.client.event.DemoBusinessEvent;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PNotificationManager.Notification;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class EventPageActivity extends SamplePageActivity {

    public EventPageActivity() {
        super("Notifications", "Rich UI Components");
    }

    @Override
    protected void onFirstShowPage() {

        super.onFirstShowPage();

        final PVerticalPanel panel = new PVerticalPanel();

        // Send 'info' business event
        final PHorizontalPanel infoPanel = new PHorizontalPanel();
        final PTextBox textField = new PTextBox("This is an info event");
        final PButton ok = new PButton("send [INFO]");
        ok.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final DemoBusinessEvent businessEvent = new DemoBusinessEvent(EventPageActivity.this);
                businessEvent.setBusinessMessage(textField.getText());
                fireEvent(businessEvent);
            }
        });

        infoPanel.add(textField);
        infoPanel.add(ok);

        // Send 'warn' business event
        final PHorizontalPanel warningPanel = new PHorizontalPanel();
        final PTextBox textField2 = new PTextBox("This is a warning event");
        final PButton ok2 = new PButton("send [WARN]");

        ok2.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final DemoBusinessEvent businessEvent = new DemoBusinessEvent(EventPageActivity.this);
                businessEvent.setLevel(Level.WARNING);
                businessEvent.setBusinessMessage(textField2.getText());
                fireEvent(businessEvent);
            }
        });

        warningPanel.add(textField2);
        warningPanel.add(ok2);

        // Send 'error' business level
        final PHorizontalPanel errorPanel = new PHorizontalPanel();
        final PTextBox textField3 = new PTextBox("This is an error event");
        final PButton ok3 = new PButton("send [ERROR]");

        ok3.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final DemoBusinessEvent businessEvent = new DemoBusinessEvent(EventPageActivity.this);
                businessEvent.setLevel(Level.ERROR);
                businessEvent.setBusinessMessage(textField3.getText());
                fireEvent(businessEvent);
            }
        });

        errorPanel.add(textField3);
        errorPanel.add(ok3);

        // Show 'tray' notification
        final PHorizontalPanel trayPanel = new PHorizontalPanel();
        final PTextBox textField4 = new PTextBox("This is a tray notification");
        final PButton ok4 = new PButton("show [TRAY]");

        ok4.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                PNotificationManager.notify(textField4.getText(), Notification.TRAY);
            }
        });

        trayPanel.add(textField4);
        trayPanel.add(ok4);

        // Build page

        panel.add(infoPanel);
        panel.add(warningPanel);
        panel.add(errorPanel);
        panel.add(trayPanel);

        examplePanel.setWidget(panel);
    }
}
