
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.EventHandler;

@FunctionalInterface
public interface PMouseDownHandler extends EventHandler {

    void onMouseDown(PMouseDownEvent mouseDownEvent);
}
