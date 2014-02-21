
package com.ponysdk.core.socket;

import java.io.IOException;

public interface WebSocket {

    void close();

    void send(String msg) throws IOException;

    void addConnectionListener(ConnectionListener listener);

}
