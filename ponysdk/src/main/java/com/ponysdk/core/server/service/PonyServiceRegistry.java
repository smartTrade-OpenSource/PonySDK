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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PonyServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(PonyServiceRegistry.class);

    private static final Map<Class<?>, PonyService> registeredServices = new ConcurrentHashMap<>();

    public static void registerPonyService(final PonyService service) {
        final Set<Class<?>> classes = new HashSet<>();

        getGeneralizations(service.getClass(), classes);

        classes.stream().filter(PonyService.class::isAssignableFrom).forEach(clazz -> {
            if (!registeredServices.containsKey(clazz)) {
                registeredServices.put(clazz, service);
                log.info("Service registered #{}", clazz);
            } else {
                throw new IllegalArgumentException("Already registered service #" + clazz);
            }
        });
    }

    private static void getGeneralizations(final Class<?> classObject, final Set<Class<?>> generalizations) {
        final Class<?> superClass = classObject.getSuperclass();

        if (superClass != null) getGeneralizations(superClass, generalizations);

        for (final Class<?> clazz : classObject.getInterfaces()) {
            generalizations.add(classObject);
            getGeneralizations(clazz, generalizations);
        }
    }

    public static <T extends PonyService> T getPonyService(final Class<T> clazz) {
        final T ponyService = (T) registeredServices.get(clazz);
        if (ponyService != null) return ponyService;
        else throw new IllegalArgumentException("Service not registered #" + clazz.getCanonicalName());
    }
}
