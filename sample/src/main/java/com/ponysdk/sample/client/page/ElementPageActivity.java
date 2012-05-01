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

import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PNotificationManager;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class ElementPageActivity extends SamplePageActivity {

    public ElementPageActivity() {
        super("Element", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel verticalPanel = new PVerticalPanel();
        verticalPanel.setSpacing(10);

        final PAnchor anchor = new PAnchor("And a link");
        anchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                PNotificationManager.showTrayNotification("Link clicked");
            }
        });

        final PElement ul = new PElement("ul");
        final PElement li1 = new PElement("li");
        final PElement li2 = new PElement("li");
        final PElement li3 = new PElement("li");
        final PElement li4 = new PElement("li");
        li1.setInnerText("Item 1");
        li2.setInnerText("Item 2");
        li3.setInnerText("Item 3");
        li3.setInnerText("Item 3");
        li4.add(anchor);
        ul.add(li1);
        ul.add(li2);
        ul.add(li3);
        ul.add(li4);
        ul.addStyleName("customList");

        verticalPanel.add(new PLabel("Example of the use of PElement to create an unordered list"));
        verticalPanel.add(ul);

        examplePanel.setWidget(verticalPanel);
    }
}
