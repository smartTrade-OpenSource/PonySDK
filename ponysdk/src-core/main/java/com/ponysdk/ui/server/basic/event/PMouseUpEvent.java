
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PMouseUpEvent extends PMouseEvent<PMouseUpHandler> {

    public static final PDomEvent.Type<PMouseUpHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.MOUSE_UP);

    public PMouseUpEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.event.Event.Type<PMouseUpHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PMouseUpHandler handler) {
        handler.onMouseUp(this);
    }
}
