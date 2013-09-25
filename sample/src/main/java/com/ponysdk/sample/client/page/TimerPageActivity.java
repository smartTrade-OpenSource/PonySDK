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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.ponysdk.core.concurrent.UIScheduledThreadPoolExecutor;
import com.ponysdk.core.place.Place;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PScheduler;
import com.ponysdk.ui.server.basic.PScheduler.RepeatingCommand;
import com.ponysdk.ui.server.basic.PTerminalScheduledCommand;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PTimer;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class TimerPageActivity extends SamplePageActivity {

    protected long time1 = 0;
    protected long time2 = 0;
    protected AtomicLong time3 = new AtomicLong();

    protected PTimer timer;

    private final PTextBox textBox = new PTextBox("1000");
    private final PVerticalPanel panel = new PVerticalPanel();
    private ScheduledFuture<?> scheduleAtFixedRate;
    private PLabel labelScheduler;
    private PLabel label;

    public TimerPageActivity() {
        super("Timer", "Extra");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        label = new PLabel("0");

        final PButton scheduleRepeatingButton = new PButton("Start");
        scheduleRepeatingButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                timer.scheduleRepeating(Integer.valueOf(textBox.getText()));
            }
        });
        panel.add(new PLabel("Simple repeating timer"));
        panel.add(label);
        panel.add(textBox);
        panel.add(scheduleRepeatingButton);

        // Fixed delay
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy MM dd hh:mm:ss");
        final PLabel dateLabel = new PLabel(dateFormat.format(Calendar.getInstance().getTime()));
        panel.add(new PHTML("<br>"));
        panel.add(new PLabel("Fixed delay timer"));
        panel.add(dateLabel);
        final PButton fixedDelayButton = new PButton("Start");
        fixedDelayButton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                PScheduler.get().scheduleFixedDelay(new RepeatingCommand() {

                    @Override
                    public boolean execute() {
                        dateLabel.setText(dateFormat.format(Calendar.getInstance().getTime()));
                        return true;
                    }
                }, 1000);
            }
        });
        panel.add(fixedDelayButton);

        // Client side only
        panel.add(new PHTML("<br>"));
        panel.add(new PLabel("Timer (terminal side)"));
        final PButton changeColorsBUtton = new PButton("Start");
        changeColorsBUtton.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                final PTerminalScheduledCommand deferred1 = new PTerminalScheduledCommand() {

                    @Override
                    protected void run() {
                        changeColorsBUtton.setStyleProperty("color", "blue");
                    }
                };
                deferred1.schedule(2000);

                final PTerminalScheduledCommand deferred2 = new PTerminalScheduledCommand() {

                    @Override
                    protected void run() {
                        changeColorsBUtton.setStyleProperty("color", "orange");
                    }
                };
                deferred2.schedule(4000);

                final PTerminalScheduledCommand deferred3 = new PTerminalScheduledCommand() {

                    @Override
                    protected void run() {
                        changeColorsBUtton.setStyleProperty("color", "black");
                    }
                };
                deferred3.schedule(6000);
            }
        });
        panel.add(changeColorsBUtton);

        labelScheduler = new PLabel("0");

        panel.add(new PLabel("UI Scheduler"));
        panel.add(labelScheduler);

        examplePanel.setWidget(panel);

    }

    @Override
    protected void onShowPage(final Place place) {
        super.onShowPage(place);
        timer = new PTimer() {

            @Override
            public void run() {
                time1++;
                label.setText("" + time1);
            }
        };
        scheduleAtFixedRate = UIScheduledThreadPoolExecutor.get().scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                labelScheduler.setText(time2++ + "");

            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onLeavingPage() {
        super.onLeavingPage();
        timer.cancel();
        scheduleAtFixedRate.cancel(true);
    }

}
