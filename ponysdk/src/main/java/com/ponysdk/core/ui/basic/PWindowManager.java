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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.server.application.UIContext;

public class PWindowManager {

    private static final String ROOT = "WindowManager";

    private final Map<Integer, PWindow> preregisteredWindows = new HashMap<>();
    private final Map<Integer, PWindow> registeredWindows = new HashMap<>();

    private PWindowManager() {
    }

    public static PWindowManager get() {
        final UIContext uiContext = UIContext.get();
        PWindowManager windowManager = uiContext.getAttribute(ROOT);
        if (windowManager == null) {
            windowManager = new PWindowManager();
            uiContext.setAttribute(ROOT, windowManager);
        }
        return windowManager;
    }

    public static void preregisterWindow(final PWindow pWindow) {
        get().preregisteredWindows.put(pWindow.getID(), pWindow);
    }

    static void registerWindow(final PWindow window) {
        get().registerWindow0(window);
    }

    static void unregisterWindow(final PWindow window) {
        get().unregisterWindow0(window);
    }

    public static void closeAll() {
        get().closeAll0();
    }

    public static final Collection<PWindow> getWindows() {
        return get().registeredWindows.values();
    }

    public static final PWindow getWindow(final int windowID) {
        return get().registeredWindows.get(windowID);
    }

    private void registerWindow0(final PWindow window) {
        registeredWindows.put(window.getID(), window);
    }

    private void unregisterWindow0(final PWindow window) {
        preregisteredWindows.remove(window.getID());
        registeredWindows.remove(window.getID());
    }

    private void closeAll0() {
        registeredWindows.forEach((id, window) -> window.close());
        registeredWindows.clear();
    }

}
