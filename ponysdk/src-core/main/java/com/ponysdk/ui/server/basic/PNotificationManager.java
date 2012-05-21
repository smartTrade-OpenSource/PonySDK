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

package com.ponysdk.ui.server.basic;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.server.basic.PPopupPanel.PPositionCallback;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;

public class PNotificationManager {

    public enum Notification {
        TRAY, HUMANIZED, WARNING_MESSAGE, ERROR_MESSAGE;
    }

    public static void notify(final String message, final Notification notification) {
        switch (notification) {
            case TRAY:
                showTrayNotification(new PLabel(message));
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
                showTrayNotification(content);
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

    public static void showHumanizedNotification(final String message) {
        showHumanizedNotification(new PLabel(message));
    }

    public static void showWarningNotification(final String message) {
        showWarningNotification(new PLabel(message));
    }

    public static void showErrorNotification(final String message) {
        showErrorNotification(new PLabel(message));
    }

    public static void showTrayNotification(final String message) {
        showTrayNotification(new PLabel(message));
    }

    private static void showHumanizedNotification(final IsPWidget content) {
        final PPopupPanel popupPanel = new PPopupPanel(true);
        popupPanel.setStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_HUMANIZED);
        popupPanel.setWidget(content);
        popupPanel.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                popupPanel.hide();
            }
        }, PClickEvent.TYPE);

        displayAtCenter(popupPanel, "closing");
        addAutoCloseTimer(popupPanel, 2500);
    }

    private static void showWarningNotification(final IsPWidget content) {
        final PPopupPanel popupPanel = new PPopupPanel(true);
        popupPanel.setStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_WARNING);
        popupPanel.setWidget(content);
        popupPanel.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                popupPanel.hide();
            }
        }, PClickEvent.TYPE);

        displayAtCenter(popupPanel, "closing");
        addAutoCloseTimer(popupPanel, 3500);
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

        displayAtCenter(popupPanel, null);
    }

    private static void showTrayNotification(final IsPWidget content) {
        final PSimplePanel div2 = new PSimplePanel();
        div2.setWidget(content);
        div2.setWidth("200px");
        div2.setHeight("70px");

        final PPopupPanel popupPanel = new PPopupPanel(true);
        popupPanel.setStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_TRAY);
        popupPanel.setWidget(div2);

        displayAtBottomRight(popupPanel, "closing");
        addAutoCloseTimer(popupPanel, 3500);
    }

    private static void displayAtBottomRight(final PPopupPanel popupPanel, final String closingAnimation) {
        popupPanel.setPopupPositionAndShow(new PPositionCallback() {

            @Override
            public void setPosition(final int offsetWidth, final int offsetHeight, final int windowWidth, final int windowHeight) {
                popupPanel.setPopupPosition(windowWidth - offsetWidth - 5, windowHeight - offsetHeight - 5);
                if (closingAnimation != null) popupPanel.addStyleName(closingAnimation);
            }
        });
    }

    private static void displayAtCenter(final PPopupPanel popupPanel, final String closingAnimation) {
        popupPanel.setPopupPositionAndShow(new PPositionCallback() {

            @Override
            public void setPosition(final int offsetWidth, final int offsetHeight, final int windowWidth, final int windowHeight) {
                popupPanel.setPopupPosition((windowWidth - offsetWidth) / 2, (windowHeight - offsetHeight) / 2);
                if (closingAnimation != null) popupPanel.addStyleName(closingAnimation);
            }
        });
    }

    private static void addAutoCloseTimer(final PPopupPanel popupPanel, final int delayBeforeClosing) {
        final PTimer timer = new PTimer() {

            @Override
            public void run() {
                popupPanel.hide();
            }
        };
        timer.schedule(delayBeforeClosing);
    }
}
