
package com.ponysdk.core.socket;

import com.ponysdk.core.servlet.WebSocketServlet.Buffer;

public interface WebSocket {

    void close();

    void flush();

    void addConnectionListener(ConnectionListener listener);

    Buffer getBuffer();

}
