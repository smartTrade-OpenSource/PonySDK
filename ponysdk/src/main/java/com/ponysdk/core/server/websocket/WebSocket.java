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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.concurrent.UIContext;
import com.ponysdk.core.ui.basic.PObject;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.core.Extension;
import org.eclipse.jetty.websocket.core.ExtensionStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class WebSocket implements Session.Listener, WebsocketEncoder {

    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    private static final String MSG_RECEIVED = "Message received from terminal : UIContext #{} on {} : {}";
    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);
    private static final Logger loggerIn = LoggerFactory.getLogger("WebSocket-IN");
    private static final Logger loggerOut = LoggerFactory.getLogger("WebSocket-OUT");

    private JettyServerUpgradeRequest request;
    private WebsocketMonitor monitor;
    private WebSocketPusher websocketPusher;
    private ApplicationManager applicationManager;

    private Session session;
    private UIContext uiContext;
    private Listener listener;

    private long lastSentPing;

    private final long ID = ID_GENERATOR.incrementAndGet();

    public long getID() {
        return ID;
    }

    @Override
    public void onWebSocketOpen(final Session session) {
        try {
            log.info("New connection, creating new UIContext for the session {}", session);
            this.session = session;
            websocketPusher = new WebSocketPusher(session, 1 << 20, 1 << 12, TimeUnit.SECONDS.toMillis(60));
            uiContext = new UIContext(this, applicationManager, request);//TODO provider / factory for UIContext and Application

            applicationManager.startUIContext(uiContext);

            log.info("UIContext #{} created for the session {}", uiContext, session);
        } catch (final Exception e) {
            log.error("Cannot create UIContext", e);
        }
    }

    @Override
    public void onWebSocketError(final Throwable throwable) {
        log.error("WebSocket Error on UIContext #{}", uiContext.getID(), throwable);
        uiContext.onDestroy();
    }

    @Override
    public void onWebSocketClose(final int statusCode, final String reason) {
        log.info("WebSocket closed on UIContext #{} : {}, reason : {}", uiContext.getID(), NiceStatusCode.getMessage(statusCode), Objects.requireNonNullElse(reason, ""));
        uiContext.onDestroy();
    }

    @Override
    public void onWebSocketText(final String message) {
        if (this.listener != null) listener.onIncomingText(message);
        if (isAlive()) {
            try {
                uiContext.onMessageReceived();
                if (monitor != null) monitor.onMessageReceived(WebSocket.this, message);

                final JsonObject jsonObject;
                try (final JsonReader reader = uiContext.getJsonProvider().createReader(new StringReader(message))) {
                    jsonObject = reader.readObject();
                }

                if (jsonObject.containsKey(ClientToServerModel.HEARTBEAT_REQUEST.toStringValue())) {
                    sendHeartbeat();
                } else if (jsonObject.containsKey(ClientToServerModel.TERMINAL_LATENCY.toStringValue())) {
                    processRoundTripLatency(jsonObject);
                } else if (jsonObject.containsKey(ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue())) {
                    processInstructions(jsonObject);
                } else if (jsonObject.containsKey(ClientToServerModel.ERROR_MSG.toStringValue())) {
                    processTerminalLog(jsonObject, ClientToServerModel.ERROR_MSG);
                } else if (jsonObject.containsKey(ClientToServerModel.WARN_MSG.toStringValue())) {
                    processTerminalLog(jsonObject, ClientToServerModel.WARN_MSG);
                } else if (jsonObject.containsKey(ClientToServerModel.INFO_MSG.toStringValue())) {
                    processTerminalLog(jsonObject, ClientToServerModel.INFO_MSG);
                } else {
                    log.error("Unknown message from terminal #{} : {}", uiContext.getID(), message);
                }

                if (monitor != null) monitor.onMessageProcessed(this, message);
            } catch (final Throwable e) {
                log.error("Cannot process message from terminal  #{} : {}", uiContext.getID(), message, e);
            } finally {
                if (monitor != null) monitor.onMessageUnprocessed(this, message);
            }
        } else {
            log.info("UI Context #{} is destroyed, message dropped from terminal : {}", uiContext != null ? uiContext.getID() : -1, message);
        }
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
    }

    private void processRoundTripLatency(final JsonObject jsonObject) {
        final long roundTripLatency = TimeUnit.MILLISECONDS.convert(System.nanoTime() - lastSentPing, TimeUnit.NANOSECONDS);
        log.trace("RoundTrip measurement : {} ms from terminal #{}", roundTripLatency, uiContext.getID());
        uiContext.addRoundtripLatencyValue(roundTripLatency);

        final long terminalLatency = jsonObject.getJsonNumber(ClientToServerModel.TERMINAL_LATENCY.toStringValue()).longValue();
        log.trace("Terminal measurement : {} ms from terminal #{}", terminalLatency, uiContext.getID());
        uiContext.addTerminalLatencyValue(terminalLatency);

        final long networkLatency = roundTripLatency - terminalLatency;
        log.trace("Network measurement : {} ms from terminal #{}", networkLatency, uiContext.getID());
        uiContext.addNetworkLatencyValue(networkLatency);
    }

    private void processInstructions(final JsonObject jsonObject) {
        final String applicationInstructions = ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue();
        loggerIn.trace("UIContext #{} : {}", this.uiContext.getID(), jsonObject);
        uiContext.forceExecute(() -> {
            final JsonArray appInstructions = jsonObject.getJsonArray(applicationInstructions);
            for (int i = 0; i < appInstructions.size(); i++) {
                uiContext.fireClientData(appInstructions.getJsonObject(i));
            }
        });
    }

    private void processTerminalLog(final JsonObject json, final ClientToServerModel level) {
        final String message = json.getJsonString(level.toStringValue()).getString();
        String objectInformation = "";

        if (json.containsKey(ClientToServerModel.OBJECT_ID.toStringValue())) {
            final PObject object = uiContext.getObject(json.getJsonNumber(ClientToServerModel.OBJECT_ID.toStringValue()).intValue());
            objectInformation = object == null ? "NA" : object.toString();
        }

        switch (level) {
            case INFO_MSG:
                if (log.isInfoEnabled()) log.info(MSG_RECEIVED, uiContext.getID(), objectInformation, message);
                break;
            case WARN_MSG:
                if (log.isWarnEnabled()) log.warn(MSG_RECEIVED, uiContext.getID(), objectInformation, message);
                break;
            case ERROR_MSG:
                if (log.isErrorEnabled()) log.error(MSG_RECEIVED, uiContext.getID(), objectInformation, message);
                break;
            default:
                log.error("Unknown log level during terminal log processing : {}", level);
        }
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
            uiContext.onDestroy();
        }
    }

    public void close() {
        if (isSessionOpen()) {
            final UIContext context = this.uiContext;
            log.info("Closing websocket programmatically for UIContext #{}", context == null ? null : context.getID());
            session.close();
        }
    }

    public void disconnect() {
        if (isSessionOpen()) {
            final UIContext context = this.uiContext;
            log.info("Disconnecting websocket programmatically for UIContext #{}", context == null ? null : context.getID());
            session.disconnect();
        }
    }

    private boolean isAlive() {
        return uiContext != null && uiContext.isAlive();
    }

    private boolean isSessionOpen() {
        return session != null && session.isOpen();
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
            if (loggerOut.isTraceEnabled())
                loggerOut.trace("UIContext #{} : {} {}", this.uiContext.getID(), model, value);
            websocketPusher.encode(model, value);
            if (listener != null) listener.onOutgoingPonyFrame(model, value);
        } catch (final IOException e) {
            log.error("Can't write on the websocket for UIContext #{}, so we destroy the application", uiContext.getID(), e);
            uiContext.destroy();
        }
    }

    public JettyServerUpgradeRequest getRequest() {
        return request;
    }

    public void setRequest(final JettyServerUpgradeRequest request) {
        this.request = request;
    }

    public void setApplicationManager(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    public void setMonitor(final WebsocketMonitor monitor) {
        this.monitor = monitor;
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
