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
 */package com.ponysdk.core;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class PonyApplicationSession {

    private final HttpSession httpSession;

    private final Map<Long, PonySession> ponySessions = new ConcurrentHashMap<Long, PonySession>();

    private final Set<HttpSessionListener> sessionListeners = new HashSet<HttpSessionListener>();

    private static final AtomicLong ponySessionIDcount = new AtomicLong();

    public PonyApplicationSession(HttpSession httpSession) {
        this.httpSession = httpSession;
    }

    public long registerPonySession(PonySession ponySession) {
        final long id = ponySessionIDcount.incrementAndGet();
        ponySessions.put(id, ponySession);
        return id;
    }

    public PonySession getPonySession(long key) {
        return ponySessions.get(key);
    }

    public HttpSession getHttpSession() {
        return httpSession;
    }

    public void fireSessionCreated(HttpSessionEvent event) {
        for (final HttpSessionListener listener : sessionListeners) {
            listener.sessionCreated(event);
        }
    }

    public void fireSessionDestroyed(HttpSessionEvent event) {
        for (final HttpSessionListener listener : sessionListeners) {
            listener.sessionDestroyed(event);
        }
    }

    public void addSessionListener(HttpSessionListener sessionListener) {
        sessionListeners.add(sessionListener);
    }

    public boolean removeSessionListener(HttpSessionListener sessionListener) {
        return sessionListeners.remove(sessionListeners);
    }

    public void setAttribute(String name, Object value) {
        httpSession.setAttribute(name, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String name) {
        return (T) httpSession.getAttribute(name);
    }

}