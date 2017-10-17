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

package com.ponysdk.core.ui.eventbus2;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private final Map<Class<?>, Set<EventHandler<?>>> handlersByType = new HashMap<>();

    public static final class EventHandler<T> implements Consumer<Object> {

        private final Class<T> type;
        private Consumer<T> function;

        public EventHandler(final Class<T> type) {
            this.type = type;
        }

        public void subscribe(final Consumer<T> function) {
            this.function = function;
        }

        @Override
        public void accept(final Object event) {
            function.accept((T) event);
        }
    }

    private <T> EventHandler<T> register(final Class<T> type) {
        final EventHandler<T> handler = new EventHandler<>(type);
        Set<EventHandler<?>> handlers = handlersByType.get(type);
        if (handlers == null) {
            handlers = new LinkedHashSet<>();
            handlersByType.put(type, handlers);
        }
        handlers.add(handler);

        return handler;
    }

    public <T> EventHandler<T> subscribe(final Class<T> type, final Consumer<T> function) {
        if (type == null) return null;
        final EventHandler<T> handler = register(type);
        handler.subscribe(function);
        return handler;
    }

    public boolean unsubscribe(final EventHandler<?> handler) {
        if (handler == null) return false;
        final Set<EventHandler<?>> handlers = handlersByType.get(handler.type);
        if (handlers != null) {
            final boolean remove = handlers.remove(handler);
            if (handlers.isEmpty()) handlersByType.remove(handler.type);
            return remove;
        } else {
            log.error("No subscribed handlers for {}", handler.type);
            return false;
        }
    }

    public void post(final Object event) {
        if (event == null) return;
        final Set<EventHandler<?>> handlers = handlersByType.get(event.getClass());
        if (handlers != null) handlers.forEach(handler -> handler.accept(event));
        else log.error("No subscribed handlers for {}", event.getClass());
    }

}
