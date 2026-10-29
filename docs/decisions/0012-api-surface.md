# ADR-0012: The public API reduced to what an application calls, and PCookies made consistent

- Status: Accepted
- Date: 2026-10-26

## Context

The audit of 2026-10-26 (ADR-0011) found public members that only the framework calls, and inconsistencies a caller
trips on:

- `PEventOptions.toJson(name)` writes the protocol's `LISTEN_EVENT`; the constructor of `PEvent` is the framework's.
  Both are public because `PWidget`, which calls them, is in `com.ponysdk.core.ui.basic` and they are in
  `com.ponysdk.core.ui.basic.event`.
- `UIContext.stackCookieRequest` exists for `PCookies` alone (ADR-0009, decision 7), on the class every application
  sees.
- `PEvent` says "absent" three ways: `getString` returns null, `getNumber` NaN, `getBoolean` false, which an
  application cannot tell from a real false.
- `PCookies` (2.x): one listener, replaced by the next `setListener`; `setCookie(name, value, expires, path)` beside
  `setCookie(name, value, expires, domain, path, secure, sameSite)`, two `String`s in another order; `sameSite` a free
  string.

Java has no visibility between packages but `public`. A member stops being public only when its caller is in its
package, or when it is reached through an object the framework hands over.

## Decision

1. `PEvent` and `PEventOptions` move to `com.ponysdk.core.ui.basic`, beside `PWidget`: `PEventOptions.toJson` and the
   constructor of `PEvent` become package-private. Both are new in 3.0.
2. `UIContext.stackCookieRequest` becomes private. `UIContext` creates its `PCookies` with that method as the channel
   for an embedded application's cookie requests: `PCookies(Consumer<StreamHandler>)`. An application gets its
   cookies from `UIContext.getCookies()`, as in 2.x.
3. `PEvent`: absent is null for every type. `getNumber` returns `Double`, `getBoolean` returns `Boolean`.
4. `PCookies`:
   - `addInitializedListener(Runnable)` returns a `HandlerRegistration`; any number of listeners; called at once once
     the cookies are known. `setListener` and `CookiesListener` go.
   - `setCookie(name, value, CookieOptions)` replaces the seven-parameter form: `CookieOptions.expires(…)
     .withPath(…).withDomain(…).secure().sameSite(SameSite.STRICT)`, immutable, each attribute named. `removeCookie(name,
     CookieOptions)` takes the path and the domain the same way. The short forms of 2.x stay
     (`setCookie(name, value)`, `(…, path)`, `(…, expires)`, `(…, expires, path)`, `removeCookie(name)`, `(…, path)`).
   - `SameSite` is an enum: `STRICT`, `LAX`, `NONE`.

## Consequences

- A 2.x application that calls `setListener` or the seven-parameter `setCookie` no longer compiles: UPGRADING.md gives
  the replacement. A silent reordering of two `String` parameters was the alternative, and is what this avoids.
- `PCookies.onClientData` stays public: `UIContext`, in another package, calls it with the terminal's message.
- The wire does not change.
