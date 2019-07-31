package com.ponysdk.core.server.context.api;

public interface UIContextListener {
    void onContextCreated(UIContext context);

    void onContextDestroyed(UIContext context);
}
