/*
 * Copyright (c) 2026 PonySDK
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

package com.ponysdk.core.ui.signal;

import java.util.Objects;
import java.util.function.UnaryOperator;

/** A {@link Signal} that is set: from any thread, the readers following it. */
public final class MutableSignal<T> extends Signal<T> {

    private volatile T value;
    private volatile long version;

    MutableSignal(final T initial) {
        this.value = initial;
    }

    @Override
    public T get() {
        final long read = version; // before the value: a change in between makes a reader look again, never miss one
        final T current = value;
        track(read);
        return current;
    }

    /** Replaces the value; nothing happens if it is equal to the current one. */
    public void set(final T next) {
        if (store(next)) changed();
    }

    /** Replaces the value with a function of the current one, atomically. {@code update} must not block. */
    public void update(final UnaryOperator<T> update) {
        if (storeUpdated(update)) changed();
    }

    @Override
    long version() {
        return version;
    }

    private synchronized boolean store(final T next) {
        if (Objects.equals(value, next)) return false;
        value = next;
        version++;
        return true;
    }

    private synchronized boolean storeUpdated(final UnaryOperator<T> update) {
        return store(update.apply(value));
    }

    @Override
    public String toString() {
        return "Signal[" + value + "]";
    }
}
