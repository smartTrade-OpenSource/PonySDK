
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.EventHandler;

@FunctionalInterface
public interface PLayoutResizeHandler extends EventHandler {

    void onLayoutResize(PLayoutResizeEvent resizeEvent);
}
