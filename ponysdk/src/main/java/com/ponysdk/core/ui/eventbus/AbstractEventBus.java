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

package com.ponysdk.core.ui.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(AbstractEventBus.class);

    protected final Set<BroadcastEventHandler> broadcastHandlerManager = new HashSet<>();

    protected final Queue<Event<? extends EventHandler>> eventQueue = new LinkedList<>();
    protected final List<HandlerContext> pendingHandlerRegistration = new ArrayList<>();
    protected boolean firing = false;

    @Override
    public HandlerRegistration addHandler(final Event.Type type, final EventHandler handler) {
        if (type == null) throw new NullPointerException("Cannot add a handler with a null type");
        else if (handler == null) throw new NullPointerException("Cannot add a null handler");
        else return doAdd(type, null, handler);
    }

    @Override
    public HandlerRegistration addHandlerToSource(final Event.Type type, final Object source, final EventHandler handler) {
        if (type == null) throw new NullPointerException("Cannot add a handler with a null type");
        else if (source == null) throw new NullPointerException("Cannot add a handler with a null source");
        else if (handler == null) throw new NullPointerException("Cannot add a null handler");
        else return doAdd(type, source, handler);
    }

    protected HandlerRegistration doAdd(final Event.Type type, final Object source, final EventHandler handler) {
        if (!firing) doAddNow(type, source, handler);
        else defferedAdd(type, source, handler);

        return () -> doRemove(type, source, handler);
    }

    protected abstract void doAddNow(final Event.Type type, final Object source, final EventHandler handler);

    private void defferedAdd(final Event.Type type, final Object source, final EventHandler handler) {
        final HandlerContext context = new HandlerContext();
        context.type = type;
        context.source = source;
        context.handler = handler;
        context.add = true;

        pendingHandlerRegistration.add(context);
    }

    @Override
    public void addHandler(final BroadcastEventHandler handler) {
        broadcastHandlerManager.add(handler);
    }

    @Override
    public void removeHandler(final Event.Type type, final EventHandler handler) {
        doRemove(type, null, handler);
    }

    @Override
    public void removeHandlerFromSource(final Event.Type type, final Object source, final EventHandler handler) {
        doRemove(type, source, handler);
    }

    protected void doRemove(final Event.Type type, final Object source, final EventHandler handler) {
        if (!firing) doRemoveNow(type, source, handler);
        else defferedRemove(type, source, handler);
    }

    protected abstract void doRemoveNow(final Event.Type type, final Object source, final EventHandler handler);

    protected void defferedRemove(final Event.Type type, final Object source, final EventHandler handler) {
        final HandlerContext context = new HandlerContext();
        context.type = type;
        context.source = source;
        context.handler = handler;
        context.add = false;

        final boolean removed = pendingHandlerRegistration.remove(context);

        if (!removed) pendingHandlerRegistration.add(context);
    }

    @Override
    public void removeHandler(final BroadcastEventHandler handler) {
        broadcastHandlerManager.remove(handler);
    }

    @Override
    public void fireEvent(final Event<? extends EventHandler> event) {
        if (event == null) throw new NullPointerException("Cannot fire null eventbus");
        else doFire(event, null);
    }

    @Override
    public void fireEventFromSource(final Event<? extends EventHandler> event, final Object source) {
        if (event == null) throw new NullPointerException("Cannot fire null eventbus");
        else if (source == null) throw new NullPointerException("Cannot fire from a null source");
        else doFire(event, source);
    }

    protected void doFire(final Event<? extends EventHandler> event, final Object source) {
        if (source != null) event.setSource(source);

        eventQueue.add(event);

        if (firing) return;

        firing = true;
        Set<Throwable> causes = null;

        try {

            Event e;

            while ((e = eventQueue.poll()) != null) {

                final Collection<? extends EventHandler> handlers = getDispatchSet(e.getAssociatedType(), e.getSource());

                for (final EventHandler handler1 : handlers) {
                    try {
                        if (log.isDebugEnabled()) log.debug("dispatch eventbus #" + e);
                        e.dispatch(handler1);
                    } catch (final Throwable t) {
                        log.error("Cannot process fired eventbus #" + e.getAssociatedType(), t);
                        if (causes == null) {
                            causes = new HashSet<>();
                        }
                        causes.add(t);
                    }
                }

                for (final BroadcastEventHandler handler : broadcastHandlerManager) {
                    if (log.isDebugEnabled()) log.debug("broadcast eventbus #" + e);
                    handler.onEvent(e);
                }
            }

            for (final HandlerContext context : pendingHandlerRegistration) {
                if (context.add) doAddNow(context.type, context.source, context.handler);
                else doRemoveNow(context.type, context.source, context.handler);
            }

            pendingHandlerRegistration.clear();

            if (causes != null) throw new UmbrellaException(causes);
        } finally {
            firing = false;
        }
    }

    private Collection<EventHandler> getDispatchSet(final Event.Type type, final Object source) {
        final Collection<EventHandler> directHandlers = getHandlers(type, source);
        if (source != null) {
            final Collection<EventHandler> globalHandlers = getHandlers(type, null);
            final Set<EventHandler> rtn = new LinkedHashSet<>(directHandlers);
            rtn.addAll(globalHandlers);
            return rtn;
        } else {
            return directHandlers;
        }
    }

    protected static class HandlerContext {

        boolean add;

        Event.Type type;
        Object source;
        EventHandler handler;

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            else if (o == null || getClass() != o.getClass()) return false;
            else {
                final HandlerContext that = (HandlerContext) o;
                return add == that.add && Objects.equals(type, that.type) && Objects.equals(source, that.source)
                        && Objects.equals(handler, that.handler);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(add, type, source, handler);
        }
    }

}
