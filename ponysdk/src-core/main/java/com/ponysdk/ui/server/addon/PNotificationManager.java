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
package com.ponysdk.ui.server.addon;

import com.ponysdk.core.PonySession;
import com.ponysdk.ui.server.basic.PAddOn;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Create;

public class PNotificationManager implements PAddOn {

    public enum Notification {
        TRAY, HUMANIZED, WARNING_MESSAGE, ERROR_MESSAGE;
    }

    public static void notify(String title, String message, Notification notification) {

        switch (notification) {
        case TRAY:
            notify(title, message);
            break;
        case HUMANIZED:
            notify(title, message, "HUMANIZED");
            break;
        case WARNING_MESSAGE:
            notify(title, message, "WARNING_MESSAGE");
            break;
        case ERROR_MESSAGE:
            notify(title, message, "ERROR_MESSAGE");
            break;
        default:
            break;
        }

    }

    private static void notify(String title, String message, String styleName) {
        final PDialogBox confirmDialog = new PDialogBox();
        confirmDialog.setAnimationEnabled(true);
        confirmDialog.setGlassEnabled(true);
        confirmDialog.setClosable(true);
        confirmDialog.addStyleName(styleName);

        final PVerticalPanel dialogContent = new PVerticalPanel();
        dialogContent.add(new PLabel(message));

        confirmDialog.setText(title);
        confirmDialog.setWidget(dialogContent);
        confirmDialog.show();
        confirmDialog.center();
    }

    // must be implemented server side
    public static void notify(String caption, String content) {
        final PonySession ponySession = PonySession.getCurrent();
        final long ID = ponySession.nextID();
        final Create create = new Create(ID, WidgetType.ADDON);
        create.setAddOnSignature(com.ponysdk.ui.terminal.addon.notification.NotificationAddon.SIGNATURE);
        ponySession.stackInstruction(create);

        // ponySession.addWidget(ID, this);
        create.getMainProperty().setProperty(PropertyKey.NOTIFICATION_CAPTION, caption != null ? caption : "");
        create.getMainProperty().setProperty(PropertyKey.NOTIFICATION_MESSAGE, content != null ? content : "");
    }

    public static void notify(String caption) {
        final PonySession ponySession = PonySession.getCurrent();
        final long ID = ponySession.nextID();
        final Create create = new Create(ID, WidgetType.ADDON);
        create.setAddOnSignature(com.ponysdk.ui.terminal.addon.notification.NotificationAddon.SIGNATURE);
        ponySession.stackInstruction(create);

        // ponySession.addWidget(ID, this);
        create.getMainProperty().setProperty(PropertyKey.NOTIFICATION_CAPTION, caption != null ? caption : "");
        create.getMainProperty().setProperty(PropertyKey.NOTIFICATION_MESSAGE, "");
    }

    @Override
    public String getSignature() {
        return com.ponysdk.ui.terminal.addon.notification.NotificationAddon.SIGNATURE;
    }

}
