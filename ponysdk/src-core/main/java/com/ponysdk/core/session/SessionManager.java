
package com.ponysdk.core.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ponysdk.core.servlet.Session;

public class SessionManager {

    private final Map<String, Session> sessionsById = new ConcurrentHashMap<String, Session>();

    public void registerSession(final Session session) {
        sessionsById.put(session.getId(), session);
    }

    public Session unregisterSession(final Session session) {
        return sessionsById.remove(session.getId());
    }

    public Session getSession(final String id) {
        return sessionsById.get(id);
    }

}
