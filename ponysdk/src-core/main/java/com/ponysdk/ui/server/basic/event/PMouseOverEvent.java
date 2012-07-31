
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PMouseOverEvent extends PMouseEvent<PMouseOverHandler> {

    public static final PDomEvent.Type<PMouseOverHandler> TYPE = new PDomEvent.Type<PMouseOverHandler>(DomHandlerType.MOUSE_OVER);

    public PMouseOverEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.event.Event.Type<PMouseOverHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PMouseOverHandler handler) {
        handler.onMouseOver(this);
    }

}