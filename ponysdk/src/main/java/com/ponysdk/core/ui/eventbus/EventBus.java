/*
 * Copyright (c) 2011 PonySDK
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

import com.ponysdk.core.util.SetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class EventBus {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    private Set<BroadcastEventHandler> broadcastHandlerManager;

    private final EventSource globalEventSource = new EventSource();

    public HandlerRegistration addHandler(final Event.Type type, final EventHandler handler) {
        return globalEventSource.addHandler(type, handler);
    }

    public boolean removeHandler(final Event.Type type, final EventHandler handler) {
        return globalEventSource.removeHandler(type, handler);
    }

    public void fireEvent(final Event<?> event) {
        if (event == null) throw new NullPointerException("Cannot fire null event");
        doFire(event, null);
    }

    public void fireEventFromSource(final Event<?> event, final EventSource source) {
        if (event == null) throw new NullPointerException("Cannot fire null event");
        if (source == null) throw new NullPointerException("Cannot fire from a null source");
        doFire(event, source);
    }

    private Collection<EventHandler> getIterableHandlers(final EventSource eventSource, final Event.Type eventType) {
        if (eventSource == null) return Collections.emptyList();
        final Collection<EventHandler> handlers = eventSource.getEventHandlers(eventType);
        if (handlers.isEmpty()) return Collections.emptyList();
        return new ArrayList<>(handlers);
    }

    private void doFire(final Event<?> event, final EventSource source) {
        Collection<Throwable> causes = null;
        final Event.Type eventType = event.getAssociatedType();

        final Collection<EventHandler> specificHandlers = getIterableHandlers(source, eventType);
        final Collection<EventHandler> globalHandlers = getIterableHandlers(globalEventSource, eventType);
        final Collection<BroadcastEventHandler> broadcastHandlers = broadcastHandlerManager == null ? null
                : new ArrayList<>(broadcastHandlerManager);

        for (final EventHandler handler : specificHandlers) {
            causes = dispatchEvent(event, eventType, handler, causes);
        }
        for (final EventHandler handler : globalHandlers) {
            causes = dispatchEvent(event, eventType, handler, causes);
        }

        if (broadcastHandlers != null) {
            for (final BroadcastEventHandler handler : broadcastHandlers) {
                log.debug("broadcast eventbus #{}", event);
                try {
                    handler.onEvent(event);
                } catch (final Exception t) {
                    log.error("Cannot broadcast fired eventbus #{}", eventType, t);
                    if (causes == null) causes = new ArrayList<>();
                    causes.add(t);
                }
            }
        }

        if (causes != null) throw new UmbrellaException(causes);
    }

    private Collection<Throwable> dispatchEvent(final Event event, final Event.Type eventType, final EventHandler handler,
                                                Collection<Throwable> causes) {
        try {
            log.debug("dispatch eventbus #{}", event);
            event.dispatch(handler);
        } catch (final Exception t) {
            log.error("Cannot process fired eventbus #{}", eventType, t);
            if (causes == null) causes = new ArrayList<>();
            causes.add(t);
        }
        return causes;
    }

    public void addHandler(final BroadcastEventHandler handler) {
        if (broadcastHandlerManager == null) broadcastHandlerManager = SetUtils.newArraySet(4);
        broadcastHandlerManager.add(handler);
    }

    public void removeHandler(final BroadcastEventHandler handler) {
        if (broadcastHandlerManager == null) return;
        broadcastHandlerManager.remove(handler);
    }

}
