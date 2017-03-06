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

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.rich.PNotificationManager;

public class ListBoxContainsEmptyItemPageActivity extends SamplePageActivity {

    public ListBoxContainsEmptyItemPageActivity() {
        super("List Box Contains Empty Item", "Lists and Menus");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PFlowPanel panel = Element.newPFlowPanel();

        final PListBox listBox = Element.newPListBox(true);
        listBox.addItem("Altai horseBengin");
        listBox.addItem("American Warmblood");
        listBox.addItem("Falabella");
        listBox.addItem("Friesian horse");
        listBox.addItem("Mustang");
        listBox.addItem("Altai horse");

        listBox.addChangeHandler(event -> {
            if (listBox.getSelectedIndex() != -1) {
                PNotificationManager.showTrayNotification(getView().asWidget().getWindow(),
                    "Item selected : " + listBox.getSelectedItem());
            }
        });

        final PListBox multiListBox = Element.newPListBox(true);
        multiListBox.setMultipleSelect(true);
        multiListBox.setVisibleItemCount(10);
        multiListBox.addItem("Altai horseBengin");
        multiListBox.addItem("American Warmblood");
        multiListBox.addItem("Falabella");
        multiListBox.addItem("Friesian horse");
        multiListBox.addItem("Mustang");
        multiListBox.addItem("Altai horse");

        multiListBox.addChangeHandler(event -> PNotificationManager.showTrayNotification(getView().asWidget().getWindow(),
            "Item selected : " + multiListBox.getSelectedItems()));

        panel.add(listBox);
        panel.add(multiListBox);

        examplePanel.setWidget(panel);
    }

}
