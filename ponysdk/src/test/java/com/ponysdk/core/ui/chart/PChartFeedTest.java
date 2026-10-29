/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.ui.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.server.stm.TxnContext;
import com.ponysdk.core.server.websocket.UpgradeRequestData;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.ui.basic.PWindow;

/**
 * The feed of a chart (docs/decisions/0026-chart.md): values offered from another thread, conflated on the context's
 * thread. A real context, not {@link com.ponysdk.test.PSuite}'s spy: the conflator's drain executes on it.
 */
public class PChartFeedTest {

    @Test
    public void theValuesOfAFeedAreConflatedIntoOnePointPerFlush() throws InterruptedException {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setHeartBeatPeriod(0, TimeUnit.SECONDS);
        config.setStringDictionaryEnabled(false);
        final Application application = new Application("chart", Mockito.mock(HttpSession.class), config);
        SessionManager.get().registerApplication(application);
        final List<Object[]> points = new ArrayList<>();
        final WebSocket socket = Mockito.mock(WebSocket.class);
        Mockito.when(socket.getUpgradeRequestData()).thenReturn(new UpgradeRequestData(Map.of(), "test", Mockito.mock(HttpSession.class)));
        Mockito.doAnswer(call -> {
            if (call.getArgument(0) == ServerToClientModel.PADDON_ARGUMENTS) {
                synchronized (points) {
                    points.add(call.getArgument(1));
                }
            }
            return null;
        }).when(socket).encode(Mockito.any(), Mockito.any());
        final TxnContext txnContext = new TxnContext(socket);
        txnContext.setApplication(application);
        final UIContext context = new UIContext(socket, txnContext, config);
        application.registerUIContext(context);
        try {
            final PChart[] chart = new PChart[1];
            context.execute(() -> {
                chart[0] = PChart.builder(PChart.Type.LINE, "A", "B").decimals(3).interval(Duration.ofMillis(200)).build();
                chart[0].attach(PWindow.getMain());
            });

            for (int i = 1; i <= 1_000; i++) chart[0].offer(0, i / 1000.0);
            await(() -> chart[0].getRows() >= 1 && lastUnits(points) == 1000);
            chart[0].offer(1, 5.0);
            await(() -> chart[0].getRows() >= 2);
            Thread.sleep(300);

            assertTrue(chart[0].getRows() <= 3, "1,001 offers, " + chart[0].getRows() + " points");
            synchronized (points) {
                long a = 0;
                long b = 0;
                for (final Object[] point : points) {
                    if (point[1] != null) a += (Long) point[1];
                    if (point[2] != null) b += (Long) point[2];
                }
                assertEquals(1000, a, "A ends at the last offered value, 1.000");
                assertEquals(5000, b, "B at 5.000");
            }
            final long before = chart[0].getRows();
            chart[0].offer(0, 2.0);
            await(() -> chart[0].getRows() > before);
            synchronized (points) {
                final Object[] last = points.get(points.size() - 1);
                assertEquals(1000L, last[1], "A moved from 1.000 to 2.000");
                assertNull(last[2], "B did not move: nothing sent for it");
            }
        } finally {
            context.destroy();
            SessionManager.get().unregisterApplication(application);
            UIContext.remove();
        }
    }

    private static long lastUnits(final List<Object[]> points) {
        synchronized (points) {
            long a = 0;
            for (final Object[] point : points) if (point[1] != null) a += (Long) point[1];
            return a;
        }
    }

    private static void await(final BooleanSupplier condition) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            assertTrue(System.nanoTime() < deadline, "not reached in 5 s");
            Thread.sleep(5);
        }
    }
}
