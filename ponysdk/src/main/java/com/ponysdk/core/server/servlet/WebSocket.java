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
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

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

    private static final byte TRUE = 1;
    private static final byte FALSE = 0;

    private static final String ENCODING_CHARSET = "UTF-8";

    private final ServletUpgradeRequest request;
    private final WebsocketMonitor monitor;
    private final BufferManager bufferManager;
    private final AbstractApplicationManager applicationManager;

    private TxnContext context;
    private Session session;

    private ByteBuffer buffer;
    private int lastUpdatedID = -1;

    WebSocket(final ServletUpgradeRequest request, final WebsocketMonitor monitor, final BufferManager bufferManager,
            final AbstractApplicationManager applicationManager) {
        this.request = request;
        this.monitor = monitor;
        this.bufferManager = bufferManager;
        this.applicationManager = applicationManager;
    }

    @Override
    public void onWebSocketConnect(final Session session) {
        if (log.isInfoEnabled()) log.info("WebSocket connected from {}, sessionID {}, userAgent {}", session.getRemoteAddress(),
            request.getSession(), request.getHeader("User-Agent"));

        this.session = session;
        this.context = new TxnContext(this);

        Application application = SessionManager.get().getApplication(request.getSession().getId());
        if (application == null) {
            application = new Application(request.getSession(), applicationManager.getOptions(),
                UserAgent.parseUserAgentString(request.getHeader("User-Agent")));
            SessionManager.get().registerApplication(application);
        }

        context.setApplication(application);

        if (log.isInfoEnabled()) log.info("Creating a new application, {}", application.toString());
        try {
            final UIContext uiContext = new UIContext(context);
            context.setUIContext(uiContext);
            application.registerUIContext(uiContext);

            final ByteBuffer buffer = getBuffer();
            try {
                encode(buffer, ServerToClientModel.UI_CONTEXT_ID, uiContext.getID());
                flush(buffer);
            } catch (final Throwable t) {
                release(buffer);
            }
            applicationManager.startApplication(context);
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
        log.info("WebSocket closed {}, reason : {}", NiceStatusCode.getMessage(statusCode), reason != null ? reason : "");
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
                uiContext.notifyMessageReceived();

                if (ClientToServerModel.HEARTBEAT.toStringValue().equals(text)) {
                    if (log.isDebugEnabled()) log.debug("Heartbeat received from terminal #" + uiContext.getID());
                } else {
                    final JsonObject jsonObject = Json.createReader(new StringReader(text)).readObject();
                    if (jsonObject.containsKey(ClientToServerModel.PING_SERVER.toStringValue())) {
                        final long start = jsonObject.getJsonNumber(ClientToServerModel.PING_SERVER.toStringValue()).longValue();
                        final long end = System.currentTimeMillis();
                        if (log.isDebugEnabled())
                            log.debug("Ping measurement : " + (end - start) + " ms from terminal #" + uiContext.getID());
                    } else if (jsonObject.containsKey(ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue())) {
                        final Application applicationSession = context.getApplication();
                        if (applicationSession == null)
                            throw new Exception("Invalid session, please reload your application (" + uiContext + ").");
                        final String applicationInstructions = ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue();
                        uiContext.execute(() -> {
                            final JsonArray appInstructions = jsonObject.getJsonArray(applicationInstructions);
                            for (int i = 0; i < appInstructions.size(); i++) {
                                uiContext.fireClientData(appInstructions.getJsonObject(i));
                            }
                        });
                    } else if (jsonObject.containsKey(ClientToServerModel.INFO_MSG.toStringValue())) {
                        log.info("Message from terminal #" + uiContext.getID() + " : "
                                + jsonObject.getJsonString(ClientToServerModel.INFO_MSG.toStringValue()));
                    } else if (jsonObject.containsKey(ClientToServerModel.ERROR_MSG.toStringValue())) {
                        log.error("Message from terminal #" + uiContext.getID() + " : "
                                + jsonObject.getJsonString(ClientToServerModel.ERROR_MSG.toStringValue()));
                    } else {
                        log.error("Unknow message from terminal #" + uiContext.getID() + " : " + text);
                    }
                }
            } catch (final Throwable e) {
                log.error("Cannot process message from terminal  #" + uiContext.getID() + " : {}", text, e);
            } finally {
                if (monitor != null) monitor.onMessageProcessed(WebSocket.this);
            }
        } else {
            if (log.isInfoEnabled()) log.info("UI Context is destroyed, message dropped from terminal : " + text);
        }
    }

    /**
     * Receive from the terminal
     */
    @Override
    public void onWebSocketBinary(final byte[] payload, final int offset, final int len) {
        // Can't receive binary data from terminal (GWT limitation)
    }

    public ByteBuffer getBuffer() {
        return bufferManager.allocate();
    }

    @Override
    public void flush() {
        if (buffer != null) {
            flush(buffer);
            buffer = null;
            lastUpdatedID = -1;
        }
    }

    /**
     * Send to the terminal
     */
    public void flush(final ByteBuffer buffer) {
        if (isLiving() && isSessionOpen()) {
            if (buffer.position() != 0) {
                if (monitor != null) monitor.onBeforeFlush(WebSocket.this, buffer.position());
                try {
                    bufferManager.send(session.getRemote(), buffer);
                } finally {
                    if (monitor != null) monitor.onAfterFlush(WebSocket.this);
                }

                return;
            }
        }

        release(buffer);
    }

    /**
     * Send heart beat to the client
     */
    public void sendHeartBeat() {
        if (isLiving() && isSessionOpen()) {
            final ByteBuffer buffer = getBuffer();
            try {
                encode(buffer, ServerToClientModel.HEARTBEAT);
                flush(buffer);
            } catch (final Throwable t) {
                release(buffer);
            }
        }
    }

    @Override
    public void release() {
        if (buffer != null) {
            release(buffer);
            buffer = null;
            lastUpdatedID = -1;
        }
    }

    public void release(final ByteBuffer buffer) {
        bufferManager.release(buffer);
    }

    /**
     * Send round trip to the client
     */
    public void sendRoundTrip() {
        if (isLiving() && isSessionOpen()) {
            final ByteBuffer buffer = getBuffer();
            try {
                encode(buffer, ServerToClientModel.PING_SERVER, System.currentTimeMillis());
                flush(buffer);
            } catch (final Throwable t) {
                release(buffer);
            }
        }
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
        if (buffer == null) buffer = getBuffer();
    }

    @Override
    public void endObject() {
        if (buffer == null) return;
        if (buffer.position() >= 4096) flush();
    }

    @Override
    public void encode(final ServerToClientModel model, final Object value) {
        if (ServerToClientModel.TYPE_UPDATE.equals(model)) {
            final int newUpdatedID = (int) value;
            if (lastUpdatedID == newUpdatedID) {
                if (log.isDebugEnabled())
                    log.debug("A consecutive update on the same id " + lastUpdatedID + ", so we concatenate the instructions");
                return;
            } else {
                lastUpdatedID = newUpdatedID;
            }
        } else if (ServerToClientModel.TYPE_ADD.equals(model) || ServerToClientModel.TYPE_ADD_HANDLER.equals(model)
                || ServerToClientModel.TYPE_CLOSE.equals(model) || ServerToClientModel.TYPE_CREATE.equals(model)
                || ServerToClientModel.TYPE_GC.equals(model) || ServerToClientModel.TYPE_HISTORY.equals(model)
                || ServerToClientModel.TYPE_REMOVE.equals(model) || ServerToClientModel.TYPE_REMOVE_HANDLER.equals(model)
                || ServerToClientModel.WINDOW_ID.equals(model) || ServerToClientModel.FRAME_ID.equals(model)) {
            lastUpdatedID = -1;
        }

        switch (model.getTypeModel()) {
            case NULL:
                encode(buffer, model);
                break;
            case BOOLEAN:
                encode(buffer, model, (boolean) value);
                break;
            case BYTE:
                encode(buffer, model, (byte) value);
                break;
            case SHORT:
                encode(buffer, model, (short) value);
                break;
            case INTEGER:
                encode(buffer, model, (int) value);
                break;
            case LONG:
                encode(buffer, model, (long) value);
                break;
            case DOUBLE:
                encode(buffer, model, (double) value);
                break;
            case STRING:
                encode(buffer, model, (String) value);
                break;
            case JSON_OBJECT:
                encode(buffer, model, (JsonObject) value);
                break;
            default:
                break;
        }
    }

    private static void encode(final ByteBuffer buffer, final ServerToClientModel model) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model + " (position : " + buffer.position() + ")");
        buffer.putShort(model.getValue());
    }

    private static void encode(final ByteBuffer buffer, final ServerToClientModel model, final boolean value) {
        encode(buffer, model, value ? TRUE : FALSE);
    }

    private static void encode(final ByteBuffer buffer, final ServerToClientModel model, final byte value) {
        if (log.isDebugEnabled())
            log.debug("Writing in the buffer : " + model + " => " + value + " (position : " + buffer.position() + ")");
        buffer.putShort(model.getValue());
        buffer.put(value);
    }

    private static void encode(final ByteBuffer buffer, final ServerToClientModel model, final short value) {
        log.error("Writing in the buffer : " + model + " => " + value + " (position : " + buffer.position() + ")");
        buffer.putShort(model.getValue());
        buffer.putShort(value);
    }

    private static void encode(final ByteBuffer buffer, final ServerToClientModel model, final int value) {
        if (log.isDebugEnabled())
            log.debug("Writing in the buffer : " + model + " => " + value + " (position : " + buffer.position() + ")");
        buffer.putShort(model.getValue());
        buffer.putInt(value);
    }

    private static void encode(final ByteBuffer buffer, final ServerToClientModel model, final long value) {
        encode(buffer, model, String.valueOf(value));
    }

    private static void encode(final ByteBuffer buffer, final ServerToClientModel model, final double value) {
        encode(buffer, model, String.valueOf(value));
    }

    private static void encode(final ByteBuffer buffer, final ServerToClientModel model, final JsonObject jsonObject) {
        encode(buffer, model, jsonObject.toString());
    }

    private static void encode(final ByteBuffer buffer, final ServerToClientModel model, final String value) {
        if (log.isDebugEnabled()) log.debug("Writing in the buffer : " + model + " => " + value + " (size : "
                + (value != null ? value.length() : 0) + ")" + " (position : " + buffer.position() + ")");
        buffer.putShort(model.getValue());

        try {
            if (value != null) {
                final byte[] bytes = value.getBytes(ENCODING_CHARSET);
                buffer.putInt(bytes.length);
                buffer.put(bytes);
            } else {
                buffer.putInt(0);
            }
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot convert string");
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

        private int statusCode;
        private String message;

        NiceStatusCode(final int statusCode, final String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getMessage() {
            return message;
        }

        public static String getMessage(final int statusCode) {
            for (final NiceStatusCode niceStatusCode : values()) {
                if (niceStatusCode.getStatusCode() == statusCode) {
                    return statusCode + " : " + niceStatusCode.getMessage();
                }
            }
            log.error("No matching status code found for {}", statusCode);
            return String.valueOf(statusCode);
        }
    }

}
