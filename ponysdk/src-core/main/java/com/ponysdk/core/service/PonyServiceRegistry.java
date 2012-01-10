
package com.ponysdk.core.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PonyServiceRegistry {

    private static Map<Class<?>, PonyService> registeredServices = new ConcurrentHashMap<Class<?>, PonyService>();

    public static void registerPonyService(PonyService service) {
        Class<?>[] classes = service.getClass().getInterfaces();
        for (Class<?> clazz : classes) {
            if (clazz.isAssignableFrom(PonyService.class)) {
                registeredServices.put(clazz, service);
            }
        }
    }

    public static <T extends PonyService> T getPonyService(final Class<T> clazz) throws Exception {
        T ponyService = (T) registeredServices.get(clazz);
        if (ponyService == null) throw new Exception("Service not registered for the class #" + clazz.getCanonicalName());
        return ponyService;
    }
}
