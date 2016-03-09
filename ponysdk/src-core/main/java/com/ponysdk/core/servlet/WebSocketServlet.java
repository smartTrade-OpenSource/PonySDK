
package com.ponysdk.core.servlet;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;

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
import com.ponysdk.core.useragent.UserAgent;

public class WebSocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet {

    protected static final Logger log = LoggerFactory.getLogger(WebSocketServlet.class);

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_BUFFER_SIZE = 512000;

    public int maxIdleTime = 1000000;

    private AbstractApplicationManager applicationManager;

    private final BlockingQueue<Buffer> buffers = new ArrayBlockingQueue<>(50);

    public class Buffer {

        ByteBuffer socketBuffer;
        CharBuffer charBuffer;

        public ByteBuffer getSocketBuffer() {
            return socketBuffer;
        }

        public CharBuffer getCharBuffer() {
            return charBuffer;
        }

        public Buffer() {
            socketBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
            // charBuffer = CharBuffer.allocate(DEFAULT_BUFFER_SIZE);
        }

    }

    @Override
    public void init() throws ServletException {
        super.init();

        applicationManager = (AbstractApplicationManager) getServletContext().getAttribute(AbstractApplicationManager.class.getCanonicalName());

        log.info("Initializing Buffer allocation ...");

        for (int i = 0; i < 50; i++) {
            buffers.add(new Buffer());
        }

        log.info("Buffer allocation initialized {}", DEFAULT_BUFFER_SIZE * 50);
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

        private ConnectionListener listener;

        private TxnSocketContext context;

        private Session session;

        private final SocketRequest request;

        private Buffer buffer;

        public JettyWebSocket(final ServletUpgradeRequest req, final ServletUpgradeResponse resp) {
            System.err.println(req.getHeader("User-Agent"));

            System.err.println(UserAgent.parseUserAgentString(req.getHeader("User-Agent")));
            request = new SocketRequest(req);
        }

        @Override
        public void close() {}

        @Override
        public void flush() {
            final UIContext uiContext = context.getUIContext();

            if (uiContext.isDestroyed()) { throw new IllegalStateException("UI Context has been destroyed"); }

            if (session == null || !session.isOpen()) {
                log.info("Session is down");
            } else if (buffer != null) {
                // onBeforeSendMessage();
                try {
                    flush(buffer);

                    buffer = null;
                } catch (final Throwable t) {
                    log.error("Cannot flush to WebSocket", t);
                } finally {
                    // onAfterMessageSent();
                }
            } else {
                System.err.println("Already flushed");
            }

        }

        private void flush(final Buffer buffer) {
            final ByteBuffer socketBuffer = buffer.getSocketBuffer();

            if (socketBuffer.position() != 0) {
                socketBuffer.flip();
                final Future<Void> sendBytesByFuture = session.getRemote().sendBytesByFuture(socketBuffer);
                try {
                    sendBytesByFuture.get(25, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    if (e instanceof EOFException) {
                        log.info("Remote Connection is closed");
                    } else {
                        log.error("Cannot stream data");
                    }

                }
                socketBuffer.clear();
            }

            try {
                buffers.put(buffer);
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
            log.info("WebSocket connected from {}", session.getRemoteAddress());

            this.session = session;

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
        public void onWebSocketBinary(final byte[] arg0, final int arg1, final int arg2) {}

        @Override
        public void onWebSocketText(final String text) {
            if (context.getUIContext().isDestroyed()) {
                log.info("Message dropped, ui context is destroyed");
            } else {
                onBeforeMessageReceived(text);
                try {
                    context.getUIContext().notifyMessageReceived();
                    request.setText(text);

                    if (log.isDebugEnabled()) log.debug("Message received : " + text);

                    applicationManager.process(context);
                } catch (final Throwable e) {
                    log.error("Cannot process message from the browser: {}", text, e);
                } finally {
                    onAfterMessageProcessed(text);
                }
            }
        }

        protected void onBeforeSendMessage(final String msg) {}

        protected void onAfterMessageSent(final String msg) {}

        protected void onBeforeMessageReceived(final String text) {}

        protected void onAfterMessageProcessed(final String text) {}

        @Override
        public Buffer getBuffer() {
            try {
                // if (session == null) {
                // if (buffer == null) {
                // buffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
                // }
                // } else {
                // if (buffer == null) {
                buffer = buffers.poll(5, TimeUnit.SECONDS);
                // }
                // }

            } catch (final InterruptedException e) {
                log.error("Cannot poll buffer", e);
            }
            return buffer;
        }

    }

    public void setMaxIdleTime(final int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

}
