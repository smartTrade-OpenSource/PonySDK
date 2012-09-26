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

package com.ponysdk.core.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ponysdk.core.event.Event.Type;

public class SimpleEventBus implements EventBus {

    /**
     * Map of event type to map of event source to list of their handlers.
     */
    private final Map<Type<?>, Map<Object, Set<?>>> map = new HashMap<Type<?>, Map<Object, Set<?>>>();

    private final Set<BroadcastEventHandler> broadcastHandlerManager = new HashSet<BroadcastEventHandler>();

    @Override
    public <H extends EventHandler> HandlerRegistration addHandler(final Type<H> type, final H handler) {
        if (type == null) { throw new NullPointerException("Cannot add a handler with a null type"); }
        if (handler == null) { throw new NullPointerException("Cannot add a null handler"); }

        return doAdd(type, null, handler);
    }

    @Override
    public <H extends EventHandler> HandlerRegistration addHandlerToSource(final Type<H> type, final Object source, final H handler) {
        if (type == null) { throw new NullPointerException("Cannot add a handler with a null type"); }
        if (source == null) { throw new NullPointerException("Cannot add a handler with a null source"); }
        if (handler == null) { throw new NullPointerException("Cannot add a null handler"); }

        return doAdd(type, source, handler);
    }

    @Override
    public void fireEvent(final Event<?> event) {
        if (event == null) { throw new NullPointerException("Cannot fire null event"); }
        doFire(event, null);
        fireBroadcastEvent(event);
    }

    @Override
    public void addHandler(final BroadcastEventHandler handler) {
        broadcastHandlerManager.add(handler);
    }

    private void fireBroadcastEvent(final Event<?> event) {
        for (final BroadcastEventHandler handler : broadcastHandlerManager) {
            handler.onEvent(event);
        }
    }

    @Override
    public void fireEventFromSource(final Event<?> event, final Object source) {
        if (event == null) { throw new NullPointerException("Cannot fire null event"); }
        if (source == null) { throw new NullPointerException("Cannot fire from a null source"); }
        doFire(event, source);
    }

    protected <H extends EventHandler> void doRemove(final Event.Type<H> type, final Object source, final H handler) {
        doRemoveNow(type, source, handler);
    }

    private <H extends EventHandler> HandlerRegistration doAdd(final Event.Type<H> type, final Object source, final H handler) {
        return doAddNow(type, source, handler);
    }

    private <H extends EventHandler> HandlerRegistration doAddNow(final Event.Type<H> type, final Object source, final H handler) {
        final Set<H> l = ensureHandlerSet(type, source);
        l.add(handler);

        return new HandlerRegistration() {

            @Override
            public void removeHandler() {
                doRemove(type, source, handler);
            }
        };
    }

    private <H extends EventHandler> void doFire(final Event<H> event, final Object source) {
        if (source != null) {
            event.setSource(source);
        }

        final Set<H> handlers = getDispatchSet(event.getAssociatedType(), source);

        final Iterator<H> it = handlers.iterator();
        while (it.hasNext()) {
            final H handler = it.next();

            try {
                event.dispatch(handler);
            } catch (final Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private <H> void doRemoveNow(final Event.Type<H> type, final Object source, final H handler) {
        final Set<H> l = getHandlerSet(type, source);

        final boolean removed = l.remove(handler);
        assert removed : "redundant remove call";
        if (removed && l.isEmpty()) {
            prune(type, source);
        }
    }

    private <H> Set<H> ensureHandlerSet(final Event.Type<H> type, final Object source) {
        Map<Object, Set<?>> sourceMap = map.get(type);
        if (sourceMap == null) {
            sourceMap = new HashMap<Object, Set<?>>();
            map.put(type, sourceMap);
        }

        // safe, we control the puts.
        @SuppressWarnings("unchecked")
        Set<H> handlers = (Set<H>) sourceMap.get(source);
        if (handlers == null) {
            handlers = new LinkedHashSet<H>();
            sourceMap.put(source, handlers);
        }

        return handlers;
    }

    private <H> Set<H> getDispatchSet(final Event.Type<H> type, final Object source) {
        final Set<H> directHandlers = getHandlerSet(type, source);
        if (source == null) { return directHandlers; }

        final Set<H> globalHandlers = getHandlerSet(type, null);

        final Set<H> rtn = new LinkedHashSet<H>(directHandlers);
        rtn.addAll(globalHandlers);
        return rtn;
    }

    public <H> Set<H> getHandlerSet(final Event.Type<H> type, final Object source) {
        final Map<Object, Set<?>> sourceMap = map.get(type);
        if (sourceMap == null) { return Collections.emptySet(); }

        // safe, we control the puts.
        @SuppressWarnings("unchecked")
        final Set<H> handlers = (Set<H>) sourceMap.get(source);
        if (handlers == null) { return Collections.emptySet(); }

        return new HashSet<H>(handlers);
    }

    private void prune(final Event.Type<?> type, final Object source) {
        final Map<Object, Set<?>> sourceMap = map.get(type);

        final Set<?> pruned = sourceMap.remove(source);

        assert pruned != null : "Can't prune what wasn't there";
        assert pruned.isEmpty() : "Pruned unempty list!";

        if (sourceMap.isEmpty()) {
            map.remove(type);
        }
    }
}