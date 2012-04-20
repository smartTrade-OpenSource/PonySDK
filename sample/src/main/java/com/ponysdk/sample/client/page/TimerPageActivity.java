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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.ponysdk.core.place.Place;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PScheduler;
import com.ponysdk.ui.server.basic.PScheduler.RepeatingCommand;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PTimer;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class TimerPageActivity extends SamplePageActivity {

    protected long time = 0;

    protected PTimer timer;

    private boolean enableDateTimer = true;

    private PTextBox textBox;

    private PLabel date;

    private SimpleDateFormat dateFormat;

    public TimerPageActivity() {
        super("Timer Page", "Timer");
    }

    @Override
    protected void onShowPage(final Place place) {
        super.onShowPage(place);

        enableDateTimer = true;

        PScheduler.get().scheduleFixedDelay(new RepeatingCommand() {

            @Override
            public boolean execute() {
                date.setText(dateFormat.format(Calendar.getInstance().getTime()));
                return enableDateTimer;
            }
        }, 1000);
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = new PVerticalPanel();
        final PLabel label = new PLabel("0");

        textBox = new PTextBox("1000");

        timer = new PTimer() {

            @Override
            public void run() {
                time++;
                label.setText("" + time);
            }
        };

        final PButton scheduleRepeatingButton = new PButton("Schedule repeating");
        scheduleRepeatingButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                timer.scheduleRepeating(Integer.valueOf(textBox.getText()));
            }
        });

        dateFormat = new SimpleDateFormat("yyyy MM dd hh:mm:ss");
        date = new PLabel(dateFormat.format(Calendar.getInstance().getTime()));

        final PHorizontalPanel datePanel = new PHorizontalPanel();
        datePanel.add(new PHTML("Date :"));
        datePanel.add(date);

        panel.add(label);
        panel.add(textBox);
        panel.add(scheduleRepeatingButton);
        panel.add(datePanel);

        examplePanel.setWidget(panel);
    }

    @Override
    protected void onLeavingPage() {
        super.onLeavingPage();
        enableDateTimer = false;
        timer.cancel();
    }

}
