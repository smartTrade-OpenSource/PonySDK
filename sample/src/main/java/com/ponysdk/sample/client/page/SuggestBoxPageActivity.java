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

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.service.query.Query;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PListBox;
import com.ponysdk.core.ui.basic.PSuggestBox;
import com.ponysdk.core.ui.basic.PSuggestBox.PMultiWordSuggestOracle;
import com.ponysdk.core.ui.basic.PSuggestOracle.PSuggestion;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.event.PChangeEvent;
import com.ponysdk.core.ui.basic.event.PChangeHandler;
import com.ponysdk.core.ui.basic.event.PSelectionEvent;
import com.ponysdk.core.ui.basic.event.PSelectionHandler;
import com.ponysdk.sample.client.event.DemoBusinessEvent;

public class SuggestBoxPageActivity extends SamplePageActivity {

    private int current = 0;

    public SuggestBoxPageActivity() {
        super("Suggest Box", "Text Input");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = Element.newPVerticalPanel();

        panel.add(Element.newPLabel("Choose a word:"));

        final PSuggestBox suggestBox = Element.newPSuggestBox();
        suggestBox.setLimit(10);

        final PMultiWordSuggestOracle suggestOracle = (PMultiWordSuggestOracle) suggestBox.getSuggestOracle();
        suggestBox.addSelectionHandler(event -> {
            final String msg = "Selected item : " + event.getSelectedItem().getReplacementString();
            UIContext.getRootEventBus().fireEvent(new DemoBusinessEvent(msg));
        });

        final Query query = new Query();
        // final FindPonysCommand command = new FindPonysCommand(query);
        // final Result<List<Pony>> ponys = command.execute();

        // final List<String> datas = new ArrayList<String>();
        // for (final Pony pony : ponys.getData()) {
        // datas.add(pony.getName());
        // }
        // suggestOracle.addAll(datas);
        // suggestOracle.setDefaultSuggestions(datas.subList(0, 5));

        panel.add(suggestBox);

        panel.add(Element.newPHTML("<br><br>"));

        panel.add(Element.newPLabel("Manipulate the suggest box:"));
        final PListBox operation = Element.newPListBox(true);
        operation.addItem("Select \"Friesian horse\"", 0);
        operation.addItem("Get textbox value", 1);
        operation.addItem("Enable/Disable textbox", 2);
        operation.addItem("Clear", 3);
        operation.addItem("Add items", 4);
        operation.addChangeHandler(event -> {
            final Integer item = (Integer) operation.getSelectedValue();
            if (item == null) return;

            if (item.equals(0)) {
                suggestBox.setText("Friesian horse");
            } else if (item.equals(1)) {
                UIContext.getRootEventBus().fireEvent(new DemoBusinessEvent("Text content: " + suggestBox.getText()));
            } else if (item.equals(2)) {
                suggestBox.getTextBox().setEnabled(!suggestBox.getTextBox().isEnabled());
            } else if (item.equals(3)) {
                final PMultiWordSuggestOracle oracle = (PMultiWordSuggestOracle) suggestBox.getSuggestOracle();
                oracle.clear();
            } else if (item.equals(4)) {
                current++;
                // final Result<List<Pony>> ponys = command.execute();
                // for (final Pony pony : ponys.getData()) {
                // suggestOracle.add(pony.getName() + " " + current);
                // }
            }
        });
        panel.add(operation);

        examplePanel.setWidget(panel);
    }
}
