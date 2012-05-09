
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PDragEvent extends PMouseEvent<PDragHandler> {

    public static final PDomEvent.Type<PDragHandler> TYPE = new PDomEvent.Type<PDragHandler>(DomHandlerType.MOUSE_OVER);

    public PDragEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.event.PEvent.Type<PDragHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PDragHandler handler) {
        handler.onDrag(this);
    }

}