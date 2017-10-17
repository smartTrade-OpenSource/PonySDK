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

package com.ponysdk.core.server.servlet;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.AbstractApplicationManager;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.useragent.UserAgent;

public class WebSocket implements WebSocketListener, WebsocketEncoder {

    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);

    private final ServletUpgradeRequest request;
    private final WebsocketMonitor monitor;
    private WebSocketPusher websocketPusher;
    private final AbstractApplicationManager applicationManager;

    private TxnContext context;
    private Session session;

    WebSocket(final ServletUpgradeRequest request, final WebsocketMonitor monitor,
            final AbstractApplicationManager applicationManager) {
        this.request = request;
        this.monitor = monitor;
        this.applicationManager = applicationManager;
    }

    @Override
    public void onWebSocketConnect(final Session session) {
        final HttpSession httpSession = request.getSession();
        final String applicationId = httpSession.getId();
        final String userAgent = request.getHeader("User-Agent");
        log.info("WebSocket connected from {}, sessionID={}, userAgent={}", session.getRemoteAddress(), applicationId, userAgent);

        this.session = session;
        // 1K for max chunk size and 1M for total buffer size
        // Don't set max chunk size > 8K because when using Jetty Websocket compression, the chunks are limited to 8K
        this.websocketPusher = new WebSocketPusher(session, 1 << 20, 1 << 12, TimeUnit.SECONDS.toMillis(60));
        this.context = new TxnContext(this);

        Application application = SessionManager.get().getApplication(applicationId);
        if (application == null) {
            application = new Application(applicationId, httpSession, applicationManager.getOptions(),
                UserAgent.parseUserAgentString(userAgent));
            SessionManager.get().registerApplication(application);
        }

        context.setApplication(application);

        try {
            final UIContext uiContext = new UIContext(context);
            final CommunicationSanityChecker communicationSanityChecker = new CommunicationSanityChecker(uiContext);
            log.info("Creating a new {}", uiContext);
            context.setUIContext(uiContext);
            context.setCommunicationSanityChecker(communicationSanityChecker);
            application.registerUIContext(uiContext);

            uiContext.begin();
            try {
                beginObject();
                encode(ServerToClientModel.CREATE_CONTEXT, uiContext.getID());
                endObject();
                if (isLiving() && isSessionOpen()) flush0();
            } catch (final Throwable e) {
                log.error("Cannot send server heart beat to client", e);
            } finally {
                uiContext.end();
            }

            applicationManager.startApplication(context);
            communicationSanityChecker.start();
        } catch (final Exception e) {
            log.error("Cannot process WebSocket instructions", e);
        }
    }

    @Override
    public void onWebSocketError(final Throwable throwable) {
        log.error("WebSocket Error", throwable);
    }

    @Override
    public void onWebSocketClose(final int statusCode, final String reason) {
        log.info("WebSocket closed on UIContext #{} : {}, reason : {}", context.getUIContext().getID(),
            NiceStatusCode.getMessage(statusCode), reason != null ? reason : "");
        if (isLiving()) context.getUIContext().onDestroy();
    }

    /**
     * Receive from the terminal
     */
    @Override
    public void onWebSocketText(final String text) {
        final UIContext uiContext = context.getUIContext();
        if (isLiving()) {
            if (monitor != null) monitor.onMessageReceived(WebSocket.this, text);
            try {
                context.getCommunicationSanityChecker().onMessageReceived();

                if (ClientToServerModel.HEARTBEAT.toStringValue().equals(text)) {
                    if (log.isDebugEnabled()) log.debug("Heartbeat received from terminal #{}", uiContext.getID());
                } else {
                    final JsonObject jsonObject = Json.createReader(new StringReader(text)).readObject();
                    if (jsonObject.containsKey(ClientToServerModel.PING_SERVER.toStringValue())) {
                        final long start = jsonObject.getJsonNumber(ClientToServerModel.PING_SERVER.toStringValue()).longValue();
                        final long end = System.currentTimeMillis();
                        if (log.isDebugEnabled())
                            log.debug("Ping measurement : {} ms from terminal #{}", end - start, uiContext.getID());
                        uiContext.addPingValue(end - start);
                    } else if (jsonObject.containsKey(ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue())) {
                        final Application applicationSession = context.getApplication();
                        if (applicationSession == null)
                            throw new Exception("Invalid session, please reload your application (" + uiContext + ")");
                        final String applicationInstructions = ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue();
                        uiContext.execute(() -> {
                            final JsonArray appInstructions = jsonObject.getJsonArray(applicationInstructions);
                            for (int i = 0; i < appInstructions.size(); i++) {
                                uiContext.fireClientData(appInstructions.getJsonObject(i));
                            }
                        });
                    } else if (jsonObject.containsKey(ClientToServerModel.ERROR_MSG.toStringValue())) {
                        String extraMsg = "";
                        if (jsonObject.containsKey(ClientToServerModel.OBJECT_ID.toStringValue())) {
                            final int objectID = jsonObject.getJsonNumber(ClientToServerModel.OBJECT_ID.toStringValue()).intValue();
                            extraMsg = " on " + uiContext.getObject(objectID);
                        }
                        log.error("Message from terminal #{} : {}{}", uiContext.getID(),
                            jsonObject.getJsonString(ClientToServerModel.ERROR_MSG.toStringValue()), extraMsg);
                    } else if (jsonObject.containsKey(ClientToServerModel.WARNING_MSG.toStringValue())) {
                        String extraMsg = "";
                        if (jsonObject.containsKey(ClientToServerModel.OBJECT_ID.toStringValue())) {
                            final int objectID = jsonObject.getJsonNumber(ClientToServerModel.OBJECT_ID.toStringValue()).intValue();
                            extraMsg = " on " + uiContext.getObject(objectID);
                        }
                        log.warn("Message from terminal #{} : {}{}", uiContext.getID(),
                            jsonObject.getJsonString(ClientToServerModel.WARNING_MSG.toStringValue()), extraMsg);
                    } else if (jsonObject.containsKey(ClientToServerModel.INFO_MSG.toStringValue())) {
                        if (log.isInfoEnabled()) {
                            String extraMsg = "";
                            if (jsonObject.containsKey(ClientToServerModel.OBJECT_ID.toStringValue())) {
                                final int objectID = jsonObject.getJsonNumber(ClientToServerModel.OBJECT_ID.toStringValue())
                                    .intValue();
                                extraMsg = " on " + uiContext.getObject(objectID);
                            }
                            log.info("Message from terminal #{} : {}{}", uiContext.getID(),
                                jsonObject.getJsonString(ClientToServerModel.INFO_MSG.toStringValue()), extraMsg);
                        }
                    } else {
                        log.error("Unknow message from terminal #{} : {}", uiContext.getID(), text);
                    }
                }
            } catch (final Throwable e) {
                log.error("Cannot process message from terminal  #" + uiContext.getID() + " : " + text, e);
            } finally {
                if (monitor != null) monitor.onMessageProcessed(WebSocket.this);
            }
        } else {
            log.info("UI Context is destroyed, message dropped from terminal : {}", text);
        }
    }

    /**
     * Receive from the terminal
     */
    @Override
    public void onWebSocketBinary(final byte[] payload, final int offset, final int len) {
        // Can't receive binary data from terminal (GWT limitation)
    }

    public String getHistoryToken() {
        final List<String> historyTokens = this.request.getParameterMap().get(ClientToServerModel.TYPE_HISTORY.toStringValue());
        return !historyTokens.isEmpty() ? historyTokens.get(0) : null;
    }

    /**
     * Send heart beat to the client
     */
    public void sendHeartBeat() {
        if (isLiving() && isSessionOpen()) {
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
        if (isLiving() && isSessionOpen()) {
            beginObject();
            encode(ServerToClientModel.PING_SERVER, System.currentTimeMillis());
            endObject();
            flush0();
        }
    }

    public void flush() {
        if (isLiving() && isSessionOpen()) flush0();
    }

    private void flush0() {
        websocketPusher.flush();
    }

    public void close() {
        if (isSessionOpen()) {
            log.info("Closing websocket programaticly");
            session.close();
        }
    }

    private boolean isLiving() {
        return context != null && context.getUIContext() != null && context.getUIContext().isLiving();
    }

    private boolean isSessionOpen() {
        return session != null && session.isOpen();
    }

    @Override
    public void beginObject() {
    }

    @Override
    public void endObject() {
        encode(ServerToClientModel.END, null);
    }

    @Override
    public void encode(final ServerToClientModel model, final Object value) {
        websocketPusher.encode(model, value);
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

        private int statusCode;
        private String message;

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

}
