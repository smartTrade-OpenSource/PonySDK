# ADR-0016: The inspector and hot reload, for development

- Status: Accepted
- Date: 2026-10-29

## Context

Two questions a developer of a PonySDK screen cannot answer from the browser, and one wait:

- Which Java created this element? The terminal builds the DOM from binary blocks: the browser's tools show
  `pony-Button` and nothing of the class or the line behind it. `ensureDebugId` names an element only where the
  application wrote a call.
- What does this screen cost on the wire? The bytes go through one WebSocket; the browser's tools show frames, not
  what each element received. Performance work (each change measured) needs the bytes per class and per object.
- A change to a screen means a server restart and a new page: tens of seconds for one line.

`ApplicationConfiguration.debugMode` existed and only sent `ensureDebugId`'s ids.

## Decision

1. In debug mode, `PObject`'s constructor walks the stack to the first frame outside `com.ponysdk.core` and keeps
   "class file:line" in its rarely used fields; `PWidget` writes it at creation as `DEBUG_INFO`, a key appended to
   protocol 4 (3.0 is not released). The context is the one the constructor reads for the object's id: off, the
   cost is one boolean read per object, no memory.
2. The terminal writes it on the element as `data-pony`, and the object's id as `data-pony-id`.
3. The first `DEBUG_INFO` loads `chunks/inspector.js` (ADR-0004). The dispatcher counts for it through a `watcher`,
   null outside debug mode: one test per message, per block and per object of an update run; from the first
   `DEBUG_INFO` until the chunk is loaded, a recording, replayed. The bytes of a block go to its object, those of an
   update run to each object, from where its models start: the key that switches object is read again, in debug
   mode only, so that the update loop keeps no offset.
4. The inspector is a badge and a panel in a shadow root, styled by a constructed sheet: no inline style, under the
   page's policy (ADR-0015). Pick takes the click in the capture phase: it does not reach the application.
5. `ApplicationConfiguration.hotReload`: `ApplicationManager`, for the Java and the Spring managers alike, scans the
   directory the entry point was loaded from (the configured class, or the first entry point bean of Spring),
   twice a second, by the paths and times of its class files. Once they stayed still 300 ms, a class loader defines
   again the classes of the entry point's package found there, others from its parent (each session's Spring context
   loads its beans through it); each live context is destroyed as `SessionAdmin.destroy` does, so its client
   reloads its page at its history token.

## Options considered

- Debug information as an attribute set through `setAttribute`: the same bytes, but stored in the widget's
  attributes, replayed and listed with the application's own. A key of its own stays out of them.
- Counting in the inspector only from when it is open: the first message, which builds the page, is the largest
  and would be missing.
- Counting the bytes per block, an update run to its first object: a table updated as a run would put every
  cell's bytes on its first cell.
- An offset kept per model in the update loop: measured +5 % on 3,000 frames of a 1,000-object update run; reading
  the switching key again in debug mode: no difference measured (196.5 vs 195.8 ms).
- `WatchService` for hot reload: on macOS it polls every few seconds; a scan of a classes directory twice a second
  costs little and behaves the same everywhere.
- Rebuilding the screens in the same page, the socket kept: every object of the context to destroy on both sides, for
  a reload that a page reload does already, the history token kept.
- Reloading every class of the directory: the main class and what it holds (services, threads) would exist twice.

## Consequences

- `terminal.js`: 93,805 -> 95,122 B (28,847 -> 29,239 gzip) for the watcher, the recording and the attribute; the
  inspector is 7.6 KB (3.1 KB gzip), loaded in debug mode only.
- Debug mode names the application's classes in the page: a development setting. The showcase turns it on with
  `-Pdebug` only: its Costs page measures the normal mode.
- The browser coverage job of the CI runs the showcase a second time, in debug mode, for the inspector's tests.
- A hot reload loses the screens' state and keeps what the main class holds, of the old classes. A reloaded class
  and a non-reloaded class of the same package lose package-private access to each other.
