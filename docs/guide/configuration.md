# Configuration reference

## ApplicationConfiguration

Each property has its setter (`heartBeatPeriod`: `setHeartBeatPeriod`). Defaults as in
`ApplicationConfiguration.java`.

| Property | Description | Default |
|----------|-------------|---------|
| `applicationContextName` | The path of the application: `http://host:port/<name>/` | `sample` |
| `entryPointClass` | The `EntryPoint` started for each browser tab | none (required) |
| `heartBeatPeriod` | Liveness: a client silent for half of it is pinged, for two of it is dropped ([ADR-0006](../decisions/0006-liveness-by-ping.md)) (ms) | 5000 |
| `sessionTimeout` | Stored and readable (`getSessionTimeout`); `PonySDKServer` does not apply it (minutes) | 15 |
| `debugMode` | Verbose logging | false |
| `style`, `javascript` | Stylesheets and scripts of the bootstrap page | none |
| `stringDictionaryEnabled` | Strings sent once, then by index | true |
| `stringDictionaryMaxSize` | Max dictionary entries | 4096 |
| `stringDictionaryMinLength` | Shorter strings are always sent inline | 8 |
| `stringDictionaryAdmitOnSecondUse` | A string enters the dictionary on its second use: one-off strings stay inline | true |
| `updateCoalescingEnabled` | Successive updates of one widget property in a flush sent once | true |
| `maxProtocolVersion` | Highest binary protocol used; each connection uses min(this, the terminal's). 4 is needed by `PWidget.addEventListener` | 4 (latest) |
| `wsOriginCheckEnabled` | The WebSocket upgrade checks the `Origin` header | true |
| `wsAllowedOrigins` | The other origins allowed to connect: the pages that embed the application ([11.7](embedding.md)) | none: same origin only |
| `wsPermessageDeflateEnabled`, `wsDeflateLevel` | Frame compression and its zlib level | true, 3 |
| `wsMaxInboundMessageSize` | Largest client message (bytes) | 1 MB |
| `wsInputBufferSize` | Jetty's buffer per incoming message (bytes); upstream messages are small JSON | 1024 |
| `wsIdleTimeoutMs`, `wsSendTimeoutMs` | WebSocket idle timeout; wait on a slow client before disconnecting (ms) | 1 000 000, 60 000 |
| `wcSharedSheets` | CSS URLs injected into every web component shadow root via `pony.wc` | none |
| `legacyClassNames` | Adds the GWT class names the 2.x CSS used ([UPGRADING.md](../../UPGRADING.md)) | false |
| `reconnectionTimeoutMs` | Transparent WebSocket reconnection window (ms); 0 = disabled (destroy on close) | 0 |
| `maxRecordingEntries` | Max buffered protocol entries during a suspension; exceeded, the context is destroyed | 10 000 |
| `reconnectionListener` | Server-side hook called on WebSocket disconnect | none |

Metrics are set on the servlet, not here: `WebSocketServlet.setMetrics(PonySDKMetrics)`
([11.4](observability.md)).

## PonySDKServer

| Setter | Description | Default |
|--------|-------------|---------|
| `setPort`, `setHost` | HTTP port and interface | 80, `0.0.0.0` |
| `setUseSSL` | Adds an HTTPS connector; needs `setSslKeyStoreFile` (and its passphrase, type) | true |
| `setSslPort` | HTTPS port | 443 |
| `setSessionCookieSecure` | The session cookie is `Secure`: a browser does not send it over plain HTTP | true |
| `setApplicationManager` | The application: a `JavaApplicationManager` holding the configuration | none (required) |

For plain HTTP on a development machine: `setUseSSL(false)` and `setSessionCookieSecure(false)`, as in the
[quick start](quick-start.md).

## HTTP compression

Static HTTP assets (bootstrap HTML, the terminal's JavaScript bundle, AJAX responses) are compressed by a
Jetty `CompressionHandler`. **gzip** is always enabled; **Brotli** is enabled automatically when its
native library is available on the platform — the browser negotiates `br` vs `gzip` via the
`Accept-Encoding` header. Already-compressed content (images, fonts, archives) is skipped.

Brotli relies on a native library (`brotli4j`). Since JDK 24 ([JEP 472](https://openjdk.org/jeps/472))
the JVM warns on native access and will eventually block it unless explicitly granted, so run your
server with:

```sh
java --enable-native-access=ALL-UNNAMED -jar your-app.jar
```

If the native library is unavailable (or the flag denied), the server degrades gracefully to gzip
only — startup is never broken.

> This applies to **HTTP asset delivery** only. The real-time WebSocket protocol uses
> permessage-deflate, configured separately via `wsPermessageDeflateEnabled`.
