
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PDragStartEvent extends PDomEvent<PDragStartHandler> {

    public static final PDomEvent.Type<PDragStartHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.DRAG_START);

    public PDragStartEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.event.Event.Type<PDragStartHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PDragStartHandler handler) {
        handler.onDragStart(this);
    }

}