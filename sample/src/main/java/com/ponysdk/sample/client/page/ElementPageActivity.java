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
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PElement;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.rich.PNotificationManager;

public class ElementPageActivity extends SamplePageActivity {

    public ElementPageActivity() {
        super("Element", "Widgets");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel verticalPanel = Element.newPVerticalPanel();
        verticalPanel.setSpacing(10);

        final PAnchor anchor = Element.newPAnchor("And a link");
        anchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                PNotificationManager.showTrayNotification(getView().asWidget().getWindowID(), "Link clicked");
            }
        });

        final PElement ul = Element.newUl();
        final PElement li1 = Element.newLi();
        final PElement li2 = Element.newLi();
        final PElement li3 = Element.newLi();
        final PElement li4 = Element.newLi();
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

        verticalPanel.add(Element.newPLabel("Example of the use of PElement to create an unordered list"));
        verticalPanel.add(ul);

        examplePanel.setWidget(verticalPanel);
    }
}
