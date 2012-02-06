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

import com.google.gwt.dom.client.Style.Unit;
import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.ui.server.addon.PDisclosurePanel;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PImage;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PStackLayoutPanel;
import com.ponysdk.ui.server.basic.PTextBox;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class StackLayoutPanelPageActivity extends PageActivity {

    private PVerticalPanel verticalPanel;

    private final PVerticalPanel header1Child = new PVerticalPanel();;

    private final PVerticalPanel header2Child = new PVerticalPanel();;

    public StackLayoutPanelPageActivity() {
        super("Stack Layout Panel", "Basics UI Components");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onShowPage(Place place) {
        pageView.getBody().setWidget(verticalPanel);
    }

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onFirstShowPage() {

        verticalPanel = new PVerticalPanel();

        // stack layout
        verticalPanel.add(new PLabel("StackLayout: "));
        final PStackLayoutPanel stackLayoutPanel = new PStackLayoutPanel(Unit.PX);
        stackLayoutPanel.setWidth("200px");
        stackLayoutPanel.setHeight("400px");
        stackLayoutPanel.add(getHeader1Child(), "Header 1", true, 30);
        stackLayoutPanel.add(getHeader2Child(), "Header 2", true, 30);
        verticalPanel.add(stackLayoutPanel);

        // disclosure panel
        verticalPanel.add(new PLabel("Disclosure: "));
        final PDisclosurePanel disclosurePanel = new PDisclosurePanel("View details", new PImage("images/treeRightTriangleBlack.png"), new PImage("images/treeDownTriangleBlack.png"));
        disclosurePanel.setContent(getDisclosurePanelContent());
        verticalPanel.add(disclosurePanel);

        final PButton button = new PButton("add item");
        button.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(PClickEvent clickEvent) {
                header1Child.add(new PAnchor("Element at " + System.currentTimeMillis()));
            }
        });

        verticalPanel.add(button);

        pageView.getBody().setWidget(verticalPanel);
    }

    private PWidget getHeader1Child() {
        header1Child.setWidth("100%");
        header1Child.add(new PAnchor("Element 1.1"));
        header1Child.add(new PAnchor("Element 1.2"));
        header1Child.add(new PAnchor("Element 1.3"));
        header1Child.add(new PAnchor("Element 1.4"));
        final PSimplePanel container = new PSimplePanel();
        container.setWidget(header1Child);
        return container;
    }

    private PSimplePanel getHeader2Child() {
        header2Child.setWidth("100%");
        header2Child.add(new PCheckBox("checkbox 1"));
        header2Child.add(new PCheckBox("checkbox 2"));
        header2Child.add(new PCheckBox("checkbox 3"));
        final PSimplePanel container = new PSimplePanel();
        container.setWidget(header2Child);
        return container;
    }

    private PWidget getDisclosurePanelContent() {
        final PVerticalPanel verticalPanel = new PVerticalPanel();
        verticalPanel.add(new PLabel("First Name: "));
        verticalPanel.add(new PTextBox());
        verticalPanel.add(new PLabel("Last Name: "));
        verticalPanel.add(new PTextBox());
        return verticalPanel;
    }

}
