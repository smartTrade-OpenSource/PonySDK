package com.ponysdk.core.server.context.api;

public interface UIContextManager {
    void addListener(UIContextListener listener);

    void removeListener(UIContextListener listener);
}
