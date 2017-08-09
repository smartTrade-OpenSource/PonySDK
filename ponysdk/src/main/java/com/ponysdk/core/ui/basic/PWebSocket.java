/*
 * Copyright (c) 2017 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.basic;

import java.io.IOException;

import com.ponysdk.core.listener.ProcessListener;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.servlet.WebSocketServer;

public class PWebSocket extends PObject implements ProcessListener {

    private WebSocketServer websocketServer;

    private ProcessListener listener;

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.WEBSOCKET;
    }

    public void close() {
        websocketServer.close();
    }

    public void send(final String text) throws IOException {
        websocketServer.sendString(text);
    }

    public void setWebsocketServer(final WebSocketServer websocketServer) {
        this.websocketServer = websocketServer;
        this.websocketServer.setProcessListener(this);
    }

    public void setListener(final ProcessListener listener) {
        this.listener = listener;
    }

    @Override
    public void process(final byte[] payload, final int offset, final int len) {
        listener.process(payload, offset, len);
    }

    @Override
    public void process(final String text) {
        listener.process(text);
    }

    @Override
    public void onClose() {
        listener.onClose();
    }

}
