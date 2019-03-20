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

package com.ponysdk.core.server.application;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;

import com.ponysdk.core.server.context.UIContext;
import com.ponysdk.core.server.context.UIContextImpl;
import com.ponysdk.core.server.websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.servlet.SessionManager;

/**
 * Wrapper of the HTTPSession, and contains the UIContexts.
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final Map<Integer, UIContextImpl> uiContexts = new ConcurrentHashMap<>();

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private final ApplicationConfiguration configuration;

    private final HttpSession session;

    private final String id;

    public Application(final String id, final HttpSession session, final ApplicationConfiguration configuration) {
        this.id = id;
        this.session = session;
        this.configuration = configuration;
    }

    public UIContext createUIContext(WebSocket socket) {
        UIContextImpl uiContext = new UIContextImpl(socket, this);
        uiContexts.put(uiContext.getID(), uiContext);
        log.info("Creating a new UIContextImpl:{}", uiContext);
        return uiContext;
    }

    public void deregisterUIContext(final int uiContextID) {
        uiContexts.remove(uiContextID);
        if (uiContexts.isEmpty()) {
            session.invalidate();
            SessionManager.get().unregisterApplication(this);
        }
    }

    public void destroy() {
        uiContexts.values().forEach(uiContext -> {
            try {
                uiContext.destroyFromApplication();
            } catch (final Exception e) {
                log.error("Can't destroy the UIContextImpl #" + uiContext.getID() + " on Application #" + id, e);
            }
        });
        uiContexts.clear();

        try {
            log.info("Invalidate session on Application #{} because all ui contexts have been destroyed", id);
            session.invalidate();
        } catch (final IllegalArgumentException e) {
            log.error("The session is already invalid", e);
        }
        SessionManager.get().unregisterApplication(this);
    }

    public UIContextImpl getUIContext(final int uiContextID) {
        return uiContexts.get(uiContextID);
    }

    public Collection<UIContextImpl> getUIContexts() {
        return uiContexts.values();
    }

    public void setAttribute(final String name, final Object value) {
        attributes.put(name, value);
    }

    public <T> T getAttribute(final String name) {
        return (T) attributes.get(name);
    }

    public ApplicationConfiguration getConfiguration() {
        return configuration;
    }

    public String getId() {
        return id;
    }

    public int countUIContexts() {
        return uiContexts.size();
    }

    @Override
    public String toString() {
        return "Application [id=" + id + ", options=" + configuration + "]";
    }

}
