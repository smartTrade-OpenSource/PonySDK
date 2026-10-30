/*
 * Copyright (c) 2026 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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
package com.ponysdk.core.ui.datagrid2.view;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.writer.ModelWriter;

/**
 * Waits for the draw of an action on a grid. A draw is scheduled on the timer and the row updates notify on their
 * own: the grid is first let settle, so a late draw of the previous action is not taken for this one, and again
 * after the first draw, so the test reads the last one.
 */
final class GridDraws {

    static final long TIMEOUT_MS = 3_000;
    /** A grid that has not drawn for this long is taken as settled. */
    private static final long SETTLE_MS = 50;

    /** The writer the context's spy gives, the one of the grid a test builds now. */
    private static final AtomicReference<ModelWriter> WRITER = new AtomicReference<>();
    private static UIContext stubbed;

    private GridDraws() {
    }

    /**
     * The grid built next writes to this writer. The suite's context is a spy shared by the tests of a class, and a
     * grid's data arrives on another thread: a draw of the previous test's grid can call the spy while a stubbing of it
     * is under way, which Mockito does not support (UnfinishedStubbingException, once in a while on CI). The spy is
     * stubbed once, to read this reference; then a grid only sets it.
     */
    static void useWriter(final ModelWriter writer) {
        WRITER.set(writer);
        final UIContext context = UIContext.get();
        if (stubbed == context) return;
        Mockito.doAnswer(invocation -> WRITER.get()).when(context).getWriter();
        stubbed = context;
    }

    static void drawAfter(final DataGridView<?, ?> grid, final Runnable action) throws InterruptedException {
        settle(grid);
        final CountDownLatch latch = new CountDownLatch(1);
        final DataGridView.DrawListener listener = count -> latch.countDown();
        grid.addDrawListener(listener);
        try {
            action.run();
            Assertions.assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "Timed out waiting for grid draw");
        } finally {
            grid.removeDrawListener(listener);
        }
        settle(grid);
    }

    /** Waits until the grid has not drawn for SETTLE_MS. */
    static void settle(final DataGridView<?, ?> grid) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS);
        while (System.nanoTime() < deadline) {
            final CountDownLatch drawn = new CountDownLatch(1);
            final DataGridView.DrawListener listener = count -> drawn.countDown();
            grid.addDrawListener(listener);
            try {
                if (!drawn.await(SETTLE_MS, TimeUnit.MILLISECONDS)) return;
            } finally {
                grid.removeDrawListener(listener);
            }
        }
    }
}
