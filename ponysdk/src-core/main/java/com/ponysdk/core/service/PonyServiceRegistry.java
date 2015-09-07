
package com.ponysdk.core.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PonyServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(PonyServiceRegistry.class);

    private static Map<Class<?>, PonyService> registeredServices = new ConcurrentHashMap<>();

    public static void registerPonyService(final PonyService service) {

        final Set<Class<?>> classes = new HashSet<>();

        getGeneralizations(service.getClass(), classes);

        for (final Class<?> clazz : classes) {
            if (PonyService.class.isAssignableFrom(clazz)) {
                registeredServices.put(clazz, service);
                log.info("Service registered #" + clazz);
            }
        }
    }

    public static void getGeneralizations(final Class<?> classObject, final Set<Class<?>> generalizations) {
        final Class<?> superClass = classObject.getSuperclass();

        if (superClass != null) getGeneralizations(superClass, generalizations);

        for (final Class<?> clazz : classObject.getInterfaces()) {
            generalizations.add(classObject);
            getGeneralizations(clazz, generalizations);
        }
    }

    public static <T extends PonyService> T getPonyService(final Class<T> clazz) {
        @SuppressWarnings("unchecked")
        final T ponyService = (T) registeredServices.get(clazz);
        if (ponyService == null) throw new RuntimeException("Service not registered #" + clazz.getCanonicalName());
        return ponyService;
    }
}
