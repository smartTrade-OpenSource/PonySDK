# ADR-0004: The terminal's rare widgets loaded on demand

- Status: Accepted
- Date: 2026-10-21

## Context

`terminal.js` is one script of 131.5 KB, 38.9 KB gzip (ADR-0002, after the names tables went). The page waits for all
of it before the first widget shows.

Seven modules hold widgets many pages do not show at first: the date picker and date box with their date patterns,
the rich text area and its toolbar, the layout panels, the stack layout panel, the tree, the menu bar and the suggest
box: 44.6 KB, 12.4 KB gzip, 32 %.

The terminal creates an object when the server's block says so, and the next blocks may use it at once: a module
loaded on demand must be there before the block that creates its widget runs.

A first attempt used esbuild's code splitting. The code the seven modules share with the core went to shared chunks,
which `terminal.js` imports statically: 29.2 KB gzip before the first widget instead of 38.9, but in four files, the
last three found only once `terminal.js` is parsed: one round trip more, which a slow network pays more than the bytes
saved.

## Decision

1. `terminal.js` is the core, an ES module. It exports what the seven modules use of it (`src/core-api.ts`: the
   model's keys, `Widget`, `FocusWidget`, `BaseObject`, `about`, `FLAG`, `POPUP_CLASS`) and imports nothing
   statically.
2. Each module is built apart into `chunks/<name>.js`, importing the core from `../terminal.js`: one module instance
   of the core, so `instanceof` holds. A module no other uses goes with its chunk (the date patterns with the date
   picker). The build (`terminal/build.mjs`) fails when a chunk imports a name the core does not export.
3. `BootstrapServlet` loads `terminal.js` with `type="module"`: deferred like before, in order with `legacy.js`.
4. The factory knows each type's module. For a type whose module is not loaded, the dispatcher stops at the block
   that creates it, before any of its effects, loads the module, then runs the block and the ones that came
   meanwhile, in order. Messages from the server queue in the reader as they do for a block split across two
   messages; the windows and frames the terminal feeds wait with it.
5. One second after the first message, when the browser is idle (4 s more at most), the terminal loads the modules
   it has not loaded: they do not compete with the page, and the first rare widget of a page does not wait for the
   network.
6. The chunks are served as `terminal.js` is, revalidated: they change with it.

## Options considered

- One script, as before: no pause in the dispatcher, 11.9 KB gzip more before the first widget.
- esbuild's code splitting: see the context. Preloading its shared chunks (`modulepreload`) would remove the round
  trip, at the cost of a manifest of hashed names for `BootstrapServlet` to read. Rejected for the simpler build.
- Every widget in a chunk: more requests for little; the core widgets (panels, labels, buttons, text boxes, tables,
  popups) show on nearly every page.
- No loading when idle: the first rare widget of a page would wait for a request.

## Consequences

- Before the first widget: 27.0 KB gzip in one file instead of 38.9 KB; the same bytes come once the page is idle.
- The dispatcher can pause: an instruction runs later than it arrived, never out of order.
- A page loaded before a deployment that loads a chunk after it gets the new chunk with the old core: a name the new
  chunk imports and the old core lacks fails to load, and its widgets are not created. Loading when idle makes the
  window short: seconds after the page loads.
- A content security policy must allow module scripts from the terminal's origin.
