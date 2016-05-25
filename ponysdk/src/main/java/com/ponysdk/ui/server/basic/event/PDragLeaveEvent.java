
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PDragLeaveEvent extends PDomEvent<PDragLeaveHandler> {

    public static final PDomEvent.Type<PDragLeaveHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.DRAG_LEAVE);

    public PDragLeaveEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.event.Event.Type<PDragLeaveHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PDragLeaveHandler handler) {
        handler.onDragLeave(this);
    }

}