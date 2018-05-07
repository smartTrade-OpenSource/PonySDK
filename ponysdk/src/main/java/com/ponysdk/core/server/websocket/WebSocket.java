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
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.AbstractApplicationManager;
import com.ponysdk.core.server.context.UIContext;
import com.ponysdk.core.ui.basic.PObject;

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
        if (isAlive()) {
            uiContext.destroy();
            log.info("WebSocket closed for UIContext #{} : {}, reason : {}", uiContext.getID(), closeReason);
        } else {

        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket Error", throwable);
    }

    @OnMessage
    public void onMessage(String message) {
        //todo if tt est a null ....


        if (isAlive()) {
            try {
                uiContext.onMessageReceived();
                if (monitor != null) monitor.onMessageReceived(WebSocket.this, message);
                //uiContext.sendHeartBeat();
                //uiContext.sendRoundTrip();

                if (ClientToServerModel.HEARTBEAT.toStringValue().equals(message)) {
                    processHeartbeat();
                    //sendHeartBeat(); //TODO nciaravola temp
                    //sendRoundTrip();
                } else {
                    final JsonObject json = Json.createReader(new StringReader(message)).readObject();

                    if (json.containsKey(ClientToServerModel.PING_SERVER.toStringValue())) {
                        processPing(json);
                    } else if (json.containsKey(ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue())) {
                        processInstructions(json);
                    } else if (json.containsKey(ClientToServerModel.ERROR_MSG.toStringValue())) {
                        processTerminalLog(json, ClientToServerModel.ERROR_MSG);
                    } else if (json.containsKey(ClientToServerModel.WARN_MSG.toStringValue())) {
                        processTerminalLog(json, ClientToServerModel.WARN_MSG);
                    } else if (json.containsKey(ClientToServerModel.INFO_MSG.toStringValue())) {
                        processTerminalLog(json, ClientToServerModel.INFO_MSG);
                    } else {
                        log.error("Unknown message received from terminal UIContext = {} : Message = {}", uiContext.getID(), message);
                    }
                }
                if (monitor != null) monitor.onMessageProcessed(WebSocket.this, message);
            } catch (final Throwable e) {
                log.error("Cannot process message from terminal  #" + uiContext.getID() + " : " + message, e);
            } finally {
                if (monitor != null) monitor.onMessageUnprocessed(WebSocket.this, message);
            }
        } else {
            log.info("UIContext is destroyed, message received from terminal has been dropped UIContext = {} : Message = {}", uiContext.getID(), message);
        }
    }

    private void processTerminalLog(JsonObject json, ClientToServerModel level) {
        String message = json.getJsonString(level.toStringValue()).getString();
        String objectInformation = "";

        if (json.containsKey(ClientToServerModel.OBJECT_ID.toStringValue())) {
            PObject object = uiContext.getObject(json.getJsonNumber(ClientToServerModel.OBJECT_ID.toStringValue()).intValue());
            objectInformation = object == null ? "NA" : object.toString();
        }

        switch (level) {
            case INFO_MSG:
                log.info("Message received from terminal UIContext = {} : Object = {} : Message = {}", uiContext.getID(), objectInformation, message);
                break;
            case WARN_MSG:
                log.warn("Message received from terminal UIContext = {} : Object = {} : Message = {}", uiContext.getID(), objectInformation, message);
                break;
            case ERROR_MSG:
                log.error("Message received from terminal UIContext = {} : Object = {} : Message = {}", uiContext.getID(), objectInformation, message);
                break;
            default:
                log.error("Unknown log level during terminal log processing : {}", level);
        }
    }

    private void processPing(JsonObject json) {
        final long start = json.getJsonNumber(ClientToServerModel.PING_SERVER.toStringValue()).longValue();
        final long end = System.currentTimeMillis();
        if (log.isDebugEnabled())
            log.debug("Ping measurement : {} ms from terminal #{}", end - start, uiContext.getID());
        uiContext.addPingValue(end - start);
    }

    private void processInstructions(JsonObject json) {
        final String applicationInstructions = ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue();
        uiContext.execute(() -> {
            final JsonArray appInstructions = json.getJsonArray(applicationInstructions);
            for (int i = 0; i < appInstructions.size(); i++) {
                uiContext.fireClientData(appInstructions.getJsonObject(i));
            }
        });
    }

    private void processHeartbeat() {
        if (log.isDebugEnabled()) log.debug("Heartbeat received from terminal #{}", uiContext.getID());
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

}
