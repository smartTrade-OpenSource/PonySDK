[![codecov](https://codecov.io/gh/smartTrade-OpenSource/PonySDK/branch/master/graph/badge.svg)](https://codecov.io/gh/Nciaravola/PonySDK)
[![Actions Status](https://github.com/smartTrade-OpenSource/PonySDK/workflows/CI/badge.svg)](https://github.com/Nciaravola/PonySDK/actions)
![.github/workflows/release.yml](https://github.com/smartTrade-OpenSource/PonySDK/workflows/.github/workflows/release.yml/badge.svg?branch=master)

# PonySDK
PonySDK is an open source project and application that uses open source tools built on the Java platform to help you develop Web applications quickly and efficiently

It encapsulates a Web server (for now, Jetty) on the backend side, and use GWT on the frontend side.
So, with PonySDK, you will be able to write standard Java code for creating your Web application.

## Version 2 Features

- Use Websocket to communicate between the Jetty Web Server and HTML5 Browser compliant
- Be able to use all Javascript frameworks using PAddons

----

## Contents

[Browser compatibility](#browser-compatibility)

[Installation](#installation)

[Pony Driver](#pony-driver)

[Demo](#demo)

[Frequently asked questions](#frequently-asked-questions)

----

## Browser compatibility

- Chrome >= 37
- Firefox >= 33
- Safari >= 6
- Internet Explorer >= 11
- Edge >= 1

## Installation

```
Git version : https://github.com/smartTrade-OpenSource/PonySDK.git
Latest version : https://github.com/smartTrade-OpenSource/PonySDK/archive/master.zip
Released version : https://github.com/smartTrade-OpenSource/PonySDK/releases
```

## Pony Driver

Pony Driver is a low level Selenium-compatible terminal that can connect to a Pony server using only WebSocket protocol (unlike the web client that depends on both HTTP and WebSocket protocols).
The communication is based only on Pony protocol and is unaware of HTML, JavaScript, or any other web language. 
The driver can be found in the same jar as PonySDK, and can only be used to connect to a Pony server that is based on the exact same version.

### Dependencies

```gradle
compile 'com.ponysdk:ponysdk:2.8.12'
compile 'org.seleniumhq.selenium:selenium-api:3.14.0'
compile 'org.seleniumhq.selenium:selenium-java:3.14.0'
compile 'javax.websocket:javax.websocket-client-api:1.1'

runtime 'jakarta.json:jakarta.json-api:1.1.6'
runtime 'org.slf4j:slf4j-api:1.7.25'
runtime 'org.glassfish.tyrus:tyrus-client:1.15'
runtime 'org.glassfish.tyrus:tyrus-container-grizzly-client:1.15'
runtime 'org.glassfish.tyrus.ext:tyrus-extension-deflate:1.15'
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
$ gradlew runSampleSpring
```

Wait a little and you will have on the console, logs like this :

```
INFO  [ContextHandler] Started o.e.j.s.ServletContextHandler@6440112d{/sample,null,AVAILABLE}
INFO  [AbstractConnector] Started ServerConnector@4239156f{HTTP/1.1,[http/1.1]}{0.0.0.0:8081}
INFO  [AbstractConnector] Started ServerConnector@5a7fe64f{SSL,[ssl, http/1.1]}{0.0.0.0:8082}
```

Now you can go on http://localhost:8081/sample/ or https://localhost:8081/sample/ (SSL is activated by default)

## [Frequently asked questions](https://github.com/Nciaravola/PonySDK/wiki)