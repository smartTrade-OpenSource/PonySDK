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

import com.ponysdk.ui.server.addon.PNotificationManager;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.event.PChangeHandler;

public class ListBoxPageActivity extends SamplePageActivity {

    public ListBoxPageActivity() {
        super("List Box", "Lists and Menus");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PFlexTable table = new PFlexTable();

        final PListBox listBoxCategory = new PListBox();
        listBoxCategory.addItem("Sports");
        listBoxCategory.addItem("Pony");

        final PListBox listBoxApplied = new PListBox();
        listBoxApplied.setVisibleItemCount(10);

        listBoxApplied.addChangeHandler(new PChangeHandler() {

            @Override
            public void onChange(final Object source, final int selectedIndex) {
                PNotificationManager.showTrayNotification("Item selected : " + listBoxApplied.getSelectedItem());
            }
        });

        fillSports(listBoxApplied);

        listBoxCategory.addChangeHandler(new PChangeHandler() {

            @Override
            public void onChange(final Object source, final int selectedIndex) {
                listBoxApplied.clear();

                if (listBoxCategory.getItem(selectedIndex).equals("Sports")) {
                    fillSports(listBoxApplied);
                } else {
                    fillPony(listBoxApplied);
                }
            }

        });

        table.setWidget(0, 0, new PLabel("Select a category:"));
        table.setWidget(1, 0, listBoxCategory);
        table.setWidget(0, 1, new PLabel("Select all that apply:"));
        table.setWidget(1, 1, listBoxApplied);

        examplePanel.setWidget(table);
    }

    protected void fillSports(final PListBox listBoxApplied) {
        listBoxApplied.addItem("Baseball");
        listBoxApplied.addItem("Basketball");
        listBoxApplied.addItem("Football");
        listBoxApplied.addItem("Hockey");
        listBoxApplied.addItem("Water Polo");
    }

    protected void fillPony(final PListBox listBoxApplied) {
        listBoxApplied.addItem("Altai horseBengin");
        listBoxApplied.addItem("American Warmblood");
        listBoxApplied.addItem("Falabella");
        listBoxApplied.addItem("Friesian horse");
        listBoxApplied.addItem("Mustang");
        listBoxApplied.addItem("Altai horse");
    }
}
