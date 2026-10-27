# ADR-0003: Protocol 4, handlers removed from their object and DOM events by name

- Status: Accepted
- Date: 2026-10-21

## Context

The GWT terminal is gone (ADR-0002): one terminal now reads the protocol, so a change to it costs one implementation.

Two limits of the listener part of the protocol date from GWT:

- `TYPE_REMOVE_HANDLER` is a `NULL` model: the id of the object it applies to is not written. The terminal applies it to
  the last integer it read, the window id or a value of the previous block. Removing the last scroll, visibility,
  paste or resize handler (5 callers: `PScrollPanel`, `PWidget`, `PWindow`, `PValueBoxBase`, `PSplitLayoutPanel`) never
  stops the terminal: it keeps sending the events, and the server drops them.
- A widget listens to the 20 DOM events of `DomHandlerType`, each sent with fields chosen once for all. The terminal
  can listen to any event (`input`, `pointerenter`, `wheel`, `beforeinput`...), read any of its fields, and limit
  the rate in the browser. `PWebComponent` already listens to custom events by name, sending only their `detail`.

Constraints:

- Keys from the server are one byte: 250 of 256 are used, 6 are left.
- A terminal cannot read a key it does not know: its type, so its length, is unknown, and the rest of the frame is
  lost. A new key goes only to terminals that announce the version that has it (`w` parameter, `ProtocolVersion`).
- Keys from the terminal are strings; `Z`, `x`, `y` and `z` are free.

## Decision

Protocol version 4, `ProtocolVersion.LISTENERS`.

1. `TYPE_REMOVE_HANDLER` is followed by `WIDGET_ID`, the object's id, an existing key:
   `[TYPE_REMOVE_HANDLER][WIDGET_ID id][HANDLER_TYPE type]`. A server talking to a terminal below 4 writes the old
   block. The terminal reads `WIDGET_ID` when it is there, and the old way otherwise.
2. Two new keys from the server, both strings:
   - `LISTEN_EVENT`: a JSON object, `{"name":"input","fields":["target.value"],"debounce":300}`. Optional members:
     `fields` (paths read on the event, `detail` included; values that are not strings, numbers, booleans or null
     are sent as JSON text), `throttle` and `debounce` in milliseconds, `prevent`, `stop`, `capture`. Listening to a
     name again replaces its options. One JSON string costs one key and leaves room for options.
   - `UNLISTEN_EVENT`: the name.
3. Two new keys from the terminal: `EVENT` (`Z`), the name, and `EVENT_FIELDS` (`z`), an object of the fields read.
4. Java: `PWidget.addEventListener(String name, PEventOptions options, Consumer<PEvent> listener)`, returning a
   `HandlerRegistration`; the last listener of a name removed, the terminal stops listening. Below protocol 4, the
   listener is kept and a warning is logged: the terminal is not told.
5. `PWebComponent.onEvent` and the `DomHandlerType` handlers stay as they are.

## Options considered

- A new `UINT31` key for the removal (`TYPE_REMOVE_HANDLER_ID`): spends one of the 6 keys for what an existing one
  says. Rejected.
- `WC_LISTEN_EVENT` for every widget: no new key, but its events carry `detail` only, and its name says web
  component. Rejected.
- Options as separate keys (`EVENT_FIELDS`, `THROTTLE`...): one key each from the 6 left. Rejected for the JSON string.
- A binary frame from the terminal for the events: measured, not done (see Update).

## Consequences

- The terminal stops sending the events of a handler the server removed.
- A Java application listens to any DOM event of any widget, with its fields, and a rate limit in the browser: less
  traffic for scrolling, pointer moves and typing.
- 2 of the 6 keys left are spent.
- A page loaded before an upgrade keeps protocol 3 until it is reloaded: it gets neither change.

## Update (2026-10-21)

Key budget. Five keys no V3 server writes and no terminal reads went before V3 is released: `HEARTBEAT` (2.x had
already removed it, so V3's numbers differed from 2.x's from index 1 on), `DICTIONARY_STRING_ADD`, `WC_SHADOW_MODE`,
`WC_USE_SHARED_SKIN` and `WC_SET_PROPERTY_TYPED` (never written). `SUGGESTIONS`, written by 2.x only, stays: V3's keys
now extend the 227 of 2.x at the same numbers. 250 keys are used, 6 left. Once V3 is released no key is renumbered: a
page loaded before a deployment may still read them.

Binary messages from the terminal (`./gradlew :ponysdk:bench -Pbench.args="v4 inbound"`). Per message, on the path
of `WebSocket.onWebSocketText`: a click (87 B of JSON) costs 0.57 µs to parse and 0.13 µs to dispatch, a value change
(29 B) 0.19 and 0.03 µs, with 1.4 to 2.3 KB allocated, most of it the `JsonObject` tree. That tree is what every
widget's `onClientData(JsonObject)` takes: a binary format would have to build it again, or change that API on every
widget. A message costs less than one setter on the way out (1.4 µs). Not done.
