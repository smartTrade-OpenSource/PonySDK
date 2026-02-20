package com.ponysdk.core.terminal.ui;

import java.util.Objects;

import com.google.gwt.user.client.Window;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;

public class PTHistory {

    private static UIBuilder uiBuilder;
    private static boolean fireEvents = true;

    private PTHistory() {
    }

    public static void addValueChangeHandler(final UIBuilder builder) {
        PTHistory.uiBuilder = builder;
        initHashChange();
    }

    public static void onHashChanged(String hash) {
        if (fireEvents && hash != null && !hash.isEmpty()) {
            final PTInstruction eventInstruction = new PTInstruction();
            eventInstruction.put(ClientToServerModel.TYPE_HISTORY, hash);
            uiBuilder.sendDataToServer(eventInstruction);
        }
    }

    private static native void initHashChange() /*-{
        $wnd.addEventListener('hashchange', function() {
            var hash = $wnd.location.hash;
            if (hash.indexOf('#') === 0) {
                hash = hash.substring(1);
            }
            @com.ponysdk.core.terminal.ui.PTHistory::onHashChanged(Ljava/lang/String;)(hash);
        });
    }-*/;

    public static String getHash() {
        return Window.Location.getHash().substring(1);
    }

    private static native void setToken(String token) /*-{
        $wnd.location.hash = token;
    }-*/;

    public static void setHash(String token, boolean fireEvents) {
        if (Objects.equals(token, getHash())) return;
        try {
            PTHistory.fireEvents = fireEvents;
            setToken(token);
        } finally {
            PTHistory.fireEvents = true;
        }
    }
}