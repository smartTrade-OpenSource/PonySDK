
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PKeyUpEvent extends PKeyEvent<PKeyUpHandler> {

    public static final PDomEvent.Type<PKeyUpHandler> TYPE = new PDomEvent.Type<PKeyUpHandler>(DomHandlerType.KEY_UP);

    private final int keyCode;

    public PKeyUpEvent(final Object sourceComponent, final int keyCode) {
        super(sourceComponent);
        this.keyCode = keyCode;
    }

    @Override
    public com.ponysdk.core.event.Event.Type<PKeyUpHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PKeyUpHandler handler) {
        handler.onKeyUp(this);
    }

    public int getKeyCode() {
        return keyCode;
    }

}