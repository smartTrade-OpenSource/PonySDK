
package com.ponysdk.core.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ponysdk.core.Application;
import com.ponysdk.core.tools.ListenerCollection;

public class SessionManager {

	private final Map<String, PSession> sessionsById = new ConcurrentHashMap<>();

	private static SessionManager INSTANCE = new SessionManager();
	private final ListenerCollection<SessionListener> sessionListeners = new ListenerCollection<>();

	public static SessionManager get() {
		return INSTANCE;
	}

	public PSession getSession(final String id) {
		return sessionsById.get(id);
	}

	public Collection<PSession> getSessions() {
		return sessionsById.values();
	}

	public Collection<Application> getApplications() {
		final List<Application> applications = new ArrayList<>();
		for (final PSession session : sessionsById.values()) {
			final Application application = (Application) session.getAttribute(Application.class.getCanonicalName());
			if (application != null)
				applications.add(application);
		}
		return applications;
	}

	public Application getApplication(final PSession session) {
		return (Application) session.getAttribute(Application.class.getCanonicalName());
	}

	public void setApplication(final String id, final Application application) {
		getSession(id).setAttribute(Application.class.getCanonicalName(), application);
	}

	public void registerSessionListener(final SessionListener listener) {
		sessionListeners.register(listener);
	}

	public void unregisterSessionListener(final SessionListener listener) {
		sessionListeners.unregister(listener);
	}

	void registerSession(final PSession session) {
		sessionsById.put(session.getId(), session);
		for (final SessionListener listener : sessionListeners) {
			listener.onSessionCreated(session);
		}
	}

	PSession unregisterSession(final String sessionID) {
		final PSession session = sessionsById.remove(sessionID);
		for (final SessionListener listener : sessionListeners) {
			listener.onSessionDestroyed(session);
		}
		return session;
	}
}
