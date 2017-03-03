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

    public Application(final HttpSession session, final ApplicationManagerOption options, final UserAgent userAgent) {
        this.options = options;
        this.session = session;
        this.userAgent = userAgent;
    }

    public void registerUIContext(final UIContext uiContext) {
        uiContexts.put(uiContext.getID(), uiContext);
    }

    void unregisterUIContext(final int uiContextID) {
        uiContexts.remove(uiContextID);
        if (uiContexts.isEmpty()) {
            log.info("Invalidate session, all ui contexts have been destroyed");
            session.invalidate();
            SessionManager.get().unregisterApplication(this);
        }
    }

    public void destroy() {
        uiContexts.values().forEach(UIContext::destroyFromApplication);
        uiContexts.clear();
        session.invalidate();
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

    public HttpSession getSession() {
        return session;
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    @Override
    public String toString() {
        return "Application [options=" + options + ", userAgent=" + userAgent + ", session=" + session + "]";
    }

}
