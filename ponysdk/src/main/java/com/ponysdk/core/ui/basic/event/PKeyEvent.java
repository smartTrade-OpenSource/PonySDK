
package com.ponysdk.core.ui.basic.event;

import com.ponysdk.core.ui.eventbus.EventHandler;

public abstract class PKeyEvent<H extends EventHandler> extends PDomEvent<H> {

    public PKeyEvent(Object sourceComponent) {
        super(sourceComponent);
    }

}