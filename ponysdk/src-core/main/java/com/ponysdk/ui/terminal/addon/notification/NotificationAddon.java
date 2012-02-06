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

package com.ponysdk.ui.terminal.addon.notification;

import com.ponysdk.ui.terminal.Addon;
import com.ponysdk.ui.terminal.PonyAddOn;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.AddHandler;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.Remove;
import com.ponysdk.ui.terminal.instruction.Update;
import com.ponysdk.ui.terminal.ui.PTWidget;

@PonyAddOn
public class NotificationAddon extends PTWidget implements Addon {

    public static final String SIGNATURE = "com.ponysdk.ui.terminal.addon.notification.NotificationAddon";

    @Override
    public void create(Create create, UIService uiService) {
        final String caption = create.getMainProperty().getStringProperty(PropertyKey.NOTIFICATION_CAPTION);
        final String message = create.getMainProperty().getStringProperty(PropertyKey.NOTIFICATION_MESSAGE);
        final Notification notification = new Notification(caption, message);
        notification.show();

        init(notification);
    }

    @Override
    public void update(Update update, UIService uiService) {
        // nothing
    }

    @Override
    public void add(Add add, UIService uiService) {
        // nothing
    }

    @Override
    public void remove(Remove remove, UIService uiService) {
        // nothing
    }

    @Override
    public void addHandler(AddHandler addHandler, UIService uiService) {
        // nothing
    }

    @Override
    public String getSignature() {
        return SIGNATURE;
    }

    @Override
    public PTWidget asPTWidget() {
        return this;
    }

}
