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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.ui.eventbus.Event.Type;

public class SimpleEventBus extends AbstractEventBus {

    private static final Logger log = LoggerFactory.getLogger(SimpleEventBus.class);

    private final Map<Type<?>, Map<Object, Set<?>>> map = new HashMap<>();

    @Override
    protected void doRemoveNow(final Type<? extends EventHandler> type, final Object source, final EventHandler handler) {
        final Map<Object, Set<?>> sourceMap = map.get(type);
        if (sourceMap == null) return;

        final Set<?> handlers = sourceMap.get(source);
        if (handlers == null) return;

        final boolean removed = handlers.remove(handler);
        if (!removed) log.warn("Useless remove call : {}", handler);

        if (removed && handlers.isEmpty()) prune(type, source);
    }

    @Override
    protected void doAddNow(final Type type, final Object source, final Object handler) {
        ensureHandlerSet(type, source).add(handler);
    }

    private <H extends EventHandler> Set<H> ensureHandlerSet(final Type<H> type, final Object source) {
        Map<Object, Set<?>> sourceMap = map.get(type);
        if (sourceMap == null) {
            sourceMap = new HashMap<>();
            map.put(type, sourceMap);
        }

        // safe, we control the puts.
        Set<H> handlers = (Set<H>) sourceMap.get(source);
        if (handlers == null) {
            handlers = new LinkedHashSet<>();
            sourceMap.put(source, handlers);
        }

        return handlers;
    }

    @Override
    public <H extends EventHandler> Collection<H> getHandlers(final Type<H> type, final Object source) {
        final Map<Object, Set<?>> sourceMap = map.get(type);
        if (sourceMap == null) return Collections.emptySet();

        // safe, we control the puts.
        final Set<H> handlers = (Set<H>) sourceMap.get(source);
        if (handlers != null) return new HashSet<>(handlers);
        else return Collections.emptySet();
    }

    private void prune(final Type<?> type, final Object source) {
        final Map<Object, Set<?>> sourceMap = map.get(type);

        final Set<?> pruned = sourceMap.remove(source);

        if (pruned != null) {
            if (!pruned.isEmpty() && log.isInfoEnabled()) log.info("Pruned unempty list! {}", pruned);
            if (sourceMap.isEmpty()) map.remove(type);
        } else {
            if (log.isInfoEnabled()) log.info("Can't prune what wasn't there {}", source);
        }
    }

}
