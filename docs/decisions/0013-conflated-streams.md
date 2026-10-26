# ADR-0013: A stream of values bound to a context, the latest value per key at each flush

- Status: Accepted
- Date: 2026-10-26

## Context

A trading screen shows values that another thread produces: a Kafka consumer, a Reactor `Flux`, a market data
callback. PonySDK gives that thread one way in, `UIContext.execute(Runnable)` (`UIContext.java:287`):

- One `execute` is one transaction and one flush: a lock, the runnable, `Txn.commit`, a WebSocket frame
  (`UIContext.java:294-304`, `Txn.java:74-79`). An application calling it per event sends a frame per event: 1 000
  price updates on 100 instruments are 1 000 frames, where the screen can show 100 values.
- The flush runs under the context's lock, and it waits when the client is behind: a send in flight and 4 buffers
  queued, the next one waits up to `wsSendTimeoutMs`, 60 s by default (`WebSocketPusher.java:916,946-950`). The
  producer's thread waits with it. One consumer thread serving several sessions is held by the slowest of them, and
  the events behind it wait in its queue, however old they become.
- While a context is suspended (transparent reconnection), `execute` writes into the `RecordingEncoder`, whose
  overflow destroys the context (`UIContext.java:943-949`): a fast producer turns a 3-second network drop into a
  lost session.
- Coalescing already keeps the last value of a property, within one transaction only (`ModelWriter.java:98-117`).

Each application writes its own batching today, if it writes one. Nothing in `ponysdk/src/main` takes a
`Flow.Publisher` or conflates.

## Decision

1. `com.ponysdk.core.server.concurrent.Conflator<K, V>`: values offered from any thread, applied on the context's
   UI thread, the latest per key.

   ```java
   final Conflator<String, Quote> quotes = Conflator.of(uiContext, (instrument, quote) -> rows.get(instrument).show(quote));
   feed.subscribe(quote -> quotes.offer(quote.instrument(), quote)); // any thread
   ```

   - `offer(key, value)` never blocks and never waits for the client: it replaces the key's pending value and starts a
     drain if none is running. It returns false once the conflator is closed.
   - A drain takes every pending value at once and applies them in one `execute`: one transaction, one flush, in the
     order the keys first arrived since the previous drain.
   - One drain at a time. A drain that waits for a slow client holds no one but itself, and the values offered
     meanwhile replace each other: the next drain sends the latest. The pending values are bounded by the number of
     keys, not by the rate.
   - The drain runs on a virtual thread by default, where waiting for the client costs no platform thread;
     `Conflator.builder(uiContext, apply).executor(executor)` gives it another.
   - `minInterval(Duration)` spaces the drains (a screen refreshed 10 times a second at most); none by default.
   - While the context is suspended, nothing is applied: the values wait, conflated, and the first drain after the
     reconnection applies the latest. The recording buffer is not filled by the stream.
   - `close()` stops it and drops what is pending; the context's destroy closes it.
   - An `apply` that throws is logged by `execute`, as today; the next values are still applied.
   - `getOffered()` and `getApplied()` count values, so an application or a page can show how much was conflated.
2. A `Flow.Subscriber` for a publisher: `quotes.subscriber(Quote::instrument)` requests without limit, since offering
   never blocks, and closes the conflator on `onComplete` or `onError`. A Reactor `Flux` reaches it through
   `JdkFlowAdapter.publisherToFlowPublisher`, a Kafka consumer calls `offer`: no dependency is added.
3. No protocol change, nothing in the terminal: a drain writes what the `apply` calls write.
4. The sample shows it: a feed thread producing quotes faster than the screen can show them, the counters of offered
   and applied values, and the code from its source.

## Options considered

- **An `execute` per event, documented as the way.** What exists: a frame per event and a producer held by the
  slowest client, as above.
- **Conflation in the write path** (`ModelWriter` keeping the last value across transactions until the socket is
  free). It would conflate every application's updates, including those an application needs in order (a row
  added then filled), and the producer would still take the lock and wait in the flush. Rejected: it changes what
  every existing application sends.
- **A bounded queue between the producer and the context.** Bounded memory, but a slow client then either blocks the
  producer or drops values in arrival order, the latest among them. For a value that replaces the previous one, the
  latest per key is the right thing to keep.
- **Reactor or RxJava as a dependency** (`sample`, `onBackpressureLatest`). They conflate one stream, not per key,
  and add a dependency to every PonySDK application. `java.util.concurrent.Flow` is in the JDK and every reactive
  library adapts to it.
- **A drain triggered by the sender's completion** (the queue of `WebSocketPusher.Sender` signalling room). Finer, but
  it exposes the sender's state, which is private today; the drain waiting in the flush on its own thread gives the
  same pacing. To revisit if the measurement shows drains blocked in the flush cost too much.

## Verification

- Unit tests: latest value per key, first-arrival order, one drain at a time, `minInterval`, nothing applied while
  suspended then the latest after resume, close on destroy, `offer` after close, `apply` throwing, the
  `Flow.Subscriber`.
- A client that does not read: `offer` keeps returning at once, and the pending values stay at the number of keys.
- The bench: per update, CPU, allocation and wire bytes of an `execute` per update against a conflator, on the same
  scenario.

## Consequences

- An application keeps `execute` for what must be applied in order; a stream of replacing values goes through a
  conflator.
- A conflator holds the latest value of each key until it is applied: memory in the number of keys.
- The values are applied by the drain's thread, outside the producer's: an `apply` sees the value, not the producer's
  thread-local state.

## Outcome (2026-10-26)

- `Conflator` (`ponysdk/src/main/java/com/ponysdk/core/server/concurrent/Conflator.java`): two maps swapped at each
  drain, no allocation per drain; the delays (`minInterval`, a suspended context looked at again every 100 ms) go
  through `CompletableFuture.delayedExecutor`, no timer thread of its own.
- `ConflatorTest`: 10 tests. With the drain blocked in its `apply`, 100 000 offers on 10 keys return at once and 11
  values are applied: the first, then the latest of each key. Each of four mutations fails at least one test: no
  suspension check, a drain per offer, an `apply` that throws stopping the batch, no cancel of the subscriptions.
- The bench (`./gradlew :ponysdk:bench -Pbench.args="v4 stream"`), per quote, 100 instruments, the client's sends
  completing at once (a real socket adds its cost per frame, which the conflator divides):

  | quotes between two flushes | | frames | protocol | wire | CPU | allocation |
  |---|---|---|---|---|---|---|
  | 100 | `execute` each | 1 | 13.0 B | 10.9 B | 0.36 µs | 24 B |
  | 100 | conflated | 0.010 | 6.6 B | 2.6 B | 0.09 µs | 26 B |
  | 1 000 | `execute` each | 1 | 13.0 B | 10.8 B | 0.13 µs | 24 B |
  | 1 000 | conflated | 0.001 | 1.0 B | 0.4 B | 0.01 µs | 4 B |

  At 100 quotes, 63 % of the instruments changed: each applied value costs the same, the frame is shared. The CPU of
  an `execute` each varies with the order of the runs (0.13 to 0.36 µs); the conflated cost does not depend on it.
- The showcase's Streams page: 24 instruments, a feed thread at 1 000, 10 000 or 100 000 quotes a second,
  `minInterval` 50 ms. On Chrome, localhost: 9 945 offered and 408 applied a second at 10 000 (95.9 % replaced),
  100 133 and 408 at 100 000 (99.6 %). `showcase-smoke.spec.js` checks it on Chrome, Firefox and WebKit.
