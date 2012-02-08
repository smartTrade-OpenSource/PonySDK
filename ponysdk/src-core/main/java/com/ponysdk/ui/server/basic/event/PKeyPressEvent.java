
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.terminal.DomHandlerType;

public class PKeyPressEvent extends PKeyEvent<PKeyPressHandler> {

    public static final PDomEvent.Type<PKeyPressHandler> TYPE = new PDomEvent.Type<PKeyPressHandler>(DomHandlerType.KEY_PRESS);

    private final int keyCode;

    public PKeyPressEvent(Object sourceComponent, int keyCode) {
        super(sourceComponent);
        this.keyCode = keyCode;
    }

    @Override
    public PDomEvent.Type<PKeyPressHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(PKeyPressHandler handler) {
        handler.onKeyPress(keyCode);
    }

}