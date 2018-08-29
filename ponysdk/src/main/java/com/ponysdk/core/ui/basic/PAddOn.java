/*
 * Copyright (c) 2017 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.UIContext;
import com.ponysdk.core.writer.ModelWriter;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;

/**
 * AddOn are used to bind server side with javascript browser
 */
public abstract class PAddOn extends PObject {

    private static final Map<Level, Byte> LOG_LEVEL = Map.of(Level.OFF, (byte) 0, Level.SEVERE, (byte) 1, Level.WARNING, (byte) 2,
        Level.INFO, (byte) 3, Level.CONFIG, (byte) 4, Level.FINE, (byte) 5, Level.FINER, (byte) 6, Level.FINEST, (byte) 7, Level.ALL,
        (byte) 8);

    private JsonObject args;

    /**
     * Instantiate a new PAddOn
     */
    protected PAddOn() {
    }

    /**
     * Instantiate a new PAddOn
     *
     * @param args
     *            the JsonObject arguments
     */
    protected PAddOn(final JsonObject args) {
        this.args = args;
    }

    /**
     * Attach the PAddOn to a window
     *
     * @param window
     *            the window
     * @return true, if successful
     */
    public boolean attach(final PWindow window) {
        return attach(window, null);
    }

    /**
     * Attach the PAddOn to a frame if not null else to a window
     *
     * @param window
     *            the window
     * @param frame
     *            the frame
     * @return true, if successful
     */
    @Override
    public boolean attach(final PWindow window, final PFrame frame) {
        final boolean result = super.attach(window, frame);
        if (result) window.addDestroyListener(event -> onDestroy());
        return result;
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        writer.write(ServerToClientModel.FACTORY, getSignature());
        if (args != null) {
            writer.write(ServerToClientModel.PADDON_CREATION, args);
            args = null;
        }
    }

    /**
     * Get the signature
     *
     * @return the signature
     */
    public String getSignature() {
        return getClass().getCanonicalName();
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ADDON;
    }

    /**
     * Call terminal method
     *
     * @param methodName
     *            the method name
     * @param args
     *            the arguments
     */
    protected void callTerminalMethod(final String methodName, final Object... args) {
        final JsonObject arguments;
        if (args.length > 0) {
            final JsonArrayBuilder arrayBuilder = UIContext.get().getJsonProvider().createArrayBuilder();
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

            final JsonObjectBuilder argumentsBuilder = UIContext.get().getJsonProvider().createObjectBuilder();
            argumentsBuilder.add("arg", arrayBuilder.build());
            arguments = argumentsBuilder.build();
        } else {
            arguments = null;
        }

        saveUpdate(writer -> {
            writer.write(ServerToClientModel.PADDON_METHOD, methodName);
            if (arguments != null) writer.write(ServerToClientModel.PADDON_ARGUMENTS, arguments);
        });
    }

    /**
     * Set the log level
     *
     * @param logLevel
     *            the new log level
     */
    public void setLogLevel(final Level logLevel) {
        callTerminalMethod("setLogLevel", LOG_LEVEL.get(logLevel));
    }

    /**
     * Destroy
     */
    public void destroy() {
        saveUpdate(writer -> writer.write(ServerToClientModel.DESTROY));
    }

}
