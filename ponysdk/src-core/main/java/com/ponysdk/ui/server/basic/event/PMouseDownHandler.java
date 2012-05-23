
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.PEventHandler;

public interface PMouseDownHandler extends PEventHandler {

    void onMouseDown(PMouseDownEvent mouseDownEvent);
}
