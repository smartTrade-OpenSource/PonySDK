
package com.ponysdk.core.instruction;

import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.Collection;

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.ui.terminal.model.Model;

public class Parser {

    private static final Logger log = LoggerFactory.getLogger(Parser.class);

    private static final byte CURVE_LEFT = '{';
    private static final byte CURVE_RIGHT = '}';
    private static final byte BRACKET_LEFT = '[';
    private static final byte BRACKET_RIGHT = ']';
    private static final byte COMMA = ',';

    private final byte ZERO = 0;
    private final byte ONE = 1;

    private ByteBuffer buffer = null;
    private Writer writer = null;

    public Parser(final ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public Parser(final Writer writer) {
        this.writer = writer;
    }

    public void beginObject() {
        buffer.put(CURVE_LEFT);
    }

    public void endObject() {
        buffer.put(CURVE_RIGHT);
    }

    public void comma() {
        buffer.put(COMMA);
    }

    public void beginArray() {
        buffer.put(BRACKET_LEFT);
    }

    public void endArray() {
        buffer.put(BRACKET_RIGHT);
    }

    public void parse(final Model type) {
        buffer.put(Model.TYPE.getBytesKey());
        buffer.put(type.getBytesKey());
    }

    public void parse(final Model type, final String value) {
        buffer.put(type.getBytesKey());
        try {
            buffer.put(value.getBytes("UTF8"));
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot encode value " + value, e);
        }
    }

    public void parse(final Model type, final boolean value) {
        buffer.put(type.getBytesKey());
        if (value) {
            buffer.put(ZERO);
        } else {
            buffer.put(ONE);
        }
    }

    public void parse(final Model type, final long value) {
        buffer.put(type.getBytesKey());
        buffer.putLong(value);
    }

    public void parse(final Model type, final int value) {
        buffer.put(type.getBytesKey());
        buffer.putInt(value);
    }

    public void parse(final Model type, final double value) {
        buffer.put(type.getBytesKey());
        buffer.putDouble(value);
    }

    public void parse(final Model type, final Collection<String> collection) {
        buffer.put(type.getBytesKey());

        beginArray();

        for (final String s : collection) {
            try {
                buffer.put(s.getBytes("UTF8"));
            } catch (final UnsupportedEncodingException e) {
                log.error("Cannot encode value " + s, e);
            }
        }

        endArray();
    }

    public void parse(final Model model, final JsonObject json) {
        // TODO Auto-generated method stub

    }

}
