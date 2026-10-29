package com.ponysdk.core.server.websocket;

import java.io.IOException;

import org.eclipse.jetty.websocket.api.Session;

import com.ponysdk.core.model.ServerToClientModel;

/** Test access to the package-private encode methods of {@link WebSocketPusher}. */
public final class PusherAccess {
    private final WebSocketPusher pusher;

    public PusherAccess(final Session session, final ByteBufferPool pool) {
        this.pusher = new WebSocketPusher(session, 1 << 15, 1_000, pool);
    }

    public void encode(final ServerToClientModel model, final Object value) throws IOException { pusher.encode(model, value); }
    public void encodeInt(final ServerToClientModel model, final int value) throws IOException { pusher.encodeInt(model, value); }
    public void flush() throws IOException { pusher.flush(); }
}
