
package com.ponysdk.core.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ponysdk.core.Application;
import com.ponysdk.core.tools.ListenerCollection;

public class SessionManager {

    private final Map<String, Session> sessionsById = new ConcurrentHashMap<>();

    private static SessionManager INSTANCE = new SessionManager();
    private final ListenerCollection<SessionListener> sessionListeners = new ListenerCollection<>();

    public static SessionManager get() {
        return INSTANCE;
    }

    public Session getSession(final String id) {
        return sessionsById.get(id);
    }

    public Collection<Session> getSessions() {
        return sessionsById.values();
    }

    public Collection<Application> getApplications() {
        final List<Application> applications = new ArrayList<>();
        for (final Session session : sessionsById.values()) {
            final Application application = (Application) session.getAttribute(Application.class.getCanonicalName());
            if (application != null) applications.add(application);
        }
        return applications;
    }

    public Application getApplication(final Session session) {
        return (Application) session.getAttribute(Application.class.getCanonicalName());
    }

    public void registerSessionListener(final SessionListener listener) {
        sessionListeners.register(listener);
    }

    public void unregisterSessionListener(final SessionListener listener) {
        sessionListeners.unregister(listener);
    }

    void registerSession(final Session session) {
        sessionsById.put(session.getId(), session);
        for (final SessionListener listener : sessionListeners) {
            listener.sessionCreated(session);
        }
    }

    Session unregisterSession(final String sessionID) {
        final Session session = sessionsById.remove(sessionID);
        for (final SessionListener listener : sessionListeners) {
            listener.sessionDestroyed(session);
        }
        return session;
    }
}
