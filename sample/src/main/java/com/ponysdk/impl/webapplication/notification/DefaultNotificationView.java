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

import java.util.Date;

import com.ponysdk.core.ui.eventbus.BusinessEvent;
import com.ponysdk.core.ui.rich.PNotificationManager;
import com.ponysdk.core.ui.rich.PNotificationManager.Notification;
import com.ponysdk.impl.webapplication.notification.renderer.NotificationTypeRenderer;
import com.ponysdk.impl.webapplication.notification.renderer.PLabelRenderer;

public class DefaultNotificationView extends LogConsolePanel implements NotificationView {

    private NotificationTypeRenderer<String> logTypeRenderer = new PLabelRenderer();
    private NotificationTypeRenderer<String> notificationTypeRenderer = new PLabelRenderer();

    public DefaultNotificationView() {
        super("Logs");
        setSizeFull();
    }

    @Override
    public void addEvent(final BusinessEvent<?> event) {
        addEventLog(event);
        addEventNotification(event);
    }

    public void addEventLog(final BusinessEvent<?> event) {
        final String time = "[" + dateFormat.format(new Date()) + "]";
        final String message = time + " [" + event.getLevel().name() + "] " + event.getBusinessMessage();
        logsPanel.insert(logTypeRenderer.getWidget(message), 0);
    }

    public void addEventNotification(final BusinessEvent<?> event) {
        switch (event.getLevel()) {
            case INFO:
                PNotificationManager.notify(window, notificationTypeRenderer.getWidget(event.getBusinessMessage()),
                        Notification.HUMANIZED);
                break;
            case WARNING:
                PNotificationManager.notify(window, notificationTypeRenderer.getWidget(event.getBusinessMessage()),
                        Notification.WARNING_MESSAGE);
                break;
            case ERROR:
                PNotificationManager.notify(window, notificationTypeRenderer.getWidget(event.getBusinessMessage()),
                        Notification.ERROR_MESSAGE);
                break;
            default:
                break;
        }
    }

    public void addMessageLog(final String msg) {
        final String time = "[" + dateFormat.format(new Date()) + "]";
        logsPanel.insert(notificationTypeRenderer.getWidget(time + msg), 0);
    }

    public void setLogTypeRenderer(final NotificationTypeRenderer<String> logTypeRenderer) {
        this.logTypeRenderer = logTypeRenderer;
    }

    public void setNotificationTypeRenderer(final NotificationTypeRenderer<String> notificationTypeRenderer) {
        this.notificationTypeRenderer = notificationTypeRenderer;
    }

}