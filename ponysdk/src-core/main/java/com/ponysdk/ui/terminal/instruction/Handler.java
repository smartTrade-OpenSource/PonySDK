
package com.ponysdk.ui.terminal.instruction;

import com.google.gwt.user.client.Window;
import com.ponysdk.ui.terminal.HandlerType;

public class Handler extends Instruction {

    private static final long serialVersionUID = 1L;

    protected String type;

    public Handler() {}

    public Handler(final long objectID, final HandlerType type) {
        super(objectID);
        this.type = type.getCode();
    }

    public Handler(final long objectID, final String type) {
        super(objectID);
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public HandlerType getHandlerType() {
        try {
            return HandlerType.from(type);
        } catch (final Throwable e) {

            // String ord = "";
            // final HandlerType[] values = HandlerType.values();
            // for (final HandlerType handlerType : values) {
            // ord += handlerType.name() + "/" + handlerType.ordinal() + ", ";
            // }

            Window.alert("type = " + type + ", ord=" + "o" + ", getHandlerType ERROR: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
