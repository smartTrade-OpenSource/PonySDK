/*
 * Copyright (c) 2026 PonySDK — Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.sample.client;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.management.GcInfo;

/**
 * Heap still in use right after the last garbage collection. {@code totalMemory - freeMemory} also counts the
 * dead objects not collected yet: with a concurrent collector (ZGC) it showed 284 MB for 66 MB of live objects.
 */
final class LiveHeap {

    private static final Set<String> HEAP_POOLS = ManagementFactory.getMemoryPoolMXBeans().stream()
        .filter(pool -> pool.getType() == MemoryType.HEAP).map(MemoryPoolMXBean::getName).collect(Collectors.toUnmodifiableSet());

    private static volatile long sampledAt;
    private static volatile long bytes;

    private LiveHeap() {
    }

    /** In bytes, shared by every session: sampled at most once a second. Before the first GC, the heap in use. */
    static long bytes() {
        final long now = System.nanoTime();
        if (sampledAt == 0 || now - sampledAt > 1_000_000_000L) {
            bytes = sample();
            sampledAt = now;
        }
        return bytes;
    }

    private static long sample() {
        GcInfo last = null;
        for (final GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            // ZGC also reports its pauses, which happen inside a cycle: only whole collections count
            if (!(gc instanceof com.sun.management.GarbageCollectorMXBean collector) || gc.getName().endsWith("Pauses")) continue;
            final GcInfo info = collector.getLastGcInfo();
            if (info != null && (last == null || info.getEndTime() > last.getEndTime())) last = info;
        }
        if (last == null) {
            final Runtime rt = Runtime.getRuntime();
            return rt.totalMemory() - rt.freeMemory();
        }
        long used = 0;
        for (final Map.Entry<String, MemoryUsage> pool : last.getMemoryUsageAfterGc().entrySet()) {
            if (HEAP_POOLS.contains(pool.getKey())) used += pool.getValue().getUsed();
        }
        return used;
    }
}
