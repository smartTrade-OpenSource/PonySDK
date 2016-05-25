
package com.ponysdk.core.socket;

import com.ponysdk.core.servlet.WebSocketServlet.Buffer;

public interface WebSocket {

    void flush(final Buffer buffer);

    void addConnectionListener(ConnectionListener listener);

    Buffer getBuffer();

    void sendHeartBeat();

}
