
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

public class ParserImpl implements Parser {

    private static final Logger log = LoggerFactory.getLogger(ParserImpl.class);

    private final WebSocket socket;

    private Buffer buffer;

    private final CharsetEncoder UTF8Encoder = Charset.forName("UTF8").newEncoder();

    public ParserImpl(final WebSocket socket) {
        this.socket = socket;
    }

    @Override
    public void reset() {
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
    public void parseAndFlushHeartBeat() {
        buffer = socket.getBuffer();
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(Model.HEARTBEAT.asByte());
        buffer = null; // avoid end of parsing call
    }

    @Override
    public void beginObject() {
        if (buffer == null) {
            buffer = socket.getBuffer();
        }

        final ByteBuffer socketBuffer = buffer.getSocketBuffer();

        // socketBuffer.put(Model.BEGIN_OBJECT);
    }

    @Override
    public void endObject() {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();

        if (socketBuffer.position() >= 2000240) {
            parse(Model.APPLICATION_SEQ_NUM, UIContext.get().getAndIncrementNextSentSeqNum());
            socket.flush();
            buffer = null;
        }
    }

    @Override
    public void parseKey(final byte[] key) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(key);
    }

    @Override
    public void parse(final byte value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(value);
    }

    @Override
    public void parse(final JsonValue jsonObject) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(UTF8StringToByteBuffer(jsonObject.toString()));
    }

    @Override
    public void parse(final Model model) {
        parse(model.asByte());
    }

    @Override
    public void parse(final Model model, final String value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.asByte());
        socketBuffer.put(UTF8StringToByteBuffer(value));
    }

    @Override
    public void parse(final Model model, final JsonObjectBuilder builder) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.asByte());
        socketBuffer.put(UTF8StringToByteBuffer(builder.build().toString()));
    }

    @Override
    public void parse(final Model model, final boolean value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.asByte());

        if (value) {
            socketBuffer.put(Model.TRUE.asByte());
        } else {
            socketBuffer.put(Model.FALSE.asByte());
        }
    }

    @Override
    public void parse(final Model model, final long value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.asByte());
        socketBuffer.putLong(value);
    }

    @Override
    public void parse(final Model model, final int value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.asByte());
        socketBuffer.putInt(value);
    }

    @Override
    public void parse(final Model model, final double value) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.asByte());
        socketBuffer.putDouble(value);
    }

    @Override
    public void parse(final Model model, final Collection<String> collection) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.asByte());
        final Iterator<String> iterator = collection.iterator();

        while (iterator.hasNext()) {
            final String value = iterator.next();
            socketBuffer.put(UTF8StringToByteBuffer(value));
        }
    }

    @Override
    public void parse(final Model model, final JsonValue jsonObject) {
        final ByteBuffer socketBuffer = buffer.getSocketBuffer();
        socketBuffer.put(model.asByte());
        socketBuffer.put(UTF8StringToByteBuffer(jsonObject.toString()));
    }

    public void endOfParsing() {
        if (buffer == null) return;

        final ByteBuffer socketBuffer = buffer.getSocketBuffer();

        if (socketBuffer.position() != 0) {
            parse(Model.APPLICATION_SEQ_NUM, UIContext.get().getAndIncrementNextSentSeqNum());
        }
    }

}
