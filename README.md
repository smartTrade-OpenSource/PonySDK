[![codecov](https://codecov.io/gh/PonySDK-Organization/PonySDK/branch/master/graph/badge.svg)](https://codecov.io/gh/PonySDK-Organization/PonySDK)
[![CI](https://github.com/PonySDK-Organization/PonySDK/actions/workflows/ci.yml/badge.svg)](https://github.com/PonySDK-Organization/PonySDK/actions)

# PonySDK

PonySDK is an open source framework for building real-time web applications entirely in Java — no JavaScript required.

It runs a Jetty 12 WebSocket server on the backend and uses GWT on the frontend. Every UI interaction is handled server-side: the browser is a thin rendering layer that receives binary protocol instructions over WebSocket.

---

## Tech Stack

| Component | Version |
|-----------|---------|
| Java (server) | 21 (virtual threads) |
| Java (terminal / GWT) | 17 |
| Jetty | 12.0.x (EE10 / Jakarta EE) |
| GWT | 2.13.0 |
| elemental2 | 1.2.1 |
| Spring | 6.x |
| Selenium | 4.x |
| Gradle | 8.x |

---

## Features

**Protocol**
- Binary WebSocket protocol between server and browser
- String Dictionary: up to 83% bandwidth reduction on repeated strings, shared across sessions
- 5-level incremental encoding: equality check → dictionary → merge-patch → binary → WebSocket deflate

**Connectivity**
- Transparent WebSocket reconnection (opt-in): UIContext survives network drops, state replayed on reconnect
- `RecordingEncoder` buffers all protocol ops during suspension with compaction (merge updates, cancel create+remove pairs)
- Configurable reconnection timeout and buffer limit — safe default is disabled (no silent state resume)

**Observability**
- OpenTelemetry instrumentation via `PonySDKMetrics` (OTel API only, SDK goes in your app)
- Metrics: active contexts (gauge), created/destroyed (counters), bytes sent/received, roundtrip latency, execute duration, lock wait (histograms)
- Fully opt-in — no-op when not configured

**UI**
- Web Component integration (`PWebComponent`) with `PropertyHandle` API (on-heap, off-heap, stateless modes)
- `PAddOn` for integrating any JavaScript library
- Virtual threads (Java 21) for scalable concurrent `UIContext`s
- JsInterop / elemental2 terminal — no legacy `gwt-elemental`

---

## Quick Start

```sh
git clone https://github.com/PonySDK-Organization/PonySDK.git
cd PonySDK
./gradlew :sample:runSampleSpring --no-configuration-cache
```

Then open [http://localhost:8081/sample/](http://localhost:8081/sample/)

---

## Build

```sh
# Compile
./gradlew :ponysdk:compileJava

# Run tests
./gradlew :ponysdk:test

# GWT compile (terminal → JavaScript)
./gradlew :ponysdk:gwtc

# Full build
./gradlew :ponysdk:build
```

---

## Transparent Reconnection

Opt-in via `ApplicationConfiguration`:

```java
config.setReconnectionTimeoutMs(10_000); // keep UIContext alive for 10s on disconnect
config.setMaxRecordingEntries(10_000);   // max buffered protocol entries (default 10k ≈ 320KB)
```

During the reconnection window, all server-side state mutations are buffered by `RecordingEncoder`. On reconnect, the buffer is compacted and replayed on the new WebSocket — the client receives a `RECONNECT_CONTEXT` signal and resumes without a page reload.

**Not recommended for trading applications** where stale market data must never be silently resumed. Use the JS hooks instead:

```javascript
window.onPonyReconnected = function() {
    // custom reconnection acknowledgement
};
```

---

## OpenTelemetry Metrics

```java
// In your application setup — bring your own OTel SDK
OpenTelemetry otel = ...; // configure exporter (OTLP, Dynatrace, etc.)
PonySDKMetrics metrics = new PonySDKMetrics(otel, "my-app");
webSocketServlet.setMetrics(metrics);
```

Metrics exported:

| Metric | Type | Description |
|--------|------|-------------|
| `uicontext.active` | Gauge | Live UIContext count |
| `uicontext.created` | Counter | Total contexts created |
| `uicontext.destroyed` | Counter | Total contexts destroyed (with reason) |
| `websocket.bytes.sent` | Counter | Total bytes sent |
| `websocket.bytes.received` | Counter | Total bytes received |
| `roundtrip.latency.ms` | Histogram | Server→client→server roundtrip |
| `execute.duration.ms` | Histogram | UIContext.execute() duration |
| `lock.wait.ms` | Histogram | UIContext lock contention |

---

## Pony Driver

A Selenium-compatible WebSocket driver for headless testing — no browser required.

```java
PonySDKWebDriver driver = new PonySDKWebDriver();
driver.get("ws://localhost:8081/sample/ws");

WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
wait.until(d -> d.findElement(By.cssSelector(".main TEXTBOX.login"))).sendKeys("admin");
wait.until(d -> d.findElement(By.tagName("BUTTON"))).click();
```

### Dependencies

```gradle
implementation 'com.ponysdk:ponysdk:2.8.99.x'
implementation 'org.seleniumhq.selenium:selenium-api:4.x'
implementation 'jakarta.websocket:jakarta.websocket-client-api:2.2.0'

runtimeOnly 'org.glassfish.tyrus:tyrus-client:2.2.0'
runtimeOnly 'org.glassfish.tyrus:tyrus-container-grizzly-client:2.2.0'
```

---

## Browser Compatibility

| Browser | Min version |
|---------|-------------|
| Chrome | 60+ |
| Firefox | 55+ |
| Safari | 11+ |
| Edge | 79+ (Chromium) |

---

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) — deep dive into the protocol, reconnection engine, threading model, OTel metrics, and design patterns
- [Wiki](https://github.com/PonySDK-Organization/PonySDK/wiki) — FAQ
