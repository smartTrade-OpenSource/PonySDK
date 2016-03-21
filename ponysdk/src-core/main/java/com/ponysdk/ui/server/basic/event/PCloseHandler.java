
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.EventHandler;

@FunctionalInterface
public interface PCloseHandler extends EventHandler {

    void onClose(PCloseEvent closeEvent);
}
