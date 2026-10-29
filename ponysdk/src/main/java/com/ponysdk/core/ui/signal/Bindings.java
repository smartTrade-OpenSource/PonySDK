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

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.ContextDestroyListener;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.concurrent.Conflator;
import com.ponysdk.core.ui.basic.PObject;

/**
 * The bindings of one context, and the {@link Conflator} that runs them. A binding whose signal changed joins the
 * dirty list, once; the conflator's one key, the wake-up, makes the next drain run them all on the UI thread, in one
 * transaction: one flush for every binding that changed since the previous drain (ADR-0013). The list is an array
 * swapped with a spare at each drain: no allocation per binding. The context's destroy ends every binding, whatever
 * the signals they follow outlive.
 */
final class Bindings implements ContextDestroyListener {

    private static final Logger log = LoggerFactory.getLogger(Bindings.class);
    private static final String ATTRIBUTE = "ponysdk.signal.bindings";
    private static final Object WAKE_UP = new Object();

    private final UIContext uiContext;
    private final Conflator<Object, Object> conflator;
    private final Set<Effect<?>> live = new HashSet<>();

    // Guarded by this
    private Effect<?>[] dirty = new Effect<?>[64];
    private Effect<?>[] spare = new Effect<?>[64];
    private int dirtyCount;

    private Bindings(final UIContext uiContext, final Duration minInterval, final Executor executor) {
        this.uiContext = uiContext;
        final Conflator.Builder<Object, Object> builder = Conflator
            .<Object, Object> builder(uiContext, (key, value) -> runDirty()).minInterval(minInterval);
        if (executor != null) builder.executor(executor);
        this.conflator = builder.build();
        uiContext.addContextDestroyListener(this);
    }

    /** The context's bindings, created with the defaults if it has none yet. On the context's UI thread. */
    static Bindings of(final UIContext uiContext) {
        Bindings bindings = uiContext.getAttribute(ATTRIBUTE);
        if (bindings == null) bindings = create(uiContext, Duration.ZERO, null);
        return bindings;
    }

    static void configure(final UIContext uiContext, final Duration minInterval, final Executor executor) {
        if (uiContext.getAttribute(ATTRIBUTE) != null) throw new IllegalStateException("The context has bindings already");
        create(uiContext, minInterval, executor);
    }

    private static Bindings create(final UIContext uiContext, final Duration minInterval, final Executor executor) {
        final Bindings bindings = new Bindings(uiContext, minInterval, executor);
        uiContext.setAttribute(ATTRIBUTE, bindings);
        return bindings;
    }

    <T> Binding bind(final PObject widget, final Signal<T> signal, final Consumer<? super T> apply, final Runnable onClose) {
        final Effect<T> effect = new Effect<>(this, widget, signal, apply, onClose);
        synchronized (this) {
            live.add(effect);
        }
        try {
            effect.start();
        } catch (final RuntimeException e) {
            effect.close(); // the first application failed: no binding is left behind
            throw e;
        }
        return effect;
    }

    /** From the thread that changed the effect's signal. */
    void offer(final Effect<?> effect) {
        final boolean first;
        synchronized (this) {
            if (effect.queued) return; // another thread queued it meanwhile
            effect.queued = true;
            if (dirtyCount == dirty.length) dirty = Arrays.copyOf(dirty, dirtyCount * 2);
            dirty[dirtyCount++] = effect;
            first = dirtyCount == 1; // a later one rides the drain the first one asked for
        }
        if (first) conflator.offer(WAKE_UP, WAKE_UP);
    }

    /** On the UI thread: the bindings that changed since the previous drain. One that fails does not stop the others. */
    private void runDirty() {
        final Effect<?>[] batch;
        final int count;
        synchronized (this) {
            batch = dirty;
            count = dirtyCount;
            dirty = spare;
            dirtyCount = 0;
            // before the reads: a change from now on queues its binding again
            for (int i = 0; i < count; i++) batch[i].queued = false;
        }
        for (int i = 0; i < count; i++) {
            try {
                batch[i].run();
            } catch (final RuntimeException e) {
                log.error("Cannot apply a binding on UIContext #{}", uiContext.getID(), e);
            }
        }
        Arrays.fill(batch, 0, count, null);
        synchronized (this) {
            spare = batch;
        }
    }

    synchronized void forget(final Effect<?> effect) {
        live.remove(effect);
    }

    @Override
    public void onBeforeDestroy(final UIContext uiContext) {
        final List<Effect<?>> all;
        synchronized (this) {
            all = List.copyOf(live);
        }
        all.forEach(Effect::close);
        synchronized (this) {
            Arrays.fill(dirty, 0, dirtyCount, null);
            dirtyCount = 0;
        }
    }
}
