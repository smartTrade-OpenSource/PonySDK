[![codecov](https://codecov.io/gh/smartTrade-OpenSource/PonySDK/branch/master/graph/badge.svg)](https://codecov.io/gh/smartTrade-OpenSource/PonySDK)
[![CI](https://github.com/smartTrade-OpenSource/PonySDK/actions/workflows/ci.yml/badge.svg)](https://github.com/smartTrade-OpenSource/PonySDK/actions)

# PonySDK

PonySDK is an open source framework for building real-time web applications entirely in Java — no JavaScript required.

It runs a Jetty 12 WebSocket server on the backend and uses GWT on the frontend. Every UI interaction is handled server-side: the browser is a thin rendering layer that receives binary protocol instructions over WebSocket.

---

## Tech Stack

| Component | Version |
|-----------|---------|
| Java (server) | 25 (LTS, virtual threads) |
| Java (terminal / GWT) | 17 |
| Jetty | 12.1.x (EE11 / Jakarta EE) |
| GWT | 2.13.0 |
| elemental2 | 1.3.2 |
| Spring | 7.x |
| Selenium | 4.x |
| Gradle | 9.x |

---

## Features

**Protocol**
- Binary WebSocket protocol between server and browser
- String Dictionary: up to 83% bandwidth reduction on repeated strings, shared across sessions
- 5-level incremental encoding: equality check → dictionary → merge-patch → binary → WebSocket deflate
- Typed binary arrays: `PAddOn` creation args and method calls carry int/long/double/boolean/String in pure binary, with a uint31 length (no 255-element cap)

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
- `PAddOn` for integrating any JavaScript library — typed creation args and method calls sent in pure binary (no JSON) via `PAddOn(Object...)`
- Virtual threads (Java 25) for scalable concurrent `UIContext`s
- JsInterop / elemental2 terminal — no legacy `gwt-elemental`

---

## Quick Start

```sh
git clone https://github.com/smartTrade-OpenSource/PonySDK.git
cd PonySDK
./gradlew :sample:runSampleSpring
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

## WebSocket Security & Limits

The WebSocket transport is hardened and tunable via `ApplicationConfiguration`:

```java
// Cross-Site WebSocket Hijacking (CSWSH) protection — enabled by default.
// Same-origin is accepted automatically (honouring X-Forwarded-Host behind a proxy);
// requests without an Origin header (non-browser clients) are allowed.
config.setWsOriginCheckEnabled(true);                       // default: true
config.setWsAllowedOrigins(Set.of("https://app.example.com")); // optional extra trusted origins

config.setWsMaxInboundMessageSize(1 << 20); // max client→server text message in bytes (default 1 MB)
config.setWsIdleTimeoutMs(1_000_000);       // close after this much inactivity (ms)
config.setWsSendTimeoutMs(60_000);          // disconnect a slow consumer after this (ms)
config.setWsPermessageDeflateEnabled(true); // frame compression (default on); disable for low-CPU/latency
```

**CSWSH:** a WebSocket upgrade is *not* subject to the same-origin policy, yet the browser still
attaches the session cookie. With the Origin check on, cross-origin upgrades are rejected with `403`,
so a malicious page cannot drive a logged-in user's session.

**Backpressure:** the server keeps a single send in flight per connection; if a slow client does not
drain within `wsSendTimeoutMs`, the connection is closed — memory stays bounded, no unbounded buffering.

---

## HTTP Compression

Static HTTP assets (bootstrap HTML, the GWT JavaScript bundle, AJAX responses) are compressed by a
Jetty `CompressionHandler`. **gzip** is always enabled; **Brotli** is enabled automatically when its
native library is available on the platform — the browser negotiates `br` vs `gzip` via the
`Accept-Encoding` header. Already-compressed content (images, fonts, archives) is skipped.

Brotli relies on a native library (`brotli4j`). Since JDK 24 ([JEP 472](https://openjdk.org/jeps/472))
the JVM warns on native access and will eventually block it unless explicitly granted, so run your
server with:

```sh
java --enable-native-access=ALL-UNNAMED -jar your-app.jar
```

If the native library is unavailable (or the flag denied), the server degrades gracefully to gzip
only — startup is never broken.

> This applies to **HTTP asset delivery** only. The real-time WebSocket protocol uses
> permessage-deflate, configured separately via `wsPermessageDeflateEnabled`.

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
- [CONCEPTS_CLES.md](CONCEPTS_CLES.md) — conceptual primer (French): Application, UIContext, PObject, the protocol, EventBus, transactions
- [Wiki](https://github.com/smartTrade-OpenSource/PonySDK/wiki) — FAQ
