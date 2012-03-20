
package com.ponysdk.ui.terminal.instruction;

import com.ponysdk.ui.terminal.HandlerType;

public class Handler extends Instruction {

    protected HandlerType handlerType;

    protected String addOnHandlerType;

    public Handler() {}

    public Handler(final long objectID, final HandlerType type) {
        super(objectID);
        this.handlerType = type;
    }

    public Handler(final long objectID, final String type) {
        super(objectID);
        this.addOnHandlerType = type;
    }

    public HandlerType getType() {
        return handlerType;
    }

}
