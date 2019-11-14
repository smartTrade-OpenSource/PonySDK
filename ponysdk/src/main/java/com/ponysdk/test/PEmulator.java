package com.ponysdk.test;

import com.google.gwt.json.client.JSONObject;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.DomHandlerType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.PObject;

import javax.json.Json;
import javax.json.JsonObject;

public class PEmulator {

    public static void windowOpened(PObject object) {
        JsonObject jsonObject = Json.createObjectBuilder().add(ClientToServerModel.HANDLER_OPEN.toStringValue(), "url").build();
        object.onClientData(jsonObject);
    }

    public static void windowClosed(PObject object) {
        JsonObject jsonObject = Json.createObjectBuilder().addNull(ClientToServerModel.HANDLER_CLOSE.toStringValue()).build();
        object.onClientData(jsonObject);
    }

    public static void valueChange(PObject object, boolean value) {
        JsonObject jsonObject = Json.createObjectBuilder().add(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE.toStringValue(), value).build();
        object.onClientData(jsonObject);
    }

    public static void click(int objectID) {
        click(UIContext.get().getObject(objectID));
    }

    public static void click(PObject object) {
        JsonObject jsonObject = Json.createObjectBuilder().add(ClientToServerModel.DOM_HANDLER_TYPE.toStringValue(), DomHandlerType.CLICK.getValue()).build();
        object.onClientData(jsonObject);
    }

    public static void addonData(PObject object, JSONObject value) {
        JsonObject jsonObject = Json.createObjectBuilder().add(ClientToServerModel.NATIVE.toStringValue(), "NATIVE").add("NATIVE", value.toString()).build();
        object.onClientData(jsonObject);
    }
}
