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

import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PTwinListBox;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PChangeHandler;

public class TwinListBoxPageActivity extends SamplePageActivity {

    public TwinListBoxPageActivity() {
        super("Twin List Box", "Lists and Menus");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PTwinListBox<String> twinListBox = new PTwinListBox<String>("From", "To");

        final PListBox leftListBox = twinListBox.getLeftListBox();
        leftListBox.addItem("Item1");
        leftListBox.addItem("Item2");
        leftListBox.addItem("Item3");
        leftListBox.addItem("Item4");
        leftListBox.addItem("Item5");
        leftListBox.setVisibleItemCount(10);
        leftListBox.addChangeHandler(new PChangeHandler() {

            @Override
            public void onChange(final PChangeEvent event) {
                PNotificationManager.showTrayNotification("Item selected : " + leftListBox.getSelectedItem());
            }
        });

        final PListBox rightListBox = twinListBox.getRightListBox();
        rightListBox.addItem("Item6");
        rightListBox.addItem("Item7");
        rightListBox.addItem("Item8");
        rightListBox.addItem("Item9");
        rightListBox.addItem("Item10");
        rightListBox.setVisibleItemCount(10);
        rightListBox.addChangeHandler(new PChangeHandler() {

            @Override
            public void onChange(final PChangeEvent event) {
                PNotificationManager.showTrayNotification("Item selected : " + rightListBox.getSelectedItem());
            }
        });

        examplePanel.setWidget(twinListBox);
    }
}
