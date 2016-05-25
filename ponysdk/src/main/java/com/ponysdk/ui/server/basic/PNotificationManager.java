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

    public static int humanizedDuration = 5000;
    public static int warningDuration = 6000;
    public static int trayDuration = 6000;

    public enum Notification {
        TRAY, HUMANIZED, WARNING_MESSAGE, ERROR_MESSAGE;
    }

    public static void notify(final String message, final Notification notification) {
        notify(message, notification);
    }

    public static void notify(final int windowID, final String message, final Notification notification) {
        switch (notification) {
            case TRAY:
                showTrayNotification(windowID, new PLabel(message));
                break;
            case HUMANIZED:
                showHumanizedNotification(windowID, new PLabel(message));
                break;
            case WARNING_MESSAGE:
                showWarningNotification(windowID, new PLabel(message));
                break;
            case ERROR_MESSAGE:
                showErrorNotification(windowID, new PLabel(message));
                break;
            default:
                break;
        }
    }

    public static void notify(final int windowID, final IsPWidget content, final Notification notification) {
        switch (notification) {
            case TRAY:
                showTrayNotification(windowID, content);
                break;
            case HUMANIZED:
                showHumanizedNotification(windowID, content);
                break;
            case WARNING_MESSAGE:
                showWarningNotification(windowID, content);
                break;
            case ERROR_MESSAGE:
                showErrorNotification(windowID, content);
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

    public static void showHumanizedNotification(final PWindow windowID, final String message) {
        showHumanizedNotification(windowID.getID(), new PLabel(message));
    }

    public static void showHumanizedNotification(final int windowID, final String message) {
        showHumanizedNotification(windowID, new PLabel(message));
    }

    public static void showWarningNotification(final PWindow window, final String message) {
        showWarningNotification(window.getID(), new PLabel(message));
    }

    public static void showWarningNotification(final int windowID, final String message) {
        showWarningNotification(windowID, new PLabel(message));
    }

    public static void showErrorNotification(final PWindow window, final String message) {
        showErrorNotification(window.getID(), new PLabel(message));
    }

    public static void showErrorNotification(final int windowID, final String message) {
        showErrorNotification(windowID, new PLabel(message));
    }

    public static void showTrayNotification(final PWindow window, final String message) {
        showTrayNotification(window.getID(), new PLabel(message));
    }

    public static void showTrayNotification(final int windowID, final String message) {
        showTrayNotification(windowID, new PLabel(message));
    }

    private static void showHumanizedNotification(final int windowID, final IsPWidget content) {
        final PPopupPanel popupPanel = new PPopupPanel(true);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_HUMANIZED);
        popupPanel.setWidget(content);
        popupPanel.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                popupPanel.hide();
            }
        }, PClickEvent.TYPE);
        popupPanel.addStyleName("closing");
        popupPanel.center();

        addAutoCloseTimer(popupPanel, humanizedDuration);
    }

    private static void showHumanizedNotification(final IsPWidget content) {
        final PPopupPanel popupPanel = new PPopupPanel(true);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_HUMANIZED);
        popupPanel.setWidget(content);
        popupPanel.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                popupPanel.hide();
            }
        }, PClickEvent.TYPE);
        popupPanel.addStyleName("closing");
        popupPanel.center();

        addAutoCloseTimer(popupPanel, humanizedDuration);
    }

    private static void showWarningNotification(final int windowID, final IsPWidget content) {
        final PPopupPanel popupPanel = new PPopupPanel(true);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_WARNING);
        popupPanel.setWidget(content);
        popupPanel.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                popupPanel.hide();
            }
        }, PClickEvent.TYPE);

        popupPanel.center();
        addAutoCloseTimer(popupPanel, warningDuration);
    }

    private static void showWarningNotification(final IsPWidget content) {
        final PPopupPanel popupPanel = new PPopupPanel(true);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_WARNING);
        popupPanel.setWidget(content);
        popupPanel.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                popupPanel.hide();
            }
        }, PClickEvent.TYPE);

        popupPanel.center();
        addAutoCloseTimer(popupPanel, warningDuration);
    }

    private static void showErrorNotification(final int windowID, final IsPWidget content) {
        final PPopupPanel popupPanel = new PPopupPanel(false);
        popupPanel.setGlassEnabled(false);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_ERROR);
        popupPanel.setWidget(content);
        popupPanel.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                popupPanel.hide();
            }
        }, PClickEvent.TYPE);

        popupPanel.center();
    }

    private static void showErrorNotification(final IsPWidget content) {
        final PPopupPanel popupPanel = new PPopupPanel(false);
        popupPanel.setGlassEnabled(false);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_ERROR);
        popupPanel.setWidget(content);
        popupPanel.addDomHandler(new PClickHandler() {

            @Override
            public void onClick(final PClickEvent event) {
                popupPanel.hide();
            }
        }, PClickEvent.TYPE);

        popupPanel.center();
    }

    private static void showTrayNotification(final int windowID, final IsPWidget content) {
        final PSimplePanel div2 = new PSimplePanel();
        div2.setWidget(content);

        final PPopupPanel popupPanel = new PPopupPanel(true);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_TRAY);
        popupPanel.setWidget(div2);
        displayAtBottomRight(popupPanel, "closing");
        addAutoCloseTimer(popupPanel, trayDuration);
    }

    private static void showTrayNotification(final IsPWidget content) {
        final PSimplePanel div2 = new PSimplePanel();
        div2.setWidget(content);

        final PPopupPanel popupPanel = new PPopupPanel(true);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION);
        popupPanel.addStyleName(PonySDKTheme.NOTIFICATION_TRAY);
        popupPanel.setWidget(div2);
        displayAtBottomRight(popupPanel, "closing");
        addAutoCloseTimer(popupPanel, trayDuration);
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

    private static void addAutoCloseTimer(final PPopupPanel popupPanel, final int delayBeforeClosing) {
        final PTerminalScheduledCommand hideCommand = new PTerminalScheduledCommand() {

            @Override
            protected void run() {
                popupPanel.hide();
            }
        };
        hideCommand.schedule(delayBeforeClosing);
    }
}
