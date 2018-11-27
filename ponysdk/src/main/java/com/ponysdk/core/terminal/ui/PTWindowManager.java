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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;

import elemental.util.ArrayOf;
import elemental.util.Collections;
import elemental.util.MapFromIntTo;

public class PTWindowManager {

    private static final int IS_ALIVE_WINDOWS_TIMER = 10000; // 10 seconds

    private static final Logger log = Logger.getLogger(PTWindowManager.class.getName());

    private static final PTWindowManager instance = new PTWindowManager();

    private int mainWindowId;
    private final MapFromIntTo<PTWindow> windows = Collections.mapFromIntTo();

    private PTWindowManager() {
        checkWindowsAlive();
    }

    public static PTWindowManager get() {
        return instance;
    }

    public static int getMainWindowId() {
        return get().mainWindowId;
    }

    public static ArrayOf<PTWindow> getWindows() {
        return get().windows.values();
    }

    public static PTWindow getWindow(final int windowID) {
        return get().windows.get(windowID);
    }

    public static void closeAll() {
        final ArrayOf<PTWindow> windows = get().windows.values();
        for (int i = windows.length() - 1; i >= 0; i--) {
            final PTWindow window = windows.get(i);
            if (window != null) {
                try {
                    window.close(true);
                } catch (final Exception e) {
                    log.log(Level.WARNING, "Can't close window : " + window.getObjectID());
                }
            }
        }
    }

    public void setMainWindowId(final int mainWindowId) {
        this.mainWindowId = mainWindowId;
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
                final ArrayOf<PTWindow> windows = get().windows.values();
                for (int i = windows.length() - 1; i >= 0; i--) {
                    final PTWindow window = windows.get(i);
                    if (window != null && window.isClosed()) window.onClose();
                }
            } catch (final Throwable t) {
                log.log(Level.SEVERE, "Can't checking windows status", t);
            }
            return true;
        }, IS_ALIVE_WINDOWS_TIMER);
    }

}
