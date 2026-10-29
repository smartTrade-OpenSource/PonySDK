/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.signal;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/** The graph alone: values, computed signals, what is told to whom. Bindings to widgets are {@link BindTest}. */
public class SignalTest {

    private static Observer counting(final AtomicInteger told) {
        return told::incrementAndGet;
    }

    @Test
    public void aSignalIsReadSetAndUpdated() {
        final MutableSignal<Integer> signal = Signal.of(1);
        final AtomicInteger told = new AtomicInteger();
        signal.addObserver(counting(told));

        signal.set(2);
        signal.update(v -> v * 10);

        assertEquals(20, signal.get());
        assertEquals(2, told.get());
        assertEquals("Signal[20]", signal.toString());
    }

    @Test
    public void settingAnEqualValueDoesNothing() {
        final MutableSignal<String> signal = Signal.of("a");
        final AtomicInteger told = new AtomicInteger();
        signal.addObserver(counting(told));

        signal.set(new String("a"));
        signal.update(v -> v);
        final MutableSignal<String> empty = Signal.of(null);
        empty.addObserver(counting(told));
        empty.set(null);

        assertEquals(0, told.get());
    }

    @Test
    public void aComputedSignalComputesWhenFirstReadThenKeepsItsValue() {
        final MutableSignal<Integer> a = Signal.of(1);
        final AtomicInteger computed = new AtomicInteger();
        final Signal<Integer> twice = Signal.computed(() -> {
            computed.incrementAndGet();
            return a.get() * 2;
        });
        assertEquals(0, computed.get(), "lazy");

        assertEquals(2, twice.get());
        assertEquals(2, twice.get());

        assertEquals(1, computed.get(), "cached");
    }

    @Test
    public void aComputedSignalNothingFollowsComputesOnceAfterAnyNumberOfChanges() {
        final MutableSignal<Integer> a = Signal.of(1);
        final AtomicInteger computed = new AtomicInteger();
        final Signal<Integer> twice = Signal.computed(() -> {
            computed.incrementAndGet();
            return a.get() * 2;
        });
        twice.get();

        a.set(2);
        a.set(3);
        a.set(4);

        assertEquals(8, twice.get());
        assertEquals(8, twice.get());
        assertEquals(2, computed.get());
    }

    @Test
    public void aFollowedComputedSignalIsToldOnceAndComputesOnceAfterAnyNumberOfChanges() {
        final MutableSignal<Integer> a = Signal.of(1);
        final AtomicInteger computed = new AtomicInteger();
        final Signal<Integer> twice = Signal.computed(() -> {
            computed.incrementAndGet();
            return a.get() * 2;
        });
        final AtomicInteger told = new AtomicInteger();
        twice.addObserver(counting(told));
        twice.get();

        a.set(2);
        a.set(3);
        a.set(4);

        assertEquals(1, told.get(), "told when it turns dirty, not at each change");
        assertEquals(8, twice.get());
        assertEquals(8, twice.get());
        assertEquals(2, computed.get());
    }

    @Test
    public void aFollowedSignalFollowsBeforeItsFirstReadToo() {
        final MutableSignal<Integer> a = Signal.of(1);
        final Signal<Integer> twice = Signal.computed(() -> a.get() * 2);
        final AtomicInteger told = new AtomicInteger();
        twice.addObserver(counting(told)); // nothing read yet: no source to follow
        assertEquals(2, twice.get());

        a.set(5);

        assertEquals(1, told.get());
        assertEquals(10, twice.get());
    }

    @Test
    public void aDiamondIsComputedOncePerChangeAndNeverShowsAMixOfOldAndNewValues() {
        final MutableSignal<Integer> a = Signal.of(1);
        final AtomicInteger computed = new AtomicInteger();
        final Signal<Integer> plusOne = Signal.computed(() -> a.get() + 1);
        final Signal<Integer> twice = Signal.computed(() -> a.get() * 2);
        final List<Integer> seen = new ArrayList<>();
        final Signal<Integer> sum = Signal.computed(() -> {
            computed.incrementAndGet();
            final int total = plusOne.get() + twice.get();
            seen.add(total);
            return total;
        });
        final AtomicInteger told = new AtomicInteger();
        sum.addObserver(counting(told));
        sum.get();

        a.set(2);
        a.set(3);

        assertEquals(1, told.get());
        assertEquals(10, sum.get()); // 3 + 1 + 3 * 2
        assertEquals(List.of(4, 10), seen, "no value between the two that were set");
        assertEquals(2, computed.get());
    }

    @Test
    public void aComputedSignalWhoseValueDoesNotChangeDoesNotChangeItsReaders() {
        final MutableSignal<Integer> a = Signal.of(1);
        final Signal<Boolean> odd = Signal.computed(() -> a.get() % 2 == 1);
        final AtomicInteger computed = new AtomicInteger();
        final Signal<String> label = Signal.computed(() -> {
            computed.incrementAndGet();
            return odd.get() ? "odd" : "even";
        });
        final AtomicInteger told = new AtomicInteger();
        label.addObserver(counting(told));
        label.get();

        a.set(3); // odd again

        assertEquals("odd", label.get());
        assertEquals(1, computed.get(), "the label saw the same value");
        a.set(4);
        assertEquals("even", label.get());
        assertEquals(2, computed.get());
    }

    @Test
    public void aComputedSignalFollowsWhatItReadLastTime() {
        final MutableSignal<Boolean> useX = Signal.of(true);
        final MutableSignal<String> x = Signal.of("x");
        final MutableSignal<String> y = Signal.of("y");
        final Signal<String> chosen = Signal.computed(() -> useX.get() ? x.get() : y.get());
        final AtomicInteger told = new AtomicInteger();
        chosen.addObserver(counting(told));
        assertEquals("x", chosen.get());

        y.set("y2"); // not read: not followed
        assertEquals(0, told.get());
        useX.set(false);
        assertEquals("y2", chosen.get());
        told.set(0);
        x.set("x2"); // no longer read
        assertEquals(0, told.get());
        y.set("y3");

        assertEquals(1, told.get());
        assertEquals("y3", chosen.get());
    }

    @Test
    public void aComputedSignalReadsManySignalsAndTheSameOneTwice() {
        final List<MutableSignal<Integer>> inputs = new ArrayList<>();
        for (int i = 0; i < 9; i++) inputs.add(Signal.of(i));
        final Signal<Integer> sum = Signal.computed(() -> {
            int total = 0;
            for (final MutableSignal<Integer> input : inputs) total += input.get() + input.get();
            return total;
        });
        assertEquals(72, sum.get());

        inputs.get(8).set(10);

        assertEquals(76, sum.get());
    }

    @Test
    public void aComputedSignalNothingFollowsIsHeldByNoSource() {
        final MutableSignal<Integer> source = Signal.of(1);
        Signal<Integer> derived = Signal.computed(() -> source.get() + 1);
        derived.get();
        final WeakReference<Signal<?>> reference = new WeakReference<>(derived);
        derived = null;

        assertTrue(collected(reference), "a source, long-lived, does not keep what it no longer feeds");
        assertEquals(1, source.get());
    }

    @Test
    public void aSignalThatLosesItsLastFollowerStopsFollowingItsSources() {
        final MutableSignal<Integer> a = Signal.of(1);
        final AtomicInteger computed = new AtomicInteger();
        final Signal<Integer> twice = Signal.computed(() -> {
            computed.incrementAndGet();
            return a.get() * 2;
        });
        final AtomicInteger told = new AtomicInteger();
        final Observer observer = counting(told);
        final Observer other = counting(told);
        twice.addObserver(observer);
        twice.addObserver(other);
        twice.get();

        twice.removeObserver(observer);
        a.set(2);
        assertEquals(1, told.get(), "still followed by the other one");
        twice.removeObserver(other);
        a.set(3);
        assertEquals(1, told.get(), "no longer followed, no longer told");

        assertEquals(6, twice.get(), "yet it notices the changes made meanwhile");
        assertEquals(2, computed.get());
        twice.removeObserver(other); // twice: nothing to undo
    }

    @Test
    public void aSignalFollowedAgainFindsTheChangesMadeWhileItWasNot() {
        final MutableSignal<Integer> a = Signal.of(1);
        final Signal<Integer> twice = Signal.computed(() -> a.get() * 2);
        twice.get();
        a.set(2);
        final AtomicInteger told = new AtomicInteger();

        twice.addObserver(counting(told));

        assertEquals(4, twice.get());
        a.set(3);
        assertEquals(1, told.get());
        assertEquals(6, twice.get());
    }

    @Test
    public void aComputedSignalCannotReadItself() {
        final Signal<?>[] self = new Signal<?>[1];
        self[0] = Signal.computed(() -> self[0].get());

        assertThrows(IllegalStateException.class, self[0]::get);
        assertThrows(IllegalStateException.class, self[0]::get, "and reading it again is no different");
    }

    @Test
    public void aComputedSignalFailingIsComputedAgainAtTheNextRead() {
        final MutableSignal<Integer> a = Signal.of(0);
        final Signal<Integer> inverse = Signal.computed(() -> 100 / a.get());
        final AtomicInteger told = new AtomicInteger();
        inverse.addObserver(counting(told));

        assertThrows(ArithmeticException.class, inverse::get);
        a.set(5);
        assertEquals(20, inverse.get());
        a.set(0);
        assertThrows(ArithmeticException.class, inverse::get);
        a.set(10);
        assertEquals(10, inverse.get());
    }

    @Test
    public void aComputedSignalNothingFollowsFailingIsComputedAgainAtTheNextRead() {
        final MutableSignal<Integer> a = Signal.of(5);
        final Signal<Integer> inverse = Signal.computed(() -> 100 / a.get());
        assertEquals(20, inverse.get());
        a.set(0);

        assertThrows(ArithmeticException.class, inverse::get);
        assertThrows(ArithmeticException.class, inverse::get, "the failure is not taken for a value");
        a.set(4);
        assertEquals(25, inverse.get());
    }

    @Test
    public void aFailureInANestedComputedSignalLeavesTheOuterOneAlone() {
        final MutableSignal<Integer> a = Signal.of(0);
        final Signal<Integer> inner = Signal.computed(() -> 1 / a.get());
        final Signal<Integer> outer = Signal.computed(() -> {
            try {
                return inner.get();
            } catch (final ArithmeticException e) {
                return -1;
            }
        });

        assertEquals(-1, outer.get());
        a.set(1);
        assertEquals(1, outer.get());
    }

    @Test
    public void aFollowedComputedSignalThatFailedIsToldWhatItReadChanges() {
        final MutableSignal<Integer> a = Signal.of(0);
        final Signal<Integer> inverse = Signal.computed(() -> 100 / a.get());
        final AtomicInteger told = new AtomicInteger();
        inverse.addObserver(counting(told));
        assertThrows(ArithmeticException.class, inverse::get); // read before failing: that is what it follows

        a.set(5);

        assertEquals(1, told.get());
        assertEquals(20, inverse.get());
    }

    @Test
    public void whatReadsAComputedSignalComputesAgainWhenItRecoversWithTheValueItHadBeforeFailing() {
        final MutableSignal<Integer> a = Signal.of(1);
        final Signal<Integer> inner = Signal.computed(() -> 10 / a.get());
        final Signal<String> outer = Signal.computed(() -> {
            try {
                return "" + inner.get();
            } catch (final ArithmeticException e) {
                return "failed";
            }
        });
        assertEquals("10", outer.get());

        a.set(0);
        assertEquals("failed", outer.get());
        a.set(1);

        assertEquals("10", outer.get());
    }

    @Test
    public void aChangeMadeWhileAFollowedSignalComputesIsToldNotMissed() {
        final MutableSignal<Integer> a = Signal.of(1);
        final AtomicInteger computed = new AtomicInteger();
        final Signal<Integer> copy = Signal.computed(() -> {
            final int read = a.get();
            if (computed.getAndIncrement() == 0) a.set(2); // another thread's change, between the read and the follow
            return read;
        });
        final AtomicInteger told = new AtomicInteger();
        copy.addObserver(counting(told));

        assertEquals(1, copy.get());

        assertEquals(1, told.get());
        assertEquals(2, copy.get());
    }

    @Test
    public void aComputedSignalNeedsAComputation() {
        assertThrows(NullPointerException.class, () -> Signal.computed(null));
    }

    @Test
    public void updatesFromManyThreadsAreAllCounted() throws InterruptedException {
        final MutableSignal<Integer> counter = Signal.of(0);
        final Signal<Integer> twice = Signal.computed(() -> counter.get() * 2);
        final AtomicInteger told = new AtomicInteger();
        twice.addObserver(counting(told));
        final AtomicInteger odd = new AtomicInteger();
        final int threads = 8, each = 10_000;
        final CountDownLatch start = new CountDownLatch(1);
        final List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            workers.add(Thread.ofPlatform().start(() -> {
                await(start);
                for (int i = 0; i < each; i++) counter.update(v -> v + 1);
            }));
        }
        final Thread reader = Thread.ofPlatform().start(() -> {
            await(start);
            for (int i = 0; i < each; i++) if (twice.get() % 2 != 0) odd.incrementAndGet();
        });

        start.countDown();
        for (final Thread worker : workers) worker.join();
        reader.join();

        assertEquals(0, odd.get());
        assertEquals(threads * each, counter.get());
        assertEquals(2 * threads * each, twice.get());
        assertTrue(told.get() >= 1);
    }

    private static void await(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static boolean collected(final WeakReference<?> reference) {
        for (int i = 0; i < 20 && reference.get() != null; i++) {
            System.gc();
            try {
                Thread.sleep(20);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return reference.get() == null;
    }
}
