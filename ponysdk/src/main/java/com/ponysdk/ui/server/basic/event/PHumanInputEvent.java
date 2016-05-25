
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.EventHandler;

public abstract class PHumanInputEvent<H extends EventHandler> extends PDomEvent<H> {

    public PHumanInputEvent(Object sourceComponent) {
        super(sourceComponent);
    }

}