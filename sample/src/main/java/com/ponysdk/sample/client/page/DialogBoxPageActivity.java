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
import com.ponysdk.core.ui.basic.PDialogBox;
import com.ponysdk.core.ui.basic.PFlexTable;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PPopupPanel;
import com.ponysdk.core.ui.basic.PVerticalPanel;
import com.ponysdk.core.ui.basic.event.PCloseEvent;
import com.ponysdk.core.ui.basic.event.PCloseHandler;
import com.ponysdk.core.ui.rich.PClosableDialogBox;
import com.ponysdk.core.ui.rich.PConfirmDialog;
import com.ponysdk.core.ui.rich.POptionPane;
import com.ponysdk.core.ui.rich.POptionPane.PActionHandler;
import com.ponysdk.core.ui.rich.POptionPane.POptionType;
import com.ponysdk.sample.client.event.DemoBusinessEvent;

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
        layout = Element.newPFlexTable();

        addLabel("Show a basic popup");
        final PButton anchor2 = addButton("Open");
        anchor2.addClickHandler(clickEvent -> {
            final PPopupPanel popupPanel = Element.newPPopupPanel();
            final PVerticalPanel content = Element.newPVerticalPanel();
            final PButton closeButton = Element.newPButton("Close");
            closeButton.addClickHandler(clickEvent1 -> popupPanel.hide());
            content.add(Element.newPLabel("A popup displayed relatively to the mouse click"));
            content.add(closeButton);
            content.setWidth("200px");
            content.setHeight("200px");
            popupPanel.setWidget(content);
            popupPanel.setPopupPosition(clickEvent.getClientX(), clickEvent.getClientY());
            popupPanel.show();
            getView().asWidget().getWindow().add(popupPanel);
        });

        addLabel("A draggable popup");
        final PButton anchor3 = addButton("Open");
        anchor3.addClickHandler(clickEvent -> {
            final PClosableDialogBox dialogBox = new PClosableDialogBox("Custom caption");
            dialogBox.setDraggable(true);
            dialogBox.setContent(Element.newPLabel("Content of a popup"));
            dialogBox.center();
            getView().asWidget().getWindow().add(dialogBox);
        });

        addLabel("A confirm dialog listenening on the close eventbus");
        final PButton anchor4 = addButton("Open");
        anchor4.addClickHandler(clickEvent -> {
            final POptionPane dialodBox = POptionPane.showConfirmDialog(getView().asWidget().getWindow(),
                    (dialogBox, option) -> dialogBox.hide(), "Your custom text");

            dialodBox.getDialogBox().addCloseHandler(new PCloseHandler() {

                @Override
                public void onClose(final PCloseEvent closeEvent) {
                    final DemoBusinessEvent event = new DemoBusinessEvent(this);
                    event.setBusinessMessage("Dialog box closed");
                    fireEvent(event);
                }
            });
        });

        addLabel("A confirm dialog listenening on the close eventbus");
        final PButton anchor5 = addButton("Open");
        anchor5.addClickHandler(clickEvent -> POptionPane.showConfirmDialog(getView().asWidget().getWindow(), new PActionHandler() {

            @Override
            public void onAction(final PDialogBox dialogBox, final String option) {
                dialogBox.hide();
                final DemoBusinessEvent event = new DemoBusinessEvent(this);
                event.setBusinessMessage("Option selected #" + option);
                fireEvent(event);
            }
        }, "Your custom text", "Your title", POptionType.YES_NO_CANCEL_OPTION));

        addLabel("PConfirmDialogBox");
        final PButton anchor6 = addButton("Open");
        anchor6.addClickHandler(clickEvent -> PConfirmDialog.show(getView().asWidget().getWindow(), "Question ?",
                Element.newPLabel("This is a confirm dialog box")));

        examplePanel.setWidget(layout);
    }

    private PLabel addLabel(final String text) {
        final PLabel label = Element.newPLabel(text);
        layout.setWidget(row, 0, label);
        return label;
    }

    private PButton addButton(final String text) {
        final PButton button = Element.newPButton(text);
        layout.setWidget(row++, 1, button);
        return button;
    }

}
