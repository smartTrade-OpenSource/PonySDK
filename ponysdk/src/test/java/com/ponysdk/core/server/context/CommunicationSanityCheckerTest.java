/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;

/** The roundtrip probe: every 5 s, or every heartbeat period when shorter, the first check included. */
public class CommunicationSanityCheckerTest {

    private static int probesIn10Checks(final long heartBeatPeriodMs) {
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setHeartBeatPeriod(heartBeatPeriodMs, TimeUnit.MILLISECONDS);
        final UIContext uiContext = Mockito.mock(UIContext.class);
        Mockito.when(uiContext.getConfiguration()).thenReturn(configuration);
        final CommunicationSanityChecker checker = new CommunicationSanityChecker(uiContext);
        for (int i = 0; i < 10; i++) {
            final long now = System.currentTimeMillis();
            Mockito.when(uiContext.getLastReceivedTime()).thenReturn(now);
            Mockito.when(uiContext.getLastSentTime()).thenReturn(now);
            checker.checkCommunicationState();
        }
        return Mockito.mockingDetails(uiContext).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("sendRoundTrip")).mapToInt(invocation -> 1).sum();
    }

    @Test
    public void probesEveryFiveSeconds() {
        assertEquals(2, probesIn10Checks(5000));
        assertEquals(2, probesIn10Checks(30000));
    }

    /** A checker on a mocked context, at the given clock, the client's last frame read from received[0]. */
    private static CommunicationSanityChecker checker(final UIContext uiContext, final long[] now, final long[] received) {
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setHeartBeatPeriod(5000, TimeUnit.MILLISECONDS);
        Mockito.when(uiContext.getConfiguration()).thenReturn(configuration);
        Mockito.when(uiContext.getLastReceivedTime()).thenAnswer(i -> received[0]);
        Mockito.when(uiContext.getLastSentTime()).thenAnswer(i -> now[0]); // the server keeps sending
        final CommunicationSanityChecker checker = new CommunicationSanityChecker(uiContext);
        checker.clock = () -> now[0];
        return checker;
    }

    /** Pinged after half a period of silence; dead after two periods, though the server kept sending. */
    @Test
    public void aSilentClientIsPingedThenDeclaredDead() {
        final UIContext uiContext = Mockito.mock(UIContext.class);
        final long[] now = { 100_000 };
        final long[] received = { 100_000 };
        final CommunicationSanityChecker checker = checker(uiContext, now, received);

        now[0] += 2_000;
        checker.checkCommunicationState();
        Mockito.verify(uiContext, Mockito.never()).ping();
        now[0] += 600; // 2.6 s of silence
        checker.checkCommunicationState();
        Mockito.verify(uiContext, Mockito.times(1)).ping();
        now[0] += 2_400; // 5 s: suspect
        checker.checkCommunicationState();
        now[0] += 4_000; // 9 s
        checker.checkCommunicationState();
        Mockito.verify(uiContext, Mockito.never()).connectionDead();
        now[0] += 1_000; // 10 s: dead
        checker.checkCommunicationState();
        Mockito.verify(uiContext, Mockito.times(1)).connectionDead();
        Mockito.clearInvocations(uiContext);
        now[0] += 1_000;
        checker.checkCommunicationState();
        Mockito.verify(uiContext, Mockito.never()).ping();
        Mockito.verify(uiContext, Mockito.never()).connectionDead();
    }

    /** A pong, or any message, clears the suspicion. */
    @Test
    public void aPongClearsTheSuspicion() {
        final UIContext uiContext = Mockito.mock(UIContext.class);
        final long[] now = { 100_000 };
        final long[] received = { 100_000 };
        final CommunicationSanityChecker checker = checker(uiContext, now, received);

        now[0] += 5_000; // suspect
        checker.checkCommunicationState();
        now[0] += 1_000;
        received[0] = now[0]; // the browser answered the ping
        checker.checkCommunicationState();
        now[0] += 4_500;
        checker.checkCommunicationState();
        Mockito.verify(uiContext, Mockito.never()).connectionDead();
    }

    @Test
    public void probesEveryHeartbeatPeriodWhenShorter() {
        assertEquals(5, probesIn10Checks(2000));
        assertEquals(10, probesIn10Checks(500));
    }

    /** A check that throws is logged: the periodic task keeps running until stopped. */
    @Test
    public void aFailingCheckDoesNotStopTheChecker() {
        final UIContext uiContext = Mockito.mock(UIContext.class);
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        Mockito.when(uiContext.getConfiguration()).thenReturn(configuration);
        Mockito.when(uiContext.isSuspended()).thenThrow(new IllegalStateException("broken"));
        final CommunicationSanityChecker checker = new CommunicationSanityChecker(uiContext);

        checker.start();
        try {
            assertTrue(checker.started.get());
            Mockito.verify(uiContext, Mockito.timeout(5_000).atLeast(2)).isSuspended();
        } finally {
            checker.stop();
        }
        assertFalse(checker.started.get());
    }

    @Test
    public void aZeroHeartbeatNeverStarts() {
        final UIContext uiContext = Mockito.mock(UIContext.class);
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setHeartBeatPeriod(0);
        Mockito.when(uiContext.getConfiguration()).thenReturn(configuration);
        final CommunicationSanityChecker checker = new CommunicationSanityChecker(uiContext);

        checker.start();
        assertFalse(checker.started.get());
        checker.stop();
        assertFalse(checker.started.get());
    }

    @Test
    public void aSuspendedContextIsNotChecked() {
        final UIContext uiContext = Mockito.mock(UIContext.class);
        final long[] now = { 100_000 };
        final long[] received = { 100_000 };
        final CommunicationSanityChecker checker = checker(uiContext, now, received);
        Mockito.when(uiContext.isSuspended()).thenReturn(true);

        now[0] += 20_000;
        checker.checkCommunicationState();
        checker.checkCommunicationState();

        Mockito.verify(uiContext, Mockito.never()).ping();
        Mockito.verify(uiContext, Mockito.never()).sendRoundTrip();
        Mockito.verify(uiContext, Mockito.never()).connectionDead();
    }

    @Test
    public void aSecondStartKeepsTheRunningChecker() {
        final UIContext uiContext = Mockito.mock(UIContext.class);
        Mockito.when(uiContext.getConfiguration()).thenReturn(new ApplicationConfiguration());
        final CommunicationSanityChecker checker = new CommunicationSanityChecker(uiContext);
        final int before = CommunicationSanityChecker.sanityCheckerTimer.getQueue().size();

        checker.start();
        checker.start();
        assertTrue(checker.started.get());
        checker.stop();

        assertFalse(checker.started.get());
        assertEquals(before, CommunicationSanityChecker.sanityCheckerTimer.getQueue().size(), "no second task left behind");
    }
}
