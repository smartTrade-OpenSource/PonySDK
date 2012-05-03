
package com.ponysdk.test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppClassLoader;
import org.mortbay.jetty.webapp.WebAppContext;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.test.UiBuilderTestEntryPoint.RequestHandler;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PRootPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;

public class UiBuilderTest {

    private static final Logger log = LoggerFactory.getLogger(UiBuilderTest.class);

    private static Server webServer;
    private static WebDriver webDriver;
    private static PEventsListener eventsListener;

    private final Map<String, PWidget> widgetByDebugID = new HashMap<String, PWidget>();

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void runBeforeClass() throws Exception {
        log.info("Starting jetty webserver");

        // System.setProperty("webdriver.firefox.bin", "PATH TO/firefox.exe");

        eventsListener = new PEventsListener();

        webServer = new Server(5000);
        final WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/test");
        webapp.setDescriptor("test");
        webapp.setWar("src-core/test/resources/war");
        webapp.setExtractWAR(true);
        webapp.setParentLoaderPriority(true);
        webapp.setClassLoader(new WebAppClassLoader(UiBuilderTest.class.getClassLoader(), webapp));

        webServer.addHandler(webapp);
        webServer.start();

        webDriver = new FirefoxDriver();

        webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        webDriver.manage().deleteAllCookies();

        webDriver.navigate().to("http://localhost:5000/test");
    }

    @AfterClass
    public static void runAfterClass() throws Exception {
        log.info("Closing browser");
        webDriver.quit();

        log.info("Stopping jetty webserver");
        webServer.stop();
    }

    @Before
    public void beforeTest() {
        log.info(name.getMethodName());
    }

    @After
    public void afterTest() {
        widgetByDebugID.clear();
        sleep(1000);
    }

    @Test
    public void testPAnchor() {

        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PAnchor anchor = new PAnchor("An anchor");
                anchor.ensureDebugId("anchor1");
                PRootPanel.get().add(anchor);

                register(anchor);
            }
        });

        WebElement element = findElementById("anchor1");
        Assert.assertEquals("An anchor", element.getText());

        // update text
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PAnchor anchor = get("anchor1");
                anchor.setText("New text of the anchor");
            }
        });

        element = findElementById("anchor1");
        Assert.assertEquals("New text of the anchor", element.getText());

        // update html
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PAnchor anchor = get("anchor1");
                anchor.setHTML("Anchor <font color='red'>with pure html</font>");
            }
        });

        element = findElementById("anchor1");
        Assert.assertEquals("Anchor with pure html", element.getText());

        final WebElement font = element.findElement(By.tagName("font"));
        final String color = font.getAttribute("color");
        Assert.assertEquals("red", color);

        // check server fields
        final PAnchor anchor = get("anchor1");
        Assert.assertEquals("New text of the anchor", anchor.getText());
        Assert.assertEquals("Anchor <font color='red'>with pure html</font>", anchor.getHTML());
    }

    @Test
    public void testPButtonBase() {

        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PButton button1 = new PButton("A button");
                button1.ensureDebugId("button1");
                PRootPanel.get().add(button1);

                register(button1);
            }
        });

        WebElement element = findElementById("button1");
        Assert.assertEquals("A button", element.getText());

        // update text
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PButton button1 = get("button1");
                button1.setText("New text of the button");
            }
        });

        element = findElementById("button1");
        Assert.assertEquals("New text of the button", element.getText());

        // update html
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PButton button1 = get("button1");
                button1.setHTML("Button <font color='red'>with pure html</font>");
            }
        });

        element = findElementById("button1");
        Assert.assertEquals("Button with pure html", element.getText());

        final WebElement font = element.findElement(By.tagName("font"));
        final String color = font.getAttribute("color");
        Assert.assertEquals("red", color);

        // check server fields
        final PButton anchor = get("button1");
        Assert.assertEquals("New text of the button", anchor.getText());
        Assert.assertEquals("Button <font color='red'>with pure html</font>", anchor.getHTML());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPCheckBox() {

        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PCheckBox checkbox1 = new PCheckBox("A checkbox");
                checkbox1.ensureDebugId("checkbox1");
                PRootPanel.get().add(checkbox1);
                checkbox1.addValueChangeHandler(eventsListener);
                register(checkbox1);
            }
        });

        final WebElement element = findElementById("checkbox1");
        Assert.assertEquals("A checkbox", element.getText());
        element.click();

        // check event
        final PValueChangeEvent<Boolean> e1 = eventsListener.poll();
        Assert.assertEquals(Boolean.TRUE, e1.getValue());

        // check server-side value
        final PCheckBox checkbox1 = get("checkbox1");
        Assert.assertEquals(Boolean.TRUE, checkbox1.getValue());

        // toggle
        element.click();

        // check event
        final PValueChangeEvent<Boolean> e2 = eventsListener.poll();
        Assert.assertEquals(Boolean.FALSE, e2.getValue());

        // check server-side value
        Assert.assertEquals(Boolean.FALSE, checkbox1.getValue());
    }

    protected void register(final PWidget widget) {
        widgetByDebugID.put(widget.getDebugID(), widget);
    }

    @SuppressWarnings("unchecked")
    protected <T> T get(final String debugID) {
        return (T) widgetByDebugID.get(debugID);
    }

    private static WebElement findElementById(final String id) {

        sleep(200);

        return webDriver.findElement(By.id("gwt-debug-" + id));
    }

    private static void updateUI(final RequestHandler handler) {
        UiBuilderTestEntryPoint.setRequestHandler(handler);
        final WebElement element = findElementById("startingpoint");
        element.click();
    }

    private static void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {}
    }
}
