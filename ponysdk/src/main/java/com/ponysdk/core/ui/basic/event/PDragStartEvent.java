
package com.ponysdk.core.ui.basic.event;


import com.ponysdk.core.terminal.DomHandlerType;

public class PDragStartEvent extends PDomEvent<PDragStartHandler> {

    public static final PDomEvent.Type<PDragStartHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.DRAG_START);

    public PDragStartEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.ui.eventbus.Event.Type<PDragStartHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PDragStartHandler handler) {
        handler.onDragStart(this);
    }

}