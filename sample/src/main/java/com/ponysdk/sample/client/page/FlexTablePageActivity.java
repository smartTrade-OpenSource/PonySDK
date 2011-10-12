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
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PTimer;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class FlexTablePageActivity extends PageActivity {

    private final PFlexTable table = new PFlexTable();

    protected long time = 0;

    protected PTimer currentTimer;

    private final PLabel[][] labels = new PLabel[100][6];

    private PTextBox textBox;

    public FlexTablePageActivity() {
        super("FlexTable", "Fast UI Components");
    }

    @Override
    protected void onInitialization() {
    }

    @Override
    protected void onShowPage(Place place) {
        if (currentTimer != null) {
            currentTimer.scheduleRepeating(Integer.valueOf(textBox.getText()));
        }
    }

    @Override
    protected void onLeavingPage() {
        if (currentTimer != null) {
            currentTimer.cancel();
        }
    }

    @Override
    protected void onFirstShowPage() {
        table.setCellPadding(0);
        table.setCellSpacing(0);
        table.setSizeFull();

        final PVerticalPanel bodyLayout = new PVerticalPanel();
        bodyLayout.setSizeFull();
        pageView.getBody().setWidget(bodyLayout);

        final PButton button = new PButton("Remove last row");
        button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                final int lastRow = table.getRowCount() - 1;
                table.removeRow(lastRow);
            }
        });

        bodyLayout.add(button);
        bodyLayout.add(table);

        textBox = new PTextBox();

        final PButton scheduleButton = new PButton("Schedule");
        scheduleButton.addClickHandler(new PClickHandler() {
            @Override
            public void onClick(PClickEvent clickEvent) {
                if (currentTimer != null) {
                    currentTimer.cancel();
                }
                currentTimer = new PTimer() {

                    @Override
                    public void run() {
                        time++;
                        updateTableData(100);
                    }
                };

                for (int r = 0; r < 100; r++) {
                    for (int c = 0; c < 6; c++) {
                        final PLabel label = new PLabel(r + "_" + c + Math.random());
                        labels[r][c] = label;
                        table.setWidget(r, c, label);
                    }
                }

                currentTimer.scheduleRepeating(Integer.valueOf(textBox.getText()));
            }
        });
        bodyLayout.add(textBox);
        bodyLayout.add(scheduleButton);
    }

    private void updateTableData(int rowCount) {
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < 6; c++) {
                labels[r][c].setText(r + "_" + c + Math.random());
            }
        }
    }
}
