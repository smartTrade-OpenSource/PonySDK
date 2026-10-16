/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.eventbus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.ponysdk.core.util.SetPool;

/** The compact per-widget handler storage keeps the contract of {@link EventSource}; pooled sets join once. */
public class CompactEventSourceTest {

    private static final Event.Type A = new Event.Type();
    private static final Event.Type B = new Event.Type();

    @Test
    public void handlersPerTypeInOrder() {
        final CompactEventSource s = new CompactEventSource();
        final EventHandler h1 = new EventHandler() {};
        final EventHandler h2 = new EventHandler() {};
        final EventHandler h3 = new EventHandler() {};
        s.addHandler(A, h1);
        s.addHandler(B, h2);
        s.addHandler(A, h3);
        assertEquals(List.of(h1, h3), List.copyOf(s.getEventHandlers(A)));
        assertEquals(List.of(h2), List.copyOf(s.getEventHandlers(B)));
    }

    @Test
    public void addingAHandlerTwiceRegistersItOnce() {
        final CompactEventSource s = new CompactEventSource();
        final EventHandler h = new EventHandler() {};
        s.addHandler(A, h);
        s.addHandler(A, h);
        assertEquals(1, s.getEventHandlers(A).size());
        assertTrue(s.removeHandler(A, h));
        assertTrue(s.getEventHandlers(A).isEmpty());
    }

    @Test
    public void registrationRemovesTheHandler() {
        final CompactEventSource s = new CompactEventSource();
        final EventHandler h = new EventHandler() {};
        s.addHandler(A, h).removeHandler();
        assertTrue(s.getEventHandlers(A).isEmpty());
        assertFalse(s.removeHandler(A, h));
    }

    @Test
    public void plainEventSourceWithoutHandlerAllocatesNothingAndAnswersEmpty() {
        final EventSource s = new EventSource();
        assertTrue(s.getEventHandlers(A).isEmpty());
        assertFalse(s.removeHandler(A, new EventHandler() {}));
    }

    @Test
    public void pooledSetJoinsOnceAndShares() {
        final SetPool<String> pool = new SetPool<>();
        final SetPool<String>.ImmutableSet ab = pool.emptyImmutableSet().getAdd("a").getAdd("b");
        assertEquals("a b", ab.join());
        assertSame(ab.join(), pool.emptyImmutableSet().getAdd("a").getAdd("b").join());
        assertEquals("", pool.emptyImmutableSet().join());
    }
}
