# Troubleshooting

## 15.1 Connection Issues

### WebSocket Connection Failed

**Symptom:** Browser shows "WebSocket connection failed" or app doesn't load.

**Causes & Solutions:**

| Cause | Solution |
|-------|----------|
| Wrong port or path | Verify `server.setPort()` and `applicationContextName`: the URL is `http://host:port/<name>/` |
| Firewall blocking WS | Allow WebSocket traffic (port 80/443 or custom) |
| Proxy not configured | Configure proxy for WebSocket upgrade |
| SSL mismatch | Use `wss://` for HTTPS, `ws://` for HTTP |

```java
// Debug: Enable verbose logging
config.setDebugMode(true);

// Check server is listening
System.out.println("Server started on port: " + config.getPort());
```

### Connection Drops Frequently

**Symptom:** Users get disconnected randomly.

**Solutions:**
```java
// Increase heartbeat tolerance
config.setHeartBeatPeriod(10_000);  // ms: 10 seconds instead of 5

// Check server logs for
// "Nothing received on UIContext #... not even a pong: the connection is dead" → the client went silent (network issues)
// "WebSocket lost on UIContext #..." → the connection ended under the server (EOF, reset)
// "WebSocket Error on UIContext #..." → anything else: check Jetty logs
```

---

## 15.2 UI Not Updating

### Changes Not Visible in Browser

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

## 15.3 Memory Issues

### OutOfMemoryError on Server

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

### Memory Leak Detection

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

## 15.4 Threading Issues

### IllegalStateException: No UIContext

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

### Deadlock

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

## 15.5 Event Handling Issues

### Click Handler Not Firing

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

### Event Fired Multiple Times

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

## 15.6 Performance Issues

### Slow Initial Load

**Symptom:** App takes 5+ seconds to show UI.

**Solutions:**

```java
// 1. Lazy load heavy components
public void start(UIContext ctx) {
    // Show skeleton immediately
    PWindow.getMain().add(loadingSpinner);
    
    // Load heavy stuff async
    scheduler.schedule(() -> {
        ctx.execute(() -> {
            loadingSpinner.removeFromParent();
            PWindow.getMain().add(createHeavyUI());
        });
    }, 100, TimeUnit.MILLISECONDS);
}

// 2. Reduce initial widget count
// Don't create all tabs upfront - create on demand

// 3. Enable compression
config.setEnableCompression(true);
```

### Laggy UI Updates

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

## 15.7 Quick Diagnostic Checklist

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
