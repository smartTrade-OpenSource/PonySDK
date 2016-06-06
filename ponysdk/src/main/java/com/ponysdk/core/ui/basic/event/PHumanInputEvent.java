
package com.ponysdk.core.ui.basic.event;

import com.ponysdk.core.ui.eventbus.EventHandler;

public abstract class PHumanInputEvent<H extends EventHandler> extends PDomEvent<H> {

    public PHumanInputEvent(Object sourceComponent) {
        super(sourceComponent);
    }

}