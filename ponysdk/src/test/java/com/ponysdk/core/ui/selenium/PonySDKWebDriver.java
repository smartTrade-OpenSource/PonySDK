/*
 * Copyright (c) 2017 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.selenium;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.MessageHandler;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;

public class PonySDKWebDriver implements WebDriver, MessageHandler.Whole<ByteBuffer> {

    private final Map<Integer, PonyWebElement> elements = new HashMap<>();

    private final WebsocketClient client = new WebsocketClient();

    public PonySDKWebDriver() {
        client.setMessageHandler(this);
    }

    @Override
    public void get(final String url) {
        try {
            client.connect(new URI(url));
        } catch (final Exception e) {
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
    public List<WebElement> findElements(final By by) {
        return null;
    }

    @Override
    public WebElement findElement(final By by) {
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
    public void onMessage(final ByteBuffer message) {
        message.rewind();

        if (buffer == null || !buffer.hasRemaining()) {
            buffer = message;
        } else {
            buffer.put(message);
        }

        try {

            Object value = null;

            while (buffer.hasRemaining()) {
                final ServerToClientModel model = readModel(buffer);

                System.err.println("Model : " + model);

                switch (model.getTypeModel()) {
                    case NULL:
                        break;
                    case BOOLEAN:
                        if (buffer.hasRemaining()) {
                            value = buffer.get();
                        }
                        break;
                    case BYTE:
                        value = buffer.get();
                        break;
                    case SHORT:
                        value = buffer.getShort();
                        break;
                    case INTEGER:
                        if (buffer.remaining() > 4) value = buffer.getInt();
                        else System.err.println("Not enought : ");
                        break;
                    case STRING: {
                        final int size = buffer.getShort();

                        if (buffer.remaining() > size) {
                            final byte[] bytes = new byte[size];
                            buffer.get(bytes);
                            value = new String(bytes, Charset.forName("UTF8"));
                        } else {
                            System.err.println("Not enought : ");
                            return;
                        }
                        break;
                    }

                    case LONG: {
                        final int size = getUnsignedByte(buffer);

                        if (buffer.remaining() > size) {
                            final byte[] bytes = new byte[size];
                            buffer.get(bytes);
                            value = new String(bytes, Charset.forName("UTF8"));
                        } else {
                            System.err.println("Not enought : ");
                            return;
                        }
                        break;
                    }

                    case DOUBLE: {
                        final int size = buffer.getShort();

                        if (buffer.remaining() > size) {
                            final byte[] bytes = new byte[size];
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
                        final ServerToClientModel m = readModel(buffer);
                        System.err.println("Model " + m); //TODO ciaravola pas obligatoire ?
                        final WidgetType type = readWidgetType(buffer);
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
                        final ServerToClientModel m = readModel(buffer);
                        final int parent = buffer.getInt();
                        System.err.println("Read parent " + parent);
                        break;
                    case TEXT:
                        System.err.println("Text " + value);
                        break;
                    default:
                        break;
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private ServerToClientModel readModel(final ByteBuffer buffer) {
        return ServerToClientModel.fromRawValue(getUnsignedByte(buffer));
    }

    private WidgetType readWidgetType(final ByteBuffer buffer) {
        return WidgetType.fromRawValue(buffer.get());
    }

    private short getUnsignedByte(final ByteBuffer buffer) {
        return (short) (buffer.get() & 0xFF);
    }

    class PonyWebElement implements WebElement {

        private final Map<String, String> attributes = new HashMap<>();

        private PonyWebElement() {
        }

        @Override
        public void click() {

        }

        @Override
        public void submit() {

        }

        @Override
        public void sendKeys(final CharSequence... keysToSend) {

        }

        @Override
        public void clear() {

        }

        @Override
        public String getTagName() {
            return null;
        }

        @Override
        public String getAttribute(final String name) {
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
        public List<WebElement> findElements(final By by) {
            return null;
        }

        @Override
        public WebElement findElement(final By by) {
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
        public String getCssValue(final String propertyName) {
            return null;
        }

        @Override
        public <X> X getScreenshotAs(final OutputType<X> target) throws WebDriverException {
            return null;
        }
    }
}
