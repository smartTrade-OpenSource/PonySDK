package com.ponysdk.core.server.websocket;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.context.UIContext;
import com.ponysdk.core.ui.basic.PObject;

import javax.json.*;
import javax.json.spi.JsonProvider;
import java.io.StringReader;

public class WebSocketDecoderImpl implements WebsocketDecoder {
    private static final String DEFAULT_PROVIDER = "org.glassfish.json.JsonProviderImpl";

    private JsonProvider jsonProvider;

    public WebSocketDecoderImpl() {
        try {
            jsonProvider = (JsonProvider) Class.forName(DEFAULT_PROVIDER).getDeclaredConstructor().newInstance();
        } catch (final Exception t) {
            jsonProvider = JsonProvider.provider();
        }
    }

    public void decode(UIContext uiContext, final String message) {
        final JsonObject jsonObject;
        try (final JsonReader reader = jsonProvider.createReader(new StringReader(message))) {
            jsonObject = reader.readObject();
        }


        final String applicationInstructions = ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue();
        uiContext.execute(() -> {
            final JsonArray appInstructions = jsonObject.getJsonArray(applicationInstructions);
            for (int i = 0; i < appInstructions.size(); i++) {
                appInstructions.getJsonObject(i)

                if (jsonObject.containsKey(ClientToServerModel.TYPE_HISTORY.toStringValue())) {
                    history.fireHistoryChanged(jsonObject.getString(ClientToServerModel.TYPE_HISTORY.toStringValue()));
                } else {
                    final JsonValue jsonValue = jsonObject.get(ClientToServerModel.OBJECT_ID.toStringValue());
                    int objectID;
                    final JsonValue.ValueType valueType = jsonValue.getValueType();
                    if (JsonValue.ValueType.NUMBER == valueType) {
                        objectID = ((JsonNumber) jsonValue).intValue();
                    } else if (JsonValue.ValueType.STRING == valueType) {
                        objectID = Integer.parseInt(((JsonString) jsonValue).getString());
                    } else {
                        log.error("unknown reference from the browser. Unable to execute instruction: {}", jsonObject);
                        return;
                    }

                    //Cookies
                    if (objectID == 0) {
                        cookies.onClientData(jsonObject);
                    } else {
                        final PObject object = getObject(objectID);

                        if (object == null) {
                            log.error("unknown reference from the browser. Unable to execute instruction: {}", jsonObject);

                            if (jsonObject.containsKey(ClientToServerModel.PARENT_OBJECT_ID.toStringValue())) {
                                final int parentObjectID = jsonObject.getJsonNumber(ClientToServerModel.PARENT_OBJECT_ID.toStringValue())
                                        .intValue();
                                final PObject gcObject = getObject(parentObjectID);
                                if (log.isWarnEnabled()) log.warn(String.valueOf(gcObject));
                            }

                            return;
                        }

                        if (terminalDataReceiver != null) terminalDataReceiver.onDataReceived(object, jsonObject);

                        object.onClientData(jsonObject);
                    }
                }
            }
        });

    }
}
