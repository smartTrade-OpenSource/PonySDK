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

package com.ponysdk.core.server.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.tools.ListenerCollection;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final Map<String, HttpSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, Application> applicationsBySessionID = new ConcurrentHashMap<>();
    private final ListenerCollection<HttpSessionListener> sessionListeners = new ListenerCollection<>();

    public static SessionManager get() {
        return INSTANCE;
    }

    public HttpSession getSession(final String id) {
        return sessionsById.get(id);
    }

    public Collection<HttpSession> getSessions() {
        return sessionsById.values();
    }

    public Collection<Application> getApplications() {
        final List<Application> applications = new ArrayList<>();
        for (final HttpSession session : sessionsById.values()) {
            final Application application = (Application) session.getAttribute(Application.class.getCanonicalName());
            if (application != null) applications.add(application);
        }
        return applications;
    }

    public Application getApplication(final HttpSession session) {
        if (session == null) return null;
        return getApplication(session.getId());
    }

    public Application getApplication(final String sessionID) {
        return applicationsBySessionID.get(sessionID);
    }

    public void setApplication(final String sessionID, final Application application) {
        applicationsBySessionID.put(sessionID, application);
    }

    public void registerSessionListener(final HttpSessionListener listener) {
        sessionListeners.register(listener);
    }

    public void unregisterSessionListener(final HttpSessionListener listener) {
        sessionListeners.unregister(listener);
    }

    void registerSession(final HttpSession session) {
        sessionsById.put(session.getId(), session);
        sessionListeners.forEach(listener -> listener.sessionCreated(new HttpSessionEvent(session)));
    }

    HttpSession unregisterSession(final String sessionID) {
        final HttpSession session = sessionsById.remove(sessionID);
        sessionListeners.forEach(listener -> listener.sessionDestroyed(new HttpSessionEvent(session)));
        return session;
    }
}
