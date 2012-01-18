
package com.ponysdk.core.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PonyServiceRegistry {

    private static final Logger log = LoggerFactory.getLogger(PonyServiceRegistry.class);

    private static Map<Class<?>, PonyService> registeredServices = new ConcurrentHashMap<Class<?>, PonyService>();

    public static void registerPonyService(PonyService service) {

        Set<Class<?>> classes = new HashSet<Class<?>>();

        getGeneralizations(service.getClass(), classes);

        for (Class<?> clazz : classes) {
            if (PonyService.class.isAssignableFrom(clazz)) {
                registeredServices.put(clazz, service);
                log.info("Registering impl " + service + " for service : " + clazz);
            }
        }
    }

    public static void getGeneralizations(Class<?> classObject, Set<Class<?>> generalizations) {

        Class<?> superClass = classObject.getSuperclass();

        if (superClass != null) getGeneralizations(superClass, generalizations);

        for (Class<?> clazz : classObject.getInterfaces()) {
            generalizations.add(classObject);
            getGeneralizations(clazz, generalizations);
        }
    }

    public static <T extends PonyService> T getPonyService(final Class<T> clazz) throws Exception {
        @SuppressWarnings("unchecked")
        T ponyService = (T) registeredServices.get(clazz);
        if (ponyService == null) throw new Exception("Service not registered for the class #" + clazz.getCanonicalName());
        return ponyService;
    }
}
