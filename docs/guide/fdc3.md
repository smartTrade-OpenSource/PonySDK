# FDC3

The applications of a trader's desktop (OpenFin, interop.io, Connectifi…) share the instrument in view and hand each
other work through FDC3, the FINOS standard. A PonySDK screen takes part from Java: `Fdc3` drives the desktop agent
the desktop puts in the page, `window.fdc3`. Why it is built so: [ADR-0014](../decisions/0014-fdc3.md).

List the addon's script with the application's, after `ponysdk.js`:

```java
configuration.setJavascript(new LinkedHashSet<>(List.of("script/fdc3-addon.js")));
```

Then, on the UI thread:

```java
final Fdc3 fdc3 = Fdc3.get(); // one per context

// Follow the instrument the channel brings
fdc3.addContextListener("fdc3.instrument", instrument -> chart.show(instrument.getJsonObject("id").getString("ticker")));

// Share the one in view
row.addClickHandler(e -> fdc3.broadcast(Fdc3.instrument("AAPL", "Apple")));

// Do work for the others: the handler's value is the intent's result
fdc3.addIntentListener("ViewChart", context -> {
    chart.show(context);
    return JsonValue.NULL;
});

// Hand work to another application
fdc3.raiseIntent("ViewNews", Fdc3.instrument("AAPL", null), result -> status.setText(
    result.failed() ? "No application: " + result.error() : "Opened in " + result.app()));
```

- Contexts are `JsonObject`s with a `type`, as the standard defines them; `Fdc3.instrument` builds the most common one.
- Listeners are called on the context's UI thread. Each returns its `HandlerRegistration`: the agent stops sending a
  type once its last listener is removed. One handler per intent.
- Channels: `getUserChannels()`, `joinUserChannel(id)`, `leaveCurrentChannel()`, `getCurrentChannel()`,
  `addChannelListener`. Joining a channel delivers its current context to the listeners.
- The agent is found as the standard says: `window.fdc3`, or its `fdc3Ready` event. `getState()` is `WAITING`, then
  `READY` (with `getInfo()`, the agent's metadata) or `UNAVAILABLE` after 5 seconds without one; `addStateListener`
  is called at once with the current state. A call made before the agent is ready waits for it.

## The sample

A browser alone has no desktop agent. The sample brings a demonstration one, `script/fdc3-agent.js`, which links the
pages of one browser; a real desktop brings its own, and the sample's sets nothing where one is already there. The
showcase's FDC3 page broadcasts instruments, follows its channel and handles `ViewInstrument`; `fdc3-monitor.html`, a
plain JavaScript application, does the same from the other side. Open both, or the FDC3 page in two windows.
