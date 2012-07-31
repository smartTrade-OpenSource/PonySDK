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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.ponysdk.core.servlet.Session;
import com.ponysdk.core.servlet.SessionListener;

public class Application {

    private final Session session;

    private final Map<Long, UIContext> ponySessions = new ConcurrentHashMap<Long, UIContext>();

    private final Set<SessionListener> sessionListeners = new HashSet<SessionListener>();

    private static final AtomicLong ponySessionIDcount = new AtomicLong();

    public Application(final Session session) {
        this.session = session;
    }

    public long registerUIContext(final UIContext ponySession) {
        final long id = ponySessionIDcount.incrementAndGet();
        ponySessions.put(id, ponySession);
        return id;
    }

    public UIContext getUIContext(final long key) {
        return ponySessions.get(key);
    }

    public Session getSession() {
        return session;
    }

    public void fireSessionCreated() {
        for (final SessionListener listener : sessionListeners) {
            listener.sessionCreated(session);
        }
    }

    public void fireSessionDestroyed() {
        for (final SessionListener listener : sessionListeners) {
            listener.sessionDestroyed(session);
        }
    }

    public void addSessionListener(final SessionListener sessionListener) {
        sessionListeners.add(sessionListener);
    }

    public boolean removeSessionListener(final SessionListener sessionListener) {
        return sessionListeners.remove(sessionListeners);
    }

    public void setAttribute(final String name, final Object value) {
        session.setAttribute(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(final String name) {
        return (T) session.getAttribute(name);
    }

}