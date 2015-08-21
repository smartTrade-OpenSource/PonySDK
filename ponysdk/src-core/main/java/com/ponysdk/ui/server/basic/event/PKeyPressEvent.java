
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PKeyPressEvent extends PKeyEvent<PKeyPressHandler> {

    public static final PDomEvent.Type<PKeyPressHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.KEY_PRESS);

    private final int keyCode;

    public PKeyPressEvent(final Object sourceComponent, final int keyCode) {
        super(sourceComponent);
        this.keyCode = keyCode;
    }

    @Override
    public PDomEvent.Type<PKeyPressHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PKeyPressHandler handler) {
        handler.onKeyPress(this);
    }

    public int getKeyCode() {
        return keyCode;
    }

}