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

package com.ponysdk.impl.webapplication.notification;

import com.ponysdk.core.event.PBroadcastEventHandler;
import com.ponysdk.core.event.PBusinessEvent;
import com.ponysdk.core.event.PEvent;
import com.ponysdk.core.place.Place;
import com.ponysdk.impl.webapplication.page.InitializingActivity;
import com.ponysdk.ui.server.basic.IsPWidget;

public class NotificationActivity extends com.ponysdk.core.activity.AbstractActivity implements PBroadcastEventHandler, InitializingActivity {

    private NotificationView notificationView;

    @Override
    public IsPWidget buildView() {
        return notificationView;
    }

    @Override
    public void updateView(final Place place) {}

    @Override
    public void afterContextInitialized() {
        addHandler(this);
    }

    @Override
    public void onEvent(final PEvent<?> event) {
        if (event instanceof PBusinessEvent<?>) {
            final PBusinessEvent<?> businessEvent = (PBusinessEvent<?>) event;
            notificationView.addEvent(businessEvent);
        }
    }

    public void setNotificationView(final NotificationView notificationView) {
        this.notificationView = notificationView;
    }

}
