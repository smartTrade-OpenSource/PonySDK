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

import java.util.Map;
import java.util.logging.Level;

import javax.json.JsonObject;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.writer.ModelWriter;

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
            writer.write(ServerToClientModel.PADDON_CREATION, args.toString());
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
     *            the arguments : An array that can contain primitives, String, JsonValue
     */
    protected void callTerminalMethod(final String methodName, final Object... args) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.PADDON_METHOD, methodName);
            if (args.length > 0) writer.write(ServerToClientModel.PADDON_ARGUMENTS, args);
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
