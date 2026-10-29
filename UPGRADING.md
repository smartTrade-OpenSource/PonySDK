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

3.0 has no deprecated API: what 2.x deprecated is gone, and of the packages that existed twice one is left. An
application that used a removed package can keep a copy of it in its own sources: they depend on the public API only.

| Removed | Use |
|---|---|
| Package `com.ponysdk.core.ui.datagrid` (`DataGrid`, `DynamicDataGrid`, `ColumnDescriptor`...) | `com.ponysdk.core.ui.datagrid2` |
| Package `com.ponysdk.core.ui.eventbus2`, `UIContext.getNewEventBus()` | `com.ponysdk.core.ui.eventbus.EventBus`, `UIContext.getRootEventBus()` ([EventBus](docs/guide/event-bus.md)) |
| Package `com.ponysdk.core.ui.form2` | `com.ponysdk.core.ui.form`: `AbstractFormField` and its fields, `FieldValidator`s |
| `Focusable`, `setFocus(boolean)` of `PFocusWidget`, `PDateBox`, `PSuggestBox` | `focus()`, `blur()` |
| `PWidget.isFocused()` (the server's guess, never told what the browser did) | `addFocusHandler`, `addBlurHandler` of a focus widget; `addDomHandler(handler, PFocusEvent.TYPE)` on another |
| `PPopupPanel.isShowing()` | `isVisible()` |
| `Event.getEventID()` | nothing: it returned 0 |
| `HasPEvent` | nothing: no class implemented it |
| `com.ponysdk.core.ui.basic.DataListener` | `com.ponysdk.core.server.application.DataListener`, which it only extended |
| `new FlexLayoutPanel(String modelJson, String theme[, String bordersJson])` | `new FlexLayoutPanel(FlexLayoutModel, FlexTheme, FlexBorder...)`; a saved model: `loadModel(json)` |
| `FlexLayoutPanel.setKeymap(String)`, `getOpenTabsRaw(Consumer<String>)`, `setBlockedActions(String...)` | `setKeymap(FlexKeymap)`, `getOpenTabs(Consumer<List<FlexTabInfo>>)`, `setBlockedActions(FlexAction...)` |
| `com.ponysdk.spring.ListLoader` (`ponysdk-spring`) | nothing: it was deprecated as useless |
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
| `PScript.execute(window, js, ...)`: JavaScript source, run with eval | `PScript.call(window, functionName, args...)`, with a callback and a delay as before ([ADR-0015](docs/decisions/0015-content-security-policy.md)) |
| `PObject.evalTerminalScript(js)` | `callTerminalFunction(functionName, args...)`: the function receives the element, then the args |
| `new TextFunction(java, js)`, `getJsFunction()` | `TextFunction.named(java, functionName)`: the function receives the args array |

The functions those three call are functions of `window`, defined by the application's scripts: no JavaScript source
travels, and the page's policy forbids eval.

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
- Its page is served with a Content-Security-Policy and `X-Content-Type-Options: nosniff`
  ([ADR-0015](docs/decisions/0015-content-security-policy.md), [security](docs/guide/security.md)). What to check:
  - scripts from another origin than the page, not in `setJavascript` (a script that loads another): add the origin
    with `addScriptSources`;
  - inline scripts added to the page by overriding `addScript`: allow them in an override of
    `buildContentSecurityPolicy`, or move them to a file;
  - a site that shows the application in a frame: list its origin in `setWsAllowedOrigins`;
  - HTML sent to the terminal (`PHTML`, `setInnerHTML`, `setAttribute`) with `onclick=` attributes or `javascript:`
    URLs: the policy blocks them; listen with handlers instead;
  - scripts of the application that use `eval` or `new Function` (a library that compiles templates): the policy
    blocks them; replace them, or send a policy of your own;
  - an application with its own policy: `setContentSecurityPolicyEnabled(false)`.
- The reconnection flag and the web components' shared sheets are `<meta>` elements (`pony-reconnection`,
  `pony-wc-shared-sheets`) instead of inline scripts.
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

- From the classpath, the server serves web files only (`html`, `js`, `mjs`, `css`, `map`, images, fonts). 2.x served
  any file of the classpath, classes and configuration included. An application that serves another type from its
  jars adds it: `configuration.addServedResourceExtensions("json")`.
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
- After `onClientData`, the context calls `PObject.onClientDataProcessed`, even when `onClientData` throws: what
  answers the terminal for its message goes there. A focus widget's `END_OF_PROCESSING` is sent from it, so a click
  filtered out by an override (`PTextBoxBase` on a widget hidden meanwhile) still enables the widget again. A test
  that sends a click by calling `onClientData` itself gets no `END_OF_PROCESSING`: it calls
  `UIContext.fireClientData`, the path of the terminal's messages.
- A focus widget disabled by a click (the default, `setEnabledOnRequest(false)`) is enabled again when its context
  resumes after a lost connection: a click made while the connection was down never reached the server.
- `debugMode` is a development mode: each widget's creation carries its Java class and the line that created it,
  found by walking the stack, and the terminal loads its inspector ([development tools](docs/guide/development.md)).
  Leave it off in production. It used to only send `ensureDebugId`'s ids, which it still does.
- `WebComponentGenerator` ends a usage error with status 1 (it was 2): it throws instead of exiting the JVM.
- A path the server does not have is logged at DEBUG ("Resource not found"), and a send that fails because the
  connection is closing too; they were ERRORs. A send that fails on an open connection stays an ERROR.
- Latency: the probe goes every 5 s (or every heartbeat period when shorter); `getRoundtripLatency()` and the others
  average the last 10 probes received, not 10 slots counting the empty ones as 0. `getTerminalLatency()` is the time
  the probe waited in the terminal (it was always 0), and `getNetworkLatency()` the roundtrip minus that time.

## Protocol

The protocol version is negotiated at the WebSocket handshake: a terminal announcing an older version gets its
format, and `setMaxProtocolVersion` caps it for everyone. The server's keys keep the numbers of 2.10.
`PWidget.addEventListener` needs protocol 4: below it, it throws `IllegalStateException`
([ADR-0011](docs/decisions/0011-one-way-to-listen.md)). Protocol 4 also has `EVENT_HANDLED`, the answer to an event
listened to with `PEventOptions.lock()`, and `DEBUG_INFO`, sent in debug mode only
([ADR-0016](docs/decisions/0016-development-tools.md)).
