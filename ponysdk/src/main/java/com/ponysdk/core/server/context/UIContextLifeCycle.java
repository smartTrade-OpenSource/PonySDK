package com.ponysdk.core.server.context;

public interface UIContextLifeCycle {

    void onUIContextStarted(UIContext uiContext);

    void onUIContextStopped(UIContext uiContext);

}
