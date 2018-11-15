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
import com.ponysdk.core.util.Pair;

class WebSocketStatsRecorder {

    private static final Logger log = LoggerFactory.getLogger(WebSocketStatsRecorder.class);
    private static final ServerToClientModel[] MODELS = ServerToClientModel.values();
    private volatile ThreadLocal<AtomicReferenceArray<Map<Object, MutableRecord>>> localStats = null;
    private volatile Collection<AtomicReferenceArray<Map<Object, MutableRecord>>> allStats = null;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private LocalDateTime startTime;
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
        final ThreadLocal<AtomicReferenceArray<Map<Object, MutableRecord>>> localStats = this.localStats;
        if (localStats == null) return;
        Object v = valueConverter == null ? value : valueConverter.apply(value);
        if (groupBy != null) v = new Pair<>(v, groupBy.apply(model, value));
        final AtomicReferenceArray<Map<Object, MutableRecord>> stats = localStats.get();
        Map<Object, MutableRecord> records = stats.get(modelToIndex(model));
        if (records == null) {
            records = new ConcurrentHashMap<>();
            stats.set(modelToIndex(model), records);
        }
        records.computeIfAbsent(v, vv -> new MutableRecord(metaBytes, dataBytes)).count++;
    }

    public <T> void record(final ServerToClientModel model, final T value, final int metaBytes, final int dataBytes) {
        record(model, value, metaBytes, dataBytes, null);
    }

    public synchronized boolean start(final long timeout, final TimeUnit unit) {
        if (localStats != null || summary != null) return false; //started OR stopped
        startTime = LocalDateTime.now();
        localStats = ThreadLocal.withInitial(this::newStats);
        allStats = ConcurrentHashMap.newKeySet();
        scheduler.schedule(this::stop, timeout, unit);
        log.info("Start recording for {} {}", timeout, unit);
        return true;
    }

    private static int modelToIndex(final ServerToClientModel model) {
        return model == null ? MODELS.length : model.ordinal();
    }

    private AtomicReferenceArray<Map<Object, MutableRecord>> newStats() {
        final AtomicReferenceArray<Map<Object, MutableRecord>> stats = new AtomicReferenceArray<>(MODELS.length + 1);
        final Collection<AtomicReferenceArray<Map<Object, MutableRecord>>> allStats = this.allStats;
        if (allStats != null) allStats.add(stats);
        return stats;
    }

    public boolean isStarted() {
        return localStats != null;
    }

    public synchronized WebSocketStats stop() {
        if (localStats == null || summary != null) return null; //not started OR stopped
        localStats = null;
        final LocalDateTime endTime = LocalDateTime.now();
        scheduler.shutdownNow();
        final Collection<AtomicReferenceArray<Map<Object, MutableRecord>>> allStats = this.allStats;
        this.allStats = null;
        final Map<ServerToClientModel, Map<Object, MutableRecord>> stats = new EnumMap<>(ServerToClientModel.class);
        for (final AtomicReferenceArray<Map<Object, MutableRecord>> s : allStats) {
            for (int i = 0; i < s.length(); i++) {
                final ServerToClientModel model = i >= MODELS.length ? null : MODELS[i];
                final Map<Object, MutableRecord> records = s.get(i);
                if (records != null) {
                    for (final Map.Entry<Object, MutableRecord> recordEntry : records.entrySet()) {
                        final Object value = recordEntry.getKey();
                        final MutableRecord record = recordEntry.getValue();
                        stats.computeIfAbsent(model, m -> new HashMap<>()).computeIfAbsent(value,
                            o -> new MutableRecord(record.metaBytes, record.dataBytes)).count += record.count;
                    }
                }
            }
        }
        summary = new WebSocketStats(stats, startTime, endTime, groupBy != null);
        if (listener != null) listener.accept(summary);
        log.info("Recording stopped, Statistics Summary: {}", summary);
        return summary;
    }

    public synchronized WebSocketStats getSummary() {
        return summary;
    }

    private static class MutableRecord implements WebSocketStats.Record {

        private final int metaBytes;
        private final int dataBytes;
        private int count = 0;

        private MutableRecord(final int metaBytes, final int dataBytes) {
            super();
            this.metaBytes = metaBytes;
            this.dataBytes = dataBytes;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public int getMetaBytes() {
            return metaBytes;
        }

        @Override
        public int getDataBytes() {
            return dataBytes;
        }

    }
}
