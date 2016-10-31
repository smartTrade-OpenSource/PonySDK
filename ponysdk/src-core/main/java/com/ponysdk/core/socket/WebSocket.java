
package com.ponysdk.core.socket;

import java.io.IOException;

import com.ponysdk.ui.server.basic.PPusher;

public interface WebSocket {

    void close();

    void send(String msg) throws IOException;

    void addConnectionListener(PPusher listener);

}
