package com.ponysdk.core.server.context;

import com.ponysdk.core.server.context.UIContextImpl;

public interface UIContextDestroyListener {

    void onUIContextDestroyed(UIContextImpl uiContext);
}
