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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.event.Event.Type;
import com.ponysdk.core.exception.UmbrellaException;

public class SimpleEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(SimpleEventBus.class);

    private final Map<Type<?>, Map<Object, Set<?>>> map = new HashMap<Type<?>, Map<Object, Set<?>>>();

    private final Set<BroadcastEventHandler> broadcastHandlerManager = new HashSet<BroadcastEventHandler>();

    private boolean firing = false;

    private final Queue<Event<? extends EventHandler>> eventQueue = new LinkedList<Event<? extends EventHandler>>();

    private final List<HandlerContext<? extends EventHandler>> pendingHandlerRegistration = new ArrayList<SimpleEventBus.HandlerContext<? extends EventHandler>>();

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
    }

    @Override
    public void addHandler(final BroadcastEventHandler handler) {
        broadcastHandlerManager.add(handler);
    }

    @Override
    public void fireEventFromSource(final Event<? extends EventHandler> event, final Object source) {
        if (event == null) { throw new NullPointerException("Cannot fire null event"); }
        if (source == null) { throw new NullPointerException("Cannot fire from a null source"); }
        doFire(event, source);
    }

    protected <H extends EventHandler> void doRemove(final Type<H> type, final Object source, final H handler) {
        if (!firing) {
            doRemoveNow(type, source, handler);
        } else {
            defferedRemove(type, source, handler);
        }
    }

    private void doRemoveNow(final Type<? extends EventHandler> type, final Object source, final EventHandler handler) {
        final Set<? extends EventHandler> l = getHandlerSet(type, source);

        final boolean removed = l.remove(handler);
        assert removed : "redundant remove call";
        if (removed && l.isEmpty()) {
            prune(type, source);
        }
    }

    private <H extends EventHandler> void defferedRemove(final Type<H> type, final Object source, final H handler) {
        final HandlerContext<H> context = new HandlerContext<H>();
        context.type = type;
        context.source = source;
        context.handler = handler;
        context.add = false;

        final boolean removed = pendingHandlerRegistration.remove(context);

        if (!removed) pendingHandlerRegistration.add(context);
    }

    private <H extends EventHandler> HandlerRegistration doAdd(final Type<H> type, final Object source, final H handler) {
        if (!firing) {
            doAddNow(type, source, handler);
        } else {
            defferedAdd(type, source, handler);
        }

        return new HandlerRegistration() {

            @Override
            public void removeHandler() {
                doRemove(type, source, handler);
            }
        };
    }

    private <H extends EventHandler> void defferedAdd(final Type<H> type, final Object source, final H handler) {
        final HandlerContext<H> context = new HandlerContext<H>();
        context.type = type;
        context.source = source;
        context.handler = handler;
        context.add = true;

        pendingHandlerRegistration.add(context);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void doAddNow(final Type type, final Object source, final Object handler) {
        ensureHandlerSet(type, source).add(handler);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void doFire(final Event<? extends EventHandler> event, final Object source) {
        if (source != null) event.setSource(source);

        eventQueue.add(event);

        if (firing) return;

        firing = true;
        Set<Throwable> causes = null;

        try {

            Event e;

            while ((e = eventQueue.poll()) != null) {

                final Set<? extends EventHandler> handlers = getDispatchSet(e.getAssociatedType(), e.getSource());

                final Iterator<? extends EventHandler> it = handlers.iterator();
                while (it.hasNext()) {
                    try {
                        if (log.isDebugEnabled()) log.debug("dispatch event #" + e);
                        e.dispatch(it.next());
                        for (final BroadcastEventHandler handler : broadcastHandlerManager) {
                            if (log.isDebugEnabled()) log.debug("broadcast event #" + event);
                            handler.onEvent(event);
                        }
                    } catch (final Throwable t) {
                        log.error("Cannot process fired event #" + e.getAssociatedType(), t);
                        if (causes == null) {
                            causes = new HashSet<Throwable>();
                        }
                        causes.add(t);
                    }
                }
            }

            for (final HandlerContext<? extends EventHandler> context : pendingHandlerRegistration) {
                if (context.add) doAddNow(context.type, context.source, context.handler);
                else doRemoveNow(context.type, context.source, context.handler);
            }

            pendingHandlerRegistration.clear();

            if (causes != null) throw new UmbrellaException(causes);
        } finally {
            firing = false;
        }
    }

    private <H extends EventHandler> Set<H> ensureHandlerSet(final Type<H> type, final Object source) {
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

    private <H extends EventHandler> Set<H> getDispatchSet(final Type<H> type, final Object source) {
        final Set<H> directHandlers = getHandlerSet(type, source);
        if (source == null) { return directHandlers; }

        final Set<H> globalHandlers = getHandlerSet(type, null);

        final Set<H> rtn = new LinkedHashSet<H>(directHandlers);
        rtn.addAll(globalHandlers);
        return rtn;
    }

    @Override
    public <H extends EventHandler> Set<H> getHandlerSet(final Type<H> type, final Object source) {
        final Map<Object, Set<?>> sourceMap = map.get(type);
        if (sourceMap == null) { return Collections.emptySet(); }

        // safe, we control the puts.
        @SuppressWarnings("unchecked")
        final Set<H> handlers = (Set<H>) sourceMap.get(source);
        if (handlers == null) { return Collections.emptySet(); }

        return new HashSet<H>(handlers);
    }

    private void prune(final Type<?> type, final Object source) {
        final Map<Object, Set<?>> sourceMap = map.get(type);

        final Set<?> pruned = sourceMap.remove(source);

        assert pruned != null : "Can't prune what wasn't there";
        assert pruned.isEmpty() : "Pruned unempty list!";

        if (sourceMap.isEmpty()) {
            map.remove(type);
        }
    }

    class HandlerContext<H> {

        boolean add;

        Type<H> type;
        Object source;
        H handler;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((handler == null) ? 0 : handler.hashCode());
            result = prime * result + ((source == null) ? 0 : source.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final HandlerContext<H> other = (HandlerContext<H>) obj;
            if (!getOuterType().equals(other.getOuterType())) return false;
            if (handler == null) {
                if (other.handler != null) return false;
            } else if (!handler.equals(other.handler)) return false;
            if (source == null) {
                if (other.source != null) return false;
            } else if (!source.equals(other.source)) return false;
            if (type == null) {
                if (other.type != null) return false;
            } else if (!type.equals(other.type)) return false;
            return true;
        }

        private SimpleEventBus getOuterType() {
            return SimpleEventBus.this;
        }

    }

}