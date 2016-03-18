
package com.ponysdk.core;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Collection;
import java.util.Iterator;

import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.servlet.WebSocketServlet.Buffer;
import com.ponysdk.core.socket.WebSocket;
import com.ponysdk.ui.terminal.model.Model;

public class ParserImpl implements Parser {

    private static final Logger log = LoggerFactory.getLogger(ParserImpl.class);

    private static final byte CURVE_LEFT = (byte) '{';
    private static final byte CURVE_RIGHT = (byte) '}';
    private static final byte BRACKET_LEFT = (byte) '[';
    private static final byte BRACKET_RIGHT = (byte) ']';
    private static final byte COLON = (byte) ':';
    private static final byte COMMA = (byte) ',';
    private static final byte QUOTE = (byte) '\"';
    private static final byte[] COMMA_CURVE_LEFT = { COMMA, CURVE_LEFT };
    private static final byte[] CURVE_RIGHT_BRACKET_RIGHT_COMMA = { CURVE_RIGHT, BRACKET_RIGHT, COMMA };

    private static final byte[] BRACKET_RIGHT_COMMA = { BRACKET_RIGHT, COMMA };

    private static byte[] TRUE;
    private static byte[] FALSE;
    private static byte[] NULL;

    static {
        try {
            TRUE = "true".getBytes("UTF8"); // FIXME JS decoder
            FALSE = "false".getBytes("UTF8");
            NULL = "null".getBytes("UTF8");
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static byte[] HEARTBEAT;

    static {

        final byte[] bytesKey = Model.HEARTBEAT.getBytesKey();

        int index = 0;

        HEARTBEAT = new byte[bytesKey.length + NULL.length + 5];

        HEARTBEAT[index++] = CURVE_LEFT;
        HEARTBEAT[index++] = QUOTE;

        for (int i = 0; i < bytesKey.length; i++) {
            HEARTBEAT[index++] = bytesKey[i];
        }

        HEARTBEAT[index++] = QUOTE;
        HEARTBEAT[index++] = COLON;

        for (int i = 0; i < NULL.length; i++) {
            HEARTBEAT[index++] = NULL[i];
        }

        HEARTBEAT[index] = CURVE_RIGHT;

    }

    private static final byte[] COLON_NULL;

    static {
        COLON_NULL = new byte[NULL.length + 1];

        COLON_NULL[0] = COLON;

        for (int i = 0; i < NULL.length; i++) {
            COLON_NULL[1 + i] = NULL[i];
        }
    }

    private static byte[] BEGIN_OBJECT;

    static {
        final byte[] bytesKey = Model.APPLICATION_INSTRUCTIONS.getBytesKey();

        int index = 0;

        BEGIN_OBJECT = new byte[bytesKey.length + 6];

        BEGIN_OBJECT[index++] = CURVE_LEFT;
        BEGIN_OBJECT[index++] = QUOTE;

        for (int i = 0; i < bytesKey.length; i++) {
            BEGIN_OBJECT[index++] = bytesKey[i];
        }

        BEGIN_OBJECT[index++] = QUOTE;
        BEGIN_OBJECT[index++] = COLON;
        BEGIN_OBJECT[index++] = BRACKET_LEFT;
        BEGIN_OBJECT[index] = CURVE_LEFT;
    }

    private final WebSocket socket;

    private Buffer buffer;

    private final CharsetEncoder UTF8Encoder = Charset.forName("UTF8").newEncoder();
    private final CharsetEncoder ASCIIEncoder = Charset.forName("US-ASCII").newEncoder();

    public ParserImpl(final WebSocket socket) {
        this.socket = socket;
    }

    @Override
    public void reset() {
        socket.flush(buffer);
        buffer = null;
    }

    private ByteBuffer UTF8StringToByteBuffer(final String value) {
        try {
            return UTF8Encoder.encode(stringToByteBuffer(value));
        } catch (final CharacterCodingException e) {
            log.error("Cannot convert string");
        }
        return null;
    }

    private ByteBuffer ASCIIStringToByteBuffer(final String value) {
        try {
            return ASCIIEncoder.encode(stringToByteBuffer(value));
        } catch (final CharacterCodingException e) {
            log.error("Cannot convert string");
        }
        return null;
    }

    private CharBuffer stringToByteBuffer(final String value) {
        // final CharBuffer charBuffer = buffer.getCharBuffer();
        // charBuffer.clear();
        // // final String escapeValue = value.replace("\"", "\\\"");
        // for (int i = 0; i < value.length(); i++) {
        // charBuffer.put(value.charAt(i));
        // }
        // charBuffer.flip();
        // return charBuffer;
        return value != null ? CharBuffer.wrap(value) : CharBuffer.wrap("");
    }

    @Override
    public void beginObject() {
        if (buffer == null) {
            buffer = socket.getBuffer();
        }

        final ByteBuffer socketBuffer = buffer.getSocketBuffer();

        if (socketBuffer.position() == 0) {
            socketBuffer.put(BEGIN_OBJECT);
        } else {
            socketBuffer.put(COMMA_CURVE_LEFT);
        }
    }

    @Override
    public void endObject() {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();

        if (socketBuffer.position() >= 1024) {
            socketBuffer.put(CURVE_RIGHT_BRACKET_RIGHT_COMMA);
            parse(Model.APPLICATION_SEQ_NUM, UIContext.get().getAndIncrementNextSentSeqNum());
            socketBuffer.put(CURVE_RIGHT);
            reset();
        } else {
            socketBuffer.put(CURVE_RIGHT);
        }
    }

    @Override
    public void comma() {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(COMMA);
    }

    @Override
    public void quote() {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(QUOTE);
    }

    @Override
    public void beginArray() {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(BRACKET_LEFT);
    }

    @Override
    public void endArray() {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(BRACKET_RIGHT);
    }

    @Override
    public void parseKey(final byte[] key) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(QUOTE);
        socketBuffer.put(key);
        socketBuffer.put(QUOTE);
    }

    @Override
    public void parse(final JsonValue jsonObject) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(UTF8StringToByteBuffer(jsonObject.toString()));
    }

    @Override
    public void parse(final Model model) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        parseKey(model.getBytesKey());
        socketBuffer.put(COLON_NULL);
    }

    @Override
    public void parse(final Model model, final String value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        parseKey(model.getBytesKey());
        socketBuffer.put(COLON);
        socketBuffer.put(QUOTE);
        socketBuffer.put(UTF8StringToByteBuffer(value));
        socketBuffer.put(QUOTE);
    }

    @Override
    public void parse(final Model model, final JsonObjectBuilder builder) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        parseKey(model.getBytesKey());
        socketBuffer.put(COLON);
        socketBuffer.put(UTF8StringToByteBuffer(builder.build().toString()));
    }

    @Override
    public void parse(final Model model, final boolean value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        parseKey(model.getBytesKey());

        socketBuffer.put(COLON);
        if (value) {
            socketBuffer.put(TRUE);
        } else {
            socketBuffer.put(FALSE);
        }
    }

    @Override
    public void parse(final Model model, final long value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        parseKey(model.getBytesKey());

        socketBuffer.put(COLON);
        // socketBuffer.putLong(value);//FIXME JS decoder
        socketBuffer.put(ASCIIStringToByteBuffer(String.valueOf(value)));
    }

    @Override
    public void parse(final Model model, final int value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        parseKey(model.getBytesKey());
        socketBuffer.put(COLON);
        // socketBuffer.putInt(value);//FIXME JS decoder
        socketBuffer.put(ASCIIStringToByteBuffer(String.valueOf(value)));
    }

    @Override
    public void parse(final Model model, final double value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        parseKey(model.getBytesKey());
        socketBuffer.put(COLON);
        // socketBuffer.putDouble(value);//FIXME JS decoder
        socketBuffer.put(ASCIIStringToByteBuffer(String.valueOf(value)));
    }

    @Override
    public void parse(final Model model, final Collection<String> collection) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        parseKey(model.getBytesKey());
        socketBuffer.put(COLON);
        beginArray();

        final Iterator<String> iterator = collection.iterator();

        while (iterator.hasNext()) {
            final String value = iterator.next();
            socketBuffer.put(QUOTE);
            socketBuffer.put(UTF8StringToByteBuffer(value));
            socketBuffer.put(QUOTE);
            if (iterator.hasNext()) socketBuffer.put(COMMA);
        }
        endArray();
    }

    @Override
    public void parse(final Model model, final JsonValue jsonObject) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        parseKey(model.getBytesKey());
        socketBuffer.put(COLON);
        socketBuffer.put(UTF8StringToByteBuffer(jsonObject.toString()));
    }

    public boolean endOfParsing() {
        if (buffer != null) {
            final ByteBuffer socketBuffer = buffer.getSocketBuffer();

            if (socketBuffer.position() != 0) {
                socketBuffer.put(BRACKET_RIGHT_COMMA);
                parse(Model.APPLICATION_SEQ_NUM, UIContext.get().getAndIncrementNextSentSeqNum());
                socketBuffer.put(CURVE_RIGHT);
            }

            return true;
        } else {
            return false;
        }
    }

}
