# ADR-0007: A close with 1001 from the browser means its page is gone

- Status: Accepted
- Date: 2026-10-24
- Amends: [ADR-0005](0005-page-left-and-terminal-time.md), decision 1

## Context

ADR-0005 has the terminal close its socket with 4002 on `pagehide`, and the server destroy the context at once on
4002 instead of suspending it for the reconnection window. Measured on 2026-10-24 (Playwright 1.64.1, the server's
log):

| Browser | Page navigated away | Tab closed | Browser closed |
|---|---|---|---|
| Chrome 148 | 4002 | 4002 | 1001 |
| WebKit 27.2 | 4002 | 4002 | 4002 |
| Firefox 157 | 1001 | 4002 | 4002 |

Firefox closes the sockets of a page it navigates away from before `pagehide` runs: the server receives 1001, keeps
the context for the window and records what the application writes for a page that cannot come back. The E2E test of
ADR-0005 did not see it: it read the close through Playwright's `routeWebSocket`, which replaces the page's socket.

1001 is what a browser sends for the sockets of a document it unloads (HTML, "make disappear"); RFC 6455 (7.4.1)
gives it to "a browser having navigated away from a page". ADR-0005 rejected 1001 because Jetty also closes with
1001 on its idle timeout. That close is the server's: Jetty reports the timeout through `onWebSocketError`, then
closes (`WebSocketCloseTest`: the error and the close come 2 ms apart, on the same thread). Jetty also closes with
1001 when the server stops.

## Decision

1. A 1001 received with no error reported on the socket before it is the browser's: its page is gone, and the server
   destroys the context at once, as for 4002.
2. A 1001 that follows an error is the server's own close (idle timeout): the context is suspended with transparent
   reconnection, as before.
3. The terminal keeps closing with 4002 on `pagehide`: Chromium and WebKit send it before the browser closes the
   socket, and it names the cause in the log.

## Options considered

- Close with 4002 on an event that comes before Firefox closes the socket: `beforeunload` may be cancelled by another
  handler, and the page then stays without its context. Rejected.
- Tell the server's 1001 by its reason (`Connection Idle Timeout`): a text Jetty may change. Rejected.
- Register an idle-timeout listener on each session to flag the server's close: an object per session for what the
  error already says. Rejected.

## Consequences

- A page left in Firefox frees its context at once, as in the other browsers; so does a browser closed while
  Chrome's pages are open.
- The server's stop closes with 1001 and no error: its contexts are destroyed rather than suspended, which a stopping
  server does in any case.
- A page kept in the back-forward cache and closed by its browser with 1001 before `pagehide` loses its context, and
  reloads when it comes back (the reconnection finds no context). Not measured: under Playwright, no browser kept the
  showcase in the cache.
- One boolean per socket, written once on an error; nothing on the message path.
