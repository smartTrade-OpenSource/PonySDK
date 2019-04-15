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

import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.context.UIContextImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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

    public Application register(String appID, Supplier<Application> supplier) {
        return applications.computeIfAbsent(appID, id -> {
            Application application = supplier.get();
            fireApplicationCreated();
            return application;
        });
    }

    public void unregister(final Application application) {
        applications.remove(application.getId());
        listeners.forEach(listener -> listener.onApplicationDestroyed(application));
    }

    private void fireApplicationCreated(Application application) {
        listeners.forEach(listener -> listener.onApplicationCreated(application));
    }

    public void addApplicationListener(final ApplicationListener listener) {
        listeners.add(listener);
    }

    public UIContextImpl getUIContext(final Integer id) {
        return applications.values().stream().map(app -> app.getUIContext(id)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    public int countUIContexts() {
        return applications.values().stream().mapToInt(Application::countUIContexts).sum();
    }

}
