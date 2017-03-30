
package com.ponysdk.core.terminal.ui;

import com.google.gwt.user.client.History;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;

public class PTHistory {

    private PTHistory(final UIBuilder uiBuilder) {
    }

    public static final void addValueChangeHandler(final UIBuilder uiBuilder) {
        History.addValueChangeHandler(event -> {
            if (event.getValue() != null && !event.getValue().isEmpty()) {
                final PTInstruction eventInstruction = new PTInstruction();
                eventInstruction.put(ClientToServerModel.TYPE_HISTORY, event.getValue());
                uiBuilder.sendDataToServer(eventInstruction);
            }
        });
    }

}
