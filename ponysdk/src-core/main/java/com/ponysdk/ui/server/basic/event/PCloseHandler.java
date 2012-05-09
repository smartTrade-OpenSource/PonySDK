
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.PEventHandler;

public interface PCloseHandler extends PEventHandler {

    void onClose(PCloseEvent closeEvent);
}
