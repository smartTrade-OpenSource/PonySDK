# Observability: OpenTelemetry and Dynatrace

PonySDK ships a built-in `PonySDKMetrics` class that exports metrics via the OpenTelemetry API. It is vendor-neutral — you bring your own OTel SDK and exporter. Dynatrace ingests natively via OTLP, no proprietary agent required.

## Metrics exposed

| Metric | Type | Unit | What it tells you |
|--------|------|------|-------------------|
| `ponysdk.uicontext.active` | gauge | `{context}` | Open browser tabs right now |
| `ponysdk.uicontext.created` | counter | `{context}` | Connection rate |
| `ponysdk.uicontext.destroyed` | counter | `{context}` | Disconnection rate + reason |
| `ponysdk.websocket.bytes.sent` | counter | `By` | Outbound bandwidth |
| `ponysdk.websocket.bytes.received` | counter | `By` | Inbound bandwidth |
| `ponysdk.websocket.messages.in` | counter | `{message}` | Event throughput from clients |
| `ponysdk.roundtrip.latency.ms` | histogram | `ms` | Full network round-trip (p50/p95/p99) |
| `ponysdk.execute.duration.ms` | histogram | `ms` | Time spent holding the UIContext lock |
| `ponysdk.lock.wait.ms` | histogram | `ms` | Time waiting to acquire the lock |

All metrics carry a `ponysdk.app` attribute so you can filter by application name in Dynatrace.

## Understanding the three latency metrics

```
┌─────────────────────────────────────────────────────────────────┐
│              Latency breakdown in PonySDK                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  User clicks button                                             │
│       │                                                         │
│       ▼                                                         │
│  ┌──────────────┐                                               │
│  │ lock.wait.ms │  ← Another thread holds the UIContext lock.   │
│  │              │    High value = thread contention.            │
│  │              │    Fix: move heavy work outside execute().    │
│  └──────────────┘                                               │
│       │  lock acquired                                          │
│       ▼                                                         │
│  ┌──────────────────┐                                           │
│  │ execute.         │  ← Your Java handler runs here +          │
│  │ duration.ms      │    txn.commit() flushes the WebSocket.    │
│  │                  │    High value = slow handler or large UI  │
│  │                  │    update. Fix: batch, cache, paginate.   │
│  └──────────────────┘                                           │
│       │  response sent                                          │
│       ▼                                                         │
│  ┌──────────────────┐                                           │
│  │ roundtrip.       │  ← Full server→client→server cycle.       │
│  │ latency.ms       │    Measured by the server using           │
│  │                  │    System.nanoTime() ping/pong.           │
│  │                  │    High value = network or slow client.   │
│  └──────────────────┘                                           │
│                                                                 │
│  Diagnosis guide:                                               │
│  lock.wait high    → thread contention on this UIContext        │
│  execute high      → your handler is doing too much work        │
│  roundtrip high    → network latency or heavy DOM rendering     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Setup — Dynatrace via OTLP

Add the OTel SDK to your application (not to the `ponysdk` module — it only depends on the API):

```groovy
// your app's build.gradle
runtimeOnly(
    'io.opentelemetry:opentelemetry-sdk:1.44.1',
    'io.opentelemetry:opentelemetry-exporter-otlp:1.44.1'
)
```

Wire it up in your `main()`:

```java
// 1. Build OTel SDK with OTLP exporter → Dynatrace
OpenTelemetry otel = OpenTelemetrySdk.builder()
    .setMeterProvider(SdkMeterProvider.builder()
        .setResource(Resource.getDefault().merge(
            Resource.create(Attributes.of(
                ResourceAttributes.SERVICE_NAME, "ponysdk-trading",
                ResourceAttributes.SERVICE_VERSION, "3.0.x"
            ))))
        .registerMetricReader(PeriodicMetricReader.builder(
            OtlpGrpcMetricExporter.builder()
                .setEndpoint("https://<env>.live.dynatrace.com/api/v2/otlp")
                .addHeader("Authorization", "Api-Token " + System.getenv("DT_API_TOKEN"))
                .build())
            .setInterval(Duration.ofSeconds(30))
            .build())
        .build())
    .buildAndRegisterGlobal();

// 2. Create the PonySDK metrics bridge
PonySDKMetrics metrics = new PonySDKMetrics(otel, "trading");

// 3. Plug into the WebSocket servlet — one line
webSocketServlet.setMetrics(metrics);

// Optional: also capture incoming message bytes
webSocketServlet.setWebsocketMonitor(metrics.websocketMonitor());
```

That's it. `PonySDKMetrics` hooks into `WebSocket.onWebSocketOpen/Close` and `UIContext.execute()` automatically — no further instrumentation needed.

## Built-in latency API (no OTel required)

If you don't use OTel, the per-tab latency averages are still available directly:

```java
UIContext ctx = UIContext.get();
double roundtrip = ctx.getRoundtripLatency();  // rolling avg of last 10 measurements (ms)
double network   = ctx.getNetworkLatency();    // the roundtrip minus the terminal's part (ms)
double terminal  = ctx.getTerminalLatency();   // from the arrival of the probe's message to the answer (ms)

if (roundtrip > 200) {
    log.warn("High latency on UIContext #{}: {}ms", ctx.getID(), roundtrip);
}
```

These are rolling averages over the last 10 round-trip measurements: the server sends a probe every 5 s (or every heartbeat period when shorter), so they cover the last 50 s. The terminal answers with the time the probe waited in it, from the arrival of its message (behind the blocks before it, the page's other tasks, a module loading); the rest of the roundtrip is the network's ([ADR-0005](../decisions/0005-page-left-and-terminal-time.md)). An older terminal, or the Pony driver, reports no terminal time: the network latency is then the roundtrip. Useful for in-process alerting without an external metrics system.
