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
import com.ponysdk.core.ui.basic.PDecoratorPanel;
import com.ponysdk.core.ui.basic.PFlexTable;

public class DecoratorPanelPageActivity extends SamplePageActivity {

    public DecoratorPanelPageActivity() {
        super("Decorator Panel", "Panels");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PDecoratorPanel decoratorPanel = Element.newPDecoratorPanel();

        final PFlexTable panel = Element.newPFlexTable();

        panel.setStyleProperty("padding", "10px");

        panel.setWidget(0, 0, Element.newPLabel("Name :"));
        panel.setWidget(0, 1, Element.newPTextBox("name"));
        panel.setWidget(1, 0, Element.newPLabel("Description :"));
        panel.setWidget(1, 1, Element.newPTextBox("description"));

        decoratorPanel.setWidget(panel);

        examplePanel.setWidget(decoratorPanel);
    }

}
