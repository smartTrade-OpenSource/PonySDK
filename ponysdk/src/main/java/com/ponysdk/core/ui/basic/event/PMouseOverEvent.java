
package com.ponysdk.core.ui.basic.event;

import com.ponysdk.core.terminal.DomHandlerType;

public class PMouseOverEvent extends PMouseEvent<PMouseOverHandler> {

    public static final PDomEvent.Type<PMouseOverHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.MOUSE_OVER);

    public PMouseOverEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.ui.eventbus.Event.Type<PMouseOverHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PMouseOverHandler handler) {
        handler.onMouseOver(this);
    }

}