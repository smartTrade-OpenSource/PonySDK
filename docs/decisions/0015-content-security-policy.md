# ADR-0015: A Content Security Policy built by PonySDK, and JavaScript called by name

- Status: Accepted
- Date: 2026-10-27
- Supersedes: pull request #379 (2.x, `feat(csp)!`), whose API it keeps

## Context

A Content Security Policy (CSP) tells the browser which scripts a page may run. Without one, an HTML injection in
an application (a label set with `setInnerHTML` from user data) runs script. Applications on PonySDK (LiquidityFX
first) must serve `script-src` without `'unsafe-inline'` and without `'unsafe-eval'`, and frame protection.

What stood in the way, in 3.0:

1. The bootstrap page carries three inline scripts (`BootstrapServlet`): the early WebSocket, the reconnection
   flag, the registration of the web components' shared sheets.
2. Three APIs send JavaScript source that the terminal runs with `eval` or `new Function`:
   `PScript.execute` (`objects/script.ts`), `PObject.evalTerminalScript` (`runtime/widget.ts`) and the
   `TextFunction` of a `PFunctionalLabel` (`objects/script.ts`).
3. The server sends no security header: any site can show the application in a frame.

The 2.x needs of #379 (GWT's `installCode`, the history iframe) do not exist in 3.0: the terminal is a module served
by `'self'`.

## Decision

1. The bootstrap page keeps one inline script, the early WebSocket: a constant, so its SHA-256 is computed once.
   Moving it to a file would cost a round trip before the socket opens. The reconnection flag and the shared sheets
   become `<meta>` elements (`pony-reconnection`, `pony-wc-shared-sheets`), read by the terminal and `ponysdk.js`.
2. The page PonySDK builds is served with
   ```
   Content-Security-Policy: script-src 'self' 'sha256-…' [script origins];
       object-src 'none'; base-uri 'self'; frame-ancestors 'self' [wsAllowedOrigins]
   ```
   - script origins: the origins of the absolute URLs in `setJavascript`, and `addScriptSources(...)`;
   - `frame-ancestors`: the pages that embed the application are the ones `setWsAllowedOrigins` already lists;
   - every response of `BootstrapServlet` carries `X-Content-Type-Options: nosniff`.

   On by default; `setContentSecurityPolicyEnabled(false)` removes the header, for an application that sends its
   own policy (a proxy) or builds its own page. An `index.html` of the application's files is served as it is, with
   no policy: PonySDK does not know its inline scripts.
   `style-src`, `connect-src`, `img-src` are not set: PonySDK's terminal writes `<style>` elements, and an
   application's images, fetches and fonts come from anywhere today. Restricting them is the application's choice.
3. JavaScript is called by name, a function of `window` that the application's own scripts define, as
   `bindTerminalFunction` already does. The API of #379:
   - `PScript.call(window, functionName, args...)`, and with a callback and a delay;
   - `PObject.callTerminalFunction(functionName, args...)`: the function receives the element, then the args;
   - `TextFunction.named(javaFunction, jsFunctionName)`: the function receives the args array.

   One new protocol key, `FUNCTION_NAME`, appended (no number moves); its arguments travel in `FUNCTION_ARGS`.
   The function is looked up when it is called, so a script loaded later still provides it.
4. `PScript.execute`, `evalTerminalScript` and the `TextFunction` constructor are removed, and so is the terminal's
   code that evaluated their source: no JavaScript source travels, the policy never allows `'unsafe-eval'`.
   `EVAL` and `FUNCTION_CREATION` stay in the protocol, unused, so that no key is renumbered.
   Rejected: deprecating them behind a switch that kept `'unsafe-eval'` by default (first version of this ADR). 3.0
   already breaks the API of 2.x; a switch left on by default would leave eval allowed in most applications.
5. An application embedded with `<pony-app>` runs in the host page: the host's policy applies. The embedding guide
   says what it must allow: `script-src` and `connect-src` the application's origin.

## Consequences

- An application whose scripts come from another origin than its page, and are not listed in `setJavascript`, adds
  that origin with `addScriptSources`, or the browser blocks them. One that adds inline scripts to the bootstrap
  page (overriding `addScript`) must hash them itself or disable the policy. One shown in a frame by another site
  lists that site in `setWsAllowedOrigins`. UPGRADING.md says so.
- HTML sent to the terminal (`PHTML`, `setInnerHTML`, `setAttribute`) cannot carry inline event handlers
  (`onclick=`) or `javascript:` URLs: the policy blocks them, as it should.
- The showcase's browser suites, run under the policy, are the proof that PonySDK needs neither `'unsafe-inline'` nor
  `'unsafe-eval'`.
- An application moving from 2.x replaces each `execute`, `evalTerminalScript` and `new TextFunction` with a function
  of its own scripts: the compiler lists them. One whose own scripts need eval (a library that compiles templates)
  turns the policy off and sends its own.
- A CSP source expression cannot name an IPv6 address (`http://[::1]:8082`): Chrome rejects it and logs an error on
  every page. Such an origin is left out of the policy with a warning, so a page at an IPv6 address cannot frame the
  application. The browser suites that frame the showcase run on `localhost`, as in CI.
