
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.EventHandler;

public interface PCloseHandler extends EventHandler {

    void onClose(PCloseEvent closeEvent);
}
