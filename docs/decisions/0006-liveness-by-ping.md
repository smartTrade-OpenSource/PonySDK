# ADR-0006: The server judges a client alive by what it receives, and pings a silent one

- Status: Accepted
- Date: 2026-10-22

## Context

`CommunicationSanityChecker` suspected a connection when nothing was received or sent for a heartbeat period (5 s
by default), and dropped it after another period. The server's roundtrip probe went every second and counted as
sent: no connection was ever suspected. A client gone without a close frame (a network cut, a sleeping laptop) was
noticed only by TCP, or, for a session the application keeps pushing to, by the pusher's 60 s send timeout. What the
server sends proves nothing: it fills the socket's buffers whether the client reads them or not.

The terminal sent a heartbeat message every second (half the period of silence since `2889775d`), and one after a
batch longer than a second. These are JavaScript: a page busy longer than the period sends nothing. Counting only what
is received, with these heartbeats, would drop a busy page that is alive.

RFC 6455 (5.5.2, 5.5.3): an endpoint answers a Ping frame with a Pong. Browsers answer in their network layer,
outside the page's JavaScript, so while the page is busy too. Jetty sends pings (`Session.sendPing`) and reports
pongs (`Session.Listener.onWebSocketPong`).

## Decision

1. A client is alive when something is received from it: a message or a pong. The server's own sends no longer
   count.
2. At each check (every second), a client silent for half a heartbeat period gets a ping. Silent for a period, its
   connection is suspect; still silent a period later, it is dead: the server drops the socket without a close
   handshake and treats it as an abnormal close (1006). The context is suspended with transparent reconnection,
   destroyed without, as for any connection lost. Jetty may report that close again: a socket's close is handled
   once.
3. The terminal sends no heartbeat. It keeps its watchdog on the server's messages (no message for two periods closes
   the socket and reconnects); the server still accepts the heartbeats of older pages.

## Options considered

- Count only what is received, keep the terminal's heartbeats: a page busy for two periods would be dropped, then
  resume through a reconnection. Rejected.
- Keep counting what is sent: a dead client is never noticed by the check. Rejected.
- Heartbeats from the terminal in a Web Worker: they would go while the page is busy, but the worker cannot use the
  page's socket, and the browser already answers pings.

## Consequences

- A client gone without a close frame is noticed within two periods, 10 s by default, pushing or not.
- A busy page keeps its connection: the E2E test blocks a page for 12 s on Chromium, Firefox and WebKit.
- An idle page sends only its answers to the probe (every 5 s); the server pings it after 2.5 s of silence, a
  control frame of 2 bytes each way, answered without JavaScript and without a JSON parse on the server.
- A proxy that terminates WebSocket and answers pings itself hides a dead browser behind it until the proxy notices;
  a proxy that drops pings leaves the probe's answers, every 5 s, as signs of life.
- An older terminal still sends heartbeats; the server reads them as before.
