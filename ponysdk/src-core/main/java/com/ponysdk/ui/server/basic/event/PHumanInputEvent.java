
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.PEventHandler;

public abstract class PHumanInputEvent<H extends PEventHandler> extends PDomEvent<H> {

    public PHumanInputEvent(Object sourceComponent) {
        super(sourceComponent);
    }

}