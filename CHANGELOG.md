# PonySDK Changelog

## Unreleased (next major version)

### Breaking — strict Content Security Policy

PonySDK now runs under `Content-Security-Policy: script-src 'self'; object-src 'none'; base-uri 'self'`
(no `'unsafe-eval'`, no `'unsafe-inline'` for scripts). See
[ADR 0001](docs/decisions/0001-strict-content-security-policy.md).

To get there, JavaScript source no longer travels from the server to the terminal. The APIs that
sent it are removed; their replacements call a **named** global function that the application
defines in one of its own static scripts (`ApplicationConfiguration.javascript`).

| Removed | Replacement |
| --- | --- |
| `PScript.execute(window, js)` | `PScript.call(window, functionName, args...)` |
| `PScript.execute(window, js, callback)` / `(window, js, period)` / `(window, js, callback, period)` | `PScript.call(window, functionName, args, callback, period)` — `args`, `callback` and `period` may be null |
| `PObject.evalTerminalScript(js)` | `PObject.callTerminalFunction(functionName, args...)` — the function receives the widget's element first, then `args` |
| `new TextFunction(javaFunction, jsBody)` | `TextFunction.named(javaFunction, jsFunctionName)` — the function receives the `args` array and returns a string |
| `TextFunction.getJsFunction()` | `TextFunction.getJsFunctionName()` |
| `BootstrapServlet.addHistoryIFrame(request)` | removed: the legacy GWT history iframe is no longer needed |

Migration example:

```java
// before
PScript.execute(window, "console.log('saved ' + 3)");
checkbox.evalTerminalScript("element.checked = true");
new TextFunction(f, "return args[0] + '-' + args[1];");

// after
PScript.call(window, "appLog", "saved 3");
checkbox.callTerminalFunction("appCheck");
TextFunction.named(f, "appJoin");
```

```javascript
// in one of the application's scripts
function appLog(message) { console.log(message); }
function appCheck(element) { element.checked = true; }
function appJoin(args) { return args[0] + '-' + args[1]; }
```

Arguments may be strings, numbers, booleans or null. A string that starts with `{` or `[` and is
valid JSON reaches the function already parsed: do not call `JSON.parse` on it again.

Also check, before enabling the policy, that the HTML your application sends (`PHTML`,
`setInnerHTML`, `PAnchor`, ...) and the attributes it sets (`setAttribute`) carry no inline event
handler (`onclick=...`) and no `javascript:` URL: the policy blocks them. Serving the policy as
`Content-Security-Policy-Report-Only` first lists them without breaking anything.

### Fixed

- `InfiniteScrollAddon` (used by `ListBox`) passed the result of `updateView()` to `setTimeout`
  instead of a function: the view was refreshed at once instead of debounced, and the string
  evaluation it triggered is blocked under a strict policy.

### Added

- `PonySDKServer.setResponseHeaders(Map)`: headers added to every HTTP response, for instance a
  `Content-Security-Policy`. None by default.
- The GWT terminal is loaded with `installCode=false`: the compiled code is fetched by a
  `<script src>` instead of being injected as inline script.
