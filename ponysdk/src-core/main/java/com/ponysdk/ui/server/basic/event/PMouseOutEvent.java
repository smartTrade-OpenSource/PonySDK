
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PMouseOutEvent extends PMouseEvent<PMouseOutHandler> {

    public static final PDomEvent.Type<PMouseOutHandler> TYPE = new PDomEvent.Type<PMouseOutHandler>(DomHandlerType.MOUSE_OUT);

    public PMouseOutEvent(Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.event.PEvent.Type<PMouseOutHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(PMouseOutHandler handler) {
        handler.onMouseOut();
    }
}
