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
import com.ponysdk.ui.server.addon.PDialogBox;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PChangeHandler;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class ListBoxPageActivity extends PageActivity {

    public ListBoxPageActivity() {
        super("ListBox", "Basics UI Components");
    }

    @Override
    protected void onInitialization() {
    }

    @Override
    protected void onShowPage(Place place) {
    }

    @Override
    protected void onLeavingPage() {
    }

    @Override
    protected void onFirstShowPage() {

        final PVerticalPanel layout = new PVerticalPanel();
        pageView.getBody().setWidget(layout);

        final PListBox listBox = new PListBox(true, true);

        listBox.addItem("AAAAAAAA");
        listBox.addItem("BBBBBBBBB");
        listBox.addItem("CCCCCCCCC");
        listBox.addItem("DDDDDDDDD");
        listBox.addItem("EEEEEEEEE");
        listBox.addItem("FFFFFFFFF");

        listBox.addChangeHandler(new PChangeHandler() {

            @Override
            public void onChange(Object source, int selectedIndex) {
                final PDialogBox dialogBox = new PDialogBox(true);

                String msg = "Current selected index : " + selectedIndex + "\n";
                msg += "Selected indexes :  ";

                for (int i = 0; i < listBox.getItemCount(); i++) {
                    if (listBox.isItemSelected(i))
                        msg += ", " + i;
                }

                dialogBox.setWidget(new PLabel(msg));
                dialogBox.center();
                dialogBox.show();
            }
        });

        layout.add(listBox);

        final PButton clear = new PButton("clear");
        clear.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                listBox.clear();
            }
        });

        final PButton add = new PButton("Add item");
        add.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                listBox.addItem("Item_" + System.currentTimeMillis());
            }
        });

        layout.add(clear);
        layout.add(add);
    }
}
