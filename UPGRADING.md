# Upgrading from 2.10 to 3.0

## Platform

- Java 25 to build and run (2.10: Java 17).
- JSON-P: `javax.json` becomes `jakarta.json` (API 2.1, Eclipse Parsson). Imports in application code change;
  `PEmulator.addonData` takes a `jakarta.json.JsonObject`.
- Jetty 12.1 (EE 11), Jakarta Servlet 6.1 and Spring 7, as in 2.10.
- `ponysdk` no longer brings `jetty-client`, and `ponysdk-spring` no longer brings `spring-web`: an application that
  uses them declares them.
- Browsers: current Chrome, Edge, Firefox, and Safari 17 or later. The terminal uses `<dialog>`, the popover API,
  `ResizeObserver` and ES2022, without polyfills.

## Removed APIs

| Removed | Use |
|---|---|
| `UIContext.getRequest()` (Jetty 12 recycles the request after the upgrade) | `getParameterMap`, `getHeader(s)`, `getRequestCookies`, `getHost`, `isSecure`, `getRemoteSocketAddress`, `getSession` |
| `SessionManager.getUIContext(int)` (a lookup across sessions) | `getUIContext(applicationId, id)` |
| `WebSocket.getRequest()` / `setRequest()` (the same recycled request) | `WebSocket.getUpgradeRequestData()`, the snapshot taken during the handshake |
| `ValueProvider`, `IdentityValueProvider` | `java.util.function.Function`, `Function.identity()` |
| `SimpleEventBus` | `EventBus` |
| `HasPClickHandlers`, `HasPKeyPressHandlers`, `HasPSelectionHandlers`, `PHasText` | the widgets' own methods (no widget implemented them) |
| `BootstrapServlet.setApplication` | `setConfiguration` |
| `Query.setCriteria` | `addCriteria` |
| `PValueBoxBase.setSelectionRange` | `select` |
| `PDatePicker.setMonth` | nothing: it did nothing |
| `BootstrapServlet.addHistoryIFrame` | nothing: history needs no frame |
| `PObject`'s protected fields `frame` and `saveKey` | `getFrame()` / `setFrame()`; `CompactIntRunnableMap.append` for one-off stacked instructions |
| `PCookies.setListener(CookiesListener)`, `CookiesListener` (one listener, never called when set late) | `addInitializedListener(Runnable)`: several, called at once when the cookies are known, removed by its `HandlerRegistration` |
| `PCookies.setCookie(name, value, expires, domain, path, secure, sameSite)` (path and domain in the other order than `setCookie(name, value, expires, path)`) | `setCookie(name, value, CookieOptions.expires(instant).withDomain(domain).withPath(path).secure().sameSite(SameSite.STRICT))` ([ADR-0012](docs/decisions/0012-api-surface.md)) |
| `new PCookies()` | `UIContext.getCookies()`, which creates it |

The `UIContext` constructors no longer take the upgrade request; `PSuite` builds its context accordingly.

`PSuite` moves from JUnit 4 to JUnit 6 (Jupiter): its setup runs as `@BeforeAll` / `@AfterAll`, and the `ponysdk`
artifact depends on `junit-jupiter-api` instead of `junit:junit`. Tests that extend `PSuite` run on the JUnit
Platform (`useJUnitPlatform()` in Gradle).

## The terminal

The GWT terminal is replaced by a TypeScript one ([ADR-0002](docs/decisions/0002-javascript-terminal.md)). The Java
API and the `window.pony` JavaScript API stay; the DOM changes.

- Widgets are native elements with ARIA roles and states. Each root element carries `pony-<Type>`
  (`pony-Button`, `pony-DialogBox`); state is read from native attributes and ARIA (`disabled`, `checked`, `open`,
  `aria-selected`, `aria-expanded`), not from dependent class names such as `gwt-MenuItem-selected`.
- Stylesheets written on `gwt-*` classes: ADR-0002, section 5, maps each widget to its elements. Meanwhile,
  `ApplicationConfiguration.setLegacyClassNames(true)` adds GWT's class names to the new DOM; it does not rebuild
  GWT's tables (the caption table of `DialogBox`, the nine cells of `DecoratorPanel`, the tables of `MenuBar`).
- Addons that walk into a widget's children may find another structure.
- `window.decode` and `window.UTF8` are gone from `ponysdk.js`.

## Bootstrap page

- `BootstrapServlet` loads `script/ponysdk.js`, then `ponyterminal/terminal.js` as an ES module
  (`type="module"`), which loads `ponyterminal/chunks/*.js` on demand
  ([ADR-0004](docs/decisions/0004-terminal-chunks.md)). A content security policy must allow module scripts from
  the application's origin.
- Its page opens the WebSocket in an inline script; a custom page without it connects as before.
- It sends `Cache-Control` and `Last-Modified` and answers conditional requests.
- `ponyapp.json` is the manifest of an embedded application ([ADR-0008](docs/decisions/0008-embedded-application.md)):
  a resource of the application by that name is no longer served.
- `BootstrapServlet` creates no HTTP session: the WebSocket upgrade does, and the browser keeps its cookie. An
  application that reads the session while its page loads (a filter, a servlet) creates it there
  ([ADR-0009](docs/decisions/0009-embedding-limits.md)).
- With `wsAllowedOrigins`, the page lists them in `<meta name="pony-parent-origins">`: the pages a window or a frame
  of an embedded application accepts as its parent.
- A page with a file name (`index.html`, a custom page under the application's path) connects to the socket at its
  directory; it used to build `…/index.htmlws`.
- `legacy.js` loaded with `data-pony-legacy="element"` does not start on the document; `window.ponyLegacy.observe(root)`
  decorates a root it is given.

## Behaviour changes

- The session cookie is `Secure` by default: a browser does not send it back over plain HTTP, so a server on plain
  HTTP calls `setSessionCookieSecure(false)` (Chrome and Firefox accept it on `http://localhost`, Safari does not).
  HTTPS is on by default as before (`setUseSSL`); without a key store the server now stops at start and says what to
  set, where it used to stop on a `NullPointerException`.
- A terminal posts the blocks of its windows and frames to its own origin, no longer to `*`: a window or a frame that
  navigated to another origin receives nothing.
- The addon factories are a table of the page (`window.ponyAddOnFactories`), shared by the terminals of the page's
  PonySDK versions.

- `setStyleName(name)` replaces all style names before attachment too (it used to add before attachment).
- `PHTMLTable.iterator()` returns the cell widgets (it was empty): the children of `PFlexTable` and `PGrid` are
  attached and destroyed with their table.
- `PTabLayoutPanel` and `PTabPanel` attach their children with the panel, as `PComplexPanel` does.
- `document.onPonyLoaded(callback)` called once `pony` is loaded runs the callback at once (it was never run).
- `PAddOn.destroy()` also unregisters the addon from its window: it no longer receives `onDestroy()` when that
  window closes. `addInitializeListener` after initialization does nothing (it was stored and never called).
- `PFocusWidget` registers click handlers as given; `END_OF_PROCESSING` is sent once per click.
- `PDatePicker` sends its dates as the day they fall on in its time zone (`setTimeZone`, UTC by default); a
  `PDateBox` sets its picker's zone to its format's.
- `DataGrid2`'s default column headers read "Pin / Unpin / Hide" (were "[pin me] / [hide me]").
- Transparent reconnection (`setReconnectionTimeoutMs`, off by default): a reconnection to a context the server no
  longer has reloads the page, and starts no application on the server for it.
- With transparent reconnection, a page left (closed, reloaded, navigated away) destroys its context at once instead
  of suspending it for the window ([ADR-0005](docs/decisions/0005-page-left-and-terminal-time.md),
  [ADR-0007](docs/decisions/0007-page-left-by-going-away.md)).
- A transaction that fails (a handler that throws) sends what it wrote before the failure.
- Liveness ([ADR-0006](docs/decisions/0006-liveness-by-ping.md)): the server counts only what it receives, and pings a
  client silent for half a heartbeat period; the browser answers outside the page's JavaScript. A client silent for
  two periods (10 s by default) is dropped as an abnormal close: suspended with transparent reconnection, destroyed
  without. The terminal no longer sends heartbeats. A proxy between browser and server must forward ping and pong
  frames, as TCP-level WebSocket proxies do.
- Latency: the probe goes every 5 s (or every heartbeat period when shorter); `getRoundtripLatency()` and the others
  average the last 10 probes received, not 10 slots counting the empty ones as 0. `getTerminalLatency()` is the time
  the probe waited in the terminal (it was always 0), and `getNetworkLatency()` the roundtrip minus that time.

## Protocol

The protocol version is negotiated at the WebSocket handshake: a terminal announcing an older version gets its
format, and `setMaxProtocolVersion` caps it for everyone. The server's keys keep the numbers of 2.10.
`PWidget.addEventListener` needs protocol 4: below it, the listener is kept, the terminal is not told, and a warning
is logged.
