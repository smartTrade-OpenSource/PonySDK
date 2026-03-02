/*
 * Copyright (c) 2011 PonySDK
 * Licensed under the Apache License, Version 2.0
 */

package com.ponysdk.core.server.metrics;

import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.server.websocket.WebSocket;
import com.ponysdk.core.server.websocket.WebsocketMonitor;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;

import java.util.concurrent.atomic.AtomicLong;

/**
 * OpenTelemetry instrumentation for PonySDK.
 *
 * <p>Exposes the following metrics under the {@code ponysdk} scope:
 * <ul>
 *   <li>{@code ponysdk.uicontext.active}       — gauge: currently active UIContexts (open tabs)</li>
 *   <li>{@code ponysdk.uicontext.created}       — counter: total UIContexts created since startup</li>
 *   <li>{@code ponysdk.uicontext.destroyed}     — counter: total UIContexts destroyed</li>
 *   <li>{@code ponysdk.websocket.bytes.sent}    — counter: bytes sent to all clients</li>
 *   <li>{@code ponysdk.websocket.bytes.received}— counter: bytes received from all clients</li>
 *   <li>{@code ponysdk.websocket.messages.in}   — counter: text messages received</li>
 *   <li>{@code ponysdk.roundtrip.latency.ms}    — histogram: full roundtrip latency in ms</li>
 *   <li>{@code ponysdk.execute.duration.ms}     — histogram: UIContext.execute() duration in ms</li>
 *   <li>{@code ponysdk.dictionary.size}         — gauge: string dictionary entries per UIContext</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // 1. Build with your OTel SDK (e.g. OTLP exporter → Dynatrace)
 * OpenTelemetry otel = OpenTelemetrySdk.builder()
 *     .setMeterProvider(SdkMeterProvider.builder()
 *         .registerMetricReader(PeriodicMetricReader.builder(
 *             OtlpGrpcMetricExporter.builder()
 *                 .setEndpoint("https://<env>.live.dynatrace.com/api/v2/otlp")
 *                 .addHeader("Authorization", "Api-Token " + token)
 *                 .build())
 *             .setInterval(Duration.ofSeconds(30))
 *             .build())
 *         .build())
 *     .buildAndRegisterGlobal();
 *
 * // 2. Create and register the metrics bridge
 * PonySDKMetrics metrics = new PonySDKMetrics(otel);
 *
 * // 3. Set on your WebSocketServlet (for WS-level metrics)
 * webSocketServlet.setMonitor(metrics.websocketMonitor());
 *
 * // 4. Register as ContextDestroyListener on each UIContext
 * //    (done automatically if you use ApplicationManager.startApplication hook)
 * uiContext.addContextDestroyListener(metrics.contextDestroyListener());
 * }</pre>
 *
 * <p>The OTel API dependency is {@code compileOnly}-safe: if no SDK is on the classpath,
 * {@link GlobalOpenTelemetry#get()} returns a no-op implementation and nothing breaks.
 */
public final class PonySDKMetrics {

    private static final String INSTRUMENTATION_SCOPE = "ponysdk";

    // Attribute keys
    static final AttributeKey<String> ATTR_APP = AttributeKey.stringKey("ponysdk.app");
    static final AttributeKey<String> ATTR_REASON = AttributeKey.stringKey("ponysdk.destroy.reason");

    // Internal counters
    private final AtomicLong activeContexts = new AtomicLong(0);
    private final AtomicLong totalBytesSent = new AtomicLong(0);
    private final AtomicLong totalBytesReceived = new AtomicLong(0);

    // OTel instruments
    private final LongCounter createdCounter;
    private final LongCounter destroyedCounter;
    private final LongCounter bytesSentCounter;
    private final LongCounter bytesReceivedCounter;
    private final LongCounter messagesInCounter;
    private final LongHistogram roundtripHistogram;
    private final LongHistogram executeHistogram;
    private final LongHistogram lockWaitHistogram;

    // Keep references to prevent GC of observable instruments
    @SuppressWarnings("FieldCanBeLocal")
    private final ObservableLongGauge activeGauge;

    private final String appName;

    /**
     * Creates metrics using {@link GlobalOpenTelemetry} — works if you called
     * {@code OpenTelemetrySdk.builder()...buildAndRegisterGlobal()} at startup.
     */
    public PonySDKMetrics(final String appName) {
        this(GlobalOpenTelemetry.get(), appName);
    }

    /**
     * Creates metrics with an explicit {@link OpenTelemetry} instance.
     */
    public PonySDKMetrics(final OpenTelemetry otel, final String appName) {
        this.appName = appName;
        final Meter meter = otel.getMeter(INSTRUMENTATION_SCOPE);

        createdCounter = meter.counterBuilder("ponysdk.uicontext.created")
                .setDescription("Total UIContexts (browser tabs) created since startup")
                .setUnit("{context}")
                .build();

        destroyedCounter = meter.counterBuilder("ponysdk.uicontext.destroyed")
                .setDescription("Total UIContexts destroyed")
                .setUnit("{context}")
                .build();

        bytesSentCounter = meter.counterBuilder("ponysdk.websocket.bytes.sent")
                .setDescription("Total bytes sent to all clients via WebSocket")
                .setUnit("By")
                .build();

        bytesReceivedCounter = meter.counterBuilder("ponysdk.websocket.bytes.received")
                .setDescription("Total bytes received from all clients via WebSocket")
                .setUnit("By")
                .build();

        messagesInCounter = meter.counterBuilder("ponysdk.websocket.messages.in")
                .setDescription("Total text messages received from terminals")
                .setUnit("{message}")
                .build();

        roundtripHistogram = meter.histogramBuilder("ponysdk.roundtrip.latency.ms")
                .ofLongs()
                .setDescription("Full roundtrip latency measured by the server (ms)")
                .setUnit("ms")
                .setExplicitBucketBoundariesAdvice(java.util.List.of(1L, 5L, 10L, 25L, 50L, 100L, 200L, 500L, 1000L))
                .build();

        executeHistogram = meter.histogramBuilder("ponysdk.execute.duration.ms")
                .ofLongs()
                .setDescription("Duration of UIContext.execute() calls — time spent holding the lock (ms)")
                .setUnit("ms")
                .setExplicitBucketBoundariesAdvice(java.util.List.of(1L, 5L, 10L, 25L, 50L, 100L, 250L, 500L))
                .build();

        lockWaitHistogram = meter.histogramBuilder("ponysdk.lock.wait.ms")
                .ofLongs()
                .setDescription("Time spent waiting to acquire the UIContext lock (ms) — indicates thread contention")
                .setUnit("ms")
                .setExplicitBucketBoundariesAdvice(java.util.List.of(0L, 1L, 5L, 10L, 25L, 50L, 100L, 250L))
                .build();

        activeGauge = meter.gaugeBuilder("ponysdk.uicontext.active")
                .ofLongs()
                .setDescription("Currently active UIContexts (open browser tabs)")
                .setUnit("{context}")
                .buildWithCallback(measurement ->
                        measurement.record(activeContexts.get(), appAttributes()));
    }

    // -------------------------------------------------------------------------
    // Hooks to call from framework integration points
    // -------------------------------------------------------------------------

    /**
     * Call from {@code WebSocket.onWebSocketOpen()} after UIContext is created.
     */
    public void onContextCreated() {
        activeContexts.incrementAndGet();
        createdCounter.add(1, appAttributes());
    }

    /**
     * Call from {@code UIContext.onDestroy()} or via {@link #contextDestroyListener()}.
     *
     * @param reason e.g. "close", "error", "timeout"
     */
    public void onContextDestroyed(final String reason) {
        activeContexts.decrementAndGet();
        destroyedCounter.add(1, Attributes.of(ATTR_APP, appName, ATTR_REASON, reason));
    }

    /**
     * Call after measuring roundtrip latency (already done in WebSocket.processRoundtripLatency).
     */
    public void recordRoundtrip(final long latencyMs) {
        roundtripHistogram.record(latencyMs, appAttributes());
    }

    /**
     * Record time spent holding the UIContext lock (runnable + txn.commit).
     * Called from UIContext.execute().
     */
    public void recordExecute(final long durationMs) {
        executeHistogram.record(durationMs, appAttributes());
    }

    /**
     * Record time spent waiting to acquire the UIContext lock.
     * A high value means thread contention — multiple threads competing for the same tab.
     * Called from UIContext.execute().
     */
    public void recordLockWait(final long waitMs) {
        lockWaitHistogram.record(waitMs, appAttributes());
    }

    /**
     * Record outgoing bytes (call from WebSocketPusher or WebsocketMonitor).
     */
    public void recordBytesSent(final int bytes) {
        totalBytesSent.addAndGet(bytes);
        bytesSentCounter.add(bytes, appAttributes());
    }

    /**
     * Record incoming message bytes.
     */
    public void recordBytesReceived(final int bytes) {
        totalBytesReceived.addAndGet(bytes);
        bytesReceivedCounter.add(bytes, appAttributes());
        messagesInCounter.add(1, appAttributes());
    }

    // -------------------------------------------------------------------------
    // Convenience adapters — plug directly into existing PonySDK hooks
    // -------------------------------------------------------------------------

    /**
     * Returns a {@link WebsocketMonitor} that feeds incoming message metrics.
     * Set via {@code webSocket.setMonitor(metrics.websocketMonitor())}.
     */
    public WebsocketMonitor websocketMonitor() {
        return new WebsocketMonitor() {
            @Override
            public void onMessageReceived(final WebSocket webSocket, final String message) {
                recordBytesReceived(message.length());
            }

            @Override
            public void onMessageProcessed(final WebSocket webSocket, final String message) {
                // no-op — processing time tracked via recordExecute
            }

            @Override
            public void onMessageUnprocessed(final WebSocket webSocket, final String message) {
                // no-op
            }
        };
    }

    /**
     * Returns a {@link com.ponysdk.core.server.application.ContextDestroyListener}
     * that decrements the active gauge on tab close.
     * Register via {@code uiContext.addContextDestroyListener(metrics.contextDestroyListener())}.
     */
    public com.ponysdk.core.server.application.ContextDestroyListener contextDestroyListener() {
        return uiContext -> onContextDestroyed("close");
    }

    // -------------------------------------------------------------------------
    // Snapshot for logging / health endpoints
    // -------------------------------------------------------------------------

    public long getActiveContexts() { return activeContexts.get(); }
    public long getTotalBytesSent() { return totalBytesSent.get(); }
    public long getTotalBytesReceived() { return totalBytesReceived.get(); }

    // -------------------------------------------------------------------------

    private Attributes appAttributes() {
        return Attributes.of(ATTR_APP, appName);
    }
}
