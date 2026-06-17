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
import com.ponysdk.core.server.metrics.PonySDKMetrics;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.context.CommunicationSanityChecker;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.ui.basic.PObject;
import org.eclipse.jetty.ee11.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class WebSocket implements Session.Listener.AutoDemanding, WebsocketEncoder {

    private static final String MSG_RECEIVED = "Message received from terminal : UIContext #{} on {} : {}";
    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);
    private static final Logger loggerIn = LoggerFactory.getLogger("WebSocket-IN");
    private static final Logger loggerOut = LoggerFactory.getLogger("WebSocket-OUT");

    private static final String KEY_HEARTBEAT = ClientToServerModel.HEARTBEAT_REQUEST.toStringValue();
    private static final String KEY_LATENCY = ClientToServerModel.TERMINAL_LATENCY.toStringValue();
    private static final String KEY_INSTRUCTIONS = ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue();
    private static final String KEY_ERROR = ClientToServerModel.ERROR_MSG.toStringValue();
    private static final String KEY_WARN = ClientToServerModel.WARN_MSG.toStringValue();
    private static final String KEY_INFO = ClientToServerModel.INFO_MSG.toStringValue();

    /** Shared buffer pool for all WebSocket sessions. 32KB buffers, max 128 pooled. */
    private static final ByteBufferPool BUFFER_POOL = new ByteBufferPool(1 << 15, 128);

    /** Max time to wait, on reconnect, for the old context to enter the suspended state (race window). */
    private static final long RECONNECT_SUSPEND_WAIT_MS = 500;
    /** Poll interval while waiting for the suspended state. */
    private static final long RECONNECT_SUSPEND_POLL_MS = 20;

    private JettyServerUpgradeRequest request;
    private Map<String, List<String>> cachedParameterMap;
    private String cachedUserAgent;
    private HttpSession cachedHttpSession;
    private WebsocketMonitor monitor;
    private WebSocketPusher websocketPusher;
    private ApplicationManager applicationManager;
    private PonySDKMetrics metrics;

    private TxnContext context;
    private Session session;
    private UIContext uiContext;
    private Listener listener;
    private int reconnectContextId = -1; // -1 = normal connect, ≥0 = reconnect attempt

    public void setReconnectContextId(final int id) {
        this.reconnectContextId = id;
    }

    public WebSocket() {
    }

    @Override
    public void onWebSocketOpen(final Session session) {
        try {
            if (!session.isOpen()) throw new IllegalStateException("Session already closed");
            this.session = session;

            // #4 Slow-consumer backpressure: the pusher keeps a single send in flight and waits
            // up to this timeout for the previous send before disconnecting a stuck client.
            final ApplicationConfiguration cfg = applicationManager != null ? applicationManager.getConfiguration() : null;
            final long sendTimeoutMs = cfg != null && cfg.getWsSendTimeoutMs() > 0
                    ? cfg.getWsSendTimeoutMs()
                    : TimeUnit.SECONDS.toMillis(60);
            this.websocketPusher = new WebSocketPusher(session, 1 << 15, sendTimeoutMs, BUFFER_POOL);

            // Transparent reconnection: resume a suspended UIContext instead of creating a new one
            if (reconnectContextId >= 0) {
                final Application application = context.getApplication();
                UIContext suspended = application != null ? application.getUIContext(reconnectContextId) : null;
                // Race condition: client reconnects before onWebSocketClose has been processed
                // server-side (common on fast local networks). Wait up to 500ms for the context
                // to enter suspended state before falling back to creating a new one.
                if (suspended != null && suspended.isAlive() && !suspended.isSuspended()) {
                    final long deadline = System.currentTimeMillis() + RECONNECT_SUSPEND_WAIT_MS;
                    // Wait for the suspend to land, but stop early if the context dies meanwhile.
                    while (suspended.isAlive() && !suspended.isSuspended() && System.currentTimeMillis() < deadline) {
                        try { Thread.sleep(RECONNECT_SUSPEND_POLL_MS); } catch (final InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                    }
                    suspended = application.getUIContext(reconnectContextId); // re-fetch after wait
                }
                if (suspended != null && suspended.isSuspended()) {
                    this.uiContext = suspended;
                    if (suspended.getStringDictionary() != null) {
                        websocketPusher.setStringDictionary(suspended.getStringDictionary());
                    }
                    log.info("Resuming {} on new WebSocket", suspended);
                    suspended.resume(this);
                    return;
                } else {
                    log.warn("Reconnect requested for UIContext #{} but it is not suspended — creating new context", reconnectContextId);
                }
            }

            uiContext = new UIContext(this, context, applicationManager.getConfiguration(), request,
                    applicationManager.getSharedDictionaryProvider());
            log.info("Creating a new {}", uiContext);

            if (uiContext.getStringDictionary() != null) {
                websocketPusher.setStringDictionary(uiContext.getStringDictionary());
            }

            final CommunicationSanityChecker communicationSanityChecker = new CommunicationSanityChecker(uiContext);
            context.registerUIContext(uiContext);

            boolean initFlushOk = false;
            uiContext.acquire();
            try {
                beginObject();
                final ApplicationConfiguration configuration = uiContext.getConfiguration();
                final boolean enableClientToServerHeartBeat = configuration.isEnableClientToServerHeartBeat();
                final TimeUnit heartBeatPeriodTimeUnit = configuration.getHeartBeatPeriodTimeUnit();
                final int heartBeatPeriod = enableClientToServerHeartBeat
                        ? (int) heartBeatPeriodTimeUnit.toSeconds(configuration.getHeartBeatPeriod())
                        : 0;

                encode(ServerToClientModel.CREATE_CONTEXT, uiContext.getID());
                encode(ServerToClientModel.OPTION_FORMFIELD_TABULATION, configuration.isTabindexOnlyFormField());
                encode(ServerToClientModel.HEARTBEAT_PERIOD, heartBeatPeriod);
                endObject();
                if (isAlive()) flush0();
                initFlushOk = true;
            } catch (final Throwable e) {
                // Socket already closed before we could send CREATE_CONTEXT — destroy the orphan
                // immediately so it never enters suspended state and triggers a reconnect loop.
                log.warn("Cannot send CREATE_CONTEXT to UIContext #{} (socket already closed) — destroying orphan", uiContext.getID(), e);
                uiContext.destroy();
            } finally {
                uiContext.release();
            }

            if (!initFlushOk) return;

            applicationManager.startApplication(uiContext);
            if (metrics != null) {
                metrics.onContextCreated();
                uiContext.setMetrics(metrics);
                uiContext.addContextDestroyListener(metrics.contextDestroyListener());
                // Wire bytes-sent tracking via the pusher listener
                websocketPusher.setWebSocketListener(new WebSocket.Listener() {
                    @Override public void onOutgoingPonyFrame(final ServerToClientModel model, final Object value) {}
                    @Override public void onOutgoingPonyFramesBytes(final int bytes) { metrics.recordBytesSent(bytes); }
                    @Override public void onOutgoingWebSocketFrame(final int headerLength, final int payloadLength) {}
                    @Override public void onIncomingText(final String text) {}
                    @Override public void onIncomingWebSocketFrame(final int headerLength, final int payloadLength) {}
                });
            }
            communicationSanityChecker.start();
        } catch (final Exception e) {
            log.error("Cannot process WebSocket instructions", e);
        }
    }

    @Override
    public void onWebSocketError(final Throwable throwable) {
        if (uiContext == null) {
            log.error("WebSocket Error before UIContext initialization", throwable);
            return;
        }
        log.error("WebSocket Error on UIContext #{}", uiContext.getID(), throwable);
        // During suspension, the old WebSocket may still fire errors (e.g. broken pipe).
        // Do NOT destroy the UIContext — it is waiting for a new WebSocket to resume.
        if (!uiContext.isSuspended()) {
            uiContext.onDestroy();
        }
    }

    @Override
    public void onWebSocketClose(final int statusCode, final String reason, final Callback callback) {
        try {
            if (uiContext == null) {
                log.info("WebSocket closed before UIContext initialization: {}, reason: {}",
                        NiceStatusCode.getMessage(statusCode), Objects.requireNonNullElse(reason, ""));
                return;
            }
            if (log.isInfoEnabled())
                log.info("WebSocket closed on UIContext #{} : {}, reason : {}", uiContext.getID(),
                        NiceStatusCode.getMessage(statusCode), Objects.requireNonNullElse(reason, ""));
            final ApplicationConfiguration config = applicationManager.getConfiguration();
            if (config.getReconnectionListener() != null) {
                try {
                    config.getReconnectionListener().onConnectionLost(uiContext);
                } catch (final Throwable e) {
                    log.error("ReconnectionListener threw an exception for UIContext #{}", uiContext.getID(), e);
                }
            }
            if (config.getReconnectionTimeoutMs() > 0) {
                // Transparent reconnection enabled — suspend instead of destroy
                uiContext.suspend(config.getReconnectionTimeoutMs());
            } else {
                uiContext.onDestroy();
            }
        } finally {
            callback.succeed();
        }
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

                if (jsonObject.containsKey(KEY_HEARTBEAT)) {
                    //sendHeartbeat();
                } else if (jsonObject.containsKey(KEY_LATENCY)) {
                    processRoundtripLatency(jsonObject);
                } else if (jsonObject.containsKey(KEY_INSTRUCTIONS)) {
                    processInstructions(jsonObject);
                } else if (jsonObject.containsKey(KEY_ERROR)) {
                    processTerminalLog(jsonObject, ClientToServerModel.ERROR_MSG);
                } else if (jsonObject.containsKey(KEY_WARN)) {
                    processTerminalLog(jsonObject, ClientToServerModel.WARN_MSG);
                } else if (jsonObject.containsKey(KEY_INFO)) {
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
        final long lastSentPing = jsonObject.getJsonNumber(KEY_LATENCY).longValue();

        final long roundtripLatency = TimeUnit.MILLISECONDS.convert(System.nanoTime() - lastSentPing, TimeUnit.NANOSECONDS);
        log.trace("Roundtrip measurement : {} ms from terminal #{}", roundtripLatency, uiContext.getID());
        uiContext.addRoundtripLatencyValue(roundtripLatency);
        uiContext.addNetworkLatencyValue(roundtripLatency);
        if (metrics != null) metrics.recordRoundtrip(roundtripLatency);
    }

    private void processInstructions(final JsonObject jsonObject) {
        loggerIn.trace("UIContext #{} : {}", this.uiContext.getID(), jsonObject);
        uiContext.execute(() -> {
            final JsonArray appInstructions = jsonObject.getJsonArray(KEY_INSTRUCTIONS);
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
            case INFO_MSG -> { if (log.isInfoEnabled()) log.info(MSG_RECEIVED, uiContext.getID(), objectInformation, message); }
            case WARN_MSG -> { if (log.isWarnEnabled()) log.warn(MSG_RECEIVED, uiContext.getID(), objectInformation, message); }
            case ERROR_MSG -> { if (log.isErrorEnabled()) log.error(MSG_RECEIVED, uiContext.getID(), objectInformation, message); }
            default -> log.error("Unknown log level during terminal log processing : {}", level);
        }
    }

    @Override
    public void onWebSocketBinary(final ByteBuffer payload, final Callback callback) {
        callback.succeed();
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
            if (uiContext != null) uiContext.onMessageSent();
        } catch (final IOException e) {
            log.error("Can't write on the websocket for #{}, so we destroy the application", uiContext.getID(), e);
            uiContext.onDestroy();
        }
    }

    public void close() {
        if (isSessionOpen()) {
            final UIContext context = this.uiContext;
            log.info("Closing websocket programmatically for UIContext #{}", context == null ? null : context.getID());
            session.close(StatusCode.NORMAL, "close", Callback.NOOP);
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
            uiContext.destroy();
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

        private static final Map<Integer, NiceStatusCode> BY_CODE;
        static {
            final Map<Integer, NiceStatusCode> map = new java.util.HashMap<>();
            for (final NiceStatusCode nsc : values()) {
                map.putIfAbsent(nsc.statusCode, nsc);
            }
            BY_CODE = Map.copyOf(map);
        }

        private final int statusCode;
        private final String message;

        NiceStatusCode(final int statusCode, final String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        public static String getMessage(final int statusCode) {
            final NiceStatusCode nsc = BY_CODE.get(statusCode);
            if (nsc != null) {
                return nsc.toString();
            }
            log.error("No matching status code found for {}", statusCode);
            return String.valueOf(statusCode);
        }

        @Override
        public String toString() {
            return message + " (" + statusCode + ")";
        }

    }

    public JettyServerUpgradeRequest getRequest() {
        return request;
    }

    public void setRequest(final JettyServerUpgradeRequest request) {
        this.request = request;
        // Snapshot request data NOW — Jetty 12 recycles the underlying HTTP request
        // after the WebSocket upgrade, so these will be unavailable in onWebSocketOpen.
        this.cachedParameterMap = request.getParameterMap();
        this.cachedUserAgent = request.getHeader("User-Agent");
        this.cachedHttpSession = request.getHttpServletRequest().getSession();
    }

    public Map<String, List<String>> getCachedParameterMap() {
        return cachedParameterMap;
    }

    public String getCachedUserAgent() {
        return cachedUserAgent;
    }

    public HttpSession getCachedHttpSession() {
        return cachedHttpSession;
    }

    public void setApplicationManager(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    public void setMonitor(final WebsocketMonitor monitor) {
        this.monitor = monitor;
    }

    public void setMetrics(final PonySDKMetrics metrics) {
        this.metrics = metrics;
    }

    public void setContext(final TxnContext context) {
        this.context = context;
    }

    public void setListener(final Listener listener) {
        this.listener = listener;
        this.websocketPusher.setWebSocketListener(listener);
        // Frame-level compression monitoring disabled — PonyPerMessageDeflateExtension removed in Jetty 12 migration
        // permessage-deflate is handled natively by Jetty 12
    }

    public interface Listener {

        void onOutgoingPonyFrame(ServerToClientModel model, Object value);

        void onOutgoingPonyFramesBytes(int bytes);

        void onOutgoingWebSocketFrame(int headerLength, int payloadLength);

        void onIncomingText(String text);

        void onIncomingWebSocketFrame(int headerLength, int payloadLength);

    }
}
