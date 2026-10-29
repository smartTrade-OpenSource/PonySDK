# Testing

## Widgets without a browser: PSuite

`com.ponysdk.test.PSuite` (in the `ponysdk` artifact, JUnit 6) gives each test a `UIContext` whose writes are
recorded: a test builds widgets, drives their handlers and asserts on what they would send.

```java
public class PriceLabelTest extends PSuite {

    @Test
    public void aTickChangesTheText() {
        final PLabel price = Element.newPLabel("1.08512");
        PWindow.getMain().add(price);
        price.setText("1.08514");
        assertEquals("1.08514", price.getText());
    }
}
```

Tests that extend `PSuite` run on the JUnit Platform (`useJUnitPlatform()` in Gradle).

## The Pony driver

A Selenium-compatible WebSocket driver for headless testing — no browser required.

```java
PonySDKWebDriver driver = new PonySDKWebDriver();
driver.get("ws://localhost:8081/sample/ws");

WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
wait.until(d -> d.findElement(By.cssSelector(".main TEXTBOX.login"))).sendKeys("admin");
wait.until(d -> d.findElement(By.tagName("BUTTON"))).click();
```

### Its dependencies

```gradle
implementation 'com.ponysdk:ponysdk:3.0.0-SNAPSHOT' // ./gradlew :ponysdk:publishToMavenLocal, then mavenLocal()
implementation 'org.seleniumhq.selenium:selenium-api:4.x'
implementation 'jakarta.websocket:jakarta.websocket-client-api:2.2.0'

runtimeOnly 'org.glassfish.tyrus:tyrus-client:2.2.0'
runtimeOnly 'org.glassfish.tyrus:tyrus-container-grizzly-client:2.2.0'
```

## In a browser

The sample's end-to-end tests drive the showcase with Playwright (`sample/tests/*.spec.js`); their header gives the
command, for instance `BASE_URL=http://localhost:8081/sample/ npx playwright test showcase-smoke`.
