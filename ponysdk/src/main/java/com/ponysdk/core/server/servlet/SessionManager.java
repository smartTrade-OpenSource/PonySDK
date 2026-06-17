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

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.UIContext;

public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final Map<String, Application> applications = new ConcurrentHashMap<>();

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
        listeners.forEach(listener -> listener.onApplicationDestroyed(application));
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

    /**
     * Returns the UIContext with the given id, searching across <strong>all</strong> sessions.
     *
     * @deprecated This performs a global lookup and does <strong>not</strong> verify that the
     *             caller owns the context, which exposes an IDOR (cross-session access) when the
     *             id is attacker-controlled. Prefer {@link #getUIContext(String, int)} scoped to
     *             the caller's HTTP session.
     */
    @Deprecated(forRemoval = true)
    public UIContext getUIContext(final int id) {
        for (final Application app : applications.values()) {
            final UIContext ctx = app.getUIContext(id);
            if (ctx != null) return ctx;
        }
        return null;
    }

    public int countUIContexts() {
        int count = 0;
        for (final Application app : applications.values()) {
            count += app.countUIContexts();
        }
        return count;
    }

}
