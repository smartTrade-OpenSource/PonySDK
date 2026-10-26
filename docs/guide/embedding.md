# Embedding in another page: <pony-app>

An application runs inside a page it does not own: a portal, a client's web application, a desktop container
([ADR-0008](../decisions/0008-embedded-application.md)). The host page loads the application's terminal and writes
the element; each element runs its own terminal on its own context.

```html
<script type="module" src="https://fx.example.com/app/ponyterminal/terminal.js"></script>
<pony-app src="https://fx.example.com/app/" view="ticket" instrument="EURUSD">Loading…</pony-app>
```

| Attribute | Values | Effect |
|---|---|---|
| `src` | the application's URL | its query: the application's parameters; its fragment: the first history token |
| `isolation` | `shadow` (default), `none` | an open shadow root, or the host's document (for addons that search the document) |
| `history` | `memory` (default), `hash` | the token stays in the element, or the page's fragment is the application's |
| any other | | the application's, read through `PHost` |

The host and the application talk through attributes and events, in both directions:

```java
final PHost host = PHost.get();                                      // null when the application runs as a page
label.setText(host.getAttribute("instrument"));                      // attributes in, at the handshake
host.addAttributeListener("instrument", label::setText);             // then each change
host.addEventListener("reset", detail -> quantity.setText("1000000")); // the host's CustomEvents; each returns its HandlerRegistration
host.dispatchEvent("trade", Json.createObjectBuilder().add("side", "BUY").build()); // events out
```

```javascript
ticket.addEventListener('trade', e => blotter.add(e.detail));
ticket.setAttribute('instrument', 'USDJPY');
ticket.dispatchEvent(new CustomEvent('reset'));
```

- **Loading.** `terminal.js` starts a page terminal only in the application's own page (the early socket of the
  bootstrap page, a child window or frame, or a page under the application's path). Elsewhere it loads
  `chunks/pony-app.js` (4.9 KB gzip), which defines the element; an application's page loads it with the other modules
  on demand. The element reads `<application>/ponyapp.json` (stylesheets, scripts, shared web-component sheets,
  transparent reconnection, the legacy layer, its terminal), loads the scripts into the host page once, and puts the
  stylesheets in its shadow root.
- **Server.** `wsAllowedOrigins` lists the pages allowed to embed the application; `PonySDKServer` answers their CORS
  requests with credentials (`CrossOriginFilter`). Nothing else changes on the server:

  ```java
  configuration.setWsAllowedOrigins(Set.of("https://portal.example.com")); // the host pages' origins
  ```
- **Examples.** `./gradlew :sample:runShowcase` serves, besides the showcase, a gallery of every way to integrate on
  http://localhost:8082/ (alone in a page, a custom page, an iframe, an element of the same site or of another site,
  several applications on a page, windows and frames, two versions on a page), each with its code.
- **Another site** ([ADR-0009](../decisions/0009-embedding-limits.md)). No session cookie comes from a page of
  another site. The context of an embedded terminal has two keys, sent with `CREATE_CONTEXT`: `K` proves it in the
  `CONTEXT_KEY` header (AJAX, uploads) and, on a reconnection, in the WebSocket subprotocol `ponysdk.key.<K>`; `S`, in
  a URL, only fetches a pending stream. The host gives the element a credential with its property `credential`, offered
  as the subprotocol `ponysdk.credential.<base64url>`; the application reads it with `PHost.getCredential()`.
- **Stylesheets.** Loaded with CORS, the application's stylesheets are adapted once loaded: their `:root`, `html` and
  `body` selectors name the element (`:host`, the container), and in a shadow root their `@font-face` rules are
  declared to the document, which Chrome and Firefox require.
- **What stays the host's.** The page's fragment (unless `history="hash"`), its title and location (the element
  dispatches `pony-title` and `pony-location`: the host decides). A lost context or `RELOAD` restarts the element
  instead of reloading the page; the element dispatches `pony-connection-lost` and `pony-reconnected`. `PCookies` holds
  the cookies of the application's origin: read from the upgrade, written by a request the terminal makes there.
- **Windows and frames.** A `PWindow` or a `PFrame` of an embedded terminal runs a page of the application, which talks
  to the element's terminal by `postMessage` only, and accepts as its parent only a page of `wsAllowedOrigins`
  (the meta `pony-parent-origins` of the bootstrap page).
- **Versions.** The element defined by one PonySDK version hands an application of another (whose manifest names
  another terminal) to that terminal's `mountPonyApp(element, src)`; `ponysdk.js` is one per page, the first loaded,
  and the addon factories are the page's.
- **Leaving.** An element out of the page at the next task closes its socket with 4002: the server destroys the
  context (ADR-0005).
- **Limits.** In a shadow root, addons that search the document find nothing (`isolation="none"`). Each element has
  its own socket and context (about 80 KB of server heap). The application's scripts run in the host page's global
  scope.

The showcase's **Embedding** page embeds the ticket in the showcase itself. `ShowcaseMain` serves, on port 8082, another
origin with a gallery of the ways to integrate PonySDK (`sample/src/main/resources/portal`): alone in its page, a
custom page, an iframe, an element of the same site or of another site, in the page's document, windows and frames, two
versions on a page (`sample/tests/embedding.spec.js`, `sample/tests/integration-gallery.spec.js`).
