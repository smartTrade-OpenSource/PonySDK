[![Build Status](https://travis-ci.org/Nciaravola/PonySDK.svg?branch=master)](https://travis-ci.org/Nciaravola/PonySDK)

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
Git version : https://github.com/Nciaravola/PonySDK.git
Latest version : https://github.com/Nciaravola/PonySDK/archive/master.zip
Released version : https://github.com/Nciaravola/PonySDK/releases
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
