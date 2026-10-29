# Deployment

A PonySDK server holds each screen in its JVM and talks to its page over one WebSocket that stays open. Whatever
runs in front of it carries long connections to the same JVM; what stops it lets it move its pages first. This page
says what fits, on AWS for the examples, and what to set. Why the drain is built so:
[ADR-0017](../decisions/0017-drain.md).

## What fits, what does not

| Fits | Does not |
|---|---|
| A load balancer that forwards WebSockets: AWS ALB or NLB, nginx, HAProxy, Envoy | A gateway that ends the WebSocket and calls a backend per message: API Gateway WebSocket APIs (they also cut a connection after 2 h, and after 10 min idle) |
| Long-running JVMs: ECS on Fargate or EC2, EC2 Auto Scaling, Kubernetes | Functions: Lambda, any platform that freezes or recycles the process between requests |
| TLS ended at the load balancer, a CDN in front for the static files (CloudFront forwards WebSockets) | |

Nothing else is needed: the state is in the JVM, no shared cache or database for the screens.

## A small deployment on AWS

Two tasks of the application behind an Application Load Balancer, in two availability zones: ECS on Fargate (Fargate
Spot for the lowest price), or two small EC2 instances (Graviton). The order of magnitude is a few tens of dollars a
month for the balancer and two small tasks; the AWS Pricing Calculator gives it for a region and a size.

| Setting | Value | Why |
|---|---|---|
| Target group stickiness | On (load-balancer cookie) | A page that lost its connection reconnects to the server that holds its context ([reconnection](reconnection.md)) |
| Health check path | `/<applicationContextName>/health` | 200 `UP` once the server is up, 503 `DRAINING` while it moves its pages; no session, not cached, unlike the bootstrap page the server builds for each request |
| ALB idle timeout | The default, 60 s | A silent client is pinged every half heartbeat period, 2.5 s by default ([ADR-0006](../decisions/0006-liveness-by-ping.md)): the connection is never idle that long |
| `PonySDKServer.setUseSSL(false)` | | TLS ends at the balancer |
| `setSessionCookieSecure(true)` | The default | The browser speaks HTTPS to the balancer |
| `ApplicationConfiguration.setReconnectionTimeoutMs` | 10 000 or more | A network cut resumes the same context instead of a new page |
| `PonySDKServer.setDrainMillis` | Below the deregistration delay and the stop timeout | See below |

## A server that stops

A deployment, a scale-in or a spot interruption stops a server: the platform takes it out of the target group, sends
SIGTERM, and kills it after its stop timeout. Without a drain, its pages find it gone and reload, all at once.

```java
server.setDrainMillis(25_000);
```

With Spring, the property of the server bean:

```xml
<bean name="ponySDKServer" class="com.ponysdk.impl.main.PonySDKServer" init-method="start">
    <property name="drainMillis" value="25000" />
</bean>
```

On SIGTERM the server destroys its contexts one by one over four fifths of that time: each page is told, reloads at
its address, and the balancer, which no longer sends anything to a server it drains, sends it to another one. Then
the server stops. Set it below both:

- the target group's deregistration delay (300 s by default; 30 s is plenty with a drain of 25 s);
- the platform's stop timeout: an ECS task's `stopTimeout` (30 s by default, 120 s at most on Fargate), a
  Kubernetes pod's `terminationGracePeriodSeconds`, the two minutes of a spot interruption.

Meanwhile `/health` answers 503: a balancer that only follows health checks (an EC2 Auto Scaling group behind an
ALB, a proxy without a deregistration step) stops sending pages to the server too.

On one server alone the reloaded pages come back to it until it stops: a drain needs a second server.

## What a moved page keeps

A move is a page reload on another server: what the page's address and the browser keep survives, what the screen
held in the JVM does not.

| Kept | Not kept |
|---|---|
| The history token (`uiContext.getHistory().newItem(token, false)`): a selection, a tab, a filter of this tab | What a user typed and did not send |
| Cookies: preferences of the browser, written with `uiContext.getCookies().setCookie`, read at the start from `uiContext.getRequestCookies()` (those of the WebSocket handshake) | The objects of the screen, its subscriptions |

The Reconnection page of the showcase shows it: click a price in its frame, then Move server; the new session
restores the price from its address.

A server that crashes has no time to drain: its pages reconnect, the balancer sends them to another server, which
does not know their context, and they reload as after a move.
