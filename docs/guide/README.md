# Guides

How to use PonySDK, one subject per page. How it works inside: [ARCHITECTURE.md](../../ARCHITECTURE.md); why it is
built so: the [decisions](../decisions/README.md); from 2.10: [UPGRADING.md](../../UPGRADING.md).

| Guide | For |
|---|---|
| [Quick start](quick-start.md) | The dependency, a first screen, the server, in five minutes |
| [Configuration](configuration.md) | Every setting of `ApplicationConfiguration` and `PonySDKServer`, HTTP compression |
| [Web components](web-components.md) | Driving custom elements from Java; typed classes from a Custom Elements Manifest |
| [Custom JavaScript](addons.md) | `PAddOn`: a JavaScript library behind a Java class |
| [Forms](forms.md) | Binding fields to a bean: converters, validators, Bean Validation, errors a screen reader reads |
| [EventBus](event-bus.md) | Decoupling the parts of a screen |
| [Internationalisation](i18n.md) | The visitor's locale, translated texts, numbers and dates, right-to-left pages |
| [Streams](streams.md) | Values from another thread, the latest per key: `Conflator` |
| [Signals](signals.md) | A value shown in several places, computed from others, bound to widgets: `Signal`, `Bind` |
| [Client commands](client-commands.md) | A click that shows a panel, toggles a class or takes the focus in the browser, with no round trip: `PCommands` |
| [Routing](routing.md) | `@Route` views, a `Router` on the history token, guards and access |
| [Charts](charts.md) | Time series drawn by the terminal: `PChart`, live feeds, sliding window |
| [Embedding](embedding.md) | The application inside another page, `<pony-app>` |
| [FDC3](fdc3.md) | Talking to the other applications of a trader's desktop |
| [Reconnection](reconnection.md) | Keeping a session through a network cut |
| [Spring Boot](spring-boot.md) | The starter: one dependency, the `ponysdk.*` properties, the entry point as a bean, Boot's graceful shutdown |
| [Deployment](deployment.md) | Behind a load balancer, on AWS for the examples; a server that stops moves its pages |
| [Security](security.md) | What is protected by default, and the settings |
| [Observability](observability.md) | OpenTelemetry metrics, latency |
| [Accessibility](accessibility.md) | What a screen reader hears of `ListBox` and `DataGrid2`, the grid from the keyboard, naming a widget, the page language, the axe audit of the showcase |
| [Testing](testing.md) | `PSuite`, the Pony driver, browser tests |
| [Theme](theme.md) | A finished look without CSS: `setTheme("pony")`, light and dark, the tokens, overriding them |
| [Development tools](development.md) | The inspector over the page, hot reload of a recompiled screen |
| [Troubleshooting](troubleshooting.md) | Symptoms and their causes |
