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
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PTimer;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class TimerPageActivity extends PageActivity {

    protected long time = 0;

    protected PTimer currentTimer;

    protected PTimer timer;

    private PVerticalPanel verticalPanel;

    public TimerPageActivity() {
        super("Timer Page", "Category");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onShowPage(Place place) {
        pageView.getBody().setWidget(verticalPanel);
    }

    @Override
    protected void onFirstShowPage() {

        verticalPanel = new PVerticalPanel();
        final PLabel label = new PLabel("0");
        verticalPanel.add(label);

        final PTextBox textBox = new PTextBox("1000");

        final PButton scheduleRepeatingButton = new PButton("Schedule repeating");
        scheduleRepeatingButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                if (currentTimer != null) {
                    currentTimer.cancel();
                }
                currentTimer = new PTimer() {

                    @Override
                    public void run() {
                        time++;
                        label.setText("" + time);
                    }
                };
                currentTimer.scheduleRepeating(Integer.valueOf(textBox.getText()));
            }
        });
        verticalPanel.add(textBox);
        verticalPanel.add(scheduleRepeatingButton);

        timer = new PTimer() {

            @Override
            public void run() {
                time++;
                label.setText("Timer executed" + time);
            }
        };

        PButton scheduleButton = new PButton("Schedule");
        scheduleButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                label.setText("Timer scheduled....");
                timer.schedule(Integer.valueOf(textBox.getText()));
            }
        });
        verticalPanel.add(scheduleButton);
    }
}
