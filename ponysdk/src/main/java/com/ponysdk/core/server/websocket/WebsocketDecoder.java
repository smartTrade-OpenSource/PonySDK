package com.ponysdk.core.server.websocket;

import com.ponysdk.core.server.context.api.UIContext;

public interface WebsocketDecoder {
    void decode(UIContext uiContext, String message);
}
