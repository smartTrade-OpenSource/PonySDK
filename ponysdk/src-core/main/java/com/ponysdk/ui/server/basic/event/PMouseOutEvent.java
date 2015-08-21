
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PMouseOutEvent extends PMouseEvent<PMouseOutHandler> {

    public static final PDomEvent.Type<PMouseOutHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.MOUSE_OUT);

    public PMouseOutEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.event.Event.Type<PMouseOutHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PMouseOutHandler handler) {
        handler.onMouseOut(this);
    }
}
