
package com.ponysdk.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collection;

import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.ui.terminal.model.Model;

public class Parser2 implements Parser {

    private static final Logger log = LoggerFactory.getLogger(Parser2.class);

    private static final char CURVE_LEFT = '{';
    private static final char CURVE_RIGHT = '}';
    private static final char BRACKET_LEFT = '[';
    private static final char BRACKET_RIGHT = ']';
    private static final char COLON = ':';
    private static final char COMMA = ',';
    private static final char QUOTE = '\"';

    private final char ZERO = '0';
    private final char ONE = '1';

    private Writer writer = null;

    public Parser2(final Writer writer) {
        this.writer = writer;
    }

    @Override
    public void beginObject() {
        try {
            writer.append(CURVE_LEFT);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void endObject() {
        try {
            writer.append(CURVE_RIGHT);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void parse(final JsonValue jsonObject) {
        try {
            writer.append(jsonObject.toString());
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot encode value " + jsonObject, e);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void parse(final Model type) {
        try {
            writer.append(type.getValue());
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void parse(final Model type, final String value) {
        try {
            writer.append(type.getValue());
        } catch (final IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            writer.append(value);
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot encode value " + value, e);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void parse(final Model type, final JsonObjectBuilder builder) {
        try {
            writer.append(type.getValue());
        } catch (final IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            writer.append(builder.build().toString());
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot encode value " + builder.toString(), e);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void parse(final Model type, final boolean value) {
        try {
            writer.append(type.getValue());
        } catch (final IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        if (value) {
            try {
                writer.append(ZERO);
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                writer.append(ONE);
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void parse(final Model type, final long value) {
        try {
            writer.append(type.getValue());
        } catch (final IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            writer.append("" + value);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void parse(final Model type, final int value) {
        try {
            writer.append(type.getValue());
        } catch (final IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            writer.append(value + "");
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void parse(final Model type, final double value) {
        try {
            writer.append(type.getValue());
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            writer.append(value + "");
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void parse(final Model type, final Collection<String> collection) {
        try {
            writer.append(type.getValue());
        } catch (final IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        for (final String s : collection) {
            try {
                writer.append(s);
            } catch (final UnsupportedEncodingException e) {
                log.error("Cannot encode value " + s, e);
            } catch (final IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void parse(final Model model, final JsonValue jsonObject) {
        try {
            writer.append(model.getValue());
        } catch (final IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        try {
            writer.append(COLON);
        } catch (final IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            writer.append(jsonObject.toString());
        } catch (final UnsupportedEncodingException e) {
            log.error("Cannot encode value " + jsonObject, e);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void parse(final Model model, final Object value) {
    }

}
