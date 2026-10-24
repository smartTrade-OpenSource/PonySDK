# ADR-0005: The terminal tells the server its page is left, and how long a probe waited in it

- Status: Accepted; decision 1 amended by [ADR-0007](0007-page-left-by-going-away.md) (a 1001 from the browser)
- Date: 2026-10-22

## Context

A page left: on `pagehide` the terminal closes its socket without a code, and the server reads 1005. With
transparent reconnection, the server suspends the context for the reconnection window (10 s in the showcase): it keeps
the widgets and records what the application writes, for a page that cannot come back, since a page loaded again
starts a new context. In one E2E run of the showcase, 184 contexts ended this way, each logging a WARN.

- A script may close a WebSocket with 1000 or with 3000 to 4999 only. The terminal also closes with 1000 before it
  reconnects (its watchdog), and Jetty closes with 1001 on an idle timeout, a network stall the client may recover
  from. The server already closes with 4000 (dropped by an administrator) and 4001 (reconnection held).
- A page kept in the back-forward cache (`pagehide` with `persisted`) may come back and reconnect.

Latency: `UIContext.getTerminalLatency()` is fed by nothing, in 2.10 as here, and `getNetworkLatency()` receives the
roundtrip. The roundtrip of a probe includes the time the probe waits in the terminal: behind the blocks before it
in its message, behind the page's other tasks when the page is busy, behind a module that loads (ADR-0004). Only the
terminal can tell that time from the network's.

## Decision

1. Close code 4002, page left. The terminal closes with it on `pagehide` when the page is not persisted; the server
   then destroys the context at once instead of suspending it. A persisted page closes as before.
2. The terminal's answer to the probe gains `x`, the milliseconds from the arrival of the message that carried the
   probe (the message event's `timeStamp`) to the answer: `{"h":<nanos>,"x":<ms>}`. The bootstrap page records the
   arrival time of the messages it receives before the terminal runs. The server feeds the terminal latency with `x`
   and the network latency with the roundtrip minus `x`. An answer without `x` (a page loaded before an upgrade, the
   Pony driver) counts 0: the network latency is the roundtrip, as before.
3. The parser that reads the answer without a JSON tree reads the optional `x`.

## Options considered

- Destroy on 1001 or 1005: 1001 is also Jetty's idle timeout, and 1005 is what an older terminal sends on any
  close. Rejected for a code of our own.
- Remove `getTerminalLatency` and `getNetworkLatency`: they are the way to tell a busy page from a slow network, at
  the cost of one number every 5 s. Rejected.
- Measure only the probe's own block in the terminal: misses the wait behind other tasks, the part that grows on a
  busy page. Rejected.

## Consequences

- A tab closed or reloaded frees its context at once, its recording buffer included; the WARN on the reconnection
  timeout is left to connections that were lost.
- `getTerminalLatency()` measures something, and `getNetworkLatency()` no longer counts the terminal's time.
- A page loaded before an upgrade keeps the old behaviour until it is reloaded: suspended when left, answers
  without `x`.
- While the dispatcher waits for a module, the messages that come meanwhile are counted from the arrival of the one
  that made it wait: their terminal time is overestimated by at most the module's loading, once per page.
