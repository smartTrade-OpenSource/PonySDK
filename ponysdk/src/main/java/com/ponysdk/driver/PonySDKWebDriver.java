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

package com.ponysdk.driver;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.websocket.MessageHandler;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.ArrayValueModel;
import com.ponysdk.core.model.BooleanModel;
import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.ValueTypeModel;
import com.ponysdk.core.model.WidgetType;

public class PonySDKWebDriver implements WebDriver {

    private final static Logger log = LoggerFactory.getLogger(PonySDKWebDriver.class);
    private final static ThreadLocal<byte[]> byteArrays = ThreadLocal.withInitial(() -> new byte[32]);
    private final static PonyMessageListener INDIFFERENT_MSG_LISTENER = new PonyMessageListener() {

        @Override
        public void onSendMessage(final JsonObject message) {
        }

        @Override
        public void onReceiveMessage(final List<PonyFrame> message) {
        }
    };
    private final static PonyBandwithListener INDIFFERENT_BANDWITH_LISTENER = new PonyBandwithListener() {

        @Override
        public void onSend(final int bytes) {
        }

        @Override
        public void onReceive(final int bytes) {
        }

        @Override
        public void onSendCompressed(final int bytes) {
        }

        @Override
        public void onReceiveCompressed(final int bytes) {
        }
    };

    private static final BiConsumer<List<PonyFrame>, PonyFrame> DO_NOTHING_WITH_FRAME = (message, frame) -> {
    };

    private final ConcurrentHashMap<Integer, PonyWebElement> elements = new ConcurrentHashMap<>();
    private final PonySearchContext globalContext = new PonySearchContext(Collections.unmodifiableCollection(elements.values()),
        false);
    private final MessageHandler.Whole<ByteBuffer> messageHandler = this::onMessage;
    private final Map<String, String> cookies = new ConcurrentHashMap<>();
    private final WebsocketClient client;
    private final List<PonyFrame> messageInConstruction = new ArrayList<>();
    private final PonyMessageListener messageListener;
    private final PonyBandwithListener bandwithListener;
    private final boolean handleImplicitCommunication;

    /*
     * Use EnumMap instead of normal switch since, with ecj compiler, ServerToClientModel::values is called on every
     * switch invocation causing enormous garbage
     */
    private final EnumMap<ServerToClientModel, BiConsumer<List<PonyFrame>, PonyFrame>> onMessageSwitch = new EnumMap<>(
        ServerToClientModel.class);

    private volatile String typeHistory;
    private volatile String url;
    private volatile int contextId;

    private ByteBuffer buffer = ByteBuffer.allocate(4096);
    private int length = 0;

    public PonySDKWebDriver() {
        this(null, null, true);
    }

    public PonySDKWebDriver(final PonyMessageListener messageListener, final PonyBandwithListener bandwithListener,
            final boolean handleImplicitCommunication) {
        super();
        this.handleImplicitCommunication = handleImplicitCommunication;
        this.messageListener = messageListener == null ? INDIFFERENT_MSG_LISTENER : messageListener;
        this.bandwithListener = bandwithListener == null ? INDIFFERENT_BANDWITH_LISTENER : bandwithListener;
        this.client = new WebsocketClient(messageHandler, bandwithListener);
        onMessageSwitch.put(ServerToClientModel.CREATE_CONTEXT, (message, frame) -> {
            log.info("UI Context created with ID {}", contextId = (int) frame.value);
            if (handleImplicitCommunication) sendCookies();
        });
        onMessageSwitch.put(ServerToClientModel.HISTORY_FIRE_EVENTS, (message, frame) -> {
            if ((boolean) frame.getValue()) {
                final String typeHistory = (String) findValueForModel(message, ServerToClientModel.TYPE_HISTORY);
                if (typeHistory == null) return;
                if (handleImplicitCommunication) sendTypeHistory(this.typeHistory = typeHistory);
            }
        });
        onMessageSwitch.put(ServerToClientModel.TYPE_ADD, (message, frame) -> {
            final PonyWebElement element = elements.get(frame.value);
            if (element == null) return;
            final Object parentId = findValueForModel(message, ServerToClientModel.PARENT_OBJECT_ID);
            if (parentId == null) return;
            element.parent = elements.get(parentId);
            if (element.parent == null) return;
            final Integer index = (Integer) findValueForModel(message, ServerToClientModel.INDEX);
            if (index == null) {
                element.parent.children.add(element);
            } else {
                element.parent.children.add(index, element);
            }
        });
        onMessageSwitch.put(ServerToClientModel.TYPE_REMOVE, (message, frame) -> {
            final PonyWebElement element = elements.get(frame.value);
            if (element == null || element.parent == null) return;
            element.parent.children.remove(element);
            element.parent = null;
        });
        onMessageSwitch.put(ServerToClientModel.ADD_COOKIE, (message, frame) -> {
            final String value = (String) findValueForModel(message, ServerToClientModel.VALUE);
            if (value == null) return;
            cookies.put((String) frame.value, value);
        });
        onMessageSwitch.put(ServerToClientModel.REMOVE_COOKIE, (message, frame) -> {
            cookies.remove(frame.value);
        });
        onMessageSwitch.put(ServerToClientModel.TYPE_CREATE, (message, frame) -> {
            final Byte widget = (Byte) findValueForModel(message, ServerToClientModel.WIDGET_TYPE);
            if (widget == null) return;
            final int elementId = (int) frame.value;
            elements.put(elementId, new PonyWebElement(this, elementId, WidgetType.fromRawValue(widget)));
        });
        onMessageSwitch.put(ServerToClientModel.TYPE_GC, (message, frame) -> {
            elements.remove(frame.value);
        });
        onMessageSwitch.put(ServerToClientModel.PUT_ATTRIBUTE_KEY, (message, frame) -> {
            final PonyWebElement element = findElement(message);
            if (element == null) return;
            final String key = (String) frame.value;
            final String value = (String) findValueForModel(message, ServerToClientModel.ATTRIBUTE_VALUE);
            if (value == null) return;
            element.attributes.put(key, value);
        });
        onMessageSwitch.put(ServerToClientModel.REMOVE_ATTRIBUTE_KEY, (message, frame) -> {
            final PonyWebElement element = findElement(message);
            if (element == null) return;
            element.attributes.remove(frame.value);
        });
        onMessageSwitch.put(ServerToClientModel.STYLE_NAME, (message, frame) -> {
            final PonyWebElement element = findElement(message);
            if (element == null) return;
            element.styles.clear();
            element.styles.addAll(Arrays.asList(((String) frame.value).split(" ")));
        });
        onMessageSwitch.put(ServerToClientModel.ADD_STYLE_NAME, (message, frame) -> {
            final PonyWebElement element = findElement(message);
            if (element == null) return;
            element.styles.addAll(Arrays.asList(((String) frame.value).split(" ")));
        });
        onMessageSwitch.put(ServerToClientModel.REMOVE_STYLE_NAME, (message, frame) -> {
            final PonyWebElement element = findElement(message);
            if (element == null) return;
            element.styles.removeAll(Arrays.asList(((String) frame.value).split(" ")));
        });
        final BiConsumer<List<PonyFrame>, PonyFrame> onText = (message, frame) -> {
            final PonyWebElement element = findElement(message);
            if (element == null) return;
            element.text = (String) frame.value;
        };
        onMessageSwitch.put(ServerToClientModel.HTML, onText);
        onMessageSwitch.put(ServerToClientModel.TEXT, onText);
        onMessageSwitch.put(ServerToClientModel.OPEN, (message, frame) -> {
            final PonyWebElement element = findElement(message);
            if (element == null) return;
            if (handleImplicitCommunication) {
                element.sendApplicationInstruction(ClientToServerModel.HANDLER_OPEN, "");
                sendCookies();
            }
        });
        onMessageSwitch.put(ServerToClientModel.PING_SERVER, (message, frame) -> {
            if (handleImplicitCommunication) {
                final JsonObject json = Json.createObjectBuilder() //
                    .add(ClientToServerModel.PING_SERVER.toStringValue(), (long) frame.value) //
                    .build();
                sendMessage(json);
            }
        });
        onMessageSwitch.put(ServerToClientModel.HEARTBEAT, (message, frame) -> {
            if (handleImplicitCommunication) sendMessage("0");
        });
        onMessageSwitch.put(ServerToClientModel.WIDGET_VISIBLE, (message, frame) -> {
            final PonyWebElement element = findElement(message);
            if (element == null) return;
            element.displayed = (boolean) frame.value;
        });
        onMessageSwitch.put(ServerToClientModel.ENABLED, (message, frame) -> {
            final PonyWebElement element = findElement(message);
            if (element == null) return;
            element.enabled = (boolean) frame.value;
        });
        onMessageSwitch.put(ServerToClientModel.FUNCTION_ARGS, (message, frame) -> {
            final PonyWebElement element = findElement(message);
            if (element == null) return;
            element.text = Arrays.toString((Object[]) frame.value);
        });
    }

    @Override
    public void get(final String url) {
        try {
            client.connect(new URI(url));
            this.url = url;
        } catch (final Exception e) {
            throw new PonyIOException("Unable to connect to " + url, e);
        }
    }

    public int getContextId() {
        return contextId;
    }

    @Override
    public String getCurrentUrl() {
        return this.url;
    }

    @Override
    public String getTitle() {
        return typeHistory;
    }

    @Override
    public List<WebElement> findElements(final By by) {
        return globalContext.findElements(by);
    }

    public List<PonyWebElement> findElementsAsPony(final By by) {
        return (List<PonyWebElement>) (Object) findElements(by);
    }

    @Override
    public PonyWebElement findElement(final By by) {
        return (PonyWebElement) globalContext.findElement(by);
    }

    public PonyWebElement findElementByPonyId(final int id) {
        return elements.get(id);
    }

    @Override
    public String getPageSource() {
        final StringWriter writer = new StringWriter();
        try {
            printAsXml(writer);
        } catch (final IOException e) {
            //unreachable
        }
        return writer.toString();
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public void quit() {
    }

    public String getSessionId() {
        return client.getSessionId();
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

    private Object readValue(final ByteBuffer b, final int minSize, final Function<ByteBuffer, Object> function) {
        length += minSize;
        return function.apply(b);
    }

    private String getString(final ByteBuffer b) {
        int stringLength = readUnsignedByte(b);
        boolean ascii = true;
        if (stringLength > ValueTypeModel.STRING_ASCII_UINT8_MAX_LENGTH) {
            switch (stringLength) {
                case ValueTypeModel.STRING_ASCII_UINT16:
                    length += 2;
                    stringLength = readUnsignedShort(b);
                    break;
                case ValueTypeModel.STRING_ASCII_INT32:
                    length += 4;
                    stringLength = b.getInt();
                    break;
                case ValueTypeModel.STRING_UTF8_UINT8:
                    length += 1;
                    stringLength = readUnsignedByte(b);
                    ascii = false;
                    break;
                case ValueTypeModel.STRING_UTF8_UINT16:
                    length += 2;
                    stringLength = readUnsignedShort(b);
                    ascii = false;
                    break;
                case ValueTypeModel.STRING_UTF8_INT32:
                    length += 4;
                    stringLength = b.getInt();
                    ascii = false;
                    break;
                default:
                    assert false; //unreachable
            }
        }
        length += stringLength;
        return getString(ascii ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8, b, stringLength);
    }

    private Object getString(final ByteBuffer b, final Charset charset, final ToIntFunction<ByteBuffer> readStringLength) {
        final int strLength = readStringLength.applyAsInt(b);
        length += strLength;
        return getString(charset, b, strLength);
    }

    private Object getArray(final ByteBuffer b) {
        final Object[] array = new Object[readUnsignedByte(b)];
        length += array.length; //elements types
        for (int i = 0; i < array.length; i++) {
            final ArrayValueModel model = readArrayValueModel(b);
            array[i] = readArrayElementValue(b, model);
        }
        return array;
    }

    private int getUint31(final ByteBuffer buffer) {
        final int value = buffer.getShort();
        if (value >= 0) return value;
        length += Short.BYTES;
        return (value << 16 | readUnsignedShort(buffer)) & 0x7F_FF_FF_FF;
    }

    private Object readArrayElementValue(final ByteBuffer b, final ArrayValueModel model) {
        length += model.getMinSize();
        switch (model) {
            case NULL:
                return null;
            case BOOLEAN_FALSE:
                return Boolean.FALSE;
            case BOOLEAN_TRUE:
                return Boolean.TRUE;
            case BYTE:
                return b.get();
            case SHORT:
                return b.getShort();
            case INTEGER:
                return b.getInt();
            case LONG:
                return b.getLong();
            case DOUBLE:
                return b.getDouble();
            case FLOAT:
                return b.getFloat();
            case STRING_ASCII_UINT8_LENGTH:
                return getString(b, StandardCharsets.ISO_8859_1, PonySDKWebDriver::readUnsignedByte);
            case STRING_ASCII_UINT16_LENGTH:
                return getString(b, StandardCharsets.ISO_8859_1, PonySDKWebDriver::readUnsignedShort);
            case STRING_UTF8_UINT8_LENGTH:
                return getString(b, StandardCharsets.UTF_8, PonySDKWebDriver::readUnsignedByte);
            case STRING_UTF8_UINT16_LENGTH:
                return getString(b, StandardCharsets.UTF_8, PonySDKWebDriver::readUnsignedShort);
            case STRING_UTF8_INT32_LENGTH:
                return getString(b, StandardCharsets.UTF_8, ByteBuffer::getInt);
            default:
                throw new IllegalArgumentException("ArrayValueModel " + model + " is not supported");
        }
    }

    private Object readModelValue(final ByteBuffer b, final ValueTypeModel type) {
        switch (type) {
            case NULL:
                return null;
            case BOOLEAN:
                return readValue(b, 1, buff -> buff.get() == BooleanModel.TRUE.getValue());
            case BYTE:
                return readValue(b, 1, ByteBuffer::get);
            case SHORT:
                return readValue(b, 2, ByteBuffer::getShort);
            case INTEGER:
                return readValue(b, 4, ByteBuffer::getInt);
            case LONG:
                return readValue(b, 8, ByteBuffer::getLong);
            case DOUBLE:
                return readValue(b, 8, ByteBuffer::getDouble);
            case FLOAT:
                return readValue(b, 4, ByteBuffer::getFloat);
            case STRING:
                return readValue(b, 1, this::getString);
            case ARRAY:
                return readValue(b, 1, this::getArray);
            case UINT31:
                return readValue(b, 2, this::getUint31);
            default:
                throw new IllegalArgumentException("ValueTypeModel " + type + " is not supported");
        }
    }

    private synchronized void onMessage(final ByteBuffer message) {
        bandwithListener.onReceive(message.remaining());
        while (message.hasRemaining()) {
            final ByteBuffer b = prepareBuffer(message);

            loop: while (b.hasRemaining()) {

                final int position = b.position();
                final ServerToClientModel model = readModel(b);
                length = 1;
                try {
                    final Object value = readModelValue(b, model.getTypeModel());
                    onMessage(model, value);
                } catch (final BufferUnderflowException e) {
                    b.position(position);
                    break loop;
                } catch (final PonyIOException e) {
                    log.error("Error when reading message", e);
                    close();
                    return;
                }
            }

            postpareBuffer(message, b);
        }

    }

    private ByteBuffer prepareBuffer(final ByteBuffer message) {
        ByteBuffer b;
        //buffer is in write mode
        if (buffer.position() == 0) { //buffer has no pending data => use message directly
            b = message;
        } else { //buffer has pending data => append message to it
            if (message.remaining() <= buffer.remaining()) {
                buffer.put(message);
            } else {
                final int limit = message.limit();
                message.limit(message.position() + buffer.remaining());
                buffer.put(message);
                message.limit(limit);
            }
            buffer.flip();
            b = buffer;
        }
        return b;
    }

    private void postpareBuffer(final ByteBuffer message, final ByteBuffer b) {
        if (b == message && b.hasRemaining()) {
            if (buffer.capacity() < length) {
                buffer = ByteBuffer.allocate(Math.max(buffer.capacity() << 1, length));
            }
            buffer.put(message);
        } else if (b == buffer && b.hasRemaining()) {
            if (buffer.capacity() < length) {
                buffer = ByteBuffer.allocate(Math.max(buffer.capacity() << 1, length));
                buffer.put(b);
            } else {
                b.compact();
            }
        } else if (b == buffer && !b.hasRemaining()) {
            b.clear();
        }
    }

    private void onMessage(final ServerToClientModel model, final Object value) {
        if (model == ServerToClientModel.END) {
            onMessage(messageInConstruction);
            messageInConstruction.clear();
        } else {
            messageInConstruction.add(new PonyFrame(model, value));
        }
    }

    private void onMessage(final List<PonyFrame> message) {
        log.debug("IN : {}", message);
        for (final PonyFrame frame : message) {
            onMessageSwitch.getOrDefault(frame.getModel(), DO_NOTHING_WITH_FRAME).accept(message, frame);
        }
        messageListener.onReceiveMessage(message);
    }

    public PonyWebElement findElement(final List<PonyFrame> message) {
        Object id = findValueForModel(message, ServerToClientModel.TYPE_UPDATE);
        if (id == null) {
            id = findValueForModel(message, ServerToClientModel.TYPE_CREATE);
            if (id == null) return null;
        }
        return elements.get(id);
    }

    public static Object findValueForModel(final List<PonyFrame> message, final ServerToClientModel model) {
        for (final PonyFrame event : message) {
            if (event.model == model) return event.getValue();
        }
        return null;
    }

    private static byte[] getLocalByteArray(final int minLength) {
        byte[] array = byteArrays.get();
        if (array.length < minLength) byteArrays.set(array = new byte[Math.max(array.length << 1, minLength)]);
        return array;
    }

    private static String getString(final Charset charset, final ByteBuffer b, final int length) {
        final byte[] bytes = getLocalByteArray(length);
        b.get(bytes, 0, length);
        return new String(bytes, 0, length, charset);
    }

    private ServerToClientModel readModel(final ByteBuffer buffer) {
        return ServerToClientModel.fromRawValue(readUnsignedByte(buffer));
    }

    private ArrayValueModel readArrayValueModel(final ByteBuffer buffer) {
        return ArrayValueModel.fromRawValue(buffer.get());
    }

    private static int readUnsignedByte(final ByteBuffer buffer) {
        return buffer.get() & 0xFF;
    }

    private static int readUnsignedShort(final ByteBuffer buffer) {
        return buffer.getShort() & 0xFFFF;
    }

    private void sendTypeHistory(final String value) {
        sendApplicationInstruction(Json.createObjectBuilder().add(ClientToServerModel.TYPE_HISTORY.toStringValue(), value).build());
    }

    public void sendCookies() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        for (final Map.Entry<String, String> entry : cookies.entrySet()) {
            builder.add(Json.createObjectBuilder() //
                .add(ClientToServerModel.COOKIE_NAME.toStringValue(), entry.getKey())
                .add(ClientToServerModel.COOKIE_VALUE.toStringValue(), entry.getValue()) //
                .build());
        }
        sendApplicationInstruction(Json.createObjectBuilder() //
            .add(ClientToServerModel.OBJECT_ID.toStringValue(), 0) //
            .add(ClientToServerModel.COOKIES.toStringValue(), builder.build())//
            .build());
    }

    public void sendApplicationInstruction(final JsonObject instruction) {
        final JsonObject json = Json.createObjectBuilder()
            .add(ClientToServerModel.APPLICATION_INSTRUCTIONS.toStringValue(), Json.createArrayBuilder() //
                .add(instruction).build() //
            ).build();
        sendMessage(json);
    }

    public void sendMessage(final JsonObject json) {
        final String str = json.toString();
        log.debug("OUT : {}", str);
        sendMessage(str);
        messageListener.onSendMessage(json);
    }

    private void sendMessage(final String msg) {
        try {
            client.sendMessage(msg);

            // UTF-8 encoding is used (for ASCII characters : 1 char <=> 1 byte)
            // To avoid iterating all characters, I consider the amount of non-ASCII characters to be negligible
            bandwithListener.onSend(msg.length());
        } catch (IOException | RuntimeException e) {
            throw new PonyIOException("Failed to send message " + msg, e);
        }
    }

    public void printAsXml(final Writer writer) throws IOException {
        writer.write("<WEB>");
        writer.write('\n');
        for (final PonyWebElement e : elements.values()) {
            if (e.parent == null) e.printTree(1, writer);
        }
        writer.write("</WEB>");
    }

    public void clear() {
        elements.clear();
    }

    public boolean isHandleImplicitCommunication() {
        return handleImplicitCommunication;
    }

}
