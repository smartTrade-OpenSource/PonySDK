# Upgrading from 2.10 to 3.0

## Platform

- Java 25 to build and run (2.10: Java 17).
- JSON-P: `javax.json` becomes `jakarta.json` (API 2.1, Eclipse Parsson). Imports in application code change;
  `PEmulator.addonData` takes a `jakarta.json.JsonObject`.
- Jetty 12.1 (EE 11), Jakarta Servlet 6.1 and Spring 7, as in 2.10.
- Browsers: current Chrome, Edge, Firefox, and Safari 17 or later. The terminal uses `<dialog>`, the popover API,
  `ResizeObserver` and ES2022, without polyfills.

## Removed APIs

| Removed | Use |
|---|---|
| `UIContext.getRequest()` (Jetty 12 recycles the request after the upgrade) | `getParameterMap`, `getHeader(s)`, `getRequestCookies`, `getHost`, `isSecure`, `getRemoteSocketAddress`, `getSession` |
| `SessionManager.getUIContext(int)` (a lookup across sessions) | `getUIContext(applicationId, id)` |
| `ValueProvider`, `IdentityValueProvider` | `java.util.function.Function`, `Function.identity()` |
| `SimpleEventBus` | `EventBus` |
| `HasPClickHandlers`, `HasPKeyPressHandlers`, `HasPSelectionHandlers`, `PHasText` | the widgets' own methods (no widget implemented them) |
| `BootstrapServlet.setApplication` | `setConfiguration` |
| `Query.setCriteria` | `addCriteria` |
| `PValueBoxBase.setSelectionRange` | `select` |
| `PDatePicker.setMonth` | nothing: it did nothing |
| `BootstrapServlet.addHistoryIFrame` | nothing: history needs no frame |
| `PObject`'s protected fields `frame` and `saveKey` | `getFrame()` / `setFrame()`; `CompactIntRunnableMap.append` for one-off stacked instructions |

The `UIContext` constructors no longer take the upgrade request; `PSuite` builds its context accordingly.

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

## Behaviour changes

- `setStyleName(name)` replaces all style names before attachment too (it used to add before attachment).
- `PHTMLTable.iterator()` returns the cell widgets (it was empty): the children of `PFlexTable` and `PGrid` are
  attached and destroyed with their table.
- `PTabLayoutPanel` and `PTabPanel` attach their children with the panel, as `PComplexPanel` does.
- `PAddOn.destroy()` also unregisters the addon from its window: it no longer receives `onDestroy()` when that
  window closes. `addInitializeListener` after initialization does nothing (it was stored and never called).
- `PFocusWidget` registers click handlers as given; `END_OF_PROCESSING` is sent once per click.
- `PDatePicker` sends its dates as the day they fall on in its time zone (`setTimeZone`, UTC by default); a
  `PDateBox` sets its picker's zone to its format's.
- `DataGrid2`'s default column headers read "Pin / Unpin / Hide" (were "[pin me] / [hide me]").
- Transparent reconnection (`setReconnectionTimeoutMs`, off by default): a reconnection to a context the server no
  longer has reloads the page, and starts no application on the server for it.

## Protocol

The protocol version is negotiated at the WebSocket handshake: a terminal announcing an older version gets its
format, and `setMaxProtocolVersion` caps it for everyone. The server's keys keep the numbers of 2.10.
`PWidget.addEventListener` needs protocol 4: below it, the listener is kept, the terminal is not told, and a warning
is logged.
