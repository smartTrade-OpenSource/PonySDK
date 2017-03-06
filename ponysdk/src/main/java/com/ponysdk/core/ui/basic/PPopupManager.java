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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.server.application.UIContext;

import java.util.HashMap;
import java.util.Map;

public class PPopupManager {

    private static final String SCRIPT_KEY = PPopupManager.class.getCanonicalName();

    private final Map<Integer, PPopupPanel> popups = new HashMap<>();

    private final PWindow window;

    private PPopupManager(final PWindow window) {
        this.window = window;
    }

    public static PPopupManager get(final PWindow window) {
        final UIContext uiContext = UIContext.get();
        final PPopupManager popupManager = uiContext.getAttribute(SCRIPT_KEY + window.getID());
        if (popupManager == null) {
            final PPopupManager newPopupManager = new PPopupManager(window);
            uiContext.setAttribute(SCRIPT_KEY + window, newPopupManager);
            if (window.isOpened()) {
                for (final PPopupPanel popup : newPopupManager.popups.values()) {
                    popup.attach(window);
                }
            } else {
                window.addOpenHandler(e -> newPopupManager.popups.forEach((key, value) -> value.attach(window)));
                window.addCloseHandler(e -> newPopupManager.popups.forEach((key, value) -> {
                    newPopupManager.popups.clear();
                    uiContext.removeAttribute(SCRIPT_KEY + window);
                }));
            }
            return newPopupManager;
        }
        return popupManager;
    }

    void registerPopup(final PPopupPanel popup) {
        popups.put(popup.getID(), popup);
        popup.attach(window);
        popup.addCloseHandler((event) -> unregisterPopup(popup));
    }

    void unregisterPopup(final PPopupPanel popup) {
        popups.remove(popup.getID());
    }

}
