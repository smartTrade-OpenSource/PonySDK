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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.ponysdk.core.ui.basic.PObject;

/**
 * A signal's value applied to a widget, on the UI thread. The signal tells the effect it changed, on the thread that
 * changed it: the effect then asks its context for one run, however many changes come before it, and reads the
 * signal when it runs, so it shows the latest value and a computed signal has computed once.
 */
final class Effect<T> implements Observer, Binding {

    private final Bindings owner;
    private final PObject widget;
    private final Signal<T> signal;
    private final Consumer<? super T> apply;
    private final Runnable onClose; // what the application's handlers need removed, or null
    private final PObject.DestroyListener destroyListener = object -> close();
    private final AtomicBoolean queued = new AtomicBoolean();
    private volatile boolean closed;

    Effect(final Bindings owner, final PObject widget, final Signal<T> signal, final Consumer<? super T> apply,
           final Runnable onClose) {
        this.owner = owner;
        this.widget = widget;
        this.signal = signal;
        this.apply = apply;
        this.onClose = onClose;
    }

    /** Follows the signal and shows its current value. A change between the two is run again: nothing is missed. */
    void start() {
        widget.addDestroyListener(destroyListener);
        signal.addObserver(this);
        run();
    }

    @Override
    public void invalidate() {
        if (queued.compareAndSet(false, true)) owner.offer(this);
    }

    /** On the UI thread. */
    void run() {
        queued.set(false); // before the read: a change from now on asks again
        if (!closed) apply.accept(signal.get());
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        signal.removeObserver(this);
        widget.removeDestroyListener(destroyListener);
        owner.forget(this);
        if (onClose != null) onClose.run();
    }
}
