
package com.ponysdk.core.ui.basic.event;


import com.ponysdk.core.terminal.DomHandlerType;

public class PMouseDownEvent extends PMouseEvent<PMouseDownHandler> {

    public static final PDomEvent.Type<PMouseDownHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.MOUSE_DOWN);

    public PMouseDownEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.ui.eventbus.Event.Type<PMouseDownHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PMouseDownHandler handler) {
        handler.onMouseDown(this);
    }
}
