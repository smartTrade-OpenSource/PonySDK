# ADR-0029: FINOS Perspective in a PonySDK screen, a table the server feeds with typed arrays

- Status: Accepted
- Date: 2026-10-29

## Context

A trading screen shows a blotter of thousands of rows that change many times a second, and its user groups, pivots,
sorts, filters and charts it. PonySDK's `DataGrid2` pages rows from the server, so each of those operations is a round
trip and a query on the server. [Perspective](https://perspective-dev.github.io/) (FINOS, Apache-2.0) does them in the
browser: a streaming query engine in WebAssembly, a `<perspective-viewer>` custom element, and plugins for a grid and
WebGL charts.

Its packages are named `@perspective-dev/*` since 5.0 (`@finos/perspective-*` is 3.x and 4.x). The latest 5.x, 5.6.1,
was published on 2026-10-23; the latest published more than two weeks before this work is **5.6.0** (2026-10-14).
`client`, `server` (the engine's WebAssembly), `viewer`, `viewer-datagrid` and `viewer-charts` are Apache-2.0, as are
their bundled dependencies (`regular-table`, `regular-layout`).

Constraints of the task: no CDN at run time; the policy of ADR-0015 (no eval); no protocol key and no widget type (4 keys
are left); the library loaded by the page that has a viewer only; the feed's thread never blocked (ADR-0013).

What Perspective asks of a policy was measured, in Chrome 156, with a server that sends a policy and a page that
loads a viewer over a table of two rows:

| Policy and loading | Result |
|---|---|
| `script-src 'self'` | `WebAssembly.compile()` refused, for the viewer's module and for the engine's: nothing renders |
| `+ 'wasm-unsafe-eval'`, the packages' CDN bundles | the viewer imports its own glue from a `blob:` URL: refused |
| `+ 'wasm-unsafe-eval'`, the ESM viewer given its glue module, the engine's worker started from its file, the grid | **works, no violation** |
| the same, a chart | the chart's worker is started from a `blob:` URL: refused |
| the same, `worker-src 'self' blob:` | the worker starts and draws nothing: it imports the viewer's glue from a `blob:` URL, which `script-src` governs inside the worker |
| the same, `script-src` with `blob:` | **the charts draw** |

The library contains `new Function` (wasm-bindgen's `Function::new_no_args`, and a single-threaded fall-back of the
engine). Neither runs under the policy: the page raises no violation and no error.

## Decision

1. **A `PAddOn` in the sample, not a widget type, not `PWebComponent`, not ponysdk.** `PPerspective`
   (`sample/.../perspective/PPerspective.java`) is a `PAddOnComposite<PElement>`; `PerspectiveConfig` models what a
   viewer shows. It uses public API only (`PAddOnComposite`, `callTerminalMethod`, `setTerminalHandler`, `Conflator`),
   so it lives with the application that wants it and the guide shows how to copy it. `PWebComponent` cannot do the job:
   the viewer's table is a JavaScript object in a worker that the browser creates and that must be fed from typed
   arrays, so glue is needed whatever the Java class; and `@perspective-dev/viewer` ships no Custom Elements Manifest for
   the generator of ADR-0010.
2. **The library is the application's own files.** `sample/perspective/package.json` pins the five packages
   exactly (`5.6.0`), with a lock file. Gradle's `copyPerspective` (`sample/build.gradle`), after an `npm ci` through the
   Node plugin ponysdk's terminal already uses, copies what a browser loads into `script/perspective/<package>/dist/...`,
   the layout of the packages, with their licences: the two ES modules (`client`, `viewer`), the viewer's wasm-bindgen
   glue and snippets, the grid and the charts (their bundled builds), the two WebAssembly files, the engine's worker
   and the two themes: 5.1 MB served out of 30 MB of `node_modules` (23 MB of them the five packages), none of it from a CDN. `wasm` is added to the served
   extensions (`addServedResourceExtensions("wasm")`; Jetty answers `application/wasm`).
3. **A script of 11.9 KB (3.9 KB gzip) on every page, the library on one.** `script/perspective-addon.js` is listed with
   the application's scripts and registers the addon. The library is loaded by the first `PPerspective` of a page with
   dynamic `import()`: the viewer, its glue, the client, `init_server(fetch(wasm))`, `init_client(fetch(wasm), glue)`, the
   grid and the themes, then the engine's worker, started from its file. The glue module is passed to `init_client`
   (its second parameter, typed in the package) so that it does not import it from a `blob:` URL; the worker is a file
   for the same reason. The charts (285 KB) load only for `charts(true)`. Pages without a viewer request no file of
   `script/perspective/` (`perspective.spec.js` checks it).
4. **The policy: one new option, `setWebAssemblyEnabled(true)`.** It adds `'wasm-unsafe-eval'` to `script-src`, which
   compiles a module and does not run a string (ADR-0015 stays true: `'unsafe-eval'` is never written). Off by default,
   with a Spring Boot property (`ponysdk.content-security-policy.web-assembly`). A grid needs nothing else. The charts also
   need `blob:` in `script-src`, which `addScriptSources("blob:")` already gives: a `worker-src` option, written first,
   does not help (table above) and was dropped. The showcase sets both; an application that only shows the grid sets the
   first.
5. **One call per flush, a typed array, rows with a mask.** Perspective takes Arrow, column-oriented objects, row
   objects or CSV. The protocol carries tagged values in arrays and no bytes, so Arrow would be Base64 text and a Java
   Arrow dependency. A column-oriented update cannot say "unchanged": in Perspective 5.6.0 an `undefined` or a `null` in
   a column sets the cell to null (checked in `Table.update`), and a feed's updates change a few columns of each row. Row
   objects can leave a column out. So the wire is a row-oriented array that the terminal turns into row objects:
   ```
   flags (bit 0: replace the table's rows), the batch's time (only if a column is a datetime),
   the number of keys to remove and the keys, the number of rows, then per row:
   its key, the mask of the columns it carries (bit i: the i-th column that is not the key), their values
   ```
   A value is the smallest of byte, short, int and long the array encoder writes. A float is a whole number of its
   column's decimals (`Column.floating("bid", 4)`: 1.08512 is 10,851), a datetime is the time since the batch's, a date a
   day number, a string a dictionary entry after its first use, a null a tag. Nothing is kept between batches: a lost or
   replayed frame cannot desynchronise the two sides. A call carries at most 16,384 values (the terminal applies them as
   arguments of a function, which engines limit): a replace of 5,000 rows is several calls of one flush.
6. **Conflation per row and per flush.** `update(Map)` validates on the caller's thread (a column that is not in the
   table, a value of another type, a float out of range: `IllegalArgumentException`, nothing kept), merges the columns
   into the row pending for its key, and offers one constant key to a `Conflator` that sets the flush and the interval
   (100 ms), and handles the suspended context and the context's destroy as for `PChart` (ADR-0026). `Conflator` replaces
   a key's value and these updates carry different columns, so the merge is the component's; the conflator gives the
   flush. `remove(keys)`, `replace(rows)` and `clear()` take part in the same pending state, from any thread, in order.
7. **The configuration both ways, as JSON; clicks and selections as rows.** `configure(PerspectiveConfig)` (plugin,
   `group_by`, `split_by`, `columns`, `sort`, `filter`, `aggregates`, the plugin's settings) goes as a JSON text and
   `restore`s the viewer. A change of the user's (`perspective-config-update`) is saved after 100 ms of calm, compared to the
   last configuration the terminal applied or sent, and reported only if it differs, so what Java set does not come back
   and a column resize alone is not a change of the modeled fields. `addClickListener` gets the row the user clicked,
   `addSelectListener` the row selected (the grid in its `SELECT_ROW` mode reports a window of the view; the terminal reads
   its first row) or null. The showcase broadcasts the clicked instrument on the FDC3 channel (ADR-0014).
8. **Accessibility is the library's, and one thing of it is not.** The Pro Light theme's blue and red numbers (3.6 and 3.4
   to 1 on white) and its inactive text (2.3 to 1) fail WCAG AA; `showcase.css` overrides three of its custom properties,
   on the viewer and on the elements inside it that carry the theme. One icon button of the viewer's title bar has no name:
   its own markup, in a shadow root, allowed in `a11y-audit.spec.js` with that reason.
9. **Showcase.** A Perspective page in "Under the hood": a blotter of 5,000 rows fed at 100, 1,000 or 5,000 updates a
   second from a virtual thread, a pivot and a chart set from Java, the counters (updates offered, rows sent, batches,
   bytes), what comes back from the user, and the code panels cut from the source.

## Options considered

- **Arrow IPC.** Perspective's fastest input for a large load. Here: no byte channel in the protocol (Base64, +33 %), a
  Java Arrow dependency of several MB, and a batch of 100 rows is not a large load (main thread below).
- **One column-oriented object per flush.** One array per column, no mask. A row that did not change a column of the
  union would have to carry that column's current value: the server would hold the whole table, which the browser already
  does.
- **The same layout with the changes as differences from the last value sent.** 28 % fewer protocol bytes and 17 %
  fewer on the wire than the whole numbers, below, for a state of the table on both sides to keep in step with `replace`,
  `remove`, `clear` and every reconnection. `PChart` takes that trade for a time series; a blotter's rows are many and
  each moves rarely, so the saving is small and the risk is the same.
- **Doubles**, what a `List<Double>` would be: 53 % more protocol and 21 % more on the wire.
- **The rows as one JSON text**, what the browser parses with `JSON.parse` and the engine takes as it is: 3.5 times the
  protocol, 1.4 times the wire, 1.7 times the CPU and twice the allocation.
- **A `PWebComponent`.** Point 1.
- **`worker-src` for the charts.** Point 4: the worker imports a `blob:` script of its own.
- **The packages' CDN bundles.** They import a `blob:` module at load: a policy with `blob:` in `script-src` for the
  grid, which the ESM build and the glue passed in avoid.
- **A table shared by several viewers.** Each viewer owns its table; a second view of the same data is a second table fed
  by the same calls. Perspective can share one; not asked.

## Consequences

Measured on 2026-10-29 and 30, on a Mac, Chrome 156, Firefox 157 and WebKit 27 for the tests.

- **Bytes of the library**, nothing cached (`sample/tests/bench/perspective.js`, from CDP's network events):

  | | decoded | on the wire (gzip) |
  |---|---|---|
  | JavaScript, 12 files | 784 KB | 207 KB |
  | WebAssembly, 2 files | 4,169 KB | 4,140 KB |
  | CSS, 2 themes | 181 KB | 37 KB |
  | **All** | **5,134 KB** | **4,383 KB** |
  | all but the charts (279 KB, 91 KB gzip) | 4,855 KB | 4,292 KB |

  The WebAssembly does not compress (`gzip -9`: 1.2 % and 0.5 %): Perspective compresses it itself and the browser
  unpacks it. From the click on the tab to the first rows, over localhost: 310 to 430 ms (two runs). A page without a viewer: 11.9 KB
  of script more (3.9 KB gzip) and nothing else.
- **Wire, per flush of 100 rows** (`./gradlew :ponysdk:bench -Pbench.args="v4 perspective"`: each row moves bid, ask,
  last and its time, one in three its volume, one in two its P&amp;L, over 5,000 rows; deflate as the bench computes it,
  frame header included; CPU and allocation are those of the write path on the caller's thread):

  | per flush | protocol | wire | CPU | allocation |
  |---|---|---|---|---|
  | whole numbers, the layout of point 5 | 2,634 B (26 B a row) | 1,679 B (**16.8 B a row**) | 14.6 µs | 17.0 KB |
  | doubles | 4,030 B | 2,025 B | 16.1 µs | 16.9 KB |
  | differences | 1,898 B | 1,390 B | 15.3 µs | 13.6 KB |
  | one JSON text | 9,254 B | 2,339 B | 24.3 µs | 33.3 KB |

  On the showcase page, the whole page's framing included: 1,000 updates a second offered (960 measured: the feed paces
  itself), about 10 batches and 900 to 1,000 rows sent a second, 17 to 20 KB a second, **20 B a row**; at 5,000 a second
  83 to 93 KB a second and 19 B a row; at 100 a second 1.8 KB a second.
- **Server, per update**: `update()` on the feed's thread costs 180 to 260 ns and about 195 B on top of the caller's own
  `HashMap` (45 to 90 ns, 400 B), over 500,000 updates of 5,000 keys (a throwaway harness on a real `UIContext`, not kept).
  The flush's own cost is the write path above, about 0.15 µs and 170 B a row. The server keeps the rows pending, never
  the table: the table is in the browser.
- **Browser main thread** at 5,000 rows (CDP `Performance.getMetrics`, windows of 6 to 8 s, three runs; the engine and the
  charts run in workers): 14 to 23 ms a second at 100 updates a second, **17 to 27 ms a second at 1,000** (7 to 12 of script,
  1 of layout), 29 to 40 at 5,000; a pivot at 1,000 a second 25 to 38, a chart 16 to 22. JS heap 22 MB; the engine's
  WebAssembly memory is not in it.
- **Tests.** Java, `PPerspectiveTest` (22: the creation arguments, the wire of each type, merging, removal, replace and clear,
  chunking of 5,000 rows and of 40,000 keys, what is refused and leaves nothing behind, the events from the terminal,
  the configuration as JSON, the lifecycle, a slow interval). Playwright, `perspective.spec.js`, 9 tests on Chromium,
  Firefox and WebKit: the viewer renders 5,000 rows, updates apply, the feed is conflated, a configuration from Java is
  applied and is not reported back, a user's sort and grouping are reported, a click reaches the server and the FDC3
  monitor, the policy (`'wasm-unsafe-eval' blob:`, no `'unsafe-eval'`, no violation, the chart's pixels drawn), the
  library is fetched by that page only and from the application's own origin, leaving the page frees the viewer. The axe
  audit covers the page (`a11y-audit.spec.js`): one violation, the library's.
  `BootstrapServletTest`, `ApplicationConfigurationTest` and `PonySDKAutoConfigurationTest` cover the option.
- **The policy** of an application that shows a grid: `script-src 'self' 'sha256-…' 'wasm-unsafe-eval'`. With charts,
  `blob:` too. The showcase's `csp.spec.js` now refuses the token `'unsafe-eval'`, not the substring.
- **Licences.** What is served is Apache-2.0 (the five packages; `LICENSE.md` of each is copied). `npm ci` also installs
  Node-only packages of other licences (`ws`, `http-server`, a build-time compressor under MPL-2.0) and `npm audit` reports
  two, in `follow-redirects` and `qs`: none is copied or loaded by a browser.
- **Limits.** At most 32 columns (a mask is an `int`); an integer column is 32 bits; a float is rounded to its decimals
  and is at most 2<sup>53</sup> of them; a datetime has the milliseconds of an `Instant`. One viewer per table. The
  stylesheets of the themes go to the document's head: a viewer in an embedded `<pony-app>` (ADR-0008) is not covered.
  A page that is reloaded or moved (ADR-0018) builds a new viewer, and the application sends its rows again. The strings
  of the toolbar are Perspective's, in English. The version is pinned: moving to 5.7 means running `perspective.spec.js`,
  whose selectors (`regular-table`, `perspective-viewer`) are the library's.
