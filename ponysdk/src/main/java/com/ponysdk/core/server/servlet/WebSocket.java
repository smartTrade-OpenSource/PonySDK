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

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;

import com.ponysdk.core.server.application.ApplicationManager;
import com.ponysdk.core.server.websocket.WebsocketMonitor;

/**
 * @deprecated Use {@link com.ponysdk.core.server.websocket.WebSocket} instead
 */
@Deprecated(forRemoval = true, since = "v2.8.1")
public class WebSocket extends com.ponysdk.core.server.websocket.WebSocket {

    WebSocket(final ServletUpgradeRequest request, final WebsocketMonitor monitor, final ApplicationManager applicationManager) {
        super();
        setRequest(request);
        setMonitor(monitor);
        setApplicationManager(applicationManager);
    }

}
