# Quick start

The code below is `sample/src/main/java/com/ponysdk/sample/quickstart/`, which `QuickStartTest` starts:
`./gradlew :sample:runQuickStart` runs it from this repository.

## Step 1: Add the dependency

PonySDK 3 is not on Maven Central yet: install it in your local repository from this one
(`./gradlew :ponysdk:publishToMavenLocal`), then:

**Gradle:**
```groovy
repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {
    implementation 'com.ponysdk:ponysdk:3.0.0-SNAPSHOT'
}
```

**Maven:**
```xml
<dependency>
    <groupId>com.ponysdk</groupId>
    <artifactId>ponysdk</artifactId>
    <version>3.0.0-SNAPSHOT</version>
</dependency>
```

PonySDK 3 needs Java 25.

## Step 2: Write the screen, an EntryPoint

```java
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWindow;
import com.ponysdk.core.ui.main.EntryPoint;

public class HelloWorldEntryPoint implements EntryPoint {

    @Override
    public void start(final UIContext uiContext) {
        final PLabel label = Element.newPLabel("Hello PonySDK!");
        final PTextBox name = Element.newPTextBox();
        final PButton button = Element.newPButton("Say hello");

        // Runs on the server: the click comes over the WebSocket, the new text goes back
        button.addClickHandler(event -> label.setText("Hello, " + (name.getText().isBlank() ? "World" : name.getText()) + "!"));

        final PFlowPanel root = Element.newPFlowPanel();
        root.add(label);
        root.add(name);
        root.add(button);
        PWindow.getMain().add(root);
    }
}
```

`start` runs once per browser tab, on the server: each tab has its own `UIContext` and its own widgets.

## Step 3: Start the server

```java
import com.ponysdk.core.server.application.ApplicationConfiguration;
import com.ponysdk.impl.java.server.JavaApplicationManager;
import com.ponysdk.impl.main.PonySDKServer;

public final class HelloWorldMain {

    public static void main(final String[] args) throws Exception {
        final ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setApplicationName("Hello");
        configuration.setApplicationContextName("hello"); // the application's URL: http://localhost:8080/hello/
        configuration.setEntryPointClass(HelloWorldEntryPoint.class);

        final JavaApplicationManager applicationManager = new JavaApplicationManager();
        applicationManager.setConfiguration(configuration);

        final PonySDKServer server = new PonySDKServer();
        server.setApplicationManager(applicationManager);
        server.setPort(8080);
        server.setUseSSL(false); // plain HTTP on this machine; in production, HTTPS with a key store
        server.setSessionCookieSecure(false); // a Secure cookie is not sent over plain HTTP
        server.start();
    }
}
```

HTTPS is on by default: without `setUseSSL(false)`, the server needs a key store (`setSslKeyStoreFile`).

## Step 4: Open it

Run `HelloWorldMain`, then open http://localhost:8080/hello/ (the trailing slash matters). Type a name and press
*Say hello*: the label changes, set by the server.

## What you just built

```
HelloWorldMain ── PonySDKServer ── serves /hello/: the page and terminal.js, the browser side
                                   └── a WebSocket per tab ── HelloWorldEntryPoint.start(uiContext)
                                                                ├── PLabel, PTextBox, PButton: Java objects
                                                                └── a click handler, run on the server
```

## Next Steps

| Want to... | See section |
|------------|-------------|
| Understand the architecture | [Core Philosophy](../../ARCHITECTURE.md#2-core-philosophy) |
| Learn about performance | [Performance Architecture](../../ARCHITECTURE.md#6-performance-architecture) |
| Add server push | [Threading Model](../../ARCHITECTURE.md#9-threading-model) |
| Integrate Web Components | [Web Components](web-components.md) |
| Integrate custom JS | [Custom JavaScript (PAddOn)](addons.md) |
| Show the application in another page | [Embedding in another page](embedding.md) |
| Configure the server | [Configuration Reference](configuration.md) |
