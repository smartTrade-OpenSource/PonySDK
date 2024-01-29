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

package com.ponysdk.core.server.websocket;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.context.UIContext;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.core.Extension;
import org.eclipse.jetty.websocket.core.ExtensionStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class WebSocket implements Session.Listener, WebsocketEncoder {

    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);

    private final ApplicationManager applicationManager;
    private final long ID = ID_GENERATOR.incrementAndGet();
    private WebSocketPusher websocketPusher;
    private Session session;
    private UIContext uiContext;

    public WebSocket(ApplicationManager applicationManager, JettyServerUpgradeRequest request) {
        this.applicationManager = applicationManager;
        this.request = request;
    }

    public long getID() {
        return ID;
    }

    @Override
    public void onWebSocketOpen(final Session session) {
        try {
            log.info("New connection, creating new UIContext for the session {}", session);
            this.session = session;
            websocketPusher = new WebSocketPusher(session, 1 << 20, 1 << 12, TimeUnit.SECONDS.toMillis(60));
            uiContext = applicationManager.getUIContextFactory().create();
            applicationManager.startUIContext(uiContext);
            log.info("UIContext #{} created for the session {}", uiContext, session);
        } catch (final Exception e) {
            log.error("Cannot create UIContext", e);
            session.close();
        }
    }

    @Override
    public void onWebSocketError(final Throwable throwable) {
        log.error("WebSocket Error on UIContext #{}", uiContext.getID(), throwable);
        uiContext.stop();
        session.close();
    }

    @Override
    public void onWebSocketClose(final int statusCode, final String reason) {
        log.info("WebSocket closed on UIContext #{} : {}, reason : {}", uiContext.getID(), NiceStatusCode.getMessage(statusCode), Objects.requireNonNullElse(reason, ""));
        uiContext.stop();
    }

    @Override
    public void onWebSocketText(final String message) {
        uiContext.handleInstruction(message);
    }

    /**
     * Send round trip to the client
     */
    public void sendRoundTrip() {
        if (isAlive() && isSessionOpen()) {
            lastSentPing = System.nanoTime();
            beginObject();
            encode(ServerToClientModel.ROUNDTRIP_LATENCY, null);
            endObject();
            flush0();
        }
    }

    private void sendHeartbeat() {
        if (!isAlive() || !isSessionOpen()) return;
        beginObject();
        encode(ServerToClientModel.HEARTBEAT, null);
        endObject();
        flush0();
    }

    public void flush() {
        if (isAlive() && isSessionOpen()) flush0();
    }

    void flush0() {
        try {
            websocketPusher.flush();
        } catch (final IOException e) {
            log.error("Can't write on the websocket for #{}, so we destroy the application", uiContext.getID(), e);
            session.close();
        }
    }


    @Override
    public void beginObject() {
        // Nothing to do
    }

    @Override
    public void endObject() {
        encode(ServerToClientModel.END, null);
    }

    @Override
    public void encode(final ServerToClientModel model, final Object value) {
        try {
            if (loggerOut.isTraceEnabled()) {
                loggerOut.trace("UIContext #{} : {} {}", this.uiContext.getID(), model, value);
            }
            websocketPusher.encode(model, value);
            if (listener != null) listener.onOutgoingPonyFrame(model, value);
        } catch (final IOException e) {
            log.error("Can't write on the websocket for UIContext #{}, so we destroy the application", uiContext.getID(), e);
            uiContext.destroy();
        }
    }

    public void setListener(final Listener listener) {
        this.listener = listener;
        this.websocketPusher.setWebSocketListener(listener);
        if (!(session instanceof Container)) {
            log.warn("Unrecognized session type {} for {}", session == null ? null : session.getClass(), uiContext);
            return;
        }
        final ExtensionStack extensionStack = ((Container) session).getBean(ExtensionStack.class);
        if (extensionStack == null) {
            log.warn("No Extension Stack for {}", uiContext);
            return;
        }

        PonyPerMessageDeflateExtension ponyExtension = null;

        for (Extension extension : extensionStack.getExtensions()) {
            if (extension.getClass().equals(PonyPerMessageDeflateExtension.class)) {
                ponyExtension = (PonyPerMessageDeflateExtension) extension;
                break;
            }
        }

        if (ponyExtension == null) {
            log.warn("Missing PonyPerMessageDeflateExtension from Extension Stack for {}", uiContext);
            return;
        }
        ponyExtension.setWebSocketListener(listener);
    }

    private enum NiceStatusCode {

        NORMAL(StatusCode.NORMAL, "Normal closure"), SHUTDOWN(StatusCode.SHUTDOWN, "Shutdown"), PROTOCOL(StatusCode.PROTOCOL, "Protocol error"), BAD_DATA(StatusCode.BAD_DATA, "Received bad data"), UNDEFINED(StatusCode.UNDEFINED, "Undefined"), NO_CODE(StatusCode.NO_CODE, "No code present"), NO_CLOSE(StatusCode.NO_CLOSE, "Abnormal connection closed"), ABNORMAL(StatusCode.ABNORMAL, "Abnormal connection closed"), BAD_PAYLOAD(StatusCode.BAD_PAYLOAD, "Not consistent message"), POLICY_VIOLATION(StatusCode.POLICY_VIOLATION, "Received message violates policy"), MESSAGE_TOO_LARGE(StatusCode.MESSAGE_TOO_LARGE, "Message too big"), REQUIRED_EXTENSION(StatusCode.REQUIRED_EXTENSION, "Required extension not sent"), SERVER_ERROR(StatusCode.SERVER_ERROR, "Server error"), SERVICE_RESTART(StatusCode.SERVICE_RESTART, "Server restart"), TRY_AGAIN_LATER(StatusCode.TRY_AGAIN_LATER, "Server overload"), FAILED_TLS_HANDSHAKE(StatusCode.POLICY_VIOLATION, "Failure handshake");

        private final int statusCode;
        private final String message;

        NiceStatusCode(final int statusCode, final String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        public static String getMessage(final int statusCode) {
            final List<NiceStatusCode> codes = Arrays.stream(values()).filter(niceStatusCode -> niceStatusCode.statusCode == statusCode).toList();
            if (!codes.isEmpty()) {
                return codes.get(0).toString();
            } else {
                log.error("No matching status code found for {}", statusCode);
                return String.valueOf(statusCode);
            }
        }

        @Override
        public String toString() {
            return message + " (" + statusCode + ")";
        }

    }

    public interface Listener {

        void onOutgoingPonyFrame(ServerToClientModel model, Object value);

        void onOutgoingPonyFramesBytes(int bytes);

        void onOutgoingWebSocketFrame(int headerLength, int payloadLength);

        void onIncomingText(String text);

        void onIncomingWebSocketFrame(int headerLength, int payloadLength);

    }
}
