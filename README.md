[![codecov](https://codecov.io/gh/Nciaravola/PonySDK/branch/master/graph/badge.svg)](https://codecov.io/gh/Nciaravola/PonySDK)
[![Actions Status](https://github.com/Nciaravola/PonySDK/workflows/CI/badge.svg)](https://github.com/Nciaravola/PonySDK/actions)
![.github/workflows/release.yml](https://github.com/Nciaravola/PonySDK/workflows/.github/workflows/release.yml/badge.svg?branch=master)

# PonySDK
PonySDK is an open source project and application that uses open source tools built on the Java platform to help you develop Web applications quickly and efficiently

It encapsulates a Web server (Jetty 12) on the backend side, and uses GWT 2.13.0 on the frontend side.
With PonySDK, you write standard Java code to create your Web application — no JavaScript required.

## Tech Stack

| Component | Version |
|-----------|---------|
| Java (server) | 21 (virtual threads) |
| Java (terminal/GWT) | 17 (sourceLevel 17) |
| Jetty | 12.0.18 (EE10 / Jakarta EE) |
| GWT | 2.13.0 (`org.gwtproject`) |
| elemental2 | 1.2.1 (dom, core, webstorage) |
| Spring | 6.2.9 |
| Selenium | 4.27.0 |
| Gradle | 8.14.4 |

## Key Features

- WebSocket binary protocol between Jetty 12 and HTML5 browsers
- String Dictionary for bandwidth optimization (83% reduction on repeated strings)
- 5-level incremental protocol: equals check → string dictionary → JSON merge-patch → binary protocol → WebSocket deflate
- Web Component integration (PWebComponent) with PropertyHandle API (on-heap, off-heap, stateless)
- Virtual threads (Java 21) for scalable concurrent UIContexts
- JsInterop / elemental2 terminal (no more legacy `gwt-elemental`)
- PAddons for integrating any JavaScript framework

----

## Contents

[Browser compatibility](#browser-compatibility)

[Installation](#installation)

[Pony Driver](#pony-driver)

[Demo](#demo)

[Frequently asked questions](#frequently-asked-questions)

----

## Browser compatibility

- Chrome >= 60
- Firefox >= 55
- Safari >= 11
- Edge >= 79 (Chromium-based)

## Installation

```
Git version : https://github.com/Nciaravola/PonySDK.git
Latest version : https://github.com/Nciaravola/PonySDK/archive/master.zip
Released version : https://github.com/Nciaravola/PonySDK/releases
```

### Build

```sh
# Compile
./gradlew :ponysdk:compileJava

# Run tests
./gradlew :ponysdk:test

# GWT compile (terminal → JavaScript)
./gradlew :ponysdk:gwtc

# Full build
./gradlew :ponysdk:build
```

## Pony Driver

Pony Driver is a low level Selenium-compatible terminal that can connect to a Pony server using only WebSocket protocol (unlike the web client that depends on both HTTP and WebSocket protocols).
The communication is based only on Pony protocol and is unaware of HTML, JavaScript, or any other web language. 
The driver can be found in the same jar as PonySDK, and can only be used to connect to a Pony server that is based on the exact same version.

### Dependencies

```gradle
implementation 'com.ponysdk:ponysdk:2.8.99.6'
implementation 'org.seleniumhq.selenium:selenium-api:4.27.0'
implementation 'org.seleniumhq.selenium:selenium-java:4.27.0'
implementation 'jakarta.websocket:jakarta.websocket-client-api:2.2.0'

runtimeOnly 'jakarta.json:jakarta.json-api:2.0.2'
runtimeOnly 'org.slf4j:slf4j-api:2.0.17'
runtimeOnly 'org.glassfish.tyrus:tyrus-client:2.2.0'
runtimeOnly 'org.glassfish.tyrus:tyrus-container-grizzly-client:2.2.0'
runtimeOnly 'org.glassfish.tyrus.ext:tyrus-extension-deflate:2.2.0'
```

### Usage

An instance of PonySDKWebDriver can be used to connect to a Pony server.
```java
PonySDKWebDriver driver = new PonySDKWebDriver();
driver.get("ws://localhost:8081/sample/ws");
```

Once connected, a WebDriverWait can be used to wait for certain widgets to become ready before proceeding to action.
```java
WebDriverWait wait = new WebDriverWait(driver, 10L); //10 == timeOut in seconds
```

To select one or multiple widgets from the tree of available elements, the following find criteria can be used :

**id** : matches the id attribute.

**name** : matches the name attribute.

**class name** : all given class names (space separated) must belong to the class names of the widget.

**tag name** : matches the widget type as defined by Pony in WidgetType enum (doesn't necessarily match the html tag name).

**css selector** : matches the widget type and/or the class names. Class names must be preceeded with a dot character. The space character can be used to select descendant widgets. The > character can be used to select direct children widgets.
```java
wait.until(webDriver -> webDriver.findElement(By.className("arrow left")));
wait.until(webDriver -> webDriver.findElement(By.cssSelector(".main .auth>TEXTBOX.login"))).sendKeys("admin");
wait.until(webDriver -> webDriver.findElement(By.tagName("BUTTON"))).click();
```

An xml file contaning the entire tree of available elements can be generated. It can be useful for dubugging purposes.
```java
try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File("pony_tree.xml")) {
	driver.printAsXml(writer);
}
```

## Demo

For testing PonySDK, there is a sample that launched an embedded Jetty Server and served a demo page.

Follow the steps :

```sh
$ git clone https://github.com/Nciaravola/PonySDK.git
$ cd PonySDK
$ ./gradlew :sample:runSampleSpring --no-configuration-cache
```

Wait a little and you will have on the console, logs like this :

```
INFO  [ContextHandler] Started o.e.j.s.ServletContextHandler@...{/sample,null,AVAILABLE}
INFO  [AbstractConnector] Started ServerConnector@...{HTTP/1.1,[http/1.1]}{0.0.0.0:8081}
INFO  [AbstractConnector] Started ServerConnector@...{SSL,[ssl, http/1.1]}{0.0.0.0:8082}
```

Now you can go on http://localhost:8081/sample/ or https://localhost:8082/sample/ (SSL is activated by default)

## [Frequently asked questions](https://github.com/Nciaravola/PonySDK/wiki)
