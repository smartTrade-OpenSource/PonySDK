
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.EventHandler;

@FunctionalInterface
public interface POpenHandler extends EventHandler {

    void onOpen(POpenEvent openEvent);
}
