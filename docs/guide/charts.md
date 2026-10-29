# Charts

`PChart` draws time series in the browser: lines, areas or bars, one or several series, a value axis scaled to the
data, a time axis, a legend, a tooltip with a crosshair, and a sliding window for live data. The server sends a point
as a few bytes of a typed array; the terminal draws it on a canvas. Why it is built so, and what was measured:
[ADR-0026](../decisions/0026-chart.md).

```java
final PChart chart = PChart.builder(PChart.Type.LINE, "EURUSD", "GBPUSD")
    .title("Prices")                    // what a screen reader calls it
    .decimals(5)                        // the precision of the values, on the axis, in the tooltip and on the wire
    .window(Duration.ofSeconds(60))     // keep and show the last 60 seconds
    .build();
panel.add(chart);                       // a PChart is a widget

chart.append(System.currentTimeMillis(), 1.0851, 1.2702); // a point: a time, one value per series
```

`PChart` has no size of its own beyond a default height of 260 px: set the height like any widget's
(`chart.asWidget().setHeight("320px")`), the width follows its container.

## Points

A point is a time (milliseconds since the epoch) and one value per series.

| Call | Thread | Use |
|---|---|---|
| `append(time, values...)` | the UI thread | a point at a given time: a history, a sample taken every second |
| `offer(series, value)` | any thread | a live feed: the latest value of a series, conflated (below) |
| `clear()` | the UI thread | removes every point; the next one starts the chart again |

- Times never go back: a point with a time before the previous one's is refused (`IllegalArgumentException`), as is
  a number of values other than the series'.
- A `NaN` leaves its series where it was: no value yet before its first, its previous value after. A line holds
  a price that did not move.
- A value is rounded to `decimals` (0 to 9, 2 by default) and must stay below 2<sup>53</sup> units of that precision.

### A feed that is faster than the screen

```java
final PChart chart = PChart.builder(PChart.Type.LINE, "EURUSD", "GBPUSD").decimals(5)
    .interval(Duration.ofMillis(100)).window(Duration.ofSeconds(60)).build();

marketData.onQuote(quote -> chart.offer(quote.series(), quote.bid())); // the feed's thread, 1,000 times a second
```

`offer` never blocks. It goes through a [`Conflator`](streams.md): the values offered between two flushes replace each
other, and one point goes to the browser per flush, at most every `interval` (100 ms by default), with the time of the
last offer; a series not offered since the previous point keeps its value. A feed of 1,000 ticks a second sends 10
points a second, and a browser that reads slowly sends fewer: the feed is never held. While the connection is lost
([Reconnection](reconnection.md)) nothing is sent and the first flush after it carries the latest values. A chart that
leaves the page stops its flushes (`onDestroy`, or `destroy()`).

Ticks that must all be drawn (a trade blotter's prints, not a price) are `append`ed one by one, on the UI thread.

## What is kept

Each chart keeps its last 3,600 points. `window(Duration)` keeps the points of that long before the last one, and the
time axis shows that window: it does not stretch while the chart fills. `maxPoints(n)` (2 to 100,000) changes the
cap, which also applies to a window. The terminal trims; the server holds no points.

## Types

| Type | Draws | Value axis |
|---|---|---|
| `LINE` | one line per series, broken where a series has no value | fits the data |
| `AREA` | the lines, with a translucent fill to the bottom of the plot | fits the data |
| `BAR` | one bar per point and series, side by side | includes zero |

Up to eight series have their own colour; more repeat them.

## Reading by screen reader

The chart is an image (`role="img"`) whose label is its `title`, the last value of each series and the time of the
last point: "Prices: EURUSD 1.0851, GBPUSD 1.2702 at 10:32:05". It is updated at most once a second, whatever the
rate of the points, and is not a live region: it is read when the chart is visited. `dataTable(rows)` adds a table of
the last rows, newest first, that a screen reader walks and the eye does not see. The legend is text, with the last
value of each series. The words "no data", "at" and "Time" are English; the title and the series' names are yours.

## Colours and themes

The terminal reads CSS custom properties on the chart, with fallbacks that read on a light and on a dark page:

| Property | Used for | Fallback |
|---|---|---|
| `--pony-chart-1` … `--pony-chart-8` | the series | a palette of eight colours |
| `--pony-text` | axis labels, crosshair, tooltip text | the chart's own `color` |
| `--pony-border` | grid lines, tooltip border | a translucent grey |
| `--pony-surface` | tooltip background | `Canvas`, which follows the colour scheme |

```css
:root { --pony-chart-1: #4f46e5; --pony-chart-2: #059669; }
```

A change of the page's colour scheme (`prefers-color-scheme`), or of an attribute of the `<html>` element, redraws the
chart with the new values. The chart is drawn in a shadow root by a constructed stylesheet: no inline `style`
attribute, so it works under the policy PonySDK builds ([Security](security.md)), and the application's own CSS does
not reach into it.

## What it costs

- The drawing code is a chunk of the terminal, `chunks/chart.js`, 12.5 KB (5.1 KB gzip). A page loads it when the
  server creates its first chart, not before: a page without a chart never fetches it, and `terminal.js` grew by 374
  bytes for the hook that loads it.
- A point of ten series is 36 bytes of protocol and about 20 bytes on the wire (deflate), against 113 and 50 as
  absolute doubles. A point where one of ten series moved is 27 and 9.5 bytes.
- In the browser, three charts fed at 10 points a second cost about 8 ms of the main thread per second.

## Not there

One value axis (no secondary axis), no zoom or pan, no series hidden by a click on the legend, no categories on the
time axis. A chart that needs them is a `PAddOn` over a JavaScript library ([Custom JavaScript](addons.md)).

The showcase's Charts page shows a four-series feed at 1,000 and 10,000 ticks a second, an area and bars, each with
its code; its Performance page draws its 60-second metrics with `PChart`.
