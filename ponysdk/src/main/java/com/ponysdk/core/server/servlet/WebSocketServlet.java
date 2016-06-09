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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.AbstractApplicationManager;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.stm.TxnContext;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.common.extensions.compress.DeflateFrameExtension;
import org.eclipse.jetty.websocket.common.extensions.compress.PerMessageDeflateExtension;
import org.eclipse.jetty.websocket.common.extensions.compress.XWebkitDeflateFrameExtension;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.*;

public class WebSocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServlet.class);

    private static final long serialVersionUID = 1L;

    private static final int NUMBER_OF_BUFFERS = 50;
    private static final int DEFAULT_BUFFER_SIZE = 512000;

    public int maxIdleTime = 1000000;

    private AbstractApplicationManager applicationManager;

    private final BlockingQueue<Buffer> buffers = new ArrayBlockingQueue<>(NUMBER_OF_BUFFERS);

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
        }

    }

    @Override
    public void init() throws ServletException {
        super.init();

        applicationManager = (AbstractApplicationManager) getServletContext().getAttribute(AbstractApplicationManager.class.getCanonicalName());

        if (log.isInfoEnabled()) log.info("Initializing Buffer allocation ...");

        for (int i = 0; i < NUMBER_OF_BUFFERS; i++) {
            buffers.add(new Buffer());
        }

        if (log.isInfoEnabled()) log.info("Buffer allocation initialized {}", DEFAULT_BUFFER_SIZE * buffers.size());
    }

    @Override
    public void configure(final WebSocketServletFactory factory) {
        // Add compression capabilities
        factory.getExtensionFactory().register("deflate-frame", DeflateFrameExtension.class);
        factory.getExtensionFactory().register("permessage-deflate", PerMessageDeflateExtension.class);
        factory.getExtensionFactory().register("x-webkit-deflate-frame", XWebkitDeflateFrameExtension.class);

        factory.getPolicy().setIdleTimeout(maxIdleTime);
        factory.setCreator((final ServletUpgradeRequest req, final ServletUpgradeResponse resp) -> new WebSocket(req, resp));
    }

    public class WebSocket implements WebSocketListener {

        private TxnContext context;

        private Session session;

        private final PRequest request;

        WebSocket(final ServletUpgradeRequest req, final ServletUpgradeResponse resp) {
            log.info(req.getHeader("User-Agent"));
            request = new PRequest(req);
        }

        @Override
        public void onWebSocketConnect(final Session session) {
            if (log.isInfoEnabled()) log.info("WebSocket connected from {}, sessionID {}", session.getRemoteAddress(),request.getSessionId());

            this.session = session;
            this.context = new TxnContext();
            this.context.setRequest(request);
            this.context.setSocket(this);

            final String sessionId = request.getSessionId();

            Application application = SessionManager.get().getApplication(sessionId);
            if (application == null) {
                application = new Application(sessionId, applicationManager.getOptions());
                SessionManager.get().setApplication(sessionId, application);
            }

            context.setApplication(application);

            if (log.isInfoEnabled()) log.info("Creating a new application, {}", application.toString());
            try {
                applicationManager.startApplication(context);
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
                // onBeforeMessageReceived(text);
                try {
                    context.getUIContext().notifyMessageReceived();

                    if (!ClientToServerModel.HEARTBEAT.toStringValue().equals(text)) {
                        request.setText(text);

                        if (log.isInfoEnabled()) log.info("Message received from terminal : " + text);

                        applicationManager.fireInstructions(context.getJsonObject(), context);
                    } else {
                        if (log.isDebugEnabled()) log.debug("Heartbeat received from terminal");
                    }
                } catch (final Throwable e) {
                    log.error("Cannot process message from the browser: {}", text, e);
                } finally {
                    // onAfterMessageProcessed(text);
                }
            } else {
                if (log.isInfoEnabled()) log.info("Message dropped, ui context is destroyed");
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
                    if (session != null && session.isOpen()) {
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
                        if (log.isDebugEnabled()) log.debug("Session is down");
                    }
                } else {
                    throw new IllegalStateException("UI Context has been destroyed");
                }
            } else {
                if (log.isInfoEnabled()) log.info("Already flushed");
            }
        }

        public void flush(final ByteBuffer socketBuffer) {
            if (session != null && session.isOpen() && socketBuffer.position() != 0) {
                socketBuffer.flip();
                try {
                    final Future<Void> sendBytesByFuture = session.getRemote().sendBytesByFuture(socketBuffer);
                    sendBytesByFuture.get(25, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    if (e instanceof EOFException) {
                        if (log.isInfoEnabled()) log.info("Remote Connection is closed");
                    } else {
                        log.error("Cannot stream data");
                    }
                }
            }
        }

        /**
         * Send heart beat to the terminal
         */
        public void sendHeartBeat() {
            final ByteBuffer socketBuffer = ByteBuffer.allocateDirect(2);
            socketBuffer.putShort(ServerToClientModel.HEARTBEAT.getValue());
            flush(socketBuffer);
        }

        public void close() {
            if (session != null) {
                log.info("Closing websocket programaticly");
                session.close();
            }
        }


    }

    public void setMaxIdleTime(final int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

}
