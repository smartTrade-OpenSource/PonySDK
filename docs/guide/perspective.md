# Perspective

[FINOS Perspective](https://perspective-dev.github.io/) is a streaming query engine in WebAssembly with a grid and charts:
a blotter of thousands of rows that change many times a second, which the user groups, pivots, sorts, filters and charts
in the browser, with no round trip. The server feeds the table from any thread; the browser does the rest. Why it is built
so, and what was measured: [ADR-0029](../decisions/0029-perspective.md).

It is an example you copy, not a part of the `ponysdk` jar: `sample/src/main/java/com/ponysdk/sample/perspective/`
(`PPerspective`, `PerspectiveConfig`), `sample/src/main/resources/script/perspective-addon.js`, and the Gradle step in
`sample/build.gradle`. It uses public API only: `PAddOnComposite`, `Conflator`, `setTerminalHandler`. The showcase's
Perspective page runs it.

```java
final PPerspective blotter = PPerspective.builder("id",
        Column.integer("id"), Column.string("symbol"), Column.floating("bid", 4), Column.floating("ask", 4),
        Column.integer("volume"), Column.datetime("updated"))
    .charts(true).build();
panel.add(blotter);                                       // a widget; give it a height: blotter.asWidget().setHeight("540px")

blotter.replace(rows);                                    // the first load: List<Map<String, Object>>
feed.onQuote(q -> blotter.update(Map.of("id", q.id(), "bid", q.bid(), "ask", q.ask())));   // any thread, conflated
blotter.remove(List.of(42));
blotter.configure(PerspectiveConfig.builder().plugin("Datagrid").groupBy("symbol").columns("bid", "volume")
    .aggregate("bid", "avg").aggregate("volume", "sum").sort("volume", "desc").build());
blotter.addConfigListener(config -> save(config));        // what the user did: a grouping, a sort, a plugin
blotter.addClickListener(row -> fdc3.broadcast(Fdc3.instrument(row.getString("symbol"), null)));
```

## Setting it up

1. **The library, from your own files.** `sample/perspective/package.json` pins the five packages exactly (`5.6.0`, Apache-2.0)
   with a lock file; `./gradlew :sample:copyPerspective` runs `npm ci` and copies what a browser loads (5.1 MB: the modules,
   the two WebAssembly files, the engine's worker, the grid, the charts, two themes, the licences) into `script/perspective/`.
   Nothing is fetched from a CDN at run time. To move versions, change the pins, run the Perspective tests and read the
   release notes: the integration reads the packages' files by path.
2. **The script.** List `script/perspective-addon.js` with the application's scripts (11.9 KB, 3.9 KB gzip, on every page).
   The library itself is loaded by the first `PPerspective` of a page, and by no other page.
3. **The files are served.** `configuration.addServedResourceExtensions("wasm")`: only web files are served from the
   classpath ([Security](security.md)).
4. **The policy.** WebAssembly needs `'wasm-unsafe-eval'`, which compiles a module and does not run a string:

   ```java
   configuration.setWebAssemblyEnabled(true);      // script-src … 'wasm-unsafe-eval'; never 'unsafe-eval'
   configuration.addScriptSources("blob:");        // only with charts(true): their worker imports a blob: script
   ```

   Spring Boot: `ponysdk.content-security-policy.web-assembly=true`, `…script-sources=blob:`. A grid alone needs the first only.

## The table

`PPerspective.builder(index, columns...)`: at most 32 columns, one of them the index, the column that identifies a row.

| Column | Java values | Notes |
|---|---|---|
| `integer(name)` | `Integer`, or any `Number` that is a whole number of 32 bits | Perspective's integer is 32 bits |
| `floating(name, decimals)` | any finite `Number` | rounded to `decimals` (0 to 9), which is what travels: a float is a whole number of them |
| `string(name)` | `CharSequence` | |
| `bool(name)` | `Boolean` | |
| `date(name)` | `LocalDate` | |
| `datetime(name)` | `Instant`, `Date`, or milliseconds | |

A `null` sets the cell to null. A row is a `Map` of column names to values.

| Call | Thread | Use |
|---|---|---|
| `update(row)` | any | changes a row, or adds it: the index and the columns that changed; the others stay |
| `remove(keys)` | any | removes the rows of these keys |
| `replace(rows)` | any | replaces every row: the first load, a new day |
| `clear()` | any | removes every row |
| `configure(config)`, `reset()` | the UI thread | what the viewer shows |

`update` checks the row on the caller's thread: a column that is not in the table, a value of another type or out of range
is an `IllegalArgumentException`, and nothing is kept. It never blocks, and returns false once the viewer is destroyed.

### A feed that is faster than the screen

The updates of a row between two flushes merge (the last value of each column wins), and one batch goes to the browser per
flush, at most every `interval` (100 ms by default; `builder.interval(...)`). A feed of 1,000 updates a second over 5,000
rows sends about 10 batches and 960 rows a second; at 5,000 a second, 10 batches and about 4,800 rows. A browser that reads
slowly gets fewer batches: the feed is never held. `getOffered()`, `getApplied()` and `getFlushes()` count them; the page shows
them per second. The [`Conflator`](streams.md) sets the flush, and handles a suspended context and the context's destroy.

## What the user does

- `addConfigListener(Consumer<PerspectiveConfig>)`: the configuration after a change of the user's (a column dragged to a
  grouping, a sort, another plugin), 100 ms after the last. What `configure` set does not come back.
- `addClickListener(Consumer<JsonObject>)`: the row clicked, with the columns the viewer shows.
- `addSelectListener(Consumer<JsonObject>)`: the row selected, or null when the selection is cleared. The grid reports a
  selection in its `SELECT_ROW` mode: `PerspectiveConfig.builder().pluginConfig(Json.createObjectBuilder().add("edit_mode",
  "SELECT_ROW").build())`; a chart reports the bar or the point.

`PerspectiveConfig` uses Perspective's names: plugins (`"Datagrid"`, `"Y Line"`, `"Y Bar"`, `"Treemap"`…), sort directions
(`"asc"`, `"desc"`), filter operators (`"=="`, `">"`, `"contains"`, `"in"`…), aggregates (`"sum"`, `"avg"`, `"last"`…). A
field left unset is not changed by `configure`; `config.toJson()` is Perspective's own JSON, which also loads in a
`restore()` from JavaScript.

## On the wire

One call per flush, a typed array of numbers and strings, never JSON per row: the flags, the keys to remove, then per row its
key, the mask of the columns it carries and their values. A float is a whole number of its decimals, a datetime the time since
the batch's, a string an entry of the string dictionary after its first use. Nothing is kept between batches. A flush of 100
rows that move four columns, over 5,000 rows, is 2.6 KB of protocol and 1.7 KB on the wire (17 B a row); the same rows as one
JSON text would be 9.3 KB and 2.3 KB. A replace of 5,000 rows is several calls of one flush, of 16,384 values at most.

## What it costs

- **Bytes**: 5.1 MB of library on the page that has a viewer (4.4 MB gzip: the 4.2 MB of WebAssembly does not compress), 4.9 MB
  without the charts. A page without a viewer loads none of it. The first rows appear about 0.3 to 0.4 s after the page opens
  over localhost, nothing cached.
- **Browser**: at 1,000 updates a second over 5,000 rows, the main thread is busy 17 to 27 ms in every second; the engine and the
  charts run in workers.
- **Server**: `update` costs about 200 ns and 200 B on the feed's thread; the server keeps the rows pending, not the table.

## Not there

- A page reloaded or moved to another server builds a new viewer, and the application sends its rows again.
- One viewer per table; a second view of the same data is a second `PPerspective` fed by the same calls.
- A viewer inside an embedded `<pony-app>` ([Embedding](embedding.md)): the themes' stylesheets go to the document's head.
- Perspective's own texts (its toolbar) are in English, and its settings button has no accessible name: the library's own
  markup. The showcase darkens the Pro Light theme's blue and red numbers and its inactive text, which fall short of WCAG AA, with
  the library's custom properties ([`showcase.css`](../../sample/src/main/resources/css/showcase.css), [Accessibility](accessibility.md)).
