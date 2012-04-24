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

import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.select.PMultiSelectListBox;

public class MultiSelectionPageActivity extends SamplePageActivity {

    public MultiSelectionPageActivity() {
        super("MultiSelectListBox", "Rich UI Components");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PMultiSelectListBox listViewActivity = new PMultiSelectListBox();

        listViewActivity.addItem("Friesian horse");
        listViewActivity.addItem("Altai horseBengin");
        listViewActivity.addItem("Altai horse");
        listViewActivity.addItem("American Warmblood");
        listViewActivity.addItem("Falabella");

        final PSimplePanel panel = new PSimplePanel();

        panel.setWidget(listViewActivity);

        examplePanel.setWidget(panel);

    }

}
