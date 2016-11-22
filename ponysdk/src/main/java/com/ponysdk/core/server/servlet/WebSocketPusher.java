
package com.ponysdk.core.server.servlet;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;

import com.ponysdk.core.server.concurrent.AutoFlushedBuffer;

public class WebSocketPusher extends AutoFlushedBuffer implements WriteCallback {

    private final Session session;

    public WebSocketPusher(final Session session, final int bufferSize, final int maxChunkSize, final long timeoutMillis) {
        super(bufferSize, true, maxChunkSize, 0.25f, timeoutMillis);
        this.session = session;
    }

    @Override
    protected void doFlush(final ByteBuffer bufferToFlush) {
        session.getRemote().sendBytes(bufferToFlush, this);
    }

    @Override
    protected void closeFlusher() {
        session.close();
    }

    @Override
    public void writeFailed(final Throwable t) {
        if (t instanceof Exception) {
            onFlushFailure((Exception) t);
        } else {
            // wrap error into a generic exception to notify producer thread and rethrow the original throwable
            onFlushFailure(new IOException(t));
            WebSocketPusher.<RuntimeException> rethrow(t);
        }
    }

    @Override
    public void writeSuccess() {
        onFlushCompletion();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrow(final Throwable t) throws T {
        throw (T) t;
    }

}
