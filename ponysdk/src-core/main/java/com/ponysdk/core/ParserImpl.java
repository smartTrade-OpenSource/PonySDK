
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
    private static final byte[] CURVE_RIGHT_BRACKET_RIGHT_COMMA = { CURVE_RIGHT, BRACKET_RIGHT };

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

    private static final byte[] COLON_NULL;

    static {
        COLON_NULL = new byte[NULL.length + 1];

        COLON_NULL[0] = COLON;

        for (int i = 0; i < NULL.length; i++)
            COLON_NULL[1 + i] = NULL[i];
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
        if (buffer == null) buffer = socket.getBuffer();

        final ByteBuffer socketBuffer = buffer.getSocketBuffer();

        if (socketBuffer.position() == 0) {
            socketBuffer.putShort(Model.APPLICATION_INSTRUCTIONS.getShortKey());
            socketBuffer.putShort(Model.APPLICATION_SEQ_NUM.getShortKey());
            socketBuffer.putInt(UIContext.get().getAndIncrementNextSentSeqNum());
            socketBuffer.put(BRACKET_LEFT);
            socketBuffer.put(CURVE_LEFT);
        } else socketBuffer.put(COMMA_CURVE_LEFT);
    }

    @Override
    public void endObject() {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();

        if (socketBuffer.position() >= 4096) {
            socketBuffer.put(CURVE_RIGHT_BRACKET_RIGHT_COMMA);
            reset();
        } else socketBuffer.put(CURVE_RIGHT);
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

    public void parse(final Model model, final boolean value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        parseKey(model.getBytesKey());

        socketBuffer.put(COLON);
        if (value) socketBuffer.put(TRUE);
        else socketBuffer.put(FALSE);
    }

    public void parse(final Model model, final long value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        parseKey(model.getBytesKey());

        socketBuffer.put(COLON);
        // socketBuffer.putLong(value);//FIXME JS decoder
        socketBuffer.put(ASCIIStringToByteBuffer(String.valueOf(value)));
    }

    public void parse(final Model model, final int value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        parseKey(model.getBytesKey());
        socketBuffer.put(COLON);
        // socketBuffer.putInt(value);//FIXME JS decoder
        socketBuffer.put(ASCIIStringToByteBuffer(String.valueOf(value)));
    }

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

            if (socketBuffer.position() != 0) socketBuffer.put(BRACKET_RIGHT);

            return true;
        } else return false;
    }

    @Override
    public void parse(Model model, Object value) {
        switch (model.getTypeModel()) {
            case NULL_SIZE:
                parse(model);
                break;
            case BOOLEAN_SIZE:
                parse(model, (boolean) value);
                break;
            case BYTE_SIZE:
                parse(model, (byte) value);
                break;
            case SHORT_SIZE:
                parse(model, (short) value);
                break;
            case INTEGER_SIZE:
                parse(model, (int) value);
                break;
            case LONG_SIZE:
                parse(model, (long) value);
                break;
            case DOUBLE_SIZE:
                parse(model, (double) value);
                break;
            case VARIABLE_SIZE:
                parse(model, (String) value);
                break;
            default:
                break;
        }
    }

}
