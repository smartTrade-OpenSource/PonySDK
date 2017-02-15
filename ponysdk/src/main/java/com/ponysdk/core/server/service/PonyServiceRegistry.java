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

package com.ponysdk.core.server.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PonyServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(PonyServiceRegistry.class);

    private static final Map<Class<?>, PonyService> registeredServices = new ConcurrentHashMap<>();

    public static final void registerPonyService(final PonyService service) {
        final Class<? extends PonyService> registrableService = getGeneralizations(service.getClass());
        final PonyService putIfAbsent = registeredServices.putIfAbsent(registrableService, service);
        if (putIfAbsent != null) throw new IllegalArgumentException("Service " + registrableService + " is already registered");

        if (!registrableService.equals(service.getClass())) {
            registeredServices.putIfAbsent(service.getClass(), service);
        }

        log.info("Service {} registered for the implementation {}", registrableService, service);
    }

    private static final Class<? extends PonyService> getGeneralizations(final Class<?> classObject) {
        final Class<?> superClass = classObject.getSuperclass();

        if (superClass != null && superClass != Object.class) {
            final Class<? extends PonyService> result = getGeneralizations(superClass);
            if (result != null) return result;
        }

        for (final Class<?> clazz : classObject.getInterfaces()) {
            if (clazz.isAssignableFrom(PonyService.class)) {
                return (Class<? extends PonyService>) classObject;
            } else {
                final Class<? extends PonyService> result = getGeneralizations(clazz);
                if (result != null) return result;
            }
        }
        return null;
    }

    public static final <T extends PonyService> T getPonyService(final Class<? extends PonyService> clazz) {
        final T ponyService = (T) registeredServices.get(clazz);
        if (ponyService != null) {
            return ponyService;
        } else {
            throw new IllegalArgumentException("Service not registered #" + clazz.getCanonicalName());
        }
    }
}
