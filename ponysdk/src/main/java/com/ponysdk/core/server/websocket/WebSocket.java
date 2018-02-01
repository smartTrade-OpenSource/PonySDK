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
import com.ponysdk.core.server.application.AbstractApplicationManager;
import com.ponysdk.core.server.context.UIContext;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.websocket.*;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ServerEndpoint(value = "/ws")
public class WebSocket implements WebsocketEncoder {

    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);

    private WebsocketMonitor monitor;
    private WebSocketPusher websocketPusher;
    private AbstractApplicationManager applicationManager;
    private Session session;

    private UIContext uiContext;
    private HandshakeRequest request;

    @OnOpen
    public void onOpen(Session s, EndpointConfig config) {
        session = s;
        //final String userAgent = request.getHeaders().get("User-Agent").get(0);
        //log.info("WebSocket connected from {}, sessionID={}, userAgent={}", session.getBasicRemote(), request.getSession().getId(), userAgent);

        // 1K for max chunk size and 1M for total buffer size
        // Don't set max chunk size > 8K because when using Jetty Websocket compression, the chunks are limited to 8K
        websocketPusher = new WebSocketPusher(session, 1 << 20, 1 << 12, TimeUnit.SECONDS.toMillis(60));
        uiContext = new UIContext(this, request, applicationManager.getConfiguration());
        applicationManager.startContext(uiContext);
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        uiContext.destroy();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket Error", throwable);
    }

    @OnMessage
    public void onMessage(String message) {
        if (isAlive()) {
            if (monitor != null) monitor.onMessageReceived(WebSocket.this, message);
            try {
                uiContext.onMessageReceived();

                if (ClientToServerModel.HEARTBEAT.toStringValue().equals(message)) {
                    if (log.isDebugEnabled()) log.debug("Heartbeat received from terminal #{}", uiContext.getID());
                } else {
                    final JsonObject jsonObject = Json.createReader(new StringReader(message)).readObject();
                    if (jsonObject.containsKey(ClientToServerModel.PING_SERVER.toStringValue())) {
                        final long start = jsonObject.getJsonNumber(ClientToServerModel.PING_SERVER.toStringValue()).longValue();
                        final long end = System.currentTimeMillis();
                        if (log.isDebugEnabled())
                            log.debug("Ping measurement : {} ms from terminal #{}", end - start, uiContext.getID());
                        uiContext.addPingValue(end - start);
                    } else if (jsonObject.containsKey(ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue())) {
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
                        log.error("Unknow message from terminal #{} : {}", uiContext.getID(), message);
                    }
                }
            } catch (final Throwable e) {
                log.error("Cannot process message from terminal  #" + uiContext.getID() + " : " + message, e);
            } finally {
                if (monitor != null) monitor.onMessageProcessed(WebSocket.this);
            }
        } else {
            log.info("UI Context is destroyed, message dropped from terminal : {}", message);
        }
    }

    public void setApplicationManager(AbstractApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    public void setMonitor(WebsocketMonitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Send heart beat to the terminal
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
     * Send round trip to the terminal
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

    public void close() throws IOException {
        if (isSessionOpen()) {
            log.info("Closing websocket programaticly");
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
    }

    @Override
    public void endObject() {
        encode(ServerToClientModel.END, null);
    }

    @Override
    public void encode(final ServerToClientModel model, final Object value) {
        websocketPusher.encode(model, value);
    }

    public void setRequest(HandshakeRequest request) {
        this.request = request;
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

        private int statusCode;
        private String message;

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

}
