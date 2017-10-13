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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.useragent.UserAgent;

/**
 * Wrapper of the HTTPSession, and contains the UIContexts.
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final Map<Integer, UIContext> uiContexts = new ConcurrentHashMap<>();

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private final ApplicationManagerOption options;

    private final UserAgent userAgent;

    private final HttpSession session;

    private final String id;

    public Application(final String id, final HttpSession session, final ApplicationManagerOption options, final UserAgent userAgent) {
        this.id = id;
        this.session = session;
        this.options = options;
        this.userAgent = userAgent;
    }

    public void registerUIContext(final UIContext uiContext) {
        uiContexts.put(uiContext.getID(), uiContext);
    }

    void deregisterUIContext(final int uiContextID) {
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
                log.error("Can't destroy the UIContext #" + uiContext.getID() + " on Application #" + getId(), e);
            }
        });
        uiContexts.clear();

        try {
            log.info("Invalidate session on Application #{} because all ui contexts have been destroyed", getId());
            session.invalidate();
        } catch (final IllegalArgumentException e) {
            log.error("The session is already invalid", e);
        }
        SessionManager.get().unregisterApplication(this);
    }

    public UIContext getUIContext(final int uiContextID) {
        return uiContexts.get(uiContextID);
    }

    public Collection<UIContext> getUIContexts() {
        return uiContexts.values();
    }

    public void pushToClients(final Object message) {
        for (final UIContext uiContext : getUIContexts()) {
            if (log.isDebugEnabled()) log.debug("Pushing to {}", uiContext);
            try {
                uiContext.pushToClient(message);
            } catch (final Throwable throwable) {
                log.error("Cannot flush message on the session {}", uiContext.getContext(), throwable);
            }
        }
    }

    public void setAttribute(final String name, final Object value) {
        attributes.put(name, value);
    }

    public <T> T getAttribute(final String name) {
        return (T) attributes.get(name);
    }

    public ApplicationManagerOption getOptions() {
        return options;
    }

    public String getId() {
        return id;
    }

    public HttpSession getSession() {
        return session;
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public int countUIContexts() {
        return uiContexts.size();
    }

    @Override
    public String toString() {
        return "Application [id=" + id + ", options=" + options + ", userAgent=" + userAgent + "]";
    }

}
