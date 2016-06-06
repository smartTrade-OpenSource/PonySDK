
package com.ponysdk.core.ui.basic.event;

import com.ponysdk.core.terminal.DomHandlerType;
import com.ponysdk.core.ui.basic.PWidget;

public class PDropEvent extends PDomEvent<PDropHandler> {

    public static final PDomEvent.Type<PDropHandler> TYPE = new PDomEvent.Type<>(DomHandlerType.DROP);

    private PWidget dragSource;

    public PDropEvent(final Object sourceComponent) {
        super(sourceComponent);
    }

    @Override
    public com.ponysdk.core.ui.eventbus.Event.Type<PDropHandler> getAssociatedType() {
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