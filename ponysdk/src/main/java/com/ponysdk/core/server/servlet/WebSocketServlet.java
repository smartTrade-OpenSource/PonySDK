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

package com.ponysdk.core.server.servlet;

import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.websocket.PonyPerMessageDeflateExtension;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.server.websocket.WebsocketMonitor;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;
import org.eclipse.jetty.websocket.core.WebSocketComponents;
import org.eclipse.jetty.websocket.core.server.WebSocketServerComponents;

import java.time.Duration;

public class WebSocketServlet extends JettyWebSocketServlet {

    private final ApplicationManager applicationManager;
    private int maxIdleTime = 1000000;
    private WebsocketMonitor monitor;

    public WebSocketServlet(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    protected WebSocket createWebsocket(JettyServerUpgradeRequest request, JettyServerUpgradeResponse response) {
        final WebSocket webSocket = new WebSocket();
        webSocket.setRequest(request);
        webSocket.setApplicationManager(applicationManager);
        webSocket.setMonitor(monitor);
        return webSocket;
    }

    public void setMaxIdleTime(final int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public void setWebsocketMonitor(final WebsocketMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofMillis(maxIdleTime));
        factory.setCreator(this::createWebsocket);
        ServletContextHandler context = ServletContextHandler.getServletContextHandler(getServletContext());
        WebSocketComponents components = WebSocketServerComponents.getWebSocketComponents(context);
        components.getExtensionRegistry().register("permessage-deflate", PonyPerMessageDeflateExtension.class);
    }
}
