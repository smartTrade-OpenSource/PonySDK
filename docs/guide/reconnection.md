# Transparent WebSocket reconnection

By default, PonySDK destroys the `UIContext` when the WebSocket closes. This is the safe default for trading applications — a disconnected trader must never silently resume stale market data.

For applications where seamless reconnection is acceptable, PonySDK provides an opt-in transparent reconnection mechanism. When enabled, the server keeps the `UIContext` alive for a configurable window after disconnect. If the client reconnects within that window, the existing widget tree and all server-side state are reused — no page reload, no re-initialization.

## Enabling transparent reconnection

```java
ApplicationConfiguration config = new ApplicationConfiguration();
config.setReconnectionTimeoutMs(10_000); // keep UIContext alive for 10s after disconnect
config.setMaxRecordingEntries(10_000);   // max buffered protocol entries (default, ~320KB)
```

If the recording buffer exceeds `maxRecordingEntries` during suspension, the UIContext is destroyed immediately — no reconnection possible. This prevents unbounded memory growth when a high-frequency data feed keeps pushing updates to a disconnected session.

On the client side, the bootstrap page sets `window.ponyReconnectMode = true` itself when `reconnectionTimeoutMs > 0`
(`BootstrapServlet.addScript`). A page of your own that loads the terminal sets it before the terminal starts:

```html
<script>window.ponyReconnectMode = true;</script>
```

The reconnection flow is then:

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

A reconnection to a context the server no longer has (restarted, window over, overflow) or that another socket
resumed gets a `CREATE_CONTEXT` block with no context behind it, then a normal close: the terminal reloads its page,
and no application starts for a page that is going away.

A page left (closed, reloaded, navigated away, and not kept in the back-forward cache) closes its socket with 4002:
the server destroys its context at once instead of suspending it, since no reconnection can follow
([ADR-0005](../decisions/0005-page-left-and-terminal-time.md)). A browser may close the socket first, with 1001
(Firefox when it navigates away, Chrome when it quits): a 1001 with no error before it counts the same; Jetty's own
1001, on its idle timeout, follows the error it reports ([ADR-0007](../decisions/0007-page-left-by-going-away.md)).

## How state stays consistent during suspension

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

## Server-side hook on disconnect

Regardless of whether transparent reconnection is enabled, you can register a server-side listener to react when a WebSocket is lost:

```java
config.setReconnectionListener((uiContext) -> {
    log.warn("Connection lost for user {}", uiContext.getAttribute("userId"));
    // execute() still works during suspension — safe to push state updates
});
```

## Client-side JS hooks

| Hook | When called | Default behaviour |
|------|-------------|-------------------|
| `document.onConnectionLost(cb)` | Immediately on disconnect | — |
| `window.showReconnectionInformation()` | To show custom reconnecting UI | Built-in banner |
| `window.onPonyReconnected()` | After successful reconnection | Page reload (if `ponyReconnectMode` is false) |

## Trading applications — important note

For electronic trading, transparent reconnection should be used with care. A reconnected session resumes the server-side widget state, but market data feeds may have missed updates during the gap. Always notify traders explicitly:

```javascript
window.onPonyReconnected = function() {
    showBanner("Reconnected — please verify your positions and prices");
};
```

The `ReconnectionListener` on the server side can also trigger risk checks or force a data refresh before the session resumes.
