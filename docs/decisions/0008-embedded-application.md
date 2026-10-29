# ADR-0008: An application embedded in another page, as the element `<pony-app>`

- Status: Accepted
- Date: 2026-10-25

## Context

A PonySDK application owns the page it runs in. The terminal:

- builds the WebSocket URL from the page's location (`terminal.ts:151`): the page must be served by the application;
- uses `document.body`, or an element of the document by id, as the root panel (`root-panel.ts:18-20`);
- keeps the history token in `location.hash` (`history.ts:31`);
- reloads the page when its context is lost (`reconnection.ts:21`);
- appends popups, suggestion lists, the date picker and the root layout panel to `document.body` (`popup.ts:143`,
  `popup.ts:171`, `suggest.ts:155`, `date-picker.ts:540`, `layout.ts:158`);
- reads and writes `document.cookie` for `PCookies` (`cookies.ts`);
- is the page's one `pony` object, which the addons of `ponysdk.js` call (`pony.sendDataToServer`,
  `pony.registerAddOnFactory`).

An existing system (a portal, a client's web application, a desktop container) can show a PonySDK application only in
an iframe: another document, so the host's stylesheets do not apply, popups are clipped at the frame's edge, and the
host sizes the frame by hand.

What exists: the server checks the Origin of the WebSocket upgrade against `wsAllowedOrigins`
(`WebSocketServlet.java:95`), and the upgrade creates the HTTP session when the request has none. The terminal and
its chunks are ES modules (ADR-0004): loaded from another origin, they are fetched with CORS. The session cookie has
no SameSite attribute, so browsers treat it as `Lax`: a host page of the same site (another port, another subdomain
of the same registrable domain) sends it, a page of another site does not.

## Decision

1. A host page loads the application's `terminal.js` as a module and writes the custom element `<pony-app src="…">`.
   The terminal starts a page terminal when the page opened the early socket (the bootstrap page), is a child window
   or frame (`UI_CONTEXT_ID` in its query), or is under the application's path (`new URL('..', import.meta.url)`, a
   custom page); a host page is none of these, and loads the element from `chunks/pony-app.js` at once. An
   application's page loads that chunk with the other modules on demand (ADR-0004), for an element it creates
   itself. One core module, which the chunks import.
2. Each element runs a terminal of its own: its socket goes to the base of `src`, with its own context and the query of
   `src` as parameters. A page may hold several elements, of one application or of several running the same PonySDK
   version. Changing `src` restarts the element.
3. Isolation, the attribute `isolation`:
   - `shadow` (the default): the element renders into an open shadow root holding the terminal's rules (one
     constructed stylesheet shared by the elements), the application's stylesheets as `<link>`, then a container that
     is the root panel's body. The host's styles and the application's do not meet.
   - `none`: the container is a child of the element in the host's document, and the application's stylesheets go to
     the document's head, once per URL. For an application whose addons search the document (jQuery selectors,
     `getElementById`); its styles and the host's then apply to both.

   Popups, suggestion lists, the date picker and the root layout panel go into the container, and still show in the
   top layer. A press outside a popup is read on `composedPath()[0]`, since at the document the target of an event from
   a shadow root is the element itself. Elements are looked up by id in the container's root node. The element's own
   children show until the first widget: the host's loading text.
4. `GET <application>/ponyapp.json` describes what the bootstrap page loads, from the same configuration: stylesheets,
   scripts (`ponysdk.js` first), shared web-component sheets, and whether transparent reconnection is on. The element
   loads the scripts into the host's document once per page, in order, as classic scripts, then hands `ponysdk.js` the
   page's addon registry. Addon factories are registered per page; each addon sends through its own terminal.
5. The host and the application talk through `PHost`, the element as the application sees it (`PHost.get()`, null in
   a page), in both directions:
   - attributes in: the element's attributes but those of this ADR (`src`, `isolation`, `history`), read at the
     handshake (`HOST_ATTRIBUTES`, a JSON object) before `EntryPoint.start`, then each change: `getAttribute(name)`,
     `getAttributes()`, `addAttributeListener(name, listener)`, the listener receiving null when the attribute is
     removed;
   - events in: `addEventListener(type, listener)` receives the detail of each `CustomEvent` of that type the host
     dispatches on the element; the events the application dispatches are not delivered back to it;
   - events out: `dispatchEvent(type, detail)` dispatches a `CustomEvent` on the element, bubbling and composed.

   Protocol: the object type `HOST`, appended to `WidgetType`; one server key, `DISPATCH_EVENT` (an array: type,
   detail as JSON), 8 left free of 256; one client key, `HOST_ATTRIBUTES` (`y`). Events in use the keys of
   `PWebComponent` (`WC_LISTEN_EVENT`, `WC_UNLISTEN_EVENT`, `WC_EVENT_NAME`, `WC_EVENT_DETAIL`), with the same meaning.
   Only an embedded terminal receives these, and it is always of this version: no protocol version.
6. What an embedded application leaves to its host:
   - the history, the attribute `history`: `memory` (the default) keeps the token in the element, its first token the
     fragment of `src`; `hash` gives the application the page's fragment, for a host page that is this application's
     frame (one such element per page);
   - the page: a lost context, or `RELOAD`, restarts the element instead of reloading the page; the window title and
     `WINDOW_LOCATION_REPLACE` are ignored with a warning;
   - the page hooks: the reconnection notice (`#reconnection`) is looked up inside the element, and the element
     dispatches `pony-connection-lost` and `pony-reconnected` instead of calling `onConnectionLost` and
     `onPonyReconnected`;
   - the cookies: the host's `document.cookie` is not the application's, so `PCookies` receives none and its writes
     are ignored with a warning; the server reads the application's cookies from the handshake
     (`UIContext.getRequestCookies`);
   - child terminals: a `PWindow` is refused with a warning, and a `PFrame` shows its page without a child terminal:
     a child page, on the application's origin, reaches its parent's terminal through `window.opener` or
     `window.parent`, which another origin cannot read;
   - the legacy class names (`setLegacyClassNames`) are not applied.
7. An element removed from the document and still out of it at the next task (a move puts it back at once) closes its
   socket with 4002: the server destroys the context (ADR-0005). `pagehide` closes every terminal of the page, as for a
   page.
8. `wsAllowedOrigins` lists the origins allowed to embed the application; without it, none (same origin only, as
   today). `PonySDKServer` answers CORS with credentials for these origins, through a servlet filter
   (`CrossOriginFilter`): the module scripts, the manifest, the reconnection ping and the addons' AJAX requests need
   it. A preflight gets the headers it asks for: an addon's AJAX request carries its data as headers whose names only
   the addon knows. Jetty's `CrossOriginHandler` answers a fixed list of headers, and refuses the WebSocket upgrade of
   an origin it does not list, the application's own page included.
9. Same site only: the session is the cookie's. A host of another site needs a token at the handshake instead of the
   cookie: a decision of its own.

## Options considered

- The iframe, as today: the costs of the context above. Rejected.
- Shadow root only: core_ui's 73 addons are jQuery plugins, many search the document. Rejected for the `isolation`
  attribute.
- A second entry, `pony-app.js`, over a core split out of `terminal.js`: one more round trip before the first widget
  of every page (ADR-0004), or a `modulepreload` that each custom page must add. Rejected.
- Properties in, besides attributes: a property must be declared on the element to be seen, and an attribute carries
  a JSON object as well. Rejected.
- `PHost` as a `PWebComponent`: a widget of the tree, attached and detached with a parent, when the element is the
  tree's container. Rejected.

## Consequences

- A page runs as before: the same handshake, no message more, nothing on the message path. Measured on the
  implementation: `terminal.js` 89 074 → 91 826 bytes, 27 024 → 27 912 gzip (+888 bytes, the mount of a terminal and
  the shared addon registry); `chunks/pony-app.js` 6 982 bytes, 2 811 gzip, loaded when the browser is idle.
- In a shadow root, addons that search the document (`getElementById`, jQuery selectors from `document`) do not find
  what is inside: such an application uses `isolation="none"`. The drop-down addon of `ponysdk.js`
  (`document.getElementById`, `ponysdk.js:981`) is changed to search its root node.
- A stylesheet written for a page matches nothing inside the shadow root with its `:root`, `html` and `body` rules:
  an application to be embedded adds `:host` to them (the showcase's stylesheet has 8 such rules).
- `PScript` and the application's scripts run in the host page's global scope, as they run in the bootstrap page's.
- Fonts: an `@font-face` declared inside a shadow root is ignored by Chrome 148 and Firefox 157, applied by WebKit 27.2
  (measured on 2026-10-25, a system font as a data URL): the host page declares the application's fonts.
- `document.onPonyLoaded` called once `pony` is loaded runs its callback at once, for the application scripts the
  element loads after the page's (it was never run).
- Two PonySDK versions on one page share the globals of `ponysdk.js`: not supported.
- The sample gets a host page on another port, with two embedded tickets and the showcase, and an E2E test on the three
  browsers.
