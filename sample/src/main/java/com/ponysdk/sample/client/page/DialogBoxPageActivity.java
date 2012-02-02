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

import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.PageActivity;
import com.ponysdk.sample.client.event.DemoBusinessEvent;
import com.ponysdk.ui.server.addon.PDialogBox;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PImage;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.POptionPane;
import com.ponysdk.ui.server.basic.POptionPane.PActionHandler;
import com.ponysdk.ui.server.basic.PPopupPanel;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PCloseHandler;

public class DialogBoxPageActivity extends PageActivity {

    public DialogBoxPageActivity() {
        super("Dialog Box", "Popup");
    }

    @Override
    protected void onInitialization() {}

    @Override
    protected void onShowPage(final Place place) {}

    @Override
    protected void onLeavingPage() {}

    @Override
    protected void onFirstShowPage() {
        final PVerticalPanel verticalPanel = new PVerticalPanel();
        final PAnchor anchor = new PAnchor("Closable Dialog");
        anchor.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final PDialogBox dialogBox = new PDialogBox(true);
                dialogBox.setText("Dialog");
                final PSimplePanel content = new PSimplePanel();
                content.setWidget(new PLabel("Dialog Box"));
                dialogBox.setWidget(content);
                content.setWidth("500px");
                content.setHeight("300px");
                dialogBox.center();
                dialogBox.show();
            }
        });
        verticalPanel.add(anchor);

        final PAnchor anchor2 = new PAnchor("Popup top left");
        anchor2.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {

                final PPopupPanel popupPanel = new PPopupPanel();
                final PVerticalPanel content = new PVerticalPanel();
                final PButton closeButton = new PButton("Close");
                closeButton.addClickHandler(new PClickHandler() {

                    @Override
                    public void onClick(final PClickEvent clickEvent) {
                        popupPanel.hide();
                    }
                });
                content.add(new PLabel("A popup displayed on the top left"));
                content.add(closeButton);
                content.setWidth("200px");
                content.setHeight("200px");
                popupPanel.setWidget(content);
                popupPanel.setPopupPosition(50, 50);
                popupPanel.show();
            }
        });
        verticalPanel.add(anchor2);

        final PAnchor anchor3 = new PAnchor("Custom close dialog widget");
        anchor3.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final PDialogBox dialogBox = new PDialogBox(true);
                dialogBox.setText("Custom close");
                dialogBox.setCloseWidget(new PImage("image/cross.png"));
                final PSimplePanel content = new PSimplePanel();
                content.setWidget(new PLabel("Dialog Box with custom close widget"));
                dialogBox.setWidget(content);
                content.setWidth("200px");
                content.setHeight("200px");
                dialogBox.center();
                dialogBox.show();

            }
        });

        verticalPanel.add(anchor3);

        final PAnchor anchor4 = new PAnchor("POptionPane showConfirmDialog");
        anchor4.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                POptionPane.showConfirmDialog(new PActionHandler() {

                    @Override
                    public void onAction(final PDialogBox dialogBox, final String option) {
                        dialogBox.hide();
                    }
                }, "showConfirmDialog");

            }
        });
        verticalPanel.add(anchor4);

        final PAnchor anchor5 = new PAnchor("POptionPane with close handler");
        anchor5.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                POptionPane dialodBox = POptionPane.showConfirmDialog(new PActionHandler() {

                    @Override
                    public void onAction(final PDialogBox dialogBox, final String option) {
                        dialogBox.hide();
                    }
                }, "showConfirmDialog");

                dialodBox.getDialogBox().addCloseHandler(new PCloseHandler() {

                    @Override
                    public void onClose() {
                        fireEvent(new DemoBusinessEvent(this));
                    }
                });
            }
        });
        verticalPanel.add(anchor5);

        pageView.getBody().setWidget(verticalPanel);
    }
}
