
package com.ponysdk.ui.server.basic.event;

import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.terminal.DomHandlerType;

public class PDropEvent extends PDomEvent<PDropHandler> {

    public static final PDomEvent.Type<PDropHandler> TYPE = new PDomEvent.Type<PDropHandler>(DomHandlerType.DROP);

    private PWidget dragSource;

    public PDropEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.event.Event.Type<PDropHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(final PDropHandler handler) {
        handler.onDrop(this);
    }

    public void setDragSource(final PWidget source) {
        this.dragSource = source;
    }

    public PWidget getDragSource() {
        return dragSource;
    }

}