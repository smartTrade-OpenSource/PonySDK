
package com.ponysdk.sample.client.page.addon;

import com.ponysdk.ui.server.addon.DefaultAddOn;
import com.ponysdk.ui.server.addon.JSONBuilder;

public class BasicAddOn extends DefaultAddOn {

    public BasicAddOn() {}

    public void send(final String m) {
        update("message", "i received #" + m);
    }

    @Override
    protected void restate(final JSONObject jsonObject) throws JSONException {
        final String msg = jsonObject.getString("message");

        System.out.println("Received message: " + msg);

        final JSONBuilder jso = new JSONBuilder();
        jso.put("message", "i received #" + msg);
        jso.put("option", "an option");
        jso.addChild("child1").put("submsg1", "here").put("submsg2", "there");

        update(jso.build());
    }

}
