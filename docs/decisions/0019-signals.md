# ADR-0019: Signals, reactive state bound to widgets

- Status: Accepted
- Date: 2026-10-29

## Context

A screen shows a value in several places: a price in a tile, in a row of a list, in a total; a quantity in a text box,
in a label under it, in a button's enabled state. An application keeps them in step by hand: each place is a setter
call, in each handler and each feed callback that changes the value. A new place means finding every writer.

PonySDK has the parts to do better, and none of them is this:

- `PValueChangeHandler` and the `EventBus` (`docs/guide/event-bus.md`) carry what the user did; they do not hold a
  value that others read.
- `Conflator` (ADR-0013) applies the latest value per key on the UI thread, for a thread that produces values; the
  application still writes, per key, the setters of every widget showing it.
- A widget has no way to follow a value: `PLabel.setText` is a push.

Vaadin 25.1 has "signals" for this: a value, a value computed from others, widgets bound to them.

## Decision

1. `com.ponysdk.core.ui.signal`, four public types:
   - `Signal<T>`: `get()`, `Signal.of(initial)` and `Signal.computed(supplier)`. `MutableSignal<T>` adds `set(value)`
     and `update(fn)`. A computed signal cannot be set: the type says it, not a runtime error.
   - `Bind`: static methods, each called on the UI thread of the widget's context. `text` (label, button),
     `visible`, `enabled`, `styleName(widget, name, signal)` (on while true), `value(box, signal)` (two-way: the
     input sets the signal), `children(panel, list, key, row)` (keyed reuse), and `to(widget, signal, setter)` for
     any other property. `configure(minInterval, executor)` paces the context's bindings.
   - `Binding`: what a `Bind` method returns; `close()` ends it sooner than its widget does.
   The package is a package of its own because `Signal` and `Bind` depend on `ui.basic` and `server.concurrent` and
   nothing depends on them; `PWidget` does not change (ADR-0012), there is no `bindText` on a widget.
2. A signal is read and set from any thread, and shared by any number of contexts: one value, set once, followed by
   the bindings of every session. Values are compared with `equals`: setting an equal value does nothing.
3. A computed signal tracks what its function reads (`get()` called inside it), so its dependencies are the ones it
   read the last time; a branch not taken is not followed. It is lazy and cached, and glitch-free:
   - While something follows it, it follows its sources: a change marks it dirty and tells its followers once (the
     clean-to-dirty transition); a clean one is read without a look at its sources.
   - While nothing follows it, no source holds it, and it compares the versions of its sources with those it
     computed from.
   - A recomputation giving an equal value keeps its version: what reads it does not recompute.
   - A diamond (`sum = a + 1 + 2a`) computes once per change of `a`, whatever the number of paths, and never from a
     mix of old and new values: a binding reads when it runs, after all the changes that came before it.
   - A computation that throws leaves the signal to be computed again at the next read, and keeps following what it
     read before throwing, so a change of it is told: a binding does not stay stuck on a failure.
4. A change reaches a widget on the UI thread of its context, at most once per binding per flush. A signal tells its
   bindings it changed, on the thread that changed it; a binding joins its context's list of changed bindings once
   (a flag), and the first one to join offers the context's `Conflator` its one key. The drain, one at a time, with
   the `minInterval` and the suspension rules of ADR-0013, runs the list in one transaction: one flush for every
   binding that changed. Each binding reads its signal when it runs, so it shows the latest value. A binding that
   throws is logged; the others of the drain run.
5. A binding is released when its widget is destroyed, and when its context is: a signal outlives both and does not
   keep them. A computed signal nothing follows is held by none of its sources. A widget removed by `children` is
   destroyed with its binding.
6. `children(panel, items, key, row)` makes a widget by `row` once per key, keeps it while an item has that key, moves
   it when the order changes (the terminal moves the same element) and destroys it when the key is gone. A widget does
   not follow its item's changes: it follows signals the item holds. The list is replaced, not changed in place.
7. No protocol change, nothing in the terminal: a binding calls the setters a direct call would, and the wire is
   what they write.
8. The showcase has a Signals page: a feed thread setting 10 000 signals a second, a computed total, a text box bound
   both ways, a keyed list.

## Options considered

- **Explicit dependencies**, `computed(fn, a, b)`. No thread-local, but a dependency list that repeats what the
  function reads and goes wrong when one is added to the function only. Tracking costs one thread-local read per
  `get()`.
- **Eager computed signals**, recomputed at each change of an input. A diamond computes twice per change and a
  binding can see the first, inconsistent, result; a burst of 100 changes computes 100 times.
- **One lock for the whole graph.** Simple, and it serialises every session's UI thread behind one feed thread that sets
  a shared signal. Locks here go from a signal to its sources only, and a change takes none: it sets atomics and
  adds to a list.
- **A `Conflator<Binding, Binding>`, one key per binding.** The first version, and the most direct reuse. Each key
  costs a `LinkedHashMap` entry per drain: 41 B allocated per updated cell, against 2 B for the list swapped with a
  spare (60 of 240 cells updated per flush, `./gradlew :ponysdk:bench -Pbench.args="v4 signals"`, same run settings).
- **Weak references from signals to bindings**, so that nothing needs releasing. A binding released when the
  collector runs is not released when the widget is destroyed: the signal keeps updating a destroyed widget until
  then, and the leak test would depend on the collector. Release on destroy is deterministic.
- **`bindText(signal)` and the like on the widgets.** About ten methods on `PWidget` and its subclasses for what
  `Bind` does from outside, against ADR-0012's reduction of the surface.
- **`Signal<T>` with `set` on every signal, computed ones throwing.** A smaller API, and a mistake found at run time.
- **Applying a change in the setter's thread** (the feed's), under `execute`. The feed's thread would wait for the
  client, which ADR-0013 removes.

## Consequences

- Measured, protocol 4, 60 of 240 labels updated per flush, per cell, the drain run on the measuring thread
  (`./gradlew :ponysdk:bench -Pbench.args="v4 signals 4"`, five runs):

  | update | frames per flush | wire | CPU | allocation |
  |---|---|---|---|---|
  | `setText`, all 60 in one `execute` | 1 | 3.9 B | 33 to 41 ns | 0.40 B |
  | `setText` from the feed's thread, an `execute` each | 60 | more | 93 to 103 ns | 32 B |
  | signal set, label bound | 1 | 3.9 B | 53 to 57 ns | 1.33 B |
  | signal set, label bound to a computed signal of it | 1 | 3.9 B | 76 to 79 ns | 1.33 B |

  The first row is a setter's best case: the 60 values already on the UI thread, in one transaction. A feed's thread
  that sets widgets without signals pays a transaction and a frame per value (second row); a signal costs about half
  of that, in one frame. Against the best case a bound signal adds 18 to 20 ns, for two reasons that are its
  function: the value is compared twice (5.5 ns in the signal, to keep "the same value does nothing", then again by
  the setter), and the update takes two passes (marked on the feed's thread, applied in the drain). The 0.9 B more
  than a setter is 56 B per flush of 60 cells, nothing per binding.
- Tuned (2026-10-30): a binding reads its signal without tracking (no thread-local lookup, it is never inside a
  computation), and its "queued" flag is a volatile read before the context's lock instead of a compare-and-set. A
  computed signal's binding went from 82–88 ns to 76–79 ns; a plain one did not move (50–55 ns, within noise). Two
  other changes were measured and dropped: the signal without its monitor (−2 ns, within noise; the monitor keeps
  `update` atomic) and a lock-free list of changed bindings instead of the context's lock (no difference).
- On the showcase's Signals page (Chrome, localhost, 8 instruments, bindings paced at 50 ms): 9 836 signals set a
  second, 144 values computed and shown a second, 98.5 % replaced before being shown.
- `terminal.js` does not change: the terminal is not touched.
- Tests: 42, `SignalTest` (graph alone: equality, laziness, one computation per read, diamond, equal value stops
  propagation, followed and unfollowed states, failure and retry, a change between a read and the follow, concurrent
  `update` from 8 threads, collectability of a computed signal nothing follows) and `BindTest` (on real contexts:
  one drain for a burst of 1 000 sets, one computation for a burst of 100 changes of two inputs, one drain for 200
  bindings changed together, each binding, two-way, keyed reuse and
  reorder, a refused duplicate key, a row that cannot be made, release by widget destroy and by context destroy proved
  with `WeakReference`, a feed thread with the default virtual-thread drain). The package is covered 263 lines of 263
  and 110 branches of 110. Removing the call that ends the bindings on a context's destroy, the call that tells a
  follower of a change made during a computation, or the reset of a binding's flag makes tests fail.
- A signal set on the UI thread (the user's input, through `value`) reaches the other widgets in the next drain, not in
  the same transaction: a hop to a virtual thread, then one flush.
- `Bind.configure` must come before the context's first binding, or it throws: the showcase calls it at the start of
  its entry point. The other settings (`minInterval`, `executor`) are `Conflator`'s.
- A binding is released with its widget: a widget that is neither destroyed nor in a window of its context is
  released with the context. Nothing else in the application needs to unsubscribe.
- Not measured: the memory of a binding (an `Effect`, its flag, two lambdas, a slot in the signal's observers and in
  the context's set).
