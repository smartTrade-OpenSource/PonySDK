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

package com.ponysdk.core.ui.selenium;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

public class WebsocketClient extends Endpoint {

    private MessageHandler.Whole<ByteBuffer> handler;
    private Session session;

    public void connect(final URI uri) throws Exception {
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        final ClientManager client = ClientManager.createClient();
        client.getProperties().put(ClientProperties.REDIRECT_ENABLED, true);
        client.connectToServer(this, cec, uri);
    }

    @Override
    public void onOpen(final Session session, final EndpointConfig config) {
        this.session = session;
        if (handler != null) session.addMessageHandler(handler);
    }

    public void sendMessage(final String message) throws IOException {
        session.getBasicRemote().sendText(message);
    }

    public void setMessageHandler(final MessageHandler.Whole<ByteBuffer> handler) {
        this.handler = handler;
        if (session != null) session.addMessageHandler(handler);
    }

    public void close() {
        try {
            session.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
