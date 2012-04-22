
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.core.event.PEventHandler;

public abstract class PKeyEvent<H extends PEventHandler> extends PDomEvent<H> {

    public PKeyEvent(Object sourceComponent) {
        super(sourceComponent);
    }

}