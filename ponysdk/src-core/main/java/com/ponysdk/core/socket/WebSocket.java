
package com.ponysdk.core.socket;

import java.nio.ByteBuffer;

public interface WebSocket {

    void close();

    void flush();

    void addConnectionListener(ConnectionListener listener);

    ByteBuffer getByteBuffer();

}
