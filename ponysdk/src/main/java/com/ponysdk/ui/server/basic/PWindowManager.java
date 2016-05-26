
package com.ponysdk.ui.server.basic;

import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.UIContext;

public class PWindowManager {

    private static final String ROOT = "WindowManager";

    private final Map<Integer, PWindow> windows = new HashMap<>();

    private PWindowManager() {
    }

    public static PWindowManager get() {
        final UIContext session = UIContext.get();
        PWindowManager windowManager = session.getAttribute(ROOT);
        if (windowManager == null) {
            windowManager = new PWindowManager();
            session.setAttribute(ROOT, windowManager);
        }
        return windowManager;
    }

    static void registerWindow(final PWindow window) {
        get().registerWindow0(window);
    }

    static void unregisterWindow(final PWindow window) {
        get().registerWindow0(window);
    }

    public void registerWindow0(final PWindow window) {
        windows.put(window.getID(), window);
    }

    public void unregisterWindow0(final PWindow window) {
        windows.remove(window.getID());
    }

    public PWindow getWindow(final int windowID) {
        return windows.get(windowID);
    }

    public void closeAll() {
        for (final PWindow window : windows.values()) {
            window.close();
        }
    }

}
