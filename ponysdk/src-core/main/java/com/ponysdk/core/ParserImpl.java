
package com.ponysdk.core;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
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

    private static byte[] TRUE;
    private static byte[] FALSE;
    private static byte[] NULL;

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

    private final WebSocket socket;

    private ByteBuffer buffer;

    public ParserImpl(final WebSocket socket) {
        this.socket = socket;
    }

    @Override
    public void reset() {
        buffer = null;
    }

    @Override
    public void beginObject() {
        if (buffer == null) {
            buffer = socket.getByteBuffer();
        }

        if (buffer.position() == 0) {
            buffer.put(CURVE_LEFT);
            parseKey(Model.APPLICATION_INSTRUCTIONS.getBytesKey());
            buffer.put(COLON);
            buffer.put(BRACKET_LEFT);
        } else {
            buffer.put(COMMA);
        }
        buffer.put(CURVE_LEFT);
    }

    @Override
    public void endObject() {
        buffer.put(CURVE_RIGHT);
        if (buffer.position() >= 1024) {
            buffer.put(BRACKET_RIGHT);
            buffer.put(COMMA);
            parse(Model.APPLICATION_SEQ_NUM, UIContext.get().getAndIncrementNextSentSeqNum());
            buffer.put(CURVE_RIGHT);
            socket.flush();
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
        try {
            buffer.put(jsonObject.toString().getBytes("UTF8"));
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot encode value " + jsonObject, e);
        }
    }

    @Override
    public void parse(final Model model) {
        parseKey(model.getBytesKey());
        buffer.put(COLON);
        buffer.put(NULL);
    }

    @Override
    public void parse(final Model model, final String value) {
        parseKey(model.getBytesKey());

        buffer.put(COLON);
        try {
            buffer.put(QUOTE);
            buffer.put(value.getBytes("UTF8"));
            buffer.put(QUOTE);
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot encode value " + value, e);
        }
    }

    @Override
    public void parse(final Model model, final JsonObjectBuilder builder) {
        parseKey(model.getBytesKey());
        buffer.put(COLON);
        try {
            buffer.put(builder.build().toString().getBytes("UTF8"));
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot encode value " + builder.toString(), e);
        }
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
        try {
            buffer.put(String.valueOf(value).getBytes("UTF8"));
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void parse(final Model model, final int value) {
        parseKey(model.getBytesKey());
        buffer.put(COLON);
        // buffer.putInt(value);//FIXME JS decoder
        try {
            buffer.put(String.valueOf(value).getBytes("UTF8"));
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void parse(final Model model, final double value) {
        parseKey(model.getBytesKey());
        buffer.put(COLON);
        // buffer.putDouble(value);//FIXME JS decoder
        try {
            buffer.put(String.valueOf(value).getBytes("UTF8"));
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot encode value " + value, e);
        }
    }

    @Override
    public void parse(final Model model, final Collection<String> collection) {
        parseKey(model.getBytesKey());
        buffer.put(COLON);
        beginArray();

        final Iterator<String> iterator = collection.iterator();

        while (iterator.hasNext()) {
            final String value = iterator.next();
            try {
                buffer.put(QUOTE);
                buffer.put(value.getBytes("UTF8"));
                buffer.put(QUOTE);
                if (iterator.hasNext()) buffer.put(COMMA);
            } catch (final UnsupportedEncodingException e) {
                log.error("Cannot encode value " + value, e);
            }
        }
        endArray();
    }

    @Override
    public void parse(final Model model, final JsonObject jsonObject) {
        parseKey(model.getBytesKey());
        buffer.put(COLON);
        try {
            buffer.put(jsonObject.toString().getBytes("UTF8"));
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot encode value " + jsonObject, e);
        }
    }

    public void endOfParsing() {
        if (buffer.position() != 0) {
            buffer.put(BRACKET_RIGHT);
            buffer.put(COMMA);
            parse(Model.APPLICATION_SEQ_NUM, UIContext.get().getAndIncrementNextSentSeqNum());
            buffer.put(CURVE_RIGHT);
        }
    }

}
