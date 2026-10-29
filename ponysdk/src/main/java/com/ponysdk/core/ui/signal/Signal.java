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

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * A value that changes over time, and whose readers follow it (docs/decisions/0019-signals.md).
 * <pre>
 * final MutableSignal&lt;Double&gt; bid = Signal.of(1.0850);
 * final Signal&lt;String&gt; text = Signal.computed(() -&gt; String.format("%.4f", bid.get()));
 * Bind.text(label, text);
 * feed.onQuote(quote -&gt; bid.set(quote.bid())); // any thread
 * </pre>
 * A signal is read on any thread, and shared by any number of contexts: each context's bindings follow it on its own
 * UI thread. Values are compared with {@link Object#equals}: setting an equal value changes nothing.
 */
public abstract sealed class Signal<T> permits MutableSignal, Computed {

    private static final ThreadLocal<Computed<?>> EVALUATING = new ThreadLocal<>();

    private final CopyOnWriteArrayList<Observer> observers = new CopyOnWriteArrayList<>();

    Signal() {
    }

    /** A signal holding a value, to read and to set. */
    public static <T> MutableSignal<T> of(final T initial) {
        return new MutableSignal<>(initial);
    }

    /**
     * A signal derived from others: {@code compute} reads them with {@link #get()}, and they are the ones it follows,
     * whichever it reads the last time. It runs when read after one of them changed, once however many changed
     * before: lazily, its value cached meanwhile. A computed signal whose value does not change does not change its
     * readers. {@code compute} runs on the thread that reads, and only reads signals.
     */
    public static <T> Signal<T> computed(final Supplier<? extends T> compute) {
        return new Computed<>(compute);
    }

    /** The current value. Inside {@link #computed}, the reading signal follows this one. */
    public abstract T get();

    /** The current value, followed by no one: what a binding applies, outside any computation. */
    abstract T peek();

    /** The number of the current value, brought up to date; it changes with the value. */
    abstract long version();

    void addObserver(final Observer observer) {
        observers.add(observer);
    }

    void removeObserver(final Observer observer) {
        observers.remove(observer);
    }

    final boolean isObserved() {
        return !observers.isEmpty();
    }

    /** Tells the observers the value changed. No allocation: the list is read as it was. */
    final void changed() {
        observers.forEach(Observer::invalidate);
    }

    /** Records a read of this signal, at the version it had, for the computed signal being evaluated. */
    final void track(final long version) {
        final Computed<?> reader = EVALUATING.get();
        if (reader != null) reader.read(this, version);
    }

    static Computed<?> evaluating() {
        return EVALUATING.get();
    }

    /** Set to null, not removed: the next set on this thread would allocate an entry again. */
    static void evaluating(final Computed<?> computed) {
        EVALUATING.set(computed);
    }
}
