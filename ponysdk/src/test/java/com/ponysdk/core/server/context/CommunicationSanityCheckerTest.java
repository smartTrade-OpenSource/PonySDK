/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.context;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
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

    @Test
    public void probesEveryHeartbeatPeriodWhenShorter() {
        assertEquals(5, probesIn10Checks(2000));
        assertEquals(10, probesIn10Checks(500));
    }
}
