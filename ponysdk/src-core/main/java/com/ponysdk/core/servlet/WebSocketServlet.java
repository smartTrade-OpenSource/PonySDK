
package com.ponysdk.core.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.OnTextMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.Application;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.socket.ConnectionListener;
import com.ponysdk.ui.server.basic.PPusher;
import com.ponysdk.ui.terminal.Dictionnary;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;

public class WebSocketServlet extends org.eclipse.jetty.websocket.WebSocketServlet {

    protected static final Logger log = LoggerFactory.getLogger(WebSocketServlet.class);

    private static final long serialVersionUID = 1L;

    public int maxIdleTime = -1;

    @Override
    public WebSocket doWebSocketConnect(final HttpServletRequest req, final String arg1) {
        final long key = Long.parseLong(req.getParameter(APPLICATION.VIEW_ID));

        final Application applicationSession = (Application) req.getSession().getAttribute(Application.class.getCanonicalName());
        if (applicationSession == null) throw new RuntimeException("Invalid session, please reload your application");

        JettyWebSocket jettyWebSocket;

        final UIContext uiContext = applicationSession.getUIContext(key);
        uiContext.acquire();
        try {
            UIContext.setCurrent(uiContext);
            jettyWebSocket = newJettyWebsocket();
            PPusher.get().initialize(jettyWebSocket);
        } finally {
            UIContext.remove();
            uiContext.release();
        }

        return jettyWebSocket;
    }

    protected JettyWebSocket newJettyWebsocket() {
        return new JettyWebSocket();
    }

    public class JettyWebSocket implements OnTextMessage, com.ponysdk.core.socket.WebSocket {

        protected Connection connection;
        protected ConnectionListener connectionListener;

        @Override
        public void onOpen(final Connection connection) {
            log.info("Connection received from: " + connection.toString());
            this.connection = connection;
            this.connection.setMaxIdleTime(maxIdleTime);
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
        public void onClose(final int closeCode, final String message) {
            log.info("Connection lost from: " + connection.toString() + ". Code: " + closeCode + ". Message: " + message);
            connectionListener.onClose();
        }

        @Override
        public void onMessage(final String message) {
            try {
                final JSONObject jso = new JSONObject();
                jso.put(Dictionnary.APPLICATION.PING, (int) (System.currentTimeMillis() * .001));
                connection.sendMessage(jso.toString());
            } catch (final JSONException e) {
                log.error("", e);
            } catch (final IOException e) {
                log.error("", e);
            }
        }
    }

    public void setMaxIdleTime(final int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

}
