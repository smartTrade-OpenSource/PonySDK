
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PDragEnterEvent extends PDomEvent<PDragEnterHandler> {

    public static final PDomEvent.Type<PDragEnterHandler> TYPE = new PDomEvent.Type<PDragEnterHandler>(DomHandlerType.DRAG_ENTER);

    public PDragEnterEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.event.PEvent.Type<PDragEnterHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PDragEnterHandler handler) {
        handler.onDragEnter(this);
    }

}