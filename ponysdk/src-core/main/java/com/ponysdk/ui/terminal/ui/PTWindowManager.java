
package com.ponysdk.ui.terminal.ui;

import java.util.HashMap;
import java.util.Map;

import org.timepedia.exporter.client.Export;

import com.google.gwt.core.client.GWT;

public class PTWindowManager {

    private static PTWindowManager instance = new PTWindowManager();

    private final Map<Integer, PTWindow> windowById = new HashMap<>();

    private PTWindowManager() {}

    public static PTWindowManager get() {
        return instance;
    }

    public void register(final PTWindow window) {
        GWT.log("Register window : " + window.getObjectID());

        windowById.put(window.getObjectID(), window);
    }

    public void unregister(final PTWindow window) {
        windowById.remove(window.getObjectID());
    }

    @Export("getWindow")
    public static PTWindow getWindow(final int windowID) {
        return get().windowById.get(windowID);
    }
}
