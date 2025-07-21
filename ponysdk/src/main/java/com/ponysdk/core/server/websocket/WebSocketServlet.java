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

package com.ponysdk.core.server.websocket;

import com.ponysdk.core.server.context.RequestContext;
import jakarta.servlet.http.HttpSession;

import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.server.stm.TxnContext;

import java.time.Duration;

public class WebSocketServlet extends JettyWebSocketServlet {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServlet.class);

    private static final long serialVersionUID = 1L;
    private int maxIdleTime = 1000000;
    private final ApplicationManager applicationManager;
    private WebsocketMonitor monitor;

    public WebSocketServlet(final ApplicationManager applicationManager) {
        this.applicationManager = applicationManager;
    }

    @Override
    public void configure(final JettyWebSocketServletFactory factory) {
        factory.setIdleTimeout(Duration.ofMillis(maxIdleTime));
        factory.setCreator(this::createWebsocket);
    }

    protected WebSocket createWebsocket(final JettyServerUpgradeRequest request, final JettyServerUpgradeResponse response) {
        final WebSocket webSocket = new WebSocket();
        webSocket.setRequestContext(new RequestContext(
                request.getParameterMap(), request.getHeaders(), request.getSession())
        );
        webSocket.setApplicationManager(applicationManager);
        webSocket.setMonitor(monitor);

        final TxnContext context = new TxnContext(webSocket);
        webSocket.setContext(context);

        if (request.getHttpServletRequest().getServletContext().getSessionCookieConfig() != null) {
            configureWithSession(request, context);
        }

        return webSocket;
    }

    protected void configureWithSession(final JettyServerUpgradeRequest request, final TxnContext context) {
        // Force session creation if there is no session
        request.getHttpServletRequest().getSession(true);
        final HttpSession httpSession = (HttpSession) request.getSession();
        if (httpSession != null) {
            final String applicationId = httpSession.getId();

            Application application = SessionManager.get().getApplication(applicationId);
            if (application == null) {
                application = new Application(applicationId, httpSession, applicationManager.getConfiguration());
                SessionManager.get().registerApplication(application);
            }
            context.setApplication(application);
        } else {
            log.error("No HTTP session found");
            throw new IllegalStateException("No HTTP session found");
        }
    }

    public void setMaxIdleTime(final int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public void setWebsocketMonitor(final WebsocketMonitor monitor) {
        this.monitor = monitor;
    }

}
