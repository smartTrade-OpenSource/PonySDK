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
import com.ponysdk.core.server.context.CommunicationSanityChecker;
import com.ponysdk.core.server.context.api.UIContext;
import com.ponysdk.core.server.context.impl.UIContextImpl;
import com.ponysdk.core.ui.basic.PObject;
import org.eclipse.jetty.util.component.Container;
import org.eclipse.jetty.websocket.common.extensions.ExtensionStack;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@ServerEndpoint("/ws")
public class WebSocket {

    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);

    private static final String MESSAGE_PATTERN = "Message received from terminal : UIContextImpl #{} on {} : {}";

    private ServletUpgradeRequest request;
    private WebsocketMonitor monitor;
    private WebsocketEncoder encoder;
    private ApplicationManager applicationManager;

    private Session session;
    private UIContext uiContext;

    private CommunicationSanityChecker communicationSanityChecker;

    @OnOpen
    public void onOpen(final Session session, EndpointConfig config) throws IOException {
        this.session = session;

        uiContext = new UIContextImpl(this);
        communicationSanityChecker.registerSession(this);

        encoder = new WebSocketPusher(session, 1 << 20, 1 << 12, TimeUnit.SECONDS.toMillis(60));
        encoder.encode(ServerToClientModel.CREATE_CONTEXT, uiContext.getID());
        encoder.encode(ServerToClientModel.END, null);
        encoder.flush();

        applicationManager.startApplication(uiContext);
    }

    @OnError
    public void onError(final Throwable throwable) {
        log.error("WebSocket Error on UIContext : {}", uiContext.getID(), throwable);
        uiContext.close();
    }

    @OnClose
    public void onClose(final int statusCode, final String reason) {
        log.info("WebSocket closed on UIContext : {}, code : {}, reason : {}", uiContext.getID(), statusCode, reason);
        uiContext.close();
    }

    @OnMessage
    public void onMessage(final String message) {
        if (this.listener != null) listener.onIncomingText(message);
        if (isAlive()) {
            try {
                uiContext.onMessageReceived();
                if (monitor != null) monitor.onMessageReceived(this, message);


                if (jsonObject.containsKey(ClientToServerModel.TERMINAL_LATENCY.toStringValue())) {
                    //processRoundtripLatency(jsonObject);
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

                if (monitor != null) monitor.onMessageProcessed(this, message);
            } catch (final Exception e) {
                log.error("Cannot process message from terminal  #" + uiContext.getID() + " : " + message, e);
            } finally {
                if (monitor != null) monitor.onMessageUnprocessed(this, message);
            }
        } else {
            log.info("UI Context #{} is destroyed, message dropped from terminal : {}", uiContext != null ? uiContext.getID() : -1,
                    message);
        }
    }

    /**
     * private void processRoundtripLatency(final JsonObject jsonObject) {
     * final long roundtripLatency = TimeUnit.MILLISECONDS.convert(System.nanoTime() - lastSentPing, TimeUnit.NANOSECONDS);
     * log.debug("Roundtrip measurement : {} ms from terminal #{}", roundtripLatency, uiContext.getID());
     * uiContext.addRoundtripLatencyValue(roundtripLatency);
     * <p>
     * final long terminalLatency = jsonObject.getJsonNumber(ClientToServerModel.TERMINAL_LATENCY.toStringValue()).longValue();
     * log.debug("Terminal measurement : {} ms from terminal #{}", terminalLatency, uiContext.getID());
     * uiContext.addTerminalLatencyValue(terminalLatency);
     * <p>
     * final long networkLatency = roundtripLatency - terminalLatency;
     * log.debug("Network measurement : {} ms from terminal #{}", networkLatency, uiContext.getID());
     * uiContext.addNetworkLatencyValue(networkLatency);
     * }
     **/

    private void processTerminalLog(final JsonObject json, final ClientToServerModel level) {
        final String message = json.getJsonString(level.toStringValue()).getString();
        String objectInformation = "";

        if (json.containsKey(ClientToServerModel.OBJECT_ID.toStringValue())) {
            final PObject object = uiContext.getObject(json.getJsonNumber(ClientToServerModel.OBJECT_ID.toStringValue()).intValue());
            objectInformation = object == null ? "NA" : object.toString();
        }

        switch (level) {
            case INFO_MSG:
                log.info(MESSAGE_PATTERN, uiContext.getID(), objectInformation, message);
                break;
            case WARN_MSG:
                log.warn(MESSAGE_PATTERN, uiContext.getID(), objectInformation, message);
                break;
            case ERROR_MSG:
                log.error(MESSAGE_PATTERN, uiContext.getID(), objectInformation, message);
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
            //TODO nciaravola
            //lastSentPing = System.nanoTime();
            encode(ServerToClientModel.ROUNDTRIP_LATENCY, null);
            endObject();
            flush0();
        }
    }

    public void flush() {
        if (isAlive() && isSessionOpen()) flush0();
    }

    public String getSessionID() {
        return session.getId();
    }

    public void close() {
        if (isSessionOpen()) {
            log.info("Closing websocket programmatically on UIcontext: {}", uiContext.getID());
            encoder.close();
        }
    }

    private boolean isAlive() {
        return uiContext != null && uiContext.isAlive();
    }

    private boolean isSessionOpen() {
        return session != null && session.isOpen();
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

    public void setCommunicationSanityChecker(CommunicationSanityChecker communicationSanityChecker) {
        this.communicationSanityChecker = communicationSanityChecker;
    }

    public void setMonitor(final WebsocketMonitor monitor) {
        this.monitor = monitor;
    }

    public void setListener(final Listener listener) {
        this.encoder.setWebSocketListener(listener);
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
