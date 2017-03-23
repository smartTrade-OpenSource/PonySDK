
package com.ponysdk.core.ui.basic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.writer.ModelWriter;

public abstract class PAddOn extends PObject {

    private static final String ARGUMENTS_PROPERTY_NAME = "arg";
    private static final String METHOD_PROPERTY_NAME = "m";

    private final static Map<Level, Byte> LOG_LEVEL = new HashMap<>();

    static {
        byte level = 0;
        LOG_LEVEL.put(Level.OFF, level++);
        LOG_LEVEL.put(Level.SEVERE, level++);
        LOG_LEVEL.put(Level.WARNING, level++);
        LOG_LEVEL.put(Level.INFO, level++);
        LOG_LEVEL.put(Level.CONFIG, level++);
        LOG_LEVEL.put(Level.FINE, level++);
        LOG_LEVEL.put(Level.FINER, level++);
        LOG_LEVEL.put(Level.FINEST, level++);
        LOG_LEVEL.put(Level.ALL, level++);
    }

    private JsonObject args;

    protected PAddOn() {
    }

    protected PAddOn(final JsonObject args) {
        this.args = args;
    }

    @Override
    public boolean attach(final PWindow window) {
        if (this.window == null && window != null) {
            this.window = window;
            init();
            return true;
        } else if (this.window != window) {
            throw new IllegalAccessError(
                "Widget already attached to an other window, current window : #" + this.window + ", new window : #" + window);
        }
        return false;
    }

    @Override
    protected void enrichOnInit(final ModelWriter writer) {
        super.enrichOnInit(writer);
        writer.write(ServerToClientModel.FACTORY, getSignature());
        if (args != null) {
            writer.write(ServerToClientModel.NATIVE, args);
            args = null;
        }
    }

    public String getSignature() {
        return getClass().getCanonicalName();
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ADDON;
    }

    protected void callTerminalMethod(final String methodName, final Object... args) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(METHOD_PROPERTY_NAME, methodName);

        if (args.length > 0) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (final Object object : args) {
                if (object != null) {
                    if (object instanceof JsonValue) {
                        arrayBuilder.add((JsonValue) object);
                    } else if (object instanceof Number) {
                        final Number number = (Number) object;
                        if (object instanceof Byte || object instanceof Short || object instanceof Integer)
                            arrayBuilder.add(number.intValue());
                        else if (object instanceof Long) arrayBuilder.add(number.longValue());
                        else if (object instanceof Float || object instanceof Double) arrayBuilder.add(number.doubleValue());
                        else if (object instanceof BigInteger) arrayBuilder.add((BigInteger) object);
                        else if (object instanceof BigDecimal) arrayBuilder.add((BigDecimal) object);
                        else arrayBuilder.add(number.doubleValue());
                    } else if (object instanceof Boolean) {
                        arrayBuilder.add((Boolean) object);
                    } else if (object instanceof JsonArrayBuilder) {
                        arrayBuilder.add(((JsonArrayBuilder) object).build());
                    } else if (object instanceof JsonObjectBuilder) {
                        arrayBuilder.add(((JsonObjectBuilder) object).build());
                    } else if (object instanceof Collection) {
                        throw new IllegalArgumentException(
                            "Collections are not supported for PAddOn, you need to convert it to JsonArray on primitive array");
                    } else {
                        arrayBuilder.add(object.toString());
                    }
                } else {
                    arrayBuilder.addNull();
                }
            }
            builder.add(ARGUMENTS_PROPERTY_NAME, arrayBuilder);
        }

        saveUpdate(writer -> writer.write(ServerToClientModel.NATIVE, builder.build()));
    }

    public void setLogLevel(final Level logLevel) {
        callTerminalMethod("setLogLevel", LOG_LEVEL.get(logLevel));
    }

    public void destroy() {
        saveUpdate(writer -> writer.write(ServerToClientModel.DESTROY));
    }

}
