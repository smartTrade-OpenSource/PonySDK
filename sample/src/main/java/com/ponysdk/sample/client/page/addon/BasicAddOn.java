
package com.ponysdk.sample.client.page.addon;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.ponysdk.ui.server.addon.DefaultAddOn;

public class BasicAddOn extends DefaultAddOn {

    public BasicAddOn() {}

    public void send(final String m) {
        // update("message", "i received #" + m);
    }

    @Override
    protected void restate(final JsonObject jsonObject) {
        final String msg = jsonObject.getString("message");

        System.out.println("Received message: " + msg);

        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("message", "i received #" + msg);
        builder.add("option", "an option");

        // builder.addChild("child1").put("submsg1", "here").put("submsg2", "there");

        update(builder);
    }

}
