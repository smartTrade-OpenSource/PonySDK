
package com.ponysdk.ui.terminal.socket;

import elemental.html.ArrayBuffer;

public interface WebSocketCallback {

    void connected();

    void disconnected();

    void message(ArrayBuffer message);
}
