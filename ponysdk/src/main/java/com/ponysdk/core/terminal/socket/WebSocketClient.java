/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.terminal.socket;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.PonySDK;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.instruction.PTInstruction;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.request.WebSocketRequestBuilder;

import elemental.client.Browser;
import elemental.events.CloseEvent;
import elemental.events.Event;
import elemental.events.EventListener;
import elemental.events.MessageEvent;
import elemental.html.ArrayBuffer;
import elemental.html.WebSocket;
import elemental.html.Window;
import elemental.html.Worker;

public class WebSocketClient implements MessageSender {

    private static final Logger log = Logger.getLogger(WebSocketClient.class.getName());

    private final WebSocket webSocket;
    private final UIBuilder uiBuilder;

    private final Window window;

    private final ReaderBuffer readerBuffer;

    public WebSocketClient(final String url, final UIBuilder uiBuilder, final WebSocketDataType webSocketDataType) {
        this.uiBuilder = uiBuilder;

        readerBuffer = new ReaderBuffer();

        window = Browser.getWindow();
        webSocket = window.newWebSocket(url);
        webSocket.setBinaryType(webSocketDataType.getName());

        final MessageReader messageReader;
        if (WebSocketDataType.ARRAYBUFFER.equals(webSocketDataType)) {
            messageReader = new ArrayBufferReader(this);
        } else if (WebSocketDataType.BLOB.equals(webSocketDataType)) {
            messageReader = new BlobReader(this);
        } else {
            throw new IllegalArgumentException("Wrong reader type : " + webSocketDataType);
        }

        webSocket.setOnopen(new EventListener() {

            @Override
            public void handleEvent(final Event event) {
                if (log.isLoggable(Level.INFO)) log.info("WebSoket connected");

                Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

                    @Override
                    public boolean execute() {
                        if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Heart beat sent");
                        send(ClientToServerModel.HEARTBEAT.toStringValue());
                        return true;
                    }
                }, 1000);
            }
        });
        webSocket.setOnclose(new EventListener() {

            @Override
            public void handleEvent(final Event event) {
                if (event instanceof CloseEvent) {
                    final CloseEvent closeEvent = (CloseEvent) event;
                    if (log.isLoggable(Level.INFO)) log.info("WebSoket disconnected : " + closeEvent.getCode());
                    uiBuilder.onCommunicationError(new StatusCodeException(closeEvent.getCode(), closeEvent.getReason()));
                } else {
                    if (log.isLoggable(Level.INFO)) log.info("WebSoket disconnected");
                    uiBuilder.onCommunicationError(new Exception("Websocket connection closed"));
                }
            }
        });
        webSocket.setOnerror(new EventListener() {

            @Override
            public void handleEvent(final Event event) {
                if (log.isLoggable(Level.INFO)) log.info("WebSoket error");
                uiBuilder.onCommunicationError(new Exception("Websocket error"));
            }
        });
        webSocket.setOnmessage(new EventListener() {

            /**
             * Message from server to Main terminal
             */
            @Override
            public void handleEvent(final Event event) {
                messageReader.read((MessageEvent) event);
            }
        });
    }

    @Override
    public void read(final ArrayBuffer arrayBuffer) {
        try {
            readerBuffer.init(window.newUint8Array(arrayBuffer, 0, arrayBuffer.getByteLength()));
            // Get the first element on the message, always a key of element of the Model enum
            final BinaryModel type = readerBuffer.readBinaryModel();

            if (type.getModel() == ServerToClientModel.PING_SERVER) {
                if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Ping received");
                final PTInstruction requestData = new PTInstruction();
                requestData.put(ClientToServerModel.PING_SERVER, type.getLongValue());
                send(requestData.toString());
            } else if (type.getModel() == ServerToClientModel.HEARTBEAT) {
                if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "Heart beat received");
            } else if (type.getModel() == ServerToClientModel.UI_CONTEXT_ID) {
                PonySDK.uiContextId = type.getIntValue();
                uiBuilder.init(new WebSocketRequestBuilder(WebSocketClient.this));
            } else {
                try {
                    readerBuffer.rewind(type);
                    uiBuilder.updateMainTerminal(readerBuffer);
                } catch (final Exception e) {
                    log.log(Level.SEVERE, "Error while processing the " + readerBuffer, e);
                }
            }
        } catch (final Exception e) {
            log.log(Level.SEVERE, "Cannot parse " + arrayBuffer, e);
        }
    }

    public void send(final String message) {
        webSocket.send(message);
    }

    public void close() {
        webSocket.close();
    }

    public final native void setWebsocket(Worker w, Window window, WebSocket webSocket) /*-{
                                                                                        window.webSocket = webSocket;
                                                                                        }-*/;

    public enum WebSocketDataType {

        ARRAYBUFFER("arraybuffer"),
        BLOB("blob");

        private String name;

        private WebSocketDataType(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

}
