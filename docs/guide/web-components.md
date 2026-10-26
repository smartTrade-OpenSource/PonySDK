# Web components

PonySDK integrates native HTML Web Components directly from server-side Java, with the same protocol optimizations as built-in widgets.

## Philosophy

The separation of concerns is strict:

| Layer | Responsibility |
|-------|---------------|
| **Web Component (JS)** | Shadow DOM, rendering, internal structure, slots |
| **PonySDK (Java)** | Properties, attributes, events, slot insertion, protocol |

PonySDK never touches the shadow DOM. It only sets properties/attributes on the custom element and inserts children into named slots. The web component decides everything about its internal structure.

```
┌─────────────────────────────────────────────────────────────────┐
│                  Web Component Integration                      │
├──────────────────────────┬──────────────────────────────────────┤
│        SERVER (Java)     │           BROWSER (JS)               │
│                          │                                      │
│  PWebComponent           │  <my-dashboard>                      │
│  ├── property("theme")   │  ├── [theme="dark"]  ← JS property   │
│  ├── attr("aria-label")  │  ├── aria-label="…"  ← HTML attr     │
│  ├── slot("toolbar")     │  │   └── <div slot="toolbar">        │
│  │   └── PButton         │  │       └── <button>…</button>      │
│  ├── slot("content")     │  │   └── <div slot="content">        │
│  │   └── PFlexTable      │  │       └── <table>…</table>        │
│  └── addEventListener    │  └── dispatchEvent("submit", detail) │
│                          │                                      │
│  Binary protocol ───────▶│  Shadow DOM (managed by WC itself)   │
│  (properties, slots,     │  ├── <slot name="toolbar">           │
│   events, methods)       │  ├── <slot name="content">           │
│                          │  └── internal styles & structure     │
└──────────────────────────┴──────────────────────────────────────┘
```

## Creating a Web Component

```java
PWebComponent chart = new PWebComponent("my-chart");

// HTML attributes, as on any widget
chart.setAttribute("aria-label", "Revenue Chart");
chart.removeAttribute("aria-label");

// JS properties: JSON text, a string in quotes
chart.property("title").set("\"Revenue Q4\"");
chart.property("theme").set("\"dark\"");

// Custom events: the listener receives the event's detail, parsed (a JsonValue)
HandlerRegistration clicks = chart.addEventListener("point-click", detail -> {
    log.info("Clicked: {}", detail.asJsonObject().getInt("index"));
});
clicks.removeHandler(); // when the screen no longer needs it

// Call a JS method on the element
chart.call("refresh");
chart.call("setRange", 0, 100);

// Add to the widget tree like any other widget
flowPanel.add(chart);
```

## Slot API — inserting PonySDK widgets into a Web Component

A Web Component can expose named `<slot>` elements in its shadow DOM. PonySDK's `SlotPanel` lets you insert any PWidget into a named slot from Java:

```java
PWebComponent dashboard = new PWebComponent("my-dashboard");

// Insert PonySDK widgets into named slots
dashboard.slot("toolbar").add(refreshButton, exportButton);
dashboard.slot("content").add(myDataGrid);
dashboard.slot("footer").add(statusLabel);

flowPanel.add(dashboard);
```

Under the hood, PonySDK sets `slot="toolbar"` on the child element and appends it to the custom element. The browser handles projection via the native `<slot name="toolbar">` in the shadow DOM. This works regardless of whether the component uses shadow DOM or not.

## PropertyHandle API — unified storage strategy

`PWebComponent` exposes a `PropertyHandle` API that unifies three storage strategies behind a fluent interface:

```java
// On-heap (default) — CompactStringMap, delta check + JSON patch
var title = chart.property("title");
title.set("\"Revenue Q4\"");

// Off-heap — direct ByteBuffer, FNV-1a hash delta, ideal for large JSON
var dataset = chart.property("dataset").offHeap().withPatch();
dataset.set(hugeJson);  // stored off-heap, incremental patch automatic

// Stateless — fire & forget, no cache, no delta, no replay
var stream = chart.property("stream").stateless();
stream.set(tempData);   // sent and forgotten

// Uniform read
String t = title.get();     // on-heap → CompactStringMap
String d = dataset.get();   // off-heap → ByteBuffer read → ephemeral String
String s = stream.get();    // stateless → null (no cache)
```

## Typed classes from a Custom Elements Manifest

A library's `custom-elements.json` describes its elements; `com.ponysdk.core.tools.WebComponentGenerator`, in the
`ponysdk` jar, writes a `PWebComponent` subclass per element from it (ADR-0010): typed setters for fields, attributes
and CSS custom properties, a listener per event, the methods, the named slots. Run it from the build:

```groovy
def webComponents = tasks.register('webComponents', JavaExec) {
    classpath = configurations.runtimeClasspath
    mainClass = 'com.ponysdk.core.tools.WebComponentGenerator'
    args 'node_modules/my-library/custom-elements.json', 'com.example.wc', layout.buildDirectory.dir('generated/wc').get().asFile.path
    outputs.dir(layout.buildDirectory.dir('generated/wc'))
}
sourceSets.main.java.srcDir(webComponents)
```

```java
PMyCounter counter = new PMyCounter();
counter.setCount(42);                                  // "42" on the wire, as property("count").set(42)
counter.onCountChanged(detail -> label.setText(detail.toString()));
counter.reset();
```

The generated class writes the same frames as the untyped calls; `sample/build.gradle` (task `webComponents`) is a
working setup.

## 5-level protocol optimization

Every `property("x").set(value)` call goes through up to 5 optimization levels:

```
┌─────────────────────────────────────────────────────────────────┐
│          5-level protocol optimization                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Level 1 — Equals check (skip if identical)                    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ On-heap  : Objects.equals(previous, value)              │    │
│  │ Off-heap : FNV-1a 64-bit hash + byte comparison         │    │
│  │ → If identical: NOTHING sent. Cost = 0 bytes.           │    │
│  └─────────────────────────────────────────────────────────┘    │
│                          ↓ (if changed)                         │
│                                                                 │
│  Level 2 — String Dictionary (property names)                   │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ Property names ("dataset", "theme", etc.) are           │    │
│  │ automatically interned by the PonySDK dictionary.       │    │
│  │ First time: full name sent + ID assigned                │    │
│  │ After: only the ID (4 bytes)                            │    │
│  │ → ~83% reduction on repeated names.                     │    │
│  └─────────────────────────────────────────────────────────┘    │
│                          ↓                                      │
│                                                                 │
│  Level 3 — JSON merge-patch (RFC 7396)                          │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ If value is a JSON object and a previous value exists,  │    │
│  │ PonySDK computes the diff:                              │    │
│  │                                                         │    │
│  │ Old  : {"a":1, "b":2, "c":3}                           │    │
│  │ New  : {"a":1, "b":5, "d":4}                           │    │
│  │ Patch: {"b":5, "c":null, "d":4}                        │    │
│  │                                                         │    │
│  │ → Only changed fields transit the network.              │    │
│  │ → null = deletion on client side (RFC 7396).            │    │
│  └─────────────────────────────────────────────────────────┘    │
│                          ↓                                      │
│                                                                 │
│  Level 4 — Compact binary protocol                              │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ The patch (or full value) is sent via PonySDK's         │    │
│  │ binary WebSocket protocol:                              │    │
│  │ [WC_PATCH_PROPERTY][ref_id][WC_PROPERTY_VALUE][patch]   │    │
│  │ → No JSON wrapper, no HTTP overhead.                    │    │
│  └─────────────────────────────────────────────────────────┘    │
│                          ↓                                      │
│                                                                 │
│  Level 5 — WebSocket permessage-deflate                         │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ The WebSocket frame is compressed by Jetty (deflate).   │    │
│  │ → Additional 70-86% reduction.                          │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Storage mode summary

| Mode | Server storage | Delta check | JSON patch | Reconnect replay | Use case |
|------|---------------|-------------|------------|------------------|----------|
| `property("x")` (default) | `CompactStringMap` (on-heap) | `Objects.equals` | yes (default) | yes | Standard properties |
| `.offHeap()` | `OffHeapJsonStore` (direct ByteBuffer) | FNV-1a 64-bit hash | yes (default) | yes | Large JSON (datasets, configs) |
| `.stateless()` | None | no | no | no | Streaming, ephemeral data |
| `.withPatch()` | Per mode | Per mode | yes (forced) | yes | Incremental updates |
| `.withoutPatch()` | Per mode | Per mode | no (forced) | yes | Always full replacement |

## Writing a Web Component for PonySDK

Web Components used with PonySDK must follow one critical rule: **never do DOM work in the constructor**. The terminal creates custom elements via `document.createElement()` which invokes the constructor — any DOM access there will crash.

**Rule: constructor = empty. All init goes in `connectedCallback`.**

```javascript
// Correct pattern
class MyChart extends HTMLElement {
    // No constructor needed — or just super() with nothing else

    connectedCallback() {
        // Safe: element is now in the DOM
        this.attachShadow({ mode: 'open' });
        this._render();
    }

    _render() {
        this.shadowRoot.innerHTML = `
            <style>:host { display: block; }</style>
            <div class="chart-root">
                <slot name="toolbar"></slot>
                <slot name="content"></slot>
            </div>
        `;
    }
}
customElements.define('my-chart', MyChart);
```

```javascript
// Wrong — crashes when the terminal calls document.createElement('my-chart')
class MyChart extends HTMLElement {
    constructor() {
        super();
        this.attachShadow({ mode: 'open' }); // CRASH
        this._render();                       // CRASH
    }
}
```

## pony.wc — shared stylesheet utilities

`ponysdk.js` exposes `pony.wc` after initialization with two utilities:

**`pony.wc.registerSharedSheet(href)`** — registers a CSS URL to be injected into every shadow root that calls `pony.wc.initShadow()`. Fully opt-in: if never called, nothing is injected.

**`pony.wc.initShadow(element)`** — attaches a shadow root (open mode) and injects all registered shared sheets. Idempotent — safe to call multiple times.

**`pony.wc.PonyElement`** — optional base class that calls `initShadow` + `_render()` automatically in `connectedCallback`. Subclasses must not define a constructor.

```javascript
// In your app bootstrap (before web components are connected)
document.onPonyLoaded(function() {
    pony.wc.registerSharedSheet('/css/design-system.css');
});

// Web component using PonyElement base class
document.onPonyLoaded(function() {
    class MyCard extends pony.wc.PonyElement {
        // No constructor

        connectedCallback() {
            super.connectedCallback(); // attaches shadow + injects sheets + calls _render
        }

        _render() {
            this.shadowRoot.innerHTML = `
                <style>:host { display: block; }</style>
                <div class="card">
                    <slot></slot>
                    <slot name="actions"></slot>
                </div>
            `;
        }
    }
    customElements.define('my-card', MyCard);
});
```

## Configuring shared sheets from Java

Shared stylesheets can be declared server-side in `ApplicationConfiguration`. PonySDK injects the corresponding `pony.wc.registerSharedSheet()` calls automatically before application scripts load:

```java
ApplicationConfiguration config = new ApplicationConfiguration();
config.setWcSharedSheets(Set.of("/css/design-system.css", "/css/tokens.css"));
```

This generates in the HTML `<head>`:
```html
<script>
document.onPonyLoaded(function() {
    pony.wc.registerSharedSheet('/css/design-system.css');
    pony.wc.registerSharedSheet('/css/tokens.css');
});
</script>
```

## Registering Web Components

Web Components that depend on `pony.wc` must be registered inside `document.onPonyLoaded()` so `pony.wc` is available:

```javascript
// script/webcomponents.js

// Components that don't need pony.wc can be registered immediately
class MyCounter extends HTMLElement { ... }
customElements.define('my-counter', MyCounter);

// Components that extend PonyElement must wait for pony
document.onPonyLoaded(function() {
    class MyDashboard extends pony.wc.PonyElement { ... }
    customElements.define('my-dashboard', MyDashboard);
});
```

Add the script in your configuration:
```java
config.setJavascript(Set.of("script/webcomponents.js"));
```

## Full example: real-time trading dashboard

```java
PWebComponent dashboard = new PWebComponent("trading-dashboard");

// Static config — on-heap, patch enabled
var config = dashboard.property("config");
config.set("{\"theme\":\"dark\",\"locale\":\"fr\",\"refreshRate\":500}");

// Large dataset — off-heap, incremental patch
var positions = dashboard.property("positions").offHeap().withPatch();
positions.set(buildPositionsJson());  // 500 KB JSON, stored off-heap

// Incremental update — only changed fields transit the network
positions.set(buildUpdatedPositionsJson());  // auto patch

// Real-time price tick — stateless, fire & forget
var tick = dashboard.property("lastTick").stateless();
tick.set("{\"symbol\":\"AAPL\",\"price\":185.42,\"ts\":1708963200}");

// Slot insertion
dashboard.slot("toolbar").add(filterPanel);
dashboard.slot("content").add(positionsGrid);

// Events
dashboard.addEventListener("order-submit", detail -> orderService.submit(parseOrder(detail.asJsonObject())));

flowPanel.add(dashboard);
```
