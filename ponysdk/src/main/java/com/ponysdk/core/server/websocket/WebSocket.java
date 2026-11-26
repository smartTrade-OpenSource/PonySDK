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

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.common.extensions.ExtensionStack;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.context.CommunicationSanityChecker;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.ui.basic.PObject;

public class WebSocket implements WebSocketListener, WebsocketEncoder {

    private static final String MSG_RECEIVED = "Message received from terminal : UIContext #{} on {} : {}";
    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);
    private static final Logger loggerIn = LoggerFactory.getLogger("WebSocket-IN");
    private static final Logger loggerOut = LoggerFactory.getLogger("WebSocket-OUT");

    /** Close code sent to a terminal whose context cannot be resumed: it reloads its page. */
    public static final int CLOSE_RESUME_REFUSED = 4001;

    private static final AtomicBoolean CLIENT_HEARTBEAT_WARNED = new AtomicBoolean();

    private static final String KEY_RECONNECT_UI_CONTEXT_ID = ClientToServerModel.RECONNECT_UI_CONTEXT_ID.toStringValue();
    private static final String KEY_RECONNECT_OFFSET = ClientToServerModel.RECONNECT_OFFSET.toStringValue();
    private static final String KEY_RECEIVED_OFFSET = ClientToServerModel.RECEIVED_OFFSET.toStringValue();

    private ServletUpgradeRequest request;
    private WebsocketMonitor monitor;
    private WebSocketPusher websocketPusher;
    private ApplicationManager applicationManager;

    private TxnContext context;
    private Session session;
    private UIContext uiContext;
    private Listener listener;

    public WebSocket() {
    }

    @Override
    public void onWebSocketConnect(final Session session) {
        try {
            if (!session.isOpen()) throw new IllegalStateException("Session already closed");
            this.session = session;

            // The page's own query is part of this URL too: a parameter of the same name in it is ignored while
            // reconnection is off, and the terminal's, appended last, wins otherwise
            final String resumedID = applicationManager.getConfiguration().getReconnectionTimeoutMs() > 0
                    ? getParameter(KEY_RECONNECT_UI_CONTEXT_ID)
                    : null;
            if (resumedID != null) {
                resumeOrRefuse(resumedID, getParameter(KEY_RECONNECT_OFFSET));
                return;
            }

            this.websocketPusher = newPusher(0);
            uiContext = new UIContext(this, context, applicationManager.getConfiguration(), request);
            log.info("Creating a new {}", uiContext);

            final CommunicationSanityChecker communicationSanityChecker = new CommunicationSanityChecker(uiContext);
            uiContext.setCommunicationSanityChecker(communicationSanityChecker);
            context.registerUIContext(uiContext);

            uiContext.acquire();
            try {
                beginObject();
                final ApplicationConfiguration configuration = uiContext.getConfiguration();
                final boolean enableClientToServerHeartBeat = configuration.isEnableClientToServerHeartBeat();
                final TimeUnit heartBeatPeriodTimeUnit = configuration.getHeartBeatPeriodTimeUnit();
                final int heartBeatPeriod = enableClientToServerHeartBeat
                        ? (int) heartBeatPeriodTimeUnit.toSeconds(configuration.getHeartBeatPeriod())
                        : 0;

                encode(ServerToClientModel.CREATE_CONTEXT, uiContext.getID()); // TODO nciaravola integer ?
                encode(ServerToClientModel.OPTION_FORMFIELD_TABULATION, configuration.isTabindexOnlyFormField());
                encode(ServerToClientModel.HEARTBEAT_PERIOD, heartBeatPeriod);
                if (configuration.getReconnectionTimeoutMs() > 0)
                    encode(ServerToClientModel.RECONNECTION_TIMEOUT, (int) Math.min(Integer.MAX_VALUE, configuration.getReconnectionTimeoutMs()));
                endObject();
                if (isAlive()) flush0();
            } catch (final Throwable e) {
                log.error("Cannot send server heart beat to client", e);
            } finally {
                uiContext.release();
            }

            applicationManager.startApplication(uiContext);
            communicationSanityChecker.start();
        } catch (final Exception e) {
            log.error("Cannot process WebSocket instructions", e);
        }
    }

    /**
     * 1K for max chunk size and 1M for total buffer size. Don't set max chunk size > 8K: with Jetty WebSocket
     * compression, the chunks are limited to 8K.
     * <p>
     * With transparent reconnection enabled, the bytes sent are kept until the terminal acknowledges them.
     *
     * @param startOffset the bytes the terminal has received from this context before this WebSocket
     */
    private WebSocketPusher newPusher(final long startOffset) {
        final ApplicationConfiguration configuration = applicationManager.getConfiguration();
        ReplayBuffer replayBuffer = null;
        if (configuration.getReconnectionTimeoutMs() > 0) {
            if (!configuration.isEnableClientToServerHeartBeat() && CLIENT_HEARTBEAT_WARNED.compareAndSet(false, true))
                log.warn("Transparent reconnection without the client heartbeat: nothing is acknowledged, each UIContext holds "
                        + "up to {} bytes of what it sent", configuration.getReplayBufferMaxBytes());
            replayBuffer = new ReplayBuffer(startOffset, Math.max(configuration.getReplayBufferMaxBytes(), ReplayBuffer.MIN_SIZE));
        }
        return new WebSocketPusher(session, 1 << 20, 1 << 13, TimeUnit.SECONDS.toMillis(60), replayBuffer);
    }

    /**
     * A terminal reconnects to its suspended context. It is resumed if it belongs to this HTTP session, is still
     * suspended, and the bytes the terminal has not received are still held; otherwise the WebSocket is closed with
     * {@link #CLOSE_RESUME_REFUSED} and the terminal reloads its page.
     */
    private void resume(final String uiContextID, final String offset) {
        final Application application = context.getApplication();
        UIContext suspended = null;
        String refusal = null;
        long receivedBytes = -1;
        try {
            if (applicationManager.getConfiguration().getReconnectionTimeoutMs() <= 0) refusal = "reconnection disabled";
            else if (application == null) refusal = "no application for this HTTP session";
            else if (offset == null) refusal = "no offset";
            else {
                receivedBytes = Long.parseLong(offset);
                suspended = application.getUIContext(Integer.parseInt(uiContextID));
                // Its terminal is back before this server saw the connection lost (a cut with no close, found only
                // by the heartbeat): the old WebSocket is dropped now
                if (suspended != null && suspended.isAlive() && !suspended.isSuspended()) suspended.onCommunicationLost();
                if (suspended == null) refusal = "no such context in this HTTP session";
                else if (!suspended.isSuspended()) refusal = "context not suspended";
            }
        } catch (final NumberFormatException e) {
            refusal = "malformed parameter";
        }

        byte[] unreceivedBytes = null;
        if (refusal == null) {
            unreceivedBytes = suspended.getUnreceivedBytes(receivedBytes);
            if (unreceivedBytes == null) refusal = "bytes from offset " + offset + " no longer held";
        }

        boolean failed = false;
        if (refusal == null) {
            this.uiContext = suspended;
            this.context = suspended.getTxnContext();
            this.websocketPusher = newPusher(receivedBytes);
            if (suspended.resume(this, unreceivedBytes)) return;
            refusal = "resume failed";
            failed = true;
        }

        log.info("Reconnection to UIContext #{} refused: {}", uiContextID, refusal);
        this.uiContext = null;
        // Told first, so that the terminal reloads: destroying the context closes its socket normally
        session.close(CLOSE_RESUME_REFUSED, "resume refused");
        // A context its terminal cannot come back to is destroyed now rather than at its timeout; one whose replay
        // failed is no longer suspended nor connected, and nothing else would end it
        if (suspended != null && (failed || suspended.isSuspended())) suspended.destroy();
    }

    /** The last value of the parameter: the terminal appends its own after the page's query. */
    private String getParameter(final String name) {
        final List<String> values = request.getParameterMap().get(name);
        return values != null && !values.isEmpty() ? values.get(values.size() - 1) : null;
    }

    /** {@link #resume(String, String)}, and a refusal whatever it throws, so that the terminal never waits. */
    private void resumeOrRefuse(final String uiContextID, final String offset) {
        try {
            resume(uiContextID, offset);
        } catch (final RuntimeException e) {
            log.error("Reconnection to UIContext #{} failed", uiContextID, e);
            final UIContext resumed = this.uiContext;
            this.uiContext = null;
            session.close(CLOSE_RESUME_REFUSED, "resume refused");
            if (resumed != null) resumed.destroy();
        }
    }

    @Override
    public void onWebSocketError(final Throwable throwable) {
        if (uiContext == null) return; // a refused reconnection
        log.error("WebSocket Error on UIContext #{}", uiContext.getID(), throwable);
        uiContext.onConnectionLost(this, StatusCode.SERVER_ERROR);
    }

    @Override
    public void onWebSocketClose(final int statusCode, final String reason) {
        if (uiContext == null) return; // a refused reconnection
        if (log.isInfoEnabled())
            log.info("WebSocket closed on UIContext #{} : {}, reason : {}", uiContext.getID(),
                    NiceStatusCode.getMessage(statusCode), Objects.requireNonNullElse(reason, ""));
        uiContext.onConnectionLost(this, statusCode);
    }

    /**
     * The bytes from the given offset this WebSocket has sent, or null if they are not all held (transparent
     * reconnection disabled, or released).
     */
    public byte[] getBytesFrom(final long offset) {
        final ReplayBuffer replayBuffer = websocketPusher != null ? websocketPusher.getReplayBuffer() : null;
        return replayBuffer != null ? replayBuffer.from(offset) : null;
    }

    /** Writes bytes already encoded, as they are: the ones a resumed terminal had not received. */
    public void putRaw(final byte[] bytes) throws IOException {
        websocketPusher.put(bytes);
    }

    /**
     * Receive from the terminal
     */

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

                if (jsonObject.containsKey(ClientToServerModel.HEARTBEAT_CLIENT.toStringValue())) {
                    if (jsonObject.containsKey(KEY_RECEIVED_OFFSET)) {
                        final ReplayBuffer replayBuffer = websocketPusher.getReplayBuffer();
                        if (replayBuffer != null) replayBuffer.acknowledge(jsonObject.getJsonNumber(KEY_RECEIVED_OFFSET).longValue());
                    }
                } else if (jsonObject.containsKey(ClientToServerModel.TERMINAL_LATENCY.toStringValue())) {
                    processRoundtripLatency(jsonObject);
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
            log.info("UI Context #{} is destroyed, message dropped from terminal : {}", uiContext != null ? uiContext.getID() : -1,
                    message);
        }
    }

    private void processRoundtripLatency(final JsonObject jsonObject) {
        final long lastSentPing = jsonObject.getJsonNumber(ClientToServerModel.TERMINAL_LATENCY.toStringValue()).longValue();

        final long roundtripLatency = TimeUnit.MILLISECONDS.convert(System.nanoTime() - lastSentPing, TimeUnit.NANOSECONDS);
        log.trace("Roundtrip measurement : {} ms from terminal #{}", roundtripLatency, uiContext.getID());
        uiContext.addRoundtripLatencyValue(roundtripLatency);
        uiContext.addNetworkLatencyValue(roundtripLatency);
    }

    private void processInstructions(final JsonObject jsonObject) {
        final String applicationInstructions = ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue();
        loggerIn.trace("UIContext #{} : {}", this.uiContext.getID(), jsonObject);
        uiContext.execute(() -> {
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
                if (log.isInfoEnabled())
                    log.info(MSG_RECEIVED, uiContext.getID(), objectInformation, message); 
                break;
            case WARN_MSG:
                if (log.isWarnEnabled())
                    log.warn(MSG_RECEIVED, uiContext.getID(), objectInformation, message);
                break;
            case ERROR_MSG:
                if (log.isErrorEnabled())
                    log.error(MSG_RECEIVED, uiContext.getID(), objectInformation, message);
                break;
            default:
                log.error("Unknown log level during terminal log processing : {}", level);
        }
    }

    /**
     * Receive from the terminal
     */
    @Override
    public void onWebSocketBinary(final byte[] payload, final int offset, final int len) {
        // Can't receive binary data from terminal (GWT limitation)
    }

    /**
     * Send round trip to the client
     */
    public void sendRoundTrip() {
        if (isAlive() && isSessionOpen()) {
            beginObject();
            encode(ServerToClientModel.ROUNDTRIP_LATENCY, System.nanoTime());
            endObject();
            flush0();
        }
    }

    public void flush() {
        if (isAlive() && isSessionOpen()) flush0();
    }

    void flush0() {
        try {
            websocketPusher.flush();
        } catch (final IOException e) {
            log.error("Can't write on the websocket for #{}", uiContext.getID(), e);
            uiContext.onConnectionLost(this, StatusCode.SERVER_ERROR);
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
            try {
                session.disconnect();
            } catch (final IOException e) {
                log.error("Unable to disconnect session for UIContext #{}", context == null ? null : context.getID(), e);
            }
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
        if (UIContext.get() == null) {
            log.warn("encode in websocket without current ui context acquired", new Exception());
            uiContext.acquire();
            try {
                encode(model, value);
            } finally {
                uiContext.release();
            }
            return;
        }

        try {
            if (loggerOut.isTraceEnabled())
                loggerOut.trace("UIContext #{} : {} {}", this.uiContext.getID(), model, value);
            websocketPusher.encode(model, value);
            if (listener != null) listener.onOutgoingPonyFrame(model, value);
        } catch (final IOException e) {
            log.error("Can't write on the websocket for UIContext #{}, so we destroy the application", uiContext.getID(), e);
            uiContext.onDestroy(StatusCode.SERVER_ERROR);
        }
    }

    private enum NiceStatusCode {

        NORMAL(StatusCode.NORMAL, "Normal closure"),
        SHUTDOWN(StatusCode.SHUTDOWN, "Shutdown"),
        PROTOCOL(StatusCode.PROTOCOL, "Protocol error"),
        BAD_DATA(StatusCode.BAD_DATA, "Received bad data"),
        UNDEFINED(StatusCode.UNDEFINED, "Undefined"),
        NO_CODE(StatusCode.NO_CODE, "No code present"),
        NO_CLOSE(StatusCode.NO_CLOSE, "Abnormal connection closed"),
        ABNORMAL(StatusCode.ABNORMAL, "Abnormal connection closed"),
        BAD_PAYLOAD(StatusCode.BAD_PAYLOAD, "Not consistent message"),
        POLICY_VIOLATION(StatusCode.POLICY_VIOLATION, "Received message violates policy"),
        MESSAGE_TOO_LARGE(StatusCode.MESSAGE_TOO_LARGE, "Message too big"),
        REQUIRED_EXTENSION(StatusCode.REQUIRED_EXTENSION, "Required extension not sent"),
        SERVER_ERROR(StatusCode.SERVER_ERROR, "Server error"),
        SERVICE_RESTART(StatusCode.SERVICE_RESTART, "Server restart"),
        TRY_AGAIN_LATER(StatusCode.TRY_AGAIN_LATER, "Server overload"),
        FAILED_TLS_HANDSHAKE(StatusCode.POLICY_VIOLATION, "Failure handshake");

        private final int statusCode;
        private final String message;

        NiceStatusCode(final int statusCode, final String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        public static String getMessage(final int statusCode) {
            final List<NiceStatusCode> codes = Arrays.stream(values())
                    .filter(niceStatusCode -> niceStatusCode.statusCode == statusCode).collect(Collectors.toList());
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

    UIContext getUIContext() {
        return uiContext;
    }

    public ServletUpgradeRequest getRequest() {
        return request;
    }

    public void setRequest(final ServletUpgradeRequest request) {
        this.request = request;
    }

    public void setApplicationManager(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    public void setMonitor(final WebsocketMonitor monitor) {
        this.monitor = monitor;
    }

    public void setContext(final TxnContext context) {
        this.context = context;
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
        final PonyPerMessageDeflateExtension extension = extensionStack.getBean(PonyPerMessageDeflateExtension.class);
        if (extension == null) {
            log.warn("Missing PonyPerMessageDeflateExtension from Extension Stack for {}", uiContext);
            return;
        }
        extension.setWebSocketListener(listener);
    }

    public interface Listener {

        void onOutgoingPonyFrame(ServerToClientModel model, Object value);

        void onOutgoingPonyFramesBytes(int bytes);

        void onOutgoingWebSocketFrame(int headerLength, int payloadLength);

        void onIncomingText(String text);

        void onIncomingWebSocketFrame(int headerLength, int payloadLength);

    }
}
