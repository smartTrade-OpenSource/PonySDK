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

import java.util.Map;

import javax.servlet.ServletException;

import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.application.AbstractApplicationManager;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.tools.URLUtils;
import com.ponysdk.core.ui.basic.PWebSocket;

public class WebSocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet {

    private static final Logger log = LoggerFactory.getLogger(WebSocketServlet.class);

    private static final long serialVersionUID = 1L;
    private int maxIdleTime = 1000000;
    private AbstractApplicationManager applicationManager;
    private WebsocketMonitor monitor;

    @Override
    public void init() throws ServletException {
        super.init();
        applicationManager = (AbstractApplicationManager) getServletContext()
            .getAttribute(AbstractApplicationManager.class.getCanonicalName());
    }

    @Override
    public void configure(final WebSocketServletFactory factory) {
        // Remove compression capabilities to avoid a max buffer size bug
        factory.getExtensionFactory().unregister("permessage-deflate");

        factory.getPolicy().setIdleTimeout(maxIdleTime);
        factory.setCreator((request, response) -> {
            // Force session creation if there is no session
            request.getHttpServletRequest().getSession(true);

            Integer uiContextID = null;
            UIContext uiContext = null;
            Integer objectID = null;
            WebSocketServer webSocketServer = null;

            final Map<String, String> queryStringParameters = URLUtils.getQueryStringParameters(request.getQueryString());
            final String headerUIcontextID = queryStringParameters.get(ClientToServerModel.UI_CONTEXT_ID.toStringValue());
            final String headerObjectID = queryStringParameters.get(ClientToServerModel.OBJECT_ID.toStringValue());

            if (headerUIcontextID != null) {
                uiContextID = Integer.parseInt(headerUIcontextID);
                objectID = Integer.parseInt(headerObjectID);
                uiContext = SessionManager.get().getUIContext(uiContextID);

                final PWebSocket pWebsocket = uiContext.getObject(objectID);

                webSocketServer = new WebSocketServer();
                webSocketServer.setMonitor(monitor);
                pWebsocket.setWebsocketServer(webSocketServer);
                return webSocketServer;
            } else {
                if (request.getSession() != null) {
                    webSocketServer = new MainWebSocket(request, applicationManager);
                    webSocketServer.setMonitor(monitor);
                    return webSocketServer;
                } else {
                    log.error("No HTTP session found");
                    return null;
                }
            }
        });
    }

    public void setMaxIdleTime(final int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public void setWebsocketMonitor(final WebsocketMonitor monitor) {
        this.monitor = monitor;
    }

}
