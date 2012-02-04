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

import java.util.List;

import com.ponysdk.core.PonySession;
import com.ponysdk.core.query.Query;
import com.ponysdk.core.query.Result;
import com.ponysdk.sample.client.datamodel.Pony;
import com.ponysdk.sample.client.event.DemoBusinessEvent;
import com.ponysdk.sample.command.pony.FindPonysCommand;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PSuggestBox;
import com.ponysdk.ui.server.basic.PSuggestBox.PSuggestOracle;
import com.ponysdk.ui.server.basic.PSuggestion;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;

public class SuggestBoxPageActivity extends SamplePageActivity {

    public SuggestBoxPageActivity() {
        super("Suggest Box", "Lists and Menus");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = new PVerticalPanel();

        panel.add(new PLabel("Choose a word:"));

        final PSuggestBox suggestBox = new PSuggestBox();
        PSuggestOracle suggestOracle = suggestBox.getSuggestOracle();

        suggestBox.addSelectionHandler(new PSelectionHandler<PSuggestion>() {

            @Override
            public void onSelection(final PSelectionEvent<PSuggestion> event) {
                String msg = "Selected item : " + event.getSelectedItem().getReplacementString();
                PonySession.getRootEventBus().fireEvent(new DemoBusinessEvent(msg));
            }
        });

        Query query = new Query();
        FindPonysCommand command = new FindPonysCommand(query);
        Result<List<Pony>> ponys = command.execute();

        for (Pony pony : ponys.getData()) {
            suggestOracle.add(pony.getName());
        }

        panel.add(suggestBox);

        examplePanel.setWidget(panel);
    }
}
