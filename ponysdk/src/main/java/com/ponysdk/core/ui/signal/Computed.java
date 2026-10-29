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

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * A signal computed from others, lazily. Two ways to know whether it is up to date:
 * <ul>
 * <li>while something follows it, it follows its sources: a change marks it dirty (and tells its followers once),
 * and a clean computed signal is read without a look at its sources;</li>
 * <li>otherwise it compares the versions of its sources with those it computed from, so that a computed signal
 * nothing follows is held by no source.</li>
 * </ul>
 * Either way it computes once per read after any number of changes, and a recomputation that gives an equal value
 * keeps its version: what follows it does not recompute. Locks are taken from a signal to its sources, never the other
 * way: a change only sets atomics.
 */
final class Computed<T> extends Signal<T> implements Observer {

    private static final Signal<?>[] NO_SOURCES = new Signal<?>[0];

    private final Supplier<? extends T> compute;
    private final AtomicBoolean dirty = new AtomicBoolean(true);

    // Guarded by this
    private T value;
    private long version;
    private boolean evaluated;
    private boolean evaluating;
    private boolean following; // its sources have it as an observer
    private Signal<?>[] sources = NO_SOURCES;
    private long[] seen = new long[0]; // the version of each source at the last computation
    private Signal<?>[] reading = new Signal<?>[4]; // the sources read by the computation under way, in order
    private long[] readingSeen = new long[4];
    private int reads;

    Computed(final Supplier<? extends T> compute) {
        this.compute = Objects.requireNonNull(compute, "compute");
    }

    @Override
    public synchronized T get() {
        checkNotEvaluating();
        try {
            refresh();
        } finally {
            track(version); // a failure is a state of this signal too: whoever reads it follows it out of there
        }
        return value;
    }

    @Override
    synchronized long version() {
        checkNotEvaluating();
        refresh();
        return version;
    }

    @Override
    public void invalidate() {
        if (dirty.compareAndSet(false, true)) changed();
    }

    /** Called by the signals the computation under way reads, on its thread, inside {@link #refresh}. */
    void read(final Signal<?> source, final long sourceVersion) {
        for (int i = 0; i < reads; i++) if (reading[i] == source) return; // read twice: the first version, the oldest
        if (reads == reading.length) {
            reading = Arrays.copyOf(reading, reads * 2);
            readingSeen = Arrays.copyOf(readingSeen, reads * 2);
        }
        reading[reads] = source;
        readingSeen[reads++] = sourceVersion;
    }

    @Override
    synchronized void addObserver(final Observer observer) {
        super.addObserver(observer);
        if (following) return;
        following = true;
        for (final Signal<?> source : sources) source.addObserver(this);
        dirty.set(true); // a change between the last computation and now was not told
    }

    @Override
    synchronized void removeObserver(final Observer observer) {
        super.removeObserver(observer);
        if (!following || isObserved()) return;
        following = false;
        for (final Signal<?> source : sources) source.removeObserver(this);
    }

    private void checkNotEvaluating() {
        if (evaluating) throw new IllegalStateException("A computed signal reads itself");
    }

    private void refresh() {
        if (evaluated) {
            if (following && !dirty.get()) return;
            dirty.set(false); // before looking: a change from now on marks it again
            if (!isStale()) return;
        } else {
            dirty.set(false);
        }
        recompute();
    }

    private boolean isStale() {
        for (int i = 0; i < sources.length; i++) if (changed(sources[i], seen[i])) return true;
        return false;
    }

    /** A source that fails to compute has changed: what reads it computes again, and may handle the failure. */
    private static boolean changed(final Signal<?> source, final long seenVersion) {
        try {
            return source.version() != seenVersion;
        } catch (final RuntimeException e) {
            return true;
        }
    }

    private void recompute() {
        final Computed<?> outer = evaluating();
        T next = null;
        RuntimeException failure = null;
        reads = 0;
        evaluating = true;
        evaluating(this);
        try {
            try {
                next = compute.get();
            } catch (final RuntimeException e) {
                failure = e;
            } finally {
                evaluating = false;
                evaluating(outer);
            }
            follow(); // what was read before a failure too: a change of it is a reason to try again
        } finally {
            Arrays.fill(reading, 0, reads, null); // no reference kept in the scratch
        }
        if (failure != null) {
            evaluated = false; // the next value is a change, equal or not to the one before the failure
            version++;
            throw failure;
        }
        if (!evaluated || !Objects.equals(value, next)) {
            value = next;
            version++;
        }
        evaluated = true;
    }

    /** Keeps the sources the computation read, with their versions; an observer on each while it is followed. */
    private void follow() {
        if (reads == sources.length && Arrays.equals(reading, 0, reads, sources, 0, reads)) {
            System.arraycopy(readingSeen, 0, seen, 0, reads);
            return;
        }
        if (following) {
            for (final Signal<?> source : sources) if (indexOf(reading, reads, source) < 0) source.removeObserver(this);
            for (int i = 0; i < reads; i++) {
                if (indexOf(sources, sources.length, reading[i]) >= 0) continue;
                reading[i].addObserver(this);
                if (changed(reading[i], readingSeen[i])) invalidate(); // between its read and now: told, not missed
            }
        }
        sources = Arrays.copyOf(reading, reads);
        seen = Arrays.copyOf(readingSeen, reads);
    }

    private static int indexOf(final Signal<?>[] signals, final int length, final Signal<?> signal) {
        for (int i = 0; i < length; i++) if (signals[i] == signal) return i;
        return -1;
    }
}
