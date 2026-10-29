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
| [Streams](streams.md) | Values from another thread, the latest per key: `Conflator` |
| [Embedding](embedding.md) | The application inside another page, `<pony-app>` |
| [FDC3](fdc3.md) | Talking to the other applications of a trader's desktop |
| [Reconnection](reconnection.md) | Keeping a session through a network cut |
| [Deployment](deployment.md) | Behind a load balancer, on AWS for the examples; a server that stops moves its pages |
| [Security](security.md) | What is protected by default, and the settings |
| [Observability](observability.md) | OpenTelemetry metrics, latency |
| [Testing](testing.md) | `PSuite`, the Pony driver, browser tests |
| [Development tools](development.md) | The inspector over the page, hot reload of a recompiled screen |
| [Troubleshooting](troubleshooting.md) | Symptoms and their causes |
