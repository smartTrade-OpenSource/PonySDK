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

import com.ponysdk.core.query.Query;
import com.ponysdk.sample.client.datamodel.Pony;
import com.ponysdk.sample.command.pony.FindPonysCommand;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;

public class FlowPanelPageActivity extends SamplePageActivity {

    public FlowPanelPageActivity() {
        super("Flow Panel", "Panels");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        PVerticalPanel panel = new PVerticalPanel();

        final PFlowPanel flowPanel = new PFlowPanel();

        FindPonysCommand command = new FindPonysCommand(new Query());
        List<Pony> ponyList = command.execute().getData();

        for (Pony pony : ponyList) {
            flowPanel.add(new PCheckBox(pony.getName()));
        }

        panel.add(flowPanel);

        examplePanel.setWidget(panel);
    }

}
