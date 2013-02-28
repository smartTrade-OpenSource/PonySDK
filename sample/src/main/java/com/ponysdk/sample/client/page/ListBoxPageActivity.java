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

import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PScheduler;
import com.ponysdk.ui.server.basic.PScheduler.RepeatingCommand;
import com.ponysdk.ui.server.basic.PScript;
import com.ponysdk.ui.server.basic.PScript.ExecutionCallback;

public class ListBoxPageActivity extends SamplePageActivity {

    public ListBoxPageActivity() {
        super("List Box", "Lists and Menus");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PFlexTable table = new PFlexTable();

        final PListBox listBoxCategory = new PListBox();
        listBoxCategory.setWidth("100%");
        listBoxCategory.addItem("Test1");
        listBoxCategory.addItem("Test2");
        listBoxCategory.addItem("Test3");
        listBoxCategory.addItem("Test2");
        listBoxCategory.addItem("Test2");
        listBoxCategory.addItem("Test2");
        listBoxCategory.addItem("Test2");
        listBoxCategory.addItem("Test2");
        listBoxCategory.addItem("Test2");

        listBoxCategory.addStyleName("chzn-select"); // normal
        // listBoxCategory.addStyleName("chzn-rtl"); // align to right
        listBoxCategory.addStyleName("chzn-select-deselect"); // allow deselect

        // listBoxCategory.setAttribute("multiple", ""); // enable multiselect
        listBoxCategory.setAttribute("data-placeholder", "oh oh th eplace holder"); // place holder

        PScheduler.get().scheduleFixedRate(new RepeatingCommand() {

            @Override
            public boolean execute() {
                PScript.get().execute("$(\".chzn-select\").chosen();", new ExecutionCallback() {

                    @Override
                    public void onSuccess(final String msg) {
                        System.err.println("on succes " + msg);
                    }

                    @Override
                    public void onFailure(final String msg) {
                        System.err.println("on onFailure " + msg);
                    }
                });
                return false;
            }
        }, 0);
        PScheduler.get().scheduleFixedRate(new RepeatingCommand() {

            @Override
            public boolean execute() {
                PScript.get().execute("$(\".chzn-select-deselect\").chosen({allow_single_deselect:true});", new ExecutionCallback() {

                    @Override
                    public void onSuccess(final String msg) {
                        System.err.println("on succes " + msg);
                    }

                    @Override
                    public void onFailure(final String msg) {
                        System.err.println("on onFailure " + msg);
                    }
                });
                return false;
            }
        }, 0);

        // final PChosenListBox listBoxApplied = new PChosenListBox();
        // listBoxApplied.setVisibleItemCount(20);
        //
        // listBoxApplied.addChangeHandler(new PChangeHandler() {
        //
        // @Override
        // public void onChange(final PChangeEvent event) {
        // PNotificationManager.showTrayNotification("Item selected : " + listBoxApplied.getSelectedItem());
        // }
        // });
        //
        // fillSports(listBoxApplied);
        //
        // final PCheckBox checkBox = new PCheckBox("Enable multi-selection");
        // checkBox.addValueChangeHandler(new PValueChangeHandler<Boolean>() {
        //
        // @Override
        // public void onValueChange(final PValueChangeEvent<Boolean> event) {
        // listBoxApplied.setMultiSelect(event.getValue());
        // }
        // });
        //
        // listBoxCategory.addChangeHandler(new PChangeHandler() {
        //
        // @Override
        // public void onChange(final PChangeEvent event) {
        // listBoxApplied.clear();
        //
        // if ("Sports".equals(listBoxCategory.getSelectedItem())) {
        // fillSports(listBoxApplied);
        // } else {
        // fillPony(listBoxApplied);
        // }
        // }
        //
        // });

        table.setWidth("100%");
        table.setWidget(0, 0, listBoxCategory);
        // table.setWidget(1, 1, listBoxApplied);
        // table.setWidget(0, 2, checkBox);

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
