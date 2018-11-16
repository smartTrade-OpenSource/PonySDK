/*
 * Copyright (c) 2018 PonySDK
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

package com.ponysdk.core.server.websocket;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.server.application.Application;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.servlet.SessionManager;
import com.ponysdk.core.util.Pair;

class WebSocketStatsRecorder {

    private static final String KEY = WebSocketStatsRecorder.class.toString();
    private static final Logger log = LoggerFactory.getLogger(WebSocketStatsRecorder.class);
    private static final ServerToClientModel[] MODELS = ServerToClientModel.values();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile LocalDateTime startTime;
    private WebSocketStats summary;
    private final Consumer<WebSocketStats> listener;
    private final BiFunction<ServerToClientModel, Object, String> groupBy;

    public WebSocketStatsRecorder(final Consumer<WebSocketStats> listener,
            final BiFunction<ServerToClientModel, Object, String> groupBy) {
        super();
        this.listener = listener;
        this.groupBy = groupBy;
    }

    public WebSocketStatsRecorder(final Consumer<WebSocketStats> listener) {
        this(listener, null);
    }

    public <T> void record(final ServerToClientModel model, final T value, final int metaBytes, final int dataBytes,
                           final Function<T, Object> valueConverter) {
        if (startTime == null) return;
        final UIContext context = UIContext.get();
        AtomicReferenceArray<Map<Object, VolatileRecord>> stats = context.getAttribute(KEY);
        if (stats == null) {
            stats = new AtomicReferenceArray<>(MODELS.length + 1);
            context.setAttribute(KEY, stats);
        }
        Map<Object, VolatileRecord> records = stats.get(modelToIndex(model));
        if (records == null) {
            records = new ConcurrentHashMap<>();
            stats.set(modelToIndex(model), records);
        }
        Object v = valueConverter == null ? value : valueConverter.apply(value);
        if (groupBy != null) v = new Pair<>(v, groupBy.apply(model, value));
        records.computeIfAbsent(v, vv -> new VolatileRecord(metaBytes, dataBytes)).count++;
    }

    public <T> void record(final ServerToClientModel model, final T value, final int metaBytes, final int dataBytes) {
        record(model, value, metaBytes, dataBytes, null);
    }

    public synchronized boolean start(final long timeout, final TimeUnit unit) {
        if (startTime != null || summary != null) return false; //started OR stopped
        startTime = LocalDateTime.now();
        scheduler.schedule(this::stop, timeout, unit);
        log.info("Start recording for {} {}", timeout, unit);
        return true;
    }

    private static int modelToIndex(final ServerToClientModel model) {
        return model == null ? MODELS.length : model.ordinal();
    }

    private static void forEachUIContext(final Consumer<UIContext> consumer) {
        for (final Application app : SessionManager.get().getApplications()) {
            for (final UIContext context : app.getUIContexts()) {
                context.execute(() -> {
                    consumer.accept(context);
                });
            }
        }
    }

    public boolean isStarted() {
        return startTime != null;
    }

    public synchronized WebSocketStats stop() {
        if (this.startTime == null || summary != null) return null; //not started OR stopped
        final LocalDateTime startTime = this.startTime;
        this.startTime = null;
        final LocalDateTime endTime = LocalDateTime.now();
        scheduler.shutdownNow();
        final Collection<AtomicReferenceArray<Map<Object, VolatileRecord>>> allStats = ConcurrentHashMap.newKeySet();
        forEachUIContext(context -> {
            final AtomicReferenceArray<Map<Object, VolatileRecord>> stats = context.getAttribute(KEY);
            if (stats == null) return;
            context.removeAttribute(KEY);
            allStats.add(stats);
        });
        final Map<ServerToClientModel, Map<Object, MutableRecord>> aggStats = aggregateStats(allStats);
        summary = new WebSocketStats(aggStats, startTime, endTime, groupBy != null);
        if (listener != null) listener.accept(summary);
        log.info("Recording stopped, Statistics Summary: {}", summary);
        return summary;
    }

    private Map<ServerToClientModel, Map<Object, MutableRecord>> aggregateStats(final Collection<AtomicReferenceArray<Map<Object, VolatileRecord>>> allStats) {
        final Map<ServerToClientModel, Map<Object, MutableRecord>> aggStats = new EnumMap<>(ServerToClientModel.class);
        for (final AtomicReferenceArray<Map<Object, VolatileRecord>> s : allStats) {
            for (int i = 0; i < s.length(); i++) {
                final ServerToClientModel model = i >= MODELS.length ? null : MODELS[i];
                final Map<Object, VolatileRecord> records = s.get(i);
                if (records != null) {
                    for (final Map.Entry<Object, VolatileRecord> recordEntry : records.entrySet()) {
                        final Object value = recordEntry.getKey();
                        final VolatileRecord record = recordEntry.getValue();
                        aggStats.computeIfAbsent(model, m -> new HashMap<>()).computeIfAbsent(value,
                            o -> new MutableRecord(record.getMetaBytes(), record.getDataBytes())).count += record.count;
                    }
                }
            }
        }
        return aggStats;
    }

    public synchronized WebSocketStats getSummary() {
        return summary;
    }

    private static class VolatileRecord extends WebSocketStats.Record {

        private volatile int count = 0;

        private VolatileRecord(final int metaBytes, final int dataBytes) {
            super(metaBytes, dataBytes);
        }

        @Override
        public int getCount() {
            return count;
        }

    }

    private static class MutableRecord extends WebSocketStats.Record {

        private int count = 0;

        private MutableRecord(final int metaBytes, final int dataBytes) {
            super(metaBytes, dataBytes);
        }

        @Override
        public int getCount() {
            return count;
        }

    }
}
