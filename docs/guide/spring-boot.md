# Spring Boot

A Spring Boot 4 application gets PonySDK with one dependency and a few `ponysdk.*` properties. The starter registers
PonySDK's servlets in Boot's own Jetty: one port, with Boot's security, actuator and graceful shutdown in front. Why it is
built so: [ADR-0023](../decisions/0023-spring-boot-starter.md).

The code below is `sample/src/springBoot/`: `./gradlew :sample:runSpringBoot` runs it from this repository on
[http://localhost:8185/hello/](http://localhost:8185/hello/) (`-Pport=<port>` for another port).

## The dependency

PonySDK 3 is not on Maven Central yet: install it in your local repository from this one
(`./gradlew :ponysdk:publishToMavenLocal :ponysdk-spring-boot-starter:publishToMavenLocal`), then:

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {
    implementation 'com.ponysdk:ponysdk-spring-boot-starter:3.0.0-SNAPSHOT'
}
```

The starter brings `ponysdk` and `spring-boot-starter-jetty`. PonySDK's WebSocket servlet is Jetty's own: on Tomcat or
Undertow the application fails at start and says so. A starter of yours that brings Tomcat (`spring-boot-starter-webmvc`)
is declared without it:

```groovy
implementation('org.springframework.boot:spring-boot-starter-webmvc') {
    exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
}
```

## The entry point is a bean

```java
@SpringBootApplication
public class SpringBootSample {

    public static void main(final String[] args) {
        SpringApplication.run(SpringBootSample.class, args);
    }

    @Bean
    @Scope(UIScope.NAME)
    EntryPoint entryPoint(final GreetingService greetings) {
        return new HelloEntryPoint(greetings);
    }
}
```

`start` runs once per browser tab, as in the [quick start](quick-start.md); the entry point is any bean of type
`EntryPoint`, and may be given any other bean.

The scope `ui` (`UIScope.NAME`) gives each screen its own instance, created when the screen asks for it and destroyed,
with its destruction callbacks (`DisposableBean`, `destroyMethod`), when the screen ends. Use it for an entry point that keeps the state of its
screen, and for what that screen's beans share. A singleton that needs a bean of this scope takes it through a scoped
proxy: `@Scope(value = UIScope.NAME, proxyMode = ScopedProxyMode.TARGET_CLASS)`. Asked for outside a screen's thread
(a thread of your own), the scope throws: run the code with `UIContext.execute`.

An entry point that is a singleton is shared by every screen.

## The properties

```properties
ponysdk.application-context-name=hello
ponysdk.application-name=Hello from Spring Boot
```

The application is served at `http://host:port/<application-context-name>/`: the page, `/ws`, `/ajax`, `/stream` and
`/health` under that path, and everything else is Boot's (Spring MVC, the actuator). A property left out keeps the default
of [`ApplicationConfiguration`](configuration.md). Your IDE completes them from `spring-configuration-metadata.json`.

| Property | Description | Default |
|---|---|---|
| `ponysdk.application-context-name` | The path of the application | `sample` |
| `ponysdk.application-id`, `.application-name`, `.application-description` | Identity; the name is the page's title | none |
| `ponysdk.reconnection-timeout` | Transparent reconnection window ([reconnection](reconnection.md)); 0 disables it | 0 |
| `ponysdk.debug-mode` | Each element names its Java class and creation line, and the inspector shows ([development tools](development.md)). Not for production | false |
| `ponysdk.legacy-class-names` | Adds the GWT class names the 2.x CSS used | false |
| `ponysdk.meta` | `<meta>` attributes of the bootstrap page | none |
| `ponysdk.javascript` | Scripts of the bootstrap page, in load order | none |
| `ponysdk.style.<id>` | Stylesheets of the bootstrap page, in order: the id, then the path | none |
| `ponysdk.wc-shared-sheets` | Stylesheets injected into every web component's shadow root | none |
| `ponysdk.served-resource-extensions` | More file types served from the classpath, e.g. `json` ([security](security.md)) | web files |
| `ponysdk.drain-timeout` | On a graceful shutdown, the live pages move to the other servers over this time; 0 stops at once (below) | 10s |
| `ponysdk.content-security-policy.enabled` | The bootstrap page carries a Content-Security-Policy ([security](security.md)) | true |
| `ponysdk.content-security-policy.script-sources` | More script origins the policy allows | none |
| `ponysdk.websocket.allowed-origins` | The other origins allowed to connect: the pages that embed the application ([embedding](embedding.md)) | none: same origin only |
| `ponysdk.websocket.origin-check` | The upgrade checks the `Origin` header | true |
| `ponysdk.websocket.max-message-size` | Largest message a client sends | 1MB |
| `ponysdk.websocket.input-buffer-size` | Jetty's buffer per incoming message | 1KB |
| `ponysdk.websocket.idle-timeout` | The connection closes after this long without a frame | 1000s |
| `ponysdk.websocket.send-timeout` | Wait on a slow client before disconnecting it | 60s |
| `ponysdk.websocket.permessage-deflate` | Frame compression is negotiated | true |
| `ponysdk.websocket.deflate-level` | Its zlib level, 0 to 9, or -1 for Jetty's default | 3 |
| `ponysdk.string-dictionary.enabled` | Strings are sent once, then by index | true |
| `ponysdk.string-dictionary.persistence` | The dictionary is kept across restarts, under `persist-path` | true |
| `ponysdk.string-dictionary.persist-path` | Its directory, relative to the working directory | `data` |

What is not here is Boot's own, or the application's:

- Session timeout, cookie and port: `server.servlet.session.timeout`, `server.servlet.session.cookie.secure`,
  `server.port`. `ApplicationConfiguration.sessionTimeout` is stored and not applied. Set the cookie `Secure` behind TLS
  (`PonySDKServer`'s default; Boot's is off).
- HTTP compression of the page and the terminal: `server.compression.enabled=true` (off in Boot; the terminal's 52,817 B go
  as 10,899 B).
- Behind a proxy: `server.forward-headers-strategy`. The WebSocket's Origin check reads `X-Forwarded-Host` itself.
- Hot reload: there is no `ponysdk.hot-reload`, see [ADR-0023](../decisions/0023-spring-boot-starter.md). Restart the
  application instead.
- The other settings of `ApplicationConfiguration` (heartbeat, protocol version, session access policy, the reconnection
  listener): declare your own `ApplicationConfiguration` bean, which replaces the starter's, with its properties.

Every bean of the starter yields to one of yours: `ApplicationConfiguration`, `ApplicationManager`, and each servlet
registration by its name (`ponySDKBootstrapServlet`, `ponySDKWebSocketServlet`, `ponySDKAjaxServlet`,
`ponySDKStreamServlet`, `ponySDKHealthServlet`, `ponySDKCrossOriginFilter`). A metrics-aware WebSocket servlet
([observability](observability.md)) is your own `ServletRegistrationBean` named `ponySDKWebSocketServlet`.

## Security

Spring Security, when it is on the classpath, is in front of the application's path as of any servlet.

- With no `SecurityFilterChain` of yours, Boot closes every path behind a login: the page and `/health` answer 401 or a
  redirect. Permit the path the application is served at, or require the login you want of it:

  ```java
  http.authorizeHttpRequests(requests -> requests.requestMatchers("/hello/**").permitAll().anyRequest().authenticated())
  ```

  The WebSocket upgrade passes the same filter chain.
- CSRF protection refuses the POSTs that the terminal sends to `/<name>/ajax` (the addons' requests) and `/<name>/stream`
  (uploads), which carry no token: `csrf.ignoringRequestMatchers("/hello/ajax", "/hello/stream")`. The WebSocket
  upgrade is a GET, which CSRF protection does not check: its protection is PonySDK's Origin check
  (`ponysdk.websocket.*`).
- The rest is [security](security.md): the Content-Security-Policy, the Origin check, the path-safe resource serving.

`BootSecurityTest` runs these three cases.

## Graceful shutdown

Boot 4 stops with `server.shutdown=graceful` by default. PonySDK's drain ([ADR-0017](../decisions/0017-drain.md)) runs
first: each live page is told its context is over and reloads at its address, and the load balancer, which no longer sends
anything to a server that is stopping, sends it to another server. Then Boot stops accepting requests, and stops.

- `ponysdk.drain-timeout` is the time the pages are moved over (10 s by default). Keep it under Boot's
  `spring.lifecycle.timeout-per-shutdown-phase` (30 s) and under the platform's stop timeout (an ECS task's `stopTimeout`,
  a pod's `terminationGracePeriodSeconds`): see [deployment](deployment.md).
- `server.shutdown=immediate` or `ponysdk.drain-timeout=0`: no drain, the server stops at once.
- During the drain `/<name>/health` answers 503 `DRAINING`, for a balancer that follows health checks.
- With the actuator on the classpath, `/actuator/health` has a component `ponysdk`: UP, then OUT_OF_SERVICE (503) while the
  pages move. Without it, nothing is registered; `management.health.ponysdk.enabled=false` switches it off. A readiness
  probe follows it with Boot's group setting: `management.endpoint.health.group.readiness.include=readinessState,ponysdk`.

With one server alone the pages come back to it until it stops: a drain needs a second server.
