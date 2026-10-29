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

import com.ponysdk.core.model.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public class PonySDKWebDriver implements WebDriver {

    private static final Logger log = LoggerFactory.getLogger(PonySDKWebDriver.class);
    private static final Logger loggerIn = LoggerFactory.getLogger("PonySDKWebDriver-IN");
    private static final Logger loggerOut = LoggerFactory.getLogger("PonySDKWebDriver-OUT");

    private static final ThreadLocal<byte[]> byteArrays = ThreadLocal.withInitial(() -> new byte[32]);

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

    /**
     * Client side of the String Dictionary (DICTIONARY_ADD defines an id, DICTIONARY_REF reuses it),
     * mirroring the terminal's ReaderBuffer. The dictionary is on by default server-side, so without
     * this the driver failed on the first interned string.
     */
    private String[] dictionary = new String[256];

    private void dictionaryPut(final int id, final String value) {
        if (id >= dictionary.length) dictionary = Arrays.copyOf(dictionary, Math.max(id + 1, dictionary.length * 2));
        dictionary[id] = value;
    }

    private String dictionaryGet(final int id) {
        final String value = id < dictionary.length ? dictionary[id] : null; // a uint31: never negative
        if (value == null) throw new PonyIOException("Unknown dictionary ID: " + id);
        return value;
    }

    public PonySDKWebDriver() {
        this(null, null, null, true);
    }

    public PonySDKWebDriver(final PonyMessageListener messageListener, final PonyBandwithListener bandwithListener,
            PonySessionListener sessionListener, final boolean handleImplicitCommunication) {
        super();
        this.handleImplicitCommunication = handleImplicitCommunication;
        this.messageListener = messageListener == null ? INDIFFERENT_MSG_LISTENER : messageListener;
        this.bandwithListener = bandwithListener == null ? INDIFFERENT_BANDWITH_LISTENER : bandwithListener;
        sessionListener = sessionListener == null ? INDIFFERENT_SESSION_LISTENER : sessionListener;
        this.client = new WebsocketClient(messageHandler, this.bandwithListener, sessionListener); // field: never null (the parameter may be)
        onMessageSwitch.put(ServerToClientModel.CREATE_CONTEXT, (message, frame) -> {
            log.info("UI Context created with ID {}", contextId = (int) frame.value);
            if (handleImplicitCommunication) sendCookies();
        });
        onMessageSwitch.put(ServerToClientModel.HISTORY_FIRE_EVENTS, (message, frame) -> {
            if ((boolean) frame.getValue()) {
                final String typeHistory = (String) findValueForModel(message, ServerToClientModel.TYPE_HISTORY);
                if (typeHistory == null) return;
                this.typeHistory = typeHistory;
                if (handleImplicitCommunication) sendTypeHistory(typeHistory);
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
            final PonyWebElement element = new PonyWebElement(this, elementId, WidgetType.fromRawValue(widget));
            elements.put(elementId, element);
            // protocol 3: created directly in its parent
            final Object parentId = findValueForModel(message, ServerToClientModel.PARENT_OBJECT_ID);
            if (parentId != null) {
                element.parent = elements.get(parentId);
                if (element.parent != null) element.parent.children.add(element);
            }
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
        onMessageSwitch.put(ServerToClientModel.ROUNDTRIP_LATENCY, (message, frame) -> {
            if (handleImplicitCommunication) {
                final JsonObject json = Json.createObjectBuilder() //
                    .add(ClientToServerModel.TERMINAL_LATENCY.toStringValue(), (long) frame.getValue()) //
                    .build();
                sendMessage(json);
            }
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
            client.connect(new URI(withProtocolVersion(url)));
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
        final StringBuilder page = new StringBuilder("<WEB>\n");
        for (final PonyWebElement e : elements.values()) {
            if (e.parent == null) e.printTree(1, page);
        }
        return page.append("</WEB>").toString();
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
        final int tag = readUnsignedByte(b);
        if (tag == ValueTypeModel.STRING_DICTIONARY_REF) {
            length += Short.BYTES;
            return dictionaryGet(getUint31(b));
        }
        if (tag == ValueTypeModel.STRING_DICTIONARY_ADD) {
            length += Short.BYTES;
            final int id = getUint31(b);
            length += 1; // inner string tag
            final String value = getInlineString(b, readUnsignedByte(b));
            dictionaryPut(id, value);
            return value;
        }
        return getInlineString(b, tag);
    }

    private String getInlineString(final ByteBuffer b, final int tag) {
        int stringLength = tag;
        boolean ascii = true;
        if (stringLength > ValueTypeModel.STRING_ASCII_UINT8) {
            switch (stringLength) {
                case ValueTypeModel.STRING_ASCII_UINT16:
                    length += 2;
                    stringLength = readUnsignedShort(b);
                    break;
                case ValueTypeModel.STRING_ASCII_UINT32:
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
                default: // STRING_UTF8_INT32, the last tag above STRING_ASCII_UINT8
                    length += 4;
                    stringLength = b.getInt();
                    ascii = false;
                    break;
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
        int size = readUnsignedByte(b);
        // 255 is the escape: a uint31 length (2 or 4 bytes) follows — mirrors WebSocketPusher / ReaderBuffer
        if (size == ArrayValueModel.LENGTH_UINT31_ESCAPE) {
            length += Short.BYTES;
            size = getUint31(b);
        }
        final Object[] array = new Object[size];
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
        return switch (model) {
            case NULL -> null;
            case BOOLEAN_FALSE -> Boolean.FALSE;
            case BOOLEAN_TRUE -> Boolean.TRUE;
            case BYTE -> b.get();
            case SHORT -> b.getShort();
            case INTEGER -> b.getInt();
            case LONG -> b.getLong();
            case DOUBLE -> b.getDouble();
            case FLOAT -> b.getFloat();
            case STRING_ASCII_UINT8_LENGTH -> getString(b, StandardCharsets.ISO_8859_1, PonySDKWebDriver::readUnsignedByte);
            case STRING_ASCII_UINT16_LENGTH -> getString(b, StandardCharsets.ISO_8859_1, PonySDKWebDriver::readUnsignedShort);
            case STRING_UTF8_UINT8_LENGTH -> getString(b, StandardCharsets.UTF_8, PonySDKWebDriver::readUnsignedByte);
            case STRING_UTF8_UINT16_LENGTH -> getString(b, StandardCharsets.UTF_8, PonySDKWebDriver::readUnsignedShort);
            case STRING_UTF8_UINT32_LENGTH -> getString(b, StandardCharsets.UTF_8, ByteBuffer::getInt);
            case STRING_DICTIONARY_REF -> dictionaryGet(getUint31(b)); // min size (2) already counted above
            case STRING_DICTIONARY_ADD -> {
                final int id = getUint31(b);
                length += 1; // inner element tag
                final Object value = readArrayElementValue(b, readArrayValueModel(b));
                dictionaryPut(id, (String) value);
                yield value;
            }
        };
    }

    private Object readModelValue(final ByteBuffer b, final ValueTypeModel type) {
        return switch (type) {
            case NULL -> null;
            case BOOLEAN -> readValue(b, 1, buff -> buff.get() == BooleanModel.TRUE.getValue());
            case BYTE -> readValue(b, 1, ByteBuffer::get);
            case SHORT -> readValue(b, 2, ByteBuffer::getShort);
            case INTEGER -> readValue(b, 4, ByteBuffer::getInt);
            case LONG -> readValue(b, 8, ByteBuffer::getLong);
            case DOUBLE -> readValue(b, 8, ByteBuffer::getDouble);
            case FLOAT -> readValue(b, 4, ByteBuffer::getFloat);
            case STRING -> readValue(b, 1, this::getString);
            case ARRAY -> readValue(b, 1, this::getArray);
            case UINT31 -> readValue(b, 2, this::getUint31);
        };
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
        } else if (b == buffer) {
            b.clear();
        }
    }

    private void onMessage(final ServerToClientModel model, final Object value) {
        if (model == ServerToClientModel.END) {
            try {
                onMessage(messageInConstruction);
            } finally {
                messageInConstruction.clear(); // a listener failure must not leak this message into the next one
            }
        } else {
            messageInConstruction.add(new PonyFrame(model, value));
        }
    }

    /** Binary protocol version announced at handshake (default: the latest; 1 = original format). */
    private int protocolVersion = com.ponysdk.core.model.ProtocolVersion.CURRENT;

    public void setProtocolVersion(final int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    private String withProtocolVersion(final String url) {
        final String key = ClientToServerModel.PROTOCOL_VERSION.toStringValue();
        if (url.contains("?" + key + "=") || url.contains("&" + key + "=")) return url;
        return url + (url.indexOf('?') >= 0 ? "&" : "?") + key + "=" + protocolVersion;
    }

    /**
     * An update run (protocol 2) carries several objects in one block, chained by UPDATE_NEXT markers.
     * It is split into one message per object - each with the block's WINDOW_ID / FRAME_ID header and a
     * TYPE_UPDATE frame - so element tracking and message listeners see the same shape as version 1.
     */
    private void onMessage(final List<PonyFrame> message) {
        int updateIndex = -1;
        boolean run = false;
        for (int i = 0; i < message.size(); i++) {
            final ServerToClientModel m = message.get(i).getModel();
            if (m == ServerToClientModel.TYPE_UPDATE && updateIndex < 0) updateIndex = i;
            else if (m == ServerToClientModel.UPDATE_NEXT || m == ServerToClientModel.UPDATE_NEXT_DELTA
                    || m == ServerToClientModel.UPDATE_NEXT_ID) run = true;
        }
        if (!run || updateIndex < 0) {
            dispatch(message);
            return;
        }
        final List<PonyFrame> header = message.subList(0, updateIndex);
        int id = ((Number) message.get(updateIndex).getValue()).intValue();
        List<PonyFrame> current = new ArrayList<>(header);
        current.add(message.get(updateIndex));
        for (int i = updateIndex + 1; i < message.size(); i++) {
            final PonyFrame frame = message.get(i);
            final ServerToClientModel m = frame.getModel();
            if (m == ServerToClientModel.UPDATE_NEXT) id += 1;
            else if (m == ServerToClientModel.UPDATE_NEXT_DELTA) id += 2 + (((Number) frame.getValue()).intValue() & 0xFF);
            else if (m == ServerToClientModel.UPDATE_NEXT_ID) id = ((Number) frame.getValue()).intValue();
            else {
                current.add(frame);
                continue;
            }
            dispatch(current);
            current = new ArrayList<>(header);
            current.add(new PonyFrame(ServerToClientModel.TYPE_UPDATE, id));
        }
        dispatch(current);
    }

    private void dispatch(final List<PonyFrame> message) {
        loggerIn.trace("{}", message);
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
        loggerOut.trace(str);
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
        writer.write(getPageSource());
    }

    public void clear() {
        elements.clear();
    }

    public boolean isHandleImplicitCommunication() {
        return handleImplicitCommunication;
    }

    public void ignoreSessionStatusChanges() {
        this.client.setSessionListener(INDIFFERENT_SESSION_LISTENER);
    }


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

    private static final PonySessionListener INDIFFERENT_SESSION_LISTENER = new PonySessionListener() {

        @Override
        public void onOpen(final Session session) {
        }

        @Override
        public void onError(final Session session, final Throwable thr) {
        }

        @Override
        public void onClose(final Session session, final CloseReason closeReason) {
        }
    };
}
