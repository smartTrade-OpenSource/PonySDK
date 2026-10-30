# ADR-0027: Client commands, run by the terminal on a DOM event without a round trip

- Status: Accepted
- Date: 2026-10-29

## Context

Every reaction of a PonySDK screen goes through the server. `PEventOptions` (ADR-0003) limits the rate of an event
(throttle, debounce, lock), `PScript.call` calls a page function by name (ADR-0015), but showing a panel on a click
still waits for the server: the click is sent, the server runs the handler, the update comes back. On the showcase's
localhost that is 4 to 21 ms from the click to the visible change; behind a network of 100 ms it is 106 to 124 ms
(measured below). For a menu, a tab, a panel, a focus, the server has nothing to decide.

Constraints: no protocol key and no widget type (4 keys left); the strict policy of ADR-0015 (no `eval`, no code
string); the event options already travel as the JSON string of `LISTEN_EVENT` (ADR-0003), parsed once by the
terminal when it starts listening.

## Decision

1. **`PCommands`, an immutable list of named operations on widgets, given to a listener as an option.**
   `PEventOptions.commands(PCommands)`. Ten operations: `show`, `hide`, `toggle`, `addClass`, `removeClass`,
   `toggleClass`, `setAttribute`, `removeAttribute`, `focus`, `dispatch` (a bubbling `CustomEvent` of a name). A target is
   an `IsPWidget`: a `PWidget`, or a composite that wraps one.
   ```java
   button.addEventListener("click",
       PEventOptions.NONE.commands(PCommands.NONE.toggle(panel).toggleClass(menu, "open").focus(input)),
       e -> save());                       // the event is sent too: the server's listener runs
   button.addEventListener("mouseenter",
       PEventOptions.NONE.commands(PCommands.NONE.addClass(hint, "on")));  // no listener: nothing is sent
   ```
2. **Sent or not follows the listeners.** `addEventListener(name, options, listener)` is what ADR-0003 made it: the
   event reaches the server. `addEventListener(name, options)` (no listener) requires commands and tells the terminal
   `"send":false`: it runs the commands and sends nothing. A name has either listeners or commands alone (the second
   registration throws `IllegalStateException`): the options of a name are one set, the last given, and a registration
   removed could not hand the previous commands back. To have both, give the commands to the listener. A client-only
   registration given again for a name replaces the previous one; the handle of the replaced one stops nothing.
3. **On the wire, in the JSON that exists.** `"commands":[["toggle",12],["toggleClass",13,"open"],["focus",14]]`: each
   command is `[operation, target id, arguments...]`, in the order declared; `"send":false` when no listener wants the
   event. The target is the PObject's id, not a selector: it is what the server already names objects by, it does not
   depend on the DOM, and `PCommands` holds integers, so it keeps no widget alive.
4. **The terminal compiles the commands when it starts listening** (`runtime/commands.ts`). An operation is a function
   of a table; a name the table does not have is reported once (`context.warn`) and left out, the others run. A target
   is looked up when the event happens, through `Context.peek` (the unreported lookup the inspector had): a target the
   server collected, or not created yet, is skipped without a word. A command that throws (a name the DOM refuses) is
   reported through `context.error`, the next ones run, the event is sent.
5. **Order.** On a DOM event: `prevent` and `stop`, then the commands in order, then the send (rate-limited and locked
   as before). The commands are not throttled or debounced: only the send is, so the screen answers at once while the
   server hears the last event. `lock` gates the send alone. A command that dispatches the event of its own listener
   does not run that listener's commands again (the nested event is sent, the commands once); a chain through
   several widgets is the application's own.
6. **Visibility is the one `setVisible` renders.** `show` and `hide` call `Widget.setVisible`: `display: none` and
   `aria-hidden` on the element, what `WIDGET_VISIBLE` does. `toggle` reads `style.display`. A widget hidden by a style
   name (CSS) is not seen by `toggle`.
7. **Safe by construction.** Only these operations exist: no code string, no selector, nothing `eval`ed (the page needs
   no `'unsafe-eval'`). `setAttribute` and `removeAttribute` refuse names starting with `on`, in `PCommands` and again in
   the terminal; values are text. Class names and event names must be one token (no white space), attribute names a
   plain name: the DOM would throw otherwise. `removeClass` keeps the widget's type class, as `removeStyleNames` does.
8. **The server does not follow.** See Consequences: this is the cost of not asking it.

## Options considered

- **A builder that registers itself, `PCommands.on(button, "click").toggle(panel)`.** Reads well, but a second way to
  listen next to `addEventListener` (ADR-0011 closed such doubles), with its own registration, replacement and removal
  rules for the same name. The option and one overload reuse what is there.
- **Commands as strings, `JS.toggle("#panel")` as in Phoenix LiveView.** Targets by selector break when the DOM changes
  and cannot be checked on the server; a string grammar to parse. Typed methods give the same reach with a compile-time
  check and ids.
- **Keeping the state in step: the terminal tells the server what it did** (a visibility event, the classes). One more
  message per command, and the server would take events it did not ask for; it also gives back the latency this is
  here to remove. Rejected: the application that needs the server to know says so with a listener (below).
- **Updating the server's `visible` and style names from a Java-side command declaration.** The server cannot
  tell, at the click, whether the browser toggled a panel it had hidden by itself: it would only be right until the
  first other change. Rejected.
- **A new protocol key for the commands.** Not available, not needed: the JSON has room.
- **Running the listener's Java in the browser.** Not possible, and not the point: a named operation is the
  most a policy without `eval` allows.

## Consequences

- **What the server knows after a command: nothing.** `isVisible()` stays what the server set, its style names and
  attributes too. The traps, all of the same kind, "the server thinks it already is":
  - `panel.setVisible(true)` when the server's `visible` is already true sends nothing: a panel the browser hid stays
    hidden. The same for `addStyleName` of a name the server already has, `removeStyleName` of one it does not have,
    `setAttribute` with the value it already holds, `removeAttribute` of one it does not hold: a class or an
    attribute a command changed stays as the command left it.
  - `setStyleName` with another list replaces the whole class attribute: the classes a command added go (the type
    class stays).
  - The server also drops the events of a widget it believes hidden (`onClientData`): hidden by the server, not by a
    command.
  - A reload, or a context moved to another server (ADR-0017, ADR-0018), shows what the server holds: the commands'
    effects are not part of it.
- **Keeping the server in step is explicit**: give the commands to a listener, and mirror what they did with
  absolute operations. `show`/`hide`, not `toggle`, so that two answers arriving late leave the state they led to:
  ```java
  open.addEventListener("click", PEventOptions.NONE.commands(PCommands.NONE.show(panel)), e -> panel.setVisible(true));
  ```
  The server's answer repeats the browser's state. With `toggle` on both sides and a slow network, quick clicks flicker:
  each answer arrives after the browser has moved on.
- **A command is not a handler.** It cannot read the event, branch, or wait for data. A reaction that depends on any
  of that stays on the server, which is the point of the framework.
- **Cost, measured.**
  - Listener JSON, once per widget and name: the click's `{"name":"click"}` is 16 B; one `toggle` makes it 43 B; the
    three commands of the showcase (`toggle`, `toggleClass`, `focus`) 86 B; `"send":false` adds 13 B. The event's own
    message does not change (28 B up for a click without fields).
  - Per click, on the showcase, through a recording WebSocket route: the server's toggle is 89 B up (the DOM handler's
    fields) and 11 B down; the client command with a server listener 28 B up; the commands alone, 0 B and no message.
  - Time from the click to the visible change, Chromium on localhost, the answer of the server delayed on the
    socket (`sample/tests/client-commands.spec.js`):

    | Delay of the answer | Round trip | Client command |
    |---|---|---|
    | none | 4.2 ms (21.2 ms on an earlier run; Firefox, WebKit: 4 to 5 ms) | 1.1 ms |
    | 100 ms | 105.7 ms (123.9 ms) | 0.9 ms |
    | 300 ms | 305.9 ms (320.4 ms) | 0.8 ms |

    The client command does not depend on the network: it runs in the click's task, before the event is sent. The
    test also holds every message of the server and shows the panel changing with none received.
  - `terminal.js`: 96,680 B to 97,742 B (+1,062 B), gzip 29,871 B to 30,278 B (+407 B). `commands.ts` and the new
    branch of `named-events.ts` are in the core: every page pays, as every page listens to named events.
  - Server, per event: no change. At registration, `PEventOptions.toJson` builds the JSON: the three commands add
    0.4 µs. Writing it showed that each `Json.createObjectBuilder()` looked the JSON provider up again: 21 µs for a
    plain listener's JSON. It now uses one `JsonBuilderFactory`: 0.12 µs for the plain one, 0.49 µs with the
    three commands, for every `addEventListener`.
- **Tests.** Java: serialisation, validation, registration rules (`PCommandsTest`). Node: every operation on a fake DOM
  with the real `Widget`, the order, the skipped target, the refused names, the guard on `dispatch` (`commands.test.ts`,
  100 % of lines and branches). Playwright, Chromium, Firefox, WebKit: the change with every server message held, the
  commands before the send, commands alone sending nothing, a collected target skipped, the latencies above.
- The showcase's Client commands page puts a panel shown by the server next to one shown by the terminal, with a
  checkbox that delays the server's answers by 800 ms, and a button that asks the server what it sees of the panel.
