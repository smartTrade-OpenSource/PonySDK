# 0001 — Run under a strict Content Security Policy

- Status: accepted
- Date: 2026-10-08
- Ticket: ST-55108 (epic ST-15100)

## Context

Applications built on PonySDK (LiquidityFX first) must serve a Content Security Policy without
`'unsafe-eval'` and without `'unsafe-inline'` in `script-src`. Today such a policy blacks the
application out, because PonySDK itself needs both:

- the GWT `xsiframe` linker downloads the compiled terminal and installs it as inline script
  (and wraps it in `eval` on Chrome);
- the bootstrap page carries a legacy history iframe with a `javascript:` URL;
- three public server APIs ship JavaScript source as strings, which the terminal runs with
  `eval` or `new Function`: `PScript.execute`, `PObject.evalTerminalScript` and
  `TextFunction` (used by `PFunctionalLabel`).

## Decision

PonySDK runs, with no violation, under

```
Content-Security-Policy: script-src 'self'; object-src 'none'; base-uri 'self'
```

- The terminal is installed with `installCode=false`: the compiled code is loaded by a
  `<script src>` instead of being injected as text.
- The legacy history iframe is removed from the bootstrap page.
- No JavaScript source travels from the server to the terminal any more. The three APIs above
  are removed and replaced by calls to **named** functions that the application defines in its
  own static scripts:
  - `PScript.call(window, functionName, args...)` replaces `PScript.execute(window, js...)`;
  - `PObject.callTerminalFunction(functionName, args...)` replaces `evalTerminalScript(js)`;
  - `TextFunction` takes the name of a function instead of a function body.
- The removal is a breaking change and ships in a new major version. Applications must move
  their inline JavaScript into their own `.js` files.
- PonySDK does not emit a CSP header by default: each application owns its policy.
  `PonySDKServer` can add response headers when configured to.

`style-src` is out of scope: inline styles stay allowed.

## Consequences

- Applications upgrading must migrate every `PScript.execute`, `evalTerminalScript` and
  `TextFunction` call. The new names fail at compile time on purpose, so no call survives
  silently with a changed meaning.
- HTML that an application sends to the terminal (`PHTML`, `setInnerHTML`, ...) or attributes it
  sets with `setAttribute` must not carry inline event handlers or `javascript:` URLs: the
  policy blocks them. PonySDK does not filter them; the policy, in report-only mode first, is
  what finds them.
- A Playwright test serves the sample under this policy and fails on any violation.
