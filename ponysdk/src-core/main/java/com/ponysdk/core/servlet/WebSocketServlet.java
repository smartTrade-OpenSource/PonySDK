
package com.ponysdk.core.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.Application;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.socket.ConnectionListener;
import com.ponysdk.ui.server.basic.PPusher;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;

public class WebSocketServlet extends org.eclipse.jetty.websocket.WebSocketServlet {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServlet.class);

    private static final long serialVersionUID = 1L;

    @Override
    public WebSocket doWebSocketConnect(final HttpServletRequest req, final String arg1) {
        final long key = Long.parseLong(req.getParameter(APPLICATION.VIEW_ID));

        final Application applicationSession = (Application) req.getSession().getAttribute(Application.class.getCanonicalName());

        if (applicationSession == null) throw new RuntimeException("Invalid session, please reload your application");

        final UIContext uiContext = applicationSession.getUIContext(key);

        UIContext.setCurrent(uiContext);

        final JettyWebSocket jettyWebSocket = new JettyWebSocket();

        UIContext.get().acquire();
        try {
            PPusher.get().initialize(jettyWebSocket);
        } finally {
            UIContext.get().release();
        }

        return jettyWebSocket;
    }

    private class JettyWebSocket implements OnTextMessage, com.ponysdk.core.socket.WebSocket {

        private Connection connection;
        private ConnectionListener connectionListener;

        @Override
        public void onOpen(final Connection connection) {
            log.info("Connection received from: " + connection.toString());
            this.connection = connection;
            connectionListener.onOpen();
        }

        @Override
        public void send(final String msg) throws IOException {
            connection.sendMessage(msg);
        }

        @Override
        public void addConnectionListener(final ConnectionListener connectionListener) {
            this.connectionListener = connectionListener;
        }

        @Override
        public void onClose(final int arg0, final String arg1) {
            log.info("Connection lost from: " + connection.toString());
            connectionListener.onClose();
        }

        @Override
        public void onMessage(final String arg0) {}

    }

}
