# ADR-0017: A server that stops moves its pages to the others

- Status: Accepted
- Date: 2026-10-29

## Context

A screen lives in the JVM of one server. Behind a load balancer, that server stops for reasons that are not
failures: a rolling deployment, a scale-in, a spot interruption. The platform takes it out of the balancer, sends
SIGTERM, and kills it after a stop timeout (30 s by default for an ECS task, two minutes for a spot interruption).
Until now the server stopped at once: all its pages lost their connection together, then reloaded together onto the
other servers.

The cheap deployments run few servers on platforms that stop them often (spot capacity, scale-in): what a stop does
to the users is what they see of a deployment.

## Decision

1. `PonySDKServer.setDrainMillis(ms)`: with a time, the server registers a JVM shutdown hook that runs `Drain` then
   stops the server; 0, the default, keeps the immediate stop. `stop()` removes the hook.
2. `Drain.run(window)` destroys the live contexts one by one, as `SessionAdmin.destroy` does (ADR-0001): each client
   is told (`DESTROY_CONTEXT`) and reloads its page at its history token; the balancer, which no longer routes to a
   server it drains, sends it to another server. The moves are spread over four fifths of the window, a pace
   between two and none after the last; a context created during the drain is moved too; then the drain waits for
   the contexts to be gone, until the window ends.
3. What a move keeps is what a reload keeps: the history token, the browser's cookies and storage. No state is
   carried from server to server.

## Options considered

- Moving every page at once: the remaining servers take all the reloads in the same second, and the first page
  bootstraps compete with the rest.
- Carrying the screens' state to another server (serialised objects, a shared store): every widget and handler
  serialisable, and each update paid twice; a lambda cannot be carried. Rejected.
- Replaying the client's events on another server: needs a deterministic application; a market feed is not.
- The health check alone to take the server out: the platforms that stop a server (ECS, Kubernetes, spot) take it
  out of the balancer themselves, faster than a failing check. `/health` answers 503 while draining as well, for
  the balancers that only follow health checks.

## Consequences

- Measured with the Pony driver on a real server: a connected client receives `DESTROY_CONTEXT` and its socket
  closes before the server stops. On the showcase, `Drain: 3 contexts moved in 5003 ms`: on one server the reloaded
  pages came back to it until the window ended, as they would anywhere without a second server.
- The drain time is set below the balancer's deregistration delay and the platform's stop timeout
  ([deployment](../guide/deployment.md)).
- A crash has no drain: the pages reconnect, reach a server that does not know their context, and reload.
