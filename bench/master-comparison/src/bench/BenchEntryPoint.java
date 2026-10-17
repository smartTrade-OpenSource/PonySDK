/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package bench;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;

/**
 * Same source compiled against both versions: a 200-cell grid; every 100 ms, 130 setter calls on 130 distinct
 * cells (100 setText, 30 style toggles), each call changing the cell's state.
 */
public final class BenchEntryPoint implements EntryPoint {

    static final int CELLS = 200, CALLS = 130, TEXTS = 100;
    static final String[] VALUES = new String[4096];
    static final LongAdder calls = new LongAdder();
    static final LongAdder sessions = new LongAdder();
    static final boolean TICKS = Boolean.parseBoolean(System.getProperty("bench.ticks", "true"));
    static final ScheduledExecutorService TICKER = Executors.newScheduledThreadPool(8);

    static {
        final Random r = new Random(42);
        final Set<String> seen = new HashSet<>();
        int i = 0;
        while (i < VALUES.length) {
            final String v = String.format("%.2f", 100 + r.nextInt(5000) / 100.0);
            if (seen.add(v)) VALUES[i++] = v;
        }
    }

    @Override
    public void start(final UIContext uiContext) {
        final PFlowPanel grid = Element.newPFlowPanel();
        final PLabel[] cells = new PLabel[CELLS];
        final boolean[] up = new boolean[CELLS];
        for (int i = 0; i < CELLS; i++) {
            cells[i] = Element.newPLabel(VALUES[i]);
            grid.add(cells[i]);
        }
        PWindow.getMain().add(grid);
        sessions.increment();
        if (!TICKS) return;

        final int[] tick = {0};
        final ScheduledFuture<?> f = TICKER.scheduleAtFixedRate(() -> uiContext.execute(() -> {
            final int k = tick[0]++;
            final int base = k * CALLS;
            for (int j = 0; j < CALLS; j++) {
                final int c = (base + j) % CELLS;
                if (j < TEXTS) {
                    cells[c].setText(VALUES[(c * 31 + k * 7) & 4095]);
                } else if (up[c]) {
                    cells[c].removeStyleName("up");
                    up[c] = false;
                } else {
                    cells[c].addStyleName("up");
                    up[c] = true;
                }
            }
            calls.add(CALLS);
        }), ThreadLocalRandom.current().nextInt(100), 100, TimeUnit.MILLISECONDS);
        uiContext.addContextDestroyListener(ctx -> {
            f.cancel(false);
            sessions.decrement();
        });
    }
}
