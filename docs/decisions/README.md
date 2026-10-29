# Architecture decisions

Each structuring change of PonySDK 3 has its record here: the context, the decision, the options set aside and
what was measured once it was built. They are written for the people who change PonySDK; a user starts with
[README.md](../../README.md) and [ARCHITECTURE.md](../../ARCHITECTURE.md).

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-server-session-administration.md) | Server-side session administration | Accepted |
| [0002](0002-javascript-terminal.md) | A TypeScript terminal on native elements, replacing GWT | Accepted |
| [0003](0003-protocol-v4-listeners.md) | Protocol 4, handlers removed from their object and DOM events by name | Accepted |
| [0004](0004-terminal-chunks.md) | The terminal's rare widgets loaded on demand | Accepted |
| [0005](0005-page-left-and-terminal-time.md) | The terminal tells the server its page is left, and how long a probe waited in it | Accepted |
| [0006](0006-liveness-by-ping.md) | The server judges a client alive by what it receives, and pings a silent one | Accepted |
| [0007](0007-page-left-by-going-away.md) | A close with 1001 from the browser means its page is gone | Accepted |
| [0008](0008-embedded-application.md) | An application embedded in another page, as the element `<pony-app>` | Accepted |
| [0009](0009-embedding-limits.md) | The limits of an embedded application, lifted | Accepted |
| [0010](0010-web-component-classes.md) | Typed Java classes for web components, generated from their Custom Elements Manifest | Accepted |
| [0011](0011-one-way-to-listen.md) | One way to listen to a custom event, and the traps of the web component API closed | Accepted |
| [0012](0012-api-surface.md) | The public API reduced to what an application calls, and PCookies made consistent | Accepted |
| [0013](0013-conflated-streams.md) | A stream of values bound to a context, the latest value per key at each flush | Accepted |
| [0014](0014-fdc3.md) | FDC3 from Java, through the desktop agent of the page | Accepted |
| [0015](0015-content-security-policy.md) | A Content Security Policy built by PonySDK, and JavaScript called by name | Accepted |
| [0016](0016-development-tools.md) | The inspector and hot reload, for development | Accepted |
| [0017](0017-drain.md) | A server that stops moves its pages to the others | Accepted |
| [0018](0018-state-transfer.md) | A moved page carries the state its application declares | Accepted |
| [0019](0019-signals.md) | Signals, reactive state bound to widgets | Accepted |
| [0020](0020-routing.md) | Routes by annotation, on the browser history | Accepted |
| [0021](0021-forms-binder.md) | A binder between the fields of a screen and a bean, with Bean Validation | Accepted |
| [0022](0022-locale.md) | A locale per context: the visitor's language, texts, formats and direction | Accepted |
| [0023](0023-spring-boot-starter.md) | A Spring Boot starter, PonySDK's servlets in Boot's own Jetty | Accepted |
| [0024](0024-default-theme.md) | A default theme for the widgets: tokens, light and dark, scoped by a class | Accepted |
| [0025](0025-datagrid-accessibility.md) | DataGrid2 read as a grid by a screen reader, and driven from the keyboard | Accepted |
| [0026](0026-chart.md) | A chart drawn by the terminal, fed by typed arrays | Accepted |
