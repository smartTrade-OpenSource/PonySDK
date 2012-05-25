
package com.ponysdk.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.event.PBusinessEvent;
import com.ponysdk.core.event.PEventHandler;
import com.ponysdk.test.UiBuilderTestEntryPoint.RequestHandler;
import com.ponysdk.ui.server.basic.PAnchor;
import com.ponysdk.ui.server.basic.PButton;
import com.ponysdk.ui.server.basic.PCheckBox;
import com.ponysdk.ui.server.basic.PCommand;
import com.ponysdk.ui.server.basic.PComplexPanel;
import com.ponysdk.ui.server.basic.PDateBox;
import com.ponysdk.ui.server.basic.PDialogBox;
import com.ponysdk.ui.server.basic.PDisclosurePanel;
import com.ponysdk.ui.server.basic.PElement;
import com.ponysdk.ui.server.basic.PFlexTable;
import com.ponysdk.ui.server.basic.PFlowPanel;
import com.ponysdk.ui.server.basic.PFocusPanel;
import com.ponysdk.ui.server.basic.PGrid;
import com.ponysdk.ui.server.basic.PHTML;
import com.ponysdk.ui.server.basic.PHorizontalPanel;
import com.ponysdk.ui.server.basic.PLabel;
import com.ponysdk.ui.server.basic.PListBox;
import com.ponysdk.ui.server.basic.PMenuBar;
import com.ponysdk.ui.server.basic.PRadioButton;
import com.ponysdk.ui.server.basic.PRootPanel;
import com.ponysdk.ui.server.basic.PScheduler;
import com.ponysdk.ui.server.basic.PScheduler.RepeatingCommand;
import com.ponysdk.ui.server.basic.PScript;
import com.ponysdk.ui.server.basic.PScript.ExecutionCallback;
import com.ponysdk.ui.server.basic.PSuggestBox;
import com.ponysdk.ui.server.basic.PSuggestOracle.PSuggestion;
import com.ponysdk.ui.server.basic.PTabPanel;
import com.ponysdk.ui.server.basic.PVerticalPanel;
import com.ponysdk.ui.server.basic.PWidget;
import com.ponysdk.ui.server.basic.event.PBeforeSelectionEvent;
import com.ponysdk.ui.server.basic.event.PBlurEvent;
import com.ponysdk.ui.server.basic.event.PChangeEvent;
import com.ponysdk.ui.server.basic.event.PClickEvent;
import com.ponysdk.ui.server.basic.event.PClickHandler;
import com.ponysdk.ui.server.basic.event.PCloseEvent;
import com.ponysdk.ui.server.basic.event.PFocusEvent;
import com.ponysdk.ui.server.basic.event.PKeyPressEvent;
import com.ponysdk.ui.server.basic.event.PKeyUpEvent;
import com.ponysdk.ui.server.basic.event.PMouseOverEvent;
import com.ponysdk.ui.server.basic.event.POpenEvent;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PValueChangeEvent;
import com.ponysdk.ui.terminal.basic.PHorizontalAlignment;
import com.ponysdk.ui.terminal.basic.PVerticalAlignment;

public class UiBuilderTest {

    @SuppressWarnings("rawtypes")
    private class PTestEvent extends PBusinessEvent {

        public PTestEvent(final Object sourceComponent, final String msg) {
            super(sourceComponent);
            setBusinessMessage(msg);
        }

        @Override
        public Type getAssociatedType() {
            return null;
        }

        @Override
        protected void dispatch(final PEventHandler handler) {}

    }

    private class PTestCommand implements PCommand {

        private final String msg;
        private final PEventsListener listener;

        public PTestCommand(final PEventsListener listenr, final String msg) {
            this.listener = listenr;
            this.msg = msg;
        }

        @Override
        public void execute() {
            listener.stackCommandResult(new PTestEvent(this, msg));
        }

    }

    private static final Logger log = LoggerFactory.getLogger(UiBuilderTest.class);

    private static Server webServer;
    private static WebDriver webDriver;
    private static PEventsListener eventsListener;

    private final Map<String, PWidget> widgetByDebugID = new HashMap<String, PWidget>();

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void runBeforeClass() throws Throwable {

        log.info("Starting jetty webserver");
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

        final Properties testProperties = loadProperties();

        try {
            webDriver = buildWebDriver(testProperties);
            webDriver.manage().timeouts().implicitlyWait(2, TimeUnit.SECONDS);
            webDriver.manage().deleteAllCookies();

            webDriver.navigate().to("http://localhost:5000/test");
        } catch (final Throwable e) {
            log.error("", e);
            throw e;
        }
    }

    private static Properties loadProperties() {
        final Properties testProperties = new Properties();
        try {
            final String homeDirectory = System.getProperty("user.home");
            final File propsFile = new File(homeDirectory, "ponysdk-test.properties");
            final InputStream is = new FileInputStream(propsFile);
            testProperties.load(is);

            // webdriver.firefox.bin=C:/Program Files (x86)/Mozilla Firefox 9/firefox.exe
            // webdriver.chrome.driver=C:/Program Files (x86)/Google//Chrome/Application/chrome.exe
            System.getProperties().putAll(testProperties);

        } catch (final Throwable e) {
            log.info("Failed to load properties from #user.home/ponysdk-test.properties");
        }
        return testProperties;
    }

    private static WebDriver buildWebDriver(final Properties testProperties) {

        // webDriver = new ChromeDriver();

        final FirefoxProfile profile = new FirefoxProfile();
        profile.setEnableNativeEvents(false);
        final WebDriver driver = new FirefoxDriver(profile);
        return driver;
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
        eventsListener = new PEventsListener();
    }

    @After
    public void afterTest() {
        widgetByDebugID.clear();
        sleep(5);
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
                final PVerticalPanel complexPanel1 = get("complexPanel1");
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

    public void testPFlexTable() {

        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PFlexTable flexTable1 = new PFlexTable();
                flexTable1.ensureDebugId("flexTable1");
                final PLabel cell11 = new PLabel("Cell_1_1");
                cell11.ensureDebugId("cell11");
                flexTable1.setWidget(0, 0, new PLabel("Cell_0_0"));
                flexTable1.setWidget(0, 1, new PLabel("Cell_0_1"));
                flexTable1.setWidget(1, 0, new PLabel("Cell_1_0"));
                flexTable1.setWidget(1, 1, cell11);
                flexTable1.setBorderWidth(1);
                flexTable1.setCellPadding(2);
                flexTable1.setCellSpacing(3);
                PRootPanel.get().add(flexTable1);
                register(flexTable1);
                register(cell11);
            }
        });

        WebElement flexTable1 = findElementById("flexTable1");
        Assert.assertEquals(flexTable1.getAttribute("border"), "1");
        Assert.assertEquals(flexTable1.getAttribute("cellPadding"), "2");
        Assert.assertEquals(flexTable1.getAttribute("cellSpacing"), "3");

        List<WebElement> rows = flexTable1.findElements(By.tagName("tr"));
        Assert.assertEquals(rows.size(), 2);
        List<WebElement> cells = flexTable1.findElements(By.tagName("td"));
        Assert.assertEquals(cells.size(), 4);
        Assert.assertEquals(cells.get(0).getText(), "Cell_0_0");

        final PFlexTable pFlexTable1 = get("flexTable1");
        Assert.assertEquals(2, pFlexTable1.getRowCount());
        Assert.assertEquals(2, pFlexTable1.getCellCount(0));

        // clear cell, insert new elements
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PFlexTable flexTable1 = get("flexTable1");
                final PLabel cell11 = get("cell11");
                flexTable1.remove(cell11);
                flexTable1.insertRow(0);
                flexTable1.setWidget(0, 0, new PLabel("Cell_2_0"));
                flexTable1.setWidget(0, 1, new PLabel("Cell_2_1"));
                flexTable1.setWidget(3, 0, new PLabel("Cell_3_0"));
                flexTable1.setWidget(3, 1, new PLabel("Cell_3_1"));
            }
        });

        flexTable1 = findElementById("flexTable1");
        rows = flexTable1.findElements(By.tagName("tr"));
        Assert.assertEquals(rows.size(), 4);

        cells = flexTable1.findElements(By.tagName("td"));
        Assert.assertEquals(cells.size(), 8);
        Assert.assertEquals(cells.get(0).getText(), "Cell_2_0");
        Assert.assertEquals(cells.get(1).getText(), "Cell_2_1");
        Assert.assertEquals(cells.get(2).getText(), "Cell_0_0");
        Assert.assertEquals(cells.get(3).getText(), "Cell_0_1");
        Assert.assertEquals(cells.get(4).getText(), "Cell_1_0");
        Assert.assertEquals(cells.get(5).getText(), "");
        Assert.assertEquals(cells.get(6).getText(), "Cell_3_0");
        Assert.assertEquals(cells.get(7).getText(), "Cell_3_1");

        Assert.assertEquals(4, pFlexTable1.getRowCount());
        Assert.assertEquals(2, pFlexTable1.getCellCount(0));

        // remove row, add/remove row style, add/remove column style, add/remove cell style
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PFlexTable flexTable1 = get("flexTable1");
                flexTable1.removeRow(2);

                flexTable1.getRowFormatter().addStyleName(1, "row1");
                flexTable1.getRowFormatter().addStyleName(2, "row2");
                flexTable1.getRowFormatter().addStyleName(2, "row2bis");
                flexTable1.getRowFormatter().addStyleName(2, "row2ter");
                flexTable1.getRowFormatter().removeStyleName(2, "row2bis");

                flexTable1.getColumnFormatter().addStyleName(0, "col0");
                flexTable1.getColumnFormatter().addStyleName(1, "col1");
                flexTable1.getColumnFormatter().addStyleName(1, "col1bis");
                flexTable1.getColumnFormatter().addStyleName(1, "col1ter");
                flexTable1.getColumnFormatter().removeStyleName(1, "col1bis");

                flexTable1.getCellFormatter().addStyleName(1, 1, "cell11");
                flexTable1.getCellFormatter().addStyleName(2, 0, "cell20");
                flexTable1.getCellFormatter().addStyleName(2, 0, "cell20bis");
                flexTable1.getCellFormatter().addStyleName(2, 0, "cell20ter");
                flexTable1.getCellFormatter().removeStyleName(2, 0, "cell20bis");
            }
        });

        flexTable1 = findElementById("flexTable1");
        rows = flexTable1.findElements(By.tagName("tr"));
        cells = flexTable1.findElements(By.tagName("td"));
        final List<WebElement> cols = flexTable1.findElements(By.tagName("col"));

        Assert.assertEquals(3, rows.size());
        Assert.assertEquals(6, cells.size());
        Assert.assertEquals(2, cols.size());
        Assert.assertTrue(rows.get(1).getAttribute("class").contains("row1"));
        Assert.assertTrue(rows.get(2).getAttribute("class").contains("row2"));
        Assert.assertTrue(rows.get(2).getAttribute("class").contains("row2ter"));
        Assert.assertTrue(!rows.get(2).getAttribute("class").contains("row2bis"));

        Assert.assertTrue(cols.get(0).getAttribute("class").contains("col0"));
        Assert.assertTrue(cols.get(1).getAttribute("class").contains("col1"));
        Assert.assertTrue(cols.get(1).getAttribute("class").contains("col1ter"));
        Assert.assertTrue(!cols.get(1).getAttribute("class").contains("col1bis"));

        Assert.assertTrue(cells.get(3).getAttribute("class").contains("cell11"));
        Assert.assertTrue(cells.get(4).getAttribute("class").contains("cell20"));
        Assert.assertTrue(cells.get(4).getAttribute("class").contains("cell20ter"));
        Assert.assertTrue(!cells.get(4).getAttribute("class").contains("cell20bis"));

        final PLabel cell00 = (PLabel) pFlexTable1.getWidget(0, 0);
        final PLabel cell31 = (PLabel) pFlexTable1.getWidget(2, 1);
        Assert.assertEquals("Cell_2_0", cell00.getText());
        Assert.assertEquals("Cell_3_1", cell31.getText());

        // colspan / rowspan
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PFlexTable flexTable1 = get("flexTable1");

                flexTable1.setWidget(3, 0, new PLabel("Cell_4_0"));
                flexTable1.setWidget(4, 0, new PLabel("Cell_5_0"));
                flexTable1.setWidget(4, 1, new PLabel("Cell_5_1"));
                flexTable1.setWidget(5, 0, new PLabel("Cell_6_0"));

                flexTable1.getFlexCellFormatter().setColSpan(3, 0, 2);
                flexTable1.getFlexCellFormatter().setRowSpan(4, 1, 2);
            }
        });

        flexTable1 = findElementById("flexTable1");
        rows = flexTable1.findElements(By.tagName("tr"));
        cells = flexTable1.findElements(By.tagName("td"));

        Assert.assertEquals(6, rows.size());
        Assert.assertEquals(10, cells.size());
        Assert.assertEquals("2", cells.get(6).getAttribute("colSpan"));
        Assert.assertEquals("2", cells.get(8).getAttribute("rowSpan"));

    }

    @Test
    public void testPFlowPanel() {

        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PFlowPanel flowPanel1 = new PFlowPanel();
                flowPanel1.ensureDebugId("flowPanel1");
                flowPanel1.add(new PHTML("text1"));
                flowPanel1.add(new PHTML("text2"));
                flowPanel1.add(new PHTML("text3"));
                flowPanel1.add(new PHTML("text4"));
                PRootPanel.get().add(flowPanel1);
                register(flowPanel1);
            }
        });

        WebElement flowPanel1 = findElementById("flowPanel1");
        List<WebElement> divs = flowPanel1.findElements(By.tagName("div"));
        Assert.assertEquals(4, divs.size());

        final PFlowPanel pFlowPanel = get("flowPanel1");
        Assert.assertEquals(4, pFlowPanel.getWidgetCount());

        // remove
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PFlowPanel flowPanel1 = get("flowPanel1");
                flowPanel1.remove(2);
            }
        });

        flowPanel1 = findElementById("flowPanel1");
        divs = flowPanel1.findElements(By.tagName("div"));
        Assert.assertEquals(3, divs.size());
        Assert.assertEquals(3, pFlowPanel.getWidgetCount());
    }

    @Test
    public void testPFocusPanel() {

        final WebElement e = findElementById("startingpoint");
        final Actions actions = new Actions(webDriver);
        actions.moveToElement(e).build().perform();

        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PFocusPanel focusPanel1 = new PFocusPanel();
                focusPanel1.ensureDebugId("focusPanel1");
                focusPanel1.setWidget(new PLabel("A focusable widget"));
                focusPanel1.addMouseOverHandler(eventsListener);
                focusPanel1.addFocusHandler(eventsListener);
                focusPanel1.addKeyPressHandler(eventsListener);
                focusPanel1.addKeyUpHandler(eventsListener);
                focusPanel1.addBlurHandler(eventsListener);
                PRootPanel.get().add(focusPanel1);
                register(focusPanel1);
            }
        });

        final WebElement focusPanel1 = findElementById("focusPanel1");

        // Mouse over
        actions.moveToElement(focusPanel1).build().perform();
        final PMouseOverEvent e1 = eventsListener.poll();
        Assert.assertNotNull(e1);

        // Focus
        actions.click().build().perform();
        final PFocusEvent e2 = eventsListener.poll();
        Assert.assertNotNull(e2);

        // Key press/ Key up
        actions.sendKeys("A").build().perform();
        final PKeyPressEvent e3 = eventsListener.poll();
        final PKeyUpEvent e4 = eventsListener.poll();
        Assert.assertNotNull(e3);
        Assert.assertEquals(65, e4.getKeyCode());

        eventsListener.clear(); // TODO keyup received 2x, selenium issue ?

        final WebElement element = findElementById("startingpoint");
        element.click();
        final PBlurEvent e5 = eventsListener.poll();
        Assert.assertNotNull(e5);
    }

    @Test
    public void testPGrid() {
        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PGrid grid1 = new PGrid(3, 4);
                grid1.ensureDebugId("grid1");
                PRootPanel.get().add(grid1);
                register(grid1);
            }
        });

        final WebElement grid1 = findElementById("grid1");
        final List<WebElement> rows = grid1.findElements(By.tagName("tr"));
        Assert.assertEquals(3, rows.size());
        final List<WebElement> cells = grid1.findElements(By.tagName("td"));
        Assert.assertEquals(12, cells.size());
    }

    // TODO PHistory

    @Test
    public void testPHorizontalPanel() {
        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PHorizontalPanel horizontal1 = new PHorizontalPanel();
                horizontal1.ensureDebugId("horizontal1");
                horizontal1.add(new PLabel("cell1"));
                horizontal1.add(new PLabel("cell2"));
                horizontal1.add(new PLabel("cell3"));
                horizontal1.add(new PLabel("cell4"));
                horizontal1.setBorderWidth(2);
                horizontal1.setSpacing(3);
                PRootPanel.get().add(horizontal1);
                register(horizontal1);
            }
        });

        WebElement grid1 = findElementById("horizontal1");
        List<WebElement> rows = grid1.findElements(By.tagName("tr"));
        Assert.assertEquals(1, rows.size());
        List<WebElement> cells = grid1.findElements(By.tagName("td"));
        Assert.assertEquals(4, cells.size());

        Assert.assertEquals(grid1.getAttribute("border"), "2");
        Assert.assertEquals(grid1.getAttribute("cellSpacing"), "3");

        // update cell size/alignement
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PHorizontalPanel horizontal1 = get("horizontal1");
                final PWidget cell1 = horizontal1.getWidget(1);
                horizontal1.setCellHeight(cell1, "30px");
                horizontal1.setCellWidth(cell1, "100px");
                horizontal1.setCellHorizontalAlignment(cell1, PHorizontalAlignment.ALIGN_RIGHT);
                horizontal1.setCellVerticalAlignment(cell1, PVerticalAlignment.ALIGN_BOTTOM);
            }
        });

        grid1 = findElementById("horizontal1");
        rows = grid1.findElements(By.tagName("tr"));
        cells = rows.get(0).findElements(By.tagName("td"));

        final WebElement cell1 = cells.get(1);
        Assert.assertEquals("100px", cell1.getAttribute("width"));
        Assert.assertEquals("30px", cell1.getAttribute("height"));
        Assert.assertEquals("right", cell1.getAttribute("align"));
        Assert.assertTrue(cell1.getAttribute("style").contains("vertical-align: bottom"));
    }

    @Test
    public void testPHtml() {
        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PHTML html1 = new PHTML();
                html1.ensureDebugId("html1");
                html1.setHTML("Pure <b>HTML</b>");
                html1.setWordWrap(false);
                PRootPanel.get().add(html1);
                register(html1);
            }
        });

        final WebElement html1 = findElementById("html1");
        Assert.assertEquals("Pure HTML", html1.getText());
        Assert.assertTrue(html1.getAttribute("style").contains("white-space: nowrap;"));
        Assert.assertEquals(1, html1.findElements(By.tagName("b")).size());

        final PHTML pHtml = get("html1");
        Assert.assertEquals("Pure <b>HTML</b>", pHtml.getHTML());
        Assert.assertEquals(false, pHtml.isWordWrap());
    }

    @Test
    public void testPLabel() {
        // creation
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PLabel label2 = new PLabel();
                label2.ensureDebugId("label2");
                label2.setText("A label");
                PRootPanel.get().add(label2);
                register(label2);
            }
        });

        final WebElement label2 = findElementById("label2");
        final PLabel plabel2 = get("label2");
        Assert.assertEquals("A label", label2.getText());
        Assert.assertEquals("A label", plabel2.getText());
    }

    @Test
    public void testPListBox() {
        // creation / insert / update
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PListBox listBox1 = new PListBox();
                listBox1.ensureDebugId("listBox1");
                listBox1.addItem("Item 1");
                listBox1.addItem("Item 2");
                listBox1.addItem("Item 4");
                listBox1.insertItem("Item 3", 2);
                listBox1.addItem("Item A");
                listBox1.addItem("Item 6");
                listBox1.setItemText(4, "Item 5");
                listBox1.addChangeHandler(eventsListener);
                PRootPanel.get().add(listBox1);
                register(listBox1);
            }
        });

        final WebElement listBox1 = findElementById("listBox1");
        List<WebElement> options = listBox1.findElements(By.tagName("option"));
        final PListBox plistBox1 = get("listBox1");

        Assert.assertEquals(false, plistBox1.isEmptySelection());
        Assert.assertEquals(false, plistBox1.isMultipleSelect());

        Assert.assertEquals(6, options.size());
        Assert.assertEquals("Item 1", options.get(0).getText());
        Assert.assertEquals("Item 2", options.get(1).getText());
        Assert.assertEquals("Item 3", options.get(2).getText());
        Assert.assertEquals("Item 4", options.get(3).getText());
        Assert.assertEquals("Item 5", options.get(4).getText());
        Assert.assertEquals("Item 6", options.get(5).getText());

        Assert.assertEquals("Item 1", plistBox1.getItem(0));
        Assert.assertEquals("Item 2", plistBox1.getItem(1));
        Assert.assertEquals("Item 3", plistBox1.getItem(2));
        Assert.assertEquals("Item 4", plistBox1.getItem(3));
        Assert.assertEquals("Item 5", plistBox1.getItem(4));
        Assert.assertEquals("Item 6", plistBox1.getItem(5));

        options.get(4).click();

        final PChangeEvent e1 = eventsListener.poll();
        Assert.assertNotNull(e1);

        Assert.assertEquals(4, plistBox1.getSelectedIndex());
        Assert.assertEquals("Item 5", plistBox1.getSelectedItem());

        // remove
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PListBox listBox1 = get("listBox1");
                listBox1.removeItem("Item 2");
                listBox1.removeItem(2);
            }
        });

        options = listBox1.findElements(By.tagName("option"));

        Assert.assertEquals(4, options.size());
        Assert.assertEquals("Item 1", options.get(0).getText());
        Assert.assertEquals("Item 3", options.get(1).getText());
        Assert.assertEquals("Item 5", options.get(2).getText());
        Assert.assertEquals("Item 6", options.get(3).getText());

        Assert.assertEquals(4, plistBox1.getItemCount());
        Assert.assertEquals("Item 1", plistBox1.getItem(0));
        Assert.assertEquals("Item 3", plistBox1.getItem(1));
        Assert.assertEquals("Item 5", plistBox1.getItem(2));
        Assert.assertEquals("Item 6", plistBox1.getItem(3));

        // server-side selection
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PListBox listBox1 = get("listBox1");
                listBox1.setSelectedItem("Item 5");
            }
        });

        Select select = new Select(listBox1);
        final WebElement selectedOption = select.getFirstSelectedOption();
        Assert.assertEquals("Item 5", selectedOption.getText());
        Assert.assertEquals("Item 5", plistBox1.getSelectedItem());

        // clear
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PListBox listBox1 = get("listBox1");
                listBox1.clear();
            }
        });

        select = new Select(listBox1);
        Assert.assertEquals(0, select.getOptions().size());
        Assert.assertEquals(0, plistBox1.getItemCount());
        Assert.assertEquals(-1, plistBox1.getSelectedIndex());

    }

    @Test
    public void testPListBoxMulti() {
        // creation / insert / update
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PListBox listBox2 = new PListBox(false, true);
                listBox2.ensureDebugId("listBox2");
                listBox2.addItem("Item 1", new Long(1));
                listBox2.addItem("Item 2", new Long(2));
                listBox2.addItem("Item 4", new Long(4));
                listBox2.insertItem("Item 3", new Long(3), 2);
                listBox2.addItem("Item A", new Long(5));
                listBox2.addItem("Item 6", new Long(6));
                listBox2.setItemText(4, "Item 5");
                listBox2.addChangeHandler(eventsListener);
                PRootPanel.get().add(listBox2);
                register(listBox2);
            }
        });

        final WebElement listBox2 = findElementById("listBox2");
        List<WebElement> options2 = listBox2.findElements(By.tagName("option"));
        final PListBox plistBox2 = get("listBox2");

        Assert.assertEquals(false, plistBox2.isEmptySelection());
        Assert.assertEquals(true, plistBox2.isMultipleSelect());

        Assert.assertEquals(6, options2.size());
        Assert.assertEquals("Item 1", options2.get(0).getText());
        Assert.assertEquals("Item 2", options2.get(1).getText());
        Assert.assertEquals("Item 3", options2.get(2).getText());
        Assert.assertEquals("Item 4", options2.get(3).getText());
        Assert.assertEquals("Item 5", options2.get(4).getText());
        Assert.assertEquals("Item 6", options2.get(5).getText());

        Assert.assertEquals("Item 1", plistBox2.getItem(0));
        Assert.assertEquals("Item 2", plistBox2.getItem(1));
        Assert.assertEquals("Item 3", plistBox2.getItem(2));
        Assert.assertEquals("Item 4", plistBox2.getItem(3));
        Assert.assertEquals("Item 5", plistBox2.getItem(4));
        Assert.assertEquals("Item 6", plistBox2.getItem(5));
        Assert.assertEquals(new Long(1), plistBox2.getValue(0));
        Assert.assertEquals(new Long(2), plistBox2.getValue(1));
        Assert.assertEquals(new Long(3), plistBox2.getValue(2));
        Assert.assertEquals(new Long(4), plistBox2.getValue(3));
        Assert.assertEquals(new Long(5), plistBox2.getValue(4));
        Assert.assertEquals(new Long(6), plistBox2.getValue(5));

        options2.get(4).click(); // select item5

        final PChangeEvent e2 = eventsListener.poll();
        Assert.assertNotNull(e2);

        Assert.assertEquals(4, plistBox2.getSelectedIndex());
        Assert.assertEquals("Item 5", plistBox2.getSelectedItem());
        Assert.assertEquals(new Long(5), plistBox2.getSelectedValue());

        // remove
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PListBox listBox2 = get("listBox2");
                listBox2.removeItem("Item 2");
                listBox2.removeItem(2);
                listBox2.removeItem(new Long(6));
            }
        });

        options2 = listBox2.findElements(By.tagName("option"));

        Assert.assertEquals(3, options2.size());
        Assert.assertEquals("Item 1", options2.get(0).getText());
        Assert.assertEquals("Item 3", options2.get(1).getText());
        Assert.assertEquals("Item 5", options2.get(2).getText());

        Assert.assertEquals(3, plistBox2.getItemCount());
        Assert.assertEquals("Item 1", plistBox2.getItem(0));
        Assert.assertEquals("Item 3", plistBox2.getItem(1));
        Assert.assertEquals("Item 5", plistBox2.getItem(2));
        Assert.assertEquals(new Long(1), plistBox2.getValue(0));
        Assert.assertEquals(new Long(3), plistBox2.getValue(1));
        Assert.assertEquals(new Long(5), plistBox2.getValue(2));

        // server-side selection
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PListBox listBox2 = get("listBox2");
                listBox2.setSelectedValue(new Long(3)); // select item3
            }
        });

        final Select select2 = new Select(listBox2);
        final WebElement selectedOption2 = select2.getFirstSelectedOption();
        Assert.assertEquals("Item 3", selectedOption2.getText());
        Assert.assertEquals("Item 3", plistBox2.getSelectedItem());
        Assert.assertEquals(new Long(3), plistBox2.getSelectedValue());

        // TODO
        // multiple selection on client
        // options2 = select2.getOptions();
        // final Actions action = new Actions(webDriver);
        // action.click(options2.get(0)); // select item1
        // action.build().perform();
        //
        // action.keyDown(Keys.CONTROL);
        // action.click(options2.get(2)); // select item5
        // action.release();
        // action.build().perform();
        //
        // final PChangeEvent sel1 = eventsListener.poll();
        // final PChangeEvent sel2 = eventsListener.poll();
        // Assert.assertNotNull(sel1);
        // Assert.assertNotNull(sel2);
        //
        // final List<Integer> selectedItems = plistBox2.getSelectedItems();
        // Assert.assertEquals(2, selectedItems.size());
        // Assert.assertEquals(0, selectedItems.get(0).intValue());
        // Assert.assertEquals(2, selectedItems.get(1).intValue());
    }

    @Test
    public void testPMenuBar() {
        // creation / insert / update
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {

                final PMenuBar subMenuBar = new PMenuBar();
                subMenuBar.addItem("SubItem1");
                subMenuBar.addItem("SubItem2");
                subMenuBar.addItem("SubItem3", new PTestCommand(eventsListener, "Click on SubItem3"));
                subMenuBar.ensureDebugId("subMenuBar1");

                final PMenuBar menuBar1 = new PMenuBar();
                menuBar1.ensureDebugId("menuBar1");
                menuBar1.addItem("Item 1");
                menuBar1.addItem("Item 2", new PTestCommand(eventsListener, "Click on command 2"));
                menuBar1.addItem("Item 3 with <font color='red'>html</font>", true, new PTestCommand(eventsListener, "Click on command 3"));
                menuBar1.addItem("SubMenu", subMenuBar);

                PRootPanel.get().add(menuBar1);
                register(menuBar1);
                register(subMenuBar);
            }
        });

        WebElement menuBar = findElementById("menuBar1");

        List<WebElement> elements = menuBar.findElements(By.tagName("td"));
        Assert.assertEquals(4, elements.size());
        Assert.assertEquals("Item 1", elements.get(0).getText());
        Assert.assertEquals("Item 2", elements.get(1).getText());
        Assert.assertEquals("Item 3 with html", elements.get(2).getText());
        Assert.assertEquals("SubMenu", elements.get(3).getText());

        final WebElement font = elements.get(2).findElement(By.tagName("font"));
        Assert.assertEquals("red", font.getAttribute("color"));

        elements.get(1).click();
        PTestEvent e1 = eventsListener.poll();
        Assert.assertEquals("Click on command 2", e1.getBusinessMessage());

        elements.get(2).click();
        e1 = eventsListener.poll();
        Assert.assertEquals("Click on command 3", e1.getBusinessMessage());

        elements.get(3).click();

        WebElement subMenuBar1 = findElementById("subMenuBar1");
        elements = subMenuBar1.findElements(By.tagName("td"));
        Assert.assertEquals(3, elements.size());

        elements.get(2).click();
        e1 = eventsListener.poll();
        Assert.assertEquals("Click on SubItem3", e1.getBusinessMessage());

        // clear
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PMenuBar subMenuBar = get("subMenuBar1");
                subMenuBar.clearItems();
                subMenuBar.addItem("SubItem4");
            }
        });

        // open sub menubar
        menuBar = findElementById("menuBar1");
        elements = menuBar.findElements(By.tagName("td"));
        elements.get(3).click();

        // check clear / insert
        subMenuBar1 = findElementById("subMenuBar1");
        elements = subMenuBar1.findElements(By.tagName("td"));
        Assert.assertEquals(1, elements.size());
        Assert.assertEquals("SubItem4", elements.get(0).getText());
    }

    @Test
    public void testPRadioButton() {

        // creation
        updateUI(new RequestHandler() {

            @SuppressWarnings("unchecked")
            @Override
            public void onRequest() {
                final PRadioButton radio1 = new PRadioButton("group1", "Text 1");
                radio1.ensureDebugId("radio1");
                final PRadioButton radio2 = new PRadioButton("group1", "Text 2");
                radio2.ensureDebugId("radio2");
                final PRadioButton radio3 = new PRadioButton("group1", "Text 3");
                radio3.ensureDebugId("radio3");
                PRootPanel.get().add(radio1);
                PRootPanel.get().add(radio2);
                PRootPanel.get().add(radio3);

                radio1.addValueChangeHandler(eventsListener);
                radio2.addValueChangeHandler(eventsListener);
                radio3.addValueChangeHandler(eventsListener);

                register(radio1);
                register(radio2);
                register(radio3);
            }
        });

        final WebElement label1 = findElementById("radio1-label");
        final PRadioButton radio1 = get("radio1");
        Assert.assertEquals("Text 1", label1.getText());
        Assert.assertEquals("Text 1", radio1.getText());

        // select item
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PRadioButton radio1 = get("radio1");
                radio1.setValue(true);
            }
        });

        final WebElement input1 = findElementById("radio1-input");
        final WebElement input2 = findElementById("radio2-input");
        final WebElement input3 = findElementById("radio3-input");
        Assert.assertTrue(input1.isSelected());
        Assert.assertFalse(input2.isSelected());
        Assert.assertFalse(input3.isSelected());

        input3.click();

        final PValueChangeEvent<Boolean> e1 = eventsListener.poll();
        Assert.assertEquals(Boolean.TRUE, e1.getValue());
    }

    @Test
    public void testPScheduler() {

        // execute once
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                PScheduler.get().scheduleFixedDelay(new RepeatingCommand() {

                    @Override
                    public boolean execute() {
                        eventsListener.stackCommandResult(new PTestEvent(this, "Timer execution 1"));
                        return false;
                    }
                }, 50);
            }
        });

        final PTestEvent e1 = eventsListener.poll();
        Assert.assertEquals("Timer execution 1", e1.getBusinessMessage());

        // execute 3 times
        final AtomicInteger count = new AtomicInteger(0);
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                PScheduler.get().scheduleFixedDelay(new RepeatingCommand() {

                    @Override
                    public boolean execute() {
                        final int ct = count.incrementAndGet();
                        eventsListener.stackCommandResult(new PTestEvent(this, "Repeating timer execution " + ct));
                        return ct < 3;
                    }
                }, 50);
            }
        });

        final PTestEvent e2 = eventsListener.poll();
        final PTestEvent e3 = eventsListener.poll();
        final PTestEvent e4 = eventsListener.poll();
        Assert.assertEquals("Repeating timer execution 1", e2.getBusinessMessage());
        Assert.assertEquals("Repeating timer execution 2", e3.getBusinessMessage());
        Assert.assertEquals("Repeating timer execution 3", e4.getBusinessMessage());
    }

    @Test
    public void testPScript() {

        // execute OK
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                PScript.get().execute("var i = 5; i + 2;", new ExecutionCallback() {

                    @Override
                    public void onSuccess(final String msg) {
                        eventsListener.stackCommandResult(new PTestEvent(this, "JS result: 7"));
                    }

                    @Override
                    public void onFailure(final String msg) {
                        log.error(msg);
                    }
                });
            }
        });

        final PTestEvent e1 = eventsListener.poll();
        Assert.assertEquals("JS result: 7", e1.getBusinessMessage());

        // execute KO
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                PScript.get().execute("j + 2;", new ExecutionCallback() {

                    @Override
                    public void onSuccess(final String msg) {
                        log.error(msg);
                    }

                    @Override
                    public void onFailure(final String msg) {
                        eventsListener.stackCommandResult(new PTestEvent(this, "JS result: failed"));
                    }
                });
            }
        });

        final PTestEvent e2 = eventsListener.poll();
        Assert.assertEquals("JS result: failed", e2.getBusinessMessage());
    }

    @Test
    public void testPSuggestBox() {

        // creation
        updateUI(new RequestHandler() {

            @SuppressWarnings("unchecked")
            @Override
            public void onRequest() {
                final PSuggestBox suggestBox1 = new PSuggestBox();
                suggestBox1.getSuggestOracle().add("Suggest 1");
                suggestBox1.getSuggestOracle().add("Suggest 2");
                suggestBox1.getSuggestOracle().add("Suggest 3");
                suggestBox1.getSuggestOracle().add("Suggest 4");
                suggestBox1.ensureDebugId("suggestBox1");
                suggestBox1.addSelectionHandler(eventsListener);
                PRootPanel.get().add(suggestBox1);
                register(suggestBox1);
            }
        });

        WebElement element = findElementById("suggestBox1");
        final PSuggestBox pSuggestBox1 = get("suggestBox1");
        element.sendKeys("su");

        final WebElement popup = findElementById("suggestBox1-popup");
        final List<WebElement> items = popup.findElements(By.className("item"));
        Assert.assertEquals(4, items.size());
        Assert.assertEquals("Suggest 1", items.get(0).getText());
        Assert.assertEquals("Suggest 2", items.get(1).getText());
        Assert.assertEquals("Suggest 3", items.get(2).getText());
        Assert.assertEquals("Suggest 4", items.get(3).getText());

        items.get(2).click();

        final PSelectionEvent<PSuggestion> selection1 = eventsListener.poll();
        Assert.assertEquals("Suggest 3", selection1.getSelectedItem().getReplacementString());
        Assert.assertEquals("Suggest 3", pSuggestBox1.getText());
        Assert.assertEquals("Suggest 3", pSuggestBox1.getTextBox().getText());

        // set text
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PSuggestBox suggestBox1 = get("suggestBox1");
                suggestBox1.setText("Custom text");
            }
        });

        element = findElementById("suggestBox1");
        Assert.assertEquals("Custom text", element.getAttribute("value"));
        Assert.assertEquals(true, element.isEnabled());

        // disable
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PSuggestBox suggestBox1 = get("suggestBox1");
                suggestBox1.getTextBox().setEnabled(false);
            }
        });

        element = findElementById("suggestBox1");
        Assert.assertEquals(false, element.isEnabled());

        // TODO test display / replacement
    }

    @Test
    public void testPTabPanel() {

        // creation
        updateUI(new RequestHandler() {

            @SuppressWarnings("unchecked")
            @Override
            public void onRequest() {
                final PLabel tab2Label = new PLabel("tab 2");
                tab2Label.ensureDebugId("tab2Label");
                final PHTML tab3Label = new PHTML("tab <font color='red'>3</font>");

                final PTabPanel tabPanel1 = new PTabPanel();
                tabPanel1.add(new PLabel("content 1"), "tab 1");
                tabPanel1.add(new PLabel("content 2"), tab2Label);
                tabPanel1.add(new PLabel("content 3"), tab3Label);
                tabPanel1.addBeforeSelectionHandler(eventsListener);
                tabPanel1.addSelectionHandler(eventsListener);
                tabPanel1.ensureDebugId("tabPanel1");
                PRootPanel.get().add(tabPanel1);
                register(tabPanel1);
            }
        });

        WebElement tabPanel1Bar = findElementById("tabPanel1-bar");
        List<WebElement> tabBarElements = tabPanel1Bar.findElements(By.tagName("td"));
        Assert.assertEquals(5, tabBarElements.size()); // 3 +(start+end);
        Assert.assertEquals("tab 1", tabBarElements.get(1).getText());
        Assert.assertEquals("tab 2", tabBarElements.get(2).getText());
        Assert.assertEquals("tab 3", tabBarElements.get(3).getText());

        final WebElement fontElement = tabBarElements.get(3).findElement(By.tagName("font"));
        Assert.assertEquals("red", fontElement.getAttribute("color"));

        final PTabPanel ptabPanel1 = get("tabPanel1");
        Assert.assertEquals(3, ptabPanel1.getWidgetCount());

        final PSelectionEvent<Integer> sa = eventsListener.poll();
        Assert.assertEquals(new Integer(0), sa.getSelectedItem());

        // add / remove
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PTabPanel tabPanel1 = get("tabPanel1");
                tabPanel1.remove(2);
                tabPanel1.insert(new PLabel("content 1.5"), "tab 1.5", 1);
            }
        });

        tabPanel1Bar = findElementById("tabPanel1-bar");
        tabBarElements = tabPanel1Bar.findElements(By.tagName("td"));
        Assert.assertEquals(5, tabBarElements.size()); // 3 +(start+end);
        Assert.assertEquals(5, tabBarElements.size()); // 3 +(start+end);
        Assert.assertEquals("tab 1", tabBarElements.get(1).getText());
        Assert.assertEquals("tab 1.5", tabBarElements.get(2).getText());
        Assert.assertEquals("tab 2", tabBarElements.get(3).getText());

        Assert.assertEquals(null, ptabPanel1.getSelectedItemIndex());

        final WebElement tab2Label = findElementById("tab2Label");
        tab2Label.click();

        final PBeforeSelectionEvent<Integer> s0 = eventsListener.poll();
        Assert.assertEquals(new Integer(2), s0.getSelectedItem());

        final PSelectionEvent<Integer> s1 = eventsListener.poll();
        Assert.assertEquals(new Integer(2), s1.getSelectedItem());

        // server-side selection
        updateUI(new RequestHandler() {

            @Override
            public void onRequest() {
                final PTabPanel tabPanel1 = get("tabPanel1");
                tabPanel1.selectTab(1);
            }
        });

        tabPanel1Bar = findElementById("tabPanel1-bar");
        Assert.assertEquals(new Integer(1), ptabPanel1.getSelectedItemIndex());
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
