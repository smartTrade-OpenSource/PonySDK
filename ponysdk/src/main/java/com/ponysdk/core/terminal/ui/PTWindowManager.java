
package com.ponysdk.core.terminal.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.timepedia.exporter.client.Export;

public class PTWindowManager {

    private static final Logger log = Logger.getLogger(PTWindowManager.class.getName());

    private static PTWindowManager instance = new PTWindowManager();

    private final Map<Integer, PTWindow> windows = new HashMap<>();

    private PTWindowManager() {
    }

    public static PTWindowManager get() {
        return instance;
    }

    public void register(final PTWindow window) {
        if (log.isLoggable(Level.INFO))
            log.log(Level.INFO, "Register window : " + window.getObjectID());
        windows.put(window.getObjectID(), window);
    }

    public void unregister(final PTWindow window) {
        windows.remove(window.getObjectID());
    }

    @Export("getWindow")
    public static PTWindow getWindow(final int windowID) {
        return get().windows.get(windowID);
    }

    public static void closeAll() {
        final Collection<PTWindow> values = get().windows.values();
        for (final PTWindow window : values) {
            window.close(true);
        }
    }
}
