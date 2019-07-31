package com.ponysdk.core.server.context.impl;

import com.ponysdk.core.server.context.api.UIContext;
import com.ponysdk.core.server.context.api.UIContextListener;
import com.ponysdk.core.server.context.api.UIContextManager;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UIContextManagerImpl implements UIContextManager {
    private Map<Integer, UIContext> contexts = new ConcurrentHashMap<>();
    private Set<UIContextListener> listeners = new LinkedHashSet<>();

    public void registerUIContext(UIContext context) {
        contexts.put(context.getID(), context);
    }

    public void unregisterUIContext(UIContext context) {
        contexts.remove(context.getID());
    }

    @Override
    public void addListener(UIContextListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(UIContextListener listener) {
        listeners.remove(listener);
    }
}
