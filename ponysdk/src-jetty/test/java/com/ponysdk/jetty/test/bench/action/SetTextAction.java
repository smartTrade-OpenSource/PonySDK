
package com.ponysdk.jetty.test.bench.action;

import org.json.JSONObject;

import com.ponysdk.ui.terminal.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.Dictionnary.TYPE;

public class SetTextAction extends Action {

    private final String did;
    private final String text;

    public SetTextAction(final String did, final String text) {
        this.did = did;
        this.text = text;
    }

    @Override
    public JSONObject asJSON() throws Exception {
        final JSONObject j = newInstruction(did);
        j.put(TYPE.KEY, TYPE.KEY_.EVENT);
        j.put(HANDLER.KEY, HANDLER.KEY_.STRING_VALUE_CHANGE_HANDLER);
        j.put(PROPERTY.VALUE, text);
        return j;
    }

}
