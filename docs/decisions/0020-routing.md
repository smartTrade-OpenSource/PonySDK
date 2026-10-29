# ADR-0020: Routes by annotation, on the browser history

- Status: Accepted
- Date: 2026-10-29

## Context

`uiContext.getHistory()` keeps the history token, the URL fragment; a reload and a server move give it back
([ADR-0017](0017-drain.md)). What an application does with it is its own code: a `PValueChangeHandler` with a switch
on the token, written again for each application (the showcase's `UISampleEntryPoint` does that for its tabs). The
GWT-style `com.ponysdk.core.ui.place` and `activity` packages map a token to a screen, with a `Place` class, a
tokenizer and an activity mapper to write and register for each screen. Neither reads a parameter from a path, asks a
screen before leaving it, or keeps a user out of a screen.

A routing comparable to Vaadin's `@Route` is what an application of several screens reaches for: the path says the
screen, the parameters are read typed, a deep link opens the screen, back and forward work, a form with unsaved
changes holds the user, a screen is closed to some users.

## Decision

1. `@Route("orders/:id")` on a view class that implements `IsPWidget`; the constructor takes the `RouteParameters` or
   nothing. A path is segments, literal or `:name`; at the same position a literal beats a parameter, and the leftmost
   position decides (`orders/new` over `orders/:id`). A view can have several paths (`@Route({"orders", ""})`): the
   first is the one `navigate(View.class)` writes. No new base class, no interface of its own: the `IsPWidget` of the
   GWT-style code is the whole contract.
2. `Router.builder(panel).views(A.class, B.class)...start()`: the application lists its views, nothing is scanned.
   The router shows the view of the current token at the start (deep link, reload), on `PHistory` events (back,
   forward, a link `#orders/42`), on `navigate(View.class, params)` and on `navigate("orders/42?status=open")`. A
   `base("routing")` makes it own the tokens under `routing/` only: it can live in a page of an application that uses
   the rest of the token for something else; started on a token outside its base it goes to its root. It stops with
   its panel (the panel's destroy removes its history handler).
3. It writes the token with `newItem(token, false)`: no event comes back from the terminal. The token is written once
   the view is shown, and not at all when it is the token already there (the start of a deep link writes nothing).
4. A path parameter and a query parameter are read from `RouteParameters`: `get`, `getInt`, `getLong`,
   `getBoolean`, each with a fallback form for an absent or malformed value. Values are percent-decoded; a path
   parameter hides a query parameter of the same name. `navigate(View.class, Map)` fills the path with the
   parameters it names and writes the others, sorted, in the query. The view reaches its router with
   `RouteParameters.router()`.
5. Before entering: `Router.Builder.beforeEnter(BeforeEnter)`, called for each navigation to a matched view with a
   `Navigation`: doing nothing lets it pass, `redirect(path)` sends it elsewhere (through the hook again, eight at
   most, then an `IllegalStateException`), `deny()` shows the denied view.
6. Access: `Builder.access(View.class, predicate)`, a `Predicate<UIContext>` asked after the hook and before the view
   is built, each time. The roles are the application's (an attribute of the `UIContext`, a user object, anything the
   predicate reads); the router has no notion of role and no dependency on a security framework. A denied navigation
   shows the denied view (the application's with `denied(Class)`, a one-line default otherwise), under the token that
   was asked: a deep link to a closed route shows it too.
7. Before leaving: a view that implements `BeforeLeave` is asked before the router leaves it. `Navigation.postpone()`
   holds the navigation and gives a `Postponed`; the application calls `resume()` (the view is left, not asked
   again) or `cancel()` from a later event. When the navigation came from the browser (back, forward) the token is
   written back to the view's, so that the address bar does not lie. Another navigation replaces a postponed one.
8. A path that matches nothing shows the not-found view (`notFound(Class)`, a one-line default otherwise), built
   with the token in its `RouteParameters`.
9. A view is built when entered and its widget destroyed (`onDestroy`) when left: a scheduler or a listener stopped
   by its destroy listener stops, as the showcase does for its tabs. A view whose construction throws leaves the
   screen and the token as they were.
10. `com.ponysdk.core.ui.place`, `activity` and `scene` are not touched.

## Options considered

- Scanning the classpath for `@Route` classes (Vaadin, Spring): a scan at each start for a list the application
  can write in one line, and a dependency on the scanning library. Rejected; the explicit list also fails at
  `start()` for a class without `@Route`, a duplicated path or an access rule for a view that is not listed.
- A view as a widget base class (Vaadin's `Component`): forces the hierarchy of the application's screens. A class
  that implements `IsPWidget` can be a panel, hold one, or wrap a third-party widget.
- One interface with a `build(params)` method, beside the annotation: two things to write for each view, and the
  constructor already is that method.
- A static `Router.current()` for the views to navigate: ambiguous as soon as two routers exist (the showcase's
  has a base; an application can have one per tab). The router is in the parameters.
- `newItem(token, true)` to write and to follow: the terminal sends the token back and the server handles it a
  second time, a message more for each navigation.
- A denied navigation that stays on the current view: there is none at a deep link, and a user who follows a
  link must be told. The denied view is the same answer everywhere.
- Hooks on the router for the leave, as for the enter: the view knows its unsaved changes; a router-level hook would
  have to ask it.
- Keeping the view alive between visits (Vaadin's `@PreserveOnRefresh`): a view built each time cannot hold a
  scheduler or a model of a user who left. The application that wants state keeps it in its own object.

## Consequences

- The bench (`./gradlew :ponysdk:bench -Pbench.args="v4 route 10"`, 50 000 navigations between two views of 5 and 6
  rows, after warm-up) gives, against the same swap written by hand (`setWidget`, `onDestroy`, `newItem`):

  | per navigation | router | by hand |
  |---|---|---|
  | protocol bytes | 856.7 | 856.7 |
  | wire bytes (deflate) | 261.5 | 261.7 |
  | CPU | 7.4 to 7.9 µs | 6.7 to 6.8 µs |
  | allocated | 12.2 KB | 10.9 KB |

  The router adds no frame: the token, the swap and the destroys are what the hand-written swap sends. It adds about
  1 µs and 1.2 KB, the matching of the token and the construction of the view by reflection. A run of 5 000
  navigations without a longer warm-up shows the cold cost, 18 µs against 5 µs.
- The terminal is not changed: `terminal.js` and the protocol are what they were.
- A back or forward that a view postpones cannot be cancelled in the browser: the token is written back, which
  adds an entry to the browser's history.
- A new view is built for each navigation, including from `orders/1` to `orders/2` and from `orders` to
  `orders?status=open`.
- Leaving the page (closing the tab, a reload) does not ask the view: `beforeunload` is not wired.
- `RouterTest` and its companions cover the package at 100 % of its lines (289) and branches (148): matching, the
  precedence of literals, parameters and their decoding, the query, the start on a token (deep link, reload, outside
  the base), `navigate`, back and forward, the hook (pass, redirect, deny, a loop), access, not-found, postpone with
  resume, cancel and replacement, a back that is postponed, the destroy of a left view, a view that fails to build, a
  router that stops with its panel.
- The showcase's Routing page is a small application under the token `routing`; `UISampleEntryPoint` takes the first
  segment of the token as the tab's slug (`routing/orders/42` opens the Routing tab). `sample/tests/routing.spec.js`
  drives it in the three browsers.
