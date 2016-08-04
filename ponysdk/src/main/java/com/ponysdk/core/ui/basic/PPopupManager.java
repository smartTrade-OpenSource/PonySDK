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

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.server.application.UIContext;

public class PPopupManager {

    private static final String SCRIPT_KEY = PPopupManager.class.getCanonicalName();

    private final Map<Integer, PPopupPanel> popups = new HashMap<>();

    private final int windowID;

    private PPopupManager(final int windowID) {
        this.windowID = windowID;
    }

    public static PPopupManager get(final int windowID) {
        final UIContext uiContext = UIContext.get();
        final PPopupManager popupManager = uiContext.getAttribute(SCRIPT_KEY + windowID);
        if (popupManager == null) {
            final PPopupManager newPopupManager = new PPopupManager(windowID);
            uiContext.setAttribute(SCRIPT_KEY + windowID, newPopupManager);
            if (PWindowManager.getWindow(windowID) != null) {
                for (final PPopupPanel popup : newPopupManager.popups.values()) {
                    popup.attach(windowID);
                }
            } else {
                PWindowManager.addWindowListener(new PWindowManager.RegisterWindowListener() {

                    @Override
                    public void registered(final int registeredWindowID) {
                        if (registeredWindowID == windowID) {
                            for (final PPopupPanel popup : newPopupManager.popups.values()) {
                                popup.attach(windowID);
                            }
                        }
                    }

                    @Override
                    public void unregistered(final int registeredWindowID) {
                        if (registeredWindowID == windowID) {
                            newPopupManager.popups.clear();
                            uiContext.removeAttribute(SCRIPT_KEY + windowID);
                        }
                    }
                });
            }
            return newPopupManager;
        }
        return popupManager;
    }

    void registerPopup(final PPopupPanel popup) {
        popups.put(popup.getID(), popup);
        if (PWindowManager.getWindow(windowID) != null) {
            popup.attach(windowID);
            popup.addCloseHandler((event) -> unregisterPopup(popup));
        }
    }

    void unregisterPopup(final PPopupPanel popup) {
        popups.remove(popup.getID());
    }

}
