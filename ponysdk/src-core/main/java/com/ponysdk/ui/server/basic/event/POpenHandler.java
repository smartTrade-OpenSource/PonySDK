
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.PEventHandler;

public interface POpenHandler extends PEventHandler {

    void onOpen(POpenEvent openEvent);
}
