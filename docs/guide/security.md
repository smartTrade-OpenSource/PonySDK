# Security

PonySDK is **secure by default**: the risky options are off and the protective ones are on, so a stock deployment is safe with no extra configuration. Security spans three layers — the WebSocket transport, request authorization, and static-resource serving.

## Transport hardening (WebSocket)

A WebSocket upgrade is **not** subject to the browser same-origin policy, yet the browser still attaches the session cookie. Left unchecked, a malicious page could open a WebSocket onto a logged-in user's session (Cross-Site WebSocket Hijacking, CSWSH). The transport is validated and bounded on upgrade:

| Concern | Mechanism | Config (`ApplicationConfiguration`) | Default |
|---------|-----------|--------------------------------------|---------|
| **CSWSH** | `Origin` checked on upgrade; cross-origin rejected `403`. Same-origin is auto-accepted (honours `X-Forwarded-Host` behind a proxy); requests with no `Origin` (non-browser clients) are allowed | `wsOriginCheckEnabled`, `wsAllowedOrigins` | check **on** |
| **Inbound memory** | Max client→server text message size | `wsMaxInboundMessageSize` | 1 MB |
| **Slow consumer** | One send in flight per connection; a client that doesn't drain within the timeout is disconnected — memory stays bounded, no unbounded buffering | `wsSendTimeoutMs` | 60 s |
| **Idle connections** | Close after inactivity | `wsIdleTimeoutMs` | 1 000 s |
| **Frame compression** | permessage-deflate; can be disabled for low-CPU/latency deployments (and for non-browser clients that cannot decode compressed frames) | `wsPermessageDeflateEnabled` | on |

`WebSocketServlet.isOriginAllowed(origin, host, allowedOrigins)` is a pure, unit-tested function, so the matching rules are pinned by tests.

## Request authorization — UIContext session scoping (IDOR)

`AjaxServlet` and `StreamServiceServlet` address a UIContext by integer ID. A **global** lookup by ID would let any authenticated user act on *another* user's UIContext just by guessing the ID (an Insecure Direct Object Reference). Both servlets resolve the UIContext **scoped to the caller's HTTP session**:

```java
// Session-scoped: only returns a UIContext that belongs to THIS http session.
UIContext ctx = SessionManager.get().getUIContext(applicationId, uiContextId);
```

The legacy global `getUIContext(id)` is deprecated for removal, and cross-session access is denied (regression-tested in `AjaxServletTest` / `StreamServiceServletTest`).

## Static-resource serving — path traversal

`BootstrapServlet` serves client resources by path. Before loading anything, `isSafeResourcePath(path)` rejects traversal / absolute / backslash paths and returns `404`, so a crafted URL cannot escape the resource root to read arbitrary files.

From the classpath (the application's jars and PonySDK's), it serves web files only: `html`, `js`, `mjs`, `css`,
`map`, images and fonts. A class, a `.properties`, `.xml` or `.yml` configuration, a key store (`.jks`, `.p12`) answers
`404`, as anything not in the list. An application that serves another type from its jars adds it, knowing that every
file of that type on the classpath becomes downloadable:

```java
configuration.addServedResourceExtensions("json"); // translations kept in a jar
```

The files of the servlet context (a web application's directory) are all served: they are public by construction.

## Log-injection hardening

Untrusted client values (the reconnection parameter, malformed inbound frames) are sanitised before being logged, so a client cannot forge log lines by injecting newlines or control characters.

## Secure-by-default summary

- `Origin` check **on** — cross-origin WebSocket upgrades are rejected.
- Inbound size **bounded** (1 MB) and slow consumers **disconnected** — one connection cannot exhaust memory.
- UIContext access **scoped to the HTTP session** — no IDOR.
- Static resources **path-guarded** — no traversal — and **web files only** from the classpath — no class, configuration or key store.
- Transparent reconnection **off** by default — no silent resume of stale state (important for trading apps, where stale market data must never be replayed unnoticed).

> Each item above is backed by dedicated tests (the Origin matrix, cross-session denial, path-safety, and the configuration defaults), so the secure defaults cannot silently regress.
