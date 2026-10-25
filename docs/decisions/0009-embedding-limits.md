# ADR-0009: The limits of an embedded application, lifted

- Status: Accepted
- Date: 2026-10-25

## Context

ADR-0008 embeds a PonySDK application in another page with `<pony-app>`, and leaves ten limits (L1 to L10) and one
defect (F3). Measured on 2026-10-25 with a host page on another site (`http://127.0.0.1:8082`) embedding the showcase
(`http://[::1]:8081/sample/`), on Chrome 148, Firefox 157 and WebKit 27.2:

| | Chrome | Firefox | WebKit |
|---|---|---|---|
| Rendering, attributes, events | yes | yes | yes |
| Session cookie stored | no | yes, partitioned | no |
| Upload (`PFileUpload`) | 404 | 200 | 404 |
| Lost connection | new context, state lost | context resumed | new context, state lost |

The same page on the same site passes everything on the three browsers.

- L1, another site. The session cookie has no SameSite attribute, so it is `Lax`: Chrome and WebKit neither store nor
  send it from another site. The upgrade then creates a session per element, and the HTTP requests that find a context
  through the caller's session fail: streams (`StreamServiceServlet.java:64`), the addons' AJAX (`AjaxServlet.java:66`),
  the reconnection (`WebSocket.onWebSocketOpen`, through the session's `Application`). Nor does the application know
  who the user is: the session the user logged in with is not there.
- F3. Every request `BootstrapServlet` answers creates an HTTP session when it has none (`BootstrapServlet.java:157`):
  the page, `terminal.js`, `ponysdk.js`, the stylesheets, `ponyapp.json` (measured with curl: one `Set-Cookie` each).
  Jetty 12.1.14 never expires a session by default (`AbstractSessionManager._dftMaxIdleSecs = -1`), and only the last
  context of an `Application` invalidates its session (`Application.deregisterUIContext`). So each session of a client
  that does not send the cookie back stays: from another site, every resource of every element; from any client, a
  health check of the page.
- L2. An `@font-face` declared in a shadow root is ignored by Chrome 148 and Firefox 157, applied by WebKit 27.2.
- L3. The window title and `WINDOW_LOCATION_REPLACE` are dropped with a warning: the host never knows.
- L4. `PWindow` is refused and `PFrame` runs no child terminal: a child page reaches its parent's terminal through
  `window.opener.pony` or `window.parent.pony`, which another origin cannot read.
- L5. `PCookies` is empty and its writes are dropped: `document.cookie` is the host page's.
- L6. The legacy class names (`setLegacyClassNames`) are not applied: `legacy.js` watches `document.documentElement`,
  which a shadow root is not part of, and decorates the host page's own elements.
- L7. In a shadow root, addons that search the document (`document.getElementById`, jQuery from `document`) find
  nothing of the application.
- L8. Two PonySDK versions on one page: the first terminal loaded defines `<pony-app>` and runs every application, and
  the globals of `ponysdk.js` are the first loaded.
- L9. Each element has its own socket and its own context. Measured: 100 embedded tickets add about 8 MB to the
  server's heap after GC (52 to 60 MB, back to 52 MB when they close), about 80 KB per context; Chrome 148 holds 103
  sockets open to one host.
- L10. A page's stylesheet matches nothing inside the shadow root with its `:root`, `html` and `body` rules: the
  showcase added `:host` to 3 of them by hand.

## Decision

### 1. The context key (L1)

An embedded terminal's context has a key: two random 128-bit values, `K` and `S`, base64url, made when the context is
created from a handshake that carries `HOST_ATTRIBUTES`, and sent with `CREATE_CONTEXT` (server key `CONTEXT_KEYS`,
`K.S`; 7 keys left free of 256). A page's context has none: nothing changes for it.

- `K` proves the context where a header can carry it: the header `CONTEXT_KEY` of the addons' AJAX requests and of the
  uploads, and the reconnection, in the WebSocket subprotocols the terminal offers (`ponysdk`, `ponysdk.key.<K>`).
  The server answers with `ponysdk`.
- `S` proves the context for a stream fetched by its URL, which takes no header: an `<img>`, the download frame. A
  query parameter. It lets one fetch a pending stream, and nothing else: no AJAX, no upload, no reconnection.
- So no secret that grants more than a pending stream is ever in a URL, where access logs and the `Referer` keep it.
- The servlets and the reconnection look the context up by the caller's session first, as today, then by its key:
  `SessionManager` keeps the contexts that have one by id, until destroyed, and compares keys with
  `MessageDigest.isEqual`. An embedded terminal of the same site sends its key too, and its cookie still gives the
  session: the user's, when the application's pages are open as well.
- A reconnection by key needs no session: the upgrade creates none (each would be left without a context).

Rejected:

- The session id in the URLs (`;jsessionid=`, the servlet URL tracking mode): the whole session readable by every
  script of the host page, and written in the access logs.
- A cookie `SameSite=None; Secure; Partitioned`: HTTPS only, WebKit stored no cookie of another site in the
  measurement, and it does not say who the user is.
- The key in the first message after the socket opens: the server starts the application at the upgrade
  (`WebSocket.onWebSocketOpen`); waiting for a message would be a state the protocol does not have.

### 2. The credential (L1)

The host page gives the element a credential, an opaque string (a JWT, an OAuth access token): the property
`credential`, a function returning the string or a promise of it. The element calls it before each handshake that
creates a context (start, restart), and offers it as the subprotocol `ponysdk.credential.<base64url of its UTF-8>`.
The application reads it with `PHost.getCredential()` in `EntryPoint.start`, and validates it as it sees fit. A
credential to renew while the application runs goes as an event in (ADR-0008).

A property, not an attribute: attributes are in the DOM, which every script of the page reads and which session-replay
tools record. Not in the URL: the access logs. Its size is bounded by the server's request headers (8 KiB with Jetty's
defaults).

### 3. Only the upgrade creates a session (F3)

`BootstrapServlet` creates none. The WebSocket upgrade already creates it (`WebSocketServlet.captureUpgradeData`), and
the browsers keep the cookie of its response for the requests that follow (streams, AJAX): checked on the three
browsers by the E2E tests.

### 4. The application's stylesheets (L2, L10)

The element loads them with `crossorigin="use-credentials"`, so their rules are readable (the CORS of ADR-0008), and
once each is loaded:

- its `@font-face` rules go to the document, in one constructed stylesheet that the page adopts, with their URLs made
  absolute on the stylesheet's, once per stylesheet URL;
- the selectors that name the page are rewritten in place, in every nested rule: in a shadow root `:root` and `html`
  become `:host` (`:host(<compound>)` when they carry one), `body` becomes `[data-pony-body]`; with `isolation="none"`,
  `pony-app` and `pony-app>[data-pony-body]`, so that they leave the host page alone. A selector that cannot be written
  so (`:root:has(…)`, which `:host()` does not take) stays as it is, with a warning.

The showcase's stylesheet drops its 3 `:host` additions: its page rules run embedded as written. It keeps the 3 rules
written for the element (`:host #reconnection`, the shell's height).

### 5. Title and location (L3)

`WINDOW_TITLE` dispatches `pony-title` on the element (detail: the title), `WINDOW_LOCATION_REPLACE` dispatches
`pony-location` (detail: the URL, absolute on the application's), bubbling and composed. The host decides.

### 6. Windows and frames of another origin (L4)

A child terminal whose parent is on another origin talks to it by `postMessage` only. The same origin keeps its direct
calls.

- The parent posts to the application's origin, never to `*`: a page terminal too, so that a window that navigated
  elsewhere receives nothing.
- A child page accepts as its parent its own origin, or an origin of `wsAllowedOrigins`, which `BootstrapServlet`
  writes in the page when they are set. Any other page could open the child page and feed it HTML.
- Child to parent: `ready`, `send` (a message for the server), `closing`. Parent to child: `init` (`K`, `S`, the
  window's name), the blocks as today, `entries` (dictionary entries), `window` (print, reload, resize, move, title:
  the operations a cross-origin `WindowProxy` refuses run in the child).
- The string dictionary: a child of another origin cannot call `dictionaryEntry` synchronously. The parent sends its
  entries with `init`, then, before each block it forwards, those added since, which its reader journals while such a
  child exists.

### 7. Cookies (L5)

An embedded application's cookies are those of its origin. `PCookies` starts from the upgrade request's cookies, the
session cookie aside; a write is a stream request of its own kind (`HANDLER_COOKIE_REQUEST`) that the terminal fetches
with credentials, answered with `Set-Cookie` as the server built it: the terminal sends nothing the server did not
decide. On another site, the browser's rules for third-party cookies apply (Chrome and WebKit: none).

### 8. Legacy class names (L6)

`legacy.js` watches a root it is given: the document in the application's page, as today; the element's container when
the manifest says `legacy: true`, the element loading `legacy.js` once per page without letting it start on the host's
document. The terminal reads the legacy flag from its mount, not from `<html data-pony-legacy>`.

### 9. Addons that search the document (L7): not lifted

Lifting it would change `document.getElementById` and `querySelector` for the whole host page. `isolation="none"` is
the answer, as in ADR-0008.

### 10. Two versions on one page (L8)

`ponyapp.json` gives the application's PonySDK version and its terminal. An element whose application is of another
version imports that application's terminal and hands it the element: the module exports
`mountPonyApp(element, src)`, which returns `{ stop() }`; the manifest's `mount: 1` names this contract, which stays
frozen. `ponysdk.js` is one per page, the first loaded (its globals are the addons' API): a version that differs is
logged.

### 11. A context per element (L9): kept

One context per element keeps the applications apart: one that fails or is slow does not hold the others, and one
host's attributes and events are its own. The cost is measured above; a context shared by several elements would be
another programming model (several roots for one `EntryPoint`), for a host page that holds hundreds of them.

### 12. The sample shows every way to integrate

The portal (`ShowcaseMain`, port 8082) becomes a gallery, one page per case, each with its E2E test:

- U1 the application alone in its page (the showcase);
- U2 a custom page of the application, under its path;
- U3 an iframe on another origin;
- U4 `<pony-app>` on the same site: two tickets and the showcase (the current portal);
- U5 `isolation="none"`;
- U6 another site, with a credential;
- U7 several applications in a page, and an application embedded in another (the showcase's Embedding page);
- U8 a ticket popped out in its own window, and a frame, from an embedded application;
- U9 two versions on one page.

## Consequences

- Protocol, embedded terminals only (no version, ADR-0008): server key `CONTEXT_KEYS`; client keys `CONTEXT_KEY`
  (a header) and `STREAM_KEY` (a parameter); `HandlerModel.HANDLER_COOKIE_REQUEST`; the subprotocols `ponysdk`,
  `ponysdk.key.*`, `ponysdk.credential.*`.
- A page runs as before: no key, no subprotocol, no journal.
- The keys are readable by every script of the host page, as is everything its terminal holds: a host page is trusted
  by the application that lists it in `wsAllowedOrigins`.
