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
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeHandler;

public class ListBoxContainsEmptyItemPageActivity extends SamplePageActivity {

    public ListBoxContainsEmptyItemPageActivity() {
        super("List Box Contains Empty Item", "Lists and Menus");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PFlowPanel panel = new PFlowPanel();

        final PListBox listBox = new PListBox(true);
        listBox.addItem("Altai horseBengin");
        listBox.addItem("American Warmblood");
        listBox.addItem("Falabella");
        listBox.addItem("Friesian horse");
        listBox.addItem("Mustang");
        listBox.addItem("Altai horse");

        listBox.addChangeHandler(new PChangeHandler() {

            @Override
            public void onChange(final PChangeEvent event) {
                if (listBox.getSelectedIndex() != -1) {
                    PNotificationManager.showTrayNotification("Item selected : " + listBox.getSelectedItem());
                }
            }
        });

        final PCheckBox checkBox = new PCheckBox("Enable multi-selection");
        checkBox.addValueChangeHandler(new PValueChangeHandler<Boolean>() {

            @Override
            public void onValueChange(final PValueChangeEvent<Boolean> event) {
                listBox.setMultiSelect(event.getValue());
            }
        });

        panel.add(listBox);
        panel.add(checkBox);

        examplePanel.setWidget(panel);
    }

}
