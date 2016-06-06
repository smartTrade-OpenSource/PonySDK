
package com.ponysdk.core.ui.basic.event;

import com.ponysdk.core.terminal.DomHandlerType;

public class PKeyUpEvent extends PKeyEvent<PKeyUpHandler> {

    public static final PDomEvent.Type<PKeyUpHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.KEY_UP);

    private final int keyCode;

    public PKeyUpEvent(final Object sourceComponent, final int keyCode) {
        super(sourceComponent);
        this.keyCode = keyCode;
    }

    @Override
    public com.ponysdk.core.ui.eventbus.Event.Type<PKeyUpHandler> getAssociatedType() {
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