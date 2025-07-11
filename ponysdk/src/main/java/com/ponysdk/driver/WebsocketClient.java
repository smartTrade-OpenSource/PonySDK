/*
 * Copyright (c) 2017 PonySDK
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

package com.ponysdk.driver;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.ClientEndpointConfig.Configurator;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Extension;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.MessageHandler.Whole;
import jakarta.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WebsocketClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(WebsocketClient.class);

    private volatile Session session;

    private final static List<String> USER_AGENT = List.of("PonyDriver");
    private final static Configurator configurator = new Configurator() {

        @Override
        public void afterResponse(final HandshakeResponse hr) {
            super.afterResponse(hr);
            log.debug("Response Headers : {}", hr.getHeaders());
        }

        @Override
        public void beforeRequest(final Map<String, List<String>> headers) {
            super.beforeRequest(headers);
            headers.put("User-Agent", USER_AGENT);
            log.debug("Request Headers : {}", headers);
        }
    };

    private final MessageHandler.Whole<ByteBuffer> handler;
    private final List<Extension> extensions;
    private PonySessionListener sessionListener;

    public WebsocketClient(final Whole<ByteBuffer> handler, final PonyBandwithListener bandwidthListener,
            final PonySessionListener sessionListener) {
        super();
        this.handler = handler;
        this.extensions = List.of(new PonyDriverPerMessageDeflateExtension(bandwidthListener));
        this.sessionListener = sessionListener;
    }

    public void connect(final URI uri) throws Exception {
        if (session != null) session.close();
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().configurator(configurator).extensions(extensions)
            .build();
        final ClientManager client = ClientManager.createClient();
        client.getProperties().put(ClientProperties.REDIRECT_ENABLED, true);
        final Lock lock = new ReentrantLock();
        final Condition condition = lock.newCondition();
        client.connectToServer(new Endpoint() {

            @Override
            public void onOpen(final Session session, final EndpointConfig config) {
                lock.lock();
                try {
                    WebsocketClient.this.session = session;
                    condition.signal();
                    session.addMessageHandler(handler);
                } finally {
                    lock.unlock();
                }
                sessionListener.onOpen(session);
            }

            @Override
            public void onClose(final Session session, final CloseReason closeReason) {
                super.onClose(session, closeReason);
                log.debug("Session {} closed : {} {}", session.getId(), closeReason.getCloseCode(), closeReason.getReasonPhrase());
                sessionListener.onClose(session, closeReason);
            }

            @Override
            public void onError(final Session session, final Throwable thr) {
                super.onError(session, thr);
                log.error("Session {} error", session.getId(), thr);
                sessionListener.onError(session, thr);
            }

        }, cec, uri);
        lock.lock();
        try {
            if (session == null && !condition.await(5, TimeUnit.MINUTES)) {
                log.error("Connection to {} failed after 5 minutes timeout", uri);
                return;
            }
        } finally {
            lock.unlock();
        }
        log.debug("Connected to {} with session ID {}", uri, session.getId());
    }

    public void sendMessage(final String message) throws IOException {
        session.getBasicRemote().sendText(message);
    }

    @Override
    public void close() {
        if (session != null) {
            final String id = session.getId();
            try {
                session.close();
            } catch (final IOException e) {
                log.error("Failed to close session {}", id, e);
            }
        }
    }

    public String getSessionId() {
        return session != null ? session.getId() : null;
    }

    public void setSessionListener(PonySessionListener sessionListener){
        this.sessionListener = sessionListener;
    }
}
