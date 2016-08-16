
package com.ponysdk.core.ui.basic;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.Parser;

public abstract class PAddOn extends PObject {

    private static final String ARGUMENTS_PROPERTY_NAME = "arg";
    private static final String METHOD_PROPERTY_NAME = "m";

    @Override
    public boolean attach(final int windowID) {
        if (this.windowID == PWindow.EMPTY_WINDOW_ID && windowID != PWindow.EMPTY_WINDOW_ID) {
            this.windowID = windowID;

            final PWindow window = PWindowManager.getWindow(windowID);

            if (window != null && window.isOpened()) {
                init();
            } else {
                PWindowManager.addWindowListener(new PWindowManager.RegisterWindowListener() {

                    @Override
                    public void registered(final int registeredWindowID) {
                        if (windowID == registeredWindowID) init();
                    }

                    @Override
                    public void unregistered(final int windowID) {
                    }
                });
            }

            return true;
        } else if (this.windowID != windowID) {
            throw new IllegalAccessError(
                    "Widget already attached to an other window, current window : #" + this.windowID + ", new window : #" + windowID);
        }
        return false;
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        parser.parse(ServerToClientModel.FACTORY, getSignature());
    }

    public String getSignature() {
        return getClass().getCanonicalName();
    }

    public void update(final JsonObject jsonObject) {
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.NATIVE, jsonObject));
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ADDON;
    }

    public void update(final String key, final JsonObject object) {
        final JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add(key, object);
        update(objectBuilder.build());
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
                        else if (object instanceof Long)
                            arrayBuilder.add(number.longValue());
                        else if (object instanceof Float || object instanceof Double)
                            arrayBuilder.add(number.doubleValue());
                        else if (object instanceof BigInteger)
                            arrayBuilder.add((BigInteger) object);
                        else if (object instanceof BigDecimal)
                            arrayBuilder.add((BigDecimal) object);
                        else
                            arrayBuilder.add(number.doubleValue());
                    } else if (object instanceof Boolean) {
                        arrayBuilder.add((Boolean) object);
                    } else if (object instanceof JsonArrayBuilder) {
                        arrayBuilder.add(((JsonArrayBuilder) object).build());
                    } else if (object instanceof JsonObjectBuilder) {
                        arrayBuilder.add(((JsonObjectBuilder) object).build());
                    } else if (object instanceof Collection) {
                        ((Collection) object).toArray();
                        System.err.println("OHEH");
                    } else {
                        arrayBuilder.add(object.toString());
                    }
                } else {
                    arrayBuilder.addNull();
                }
            }
            builder.add(ARGUMENTS_PROPERTY_NAME, arrayBuilder);
        }

        update(builder.build());
    }

}
