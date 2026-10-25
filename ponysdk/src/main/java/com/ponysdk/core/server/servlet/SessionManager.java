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

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ContextKeys;
import com.ponysdk.core.server.application.UIContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final Map<String, Application> applications = new ConcurrentHashMap<>();
    /** The contexts that have keys (embedded terminals), by id, until destroyed. */
    private final Map<Integer, UIContext> keyed = new ConcurrentHashMap<>();

    private final List<ApplicationListener> listeners = new ArrayList<>();

    public static SessionManager get() {
        return INSTANCE;
    }

    public Collection<Application> getApplications() {
        return applications.values();
    }

    public Application getApplication(final String id) {
        return applications.get(id);
    }

    public void registerApplication(final Application application) {
        applications.put(application.getId(), application);
        listeners.forEach(listener -> listener.onApplicationCreated(application));
    }

    public void unregisterApplication(final Application application) {
        applications.remove(application.getId());
        listeners.forEach(listener -> listener.onApplicationDestroyed(application, application.getLastCloseStatusCode()));
    }

    public void addApplicationListener(final ApplicationListener listener) {
        listeners.add(listener);
    }

    /**
     * Returns the {@link UIContext} with the given id <strong>only if</strong> it belongs to the
     * application identified by {@code applicationId} (i.e. the caller's HTTP session).
     * <p>
     * This is the safe lookup to use from HTTP endpoints: it prevents a client from reaching a
     * UIContext owned by another session by guessing its (sequential) id — an IDOR.
     *
     * @param applicationId the caller's application id (its HTTP session id)
     * @param id            the UIContext id
     * @return the UIContext, or {@code null} if no such context exists in that application
     */
    public UIContext getUIContext(final String applicationId, final int id) {
        if (applicationId == null) return null;
        final Application app = applications.get(applicationId);
        return app != null ? app.getUIContext(id) : null;
    }

    /** A context with keys is found by them until it is destroyed (docs/decisions/0009-embedding-limits.md). */
    public void registerKeys(final UIContext uiContext) {
        if (uiContext.getKeys() == null) return;
        keyed.put(uiContext.getID(), uiContext);
        uiContext.addContextDestroyListener(destroyed -> keyed.remove(destroyed.getID(), destroyed));
    }

    /** The context whose key K this is, or null. */
    public UIContext getUIContextByKey(final int id, final String key) {
        final UIContext uiContext = keyed.get(id);
        return uiContext != null && uiContext.getKeys().isKey(key) ? uiContext : null;
    }

    /**
     * The context an HTTP request names, by the caller's session first; then, for an embedded terminal of another
     * site, which sends no cookie, by its key K in the header {@code CONTEXT_KEY}, or, when the request fetches a
     * pending stream by its URL, by its stream key S in the parameter {@code STREAM_KEY}. Null when none: the same
     * answer whether the context is missing or another caller's.
     */
    public UIContext getUIContext(final HttpServletRequest request, final int id, final boolean pendingStream) {
        final HttpSession session = request.getSession(false);
        final UIContext bySession = session != null ? getUIContext(session.getId(), id) : null;
        if (bySession != null) return bySession;
        final UIContext uiContext = keyed.get(id);
        if (uiContext == null) return null;
        final ContextKeys keys = uiContext.getKeys();
        if (keys.isKey(request.getHeader(ClientToServerModel.CONTEXT_KEY.name()))) return uiContext;
        return pendingStream && keys.isStreamKey(request.getParameter(ClientToServerModel.STREAM_KEY.toStringValue()))
                ? uiContext : null;
    }

    public int countUIContexts() {
        int count = 0;
        for (final Application app : applications.values()) {
            count += app.countUIContexts();
        }
        return count;
    }

}
