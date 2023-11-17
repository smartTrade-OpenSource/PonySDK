package com.ponysdk.core.server.concurrent;

import com.ponysdk.core.server.concurrent.UIContext;

public interface UIContextDestroyListener {

    void onUIContextDestroyed(UIContext uiContext);
}
