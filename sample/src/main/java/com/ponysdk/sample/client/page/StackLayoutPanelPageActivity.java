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

import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PStackLayoutPanel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PSelectionEvent;
import com.ponysdk.core.ui.basic.event.PSelectionHandler;
import com.ponysdk.sample.client.event.DemoBusinessEvent;

public class StackLayoutPanelPageActivity extends SamplePageActivity {

    private final PVerticalPanel header1Child = Element.newPVerticalPanel();

    private final PVerticalPanel header2Child = Element.newPVerticalPanel();

    public StackLayoutPanelPageActivity() {
        super("Stack Layout Panel", "Lists and Menus");
    }

    @Override
    protected void onFirstShowPage() {
        super.onFirstShowPage();

        final PVerticalPanel panel = Element.newPVerticalPanel();

        panel.add(Element.newPLabel("StackLayout: "));

        final PStackLayoutPanel stackLayoutPanel = Element.newPStackLayoutPanel(PUnit.PX);

        stackLayoutPanel.setStyleProperty("border", "1px solid #CCC");

        stackLayoutPanel.setWidth("200px");
        stackLayoutPanel.setHeight("400px");
        stackLayoutPanel.add(getHeader1Child(), "Header 1", true, 30);
        stackLayoutPanel.add(getHeader2Child(), "Header 2", true, 30);

        stackLayoutPanel.addSelectionHandler(new PSelectionHandler<Integer>() {

            @Override
            public void onSelection(final PSelectionEvent<Integer> event) {
                final String msg = "On selection : " + event.getSelectedItem();
                UIContext.getRootEventBus().fireEvent(new DemoBusinessEvent(msg));
            }
        });

        panel.add(stackLayoutPanel);

        examplePanel.setWidget(panel);
    }

    private PWidget getHeader1Child() {
        header1Child.setWidth("100%");
        header1Child.add(Element.newPAnchor("Element 1.1"));
        header1Child.add(Element.newPAnchor("Element 1.2"));
        header1Child.add(Element.newPAnchor("Element 1.3"));
        header1Child.add(Element.newPAnchor("Element 1.4"));
        final PSimplePanel container = Element.newPSimplePanel();
        container.setWidget(header1Child);
        return container;
    }

    private PSimplePanel getHeader2Child() {
        header2Child.setWidth("100%");
        header2Child.add(Element.newPCheckBox("checkbox 1"));
        header2Child.add(Element.newPCheckBox("checkbox 2"));
        header2Child.add(Element.newPCheckBox("checkbox 3"));
        final PSimplePanel container = Element.newPSimplePanel();
        container.setWidget(header2Child);
        return container;
    }

}
