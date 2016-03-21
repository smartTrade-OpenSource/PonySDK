
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.EventHandler;

@FunctionalInterface
public interface PMouseOutHandler extends EventHandler {

    void onMouseOut(PMouseOutEvent mouseOutEvent);
}
