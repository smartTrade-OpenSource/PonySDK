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
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ponysdk.core.server.websocket;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.junit.After;
import org.junit.Test;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.websocket.WebSocketStats.Bandwidth;
import com.ponysdk.core.server.websocket.WebSocketStats.Record;
import com.ponysdk.core.util.Pair;

/**
 * Non-regression tests for {@link WebSocketStatsRecorder} — the live recorder that accumulates
 * per-{@link UIContext} protocol byte counts and aggregates them into a {@link WebSocketStats}
 * summary on stop.
 *
 * <p>The recorder reads {@code UIContext.get()} (a thread-local) and stores per-context counters as
 * a UIContext attribute; the test binds a mock UIContext backed by a real attribute map to the
 * current thread. {@code stop()} iterates the (empty) SessionManager, so no application setup is
 * needed.
 */
public class WebSocketStatsRecorderTest {

    private static final ServerToClientModel MODEL = ServerToClientModel.HEARTBEAT_PERIOD;

    @After
    public void unbindContext() {
        UIContext.remove();
    }

    /** Binds a mock UIContext backed by a real attribute map to the current thread. */
    private static UIContext bindContextToThread() {
        final UIContext ctx = mock(UIContext.class);
        final Map<String, Object> attrs = new ConcurrentHashMap<>();
        when(ctx.isAlive()).thenReturn(true);
        doAnswer(inv -> {
            attrs.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(ctx).setAttribute(anyString(), any());
        when(ctx.getAttribute(anyString())).thenAnswer(inv -> attrs.get(inv.getArgument(0)));
        UIContext.setCurrent(ctx);
        return ctx;
    }

    @Test
    public void testRecordThenStop_aggregatesCountsAndNotifiesListener() {
        bindContextToThread();
        final AtomicReference<WebSocketStats> notified = new AtomicReference<>();
        final WebSocketStatsRecorder recorder = new WebSocketStatsRecorder(notified::set);

        assertTrue(recorder.start(10, SECONDS));
        assertTrue(recorder.isStarted());

        recorder.record(MODEL, 42, 1, 8); // meta 1, data 8
        recorder.record(MODEL, 42, 1, 8); // same value -> count 2
        recorder.record(MODEL, 99, 2, 4); // different value

        final WebSocketStats summary = recorder.stop();

        assertNotNull(summary);
        assertFalse("recorder must be stopped", recorder.isStarted());
        assertEquals("listener must receive the summary", summary, notified.get());
        assertEquals(summary, recorder.getSummary());

        final Map<Object, ? extends Record> records = summary.getStats().get(MODEL);
        assertNotNull(records);
        assertEquals(2, records.get(42).getCount());
        assertEquals(8, records.get(42).getDataBytes());
        assertEquals(1, records.get(99).getCount());
        // summaryCount = 2 (value 42) + 1 (value 99)
        assertEquals(3, summary.getSummaryCount());
    }

    @Test
    public void testGroupedRecording_producesGroupedSummary() {
        bindContextToThread();
        final BiFunction<ServerToClientModel, Object, String> groupBy = (model, value) -> "grp";
        final WebSocketStatsRecorder recorder = new WebSocketStatsRecorder(s -> {}, groupBy);

        assertTrue(recorder.start(10, SECONDS));
        recorder.record(MODEL, 7, 3, 5);
        recorder.record(MODEL, 7, 3, 5);

        final WebSocketStats summary = recorder.stop();

        assertTrue("summary built with a groupBy must be grouped", summary.isGrouped());
        final Bandwidth group = summary.getBandwidthPerGroup().get("grp");
        assertNotNull(group);
        assertEquals(10, group.getData()); // data 5 * count 2
    }

    @Test
    public void testRecordBeforeStart_isIgnored() {
        bindContextToThread();
        final WebSocketStatsRecorder recorder = new WebSocketStatsRecorder(s -> {});

        // Not started yet: this must be a no-op (no NPE, nothing recorded).
        recorder.record(MODEL, 1, 1, 1);

        assertTrue(recorder.start(10, SECONDS));
        recorder.record(MODEL, 2, 1, 1); // only this one counts
        final WebSocketStats summary = recorder.stop();

        assertEquals(1, summary.getSummaryCount());
        assertNull("the pre-start value must not have been recorded", summary.getStats().get(MODEL).get(1));
        assertNotNull(summary.getStats().get(MODEL).get(2));
    }

    @Test
    public void testStartTwice_returnsFalseTheSecondTime() {
        final WebSocketStatsRecorder recorder = new WebSocketStatsRecorder(s -> {});
        try {
            assertTrue(recorder.start(10, SECONDS));
            assertFalse("start while already started must return false", recorder.start(10, SECONDS));
        } finally {
            recorder.stop();
        }
    }

    @Test
    public void testStartAfterStop_returnsFalse() {
        final WebSocketStatsRecorder recorder = new WebSocketStatsRecorder(s -> {});
        assertTrue(recorder.start(10, SECONDS));
        recorder.stop();
        assertFalse("start after stop must return false (already finished)", recorder.start(10, SECONDS));
    }

    @Test
    public void testStopBeforeStart_returnsNull() {
        final WebSocketStatsRecorder recorder = new WebSocketStatsRecorder(s -> {});
        assertNull(recorder.stop());
        // release the scheduler created at construction
        recorder.start(10, SECONDS);
        recorder.stop();
    }
}
