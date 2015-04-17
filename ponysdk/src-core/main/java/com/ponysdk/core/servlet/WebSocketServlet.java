
package com.ponysdk.core.servlet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.Application;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.socket.ConnectionListener;
import com.ponysdk.ui.server.basic.PPusher;
import com.ponysdk.ui.terminal.Dictionnary;
import com.ponysdk.ui.terminal.Dictionnary.APPLICATION;

public class WebSocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet {

    protected static final Logger log = LoggerFactory.getLogger(WebSocketServlet.class);

    private static final long serialVersionUID = 1L;

    public int maxIdleTime = -1;

    @Override
    public void configure(final WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(10000);
        factory.setCreator(new MySessionSocketCreator());
    }

    public static class MySessionSocketCreator implements WebSocketCreator {

        @Override
        public Object createWebSocket(final ServletUpgradeRequest req, final ServletUpgradeResponse resp) {
            return new JettyWebSocket(req, resp);
        }
    }

    public static class JettyWebSocket implements WebSocketListener, com.ponysdk.core.socket.WebSocket {

        private Session session;

        private final long key;

        private final HttpSession httpSession;

        private UIContext uiContext;

        private ConnectionListener listener;

        public JettyWebSocket(final ServletUpgradeRequest req, final ServletUpgradeResponse resp) {
            final Map<String, List<String>> parameterMap = req.getParameterMap();
            this.key = Long.parseLong(parameterMap.get(APPLICATION.VIEW_ID).get(0));
            this.httpSession = req.getSession();
        }

        @Override
        public void close() {}

        @Override
        public void send(final String msg) throws IOException {
            if ((session != null) && (session.isOpen())) {
                // monitor ??
                final ByteBuffer buffer = ByteBuffer.allocateDirect(1000000);
                buffer.put(msg.getBytes("UTF8"));
                buffer.flip();
                session.getRemote().sendBytes(buffer); // callback needed ?
            } else {

            }
        }

        @Override
        public void addConnectionListener(final ConnectionListener listener) {
            this.listener = listener;
        }

        @Override
        public void onWebSocketBinary(final byte[] arg0, final int arg1, final int arg2) {}

        @Override
        public void onWebSocketClose(final int arg0, final String arg1) {
            listener.onClose();
        }

        @Override
        public void onWebSocketConnect(final Session session) {
            this.session = session;

            final Application applicationSession = (Application) httpSession.getAttribute(Application.class.getCanonicalName());
            if (applicationSession == null) throw new RuntimeException("Invalid session, please reload your application");

            uiContext = applicationSession.getUIContext(key);
            uiContext.acquire();
            try {
                UIContext.setCurrent(uiContext);
                PPusher.get().initialize(this);
            } finally {
                UIContext.remove();
                uiContext.release();
            }

            listener.onOpen();
        }

        @Override
        public void onWebSocketError(final Throwable arg0) {}

        @Override
        public void onWebSocketText(final String text) {
            try {
                uiContext.notifyMessageReceived();

                final JSONObject jso = new JSONObject();
                jso.put(Dictionnary.APPLICATION.PING, (int) (System.currentTimeMillis() * .001));
                // connection.sendMessage(jso.toString());
            } catch (final Throwable e) {
                log.error("", e);
            }
        }
    }

    public void setMaxIdleTime(final int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

}
