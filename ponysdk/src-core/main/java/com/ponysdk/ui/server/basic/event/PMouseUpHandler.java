
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.EventHandler;

@FunctionalInterface
public interface PMouseUpHandler extends EventHandler {

    void onMouseUp(PMouseUpEvent mouseUp);
}
