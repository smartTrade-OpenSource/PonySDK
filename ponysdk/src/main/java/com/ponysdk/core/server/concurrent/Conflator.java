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

package com.ponysdk.core.server.concurrent;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.server.application.ContextDestroyListener;
import com.ponysdk.core.server.application.UIContext;

/**
 * Values offered from any thread, applied on a context's UI thread, the latest per key
 * (docs/decisions/0013-conflated-streams.md).
 * <pre>
 * final Conflator&lt;String, Quote&gt; quotes = Conflator.of(uiContext, (instrument, quote) -&gt; rows.get(instrument).show(quote));
 * feed.subscribe(quote -&gt; quotes.offer(quote.instrument(), quote)); // any thread
 * </pre>
 * {@link #offer} never blocks: it replaces the key's pending value. A drain applies every pending value in one
 * transaction, one flush, one drain at a time: while it waits for a slow client, the values offered meanwhile replace
 * each other. Nothing is applied while the context is suspended; the context's destroy closes the conflator.
 */
public final class Conflator<K, V> {

    private static final Logger log = LoggerFactory.getLogger(Conflator.class);

    /** A virtual thread per drain: one waiting for its client holds no platform thread. */
    private static final Executor VIRTUAL = command -> Thread.ofVirtual().name("Conflator").start(command);
    /** How often a suspended context is looked at again. */
    private static final long SUSPENDED_RECHECK_MS = 100;

    private final UIContext uiContext;
    private final BiConsumer<K, V> apply;
    private final Executor executor;
    private final long minIntervalNanos;
    private final ContextDestroyListener closeOnDestroy = context -> close();
    private final List<Flow.Subscription> subscriptions = new CopyOnWriteArrayList<>();

    // Guarded by this
    private LinkedHashMap<K, V> pending = new LinkedHashMap<>();
    private LinkedHashMap<K, V> spare = new LinkedHashMap<>(); // the previous drain's map, cleared: two maps, no allocation
    private boolean draining;
    private boolean closed;
    private long offered;
    private long applied;
    private long lastDrainNanos;

    private Conflator(final Builder<K, V> builder) {
        this.uiContext = builder.uiContext;
        this.apply = builder.apply;
        this.executor = builder.executor;
        this.minIntervalNanos = builder.minInterval.toNanos();
        this.lastDrainNanos = System.nanoTime() - minIntervalNanos;
        if (UIContext.get() == uiContext) uiContext.addContextDestroyListener(closeOnDestroy);
        else if (!uiContext.execute(() -> uiContext.addContextDestroyListener(closeOnDestroy))) closed = true;
    }

    /** A conflator applying on the context's UI thread, drained on a virtual thread, as often as the client takes. */
    public static <K, V> Conflator<K, V> of(final UIContext uiContext, final BiConsumer<K, V> apply) {
        return builder(uiContext, apply).build();
    }

    public static <K, V> Builder<K, V> builder(final UIContext uiContext, final BiConsumer<K, V> apply) {
        return new Builder<>(uiContext, apply);
    }

    /**
     * Replaces the key's pending value, from any thread, without waiting.
     *
     * @return false once closed: the value is dropped
     */
    public boolean offer(final K key, final V value) {
        final long delay;
        synchronized (this) {
            if (closed) return false;
            pending.put(key, value);
            offered++;
            if (draining) return true;
            draining = true;
            delay = delayNanos();
        }
        schedule(delay);
        return true;
    }

    /**
     * A subscriber that offers each item under its key. It requests without limit, offering never blocks; its
     * publisher's completion or error closes this conflator, and closing it cancels the subscription.
     */
    public Flow.Subscriber<V> subscriber(final Function<? super V, ? extends K> keyOf) {
        Objects.requireNonNull(keyOf, "keyOf");
        return new Flow.Subscriber<>() {

            @Override
            public void onSubscribe(final Flow.Subscription subscription) {
                subscriptions.add(subscription);
                if (isClosed()) subscription.cancel();
                else subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final V item) {
                offer(keyOf.apply(item), item);
            }

            @Override
            public void onError(final Throwable throwable) {
                log.warn("The stream of {} failed: closed", Conflator.this, throwable);
                close();
            }

            @Override
            public void onComplete() {
                close();
            }
        };
    }

    /** Stops: what is pending is dropped, the subscriptions cancelled. Called by the context's destroy. */
    public void close() {
        synchronized (this) {
            if (closed) return;
            closed = true;
            pending.clear();
        }
        subscriptions.forEach(Flow.Subscription::cancel);
        subscriptions.clear();
        if (uiContext.isAlive()) uiContext.execute(() -> uiContext.removeContextDestroyListener(closeOnDestroy));
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    /** The values offered since the creation. */
    public synchronized long getOffered() {
        return offered;
    }

    /** The values applied since the creation: the others were replaced by a later value of their key, or dropped. */
    public synchronized long getApplied() {
        return applied;
    }

    private long delayNanos() {
        return Math.max(0, lastDrainNanos + minIntervalNanos - System.nanoTime());
    }

    private void schedule(final long delayNanos) {
        final Executor target = delayNanos <= 0 ? executor : CompletableFuture.delayedExecutor(delayNanos, TimeUnit.NANOSECONDS, executor);
        target.execute(this::drain);
    }

    private void drain() {
        final LinkedHashMap<K, V> batch;
        synchronized (this) {
            if (closed) return;
            if (uiContext.isSuspended()) {
                // Applied now, the values would fill the recording buffer: they wait, conflated, for the reconnection
                CompletableFuture.delayedExecutor(SUSPENDED_RECHECK_MS, TimeUnit.MILLISECONDS, executor).execute(this::drain);
                return;
            }
            batch = pending;
            pending = spare;
            lastDrainNanos = System.nanoTime();
        }
        uiContext.execute(() -> batch.forEach(this::applyOne));
        if (!uiContext.isAlive()) close();
        final long delay;
        synchronized (this) {
            applied += batch.size();
            batch.clear();
            spare = batch;
            if (closed || pending.isEmpty()) {
                draining = false;
                return;
            }
            delay = delayNanos();
        }
        schedule(delay);
    }

    private void applyOne(final K key, final V value) {
        try {
            apply.accept(key, value);
        } catch (final RuntimeException e) {
            log.error("Cannot apply the value of {} on UIContext #{}", key, uiContext.getID(), e);
        }
    }

    @Override
    public String toString() {
        return "Conflator of UIContext #" + uiContext.getID();
    }

    public static final class Builder<K, V> {

        private final UIContext uiContext;
        private final BiConsumer<K, V> apply;
        private Executor executor = VIRTUAL;
        private Duration minInterval = Duration.ZERO;

        private Builder(final UIContext uiContext, final BiConsumer<K, V> apply) {
            this.uiContext = Objects.requireNonNull(uiContext, "uiContext");
            this.apply = Objects.requireNonNull(apply, "apply");
        }

        /** Where the drains run; a virtual thread each by default. A drain waits there for a slow client. */
        public Builder<K, V> executor(final Executor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        /** The least time between the starts of two drains; none by default. */
        public Builder<K, V> minInterval(final Duration minInterval) {
            if (minInterval.isNegative()) throw new IllegalArgumentException("A negative interval: " + minInterval);
            this.minInterval = minInterval;
            return this;
        }

        public Conflator<K, V> build() {
            return new Conflator<>(this);
        }
    }
}
