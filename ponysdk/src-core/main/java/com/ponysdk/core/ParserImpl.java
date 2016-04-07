
package com.ponysdk.core;

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
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class ParserImpl implements Parser {

    private static final Logger log = LoggerFactory.getLogger(ParserImpl.class);

    private static final byte QUOTE = (byte) '\"';

    private final WebSocket socket;

    private Buffer buffer;

    private final CharsetEncoder UTF8Encoder = Charset.forName("UTF8").newEncoder();

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

    private CharBuffer stringToByteBuffer(final String value) {
        return value != null ? CharBuffer.wrap(value) : CharBuffer.wrap("");
    }

    @Override
    public void beginObject() {
        if (buffer == null)
            buffer = socket.getBuffer();

        final ByteBuffer socketBuffer = buffer.getSocketBuffer();

        if (socketBuffer.position() == 0) {
            socketBuffer.put(Model.APPLICATION_SEQ_NUM.getValue());
            socketBuffer.putInt(UIContext.get().getAndIncrementNextSentSeqNum());
        }
    }

    @Override
    public void endObject() {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();

        if (socketBuffer.position() >= 4096)
            reset();
    }

    @Override
    public void parse(final JsonValue jsonObject) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(UTF8StringToByteBuffer(jsonObject.toString()));
    }

    public void parse(final Model model) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.getValue());
    }

    public void parse(final Model model, final String value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.getValue());
        socketBuffer.putShort(value != null ? (short) value.length() : 0);
        socketBuffer.put(UTF8StringToByteBuffer(value));
    }

    @Override
    public void parse(final Model model, final JsonObjectBuilder builder) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.getValue());
        final String value = builder.build().toString();
        socketBuffer.putShort(value != null ? (short) value.length() : 0);
        socketBuffer.put(UTF8StringToByteBuffer(value));
    }

    public void parse(final Model model, final boolean value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.getValue());
        socketBuffer.put(value ? ReaderBuffer.TRUE : ReaderBuffer.FALSE);
    }

    public void parse(final Model model, final long value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.getValue());
        socketBuffer.putLong(value);
    }

    public void parse(final Model model, final int value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.getValue());
        socketBuffer.putInt(value);
    }

    public void parse(final Model model, final double value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.getValue());
        socketBuffer.putDouble(value);
    }

    @Override
    public void parse(final Model model, final Collection<String> collection) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.getValue());

        final Iterator<String> iterator = collection.iterator();

        while (iterator.hasNext()) {
            final String value = iterator.next();
            socketBuffer.put(QUOTE);
            socketBuffer.put(UTF8StringToByteBuffer(value));
            socketBuffer.put(QUOTE);
        }
    }

    @Override
    public void parse(final Model model, final JsonValue jsonObject) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.getValue());
        final String value = jsonObject.toString();
        socketBuffer.putShort(value != null ? (short) value.length() : 0);
        socketBuffer.put(UTF8StringToByteBuffer(value));
    }

    public boolean endOfParsing() {
        if (buffer != null)
            return true;
        else
            return false;
    }

    @Override
    public void parse(final Model model, final Object value) {
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
