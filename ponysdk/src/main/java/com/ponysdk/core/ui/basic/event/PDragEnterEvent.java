
package com.ponysdk.core.ui.basic.event;


import com.ponysdk.core.terminal.DomHandlerType;

public class PDragEnterEvent extends PDomEvent<PDragEnterHandler> {

    public static final PDomEvent.Type<PDragEnterHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.DRAG_ENTER);

    public PDragEnterEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.ui.eventbus.Event.Type<PDragEnterHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PDragEnterHandler handler) {
        handler.onDragEnter(this);
    }

}