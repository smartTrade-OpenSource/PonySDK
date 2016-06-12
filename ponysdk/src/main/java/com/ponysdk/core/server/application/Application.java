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

import com.ponysdk.core.server.servlet.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Wrapper of the HTTPSession, and contains the UIContexts.
 * </p>
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final Map<Integer, UIContext> uiContexts = new ConcurrentHashMap<>();

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private final ApplicationManagerOption options;

    private final String sessionID;

    public Application(String sessionID, final ApplicationManagerOption options) {
        this.options = options;
        this.sessionID = sessionID;
    }

    void registerUIContext(final UIContext uiContext) {
        uiContexts.put(uiContext.getID(), uiContext);
    }

    void unregisterUIContext(final int uiContextID) {
        uiContexts.remove(uiContextID);
        if (uiContexts.isEmpty()) {
            log.info("Invalidate session, all ui contexts have been destroyed");
            SessionManager.get().getSession(sessionID).invalidate();
        }
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

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(final String name) {
        return (T) attributes.get(name);
    }

    public ApplicationManagerOption getOptions() {
        return options;
    }

    public String getName() {
        return options.getApplicationName();
    }

    public String getID() {
        return options.getApplicationID();
    }

    public String getSessionID() {
        return sessionID;
    }

    @Override
    public String toString() {
        return "Application [" + options + ", ID=" + getID() + ", name=" + getName() + "]";
    }
}