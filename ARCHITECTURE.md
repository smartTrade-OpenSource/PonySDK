# PonySDK Architecture

## Table of Contents

How PonySDK works inside. To use it, start with the [guides](docs/guide/README.md).

1. [What is PonySDK?](#1-what-is-ponysdk)
2. [Core Philosophy](#2-core-philosophy)
3. [High-Level Architecture](#3-high-level-architecture)
4. [Key Concepts](#4-key-concepts)
5. [Communication Layer](#5-communication-layer)
6. [Performance Architecture](#6-performance-architecture)
7. [Component Deep Dive](#7-component-deep-dive)
8. [Lifecycle & Flows](#8-lifecycle--flows)
9. [Threading Model](#9-threading-model)
10. [Transparent reconnection: how it works](#10-transparent-reconnection-how-it-works)
11. [Package Structure](#11-package-structure)
12. [Jetty 12 / Jakarta EE Migration Notes](#12-jetty-12--jakarta-ee-migration-notes)
13. [Trade-offs & Comparisons](#13-trade-offs--comparisons)
14. [Widget Hierarchy Reference](#14-widget-hierarchy-reference)
15. [Architectural Patterns Summary](#15-architectural-patterns-summary)

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

## 2. Core Philosophy

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
| **Debugging** | Server-side breakpoints, standard logging; in debug mode, each element names its Java class and line, and an inspector shows the bytes per element ([ADR-0016](docs/decisions/0016-development-tools.md)) |
| **Testing** | JUnit tests for UI logic |

---

## 3. High-Level Architecture

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
│  │  Terminal (terminal.js) -- interprets instructions         │  │
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
│  │  Jetty 12 (EE11)                                           │  │
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
| **Browser** | Rendering, user input | `terminal.js` (TypeScript, `ponysdk/terminal/`) |

---

## 4. Key Concepts

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

## 5. Communication Layer

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

Events go up as JSON text. The keys are the short values of `ClientToServerModel` (`"0"` for `OBJECT_ID`); the
example below writes their names instead, to be read:

```json
{
  "APPLICATION_INSTRUCTIONS": [{
    "OBJECT_ID": 42,
    "DOM_HANDLER_TYPE": 0,
    "EVENT_INFO": [150, 200, 38, 11, 1, false, false, false, false],
    "WIDGET_POSITION": [112, 189, 32, 96]
  }]
}
```

| Field | Purpose |
|-------|---------|
| `OBJECT_ID` | Which widget was interacted with |
| `DOM_HANDLER_TYPE` | What happened: the ordinal of `DomHandlerType` (`CLICK`, `KEY_UP`, …) |
| `EVENT_INFO` | For a mouse event: the pointer in the page and in the widget, the button, the modifier keys |
| `WIDGET_POSITION` | The widget's left, top, height and width |

---

## 6. Performance Architecture

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
double roundtrip = ctx.getRoundtripLatency();  // server -> terminal -> server
double network = ctx.getNetworkLatency();       // the roundtrip minus the time spent in the terminal
double terminal = ctx.getTerminalLatency();     // the probe's wait in the terminal: a busy page shows here

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

## 7. Component Deep Dive

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

// Custom factory (testing, theming): your implementation of ElementFactory
Element.setElementFactory(new MyElementFactory());
```

---

## 8. Lifecycle & Flows

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

## 9. Threading Model

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
│              Virtual Threads (Java 25, PScheduler)               │
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

## 10. Transparent reconnection: how it works

How to enable and use it: [docs/guide/reconnection.md](docs/guide/reconnection.md). How it works:

### RecordingEncoder — architecture deep dive

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

### Threading model during reconnection

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

### Concurrency guarantees and known race windows

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

### Sequence diagrams

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

### Client-side reconnection flow

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

### Test coverage

The reconnection engine is covered by dedicated tests across three test classes:

| Test class | Tests | Focus |
|------------|-------|-------|
| `RecordingEncoderTest` | ~65 | Record/replay, compaction, create+remove cancellation, overflow, REMOVE-before-CREATE ordering, mass stress, memory leak (WeakRef+GC) |
| `UIContextReconnectionTest` | ~115 | Suspend/resume lifecycle, timeout, overflow, concurrent execute, race conditions (timeout vs resume, execute vs suspend), flush path coherence, encoder swap atomicity, deadlock, memory leak, EventBus/PScheduler/Txn ThreadLocal cleanup |
| `WebSocketTest` | ~47 | Default vs reconnection mode close/error, suspended error guard, timeout, double close, listener exceptions, memory leak |

---


## 11. Package Structure

The codebase is split into three main areas: `server` (runtime, transport, transactions), `ui` (widgets and event system), and the terminal (TypeScript that runs in the browser, under `ponysdk/terminal`). The `util` package contains performance-critical data structures shared by both sides.

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
│   ├── websocket/      # Transport (Jetty 12.1 native API)
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
├── util/
│   ├── CompactStringMap.java   # Flat String[] map for small property sets
│   └── OffHeapJsonStore.java   # Off-heap ByteBuffer storage for large JSON
│
└── writer/             # Serialization
    └── ModelWriter.java
```

---

## 12. Jetty 12 / Jakarta EE Migration Notes

### 14.1 Technology Stack

| Component | Version |
|-----------|---------|
| Java (server) | 25 (virtual threads) |
| TypeScript (terminal) | 7.0, esbuild 0.28 |
| Jetty | 12.1.14 (EE11) |
| Spring | 7.0.10 |
| Servlet API | jakarta.servlet 6.1.0 |
| JSON-P | jakarta.json 2.1.3 + Eclipse Parsson 1.1.9 |
| WebSocket | Jetty native API (`Session.Listener.AutoDemanding`) |
| Selenium | 4.51.0 |
| Tyrus | 2.2.2 |
| SLF4J | 2.0.20 |
| Mockito | 5.24.0 |
| JUnit | Jupiter 6.1.3 |
| Gradle | 9.8.2 |

### 14.2 HTTP Request Recycling After WebSocket Upgrade

This is the most critical pitfall of Jetty 12. Unlike Jetty 9, Jetty 12 **recycles the HTTP request object** after the WebSocket upgrade. Any access to `JettyServerUpgradeRequest.getParameterMap()`, `getHeader()`, or `getHttpServletRequest()` after `onWebSocketOpen()` will throw a `NullPointerException`.

**Solution — a snapshot taken during the handshake.** `WebSocketServlet.captureUpgradeData(request)` copies what
the application may read later (parameters, headers, cookies, host, TLS, remote address, HTTP session) into an
`UpgradeRequestData` while the request is still valid; an override starts from `super.captureUpgradeData(request)`.
The WebSocket and the `UIContext` keep only the snapshot, never the request: `UIContext.getParameterMap()`,
`getHeader(s)`, `getRequestCookies()`, `getHost()`, `isSecure()`, `getRemoteSocketAddress()`, `getUserAgent()` and
`getSession()` serve it.

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

The `UINT31` type in `ValueTypeModel` is used by some server models (e.g. `CREATE_CONTEXT`). The terminal's reader must handle this type, otherwise the first WebSocket message (handshake) fails on the client.

### 14.6 PonyPerMessageDeflateExtension Removed

The custom compression monitoring extension has been removed. Jetty 12 handles `permessage-deflate` natively. Frame-level monitoring is no longer available but can be re-implemented via the Jetty 12 core Extension API if needed.

### 14.7 TypeScript Terminal

The terminal is written in TypeScript under `ponysdk/terminal` and replaced the GWT one
([ADR-0002](docs/decisions/0002-javascript-terminal.md)). Gradle provisions Node: `terminalModel` writes the protocol
model (`model.ts`) from `com.ponysdk.core.model`, `terminalCheck` type-checks, `terminalTest` runs its tests on Node
against frames the server's write path produces, and `terminalBundle` builds `ponyterminal/terminal.js` into the jar.

What the server receives from it is checked against traces recorded with the GWT terminal
(`sample/tests/oracle.spec.js`).

---

## 13. Trade-offs & Comparisons

### 16.1 Benefits

| Benefit | Details |
|---------|---------|
| **100% Java** | No JavaScript required |
| **Type Safety** | Compile-time checking |
| **Security** | Logic stays on server |
| **Debugging** | Server-side breakpoints, the inspector in debug mode, hot reload |
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
| **Client runtime** | TypeScript terminal on native elements and ARIA | Lit / Web Components |
| **Efficiency** | Minimal overhead, 5-level optimization | More verbose |
| **Widgets** | ~60 core + native Web Components | 40+ with Lumo theme |
| **Web Components** | First-class via `PWebComponent` + `pony.wc` | Native Vaadin components |
| **Learning curve** | Simpler API, pure Java | More features, some JS required |
| **License** | Apache 2.0 | Apache + Commercial |

**Choose PonySDK for:** Low-latency trading apps, lightweight deployments, full server-side control, Web Component integration without JS boilerplate.

**Choose Vaadin for:** Rich design system needs, hybrid server/client apps, large teams with mixed Java/JS skills.

---

## 14. Widget Hierarchy Reference

Widgets extend `PWidget`, which extends `PObject`; `PAddOn` and `PHost` are `PObject`s without a widget of their
own. Panels hold children. `PWebComponent` and `PElement` sit under `PComplexPanel`: they hold children like any
panel. The main classes, from their `extends`:

```
PObject
├── PWidget
│   ├── PPanel
│   │   ├── PComplexPanel
│   │   │   ├── PFlowPanel
│   │   │   ├── PAbsolutePanel
│   │   │   ├── PCellPanel → PVerticalPanel, PHorizontalPanel
│   │   │   ├── PDockLayoutPanel → PSplitLayoutPanel
│   │   │   ├── PElement (Element.newDiv(), newSpan(), …)
│   │   │   └── PWebComponent
│   │   └── PSimplePanel
│   │       ├── PScrollPanel
│   │       ├── PFocusPanel
│   │       └── PPopupPanel → PDecoratedPopupPanel → PDialogBox
│   ├── PFocusWidget
│   │   ├── PButtonBase
│   │   │   ├── PButton → PPushButton
│   │   │   └── PCheckBox → PRadioButton
│   │   ├── PValueBoxBase → PTextBoxBase
│   │   │   ├── PTextBox → PPasswordTextBox
│   │   │   └── PTextArea
│   │   └── PListBox
│   ├── PLabel → PHTML
│   └── PImage
├── PAddOn → PAddOnComposite
└── PHost
```

---

## 15. Architectural Patterns Summary

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

*Last updated: October 2026*
