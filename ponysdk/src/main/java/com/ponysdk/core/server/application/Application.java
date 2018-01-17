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
import com.ponysdk.core.useragent.UserAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final Map<Integer, UIContext> uiContexts = new ConcurrentHashMap<>();

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private final ApplicationManagerOption options;

    private final UserAgent userAgent;

    private static final AtomicInteger applicationCount = new AtomicInteger();

    private final int ID;

    public Application(final int ID, final ApplicationManagerOption options, final UserAgent userAgent) {
        this.ID = ID;
        this.options = options;
        this.userAgent = userAgent;
    }

    public Application(final ApplicationManagerOption options, final UserAgent userAgent) {
        this(applicationCount.incrementAndGet(), options, userAgent);
    }

    public void registerUIContext(final UIContext uiContext) {
        uiContexts.put(uiContext.getID(), uiContext);
    }

    void deregisterUIContext(final int uiContextID) {
        uiContexts.remove(uiContextID);
        if (uiContexts.isEmpty()) {
            //session.invalidate();
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
                log.error("Cannot flush message on the session {}", uiContext, throwable);
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

    public int getId() {
        return ID;
    }

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public int countUIContexts() {
        return uiContexts.size();
    }

    @Override
    public String toString() {
        return "Application [id=" + ID + ", options=" + options + ", userAgent=" + userAgent + "]";
    }

}
