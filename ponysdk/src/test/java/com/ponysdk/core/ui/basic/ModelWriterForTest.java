package com.ponysdk.core.ui.basic;

import com.google.gwt.json.client.JSONObject;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.context.impl.UIContextImpl;
import com.ponysdk.core.writer.ModelWriter;

import javax.json.Json;
import javax.json.JsonObject;

public class ModelWriterForTest extends ModelWriter {
    private PWindow window;
    private int lastObjectID;

    public ModelWriterForTest() {
        super(null);
    }

    @Override
    public void beginObject(PWindow window) {
        this.window = window;
    }

    @Override
    public void endObject() {
    }

    @Override
    public PWindow getCurrentWindow() {
        return window;
    }

    @Override
    public void write(ServerToClientModel model) {
        write(model, null);
    }

    @Override
    public void write(ServerToClientModel model, Object value) {
        if (model == ServerToClientModel.TYPE_CREATE || model == ServerToClientModel.TYPE_UPDATE) {
            lastObjectID = (int) value;
        } else if (model == ServerToClientModel.OPEN) {
            PObject object = UIContextImpl.get().getObject(lastObjectID);
            if (object instanceof PWindow) {
                simulateWindowOpened(object);
            }
        } else if (model == ServerToClientModel.CLOSE) {
            PObject object = UIContextImpl.get().getObject(lastObjectID);
            if (object instanceof PWindow) {
                simulateWindowClosed(object);
            }
        } else if (model == ServerToClientModel.HISTORY_FIRE_EVENTS) {
            if (Boolean.TRUE.equals(value)) {
                UIContextImpl.get().getHistory().fireHistoryChanged(UIContextImpl.get().getHistory().getToken());
            }
        }
    }

    public static void simulateWindowOpened(PObject object) {
        JsonObject jsonObject = Json.createObjectBuilder().add(ClientToServerModel.HANDLER_OPEN.toStringValue(), "url").build();
        object.onClientData(jsonObject);
    }

    public static void simulateWindowClosed(PObject object) {
        JsonObject jsonObject = Json.createObjectBuilder().addNull(ClientToServerModel.HANDLER_CLOSE.toStringValue()).build();
        object.onClientData(jsonObject);
    }

    public static void simulateValueChange(PObject object, boolean value) {
        JsonObject jsonObject = Json.createObjectBuilder().add(ClientToServerModel.HANDLER_BOOLEAN_VALUE_CHANGE.toStringValue(), value).build();
        object.onClientData(jsonObject);
    }

    public static void simulateAddon(PObject object, JSONObject value) {
        JsonObject jsonObject = Json.createObjectBuilder().add(ClientToServerModel.NATIVE.toStringValue(), "NATIVE").add("NATIVE", value.toString()).build();
        object.onClientData(jsonObject);
    }
}
