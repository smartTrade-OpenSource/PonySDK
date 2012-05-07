
package com.ponysdk.test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
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
import com.ponysdk.ui.server.basic.PComplexPanel;
import com.ponysdk.ui.server.basic.PDateBox;
import com.ponysdk.ui.server.basic.PDialogBox;
import com.ponysdk.ui.server.basic.PDisclosurePanel;
import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PRootPanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PCloseEvent;
import com.ponysdk.ui.server.basic.event.POpenEvent;
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
    public static void runBeforeClass() throws Throwable {

        try {
            log.info("Starting jetty webserver");

            // System.setProperty("webdriver.firefox.bin", "PATH/firefox.exe");
            // System.setProperty("webdriver.firefox.bin",
            // "C:/Program Files (x86)/Mozilla Firefox 9/firefox.exe");
            // System.setProperty("webdriver.chrome.driver",
            // "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe");

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
            // webDriver = new ChromeDriver();

            webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
            webDriver.manage().deleteAllCookies();

            webDriver.navigate().to("http://localhost:5000/test");
        } catch (final Throwable e) {
            log.error("", e);
            throw e;
        }
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
        log.info("Running #" + name.getMethodName());
    }

    @After
    public void afterTest() {
        eventsListener.clear();
        widgetByDebugID.clear();
        sleep(200);
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

        WebElement element = findElementById("checkbox1-label");
        Assert.assertEquals("A checkbox", element.getText());
        element = findElementById("checkbox1-input");
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

    @Test
    public void testPComplexPanel() {

        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PComplexPanel complexPanel1 = new PVerticalPanel();
                complexPanel1.ensureDebugId("complexPanel1");
                PRootPanel.get().add(complexPanel1);
                register(complexPanel1);
            }
        });

        WebElement element = findElementById("complexPanel1");
        final PComplexPanel complexPanel1 = get("complexPanel1");

        // add child
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PComplexPanel complexPanel1 = get("complexPanel1");
                complexPanel1.add(new PAnchor("child1"));
                complexPanel1.add(new PAnchor("child3"));
            }
        });

        element = findElementById("complexPanel1");
        List<WebElement> anchors = element.findElements(By.tagName("a"));
        Assert.assertEquals(2, anchors.size());
        Assert.assertEquals(2, complexPanel1.getWidgetCount());

        // insert child
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PComplexPanel complexPanel1 = get("complexPanel1");
                final PAnchor child2 = new PAnchor("child2");
                child2.ensureDebugId("child2");
                complexPanel1.insert(child2, 1);
                register(child2);
            }
        });

        element = findElementById("complexPanel1");
        anchors = element.findElements(By.tagName("a"));
        Assert.assertEquals(3, anchors.size());
        Assert.assertEquals("child1", anchors.get(0).getText());
        Assert.assertEquals("child2", anchors.get(1).getText());
        Assert.assertEquals("child3", anchors.get(2).getText());

        Assert.assertEquals(3, complexPanel1.getWidgetCount());
        Assert.assertEquals("child1", ((PAnchor) complexPanel1.getWidget(0)).getText());
        Assert.assertEquals("child2", ((PAnchor) complexPanel1.getWidget(1)).getText());
        Assert.assertEquals("child3", ((PAnchor) complexPanel1.getWidget(2)).getText());

        // remove child
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PComplexPanel complexPanel1 = get("complexPanel1");
                final PAnchor child2 = get("child2");
                complexPanel1.remove(child2);
            }
        });

        element = findElementById("complexPanel1");
        anchors = element.findElements(By.tagName("a"));
        Assert.assertEquals(2, anchors.size());
        Assert.assertEquals("child1", anchors.get(0).getText());
        Assert.assertEquals("child3", anchors.get(1).getText());

        Assert.assertEquals(2, complexPanel1.getWidgetCount());
        Assert.assertEquals("child1", ((PAnchor) complexPanel1.getWidget(0)).getText());
        Assert.assertEquals("child3", ((PAnchor) complexPanel1.getWidget(1)).getText());
    }

    // TODO PCookies

    @Test
    public void testPDateBox() {

        final String datePattern = "yyyy-MM-dd";
        final SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
        final Calendar calendar = new GregorianCalendar(2012, 10, 27);
        final Date date = calendar.getTime();
        final String dateAsString = dateFormat.format(date);

        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PLabel label1 = new PLabel("Date test");
                label1.ensureDebugId("label1");
                PRootPanel.get().add(label1);
                register(label1);

                final PDateBox dateBox1 = new PDateBox(new SimpleDateFormat(datePattern));
                dateBox1.ensureDebugId("dateBox1");
                PRootPanel.get().add(dateBox1);
                register(dateBox1);
            }
        });

        WebElement element = findElementById("dateBox1");
        final PDateBox dateBox1 = get("dateBox1");
        Assert.assertEquals(datePattern, dateBox1.getDateFormat().toPattern());

        // update date
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PDateBox dateBox1 = get("dateBox1");
                dateBox1.setValue(date);
            }
        });

        element = findElementById("dateBox1");
        Assert.assertEquals(dateAsString, element.getAttribute("value"));
        Assert.assertEquals(dateAsString, dateBox1.getDisplayedValue());

        // add value change handler
        updateUI(new RequestHandler() {

            @SuppressWarnings("unchecked")
            @Override
            public void onRequest() {
                final PDateBox dateBox1 = get("dateBox1");
                dateBox1.addValueChangeHandler(eventsListener);
            }
        });

        element = findElementById("dateBox1");
        element.clear();
        final PValueChangeEvent<Date> e1 = eventsListener.poll();
        Assert.assertNull(e1.getValue());

        element.sendKeys(new String("2012-10-30"));
        element = findElementById("label1");
        element.click();

        final PValueChangeEvent<Date> e2 = eventsListener.poll();
        Assert.assertEquals("2012-10-30", dateFormat.format(e2.getValue()));
        Assert.assertEquals("2012-10-30", dateBox1.getDisplayedValue());
    }

    @Test
    public void testPDialogBox() {

        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PDialogBox dialogBox1 = new PDialogBox();
                dialogBox1.ensureDebugId("dialogBox1");
                dialogBox1.show();
                register(dialogBox1);
            }
        });

        findElementById("dialogBox1");

        // set caption
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PDialogBox dialogBox1 = get("dialogBox1");
                dialogBox1.setCaption("The Caption");
            }
        });

        final WebElement caption = findElementById("dialogBox1-caption");
        Assert.assertEquals("The Caption", caption.getText());

        // add close button
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PDialogBox dialogBox1 = get("dialogBox1");
                final PButton close1 = new PButton("Close");
                close1.ensureDebugId("close1");
                dialogBox1.setWidget(close1);
                dialogBox1.addCloseHandler(eventsListener);
                close1.addClickHandler(new PClickHandler() {

                    @Override
                    public void onClick(final PClickEvent event) {
                        dialogBox1.hide();
                    }
                });
                register(close1);
            }
        });

        final WebElement close1 = findElementById("close1");
        close1.click();

        // check that we received close event
        final PCloseEvent e1 = eventsListener.poll();
        Assert.assertNotNull(e1);
    }

    @Test
    public void testPDisclosurePanel() {

        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PDisclosurePanel disclosurePanel1 = new PDisclosurePanel("A disclosure panel");
                disclosurePanel1.ensureDebugId("disclosurePanel1");
                PRootPanel.get().add(disclosurePanel1);
                register(disclosurePanel1);
            }
        });

        WebElement disclosure = findElementById("disclosurePanel1");
        Assert.assertTrue(disclosure.getAttribute("class").contains("gwt-DisclosurePanel-closed"));
        final PDisclosurePanel disclosurePanel1 = get("disclosurePanel1");
        Assert.assertEquals(false, disclosurePanel1.isOpen());

        WebElement disclosureHeader = findElementById("disclosurePanel1-header");
        Assert.assertEquals("A disclosure panel", disclosureHeader.getText());

        // set content
        // open / close
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PDisclosurePanel disclosurePanel1 = get("disclosurePanel1");
                final PLabel label = new PLabel("Text");
                label.ensureDebugId("label1");
                disclosurePanel1.setContent(label);
                disclosurePanel1.addOpenHandler(eventsListener);
                disclosurePanel1.addCloseHandler(eventsListener);
                register(disclosurePanel1);
            }
        });

        disclosureHeader = findElementById("disclosurePanel1-header");
        disclosureHeader.click();
        final POpenEvent e2 = eventsListener.poll();
        Assert.assertNotNull(e2);
        Assert.assertTrue(disclosurePanel1.isOpen());

        disclosure = findElementById("disclosurePanel1");
        final WebElement content = findElementById(disclosure, "label1");
        Assert.assertEquals("Text", content.getText());

        disclosureHeader.click();
        final PCloseEvent e1 = eventsListener.poll();
        Assert.assertNotNull(e1);
        Assert.assertTrue(!disclosurePanel1.isOpen());

        // server side open
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PDisclosurePanel disclosurePanel1 = get("disclosurePanel1");
                disclosurePanel1.setOpen(true);
            }
        });

        disclosure = findElementById("disclosurePanel1");
        Assert.assertTrue(disclosure.getAttribute("class").contains("gwt-DisclosurePanel-open"));

        // server side close
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PDisclosurePanel disclosurePanel1 = get("disclosurePanel1");
                disclosurePanel1.setOpen(false);
            }
        });

        disclosure = findElementById("disclosurePanel1");
        Assert.assertTrue(disclosure.getAttribute("class").contains("gwt-DisclosurePanel-closed"));

    }

    @Test
    public void testPElement() {

        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PElement ul1 = new PElement("ul");
                ul1.ensureDebugId("ul1");
                final PElement li1 = new PElement("li");
                li1.setInnerText("1rst element");
                final PElement li2 = new PElement("li");
                li2.setInnerHTML("<font color='red'>2d</font> element");
                final PLabel label1 = new PLabel("A widget");
                ul1.add(li1);
                ul1.add(li2);
                ul1.add(label1);
                PRootPanel.get().add(ul1);
                register(ul1);
            }
        });

        final WebElement ul1 = findElementById("ul1");
        final List<WebElement> liElements = ul1.findElements(By.tagName("li"));
        Assert.assertEquals(2, liElements.size());
        Assert.assertEquals("1rst element", liElements.get(0).getText());
        Assert.assertEquals("2d element", liElements.get(1).getText());

        final WebElement font = liElements.get(1).findElement(By.tagName("font"));
        final String color = font.getAttribute("color");
        Assert.assertEquals("red", color);

        final List<WebElement> divElements = ul1.findElements(By.tagName("div"));
        Assert.assertEquals(1, divElements.size());

    }

    protected void register(final PWidget widget) {
        widgetByDebugID.put(widget.getDebugID(), widget);
    }

    @SuppressWarnings("unchecked")
    protected <T> T get(final String debugID) {
        return (T) widgetByDebugID.get(debugID);
    }

    private static WebElement findElementById(final String id) {
        return webDriver.findElement(By.id("gwt-debug-" + id));
    }

    private static WebElement findElementById(final WebElement element, final String id) {
        return element.findElement(By.id("gwt-debug-" + id));
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
