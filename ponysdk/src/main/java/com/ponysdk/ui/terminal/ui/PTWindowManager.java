
package com.ponysdk.ui.terminal.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.timepedia.exporter.client.Export;

public class PTWindowManager {

    private static final Logger log = Logger.getLogger(PTWindowManager.class.getName());

    private static PTWindowManager instance = new PTWindowManager();

    private final Map<Integer, PTWindow> windowById = new HashMap<>();

    private PTWindowManager() {
    }

    public static PTWindowManager get() {
        return instance;
    }

    public void register(final PTWindow window) {
        if (log.isLoggable(Level.INFO))
            log.log(Level.INFO, "Register window : " + window.getObjectID());
        windowById.put(window.getObjectID(), window);
    }

    public void unregister(final PTWindow window) {
        windowById.remove(window.getObjectID());
    }

    @Export("getWindow")
    public static PTWindow getWindow(final int windowID) {
        return get().windowById.get(windowID);
    }

    public static final void closeAll() {
        for (final PTWindow window : get().windowById.values()) {
            window.close(true);
        }
    }
}
