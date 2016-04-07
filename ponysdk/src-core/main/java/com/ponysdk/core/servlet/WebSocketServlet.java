/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.AbstractApplicationManager;
import com.ponysdk.core.Application;
import com.ponysdk.core.UIContext;
import com.ponysdk.core.socket.ConnectionListener;
import com.ponysdk.core.stm.TxnSocketContext;
import com.ponysdk.core.useragent.UserAgent;
import com.ponysdk.ui.terminal.model.Model;

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

        applicationManager = (AbstractApplicationManager) getServletContext()
                .getAttribute(AbstractApplicationManager.class.getCanonicalName());

        log.info("Initializing Buffer allocation ...");

        for (int i = 0; i < 50; i++) {
            buffers.add(new Buffer());
        }

        log.info("Buffer allocation initialized {}", DEFAULT_BUFFER_SIZE * 50);
    }

    @Override
    public void configure(final WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(maxIdleTime);
        factory.setCreator((final ServletUpgradeRequest req, final ServletUpgradeResponse resp) -> new JettyWebSocket(req, resp));
    }

    public class JettyWebSocket implements WebSocketListener, com.ponysdk.core.socket.WebSocket {

        private ConnectionListener listener;

        private TxnSocketContext context;

        private Session session;

        private final SocketRequest request;

        public JettyWebSocket(final ServletUpgradeRequest req, final ServletUpgradeResponse resp) {
            System.err.println(req.getHeader("User-Agent") + " : " + UserAgent.parseUserAgentString(req.getHeader("User-Agent")));
            request = new SocketRequest(req);
        }

        @Override
        public void addConnectionListener(final ConnectionListener listener) {
            this.listener = listener;
        }

        @Override
        public void onWebSocketConnect(final Session session) {
            log.info("WebSocket connected from {}", session.getRemoteAddress());

            this.session = session;

            context = new TxnSocketContext();
            context.setRequest(request);
            context.setSocket(this);

            Application application = context.getApplication();
            final boolean isNewHttpSession = application != null;
            final Integer reloadedViewID = null;
            if (application == null) {
                application = new Application(context, applicationManager.getOptions());

                final String sessionId = request.getSessionId();
                SessionManager.get().setApplication(sessionId, application);

                context.setApplication(application);

                log.info("Creating a new application, {}", application.toString());
            } else {
                // if (data.containsKey(Model.APPLICATION_VIEW_ID.getKey())) {
                // final JsonNumber jsonNumber = data.getJsonNumber(Model.APPLICATION_VIEW_ID.getKey());
                // reloadedViewID = jsonNumber.longValue();
                // }
                // log.info("Reloading application {} {}", reloadedViewID, context);
            }

            try {
                applicationManager.startApplication(context, application, isNewHttpSession, reloadedViewID);
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
        public void onWebSocketClose(final int arg0, final String arg1) {
            if (listener != null) listener.onClose();
        }

        /**
         * Receive from the terminal
         */
        @Override
        public void onWebSocketText(final String text) {
            if (context.getUIContext().isDestroyed()) {
                log.info("Message dropped, ui context is destroyed");
            } else {
                //onBeforeMessageReceived(text);
                try {
                    context.getUIContext().notifyMessageReceived();
                    request.setText(text);

                    if (log.isInfoEnabled()) log.info("Message received from terminal : " + text);

                    applicationManager.process(context);
                } catch (final Throwable e) {
                    log.error("Cannot process message from the browser: {}", text, e);
                } finally {
                    //onAfterMessageProcessed(text);
                }
            }
        }

        @Override
        public void onWebSocketBinary(final byte[] arg0, final int arg1, final int arg2) {
        }

        @Override
        public Buffer getBuffer() {
            try {
                return buffers.poll(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                log.error("Cannot poll buffer", e);
                return null;
            }
        }

        /**
         * Send to the terminal
         */
        @Override
        public void flush(final Buffer buffer) {
            final UIContext uiContext = context.getUIContext();

            if (uiContext.isDestroyed()) {
                throw new IllegalStateException("UI Context has been destroyed");
            }

            if (session == null || !session.isOpen()) {
                log.info("Session is down");
            } else if (buffer != null) {
                // onBeforeSendMessage();
                try {
                    final ByteBuffer socketBuffer = buffer.getSocketBuffer();

                    flush(socketBuffer);
                    socketBuffer.clear();

                    buffers.put(buffer);
                } catch (final Throwable t) {
                    log.error("Cannot flush to WebSocket", t);
                } finally {
                    // onAfterMessageSent();
                }
            } else {
                System.err.println("Already flushed");
            }
        }

        public void flush(final ByteBuffer socketBuffer) {
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
            }
        }

        /**
         * Send heart beat to the terminal
         */
        @Override
        public void sendHeartBeat() {
            final ByteBuffer socketBuffer = ByteBuffer.allocateDirect(2);
            socketBuffer.putShort(Model.HEARTBEAT.getValue());
            flush(socketBuffer);
        }
    }

    public void setMaxIdleTime(final int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

}
