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

import com.ponysdk.sample.client.event.DemoBusinessEvent;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PDialogBox;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPopupPanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PCloseEvent;
import com.ponysdk.ui.server.basic.event.PCloseHandler;
import com.ponysdk.ui.server.rich.PClosableDialogBox;
import com.ponysdk.ui.server.rich.PConfirmDialog;
import com.ponysdk.ui.server.rich.POptionPane;
import com.ponysdk.ui.server.rich.POptionPane.PActionHandler;
import com.ponysdk.ui.server.rich.POptionPane.POptionType;

public class DialogBoxPageActivity extends SamplePageActivity {

    private int row;
    private PFlexTable layout;

    public DialogBoxPageActivity() {
        super("Dialog Box", "Popup");
    }

    @Override
    protected void onFirstShowPage() {

        super.onFirstShowPage();

        row = 0;
        layout = new PFlexTable();

        addLabel("Show a basic popup");
        final PButton anchor2 = addButton("Open");
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
                content.add(new PLabel("A popup displayed relatively to the mouse click"));
                content.add(closeButton);
                content.setWidth("200px");
                content.setHeight("200px");
                popupPanel.setWidget(content);
                popupPanel.setPopupPosition(clickEvent.getClientX(), clickEvent.getClientY());
                popupPanel.show();
            }
        });

        addLabel("A draggable popup");
        final PButton anchor3 = addButton("Open");
        anchor3.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final PClosableDialogBox dialogBox = new PClosableDialogBox("Custom caption");
                dialogBox.setDraggable(true);
                dialogBox.setContent(new PLabel("Content of a popup"));
                dialogBox.center();
            }
        });

        addLabel("A confirm dialog listenening on the close event");
        final PButton anchor4 = addButton("Open");
        anchor4.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                final POptionPane dialodBox = POptionPane.showConfirmDialog(new PActionHandler() {

                    @Override
                    public void onAction(final PDialogBox dialogBox, final String option) {
                        dialogBox.hide();
                    }
                }, "Your custom text");

                dialodBox.getDialogBox().addCloseHandler(new PCloseHandler() {

                    @Override
                    public void onClose(final PCloseEvent closeEvent) {
                        final DemoBusinessEvent event = new DemoBusinessEvent(this);
                        event.setBusinessMessage("Dialog box closed");
                        fireEvent(event);
                    }
                });
            }
        });

        addLabel("A confirm dialog listenening on the close event");
        final PButton anchor5 = addButton("Open");
        anchor5.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                POptionPane.showConfirmDialog(new PActionHandler() {

                    @Override
                    public void onAction(final PDialogBox dialogBox, final String option) {
                        dialogBox.hide();
                        final DemoBusinessEvent event = new DemoBusinessEvent(this);
                        event.setBusinessMessage("Option selected #" + option);
                        fireEvent(event);
                    }
                }, "Your custom text", "Your title", POptionType.YES_NO_CANCEL_OPTION);
            }
        });

        addLabel("PConfirmDialogBox");
        final PButton anchor6 = addButton("Open");
        anchor6.addClickHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent clickEvent) {
                PConfirmDialog.show("Question ?", new PLabel("This is a confirm dialog box"));
            }
        });

        examplePanel.setWidget(layout);
    }

    private PLabel addLabel(final String text) {
        final PLabel label = new PLabel(text);
        layout.setWidget(row, 0, label);
        return label;
    }

    private PButton addButton(final String text) {
        final PButton button = new PButton(text);
        layout.setWidget(row++, 1, button);
        return button;
    }

}
