/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.ui.server.rich;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PConfirmDialogHandler;
import com.ponysdk.ui.server.basic.PDialogBox;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPositionCallback;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;

public class PConfirmDialog extends PDialogBox {

    public static final String DEFAULT_CAPTION = "Confirm";
    public static final String DEFAULT_MESSAGE = "Are You sure?";
    public static final String DEFAULT_OK_CAPTION = "Ok";
    public static final String DEFAULT_CANCEL_CAPTION = "Cancel";

    private PButton okButton;
    private PButton cancelButton;

    public static PDialogBox show(final String windowCaption, final String message, final String okCaption, final String cancelCaption, final PConfirmDialogHandler confirmDialogHandler) {
        return show(windowCaption, new PLabel(message), okCaption, cancelCaption, confirmDialogHandler);
    }

    public static PDialogBox show(final String windowCaption, final PWidget content, final String okCaption, final String cancelCaption, final PConfirmDialogHandler confirmDialogHandler) {
        final PConfirmDialog confirmDialog = buildPopup(windowCaption, content, okCaption, cancelCaption, confirmDialogHandler);
        confirmDialog.setPopupPositionAndShow(new PPositionCallback() {

            @Override
            public void setPosition(final int offsetWidth, final int offsetHeight, final int windowWidth, final int windowHeight) {
                if (offsetHeight > windowHeight) {
                    content.setHeight((windowHeight - 100) + "px");
                    confirmDialog.setHeight((windowHeight - 100) + "px");
                }
                confirmDialog.setWidth((offsetWidth + 35) + "px");
                content.setWidth("100%");
                confirmDialog.center();
            }
        });
        return confirmDialog;

    }

    public static PConfirmDialog buildPopup(final String windowCaption, final PWidget content, final String okCaption, final String cancelCaption, final PConfirmDialogHandler confirmDialogHandler) {
        final PConfirmDialog confirmDialog = new PConfirmDialog();
        confirmDialog.setStyleName(PonySDKTheme.CONFIRM_DIALOGBOX);
        confirmDialog.setAnimationEnabled(true);
        confirmDialog.setGlassEnabled(true);
        final PVerticalPanel dialogContent = new PVerticalPanel();
        dialogContent.setWidth("100%");
        dialogContent.add(content);
        final PHorizontalPanel controlsPanel = new PHorizontalPanel();
        controlsPanel.setStyleName(PonySDKTheme.CONFIRM_DIALOGBOX_CONTROLS);
        controlsPanel.setHorizontalAlignment(PHorizontalAlignment.ALIGN_CENTER);
        controlsPanel.setWidth("100%");

        if (cancelCaption != null) {
            final PButton cancelButton = new PButton();
            cancelButton.setText(cancelCaption != null ? cancelCaption : DEFAULT_CANCEL_CAPTION);
            cancelButton.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent clickEvent) {
                    if (confirmDialogHandler != null) {
                        confirmDialogHandler.onCancel();
                    }
                    confirmDialog.hide();
                }
            });
            controlsPanel.add(cancelButton);
            confirmDialog.setCancelButton(cancelButton);
        }
        if (okCaption != null) {
            final PButton okButton = new PButton();
            okButton.setText(okCaption != null ? okCaption : DEFAULT_OK_CAPTION);
            okButton.addClickHandler(new PClickHandler() {

                @Override
                public void onClick(final PClickEvent clickEvent) {
                    if (confirmDialogHandler != null) {
                        if (confirmDialogHandler.onOK(confirmDialog)) confirmDialog.hide();
                    } else confirmDialog.hide();
                }
            });

            controlsPanel.add(okButton);
            confirmDialog.setOkButton(okButton);
        }
        dialogContent.add(controlsPanel);
        dialogContent.setCellHorizontalAlignment(controlsPanel, PHorizontalAlignment.ALIGN_CENTER);
        dialogContent.setCellHorizontalAlignment(content, PHorizontalAlignment.ALIGN_CENTER);
        confirmDialog.setCaption(windowCaption);
        confirmDialog.setWidget(dialogContent);
        return confirmDialog;
    }

    // show a popup which have a ok button hiding the popup by default
    public static PDialogBox show(final String windowCaption, final PWidget content) {
        return show(windowCaption, content, "Ok", null, null);
    }

    public static PDialogBox show(final String windowCaption, final PWidget content, final PConfirmDialogHandler confirmDialogHandler) {
        return show(windowCaption, content, "Ok", null, confirmDialogHandler);
    }

    @Override
    public void ensureDebugId(final String debugID) {
        super.ensureDebugId(debugID);
        okButton.ensureDebugId(debugID + "[ok]");
        cancelButton.ensureDebugId(debugID + "[cancel]");
    }

    protected void setOkButton(final PButton okButton) {
        this.okButton = okButton;
    }

    protected void setCancelButton(final PButton cancelButton) {
        this.cancelButton = cancelButton;
    }
}
