
package com.ponysdk.ui.terminal.socket;

public interface WebSocketCallback {

    void connected();

    void disconnected();

    void message(String message);
}
