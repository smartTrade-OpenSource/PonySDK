# Streams

A screen that shows values another thread produces (a Kafka consumer, a Reactor `Flux`, a market data callback)
goes through a `Conflator`: the thread offers each value, the screen gets the latest value of each key, one flush at
a time. Why, and what was measured: [ADR-0013](../decisions/0013-conflated-streams.md).

```java
final Conflator<String, Quote> quotes = Conflator.of(uiContext,
    (instrument, quote) -> rows.get(instrument).setText(quote.bid()));

feed.onQuote(quote -> quotes.offer(quote.instrument(), quote)); // the feed's thread
```

- `offer(key, value)` never blocks: it replaces the key's pending value and returns. It returns false once the
  conflator is closed.
- The values are applied on the context's UI thread, in one transaction: one frame for all the keys that changed
  since the previous flush, in the order they first came.
- One flush at a time. A client that reads slowly holds the conflator's drain, never the feed: the values offered
  meanwhile replace each other, and memory stays at one value per key.
- While the connection is lost and the context waits for its client ([Reconnection](reconnection.md)), nothing is
  applied: the first flush after the reconnection shows the latest values.
- The context's destroy closes the conflator; `close()` closes it sooner, for a screen the user left.

Use `UIContext.execute` for what must be applied in order, every one of them (a row added, then filled); a
conflator for values that replace the previous one.

## Options

```java
Conflator.<String, Quote> builder(uiContext, apply)
    .minInterval(Duration.ofMillis(50)) // at most 20 flushes a second; none by default
    .executor(executor)                 // where the drains run; a virtual thread each by default
    .build();
```

## A `Flow.Publisher`

```java
publisher.subscribe(quotes.subscriber(Quote::instrument));
```

The subscriber requests without limit, since offering never blocks. The publisher's completion or error closes the
conflator, and closing it cancels the subscription. A Reactor `Flux` subscribes through
`JdkFlowAdapter.publisherToFlowPublisher(flux)`.

## Counting

`getOffered()` and `getApplied()` count values since the creation: their difference is what was replaced before
being shown. The showcase's Streams page shows them per second, with the code from its source.
