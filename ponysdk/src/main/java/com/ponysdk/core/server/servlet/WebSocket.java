/*============================================================================
 *
 * Copyright (c) 2000-2016 Smart Trade Technologies. All Rights Reserved.
 *
 * This software is the proprietary information of Smart Trade Technologies
 * Use is subject to license terms. Duplication or distribution prohibited.
 *
 *============================================================================*/

package com.ponysdk.core.server.servlet;

import java.io.StringReader;
import java.nio.ByteBuffer;

import javax.json.Json;
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
import com.ponysdk.core.server.application.Parser;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.useragent.UserAgent;

public class WebSocket implements WebSocketListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);

    private final ServletUpgradeRequest request;
    private final WebsocketMonitor monitor;
    private final BufferManager bufferManager;
    private final AbstractApplicationManager applicationManager;

    private TxnContext context;
    private Session session;

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
            Parser.encode(buffer, ServerToClientModel.UI_CONTEXT_ID, uiContext.getID());
            flush(buffer);
            applicationManager.startApplication(context);
        } catch (final Exception e) {
            log.error("Cannot process WebSocket instructions", e);
        }
    }

    @Override
    public void onWebSocketError(final Throwable throwable) {
        log.error("WebSoket Error", throwable);
    }

    @Override
    public void onWebSocketClose(final int statusCode, final String reason) {
        log.info("WebSoket closed {}, reason : {}", NiceStatusCode.getMessage(statusCode), reason != null ? reason : "");
        if (context != null && context.getUIContext() != null && context.getUIContext().isLiving()) {
            context.getUIContext().onDestroy();
        }
    }

    /**
     * Receive from the terminal
     */
    @Override
    public void onWebSocketText(final String text) {
        if (context.getUIContext().isLiving()) {
            if (monitor != null) monitor.onMessageReceived(WebSocket.this, text);
            try {
                context.getUIContext().notifyMessageReceived();

                if (ClientToServerModel.HEARTBEAT.toStringValue().equals(text)) {
                    if (log.isDebugEnabled()) log.debug("Heartbeat received from terminal");
                } else {
                    final JsonObject jsonObject = Json.createReader(new StringReader(text)).readObject();
                    if (jsonObject.containsKey(ClientToServerModel.PING_SERVER.toStringValue())) {
                        final long start = jsonObject.getJsonNumber(ClientToServerModel.PING_SERVER.toStringValue()).longValue();
                        final long end = System.currentTimeMillis();
                        if (log.isInfoEnabled()) log.info("Ping measurement : " + (end - start) + " ms");
                    } else if (jsonObject.containsKey(ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue())) {
                        if (log.isInfoEnabled()) log.info("Message received from terminal : " + text);
                        applicationManager.fireInstructions(jsonObject, context);
                    } else {
                        log.error("Unknow message from terminal : " + text);
                    }
                }
            } catch (final Throwable e) {
                log.error("Cannot process message from the terminal : {}", text, e);
            } finally {
                if (monitor != null) monitor.onMessageProcessed(WebSocket.this);
            }
        } else {
            if (log.isInfoEnabled()) log.info("Message dropped, ui context is destroyed");
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

    /**
     * Send to the terminal
     */
    public void flush(final ByteBuffer buffer) {
        if (context.getUIContext().isLiving() && isSessionOpen()) {
            if (buffer.position() != 0) {
                if (monitor != null) monitor.onBeforeFlush(WebSocket.this, buffer.position());
                buffer.flip();
                try {
                    bufferManager.send(session.getRemote(), buffer);
                } finally {
                    if (monitor != null) monitor.onAfterFlush(WebSocket.this);
                }
            }
        } else {
            throw new IllegalStateException("UI Context has been destroyed");
        }
    }

    /**
     * Send heart beat to the client
     */
    public void sendHeartBeat() {
        if (isSessionOpen()) {
            final ByteBuffer buffer = getBuffer();
            Parser.encode(buffer, ServerToClientModel.HEARTBEAT);
            flush(buffer);
        }
    }

    /**
     * Send round trip to the client
     */
    public void sendRoundTrip() {
        if (isSessionOpen()) {
            final ByteBuffer buffer = getBuffer();
            Parser.encode(buffer, ServerToClientModel.PING_SERVER, System.currentTimeMillis());
            flush(buffer);
        }
    }

    public void close() {
        if (isSessionOpen()) {
            log.info("Closing websocket programaticly");
            session.close();
        }
    }

    final boolean isSessionOpen() {
        return session != null && session.isOpen();
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

        public int getStatusCode() {
            return statusCode;
        }

        public String getMessage() {
            return message;
        }

        public static final String getMessage(final int statusCode) {
            for (final WebSocket.NiceStatusCode niceStatusCode : values()) {
                if (niceStatusCode.getStatusCode() == statusCode) {
                    return statusCode + " : " + niceStatusCode.getMessage();
                }
            }
            log.error("No matching status code found for {}", statusCode);
            return String.valueOf(statusCode);
        }
    }
}