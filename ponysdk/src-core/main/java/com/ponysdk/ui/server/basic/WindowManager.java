
package com.ponysdk.ui.server.basic;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.ponysdk.core.UIContext;
import com.ponysdk.ui.terminal.ui.PTWindow;

public class WindowManager {

    private static final String ROOT = "WindowManager";

    private final Map<Integer, PTWindow> windows = new HashMap<>();

    private WindowManager() {}

    public static WindowManager get() {
        final UIContext session = UIContext.get();
        WindowManager windowManager = session.getAttribute(ROOT);
        if (windowManager == null) {
            windowManager = new WindowManager();
            session.setAttribute(ROOT, windowManager);
        }
        return windowManager;
    }

    static void registerWindow(final PWindow window) {
        get().registerWindow0(window);
    }

    static void unregisterWindow(final PWindow window) {
        get().registerWindow0(window);;
    }

    public void registerWindow0(final PTWindow window) {
        windows.put(window.getID(), window);
    }

    public void unregisterWindow0(final PWindow window) {
        windows.remove(window.getID());
    }

    public PWindow getWindow(final int windowID) {
        return windows.get(windowID);
    }

    public Collection<PWindow> getWindows() {
        return windows.values();
    }

}
