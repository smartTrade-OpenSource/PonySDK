
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PDragEndEvent extends PDomEvent<PDragEndHandler> {

    public static final PDomEvent.Type<PDragEndHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.DRAG_END);

    public PDragEndEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.event.Event.Type<PDragEndHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PDragEndHandler handler) {
        handler.onDragEnd(this);
    }

}