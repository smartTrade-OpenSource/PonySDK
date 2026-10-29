/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.servlet.SessionManager;

public class DrainTest {

    private Application application;
    private final List<Integer> moved = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        application = new Application("drain", Mockito.mock(HttpSession.class), new ApplicationConfiguration());
        SessionManager.get().registerApplication(application);
    }

    @AfterEach
    public void tearDown() {
        SessionManager.get().unregisterApplication(application);
    }

    /** A live context that is gone once moved, unless it lingers (its client never answers). */
    private UIContext context(final int id, final boolean lingers) {
        final UIContext context = Mockito.mock(UIContext.class);
        final AtomicBoolean alive = new AtomicBoolean(true);
        Mockito.when(context.getID()).thenReturn(id);
        Mockito.when(context.isAlive()).thenAnswer(inv -> alive.get());
        Mockito.doAnswer(inv -> {
            synchronized (moved) {
                moved.add(id);
            }
            if (!lingers) alive.set(false);
            return null;
        }).when(context).moveByAdministrator();
        application.registerUIContext(context);
        return context;
    }

    @Test
    public void nothingToMoveEndsAtOnce() throws InterruptedException {
        final long start = System.nanoTime();
        assertEquals(0, Drain.run(Duration.ofSeconds(10)));
        assertTrue(System.nanoTime() - start < Duration.ofSeconds(1).toNanos());
    }

    @Test
    public void thePagesGoOneByOneOverMostOfTheWindow() throws InterruptedException {
        context(1, false);
        context(2, false);
        context(3, false);
        final long start = System.nanoTime();
        assertEquals(3, Drain.run(Duration.ofMillis(600)));
        final long elapsed = Duration.ofNanos(System.nanoTime() - start).toMillis();
        assertEquals(List.of(1, 2, 3), moved.stream().sorted().toList());
        assertTrue(elapsed >= 300 && elapsed < 480, "two paces of 160 ms between three, none after the last: " + elapsed);
    }

    @Test
    public void theLastPageMovedTheDrainEnds() throws InterruptedException {
        context(1, false);
        final long start = System.nanoTime();
        assertEquals(1, Drain.run(Duration.ofSeconds(10)));
        assertTrue(System.nanoTime() - start < Duration.ofSeconds(1).toNanos(), "no pace after the last");
    }

    @Test
    public void aPageThatDoesNotGoIsWaitedForUntilTheWindowEnds() throws InterruptedException {
        context(1, true);
        final long start = System.nanoTime();
        assertEquals(1, Drain.run(Duration.ofMillis(200)));
        assertTrue(System.nanoTime() - start >= Duration.ofMillis(200).toNanos());
        assertEquals(List.of(1), moved, "moved once, not again");
    }

    @Test
    public void noWindowMovesNothing() throws InterruptedException {
        context(1, false);
        assertEquals(0, Drain.run(Duration.ZERO));
        assertEquals(List.of(), moved);
    }

    @Test
    public void anInterruptedDrainSaysSo() {
        context(1, false);
        context(2, false);
        Thread.currentThread().interrupt();
        assertThrows(InterruptedException.class, () -> Drain.run(Duration.ofSeconds(10)));
    }
}
