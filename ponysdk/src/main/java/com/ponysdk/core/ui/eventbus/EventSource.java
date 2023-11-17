/*
 * Copyright (c) 2019 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.core.ui.eventbus;

import java.util.*;

public class EventSource {

    private final Map<Event.Type, Set<EventHandler>> handlers;

    public EventSource() {
        this.handlers = newHandlersMap();
    }

    protected Set<EventHandler> newHandlersSet() {
        return new HashSet<>();
    }

    protected Map<Event.Type, Set<EventHandler>> newHandlersMap() {
        return new HashMap<>();
    }

    public HandlerRegistration addHandler(final Event.Type type, final EventHandler handler) {
        if (type == null) throw new NullPointerException("Cannot add a handler with a null type");
        if (handler == null) throw new NullPointerException("Cannot add a null handler");
        handlers.computeIfAbsent(type, t -> newHandlersSet()).add(handler);
        return () -> removeHandler(type, handler);
    }

    public boolean removeHandler(final Event.Type type, final EventHandler handler) {
        final Set<EventHandler> typeHandlers = handlers.get(type);
        if (typeHandlers == null) return false;
        return typeHandlers.remove(handler);
    }

    public Collection<EventHandler> getEventHandlers(final Event.Type eventType) {
        return handlers.getOrDefault(eventType, Collections.emptySet());
    }
}
