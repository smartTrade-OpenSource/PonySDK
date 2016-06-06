
package com.ponysdk.core.server.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.tools.ListenerCollection;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionManager {

    private final Map<String, HttpSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, Application> applicationsBySessionID = new ConcurrentHashMap<>();

    private static SessionManager INSTANCE = new SessionManager();
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
