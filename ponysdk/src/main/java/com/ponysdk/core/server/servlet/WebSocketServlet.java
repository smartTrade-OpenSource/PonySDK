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

package com.ponysdk.core.server.servlet;

import java.nio.ByteBuffer;
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
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.AbstractApplicationManager;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.useragent.UserAgent;

public class WebSocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServlet.class);

    private static final long serialVersionUID = 1L;
    private static final int NUMBER_OF_BUFFERS = 50;
    private static final int DEFAULT_BUFFER_SIZE = 512000;
    private final BlockingQueue<Buffer> buffers = new ArrayBlockingQueue<>(NUMBER_OF_BUFFERS);
    private int maxIdleTime = 1000000;
    private AbstractApplicationManager applicationManager;
    private WebsocketMonitor monitor;

    @Override
    public void init() throws ServletException {
        super.init();

        applicationManager = (AbstractApplicationManager) getServletContext()
                .getAttribute(AbstractApplicationManager.class.getCanonicalName());

        if (log.isInfoEnabled())
            log.info("Initializing Buffer allocation ...");

        for (int i = 0; i < NUMBER_OF_BUFFERS; i++) {
            buffers.add(new Buffer());
        }

        if (log.isInfoEnabled())
            log.info("Buffer allocation initialized {}", DEFAULT_BUFFER_SIZE * buffers.size());
    }

    @Override
    public void configure(final WebSocketServletFactory factory) {
        // Remove compression capabilities to avoid a max buffer size bug
        factory.getExtensionFactory().unregister("permessage-deflate");

        factory.getPolicy().setIdleTimeout(maxIdleTime);
        factory.setCreator((request, response) -> new WebSocket(request, response));
    }

    public void setMaxIdleTime(final int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public void setWebsocketMonitor(final WebsocketMonitor monitor) {
        this.monitor = monitor;
    }

    public static class Buffer {

        final ByteBuffer socketBuffer;

        private Buffer() {
            socketBuffer = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
        }

        public ByteBuffer getSocketBuffer() {
            return socketBuffer;
        }

    }

    public class WebSocket implements WebSocketListener {

        private final PRequest request;
        private TxnContext context;
        private Session session;

        WebSocket(final ServletUpgradeRequest request, final ServletUpgradeResponse response) {
            log.info(request.getHeader("User-Agent"));
            this.request = new PRequest(request);
        }

        @Override
        public void onWebSocketConnect(final Session session) {
            if (log.isInfoEnabled())
                log.info("WebSocket connected from {}, sessionID {}", session.getRemoteAddress(), request.getSession().getId());

            this.session = session;
            this.context = new TxnContext();
            this.context.setRequest(request);
            this.context.setSocket(this);

            final HttpSession sessionId = request.getSession();

            Application application = SessionManager.get().getApplication(sessionId);
            if (application == null) {
                application = new Application(request.getSession(), applicationManager.getOptions(),
                        UserAgent.parseUserAgentString(request.getHeader("User-Agent")));
                SessionManager.get().registerApplication(application);
            }

            context.setApplication(application);

            if (log.isInfoEnabled())
                log.info("Creating a new application, {}", application.toString());
            try {
                final UIContext uiContext = applicationManager.startApplication(context);

                if (isSessionOpen()) {
                    final ByteBuffer socketBuffer = ByteBuffer.allocateDirect(6);
                    socketBuffer.putShort(ServerToClientModel.UI_CONTEXT_ID.getValue());
                    socketBuffer.putInt(uiContext.getID());
                    flush(socketBuffer);
                }
            } catch (final Exception e) {
                log.error("Cannot process WebSocket instructions", e);
            }
        }

        @Override
        public void onWebSocketError(final Throwable throwable) {
            log.error("WebSoket Error", throwable);
        }

        @Override
        public void onWebSocketClose(final int arg0, final String arg1) {
            log.info("WebSoket closed {} / {}", arg0, arg1);
            if (context != null && context.getUIContext() != null) {
                if (context.getUIContext().isLiving()) {
                    context.getUIContext().onDestroy();
                }
            }
        }

        /**
         * Receive from the terminal
         */
        @Override
        public void onWebSocketText(final String text) {
            if (context.getUIContext().isLiving()) {
                if (monitor != null)
                    monitor.onMessageReceived(WebSocket.this, text);
                try {
                    context.getUIContext().notifyMessageReceived();

                    if (!ClientToServerModel.HEARTBEAT.toStringValue().equals(text)) {
                        request.setText(text);

                        if (log.isInfoEnabled())
                            log.info("Message received from terminal : " + text);

                        applicationManager.fireInstructions(context.getJsonObject(), context);
                    } else {
                        if (log.isDebugEnabled())
                            log.debug("Heartbeat received from terminal");
                    }
                } catch (final Throwable e) {
                    log.error("Cannot process message from the browser: {}", text, e);
                } finally {
                    if (monitor != null)
                        monitor.onMessageProcessed(WebSocket.this);
                }
            } else {
                if (log.isInfoEnabled())
                    log.info("Message dropped, ui context is destroyed");
            }
        }

        /**
         * Receive from the terminal
         */
        @Override
        public void onWebSocketBinary(final byte[] arg0, final int arg1, final int arg2) {
            // Can't receive binary data from terminal (GWT limitation)
        }

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
        public void flush(final Buffer buffer) {
            if (buffer != null) {
                if (context.getUIContext().isLiving()) {
                    if (isSessionOpen()) {
                        try {
                            final ByteBuffer socketBuffer = buffer.getSocketBuffer();

                            flush(socketBuffer);
                            socketBuffer.clear();

                            buffers.put(buffer);
                        } catch (final Throwable t) {
                            log.error("Cannot flush to WebSocket", t);
                        }
                    } else {
                        if (log.isDebugEnabled())
                            log.debug("Session is down");
                    }
                } else {
                    throw new IllegalStateException("UI Context has been destroyed");
                }
            } else {
                if (log.isInfoEnabled())
                    log.info("Already flushed");
            }
        }

        private void flush(final ByteBuffer socketBuffer) {
            if (socketBuffer.position() != 0) {
                if (monitor != null)
                    monitor.onBeforeFlush(WebSocket.this, socketBuffer.position());
                socketBuffer.flip();
                try {
                    final Future<Void> sendBytesByFuture = session.getRemote().sendBytesByFuture(socketBuffer);
                    sendBytesByFuture.get(25, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    log.error("Cannot stream data", e);
                } finally {
                    if (monitor != null)
                        monitor.onAfterFlush(WebSocket.this);
                }
            }
        }

        /**
         * Send heart beat to the terminal
         */
        public void sendHeartBeat() {
            if (isSessionOpen()) {
                final ByteBuffer socketBuffer = ByteBuffer.allocateDirect(2);
                socketBuffer.putShort(ServerToClientModel.HEARTBEAT.getValue());
                flush(socketBuffer);
            }
        }

        public void close() {
            if (isSessionOpen()) {
                log.info("Closing websocket programaticly");
                session.close();
            }
        }

        final boolean isSessionOpen() {
            return session != null && session.isOpen();
        }
    }
}
