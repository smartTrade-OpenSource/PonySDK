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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.core.client.Scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PTWindowManager {

    private static final Logger log = Logger.getLogger(PTWindowManager.class.getName());

    private static final PTWindowManager instance = new PTWindowManager();

    private final Map<Integer, PTWindow> windows = new HashMap<>();

    private PTWindowManager() {
        checkWindowsAlive();
    }

    public static PTWindowManager get() {
        return instance;
    }

    public static Collection<PTWindow> getWindows() {
        return new ArrayList<>(get().windows.values());
    }

    public static PTWindow getWindow(final int windowID) {
        return get().windows.get(windowID);
    }

    public static void closeAll() {
        final Collection<PTWindow> values = new ArrayList<>(get().windows.values());
        for (final PTWindow window : values) {
            window.close(true);
        }
    }

    public void register(final PTWindow window) {
        if (log.isLoggable(Level.INFO)) log.log(Level.INFO, "Register window : " + window.getObjectID());
        windows.put(window.getObjectID(), window);
    }

    void unregister(final PTWindow window) {
        windows.remove(window.getObjectID());
    }

    private void checkWindowsAlive() {
        Scheduler.get().scheduleFixedDelay(() -> {
            try {
                for (final PTWindow window : windows.values()) {
                    if (window.isClosed()) window.onClose();
                }
            } catch (final Throwable t) {
                log.log(Level.SEVERE, "Can't checking windows status", t);
            }
            return true;
        }, 2000);
    }
}
