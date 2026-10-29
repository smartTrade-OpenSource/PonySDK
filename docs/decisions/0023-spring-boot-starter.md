# ADR-0023: A Spring Boot starter, PonySDK's servlets in Boot's own Jetty

- Status: Accepted
- Date: 2026-10-29

## Context

`ponysdk-spring` configures PonySDK from XML bean files: `SpringApplicationManager` builds a Spring context per screen
from `client_application.xml`, and `PonySDKServer` starts a Jetty of its own, on its own port, from a bean. A Spring
Boot 4 application gets neither Boot's security, nor its actuator, nor its graceful shutdown in front of PonySDK, and
writes the XML to get the screens. The goal: one dependency and a few `ponysdk.*` properties.

What `PonySDKServer` registers: the bootstrap servlet (`/*`), the WebSocket servlet (a Jetty 12 `JettyWebSocketServlet`),
the AJAX, stream and `/health` servlets, and a CORS filter, in a context named after `applicationContextName`.

## Decision

1. New module `ponysdk-spring-boot-starter` (`com.ponysdk.impl.spring.boot`), registered in
   `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. It depends on `ponysdk` and on
   `spring-boot-starter-jetty` (Boot 4.1.2, on the Spring Framework 7.0.10 and Jetty 12.1.14 of the version catalog).
2. Option (a), see below: the servlets are `ServletRegistrationBean`s and a `FilterRegistrationBean` of Boot's embedded
   Jetty, under `/<applicationContextName>/` (`/*` of the bootstrap servlet, `/ws/*`, `/ajax`, `/stream`, `/health`).
   `PonySDKBootstrapServlet` strips that prefix, so the resources are those after it, and sends `/<name>` to `/<name>/`.
   A customizer fails the start if the web server is not Jetty.
3. `@ConfigurationProperties("ponysdk")` (`PonySDKProperties`) is mapped onto an `ApplicationConfiguration` bean with
   Boot's `PropertyMapper`: a property left out keeps `ApplicationConfiguration`'s default. Durations and sizes use
   Boot's types (`reconnection-timeout=30s`, `websocket.max-message-size=64KB`).
4. The entry point is a bean of type `EntryPoint`, asked for at each screen by `BootApplicationManager`. The scope
   `ui` (`UIScope`) gives each screen its own instance, which lives in an attribute of its `UIContext` and is destroyed
   with it, destruction callbacks included: what the per-screen Spring context of `SpringApplicationManager` gave, without
   one context per screen.
5. `PonySDKDrain`, a `SmartLifecycle` of phase `GRACEFUL_SHUTDOWN_PHASE + 1`, runs `Drain.run` (ADR-0017) when
   Boot stops the context with `server.shutdown=graceful` (Boot 4's default). A lifecycle stops from the highest phase
   down, so the drain ends before the web server stops accepting requests, and before it stops. `ponysdk.drain-timeout`
   is 10 s by default, 0 stops at once.
6. With the actuator on the classpath, `PonySDKHealthIndicator` (component `ponysdk`, `management.health.ponysdk.enabled`)
   says UP, then OUT_OF_SERVICE (HTTP 503) while `Drain.isDraining()`.
7. Every bean is `@ConditionalOnMissingBean` (the registrations by name): an application's own `ApplicationConfiguration`,
   `ApplicationManager` or servlet registration replaces the starter's.
8. Not carried over from `ponysdk-spring`: `SpringEntryPoint` (it needs five beans that the XML declares per screen; an
   application that keeps it declares them in the scope `ui`), `InitializingActivity.afterContextInitialized` (beans of the
   scope `ui` are created when asked for, not at a context refresh), and the client XML files.
   Not mapped: `sessionTimeout` (`ApplicationConfiguration` stores it and nothing applies it; Boot's
   `server.servlet.session.timeout` is the real one) and `hotReload` (see below).

## Options considered

- **(a) PonySDK's servlets in Boot's Jetty (chosen).** Verified with a running application, not by reading: the WebSocket
  servlet works unchanged, because Boot's Jetty auto-configuration sets up Jetty's WebSocket support
  (`JettyServletWebServerAutoConfiguration.JettyWebSocketConfiguration`, with `jetty-ee11-websocket-jetty-server` from the
  Jetty starter) on a context that is a Jetty `ServletContextHandler`. `BootRunTest` opens the bootstrap page and the WebSocket on a random port
  with the Pony driver, gets the first screen, clicks, and closes; a headless Chrome on the `sample` application does the
  same (see Consequences). One port, and Boot's security filter chain, actuator, session and cookie settings, access logs
  and graceful shutdown are in front of PonySDK as of any servlet.
- (b) `PonySDKServer` as a `SmartLifecycle` bean, on its own port. It works with no Jetty requirement on Boot's side,
  but gives two ports and two Jetty instances, nothing of Boot's security in front of the screens, and a drain on a JVM
  shutdown hook that races Boot's own. An application that wants it can still make the bean itself: the class is unchanged.
- Hot reload as `ponysdk.hot-reload`. `HotReload` defines again the classes of the entry point's package, which are all
  of an application's; a singleton bean of the Boot context keeps the old classes, and an entry point of the new ones
  cannot be injected with it (a different class of the same name). `SpringApplicationManager` gets away with it because
  each screen has a context of its own. Not offered rather than half working; a restart of the Boot context is the way
  (Spring Boot DevTools, not tested with PonySDK).
- The entry point as a singleton. An entry point keeps the state of its screen (here: widgets, a counter): a singleton would
  share it between the tabs. `ObjectProvider.getObject()` at each screen gives a singleton when the bean is one, a new
  instance in the scope `ui`.
- A Spring context per screen, as `SpringApplicationManager`: a context refresh per tab, and the beans of a screen cannot
  be injected into the application's singletons.
- A starter and an autoconfigure module, as Boot's own: two modules for a dozen classes.

## Consequences

- Measured, Pony driver on loopback, one entry point whose click sets a label (5,000 round trips after 3,000 warm-up,
  the same JVM, second round: the first one carries the JIT warm-up of whoever runs first):

  | | p50 | p90 | p99 |
  |---|---|---|---|
  | Boot's Jetty (this starter) | 97 us | 155 us | 238 us |
  | `PonySDKServer` | 95 us | 158 us | 238 us |

  The first round, Boot first, 174 / 303 / 804 us against 113 / 216 / 477 us. The WebSocket servlet and its encoder are the
  same classes: the wire is the same. The bootstrap page is 1,226 B and 300 B of headers on both.
  `:ponysdk:bench` was not run: nothing changes on the write path.
- Real browser: headless Chrome on `./gradlew :sample:runSpringBoot` (port 8185): the page, its title from
  `ponysdk.application-name`, two clicks ("Hello, Boot! (1 in this tab)", then 2), no page error and no page resource over 400; the one console error is
  the browser's own `/favicon.ico`, a 404 as on `PonySDKServer`. `/actuator/health` shows
  `ponysdk: UP` next to Spring MVC's `DispatcherServlet`, which keeps `/`.
- Drain on that application, SIGTERM with one Chrome page open: `Drain: 1 contexts moved in 14 ms` at 22:05:56.275, Boot's
  `Commencing graceful shutdown` at .276, `Graceful shutdown complete` at .379; the first `/actuator/health` poll during the
  drain answered 503, the page reloaded at .281, while the server was already stopping: no context was created on it (a second server
  would have taken the page). `BootDrainTest` holds the order: with a lingering screen, `/health` answers 503 `DRAINING` through the web
  server for the window, the client gets `DESTROY_CONTEXT`, then the connection is refused.
- What Boot's defaults change from `PonySDKServer`'s, to set in `application.properties`: the session cookie is not `Secure`
  (`server.servlet.session.cookie.secure=true` behind TLS); HTTP compression is off (`server.compression.enabled=true`:
  the terminal's 52,817 B go as 10,899 B, measured on the sample).
- Spring Security in front, verified by `BootSecurityTest`: without a filter chain of the application's, the page and
  `/health` need a login; with `/<name>/**` permitted the page and the WebSocket of a session work; with CSRF
  protection on, the terminal's POSTs to `/<name>/ajax` and `/<name>/stream` get 403 (they carry no token).
- Tests: 30, `./gradlew :ponysdk-spring-boot-starter:check`, 224 of 224 lines and 18 of 18 branches covered. The starter's
  jar is 21 KB. The drain default (10 s) is under Boot's `spring.lifecycle.timeout-per-shutdown-phase` (30 s); a longer
  one is the application's to keep under it, and under its platform's stop timeout.
- Jetty only: a Boot application on Tomcat or Undertow fails at start with a message that says so.
- A second server of the same application takes the pages that a draining one moves, as in ADR-0017: on one server they
  come back to it.
