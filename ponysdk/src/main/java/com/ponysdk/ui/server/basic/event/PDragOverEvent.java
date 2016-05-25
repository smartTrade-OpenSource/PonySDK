
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PDragOverEvent extends PDomEvent<PDragOverHandler> {

    public static final PDomEvent.Type<PDragOverHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.DRAG_OVER);

    public PDragOverEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.event.Event.Type<PDragOverHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PDragOverHandler handler) {
        handler.onDragOver(this);
    }

}