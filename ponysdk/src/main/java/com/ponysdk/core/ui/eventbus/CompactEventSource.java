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

import java.util.Collection;
import java.util.Collections;

/**
 * Ultra-compact EventSource for widgets with very few handler types (typically 1-3).
 * <p>
 * Uses a flat Object[] array instead of HashMap + HashSet, saving ~200-400 bytes
 * per widget compared to TinyEventSource. Layout: [Type0, Handler0, Type1, Handler1, ...].
 * When a type has multiple handlers, subsequent entries share the same Type key.
 * <p>
 * This is optimal for the common case where a widget listens to 1-3 event types
 * with 1 handler each. For widgets with many handler types, the linear scan is
 * still fast because the array is small.
 */
public class CompactEventSource extends EventSource {

    /**
     * Flat array: [Type, EventHandler, Type, EventHandler, ...]
     * Null means no handlers registered yet.
     */
    private Object[] data;
    private int size; // number of (Type, Handler) pairs

    public CompactEventSource() {
        // Don't call super() with map allocation — we override everything
        super();
    }

    @Override
    public HandlerRegistration addHandler(final Event.Type type, final EventHandler handler) {
        if (type == null) throw new NullPointerException("Cannot add a handler with a null type");
        if (handler == null) throw new NullPointerException("Cannot add a null handler");

        if (data == null) {
            data = new Object[4]; // room for 2 pairs
        } else if (size * 2 >= data.length) {
            final Object[] newData = new Object[data.length * 2];
            System.arraycopy(data, 0, newData, 0, size * 2);
            data = newData;
        }

        data[size * 2] = type;
        data[size * 2 + 1] = handler;
        size++;

        return () -> removeHandler(type, handler);
    }

    @Override
    public boolean removeHandler(final Event.Type type, final EventHandler handler) {
        if (data == null) return false;
        for (int i = 0; i < size * 2; i += 2) {
            if (data[i].equals(type) && data[i + 1] == handler) {
                // Shift remaining entries left
                final int remaining = (size - 1) * 2 - i;
                if (remaining > 0) {
                    System.arraycopy(data, i + 2, data, i, remaining);
                }
                size--;
                data[size * 2] = null;
                data[size * 2 + 1] = null;
                return true;
            }
        }
        return false;
    }

    @Override
    public Collection<EventHandler> getEventHandlers(final Event.Type eventType) {
        if (data == null || size == 0) return Collections.emptyList();

        // Count matches first to avoid list allocation when possible
        int count = 0;
        int firstMatch = -1;
        for (int i = 0; i < size * 2; i += 2) {
            if (data[i].equals(eventType)) {
                if (count == 0) firstMatch = i;
                count++;
            }
        }

        if (count == 0) return Collections.emptyList();
        if (count == 1) return Collections.singletonList((EventHandler) data[firstMatch + 1]);

        // Multiple handlers for this type — build a list
        final java.util.ArrayList<EventHandler> result = new java.util.ArrayList<>(count);
        for (int i = 0; i < size * 2; i += 2) {
            if (data[i].equals(eventType)) {
                result.add((EventHandler) data[i + 1]);
            }
        }
        return result;
    }
}
