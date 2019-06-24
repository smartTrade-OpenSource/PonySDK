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

package com.ponysdk.core.ui.rich;

import com.ponysdk.core.server.context.api.UIContext;
import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;

import java.time.Duration;

public class PNotificationManager {

    public static final int humanizedDuration = 5000;
    public static final int warningDuration = 6000;
    public static final int trayDuration = 6000;

    public static void notify(final String message, final Notification notification) {
        notify(PWindow.getMain(), message, notification);
    }

    public static void notify(final PWindow window, final String message, final Notification notification) {
        switch (notification) {
            case TRAY:
                showTrayNotification(window, Element.newPLabel(message));
                break;
            case HUMANIZED:
                showHumanizedNotification(window, Element.newPLabel(message));
                break;
            case WARNING_MESSAGE:
                showWarningNotification(window, Element.newPLabel(message));
                break;
            case ERROR_MESSAGE:
                showErrorNotification(window, Element.newPLabel(message));
                break;
            default:
                break;
        }
    }

    public static void notify(final PWindow window, final PWidget w, final Notification notification) {
        switch (notification) {
            case TRAY:
                showTrayNotification(window, w);
                break;
            case HUMANIZED:
                showHumanizedNotification(window, w);
                break;
            case WARNING_MESSAGE:
                showWarningNotification(window, w);
                break;
            case ERROR_MESSAGE:
                showErrorNotification(window, w);
                break;
            default:
                break;
        }
    }

    public static void showHumanizedNotification(final PWindow window, final String content) {
        showHumanizedNotification(window, Element.newPLabel(content));
    }

    public static void showHumanizedNotification(final PWindow window, final IsPWidget content) {
        final PPopupPanel popupPanel = Element.newPPopupPanel(true);
        popupPanel.addStyleName("pony-notification");
        popupPanel.addStyleName("humanized");
        popupPanel.addStyleName("closing");
        popupPanel.setWidget(content);
        popupPanel.addDomHandler((PClickHandler) event -> popupPanel.close(), PClickEvent.TYPE);
        popupPanel.center();

        window.add(popupPanel);

        addAutoCloseTimer(popupPanel, humanizedDuration);
    }

    public static void showWarningNotification(final PWindow window, final String content) {
        showWarningNotification(window, Element.newPLabel(content));
    }

    public static void showWarningNotification(final PWindow window, final IsPWidget content) {
        final PPopupPanel popupPanel = Element.newPPopupPanel(true);
        popupPanel.addStyleName("pony-notification");
        popupPanel.addStyleName("warning");
        popupPanel.setWidget(content);
        popupPanel.addDomHandler((PClickHandler) event -> popupPanel.close(), PClickEvent.TYPE);
        popupPanel.center();

        window.add(popupPanel);

        addAutoCloseTimer(popupPanel, warningDuration);
    }

    public static void showErrorNotification(final PWindow window, final String content) {
        showErrorNotification(window, Element.newPLabel(content));
    }

    public static void showErrorNotification(final PWindow window, final IsPWidget content) {
        final PPopupPanel popupPanel = Element.newPPopupPanel(false);
        popupPanel.setGlassEnabled(false);
        popupPanel.addStyleName("pony-notification");
        popupPanel.addStyleName("error");
        popupPanel.setWidget(content);
        popupPanel.addDomHandler((PClickHandler) event -> popupPanel.close(), PClickEvent.TYPE);
        popupPanel.center();

        window.add(popupPanel);
    }

    public static void showTrayNotification(final PWindow window, final String content) {
        showTrayNotification(window, Element.newPLabel(content));
    }

    public static void showTrayNotification(final PWindow window, final IsPWidget content) {
        final PPopupPanel popupPanel = Element.newPPopupPanel(true);
        popupPanel.addStyleName("pony-notification");
        popupPanel.addStyleName("tray");

        final PSimplePanel div2 = Element.newPSimplePanel();
        div2.setWidget(content);
        popupPanel.setWidget(div2);

        window.add(popupPanel);

        displayAtBottomRight(popupPanel, "closing");
        addAutoCloseTimer(popupPanel, trayDuration);
    }

    private static void displayAtBottomRight(final PPopupPanel popupPanel, final String closingAnimation) {
        popupPanel.setPopupPositionAndShow((offsetWidth, offsetHeight, windowWidth, windowHeight) -> {
            popupPanel.setPopupPosition(windowWidth - offsetWidth - 5, windowHeight - offsetHeight - 5);
            if (closingAnimation != null) popupPanel.addStyleName(closingAnimation);
        });
    }

    private static void addAutoCloseTimer(final PPopupPanel popupPanel, final int delayBeforeClosing) {
        UIContext.get().schedule(popupPanel::close, Duration.ofMillis(delayBeforeClosing));
    }

    public enum Notification {
        TRAY,
        HUMANIZED,
        WARNING_MESSAGE,
        ERROR_MESSAGE
    }
}
