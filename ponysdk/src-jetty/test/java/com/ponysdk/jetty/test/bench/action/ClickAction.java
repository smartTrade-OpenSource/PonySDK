
package com.ponysdk.jetty.test.bench.action;

import org.json.JSONObject;

import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;
import com.ponysdk.ui.terminal.DomHandlerType;

public class ClickAction extends Action {

    private final String did;

    public ClickAction(final String did) {
        this.did = did;
    }

    @Override
    public JSONObject asJSON() throws Exception {
        final JSONObject j = newInstruction(did);
        j.put(TYPE.KEY, TYPE.KEY_.EVENT);
        j.put(HANDLER.KEY, HANDLER.KEY_.DOM_HANDLER);
        j.put(PROPERTY.DOM_HANDLER_TYPE, DomHandlerType.CLICK.ordinal());
        j.put(PROPERTY.CLIENT_X, 0);
        j.put(PROPERTY.CLIENT_Y, 0);
        j.put(PROPERTY.SOURCE_ABSOLUTE_LEFT, 0d);
        j.put(PROPERTY.SOURCE_ABSOLUTE_TOP, 0d);
        j.put(PROPERTY.SOURCE_OFFSET_HEIGHT, 0d);
        j.put(PROPERTY.SOURCE_OFFSET_WIDTH, 0d);
        return j;
    }

}
