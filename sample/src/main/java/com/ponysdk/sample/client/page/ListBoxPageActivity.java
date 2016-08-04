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

import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.rich.PNotificationManager;

public class ListBoxPageActivity extends SamplePageActivity {

    public ListBoxPageActivity() {
        super("List Box", "Lists and Menus");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PFlexTable table = new PFlexTable();

        final PListBox listBoxCategory = new PListBox();
        listBoxCategory.addItem("Test1");
        listBoxCategory.addItem("Test2");
        listBoxCategory.addItem("Test3");
        listBoxCategory.addItem("Test2");
        listBoxCategory.addItem("Test2");
        listBoxCategory.addItem("Test2");
        listBoxCategory.addItem("Test2");
        listBoxCategory.addItem("Test2");
        listBoxCategory.addItem("Test2");

        table.setWidth("100%");
        table.setWidget(0, 0, listBoxCategory);

        final PListBox roleListBox = new PListBox(false);
        roleListBox.setMultipleSelect(true);
        roleListBox.setVisibleItemCount(5);
        for (int i = 0; i < 10; i++) {
            roleListBox.addItem("Role" + i, i);
        }

        final PButton selectedRole = new PButton("Selected roles [1,2]");
        selectedRole.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                roleListBox.setSelectedValue(1);
                roleListBox.setSelectedValue(2);

                PNotificationManager.showHumanizedNotification(getView().asWidget().getWindowID(),
                        "Selected items " + roleListBox.getSelectedItems());
            }
        });
        final PButton unSelectedRole = new PButton("Selected roles [1,2]");
        unSelectedRole.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                roleListBox.setSelectedValue(1, false);
                roleListBox.setSelectedValue(2, false);
                PNotificationManager.showHumanizedNotification(getView().asWidget().getWindowID(),
                        "Unselected items " + roleListBox.getSelectedItems());
            }
        });
        table.setWidget(1, 0, roleListBox);
        table.setWidget(1, 1, selectedRole);
        table.setWidget(1, 2, unSelectedRole);

        examplePanel.setWidget(table);
    }

    protected void fillSports(final PListBox listBoxApplied) {
        listBoxApplied.addItemsInGroup("sport", "Baseball", "Basketball", "Football", "Hockey", "Water Polo");
        listBoxApplied.addItemsInGroup("test", "Baseball", "Basketball", "Football", "Hockey", "Water Polo");
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
