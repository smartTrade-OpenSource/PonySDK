package com.ponysdk.core.server.websocket;

import com.ponysdk.core.server.context.UIContext;

public interface WebsocketDecoder {
    void decode(UIContext uiContext, String message);
}
