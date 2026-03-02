# PonySDK Architecture

## Table of Contents

1. [What is PonySDK?](#1-what-is-ponysdk)
2. [Quick Start (5 minutes)](#2-quick-start-5-minutes)
3. [Core Philosophy](#3-core-philosophy)
4. [High-Level Architecture](#4-high-level-architecture)
5. [Key Concepts](#5-key-concepts)
6. [Communication Layer](#6-communication-layer)
7. [Performance Architecture](#7-performance-architecture)
8. [Component Deep Dive](#8-component-deep-dive)
9. [Lifecycle & Flows](#9-lifecycle--flows)
10. [Threading Model](#10-threading-model)
11. [Advanced Features](#11-advanced-features)
    - [11.1 Web Components (PWebComponent)](#111-web-components-pwebcomponent)
    - [11.2 Custom JavaScript (PAddOn)](#112-custom-javascript-paddon)
    - [11.3 EventBus Communication](#113-eventbus-communication)
    - [11.4 Observability — OpenTelemetry & Dynatrace](#114-observability--opentelemetry--dynatrace)
    - [11.5 Transparent WebSocket Reconnection](#115-transparent-websocket-reconnection-opt-in)
12. [Package Structure](#12-package-structure)
13. [Configuration Reference](#13-configuration-reference)
14. [Jetty 12 / Jakarta EE Migration Notes](#14-jetty-12--jakarta-ee-migration-notes)
15. [Troubleshooting](#15-troubleshooting)
16. [Trade-offs & Comparisons](#16-trade-offs--comparisons)
17. [Widget Hierarchy Reference](#widget-hierarchy-reference)
18. [Architectural Patterns Summary](#architectural-patterns-summary)

---

# Part I: The Big Picture

## 1. What is PonySDK?

PonySDK is a **Java-only web framework** that lets you build rich web applications without writing JavaScript. You write server-side Java code, and PonySDK handles the browser rendering automatically.

```
┌─────────────────────────────────────────────────────────────┐
│                     YOU WRITE THIS                          │
│                                                             │
│   PButton button = Element.newPButton("Click me");          │
│   button.addClickHandler(e -> label.setText("Clicked!"));   │
│   panel.add(button);                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                            │
                    PonySDK binary protocol
                    over WebSocket
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   USER SEES THIS                            │
│                                                             │
│   ┌──────────────┐                                          │
│   │  Click me    │  ← Real button in the browser            │
│   └──────────────┘                                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**In one sentence:** Every UI widget is a Java object on the server that mirrors a DOM element in the browser — including native Web Components.

---

## 2. Quick Start (5 minutes)

### Step 1: Add Dependency

**Gradle:**
```groovy
dependencies {
    implementation 'com.ponysdk:ponysdk:2.8.+'
}
```

**Maven:**
```xml
<dependency>
    <groupId>com.ponysdk</groupId>
    <artifactId>ponysdk</artifactId>
    <version>2.8.0</version>
</dependency>
```

### Step 2: Create Your EntryPoint

```java
import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.main.EntryPoint;

public class HelloWorldEntryPoint implements EntryPoint {

    @Override
    public void start(UIContext uiContext) {
        // Create widgets
        PFlowPanel root = Element.newPFlowPanel();
        PLabel label = Element.newPLabel("Hello PonySDK!");
        PTextBox input = Element.newPTextBox();
        PButton button = Element.newPButton("Say Hello");
        
        // Add behavior
        button.addClickHandler(event -> {
            String name = input.getText();
            label.setText("Hello, " + (name.isEmpty() ? "World" : name) + "!");
        });
        
        // Build UI tree
        root.add(label);
        root.add(input);
        root.add(button);
        
        // Attach to page
        PRootPanel.get().add(root);
    }
}
```

### Step 3: Start the Server

```java
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.impl.java.JavaApplicationManager;
import com.ponysdk.impl.main.PonySDKServer;

public class Main {
    public static void main(String[] args) throws Exception {
        ApplicationConfiguration config = new ApplicationConfiguration();
        config.setApplicationName("HelloWorld");
        config.setEntryPointClass(HelloWorldEntryPoint.class);

        JavaApplicationManager appManager = new JavaApplicationManager();
        appManager.setConfiguration(config);

        PonySDKServer server = new PonySDKServer();
        server.setApplicationManager(appManager);
        server.setPort(8080);
        server.setHost("0.0.0.0");
        server.start();

        System.out.println("Server running at http://localhost:8080");
    }
}
```

### Step 4: Run and Test

```bash
# Run your Main class, then open browser
open http://localhost:8080
```

### Quick Start Summary

```
┌─────────────────────────────────────────────────────────────────┐
│                    What You Just Built                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   Main.java                                                     │
│      │                                                          │
│      └── PonySDKServer                                          │
│             │                                                   │
│             └── HelloWorldEntryPoint.start()                    │
│                    │                                            │
│                    ├── PFlowPanel (root container)              │
│                    │      │                                     │
│                    │      ├── PLabel ("Hello PonySDK!")         │
│                    │      ├── PTextBox (user input)             │
│                    │      └── PButton ("Say Hello")             │
│                    │             │                              │
│                    │             └── ClickHandler → updates UI  │
│                    │                                            │
│                    └── PRootPanel.get().add(root)               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Next Steps

| Want to... | See section |
|------------|-------------|
| Understand the architecture | [Core Philosophy](#3-core-philosophy) |
| Learn about performance | [Performance Architecture](#7-performance-architecture) |
| Add server push | [Threading Model](#10-threading-model) |
| Integrate Web Components | [Web Components](#111-web-components-pwebcomponent) |
| Integrate custom JS | [Advanced Features](#11-advanced-features) |

---

## 3. Core Philosophy

### The Proxy Pattern at Scale

PonySDK is built on one fundamental idea:

> **Server-side Java objects act as proxies for client-side DOM elements.**

| Server (Java) | Client (Browser) |
|---------------|------------------|
| `PButton` | `<button>` |
| `PTextBox` | `<input type="text">` |
| `PFlowPanel` | `<div>` |
| `PLabel` | `<span>` |

When you call `button.setText("Hello")` in Java, PonySDK sends an instruction to the browser to update the button's text.

### Why This Approach?

| Benefit | Explanation |
|---------|-------------|
| **Security** | Business logic never leaves the server |
| **Type Safety** | Compile-time checking, IDE refactoring |
| **Simplicity** | One language, one codebase |
| **Debugging** | Server-side breakpoints, standard logging |
| **Testing** | JUnit tests for UI logic |

---

## 4. High-Level Architecture

### High-Level Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                           BROWSER                                │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  HTML / CSS / DOM                                          │  │
│  │                                                            │  │
│  │  ┌──────────────┐   ┌──────────────┐   ┌───────────────┐  │  │
│  │  │ Built-in     │   │ Web          │   │ PonySDK       │  │  │
│  │  │ widgets      │   │ Components   │   │ AddOns        │  │  │
│  │  │ (div, button)│   │ (custom el.) │   │ (JS libs)     │  │  │
│  │  └──────────────┘   └──────────────┘   └───────────────┘  │  │
│  │                                                            │  │
│  │  GWT Terminal (ponyterminal.js) -- interprets instructions │  │
│  │  ponysdk.js -- pony.wc utilities, addon registry           │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
└──────────────────────────────────┬───────────────────────────────┘
                                   │
                    WebSocket (bidirectional)
                    Server → Client : Binary protocol
                    Client → Server : JSON events
                                   │
┌──────────────────────────────────┴───────────────────────────────┐
│                           SERVER                                 │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Your Application                                          │  │
│  │                                                            │  │
│  │  EntryPoint → PWidgets → PWebComponents → Event Handlers   │  │
│  └────────────────────────────────────────────────────────────┘  │
│                              │                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  PonySDK Framework                                         │  │
│  │                                                            │  │
│  │  UIContext │ PObject │ ModelWriter │ Txn │ StringDictionary│  │
│  └────────────────────────────────────────────────────────────┘  │
│                              │                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Jetty 12 (EE10)                                           │  │
│  │                                                            │  │
│  │  WebSocket │ HTTP │ Static Resources │ permessage-deflate  │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

| Layer | Role | Key Classes |
|-------|------|-------------|
| **Your App** | Business logic, UI composition | `EntryPoint`, your widgets |
| **PonySDK** | Widget abstraction, state sync | `PObject`, `UIContext`, `Txn` |
| **Jetty** | HTTP/WebSocket transport | `WebSocket`, `WebSocketServlet` |
| **Browser** | Rendering, user input | `Terminal.js`, `UIBuilder` |

---

## 5. Key Concepts

### 5.1 The Three Pillars

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Application    │     │   UIContext     │     │    PObject      │
│                 │     │                 │     │                 │
│  1 per Session  │────▶│  1 per Tab      │────▶│  1 per Widget   │
│  (HttpSession)  │     │  (browser tab)  │     │  (button, etc.) │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        │                       │                       │
   User session            Tab state              Widget state
   Shared data            Widget registry         DOM proxy
   Multi-tab broadcast    Event routing           Event handlers
```

### 5.2 Concept Summary

| Concept | Scope | Lifetime | Purpose |
|---------|-------|----------|---------|
| **Application** | User session | Login → Logout | Share state across tabs |
| **UIContext** | Browser tab | Tab open → Tab close | Manage widgets for one tab |
| **PObject** | Single widget | Creation → Destruction | Proxy for DOM element |

### 5.3 Quick Example

```java
// One user opens 2 browser tabs
Application app;           // 1 Application (shared)
├── UIContext ctx1;        // Tab 1
│   ├── PButton (ID=1)
│   ├── PLabel (ID=2)
│   └── PFlowPanel (ID=3)
└── UIContext ctx2;        // Tab 2
    ├── PButton (ID=1)     // Different ID namespace!
    ├── PTextBox (ID=2)
    └── PFlowPanel (ID=3)
```

---

# Part II: How It Works

## 6. Communication Layer

### 6.1 Protocol Overview

```
        Server                                    Client
           │                                         │
           │──── Binary Protocol (efficient) ───────▶│  CREATE, UPDATE, ADD...
           │                                         │
           │◀─── JSON Protocol (flexible) ──────────-│  Events, user input
           │                                         │
```

**Why different protocols?**
- **Server → Client (Binary):** High volume, needs efficiency
- **Client → Server (JSON):** Lower volume, needs flexibility for event data

### 6.2 Server → Client Instructions

| Instruction | Purpose | Example |
|-------------|---------|---------|
| `TYPE_CREATE` | Create widget | New button with ID 42 |
| `TYPE_UPDATE` | Change property | Set text to "Hello" |
| `TYPE_ADD` | Add to parent | Add button to panel |
| `TYPE_REMOVE` | Remove from parent | Remove from DOM |
| `TYPE_ADD_HANDLER` | Listen for events | Register click handler |
| `TYPE_GC` | Garbage collect | Free memory |

**Binary Format:**
```
┌──────────┬──────────┬──────────┬──────────┬─────┐
│ Model ID │  Value   │ Model ID │  Value   │ ... │
│ (1 byte) │ (varies) │ (1 byte) │ (varies) │     │
└──────────┴──────────┴──────────┴──────────┴─────┘

Example: Create button "Click me"
┌────────┬────┬────────┬────────┬────────┬───────────┬─────┐
│ CREATE │ 42 │ WIDGET │ BUTTON │  TEXT  │ "Click me"│ END │
└────────┴────┴────────┴────────┴────────┴───────────┴─────┘
```

### 6.3 Client → Server Events

```json
{
  "APPLICATION_INSTRUCTIONS": [{
    "OBJECT_ID": 42,
    "DOM_HANDLER_TYPE": "CLICK",
    "NATIVE_EVENT": {
      "clientX": 150,
      "clientY": 200,
      "button": 0
    }
  }]
}
```

| Field | Purpose |
|-------|---------|
| `OBJECT_ID` | Which widget was interacted with |
| `DOM_HANDLER_TYPE` | What happened (CLICK, KEY_UP, etc.) |
| `NATIVE_EVENT` | Raw browser event data |

---

## 7. Performance Architecture

PonySDK is designed for **low-latency, high-throughput** applications like trading platforms. Every architectural decision prioritizes performance.

### 7.1 Why Binary Protocol?

```
┌─────────────────────────────────────────────────────────────────┐
│                    Protocol Comparison                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  JSON (Vaadin-style):                                           │
│  {"type":"update","id":42,"property":"text","value":"Hello"}    │
│  = 58 bytes                                                     │
│                                                                 │
│  PonySDK Binary:                                                │
│  [0x05][0x2A][0x0B][0x05]Hello                                  │
│  = 10 bytes                                                     │
│                                                                 │
│  Reduction: ~83% smaller payload                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

| Metric | JSON Protocol | Binary Protocol | Gain |
|--------|---------------|-----------------|------|
| Payload size | 100% | ~17% | **6x smaller** |
| Parse time | String parsing | Direct read | **~10x faster** |
| GC pressure | High (String objects) | Low | **Less GC pauses** |

### 7.2 Instruction Batching

Multiple UI updates are batched into a single network frame:

```java
// Without batching: 3 network round-trips 
button.setText("A");     // → send → wait
button.setEnabled(true); // → send → wait  
button.addStyleName("x");// → send → wait

// With PonySDK batching: 1 network frame 
uiContext.execute(() -> {
    button.setText("A");      // queued
    button.setEnabled(true);  // queued
    button.addStyleName("x"); // queued
});  // → single flush with all 3 instructions
```

**Impact on latency:**
```
Without batching:  3 × RTT = 3 × 50ms = 150ms
With batching:     1 × RTT = 1 × 50ms = 50ms
                   ─────────────────────────
                   3x faster UI updates
```

### 7.3 Lazy Initialization (Stacked Instructions)

Widgets queue instructions until attached to DOM:

```java
PButton button = Element.newPButton("Click");  // Nothing sent yet
button.setText("Updated");                      // Queued locally
button.addStyleName("primary");                 // Queued locally
button.setEnabled(false);                       // Queued locally

panel.add(button);  // NOW: single CREATE with all properties
```

**Wire format (single message):**
```
CREATE(42) + WIDGET(BUTTON) + TEXT("Updated") + STYLE("primary") + ENABLED(false)
```

vs naive approach (4 messages):
```
CREATE(42) + WIDGET(BUTTON) + TEXT("Click")
UPDATE(42) + TEXT("Updated")
UPDATE(42) + STYLE("primary")
UPDATE(42) + ENABLED(false)
```

### 7.4 String Dictionary Protocol (String Interning)

PonySDK uses a **String Dictionary** to dramatically reduce bandwidth for repetitive strings like CSS class names, style properties, and attribute names.

**How it works:**
```
┌─────────────────────────────────────────────────────────────────┐
│                 String Dictionary Protocol                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  First occurrence of "pony-button-primary":                     │
│  ┌────────────────────┬────┬─────────────────────────┐          │
│  │ DICTIONARY_ADD     │ ID │ "pony-button-primary"   │          │
│  │ (1 byte)           │(2B)│ (21 bytes)              │          │
│  └────────────────────┴────┴─────────────────────────┘          │
│  + Reference: [Model][REF_TYPE][ID] = 4 bytes                   │
│  Total first time: ~28 bytes                                    │
│                                                                 │
│  Subsequent occurrences:                                        │
│  ┌────────────────────┬──────────┬────┐                         │
│  │ Model Key          │ REF_TYPE │ ID │                         │
│  │ (1 byte)           │ (1 byte) │(2B)│                         │
│  └────────────────────┴──────────┴────┘                         │
│  Total: 4 bytes (vs 23 bytes raw) = 83% reduction               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Bandwidth savings by scenario:**

| Scenario | Without Dictionary | With Dictionary | Savings |
|----------|-------------------|-----------------|---------|
| Button with 3 CSS classes | ~60 bytes | ~12 bytes | **80%** |
| DataGrid row (10 cells) | ~200 bytes | ~50 bytes | **75%** |
| Style property update | ~35 bytes | ~8 bytes | **77%** |
| 1000 repeated strings | ~23 KB | ~4 KB | **83%** |

**Configuration:**
```java
ApplicationConfiguration config = new ApplicationConfiguration();

// Enable/disable (default: enabled)
config.setStringDictionaryEnabled(true);

// Max entries in dictionary (default: 65535)
config.setStringDictionaryMaxSize(65535);

// Minimum string length to intern (default: 4)
// Shorter strings are sent raw (overhead not worth it)
config.setStringDictionaryMinLength(4);
```

**How the dictionary is synchronized:**
1. Server maintains a `StringDictionary` per `UIContext`
2. When a new eligible string (≥ 6 chars) is first sent, the server uses an inline `STRING_DICTIONARY_ADD` encoding: the model key + type marker + UINT31 ID + raw string. The client stores the mapping and returns the string value — no separate instruction needed.
3. Subsequent uses of the same string send only the compact `STRING_DICTIONARY_REF` (model key + type marker + UINT31 ID = 4 bytes)
4. Strings shorter than 6 characters are never interned (overhead not worth it), but if they were previously interned via the array path, they can still be referenced
5. Dictionary is cleared on reconnection (rebuilt automatically)
6. Optionally, a `SharedDictionaryProvider` persists cross-session frequency data so the server-side dictionary is pre-populated with the most common strings, reducing the number of inline adds needed in subsequent sessions

### 7.5 WebSocket Compression (permessage-deflate)

```
┌─────────────────────────────────────────────────────────────────┐
│                 Compression Impact                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Typical UI update payload:                                     │
│  ├── Uncompressed:  2,400 bytes                                 │
│  └── Compressed:      720 bytes  (70% reduction)                │
│                                                                 │
│  Large DataGrid update (1000 rows):                             │
│  ├── Uncompressed: 85,000 bytes                                 │
│  └── Compressed:   12,000 bytes  (86% reduction)                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 7.6 Memory Efficiency

**Widget ID Generation:**
```java
// Compact integer IDs (4 bytes) vs UUIDs (36 bytes)
protected final int ID = UIContext.get().nextID();  // 1, 2, 3...
```

**Object Pooling in ModelWriter:**
```java
// Reuses ByteBuffer instead of allocating new ones
public class ModelWriter {
    private final ByteBuffer buffer;  // Pre-allocated, reused

    public void writeInt(int value) {
        buffer.putInt(value);  // No allocation
    }
}
```

**OffHeapJsonStore — large JSON off the GC heap:**

`PWebComponent` properties declared with `.offHeap()` are stored in a `OffHeapJsonStore` backed by a direct `ByteBuffer`. Only a lightweight on-heap index (key → offset/length/FNV-1a hash) is kept in the JVM heap.

```
┌─────────────────────────────────────────────────────────────────┐
│                    OffHeapJsonStore internals                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  On-heap (JVM GC sees this):                                    │
│  HashMap<String, Entry>  ← ~80 bytes per key                   │
│  Entry { offset, length, hash }  ← 3 ints                      │
│                                                                 │
│  Off-heap (direct ByteBuffer, GC-invisible):                    │
│  [ JSON bytes ... | JSON bytes ... | (wasted) | JSON bytes ... ]│
│    key="positions"  key="config"    (deleted)   key="theme"     │
│                                                                 │
│  Capacity: 4 KB initial → doubles on demand → 16 MB hard cap   │
│  Compaction: triggered when wasted space > 25% of capacity     │
│  Release: buffer freed in PWebComponent.onDestroy()            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

Delta check uses FNV-1a 64-bit hash + byte comparison — no String allocation needed to detect unchanged values.

### 7.7 Latency Breakdown

PonySDK provides built-in latency monitoring:

```
┌─────────────────────────────────────────────────────────────────┐
│                    Latency Components                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  User Click                                                     │
│      │                                                          │
│      ▼                                                          │
│  ┌─────────┐                                                    │
│  │ Browser │ ──┐                                                │
│  │ Event   │   │                                                │
│  └─────────┘   │  Network Latency (networkLatency)              │
│                │  • WebSocket frame transmission                │
│                │  • Typically 1-50ms (LAN) or 50-200ms (WAN)    │
│                ▼                                                │
│  ┌─────────┐                                                    │
│  │ Server  │ ──┐                                                │
│  │ Process │   │  Server Processing                             │
│  └─────────┘   │  • Event handler execution                     │
│                │  • Typically < 1ms                             │
│                ▼                                                │
│  ┌─────────┐                                                    │
│  │ Browser │ ──┐                                                │
│  │ Render  │   │  Terminal Latency (terminalLatency)            │
│  └─────────┘   │  • DOM manipulation                            │
│                │  • Typically 1-10ms                            │
│                ▼                                                │
│  UI Updated                                                     │
│                                                                 │
│  Total: roundtripLatency = network + server + terminal          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Monitoring API:**
```java
UIContext ctx = UIContext.get();

// Measure latencies
double roundtrip = ctx.getRoundtripLatency();  // Total: ~50-100ms typical
double network = ctx.getNetworkLatency();       // Network only: ~20-50ms
double terminal = ctx.getTerminalLatency();     // Client render: ~5-10ms

// Alert on degradation
if (roundtrip > 200) {
    log.warn("High latency detected: {}ms", roundtrip);
}
```

### 7.8 Performance Comparison

| Framework | Protocol | Typical Payload | Latency Profile |
|-----------|----------|-----------------|-----------------|
| **PonySDK** | Binary WebSocket | 10-50 bytes/update | Optimized for trading |
| Vaadin | JSON + HTTP/WS | 100-500 bytes/update | General purpose |
| GWT RPC | JSON over HTTP | 200-1000 bytes/call | Legacy |

### 7.9 Performance Best Practices

#### DO

```java
// Batch updates in single transaction
uiContext.execute(() -> {
    for (PLabel label : labels) {
        label.setText(newValue);
    }
});

// Destroy unused widgets
oldPanel.removeFromParent();  // Frees server + client memory

// Use lazy loading
public void showDetails() {
    if (detailsPanel == null) {
        detailsPanel = createDetailsPanel();  // Create on demand
    }
    detailsPanel.setVisible(true);
}

// Prefer visibility over add/remove
widget.setVisible(false);  // Cheap: just CSS
// vs
panel.remove(widget);      // Expensive: DOM manipulation + GC
```

#### DON'T

```java
// Don't update UI in tight loops without batching
for (int i = 0; i < 1000; i++) {
    uiContext.execute(() -> {  // 1000 transactions!
        label.setText("" + i);
    });
}

// Don't keep references to destroyed widgets
panel.removeFromParent();
panel.setText("test");  // Widget is destroyed!

// Don't push too frequently
scheduler.scheduleAtFixedRate(() -> {
    uiContext.execute(() -> updateUI());
}, 1, 1, TimeUnit.MILLISECONDS);  // 1000 updates/sec = overload
```

### 7.10 Benchmarks (Typical Numbers)

| Scenario | Metric | Value |
|----------|--------|-------|
| Button click → UI update | Round-trip | 20-80ms |
| Create 100 widgets | Time | < 50ms |
| DataGrid 10,000 rows | Initial render | < 500ms |
| DataGrid cell update | Latency | < 30ms |
| Server push (price tick) | End-to-end | 5-20ms |
| Memory per UIContext | Heap | 1-5 MB |
| Memory per widget | Heap | 200-500 bytes |

---

## 8. Component Deep Dive

This section goes under the hood of the three classes you'll interact with most: `PObject` (the widget base), `UIContext` (the per-tab runtime), and `Txn` (the transaction system). Understanding how they fit together makes the rest of the framework predictable.

### 8.1 PObject - The Foundation

`PObject` is the base class for every server-side widget. It holds the widget's unique ID, tracks whether it has been sent to the browser yet, and routes updates either directly to the wire (if already initialized) or to a local queue (if not yet attached). Every widget you create — `PButton`, `PLabel`, `PWebComponent` — is a `PObject`.

Every widget inherits from `PObject`:

```java
public abstract class PObject {
    // Unique ID within this UIContext
    protected final int ID = UIContext.get().nextID();
    
    // Target window (main or popup)
    protected PWindow window;
    
    // Lifecycle flags
    protected boolean initialized = false;
    protected boolean destroy = false;
    
    // Queue updates before widget is attached (compact int→Runnable map, no boxing)
    protected CompactIntRunnableMap stackedInstructions;
    
    // Abstract: what type of widget?
    protected abstract WidgetType getWidgetType();
    
    // Send update to client
    protected void saveUpdate(ModelWriterCallback callback) {
        // Queues instruction for next flush
    }
    
    // Receive event from client
    public void onClientData(JsonObject event) {
        // Override to handle events
    }
}
```

### 8.2 UIContext - The Execution Context

`UIContext` is the runtime environment for a single browser tab. It owns the widget registry, the WebSocket writer, the string dictionary, and the lock that serializes all UI access. Think of it as the "session" for one tab — it's what `UIContext.get()` returns from anywhere in your code while you're inside an `execute()` call.

Manages all widgets for one browser tab:

```java
public class UIContext {
    // Thread-local access pattern
    private static final ThreadLocal<UIContext> currentContext = new ThreadLocal<>();
    
    // Widget registry
    private final PObjectCache pObjectCache = new PObjectCache();
    private int objectCounter = 1;
    
    // Thread safety
    private final ReentrantLock lock = new ReentrantLock();
    
    // Services
    private final PHistory history;
    private final EventBus rootEventBus;
    private final PCookies cookies;
    private final ModelWriter modelWriter;
    private final StringDictionary stringDictionary; // null if disabled
    
    // Safe execution from any thread
    public boolean execute(Runnable runnable) {
        acquire();  // lock + set ThreadLocal
        try {
            Txn.get().begin(this);
            runnable.run();
            Txn.get().commit();  // flush to client
        } finally {
            release();  // unlock + clear ThreadLocal
        }
    }
    
    // Access from anywhere in your code
    public static UIContext get() {
        return currentContext.get();
    }
}
```

### 8.3 Transaction System (STM)

Without transactions, every `setText()` or `setEnabled()` call would immediately flush a WebSocket frame — one frame per property change. `Txn` batches all updates made within a single `execute()` call into one atomic flush. If an exception is thrown, the transaction rolls back and nothing is sent to the client.

Ensures atomic UI updates:

```java
// All updates within execute() are batched
uiContext.execute(() -> {
    label1.setText("A");      // Queued
    label2.setText("B");      // Queued
    button.setEnabled(false); // Queued
});  // ← All sent together on commit

// If exception occurs → rollback (nothing sent)
```

### 8.4 Element Factory

Rather than calling `new PButton()` directly, PonySDK uses a static `Element` factory. This makes it easy to swap implementations for testing or theming, and keeps widget creation consistent across the codebase.

Centralized widget creation:

```java
// Standard usage
PButton button = Element.newPButton("Click me");
PFlowPanel panel = Element.newPFlowPanel();
PTextBox input = Element.newPTextBox();

// HTML elements
PElement div = Element.newDiv();
PElement span = Element.newSpan();

// Custom factory (testing, theming)
Element.setElementFactory(new MockElementFactory());
```

---

## 9. Lifecycle & Flows

This section traces the exact sequence of events from server startup to a user clicking a button. Reading it once gives you a mental model that makes debugging much easier.

### 9.1 Application Startup

```
1. PonySDKServer.start()
   └── Jetty initialization
   
2. ApplicationManager configured
   └── EntryPoint factory registered
   
3. WebSocketServlet registered at /ws
   └── Ready for connections
```

### 9.2 Client Connection Flow

```
Client                    Server
  │                          │
  │── WebSocket Connect ────▶│
  │                          │── Create UIContext
  │                          │── Create Application (if new session)
  │◀── CREATE_CONTEXT ───────│
  │    HEARTBEAT_PERIOD      │
  │                          │── Call EntryPoint.start()
  │                          │   └── Your code builds UI
  │◀── Binary Instructions ──│   └── Widgets created & flushed
  │                          │
  │    [Application Ready]   │
```

### 9.3 User Interaction Flow

```
Client                    Server
  │                          │
  │── Click button ─────────▶│
  │   {OBJECT_ID: 42,        │
  │    HANDLER: CLICK}       │
  │                          │── UIContext.execute()
  │                          │   └── Find PObject by ID
  │                          │   └── Fire click handler
  │                          │   └── Handler updates UI
  │◀── Binary Instructions ──│   └── Txn.commit() flushes
  │                          │
  │    [UI Updated]          │
```

### 9.4 Widget Lifecycle

```java
// 1. CREATION - Widget exists only on server
PButton button = Element.newPButton("Click");
// ID assigned, but nothing sent to client yet

// 2. ATTACHMENT - Widget sent to client
panel.add(button);
// attach() → init() → TYPE_CREATE sent

// 3. UPDATES - Changes queued and batched
button.setText("New Text");
button.addStyleName("primary");
// saveUpdate() queues, commit() sends

// 4. EVENT HANDLING
button.addClickHandler(event -> {
    // Runs in UIContext thread
    label.setText("Clicked!");
});

// 5. DESTRUCTION
button.removeFromParent();
// TYPE_GC sent, memory freed on both sides
```

### 9.5 Garbage Collection Flow

```
Server                    Client
  │                          │
  │── widget.onDestroy() ───▶│
  │                          │
  │── TYPE_GC(id=42) ───────▶│── Remove from DOM
  │                          │── Clear event listeners
  │── Remove from cache      │── Free memory
  │                          │
```

---

## 10. Threading Model

PonySDK is single-threaded per `UIContext` by design — only one thread can update a tab's UI at a time. This eliminates a whole class of concurrency bugs. The rules are simple: background threads must go through `UIContext.execute()`, and you never share `PObject` instances between tabs.

### 10.1 Thread Types

```
┌─────────────────────────────────────────────────────────────────┐
│                    WebSocket Thread (Jetty)                     │
│                                                                 │
│  • Receives client events                                       │
│  • Automatically acquires UIContext lock                        │
│  • Processes within transaction                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│              Virtual Threads (Java 21, PScheduler)               │
│                                                                 │
│  • PScheduler tasks run on virtual threads                      │
│  • CommunicationSanityChecker uses virtual threads              │
│  • SharedDictionaryProvider auto-persist uses virtual threads    │
│  • Reconnection timeout + overflow destroy use virtual threads   │
│  • Dramatically reduces memory per thread (~1KB vs ~1MB)        │
│  • Scales to thousands of concurrent UIContexts                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                    Your Background Threads                      │
│                                                                 │
│  • Data feeds, timers, external events                          │
│  • MUST use UIContext.execute() for UI updates                  │
│  • Framework handles locking and transactions                   │
└─────────────────────────────────────────────────────────────────┘
```

### 10.2 Thread Safety Rules

| Rule | Why |
|------|-----|
| Always use `UIContext.execute()` from background threads | Ensures proper locking and transaction |
| Never share PObjects between UIContexts | Each tab has isolated widget state |
| Use `Application.pushToClients()` for broadcasting | Safe iteration over all tabs |

### 10.3 Example: Server Push

```java
// From a background thread (data feed, timer, etc.)
uiContext.execute(() -> {
    priceLabel.setText(newPrice.toString());
    if (newPrice > threshold) {
        priceLabel.addStyleName("alert");
    }
});

// Broadcast to all tabs of a user
application.pushToClients(notification);
```

---

# Part III: Advanced Topics

## 11. Advanced Features

### 11.1 Web Components (PWebComponent)

PonySDK integrates native HTML Web Components directly from server-side Java, with the same protocol optimizations as built-in widgets.

#### Philosophy

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
│  │   └── PDataGrid       │  │       └── <table>…</table>        │
│  └── onEvent("submit")   │  └── dispatchEvent("submit", detail) │
│                          │                                      │
│  Binary protocol ───────▶│  Shadow DOM (managed by WC itself)   │
│  (properties, slots,     │  ├── <slot name="toolbar">           │
│   events, methods)       │  ├── <slot name="content">           │
│                          │  └── internal styles & structure     │
└──────────────────────────┴──────────────────────────────────────┘
```

#### Creating a Web Component

```java
PWebComponent chart = new PWebComponent("my-chart");

// HTML attributes
chart.attr("aria-label", "Revenue Chart");
chart.removeAttr("aria-label");

// JS properties (JSON-encoded)
chart.property("title").set("\"Revenue Q4\"");
chart.property("theme").set("\"dark\"");

// Custom events
chart.onEvent("point-click", event -> {
    String detail = event.getString("WC_EVENT_DETAIL");
    log.info("Clicked: {}", detail);
});

// Call a JS method on the element
chart.call("refresh");
chart.call("setRange", 0, 100);

// Add to the widget tree like any other widget
flowPanel.add(chart);
```

#### Slot API — inserting PonySDK widgets into a Web Component

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

#### PropertyHandle API — unified storage strategy

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

#### 5-level protocol optimization

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

#### Storage mode summary

| Mode | Server storage | Delta check | JSON patch | Reconnect replay | Use case |
|------|---------------|-------------|------------|------------------|----------|
| `property("x")` (default) | `CompactStringMap` (on-heap) | `Objects.equals` | yes (default) | yes | Standard properties |
| `.offHeap()` | `OffHeapJsonStore` (direct ByteBuffer) | FNV-1a 64-bit hash | yes (default) | yes | Large JSON (datasets, configs) |
| `.stateless()` | None | no | no | no | Streaming, ephemeral data |
| `.withPatch()` | Per mode | Per mode | yes (forced) | yes | Incremental updates |
| `.withoutPatch()` | Per mode | Per mode | no (forced) | yes | Always full replacement |

#### Writing a Web Component for PonySDK

Web Components used with PonySDK must follow one critical rule: **never do DOM work in the constructor**. GWT creates custom elements via `document.createElement()` which invokes the constructor — any DOM access there will crash.

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
// Wrong — crashes when GWT calls document.createElement('my-chart')
class MyChart extends HTMLElement {
    constructor() {
        super();
        this.attachShadow({ mode: 'open' }); // CRASH
        this._render();                       // CRASH
    }
}
```

#### pony.wc — shared stylesheet utilities

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

#### Configuring shared sheets from Java

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

#### Registering Web Components

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

#### Full example: real-time trading dashboard

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
dashboard.onEvent("order-submit", event -> {
    String detail = event.getString("WC_EVENT_DETAIL");
    orderService.submit(parseOrder(detail));
});

flowPanel.add(dashboard);
```

---

### 11.2 Custom JavaScript (PAddOn)

Integrate custom JS libraries:

```java
// Server-side
public class MyChart extends PAddOn {
    public MyChart() {
        super("MyChartAddon");
    }
    
    public void setData(double[] values) {
        callTerminalMethod("setData", values);
    }
}

// Client-side (JavaScript)
function MyChartAddon(id, element, args) {
    this.setData = function(values) {
        // D3.js, Chart.js, etc.
    };
}
```

### 11.3 EventBus Communication

Decouple components:

```java
// Define event
public class PriceUpdateEvent extends BusinessEvent {
    private final String symbol;
    private final double price;
}

// Subscribe
UIContext.get().getRootEventBus().addHandler(
    PriceUpdateEvent.TYPE,
    event -> priceLabel.setText(event.getPrice())
);

// Publish
UIContext.get().getRootEventBus().fireEvent(
    new PriceUpdateEvent("AAPL", 150.25)
);
```

### 11.4 Observability — OpenTelemetry & Dynatrace

PonySDK ships a built-in `PonySDKMetrics` class that exports metrics via the OpenTelemetry API. It is vendor-neutral — you bring your own OTel SDK and exporter. Dynatrace ingests natively via OTLP, no proprietary agent required.

#### Metrics exposed

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

#### Understanding the three latency metrics

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

#### Setup — Dynatrace via OTLP

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
                ResourceAttributes.SERVICE_VERSION, "2.8.x"
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

#### Built-in latency API (no OTel required)

If you don't use OTel, the per-tab latency averages are still available directly:

```java
UIContext ctx = UIContext.get();
double roundtrip = ctx.getRoundtripLatency();  // rolling avg of last 10 measurements (ms)
double network   = ctx.getNetworkLatency();    // network portion only (ms)
double terminal  = ctx.getTerminalLatency();   // client DOM rendering (ms)

if (roundtrip > 200) {
    log.warn("High latency on UIContext #{}: {}ms", ctx.getID(), roundtrip);
}
```

These are rolling averages over the last 10 round-trip measurements, updated automatically by the heartbeat mechanism. Useful for in-process alerting without an external metrics system.

---

### 11.5 Transparent WebSocket Reconnection (opt-in)

By default, PonySDK destroys the `UIContext` when the WebSocket closes. This is the safe default for trading applications — a disconnected trader must never silently resume stale market data.

For applications where seamless reconnection is acceptable, PonySDK provides an opt-in transparent reconnection mechanism. When enabled, the server keeps the `UIContext` alive for a configurable window after disconnect. If the client reconnects within that window, the existing widget tree and all server-side state are reused — no page reload, no re-initialization.

#### Enabling transparent reconnection

```java
ApplicationConfiguration config = new ApplicationConfiguration();
config.setReconnectionTimeoutMs(10_000); // keep UIContext alive for 10s after disconnect
config.setMaxRecordingEntries(10_000);   // max buffered protocol entries (default, ~320KB)
```

If the recording buffer exceeds `maxRecordingEntries` during suspension, the UIContext is destroyed immediately — no reconnection possible. This prevents unbounded memory growth when a high-frequency data feed keeps pushing updates to a disconnected session.

On the client side, set `window.ponyReconnectMode = true` in your bootstrap HTML before PonySDK starts:

```html
<script>window.ponyReconnectMode = true;</script>
```

When both are set, the reconnection flow is:

```
Client                          Server
  |                               |
  |--- WebSocket closes --------->|
  |                               |  UIContext.suspend(10_000ms)
  |                               |  → swap encoder to RecordingEncoder
  |                               |  → start timeout virtual thread
  |                               |
  |  (external threads keep       |  execute() still runs runnables
  |   calling execute())          |  → PObjects mutate Java state
  |                               |  → protocol writes buffered in RecordingEncoder
  |                               |  → creates, adds, removes, updates all captured
  |                               |
  |--- ping GET /app?ping ------->|  (ReconnectionChecker retries)
  |<-- 200 OK --------------------|
  |                               |
  |--- new WS ?Y=<uiContextId> -->|  WebSocket.onWebSocketOpen
  |                               |  → finds suspended UIContext
  |                               |  → UIContext.resume(newSocket)
  |                               |  → compact() merges updates, cancels create+remove
  |                               |  → replay compacted buffer on real WebSocket
  |<-- compacted instructions ----|  (structural ops + merged updates)
  |<-- RECONNECT_CONTEXT ---------|
  |                               |
  | window.onPonyReconnected()    |  UIContext alive, state current
```

#### How state stays consistent during suspension

The key to correctness is the `RecordingEncoder` — a `WebsocketEncoder` implementation that buffers all protocol operations (creates, adds, removes, updates) instead of sending them over the wire.

During suspension:

1. `UIContext.suspend()` swaps the `ModelWriter` encoder to a `RecordingEncoder`
2. `execute(Runnable)` continues to run normally — PObjects mutate their Java fields, and all protocol writes (`TYPE_CREATE`, `TYPE_ADD`, `TYPE_UPDATE`, `TYPE_REMOVE`, etc.) are captured as `(model, value)` pairs in the recorder's buffer
3. Structural operations (create, add, remove) are preserved in order — no data loss

On `resume()`, before replaying:

1. `RecordingEncoder.compact()` runs a compaction pass:
   - Consecutive `TYPE_UPDATE` blocks on the same objectID are merged — last value wins per model key (natural coalescing: 1000 `setText()` → 1 update)
   - If a widget was both created (`TYPE_CREATE`) and removed (`TYPE_REMOVE` / `TYPE_GC`) during the same suspension window, all blocks for that objectID are eliminated entirely
   - Structural operations (`TYPE_ADD`, `TYPE_REMOVE`, `TYPE_ADD_HANDLER`) are preserved in order
2. The compacted buffer is replayed on the real WebSocket encoder in a single transaction
3. `RECONNECT_CONTEXT` is sent to signal the client

```
Suspension (8 seconds)              Reconnexion
                                    
TYPE_CREATE label#42  → recorded    compact():
TYPE_ADD label#42     → recorded      → merge updates on same ID
TYPE_UPDATE label#42  → recorded      → cancel create+remove pairs
  setText("100")                    
TYPE_UPDATE label#42  → recorded    replay to real WebSocket:
  setText("150")                      TYPE_CREATE label#42
TYPE_UPDATE label#42  → recorded      TYPE_ADD label#42
  setText("200")                      TYPE_UPDATE label#42 {text="200"}
                                      RECONNECT_CONTEXT
(3 updates → 1 after compaction)
```

#### Server-side hook on disconnect

Regardless of whether transparent reconnection is enabled, you can register a server-side listener to react when a WebSocket is lost:

```java
config.setReconnectionListener((uiContext) -> {
    log.warn("Connection lost for user {}", uiContext.getAttribute("userId"));
    // execute() still works during suspension — safe to push state updates
});
```

#### Client-side JS hooks

| Hook | When called | Default behaviour |
|------|-------------|-------------------|
| `document.onConnectionLost(cb)` | Immediately on disconnect | — |
| `window.showReconnectionInformation()` | To show custom reconnecting UI | Built-in banner |
| `window.onPonyReconnected()` | After successful reconnection | Page reload (if `ponyReconnectMode` is false) |

#### Trading applications — important note

For electronic trading, transparent reconnection should be used with care. A reconnected session resumes the server-side widget state, but market data feeds may have missed updates during the gap. Always notify traders explicitly:

```javascript
window.onPonyReconnected = function() {
    showBanner("Reconnected — please verify your positions and prices");
};
```

The `ReconnectionListener` on the server side can also trigger risk checks or force a data refresh before the session resumes.

#### RecordingEncoder — architecture deep dive

The `RecordingEncoder` implements `WebsocketEncoder` and captures every protocol operation as a list of `Entry(ServerToClientModel, Object)` records. It is the core of the reconnection engine — without it, structural operations (create, add, remove) would be lost during suspension.

```
┌─────────────────────────────────────────────────────────────────┐
│                    RecordingEncoder internals                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Interface: WebsocketEncoder                                    │
│  ├── beginObject()  → no-op (same as WebSocket.encode)          │
│  ├── encode(model, value) → entries.add(Entry(model, value))    │
│  └── endObject()    → entries.add(Entry(END, null))             │
│                                                                 │
│  Storage: ArrayList<Entry>                                      │
│  ├── Entry = record(ServerToClientModel model, Object value)    │
│  ├── Blocks delimited by END entries                            │
│  └── ~32 bytes per entry → 10,000 entries ≈ 320 KB             │
│                                                                 │
│  Overflow protection:                                           │
│  ├── maxEntries configurable (default 10,000)                   │
│  ├── On overflow: overflowed=true, stop recording               │
│  └── OverflowHandler callback → destroys UIContext              │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Compaction algorithm (`compact()`):**

```
Phase 1 — Parse into blocks (delimited by END entries)
  [CREATE#42, WIDGET, TEXT, END] [UPDATE#42, TEXT, END] [UPDATE#42, TEXT, END]
  → Block[0]: CREATE#42    Block[1]: UPDATE#42    Block[2]: UPDATE#42

Phase 2 — Build create/remove index for cancellation
  firstCreateIdx: {42 → 0}     (block index of first CREATE per objectID)
  firstRemoveIdx: {42 → 5}     (block index of first REMOVE/GC per objectID)
  → Cancel only if CREATE index < REMOVE index (object born during suspension)
  → If REMOVE comes first, object existed before suspension — keep both

Phase 3 — Merge and eliminate
  ├── Cancelled objectIDs: skip all blocks for that ID
  ├── Consecutive UPDATE blocks on same objectID: merge (last value wins per model key)
  │   LinkedHashMap<ServerToClientModel, Object> preserves insertion order
  │   WINDOW_ID and FRAME_ID entries preserved from last block
  └── Structural blocks (CREATE, ADD, REMOVE, ADD_HANDLER, etc.): copy as-is
```

**Example — 8 seconds of suspension with a price feed:**

```
Recorded (raw):                          After compact():
─────────────────────────────────────    ─────────────────────────────────
TYPE_CREATE label#42                     TYPE_CREATE label#42
TYPE_ADD label#42 → panel#10             TYPE_ADD label#42 → panel#10
TYPE_UPDATE label#42 {text="100"}        TYPE_UPDATE label#42 {text="200"}
TYPE_UPDATE label#42 {text="150"}        ← 3 updates merged into 1
TYPE_UPDATE label#42 {text="200"}
                                         
TYPE_CREATE temp#99                      ← entirely eliminated
TYPE_ADD temp#99 → panel#10             ← (created + removed = cancelled)
TYPE_UPDATE temp#99 {text="..."}
TYPE_REMOVE temp#99
TYPE_GC temp#99

5 blocks → 3 blocks (40% reduction)
```

#### Threading model during reconnection

The reconnection engine uses three types of threads:

```
┌─────────────────────────────────────────────────────────────────┐
│              Reconnection thread interactions                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Jetty WS thread (onWebSocketClose)                             │
│  └── calls UIContext.suspend(timeoutMs)                         │
│      ├── sets volatile suspended = true                         │
│      ├── creates RecordingEncoder                               │
│      ├── swaps ModelWriter encoder (not under UIContext lock)    │
│      └── starts timeout virtual thread                          │
│                                                                 │
│  Virtual thread — timeout (Thread.ofVirtual)                    │
│  └── sleeps for timeoutMs                                       │
│      └── if still suspended → destroy()                         │
│          (doDestroy sets suspended=false)                        │
│                                                                 │
│  Virtual thread — overflow (Thread.ofVirtual)                   │
│  └── started by RecordingEncoder.checkOverflow()                │
│      └── destroy() (doDestroy sets suspended=false)             │
│                                                                 │
│  Application threads (data feeds, timers)                       │
│  └── call UIContext.execute(runnable)                            │
│      └── acquires UIContext lock                                 │
│      └── runnable mutates PObjects                              │
│      └── protocol writes → RecordingEncoder.encode()            │
│      └── Txn.commit() → flush goes to RecordingEncoder          │
│                                                                 │
│  New Jetty WS thread (onWebSocketOpen with ?Y=contextId)        │
│  └── finds suspended UIContext via Application.getUIContext()    │
│      └── calls UIContext.resume(newSocket)                       │
│          ├── sets suspended = false                              │
│          ├── compact() + replayTo(newSocket)                     │
│          ├── swaps encoder back to real WebSocket                │
│          ├── sends RECONNECT_CONTEXT                             │
│          └── resets CommunicationSanityChecker timer             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### Concurrency guarantees and known race windows

| Guarantee | Mechanism |
|-----------|-----------|
| `suspended` visibility across threads | `volatile` field — writes by suspend/resume/timeout are immediately visible |
| Protocol writes serialized during suspension | UIContext `ReentrantLock` — only one `execute()` at a time |
| Encoder swap atomicity | `ModelWriter.setEncoder()` is a simple field assignment — safe because `execute()` holds the lock |
| No double-destroy on timeout vs resume | `suspended` flag checked before destroy — only one wins. Only `doDestroy()` and `resume()` reset `suspended` |
| CommunicationSanityChecker doesn't kill suspended context | `isSuspended()` guard at top of `checkCommunicationState()` |
| WebSocket errors on old socket don't destroy suspended context | `isSuspended()` guard in `onWebSocketError()` |

**Known race windows (benign):**

| Race | Impact | Why benign |
|------|--------|------------|
| `suspend()` not under UIContext lock | An `execute()` in progress may write 1-2 entries to the old encoder before the swap | Those entries are already committed to the wire — the client processes them before disconnect |
| `alive` field not volatile | A thread may see stale `alive=true` after destroy | The `execute()` method re-checks under lock — worst case: one extra lock acquisition |
| Timeout thread vs resume thread | Both check `suspended` without lock | Only `doDestroy()` and `resume()` set `suspended=false` — `destroy()` has `isAlive()` guard, so the second caller exits |

#### Sequence diagrams

**Normal disconnect (reconnection disabled — default):**

```
Client              WebSocket           UIContext
  │                    │                    │
  │── WS close ───────▶│                    │
  │                    │── onWebSocketClose  │
  │                    │   config.timeout=0  │
  │                    │──────────────────▶ onDestroy()
  │                    │                    │── alive=false
  │                    │                    │── deregister from Application
  │                    │                    │── fire DestroyListeners
  │                    │                    │── clear PObjectCache
```

**Transparent reconnection — happy path:**

```
Client              WebSocket(old)      UIContext            WebSocket(new)
  │                    │                    │                    │
  │── WS close ───────▶│                    │                    │
  │                    │── onWebSocketClose  │                    │
  │                    │   config.timeout>0  │                    │
  │                    │──────────────────▶ suspend(timeout)     │
  │                    │                    │── suspended=true    │
  │                    │                    │── swap to Recorder  │
  │                    │                    │── start timeout VT  │
  │                    │                    │                    │
  │  (feeds push)      │                    │◀── execute(update)  │
  │                    │                    │── record entries    │
  │                    │                    │                    │
  │── ping GET ───────────────────────────────────────────────▶ │
  │◀── 200 OK ────────────────────────────────────────────────  │
  │                    │                    │                    │
  │── new WS ?Y=id ──────────────────────────────────────────▶ │
  │                    │                    │◀── resume(newSocket)│
  │                    │                    │── suspended=false   │
  │                    │                    │── compact()         │
  │                    │                    │── replayTo(new)     │
  │◀── compacted instructions ──────────────────────────────── │
  │◀── RECONNECT_CONTEXT ──────────────────────────────────── │
  │                    │                    │                    │
  │ onPonyReconnected()│                    │  state current     │
```

**Overflow during suspension:**

```
UIContext            RecordingEncoder     Virtual Thread
  │                    │                    │
  │── execute() ──────▶│                    │
  │   (high-freq feed) │── encode()         │
  │                    │── entries > max     │
  │                    │── overflowed=true   │
  │                    │── callback ────────▶│
  │                    │                    │── Thread.ofVirtual
  │                    │                    │   └── destroy()
  │                    │                    │       └── doDestroy()
  │                    │                    │           sets suspended=false
  │  (no more recording)                    │
```

**Timeout during suspension:**

```
UIContext            Timeout VT
  │                    │
  │── suspend(10s) ───▶│── Thread.sleep(10s)
  │                    │
  │  (10 seconds pass, │
  │   no reconnection) │
  │                    │── if (suspended)
  │                    │── destroy()
  │                    │   └── doDestroy()
  │                    │       sets suspended=false
  │                    │
```

#### Client-side reconnection flow

When `window.ponyReconnectMode = true`, the client-side `ReconnectionChecker` follows this flow:

```
WebSocket.onclose
  │
  ▼
ReconnectionChecker.detectConnectionFailure()
  ├── show reconnection banner (or custom UI)
  └── start retry loop: GET /app?ping every 2s
        │
        ▼ (server responds 200)
      reloadWindow()
        │
        ├── ponyReconnectMode === true?
        │   YES → reconnectWebSocket()
        │         ├── PonySDK.buildReconnectUrl()
        │         │   → ws://host/app/ws?H=token&Y=contextId
        │         └── PonySDK.reconnectSocket(url)
        │             → new WebSocketClient(url, uiBuilder, checker)
        │
        │   NO → hasOnPonyReconnected()?
        │         YES → window.onPonyReconnected()
        │         NO  → location.reload()
```

The `Y` parameter (`ClientToServerModel.RECONNECT_UI_CONTEXT_ID`) carries the UIContext ID. On the server, `WebSocketServlet.createWebSocket()` reads it and sets `webSocket.setReconnectContextId(id)`. When `onWebSocketOpen` fires, the WebSocket looks up the suspended UIContext via `Application.getUIContext(id)` and calls `resume()`.

#### Test coverage

The reconnection engine is covered by dedicated tests across three test classes:

| Test class | Tests | Focus |
|------------|-------|-------|
| `RecordingEncoderTest` | ~65 | Record/replay, compaction, create+remove cancellation, overflow, REMOVE-before-CREATE ordering, mass stress, memory leak (WeakRef+GC) |
| `UIContextReconnectionTest` | ~115 | Suspend/resume lifecycle, timeout, overflow, concurrent execute, race conditions (timeout vs resume, execute vs suspend), flush path coherence, encoder swap atomicity, deadlock, memory leak, EventBus/PScheduler/Txn ThreadLocal cleanup |
| `WebSocketTest` | ~47 | Default vs reconnection mode close/error, suspended error guard, timeout, double close, listener exceptions, memory leak |

---

## 12. Package Structure

The codebase is split into three main areas: `server` (runtime, transport, transactions), `ui` (widgets and event system), and `terminal` (GWT code compiled to JavaScript that runs in the browser). The `util` package contains performance-critical data structures shared by both sides.

```
com.ponysdk.core
├── model/              # Protocol enums
│   ├── ServerToClientModel.java
│   ├── ClientToServerModel.java
│   └── WidgetType.java
│
├── server/
│   ├── application/    # Core runtime
│   │   ├── Application.java
│   │   ├── UIContext.java
│   │   ├── ApplicationManager.java
│   │   └── StringDictionary.java  # Protocol optimization
│   ├── stm/            # Transactions
│   │   └── Txn.java
│   ├── websocket/      # Transport (Jetty 12.0.18 native API)
│   │   ├── WebSocket.java         # Session.Listener.AutoDemanding + request caching
│   │   ├── WebSocketPusher.java   # ByteBuffer accumulator + async sendBinary
│   │   ├── WebsocketEncoder.java
│   │   └── RecordingEncoder.java   # Buffering encoder for suspension (compact + replay)
│   ├── metrics/        # OpenTelemetry instrumentation (opt-in)
│   │   └── PonySDKMetrics.java    # Gauges, counters, histograms → OTLP / Dynatrace
│   └── servlet/        # HTTP layer
│       └── SessionManager.java
│
├── ui/
│   ├── basic/          # 60+ widgets
│   │   ├── PObject.java
│   │   ├── PWidget.java
│   │   ├── PButton.java
│   │   ├── PWebComponent.java  # Web Component integration
│   │   └── ...
│   ├── eventbus/       # Event system
│   └── datagrid2/      # Advanced grid
│
├── terminal/           # Client-side (GWT 2.13.0, elemental2, sourceLevel 17)
│   ├── UIBuilder.java
│   ├── ui/PTWebComponent.java  # Terminal-side Web Component handler
│   └── CommunicationEntryPoint.java
│
├── util/
│   ├── CompactStringMap.java   # Flat String[] map for small property sets
│   └── OffHeapJsonStore.java   # Off-heap ByteBuffer storage for large JSON
│
└── writer/             # Serialization
    └── ModelWriter.java
```

---

## 13. Configuration Reference

### 13.1 ApplicationConfiguration

| Property | Description | Default |
|----------|-------------|---------|
| `heartbeatPeriod` | WebSocket keep-alive (ms) | 5000 |
| `sessionTimeout` | HTTP session timeout (s) | 900 |
| `maxInactivity` | Max client inactivity (ms) | 60000 |
| `enableCompression` | permessage-deflate | true |
| `debugMode` | Verbose logging | false |
| `stringDictionaryEnabled` | Enable string interning | true |
| `stringDictionaryMaxSize` | Max dictionary entries | 65535 |
| `stringDictionaryMinLength` | Min string length to intern | 4 |
| `wcSharedSheets` | CSS URLs injected into every WC shadow root via `pony.wc` | null (opt-in) |
| `metrics` (via `WebSocketServlet.setMetrics()`) | OTel instrumentation bridge | null (opt-in) |
| `reconnectionTimeoutMs` | Transparent WS reconnection window (ms). 0 = disabled (destroy on close) | 0 |
| `maxRecordingEntries` | Max buffered protocol entries during suspension. Exceeded → destroy | 10000 |
| `reconnectionListener` | Server-side hook called on WS disconnect | null |

### 13.2 Minimal Setup

```java
public class Main {
    public static void main(String[] args) throws Exception {
        ApplicationConfiguration config = new ApplicationConfiguration();
        config.setApplicationName("MyApp");
        config.setEntryPointClass(MyEntryPoint.class);

        JavaApplicationManager appManager = new JavaApplicationManager();
        appManager.setConfiguration(config);

        PonySDKServer server = new PonySDKServer();
        server.setApplicationManager(appManager);
        server.setPort(8080);
        server.setHost("0.0.0.0");
        server.start();
    }
}

public class MyEntryPoint implements EntryPoint {
    @Override
    public void start(UIContext uiContext) {
        PFlowPanel root = Element.newPFlowPanel();

        PLabel label = Element.newPLabel("Hello PonySDK!");
        PButton button = Element.newPButton("Click me");
        button.addClickHandler(e -> label.setText("Clicked!"));

        root.add(label);
        root.add(button);
        PRootPanel.get().add(root);
    }
}
```

---

## 14. Jetty 12 / Jakarta EE Migration Notes

### 14.1 Technology Stack

| Component | Version |
|-----------|---------|
| Java (server) | 21 (virtual threads) |
| Java (GWT terminal) | 17 (`-sourceLevel 17`) |
| Jetty | 12.0.18 (EE10) |
| Spring | 6.2.9 |
| Servlet API | jakarta.servlet 6.0.0 |
| JSON-P | jakarta.json 2.0.2 + Eclipse Parsson 1.1.7 |
| WebSocket | Jetty native API (`Session.Listener.AutoDemanding`) |
| GWT | 2.13.0 (`org.gwtproject`, sourceLevel 17) |
| elemental2 | 1.2.1 (dom, core, webstorage) |
| Selenium | 4.27.0 |
| Tyrus | 2.2.0 |
| SLF4J | 2.0.17 |
| Mockito | 5.15.2 |
| JUnit | 4.13.2 / Jupiter 5.11.4 |
| Gradle | 8.14.4 |

### 14.2 HTTP Request Recycling After WebSocket Upgrade

This is the most critical pitfall of Jetty 12. Unlike Jetty 9, Jetty 12 **recycles the HTTP request object** after the WebSocket upgrade. Any access to `JettyServerUpgradeRequest.getParameterMap()`, `getHeader()`, or `getHttpServletRequest()` after `onWebSocketOpen()` will throw a `NullPointerException`.

**Solution — cache everything during `setRequest()`**, which is called while the request is still valid:

```java
// Called during createWebSocket(), request is still valid here
public void setRequest(final JettyServerUpgradeRequest request) {
    this.request = request;
    this.cachedParameterMap = request.getParameterMap();
    this.cachedUserAgent    = request.getHeader("User-Agent");
    this.cachedHttpSession  = request.getHttpServletRequest().getSession();
}
```

`UIContext` and `EntryPoint` implementations use the cached getters (`getCachedParameterMap()`, etc.) instead of accessing the HTTP request directly.

### 14.3 WebSocket API Changes (Jetty 9 → Jetty 12)

| Jetty 9 | Jetty 12 |
|---------|----------|
| `WebSocketListener` | `Session.Listener.AutoDemanding` |
| `onWebSocketConnect(Session)` | `onWebSocketOpen(Session)` |
| `onWebSocketBinary(byte[], int, int)` | `onWebSocketBinary(ByteBuffer, Callback)` |
| `WriteCallback` | `Callback` |
| `session.getRemote().sendBytes(buf, cb)` | `session.sendBinary(buf, cb)` |
| `session.close()` | `session.close(statusCode, reason, Callback.NOOP)` |

### 14.4 Simplified WebSocketPusher

The old `WebSocketPusher` extended `AutoFlushedBuffer` with a complex double-buffering and async flush mechanism. It has been rewritten as a simple `ByteBuffer` accumulator with async send via `session.sendBinary()` and `CountDownLatch` for synchronization.

### 14.5 BinaryModel — UINT31 Type Support

The `UINT31` type in `ValueTypeModel` is used by some server models (e.g. `CREATE_CONTEXT`). The GWT client `BinaryModel.toString()` must handle this type, otherwise the first WebSocket message (handshake) crashes on the client with `IllegalArgumentException`.

### 14.6 PonyPerMessageDeflateExtension Removed

The custom compression monitoring extension has been removed. Jetty 12 handles `permessage-deflate` natively. Frame-level monitoring is no longer available but can be re-implemented via the Jetty 12 core Extension API if needed.

### 14.7 GWT Terminal — elemental2 Migration and sourceLevel 17

Code in `ponysdk/src/main/java/com/ponysdk/core/terminal/` is compiled by GWT 2.13.0 to JavaScript with `-sourceLevel 17`.

**elemental2 migration:** The old `gwt-elemental` dependency (package `elemental.*`) has been fully replaced by `elemental2-dom`, `elemental2-core`, and `elemental2-webstorage` (package `elemental2.*`). All terminal files now use native elemental2 JsInterop wrappers.

**Java 17 in the terminal:** GWT 2.13.0 supports `-sourceLevel 17`, enabling switch expressions and `instanceof` pattern matching in terminal code compiled to JavaScript.

**JSNI → JsInterop:** Most JSNI methods have been migrated to elemental2/JsInterop calls. Remaining JSNI methods are those that access `$wnd`, `$doc`, or use GWT-specific patterns (eval, bind, sendToNative).

> **Important:** Terminal code must **never** be migrated to the `jakarta.*` namespace. GWT does not support Jakarta.

---

## 15. Troubleshooting

### 15.1 Connection Issues

#### WebSocket Connection Failed

**Symptom:** Browser shows "WebSocket connection failed" or app doesn't load.

**Causes & Solutions:**

| Cause | Solution |
|-------|----------|
| Wrong port | Verify `config.setPort()` matches URL |
| Firewall blocking WS | Allow WebSocket traffic (port 80/443 or custom) |
| Proxy not configured | Configure proxy for WebSocket upgrade |
| SSL mismatch | Use `wss://` for HTTPS, `ws://` for HTTP |

```java
// Debug: Enable verbose logging
config.setDebugMode(true);

// Check server is listening
System.out.println("Server started on port: " + config.getPort());
```

#### Connection Drops Frequently

**Symptom:** Users get disconnected randomly.

**Solutions:**
```java
// Increase heartbeat tolerance
config.setHeartbeatPeriod(10000);  // 10 seconds instead of 5

// Check server logs for
// "Heartbeat timeout" → client network issues
// "WebSocket error" → check Jetty logs
```

---

### 15.2 UI Not Updating

#### Changes Not Visible in Browser

**Symptom:** You call `label.setText()` but nothing changes.

**Cause 1: Not in UIContext**
```java
// WRONG - from background thread without execute()
new Thread(() -> {
    label.setText("Updated");  // Silent failure!
}).start();

// CORRECT - wrap in execute()
new Thread(() -> {
    uiContext.execute(() -> {
        label.setText("Updated");
    });
}).start();
```

**Cause 2: Widget not attached**
```java
// WRONG - widget never added to DOM
PLabel label = Element.newPLabel("Hello");
label.setText("Updated");  // Queued but never sent

// CORRECT - attach first
panel.add(label);
label.setText("Updated");  // Now it works
```

**Cause 3: Transaction not committed**
```java
// WRONG - manual acquire without commit
uiContext.acquire();
label.setText("Updated");
uiContext.release();  // No flush!

// CORRECT - use execute() which auto-commits
uiContext.execute(() -> {
    label.setText("Updated");
});  // Auto-flush here
```

---

### 15.3 Memory Issues

#### OutOfMemoryError on Server

**Symptom:** Server crashes with `java.lang.OutOfMemoryError: Java heap space`

**Diagnosis:**
```java
// Add to your code to monitor
Runtime rt = Runtime.getRuntime();
long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
System.out.println("Heap used: " + usedMB + " MB");
```

**Common Causes:**

| Cause | Solution |
|-------|----------|
| Widgets not destroyed | Call `removeFromParent()` on unused widgets |
| Event handlers accumulating | Remove handlers when done |
| Too many UIContexts | Check for session leaks |
| Large DataGrids | Use pagination or virtual scrolling |

```java
//  Properly cleanup widgets
public void closeDialog() {
    dialog.removeFromParent();  // Triggers GC on client too
    dialog = null;              // Allow Java GC
}

//  Remove handlers when done
HandlerRegistration reg = button.addClickHandler(e -> {...});
// Later:
reg.removeHandler();
```

#### Memory Leak Detection

```java
// Monitor UIContext count
Application app = UIContext.get().getApplication();
int contextCount = app.getUIContexts().size();
System.out.println("Active tabs: " + contextCount);

// Monitor widget count (approximate)
int widgetCount = UIContext.get().getPObjectCache().size();
System.out.println("Widgets in this tab: " + widgetCount);
```

---

### 15.4 Threading Issues

#### IllegalStateException: No UIContext

**Symptom:** `java.lang.IllegalStateException: No UIContext`

**Cause:** Accessing UI from wrong thread.

```java
// WRONG - UIContext.get() returns null outside execute()
new Thread(() -> {
    UIContext ctx = UIContext.get();  // NULL!
    ctx.execute(...);  // NPE!
}).start();

// CORRECT - keep reference before spawning thread
UIContext ctx = UIContext.get();  // Capture in UI thread
new Thread(() -> {
    ctx.execute(() -> {
        // Safe UI access here
    });
}).start();
```

#### Deadlock

**Symptom:** Application freezes, threads blocked.

**Common Cause:** Nested `execute()` calls or mixing with other locks.

```java
// WRONG - nested execute() can deadlock
uiContext.execute(() -> {
    someService.doSomething();  // If this calls execute() → deadlock!
});

// CORRECT - execute() is reentrant, but avoid deep nesting
// Better: do non-UI work outside execute()
Object result = someService.doSomething();
uiContext.execute(() -> {
    label.setText(result.toString());
});
```

---

### 15.5 Event Handling Issues

#### Click Handler Not Firing

**Symptom:** Button click does nothing.

**Checklist:**
```java
// 1. Handler registered?
button.addClickHandler(e -> {
    System.out.println("Clicked!");  // Add debug log
    label.setText("Clicked");
});

// 2. Widget enabled?
button.setEnabled(true);  // Disabled widgets don't fire events

// 3. Widget visible?
button.setVisible(true);  // Hidden widgets don't receive events

// 4. Not covered by another widget?
// Check z-index and overlapping elements
```

#### Event Fired Multiple Times

**Symptom:** One click triggers handler 3 times.

**Cause:** Handler registered multiple times.

```java
// WRONG - called every time view is shown
public void showView() {
    button.addClickHandler(e -> doSomething());  // Accumulates!
}

// CORRECT - register once
private boolean handlersRegistered = false;

public void showView() {
    if (!handlersRegistered) {
        button.addClickHandler(e -> doSomething());
        handlersRegistered = true;
    }
}

//  BETTER - register in constructor
public MyView() {
    button.addClickHandler(e -> doSomething());
}
```

---

### 15.6 Performance Issues

#### Slow Initial Load

**Symptom:** App takes 5+ seconds to show UI.

**Solutions:**

```java
// 1. Lazy load heavy components
public void start(UIContext ctx) {
    // Show skeleton immediately
    PRootPanel.get().add(loadingSpinner);
    
    // Load heavy stuff async
    scheduler.schedule(() -> {
        ctx.execute(() -> {
            loadingSpinner.removeFromParent();
            PRootPanel.get().add(createHeavyUI());
        });
    }, 100, TimeUnit.MILLISECONDS);
}

// 2. Reduce initial widget count
// Don't create all tabs upfront - create on demand

// 3. Enable compression
config.setEnableCompression(true);
```

#### Laggy UI Updates

**Symptom:** UI feels slow, updates delayed.

**Diagnosis:**
```java
// Check latency
UIContext ctx = UIContext.get();
System.out.println("Roundtrip: " + ctx.getRoundtripLatency() + "ms");
System.out.println("Network: " + ctx.getNetworkLatency() + "ms");
System.out.println("Terminal: " + ctx.getTerminalLatency() + "ms");
```

| High Value | Likely Cause | Solution |
|------------|--------------|----------|
| Network > 100ms | Slow connection | Use CDN, reduce payload |
| Terminal > 50ms | Heavy DOM updates | Batch updates, simplify UI |
| Roundtrip spikes | Server overloaded | Profile handlers, add caching |

---

### 15.7 Quick Diagnostic Checklist

```
┌─────────────────────────────────────────────────────────────────┐
│                    Troubleshooting Checklist                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [ ] Is the server running? (check logs)                          │
│  [ ] Is WebSocket connected? (browser dev tools → Network → WS)   │
│  [ ] Is UIContext available? (UIContext.get() != null)            │
│  [ ] Is widget attached? (added to a parent in DOM)               │
│  [ ] Is widget enabled/visible?                                   │
│  [ ] Are you in execute() block for background threads?           │
│  [ ] Check browser console for JS errors                          │
│  [ ] Check server logs for exceptions                             │
│  [ ] Monitor memory usage (heap)                                  │
│  [ ] Monitor latency (roundtrip/network/terminal)                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 16. Trade-offs & Comparisons

### 16.1 Benefits

| Benefit | Details |
|---------|---------|
| **100% Java** | No JavaScript required |
| **Type Safety** | Compile-time checking |
| **Security** | Logic stays on server |
| **Debugging** | Server-side breakpoints |
| **Testing** | Standard JUnit |
| **Real-time** | Native WebSocket push |

### 16.2 Limitations

| Limitation | Details | Mitigation |
|------------|---------|------------|
| **Memory** | Server holds full UI state per tab | Destroy unused widgets promptly |
| **Latency** | Every interaction requires a network round-trip | Binary protocol + batching keeps it < 50ms on LAN |
| **Offline** | Requires a live WebSocket connection | Not suitable for PWAs or offline-first apps |
| **Scalability** | Session affinity required | Use sticky sessions on load balancer |

### 16.3 PonySDK vs Vaadin

| Aspect | PonySDK | Vaadin Flow |
|--------|---------|-------------|
| **Protocol** | Binary WebSocket | JSON + HTTP hybrid |
| **Client runtime** | GWT 2.13.0 compiled JS (elemental2 + JsInterop) | Lit / Web Components |
| **Efficiency** | Minimal overhead, 5-level optimization | More verbose |
| **Widgets** | ~60 core + native Web Components | 40+ with Lumo theme |
| **Web Components** | First-class via `PWebComponent` + `pony.wc` | Native Vaadin components |
| **Learning curve** | Simpler API, pure Java | More features, some JS required |
| **License** | Apache 2.0 | Apache + Commercial |

**Choose PonySDK for:** Low-latency trading apps, lightweight deployments, full server-side control, Web Component integration without JS boilerplate.

**Choose Vaadin for:** Rich design system needs, hybrid server/client apps, large teams with mixed Java/JS skills.

---

## 17. Widget Hierarchy Reference

All widgets extend `PWidget`, which extends `PObject`. Panels are containers that can hold children; focus widgets are interactive elements. `PWebComponent` sits under `PComplexPanel` because it can contain slotted children like any other panel.

```
PObject
└── PWidget
    ├── PPanel
    │   ├── PComplexPanel
    │   │   ├── PFlowPanel
    │   │   ├── PVerticalPanel
    │   │   ├── PHorizontalPanel
    │   │   ├── PAbsolutePanel
    │   │   └── PWebComponent
    │   ├── PSimplePanel
    │   │   ├── PScrollPanel
    │   │   ├── PFocusPanel
    │   │   └── PPopupPanel → PDialogBox
    │   └── PCellPanel
    │       ├── PDockLayoutPanel
    │       └── PSplitLayoutPanel
    ├── PFocusWidget
    │   ├── PButtonBase → PButton, PPushButton
    │   ├── PTextBoxBase → PTextBox, PPasswordTextBox, PTextArea
    │   ├── PListBox
    │   └── PCheckBox → PRadioButton
    ├── PLabel → PHTML
    ├── PImage
    └── PAddOn
```

---

## 18. Architectural Patterns Summary

| Pattern | Where Used |
|---------|------------|
| **Proxy** | PObject ↔ DOM Element |
| **Command** | ServerToClientModel instructions |
| **Observer** | EventBus |
| **Factory** | Element.newPButton() |
| **Singleton** | UIContext.get() |
| **STM** | Txn for atomic updates |
| **Handle/Flyweight** | PropertyHandle (unified property API) |
| **Strategy** | StorageMode (ON_HEAP, OFF_HEAP, STATELESS) |
| **Facade** | SlotPanel — hides slot="name" protocol detail |
| **Template Method** | PonyElement.connectedCallback → initShadow + _render |
| **Observer (external)** | PonySDKMetrics → OTel SDK → Dynatrace |
| **Memento / Recording** | RecordingEncoder captures protocol state for replay on reconnection |
| **Null Object** | RecordingEncoder absorbs writes during suspension (no-op from caller's perspective) |

---

*Last updated: February 2026*
