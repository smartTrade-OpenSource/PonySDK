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

import javax.servlet.http.HttpSession;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapper of the HTTPSession, and contains the UIContexts.
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final Map<Integer, UIContext> uiContexts = new ConcurrentHashMap<>();

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private final ApplicationConfiguration configuration;

    private final HttpSession session;

    private final String id;

    public Application(final String id, final HttpSession session, final ApplicationConfiguration configuration) {
        this.id = id;
        this.session = session;
        this.configuration = configuration;
    }

    public void registerUIContext(final UIContext uiContext) {
        uiContexts.put(uiContext.getID(), uiContext);
    }

    public void deregisterUIContext(final int uiContextID) {
        if (uiContexts.remove(uiContextID) != null && uiContexts.isEmpty()) {
            try {
                session.invalidate();
            } catch (final IllegalStateException e) {
                log.warn("Issue when unregistering UIContext #{} : Session {} already invalidated", uiContextID, session.getId());
            }
            SessionManager.get().unregisterApplication(this);
        }
    }

    public void destroy() {
        final Iterator<UIContext> iterator = uiContexts.values().iterator();

        while (iterator.hasNext()) {
            UIContext context = iterator.next();
            iterator.remove();
            try {
                context.destroyFromApplication();
            } catch (final Throwable e) {
                log.error("Can't destroy the UIContext #" + context.getID() + " on Application #" + id, e);
            }
        }
        
        try {
            log.info("Invalidate session on Application #{} because all ui contexts have been destroyed", id);
            session.invalidate();
        } catch (final IllegalStateException e) {
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
            log.debug("Pushing to {}", uiContext);
            try {
                uiContext.pushToClient(message);
            } catch (final Throwable throwable) {
                log.error("Cannot flush message on the session {}", uiContext, throwable);
            }
        }
    }

    public void setAttribute(final String name, final Object value) {
        attributes.put(name, value);
    }

    public <T> T getAttribute(final String name) {
        return (T) attributes.get(name);
    }

    public ApplicationConfiguration getOptions() {
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
