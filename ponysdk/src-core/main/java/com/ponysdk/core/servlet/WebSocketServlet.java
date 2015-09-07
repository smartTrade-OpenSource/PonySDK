
package com.ponysdk.core.servlet;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.AbstractApplicationManager;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.socket.ConnectionListener;
import com.ponysdk.core.stm.TxnSocketContext;

public class WebSocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet {

    protected static final Logger log = LoggerFactory.getLogger(WebSocketServlet.class);

    private static final long serialVersionUID = 1L;

    public int maxIdleTime = 1000000;

    private AbstractApplicationManager applicationManager;

    private final BlockingQueue<ByteBuffer> bufferQueue = new ArrayBlockingQueue<>(50);

    @Override
    public void init() throws ServletException {
        super.init();

        applicationManager = (AbstractApplicationManager) getServletContext().getAttribute(AbstractApplicationManager.class.getCanonicalName());

        log.info("Initializing Buffer allocation ...");

        for (int i = 0; i < 50; i++) {
            bufferQueue.add(ByteBuffer.allocateDirect(2048));
        }

        log.info("Buffer allocation initialized {}", 2048 * 50);
    }

    @Override
    public void configure(final WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(maxIdleTime);
        factory.setCreator(new MySessionSocketCreator());
    }

    public class MySessionSocketCreator implements WebSocketCreator {

        @Override
        public Object createWebSocket(final ServletUpgradeRequest req, final ServletUpgradeResponse resp) {
            return new JettyWebSocket(req, resp);
        }
    }

    public class JettyWebSocket implements WebSocketListener, com.ponysdk.core.socket.WebSocket {

        // private final long key;

        private UIContext uiContext;

        private ConnectionListener listener;

        private TxnSocketContext context;

        private final HttpSession httpSession;

        private Session session;

        private final SocketRequest request;

        private ByteBuffer buffer;

        public JettyWebSocket(final ServletUpgradeRequest req, final ServletUpgradeResponse resp) {
            final Map<String, List<String>> parameterMap = req.getParameterMap();
            // key = Long.parseLong(parameterMap.get(Model.APPLICATION_VIEW_ID.getKey()).get(0));

            httpSession = HTTPServletContext.get().getRequest().getSession();
            request = new SocketRequest(req);
        }

        @Override
        public void close() {}

        @Override
        public void flush() {
            System.err.println("Flush");

            // onBeforeSendMessage();
            try {
                // iF ((SESSION != NULL) && (SESSION.ISOPEN())) {
                // SESSION.GETREMOTE().SENDBYTES(BUFFER); // CALLBACK NEEDED ?
                // } ELSE {
                // // ??
                // }
                flush(buffer);

            } catch (final Throwable t) {
                log.error("Cannot flush to WebSocket", t);
            } finally {
                // onAfterMessageSent();
            }
        }

        private void flush(final ByteBuffer buffer) {
            if (buffer.position() != 0) {
                buffer.flip();
                final Future<Void> sendBytesByFuture = session.getRemote().sendBytesByFuture(buffer);
                try {
                    sendBytesByFuture.get(5, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    e.printStackTrace();
                }
                buffer.clear();
            }

            try {
                bufferQueue.put(buffer);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void addConnectionListener(final ConnectionListener listener) {
            this.listener = listener;
        }

        @Override
        public void onWebSocketClose(final int arg0, final String arg1) {
            if (listener != null) listener.onClose();
        }

        @Override
        public void onWebSocketConnect(final Session session) {
            this.session = session;

            log.info("Websocket opened");

            context = new TxnSocketContext();
            context.setRequest(request);
            context.setSocket(this);

            try {
                applicationManager.process(context);
            } catch (final Exception e) {
                log.error("Cannot process WebSocket instructions", e);
            }

            // listener.onOpen();
        }

        @Override
        public void onWebSocketError(final Throwable throwable) {
            log.error("WebSoket Error", throwable);
        }

        @Override
        public void onWebSocketBinary(final byte[] arg0, final int arg1, final int arg2) {
            System.err.println("TonWebSocketBinary");
        }

        @Override
        public void onWebSocketText(final String text) {
            onBeforeMessageReceived(text);
            try {
                System.err.println("Text received : " + text);

                request.setText(text);
                applicationManager.process(context);
                // uiContext.notifyMessageReceived();
            } catch (final Throwable e) {
                log.error("", e);
            } finally {
                onAfterMessageProcessed(text);
            }
        }

        protected void onBeforeSendMessage(final String msg) {}

        protected void onAfterMessageSent(final String msg) {}

        protected void onBeforeMessageReceived(final String text) {}

        protected void onAfterMessageProcessed(final String text) {}

        @Override
        public ByteBuffer getByteBuffer() {
            try {
                buffer = bufferQueue.poll(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
            return buffer;
        }

    }

    public void setMaxIdleTime(final int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

}
