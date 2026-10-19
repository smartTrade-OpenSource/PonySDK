# ADR-0001: Server-side session administration

- Status: Accepted
- Date: 2026-10-19

## Context

PonySDK keeps the whole UI state on the server: one `UIContext` per browser tab. With transparent reconnection
(`ApplicationConfiguration.setReconnectionTimeoutMs` > 0), a lost WebSocket suspends the `UIContext` instead of
destroying it. Its writes go to a `RecordingEncoder`, which compacts them on the way and is capped by
`maxRecordingEntries` (10,000 by default). The writes are replayed when the client reconnects. The `UIContext` is
destroyed if the reconnection window expires or the cap is exceeded.

Nothing lets server code act on a session other than its own:

- an operator cannot cut a session, keep it away for a while, or end it;
- an application cannot exercise its own reconnection path in a test;
- the showcase cannot show a suspension filling up and its limits.

The existing methods do not fit. `UIContext.disconnect()` destroys the context, `UIContext.close()` tells the client
to reload, and `suspend()` and `resume()` are internal steps of the reconnection engine.

PonySDK is used to build high-performance, secured trading applications. Acting on another user's session is a
privileged operation: it must be decided on the server, checked on every call, traceable, and unreachable from any
client.

## Decision

### 1. One facade, bound to a caller

```java
SessionAdmin admin = SessionAdmin.of(UIContext.get());   // the caller: the UIContext of an admin screen

List<SessionInfo> sessions = admin.sessions();            // the sessions the caller may VIEW
Optional<SessionInfo> one = admin.session(id);
admin.dropConnection(id);                                 // DROP
admin.holdReconnection(id, true);                         // HOLD (false releases)
admin.destroy(id);                                        // DESTROY
```

`SessionAdmin` covers every live `UIContext` of the server (`SessionManager.get().getApplications()`), filtered by
the policy. It is a server-side Java API only: no client instruction, servlet or URL reaches it.

### 2. Actions

| Action | Effect on the target |
|---|---|
| `VIEW` | Listed by `sessions()` and readable by `session(id)`. |
| `DROP` | Its WebSocket is closed with an abnormal close code, as a network failure would. The client goes through its usual reconnection. Without transparent reconnection, the context is destroyed, as on any lost connection. |
| `HOLD` | While held, the server refuses its reconnection attempts: the new socket is closed at once and the client retries every 2 s. Releasing lets the next attempt resume the context. The reconnection window and the recording cap still apply, so a held session cannot outlive them. |
| `DESTROY` | The context is destroyed; the client is told and reloads. |

### 3. What a session exposes

`SessionInfo` is a read-only snapshot:

- the `UIContext` id;
- the state: `CONNECTED`, `SUSPENDED` or `DESTROYED`;
- the roundtrip latency;
- whether its reconnection is held;
- while suspended, a `Suspension`: when it started, the reconnection window, the entries written since then, the
  entries kept after compaction, and the cap.

It never exposes the HTTP session: the `Application` id is the HTTP session id (`WebSocketServlet.configureWithSession`),
and an admin screen that showed it would hand out a way to hijack the session. It does not expose user identity
either: that belongs to the application, which can map context ids to its own users.

### 4. Authorization

```java
@FunctionalInterface
public interface SessionAccessPolicy {
    boolean allows(UIContext caller, UIContext target, SessionAction action);
}
configuration.setSessionAccessPolicy(policy);
```

- The policy is read from the caller's `ApplicationConfiguration`.
- The default is `SessionAccessPolicy.SAME_HTTP_SESSION`: the caller may act only on the contexts of its own HTTP
  session, that is, its own tabs and frames. The showcase works with it. An operator console acting on every user
  needs an explicit policy, typically a role read from the caller's session.
- `sessions()` leaves out what the caller may not `VIEW`.
- An id the caller may not `VIEW` is handled like an unknown id (`NoSuchElementException`), so that ids cannot be
  probed.
- An action on a visible session that the policy refuses throws a `SecurityException`.

### 5. Audit

Every `DROP`, `HOLD` and `DESTROY`, allowed (INFO) or refused (WARN), is logged on the `com.ponysdk.audit.SessionAdmin`
logger with the caller id, the target id, the action and the outcome. So is a call naming a session the caller may not
`VIEW` (WARN), which is how probing shows. Listing, which an admin screen polls, is not logged. Applications route
that logger to their audit sink.

### 6. Threading

The calls are safe from any thread, including from inside the caller's `execute()`. The caller never takes the
target's lock while holding its own, because two admins acting on each other would deadlock:

- `DROP` closes the target's socket;
- `HOLD` sets a volatile flag that the reconnection path reads;
- `DESTROY` is handed to the target, which destroys itself under its own lock.

### 7. Prerequisite: suspension timers

The timer of a suspension destroys the context if it is still suspended when the timer fires
(`UIContext.suspend`). A context suspended, resumed and suspended again within the window is therefore destroyed
by the first timer, too early. `DROP` makes that sequence easy to produce. Each suspension gets a generation, and a
timer acts only on its own. This is fixed first, with its test.

## Consequences

- Operators, tests and the showcase can act on sessions through one API, with one place to authorize and one log
  to audit.
- Secure by default: nobody can act on another user's session unless the application grants it.
- A new public API to keep stable: `SessionAdmin`, `SessionInfo`, `Suspension`, `SessionAction`,
  `SessionAccessPolicy`.
- `HOLD` relies on the terminal retrying every 2 s. The refusal is server-side, so terminals released before this
  change behave the same.

## Alternatives considered

- **Methods on `UIContext`** (`dropConnection()`, `holdReconnection()`): any code holding a reference could act on
  any session, with no place to authorize or audit. Rejected.
- **Allow everything by default**: a single exposed admin screen would let any user cut another user's session.
  Rejected.
- **Callers outside the application** (JMX, REST): out of scope. A non-UI caller can be added to `SessionAdmin`
  later without changing the existing calls.
