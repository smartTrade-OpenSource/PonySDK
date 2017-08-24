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
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PVerticalPanel;

public class CustomButtonPageActivity extends SamplePageActivity {

    protected PButton enabledPushButton;

    protected PButton disabledPushButton;

    protected PButton enabledToggleButton;

    protected PButton disabledToggleButton;

    public CustomButtonPageActivity() {
        super("Custom Button", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = Element.newPVerticalPanel();
        panel.setSpacing(10);

        panel.add(Element.newPLabel("Push button :"));
        panel.add(buildPushButtonPanel());

        examplePanel.setWidget(panel);
    }

    private PHorizontalPanel buildPushButtonPanel() {
        final PHorizontalPanel panel = Element.newPHorizontalPanel();
        panel.setSpacing(10);

        enabledPushButton = Element.newPPushButton(Element.newPImage("images/pony.png"));
        panel.add(enabledPushButton);

        disabledPushButton = Element.newPPushButton(Element.newPImage("images/pony.png"));
        disabledPushButton.setEnabled(false);
        panel.add(disabledPushButton);

        return panel;
    }
}
