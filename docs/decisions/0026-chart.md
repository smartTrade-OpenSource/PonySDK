# ADR-0026: A chart drawn by the terminal, fed by typed arrays

- Status: Accepted
- Date: 2026-10-29

## Context

PonySDK has no chart. A trading screen shows time series: a price, a P&L, a latency, several at once, updated many
times a second. The ways to get one today are a JavaScript library behind a `PAddOn` (each application writes its
glue and ships the library), or Vaadin's charts, which are commercial.

The showcase's Performance page draws its "60-second charts" as 60 `div`s per chart whose heights are set from Java
(`Sparkline`). Per sample, measured on the write path (`ProtocolBench`, a throwaway copy of that class): 797 B of
protocol, 39.9 B on the wire, 7.9 µs of CPU and 4.8 KB allocated on the server, for a bar chart with no axis, no
time and no tooltip.

Constraints of the task: no third-party library; no protocol key and no widget type (4 keys are left in
`ServerToClientModel`); the drawing code loaded only by a page that has a chart (ADR-0004); colours from CSS custom
properties; the strict policy of ADR-0015.

## Decision

1. `com.ponysdk.core.ui.chart.PChart`, a `PAddOnComposite<PElement>` built with `PChart.builder(type, series...)`:
   `LINE`, `AREA` or `BAR`; `title`, `decimals`, `window`, `maxPoints`, `interval`, `dataTable`. Points go in with
   `append(time, values...)` on the UI thread, `offer(series, value)` from any thread, `clear()`. The signature of
   the addon is the class's name; nothing is added to the protocol.
2. **The wire is the addon's typed arrays.** Creation arguments (`PADDON_CREATION_ARGS`): type, decimals, window,
   maxPoints, title, table rows, series names. A point is a `PADDON_METHOD` "append" with `PADDON_ARGUMENTS`: the time
   since the previous point, then for each series its change since the previous point in units of 10<sup>-decimals</sup>,
   or null for no change. The array encoder already writes the smallest of byte, short, int and long: a price that
   moved by a few units is a tag and a byte. The server and the terminal each keep the previous point (`PChart`, and
   `Rows` in `chart-model.ts`), both reset by `clear`. A value is rounded to the decimals, which are also what the axis
   and the tooltip show.
3. **Conflation by the existing `Conflator`** (ADR-0013). `offer` stores the value and the time under a lock and
   offers one constant key: a flush on the UI thread, at most every `interval` (100 ms), sends one point with the
   latest value of every series that moved and the time of the last offer. The conflator's own guarantees apply: the
   feed is never blocked, nothing is applied while the context is suspended, the context's destroy closes it.
4. **A chunk of the terminal, loaded when a chart is created.** The drawing code is `chunks/chart.js` (`chart.ts`,
   with `chart-model.ts` bundled in it). The dispatcher's loader (`loadingOf(type, reader)`) now also looks at an
   `ADDON` or `ADDON_COMPOSITE` block: it reads the FACTORY signature, rewinds, and for a signature in `ADDONS`
   (`runtime/factory.ts`) returns the loading of its chunk. The block waits as it does for the module of a widget
   type (ADR-0004, decision 4); the chunk's factory joins the page's addon table, where `defineAddon` puts the
   others. Unlike the modules of ADR-0004, this one is not in `loadAll`: the idle loading of decision 5 would fetch
   it on every page, and a chart is on few.
5. **Canvas**, from a measurement (below). Two canvases over each other: the data, redrawn once per frame at most
   when a point, the size or the theme changed, and the crosshair, which the pointer redraws alone. The size is
   measured in the next frame, not in the `ResizeObserver` callback (WebKit reports a loop otherwise).
6. **The pure part is apart from the drawing.** `chart-model.ts` holds the store of points (a time column and one
   column per series, trimmed by window and by count), the decoding of a point, the 1-2-5 ticks of the value axis, the
   round-time ticks of the time axis (local clock, from 100 ms to 30 days), the labels, the scales, the nearest point,
   the summary for a screen reader: the Node tests drive them.
7. **Accessible.** The plot is `role="img"`; its `aria-label` is the title, the last value of each series and the time,
   rewritten at most once a second. `dataTable(n)` adds a table of the last n rows, newest first, in a clipped wrapper
   (a table that is not clipped by a wrapper keeps its width). The legend is text with the last values; a space
   separates a name from its value.
8. **Colours and themes.** `--pony-chart-1..8`, `--pony-text`, `--pony-border`, `--pony-surface`, read with
   fallbacks (a palette of eight, the chart's own colour, a translucent grey, `Canvas`) once the chart is in the
   document, when `prefers-color-scheme` changes and when an attribute of `<html>` changes. The chart and its
   stylesheet live in a shadow root, the sheet constructed by script: no `style` attribute, and the application's CSS
   does not reach in. Style properties set from script (the tooltip's position, the colour chips) are CSSOM, which the
   policy allows. A chart out of the document watches nothing.
9. **Showcase.** A Charts page (a four-series feed at 1,000 and 10,000 ticks a second, an area, bars, "Clear", code
   panels cut from the source), and the Performance page's three charts are `PChart`s: they fit, with a point per
   second from the UI thread and the history of `LiveHeap` appended at creation. Their legend carries the last value
   and their axes the range; the "min, avg, max" line of the former cards is gone.

## Options considered

- **SVG.** One `path` per series, rewritten at each point. Measured against the canvas, same chart, below: 2 to 4 times
  the main thread, and a layout cost the canvas does not have. It would give a DOM to read, which `role="img"` and the
  table give anyway.
- **A third-party library** (uPlot, Chart.js, ECharts behind a `PAddOn`): excluded by the task; each application would
  also ship it and write its glue, which is what exists today.
- **A point as JSON, or the values as absolute doubles.** Absolute: 113 B of protocol and 50 B on the wire per point of
  ten series, against 36 and 20. JSON per point is what the task excludes.
- **A conflator key per series.** One `apply` per series per flush, which would have to guess where the flush ends to
  send one point. A single key makes the flush the point.
- **Absolute values in units instead of changes.** No previous point to keep on both sides and no `clear` to keep
  in step, at 4 to 5 bytes a value instead of 2 (not measured). Kept as the fall-back if a gap in the frames ever
  matters: with transparent reconnection the frames are replayed in order, and an overflow of the recording buffer
  destroys the context (ADR-0013), so a chart does not see a gap.
- **Loading the chunk with the others when idle.** Decision 4.
- **Decimating the points to the pixels.** A chart keeps 3,600 points by default (100,000 at most) and strokes them
  all; at 10 series of 600 points the draw call is 0.15 ms. Decimating is the first thing to add if a chart is asked
  to hold much more.
- **A secondary value axis, zoom, a toggle on the legend, a category axis.** Not asked; none is ruled out, each is
  more terminal code and more API.

## Consequences

Measured on 2026-10-29.

- **Canvas against SVG** (`sample/tests/bench/chart-render.js`, Chrome 156, headless, 900x360 at device pixel ratio 2,
  10 series of 600 points sliding, 6 s per run, main thread time per second from CDP's `Performance.getMetrics`;
  the same at ratio 1 within 1 ms):

  | | main thread | script | layout | draw call, mean / p95 | frames |
  |---|---|---|---|---|---|
  | canvas, 10 Hz | 9 ms/s | 3 | 0 | 0.17 / 0.30 ms | 60 fps, p95 16.7 ms |
  | SVG, 10 Hz | 18 ms/s | 9 | 1 | 0.79 / 1.10 ms | 60 fps, p95 16.8 ms |
  | canvas, 60 fps | 20 ms/s | 10 | 0 | 0.15 / 0.20 ms | 60 fps, p95 16.8 ms |
  | SVG, 60 fps | 77 to 85 ms/s | 49 to 54 | 8 to 9 | 0.79 to 0.87 / 0.90 to 1.10 ms | 60 fps, p95 16.8 ms |

  The rasterisation of both happens outside the main thread and is not in these figures; both held 60 fps. On the
  showcase's Charts page (three charts at 10 points a second, a four-series feed): 7.8 ms/s of the main thread against
  0.6 on the Inputs page.
- **Size.** `chunks/chart.js` 12,548 B, 5,141 B gzip, fetched by a page that creates a chart: the Charts and
  Performance pages, and none other (`chart.spec.js` checks that the Inputs page, left alone for 6 s, never requests
  it). `terminal.js` 95,330 to 95,704 B (+374), 29,377 to 29,523 B gzip (+146): the signature lookup in the
  dispatcher's loader.
- **Bytes and CPU per point** (`./gradlew :ponysdk:bench -Pbench.args="v4 chart"`, prices moving by up to 20 units
  of 10<sup>-5</sup>, 100 ms apart; wire is permessage-deflate as the bench computes it, frame header included). A
  flush of the conflator is one point:

  | per point | protocol | wire | CPU | allocation |
  |---|---|---|---|---|
  | 1 series, units | 18 B | 7.2 B | 0.1 to 0.6 µs | 120 B |
  | 1 series, absolute doubles | 32 B | 13.2 B | 0.2 µs | 120 B |
  | 10 series, units | 36 B | 20.1 B | 0.2 µs | 184 B |
  | 10 series, one moved, nine null | 27 B | 9.5 B | 0.3 µs | 208 B |
  | 10 series, absolute doubles | 113 B | 50.1 B | 0.2 µs | 352 B |

  The first scenario of a run pays the harness's warm-up (0.6 to 0.75 µs there, 0.1 when it runs second). A feed of
  1,000 ticks a second on ten series sent as each tick comes would be 1,000 frames and about 9.5 KB/s; conflated at 100
  ms it is 10 frames and about 200 B/s. On the Charts page, four series: 18 to 19 B on the wire per point, the whole
  page's framing included, at 1,000 and at 10,000 ticks a second alike (the page shows 9 to 10 points a second for
  both). The former Sparkline, per sample: 797 B, 39.9 B on the wire, 7.9 µs, 4.8 KB.
- **Tests.** Node: `chart-model.test.ts` (14: store, trim, decoding, malformed rows, ticks, domains, labels, scales,
  nearest point, summary) and `chart.test.ts` (4: the frames the real write path writes in `TerminalFixtures`' `chart`
  fixture decode to the values given, the creation arguments, the loading of the chunk). Java: `PChartTest` (11: what
  goes on the wire, what is refused, a refused point leaving the state alone, `clear`, calls before creation, destroy)
  and `PChartFeedTest` (1: 1,001 offers on a real context, three points). Playwright: `chart.spec.js`, 11 tests on
  Chromium, Firefox and WebKit. The terminal's coverage on the chart's files: 100 % of lines, 98 to 100 % of
  branches (`chart.spec.js` joins the CI's coverage job).
- A chart holds its series in the browser: the terminal's memory is the points kept, 8 bytes per value, and the server
  holds the last point only.
- A page loaded before a deployment that adds a chart to the application gets the new chunk with the old core:
  the chunk imports nothing of the core at run time, so it loads; ADR-0004's window of seconds does not apply.
- Limits: one value axis, no zoom or pan, eight colours that repeat, a text of the chart in English where the
  terminal writes it ("no data", "at", "Time").
