
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.EventHandler;

public abstract class PKeyEvent<H extends EventHandler> extends PDomEvent<H> {

    public PKeyEvent(Object sourceComponent) {
        super(sourceComponent);
    }

}