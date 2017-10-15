package com.ponysdk.core.ui.selenium;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import org.openqa.selenium.*;

import javax.websocket.MessageHandler;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PonySDKWebDriver implements WebDriver, MessageHandler.Whole<ByteBuffer> {

    private Map<Integer, PonyWebElement> elements = new HashMap<>();

    private WebsocketClient client = new WebsocketClient();

    public PonySDKWebDriver() {
        client.setMessageHandler(this);
    }

    @Override
    public void get(String url) {
        try {
            client.connect(new URI(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCurrentUrl() {
        return null;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public List<WebElement> findElements(By by) {
        return null;
    }

    @Override
    public WebElement findElement(By by) {
        return null;
    }

    @Override
    public String getPageSource() {
        return null;
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public void quit() {

    }

    @Override
    public Set<String> getWindowHandles() {
        return null;
    }

    @Override
    public String getWindowHandle() {
        return null;
    }

    @Override
    public TargetLocator switchTo() {
        return null;
    }

    @Override
    public Navigation navigate() {
        return null;
    }

    @Override
    public Options manage() {
        return null;
    }

    class BinaryModel {
        ServerToClientModel model;
    }

    private ByteBuffer buffer;
    private int pos;

    @Override
    public void onMessage(ByteBuffer message) {
        message.rewind();

        if (buffer == null || !buffer.hasRemaining()) {
            buffer = message;
        } else {
            buffer.put(message);
        }

        try {

            Object value = null;

            while (buffer.hasRemaining()) {
                ServerToClientModel model = readModel(buffer);

                System.err.println("Model : " + model);

                switch (model.getTypeModel()) {
                    case NULL:
                        break;
                    case BOOLEAN:
                        if(buffer.hasRemaining()){
                        value = buffer.get();}
                        break;
                    case BYTE:
                        value = buffer.get();
                        break;
                    case SHORT:
                        value = buffer.getShort();
                        break;
                    case INTEGER:
                        if (buffer.remaining() > 4)
                            value = buffer.getInt();
                        else System.err.println("Not enought : ");
                        break;
                    case STRING: {
                        int size = buffer.getShort();

                        if (buffer.remaining() > size) {
                            byte[] bytes = new byte[size];
                            buffer.get(bytes);
                            value = new String(bytes, Charset.forName("UTF8"));
                        } else {
                            System.err.println("Not enought : ");
                            return;
                        }
                        break;
                    }

                    case LONG: {
                        int size = getUnsignedByte(buffer);

                        if (buffer.remaining() > size) {
                            byte[] bytes = new byte[size];
                            buffer.get(bytes);
                            value = new String(bytes, Charset.forName("UTF8"));
                        } else {
                            System.err.println("Not enought : ");
                            return;
                        }
                        break;
                    }

                    case DOUBLE: {
                        int size = buffer.getShort();

                        if (buffer.remaining() > size) {
                            byte[] bytes = new byte[size];
                            buffer.get(bytes);
                            value = new String(bytes, Charset.forName("UTF8"));//Cast to Double
                        } else {
                            System.err.println("Not enought : ");
                            return;
                        }
                        break;
                    }
                    default:
                        System.err.println("not used " + model.getTypeModel());

                }



                switch (model) {
                    case CREATE_CONTEXT:
                        elements.put((Integer) value, new PonyWebElement());
                        System.err.println("Create Object ID " + value);
                        break;
                    case TYPE_CREATE: {
                        System.err.println("Create Object ID " + value);
                        ServerToClientModel m = readModel(buffer);
                        System.err.println("Model " + m); //TODO ciaravola pas obligatoire ?
                        WidgetType type = readWidgetType(buffer);
                        System.err.println("Type Object " + type);
                        break;
                    }
                    case TYPE_UPDATE:
                        System.err.println("Update Object ID " + value);
                        break;
                    case TYPE_GC:
                        System.err.println("GC Object ID " + value);
                        elements.remove(value);
                        break;
                    case TYPE_ADD:
                        System.err.println("Add Object ID " + value);
                        ServerToClientModel m = readModel(buffer);
                        int parent = buffer.getInt();
                        System.err.println("Read parent " + parent);
                        break;
                    case TEXT:
                        System.err.println("Text " + value);
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ServerToClientModel readModel(ByteBuffer buffer) {
        return ServerToClientModel.fromRawValue(getUnsignedByte(buffer));
    }

    private WidgetType readWidgetType(ByteBuffer buffer) {
        return WidgetType.fromRawValue(buffer.get());
    }

    private short getUnsignedByte(ByteBuffer buffer) {
        return (short) (buffer.get() & 0xFF);
    }


    class PonyWebElement implements WebElement {


        private Map<String, String> attributes = new HashMap<>();

        private PonyWebElement() {
        }


        @Override
        public void click() {

        }

        @Override
        public void submit() {

        }

        @Override
        public void sendKeys(CharSequence... keysToSend) {

        }

        @Override
        public void clear() {

        }

        @Override
        public String getTagName() {
            return null;
        }

        @Override
        public String getAttribute(String name) {
            return attributes.get(name);
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public String getText() {
            return null;
        }

        @Override
        public List<WebElement> findElements(By by) {
            return null;
        }

        @Override
        public WebElement findElement(By by) {
            return null;
        }

        @Override
        public boolean isDisplayed() {
            return false;
        }

        @Override
        public Point getLocation() {
            return null;
        }

        @Override
        public Dimension getSize() {
            return null;
        }

        @Override
        public Rectangle getRect() {
            return null;
        }

        @Override
        public String getCssValue(String propertyName) {
            return null;
        }

        @Override
        public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
            return null;
        }
    }
}
