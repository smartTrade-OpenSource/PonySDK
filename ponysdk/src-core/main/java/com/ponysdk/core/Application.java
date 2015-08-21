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

package com.ponysdk.core;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.servlet.Session;

/**
 * <p>
 * Wrapper of the HTTPSession, and contains the UIContexts.
 * </p>
 */
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final Session session;

    private final Map<Long, UIContext> uiContexts = new ConcurrentHashMap<>();

    private final ApplicationManagerOption options;

    private final String ID;

    private final String name;

    public Application(final String ID, final String name, final Session session, final ApplicationManagerOption options) {
        this.ID = ID;
        this.name = name;
        this.session = session;
        this.options = options;
    }

    void registerUIContext(final UIContext uiContext) {
        uiContexts.put(uiContext.getUiContextID(), uiContext);
    }

    void unregisterUIContext(final long uiContextID) {
        uiContexts.remove(uiContextID);
        if (uiContexts.isEmpty()) {
            log.info("Invalidate session, all ui contexts have been destroyed");
            session.invalidate();
        }
    }

    public UIContext getUIContext(final long uiContextID) {
        return uiContexts.get(uiContextID);
    }

    public Collection<UIContext> getUIContexts() {
        return uiContexts.values();
    }

    public Session getSession() {
        return session;
    }

    public void setAttribute(final String name, final Object value) {
        session.setAttribute(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(final String name) {
        return (T) session.getAttribute(name);
    }

    public ApplicationManagerOption getOptions() {
        return options;
    }

    public String getName() {
        return name;
    }

    public String getID() {
        return ID;
    }
}