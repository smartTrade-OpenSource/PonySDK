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

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.IsPWidget;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PPopupPanel;
import com.ponysdk.ui.server.basic.PPositionCallback;
import com.ponysdk.ui.server.basic.PSimplePanel;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class PNotificationManager {

    public enum Notification {
        TRAY, HUMANIZED, WARNING_MESSAGE, ERROR_MESSAGE;
    }

    public static void notify(final String message, final Notification notification) {
        switch (notification) {
            case TRAY:
                showTrayNotification(new PLabel(message), 2500);
                break;
            case HUMANIZED:
                showHumanizedNotification(new PLabel(message));
                break;
            case WARNING_MESSAGE:
                showWarningNotification(new PLabel(message));
                break;
            case ERROR_MESSAGE:
                showErrorNotification(new PLabel(message));
                break;
            default:
                break;
        }
    }

    public static void notify(final IsPWidget content, final Notification notification) {
        switch (notification) {
            case TRAY:
                showTrayNotification(content, 2500);
                break;
            case HUMANIZED:
                showHumanizedNotification(content);
                break;
            case WARNING_MESSAGE:
                showWarningNotification(content);
                break;
            case ERROR_MESSAGE:
                showErrorNotification(content);
                break;
            default:
                break;
        }
    }

    public static void showTrayNotification(final String message) {
        showTrayNotification(new PLabel(message), 2500);
    }

    private static void showHumanizedNotification(final IsPWidget content) {
        final PPopupPanel popupPanel = new PPopupPanel(true);
        popupPanel.setStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_HUMANIZED);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_CLOSING_ANIMATION);
        popupPanel.setWidget(content);

        displayAtCenter(popupPanel);
    }

    private static void showWarningNotification(final IsPWidget content) {
        final PPopupPanel popupPanel = new PPopupPanel(true);
        popupPanel.setStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_WARNING);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_CLOSING_ANIMATION);
        popupPanel.setWidget(content);

        displayAtCenter(popupPanel);
    }

    private static void showErrorNotification(final IsPWidget content) {
        final PPopupPanel popupPanel = new PPopupPanel(false);
        popupPanel.setGlassEnabled(true);
        popupPanel.setStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_ERROR);
        popupPanel.setWidget(content);
        popupPanel.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                popupPanel.hide();
            }
        }, PClickEvent.TYPE);

        displayAtCenter(popupPanel);
    }

    private static void showTrayNotification(final IsPWidget content, final int delayBeforeClosing) {
        final PSimplePanel div2 = new PSimplePanel();
        div2.setWidget(content);
        div2.setWidth("200px");
        div2.setHeight("70px");

        final PPopupPanel popupPanel = new PPopupPanel(true);
        popupPanel.setAnimationEnabled(true);
        popupPanel.setStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_TRAY);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_CLOSING_ANIMATION);
        popupPanel.setWidget(div2);

        displayAtBottomRight(popupPanel);
    }

    private static void displayAtBottomRight(final PPopupPanel popupPanel) {
        popupPanel.setPopupPositionAndShow(new PPositionCallback() {

            @Override
            public void setPosition(final int offsetWidth, final int offsetHeight, final int windowWidth, final int windowHeight) {
                popupPanel.setPopupPosition(windowWidth - offsetWidth - 5, windowHeight - offsetHeight - 5);
            }
        });
    }

    private static void displayAtCenter(final PPopupPanel popupPanel) {
        popupPanel.setPopupPositionAndShow(new PPositionCallback() {

            @Override
            public void setPosition(final int offsetWidth, final int offsetHeight, final int windowWidth, final int windowHeight) {
                popupPanel.setPopupPosition((windowWidth - offsetWidth) / 2, (windowHeight - offsetHeight) / 2);
            }
        });
    }

}
