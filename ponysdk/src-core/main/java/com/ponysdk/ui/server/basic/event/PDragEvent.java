
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PDragEvent extends PMouseEvent<PMouseOverHandler> {

    public static final PDomEvent.Type<PMouseOverHandler> TYPE = new PDomEvent.Type<PMouseOverHandler>(DomHandlerType.MOUSE_OVER);

    public PDragEvent(Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.event.PEvent.Type<PMouseOverHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(PMouseOverHandler handler) {
        handler.onMouseOver();
    }

}