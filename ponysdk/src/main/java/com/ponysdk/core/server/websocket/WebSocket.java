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

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.context.CommunicationSanityChecker;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.ui.basic.PObject;

public class WebSocket implements WebSocketListener, WebsocketEncoder {

    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);

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
        this.session = session;

        // 1K for max chunk size and 1M for total buffer size
        // Don't set max chunk size > 8K because when using Jetty Websocket compression, the chunks are limited to 8K
        this.websocketPusher = new WebSocketPusher(session, 1 << 20, 1 << 12, TimeUnit.SECONDS.toMillis(60));

        try {
            uiContext = new UIContext(this, context, applicationManager.getConfiguration(), request);
            log.info("Creating a new {}", uiContext);

            final CommunicationSanityChecker communicationSanityChecker = new CommunicationSanityChecker(uiContext);
            context.registerUIContext(uiContext);

            uiContext.acquire();
            try {
                beginObject();
                encode(ServerToClientModel.CREATE_CONTEXT, uiContext.getID()); // TODO nciaravola integer ?
                encode(ServerToClientModel.OPTION_FORMFIELD_TABULATION, uiContext.getConfiguration().isTabindexOnlyFormField());
                endObject();
                if (isAlive() && isSessionOpen()) flush0();
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

    @Override
    public void onWebSocketError(final Throwable throwable) {
        log.error("WebSocket Error", throwable);
        uiContext.onDestroy();
    }

    @Override
    public void onWebSocketClose(final int statusCode, final String reason) {
        if (log.isInfoEnabled()) log.info("WebSocket closed on UIContext #{} : {}, reason : {}", uiContext.getID(),
            NiceStatusCode.getMessage(statusCode), reason != null ? reason : "");
        uiContext.onDestroy();
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

                if (ClientToServerModel.HEARTBEAT.toStringValue().equals(message)) {
                    processHeartbeat();
                } else {
                    final JsonObject jsonObject;

                    try (final JsonReader reader = uiContext.getJsonProvider().createReader(new StringReader(message))) {
                        jsonObject = reader.readObject();
                    }

                    if (jsonObject.containsKey(ClientToServerModel.PING_SERVER.toStringValue())) {
                        processPing(jsonObject);
                    } else if (jsonObject.containsKey(ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue())) {
                        processInstructions(jsonObject);
                    } else if (jsonObject.containsKey(ClientToServerModel.ERROR_MSG.toStringValue())) {
                        processTerminalLog(jsonObject, ClientToServerModel.ERROR_MSG);
                    } else if (jsonObject.containsKey(ClientToServerModel.WARN_MSG.toStringValue())) {
                        processTerminalLog(jsonObject, ClientToServerModel.WARN_MSG);
                    } else if (jsonObject.containsKey(ClientToServerModel.INFO_MSG.toStringValue())) {
                        processTerminalLog(jsonObject, ClientToServerModel.INFO_MSG);
                    } else {
                        log.error("Unknow message from terminal #{} : {}", uiContext.getID(), message);
                    }
                }
                if (monitor != null) monitor.onMessageProcessed(this, message);
            } catch (final Throwable e) {
                log.error("Cannot process message from terminal  #" + uiContext.getID() + " : " + message, e);
            } finally {
                if (monitor != null) monitor.onMessageUnprocessed(this, message);
            }
        } else {
            log.info("UI Context #{} is destroyed, message dropped from terminal : {}", uiContext.getID(), message);
        }
    }

    private void processHeartbeat() {
        if (log.isDebugEnabled()) log.debug("Heartbeat received from terminal #{}", uiContext.getID());
    }

    private void processPing(final JsonObject jsonObject) {
        final long start = jsonObject.getJsonNumber(ClientToServerModel.PING_SERVER.toStringValue()).longValue();
        final long end = System.currentTimeMillis();
        if (log.isDebugEnabled()) log.debug("Ping measurement : {} ms from terminal #{}", end - start, uiContext.getID());
        uiContext.addPingValue(end - start);
    }

    private void processInstructions(final JsonObject jsonObject) {
        final String applicationInstructions = ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue();
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
                log.info("Message received from terminal : UIContext #{} on {} : {}", uiContext.getID(), objectInformation, message);
                break;
            case WARN_MSG:
                log.warn("Message received from terminal : UIContext #{} on {} : {}", uiContext.getID(), objectInformation, message);
                break;
            case ERROR_MSG:
                log.error("Message received from terminal : UIContext #{} on {} : {}", uiContext.getID(), objectInformation, message);
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
     * Send heart beat to the client
     */
    public void sendHeartBeat() {
        if (isAlive() && isSessionOpen()) {
            beginObject();
            encode(ServerToClientModel.HEARTBEAT, null);
            endObject();
            flush0();
        }
    }

    /**
     * Send round trip to the client
     */
    public void sendRoundTrip() {
        if (isAlive() && isSessionOpen()) {
            beginObject();
            encode(ServerToClientModel.PING_SERVER, System.currentTimeMillis());
            endObject();
            flush0();
        }
    }

    public void flush() {
        if (isAlive() && isSessionOpen()) flush0();
    }

    private void flush0() {
        websocketPusher.flush();
    }

    public void close() {
        if (isSessionOpen()) {
            log.info("Closing websocket programmatically");
            session.close();
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
        websocketPusher.encode(model, value);
        if (listener != null) listener.onOutgoingFrame(model, value);
    }

    private static enum NiceStatusCode {

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

        private NiceStatusCode(final int statusCode, final String message) {
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
        if (this.websocketPusher != null) this.websocketPusher.setWebSocketListener(listener);
    }

    public static interface Listener {

        void onOutgoingFrame(ServerToClientModel model, Object value);

        void onOutgoingBytes(int bytes);

        void onIncomingText(String text);
    }
}
