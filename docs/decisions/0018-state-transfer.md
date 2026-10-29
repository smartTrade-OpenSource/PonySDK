# ADR-0018: A moved page carries the state its application declares

- Status: Accepted
- Date: 2026-10-29

## Context

A drain (ADR-0017) moves each page of a stopping server to another server: the page reloads at its history token,
with the browser's cookies and storage. What the screen held in the JVM is lost: a draft typed in an order ticket, the
rows a user selected, a scroll position, a filter not put in the address. For a rolling deployment every user of the
server loses it.

Other frameworks keep the whole session: Vaadin replicates the serialised session to Hazelcast or Redis (Kubernetes
Kit, commercial), which needs every object of the screen serialisable and a shared store written on each change.
PonySDK's screens hold lambdas, threads' tasks and subscriptions: they cannot be serialised (ADR-0017, options).

## Decision

1. Off by default. `ApplicationConfiguration.setStateTransferKey(byte[])` turns it on: a 256-bit key, the same on
   every server of the application, read from the secret store or the environment, never written in a repository.
2. The application declares what a move carries: `uiContext.onMove(Consumer<MovedState>)` fills a `MovedState`, a
   map of names to values of the protocol's own types (boolean, int, long, double, string, bytes), when the context
   is moved. The next server's context finds it in `uiContext.getMovedState()` before its entry point starts.
3. The page carries it, not a store: the state goes to the client in the block that destroys the context
   (`CONTEXT_STATE`, a server-to-client key appended to the protocol, before `DESTROY_CONTEXT`), the terminal keeps it
   in the tab's `sessionStorage`, and the reloaded page offers it in its WebSocket handshake (parameter `ms`). No
   server talks to another; nothing is written while the screen lives.
4. Encoded in PonySDK's format, sealed: the values in the protocol's binary value encoding (type tag of
   `ValueTypeModel`, then the value), sealed with AES-256-GCM under the key, with the application's id as
   additional data and the time of the move inside. A state altered, from another application or older than two
   minutes is refused (logged at WARN, the page starts without it). The client cannot read it.
5. A move is a drain's or `SessionAdmin.move(id)`'s; `SessionAdmin.destroy(id)` ends a session and carries nothing.
   A state larger than 4 KB once sealed is not carried (WARN): it travels in a URL.

## Options considered

- Replicating the session to a shared store (Vaadin's way): every object serialisable, a write on each change, a
  store to run. Rejected for the reasons of ADR-0017.
- The state in a cookie: one per origin, shared by the tabs; two tabs drained together would overwrite each other's.
- The state sent by the client as its first message: the entry point starts when the socket opens, before any
  message; waiting for one would delay every page, moved or not.
- Signing without encrypting (HMAC): the client would read what the application put in, which may be internal.
  AES-GCM gives both with one key.

## Consequences

- A rolling deployment keeps what the application declares, at no cost while the screen lives: the state is built
  only when a page moves.
- One server-to-client key spent (3 left) and one handshake parameter.
- The application chooses what survives: what it does not declare is rebuilt as after a reload.
