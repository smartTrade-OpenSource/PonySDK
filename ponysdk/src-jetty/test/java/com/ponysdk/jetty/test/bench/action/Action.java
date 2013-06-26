
package com.ponysdk.jetty.test.bench.action;

import org.json.JSONObject;

import com.ponysdk.jetty.test.bench.UI;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;

public abstract class Action {

    protected UI ui;

    public void setUI(final UI ui) {
        this.ui = ui;
    }

    protected JSONObject newInstruction(final String did) throws Exception {
        final UI.Object uio = ui.objectByID.get(did);
        if (uio == null) throw new RuntimeException("UI.Object not found #" + did);

        final JSONObject j = new JSONObject();
        j.put(PROPERTY.OBJECT_ID, uio.pid);
        return j;
    }

    public abstract JSONObject asJSON() throws Exception;

}
