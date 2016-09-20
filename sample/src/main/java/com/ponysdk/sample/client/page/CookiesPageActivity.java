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
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;

public class CookiesPageActivity extends SamplePageActivity {

    public CookiesPageActivity() {
        super("Cookies", "Extra");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PTextBox name = new PTextBox();
        name.setPlaceholder("Cookie name");
        final PTextBox value = new PTextBox();
        name.setPlaceholder("Cookie value");
        final PButton add = new PButton("Add");
        add.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                UIContext.get().getCookies().setCookie(name.getValue(), value.getValue());
            }
        });

        final PTextBox name2 = new PTextBox();
        name2.setPlaceholder("Cookie name");
        final PButton remove = new PButton("Remove");
        remove.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                UIContext.get().getCookies().removeCookie(name2.getValue());
            }
        });

        final PHorizontalPanel addPanel = new PHorizontalPanel();
        addPanel.add(name);
        addPanel.add(value);
        addPanel.add(add);

        final PHorizontalPanel removePanel = new PHorizontalPanel();
        removePanel.add(name2);
        removePanel.add(remove);

        final PVerticalPanel panel = new PVerticalPanel();
        panel.setSpacing(10);

        panel.add(new PLabel("Add a cookie:"));
        panel.add(addPanel);
        panel.add(new PLabel("Remove a cookie:"));
        panel.add(removePanel);

        examplePanel.setWidget(panel);
    }
}
