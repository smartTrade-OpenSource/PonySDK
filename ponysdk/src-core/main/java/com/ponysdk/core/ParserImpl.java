
package com.ponysdk.core;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Collection;
import java.util.Iterator;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static byte[] HEARTBEAT;

    static {
        try {
            TRUE = "true".getBytes("UTF8"); // FIXME JS decoder
            FALSE = "false".getBytes("UTF8");
            NULL = "null".getBytes("UTF8");
        } catch (final UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

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

    private ByteBuffer buffer;

    private final CharsetEncoder UTF8Encoder = Charset.forName("UTF8").newEncoder();
    private final CharsetEncoder ACSIIEncoder = Charset.forName("US-ASCII").newEncoder();

    private final CharBuffer charBuffer = CharBuffer.allocate(10000);

    public ParserImpl(final WebSocket socket) {
        this.socket = socket;
    }

    @Override
    public void reset() {
        buffer = null;
    }

    private ByteBuffer UTF8StringToByteBuffer(final String value) {
        try {
            charBuffer.clear();
            final String escapeValue = value.replace("\"", "\\\"");
            for (int i = 0; i < escapeValue.length(); i++) {
                charBuffer.put(escapeValue.charAt(i));
            }
            charBuffer.flip();
            return UTF8Encoder.encode(charBuffer);
        } catch (final CharacterCodingException e) {
            log.error("Cannot convert string");
        }
        return null;
    }

    private ByteBuffer ASCIIStringToByteBuffer(final String value) {
        try {
            charBuffer.clear();
            final String escapeValue = value.replace("\"", "\\\"");
            for (int i = 0; i < escapeValue.length(); i++) {
                charBuffer.put(escapeValue.charAt(i));
            }
            charBuffer.flip();
            return ACSIIEncoder.encode(charBuffer);
        } catch (final CharacterCodingException e) {
            log.error("Cannot convert string");
        }
        return null;
    }

    @Override
    public void parseAndFlushHeartBeat() {
        // endOfParsing();
        // socket.flush();
        // reset();
        buffer = socket.getByteBuffer();
        buffer.put(HEARTBEAT);
        buffer = null; // avoid end of parsing call
        // socket.flush();
        // reset();
    }

    @Override
    public void beginObject() {
        if (buffer == null) {
            buffer = socket.getByteBuffer();
        }

        if (buffer.position() == 0) {
            buffer.put(BEGIN_OBJECT);
        } else {
            buffer.put(COMMA_CURVE_LEFT);
        }
    }

    @Override
    public void endObject() {
        if (buffer.position() >= 1024) {
            buffer.put(CURVE_RIGHT_BRACKET_RIGHT_COMMA);
            parse(Model.APPLICATION_SEQ_NUM, UIContext.get().getAndIncrementNextSentSeqNum());
            buffer.put(CURVE_RIGHT);
            socket.flush();
            buffer = null;
        } else {
            buffer.put(CURVE_RIGHT);
        }
    }

    @Override
    public void comma() {
        buffer.put(COMMA);
    }

    @Override
    public void quote() {
        buffer.put(QUOTE);
    }

    @Override
    public void beginArray() {
        buffer.put(BRACKET_LEFT);
    }

    @Override
    public void endArray() {
        buffer.put(BRACKET_RIGHT);
    }

    @Override
    public void parseKey(final byte[] key) {
        buffer.put(QUOTE);
        buffer.put(key);
        buffer.put(QUOTE);
    }

    @Override
    public void parse(final JsonObject jsonObject) {
        buffer.put(UTF8StringToByteBuffer(jsonObject.toString()));
    }

    @Override
    public void parse(final Model model) {
        parseKey(model.getBytesKey());

        buffer.put(COLON_NULL);
    }

    @Override
    public void parse(final Model model, final String value) {
        parseKey(model.getBytesKey());

        buffer.put(COLON);
        buffer.put(QUOTE);
        buffer.put(UTF8StringToByteBuffer(value));
        buffer.put(QUOTE);
    }

    @Override
    public void parse(final Model model, final JsonObjectBuilder builder) {
        parseKey(model.getBytesKey());
        buffer.put(COLON);
        buffer.put(UTF8StringToByteBuffer(builder.build().toString()));
    }

    @Override
    public void parse(final Model model, final boolean value) {
        parseKey(model.getBytesKey());

        buffer.put(COLON);
        if (value) {
            buffer.put(TRUE);
        } else {
            buffer.put(FALSE);
        }
    }

    @Override
    public void parse(final Model model, final long value) {
        parseKey(model.getBytesKey());

        buffer.put(COLON);
        // buffer.putLong(value);//FIXME JS decoder
        buffer.put(ASCIIStringToByteBuffer(String.valueOf(value)));
    }

    @Override
    public void parse(final Model model, final int value) {
        parseKey(model.getBytesKey());
        buffer.put(COLON);
        // buffer.putInt(value);//FIXME JS decoder
        buffer.put(ASCIIStringToByteBuffer(String.valueOf(value)));
    }

    @Override
    public void parse(final Model model, final double value) {
        parseKey(model.getBytesKey());
        buffer.put(COLON);
        // buffer.putDouble(value);//FIXME JS decoder
        buffer.put(ASCIIStringToByteBuffer(String.valueOf(value)));
    }

    @Override
    public void parse(final Model model, final Collection<String> collection) {
        parseKey(model.getBytesKey());
        buffer.put(COLON);
        beginArray();

        final Iterator<String> iterator = collection.iterator();

        while (iterator.hasNext()) {
            final String value = iterator.next();
            buffer.put(QUOTE);
            buffer.put(UTF8StringToByteBuffer(value));
            buffer.put(QUOTE);
            if (iterator.hasNext()) buffer.put(COMMA);
        }
        endArray();
    }

    @Override
    public void parse(final Model model, final JsonObject jsonObject) {
        parseKey(model.getBytesKey());
        buffer.put(COLON);
        buffer.put(UTF8StringToByteBuffer(jsonObject.toString()));
    }

    public void endOfParsing() {
        if (buffer == null) return;

        if (buffer.position() != 0) {
            buffer.put(BRACKET_RIGHT_COMMA);
            parse(Model.APPLICATION_SEQ_NUM, UIContext.get().getAndIncrementNextSentSeqNum());
            buffer.put(CURVE_RIGHT);
        }
    }

}
