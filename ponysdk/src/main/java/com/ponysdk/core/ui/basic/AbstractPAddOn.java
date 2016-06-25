/*
 * Copyright (c) 2011 PonySDK
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

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.Parser;

/**
 * AddOn are used to bind server side object with javascript object
 */
public abstract class AbstractPAddOn extends PObject implements PAddOn {

    private static final String ARGUMENTS_PROPERTY_NAME = "arg";
    private static final String METHOD_PROPERTY_NAME = "m";

    @Override
    public boolean attach(final PWindow window) {
        return super.attach(window);
    }

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
                    public void registered(final int windowID) {
                        init();
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
        return getClass().getSimpleName();
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

    public void setJSLogLevel(final int logLevel) {
        callTerminalMethod("setLog", logLevel);
    }

    protected void callBindedMethod(final String methodName, final JsonObjectBuilder args) {
        callTerminalMethod(methodName, args.build());
    }

    protected void callBindedMethod(final String methodName, final JsonArrayBuilder args) {
        callTerminalMethod(methodName, args.build());
    }

    protected void callTerminalMethod(final String methodName, final Object... args) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(METHOD_PROPERTY_NAME, methodName);

        if (args.length > 0) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (final Object object : args) {
                if (object != null) {
                    if (object instanceof JsonValue)
                        arrayBuilder.add((JsonValue) object);
                    else if (object instanceof Boolean)
                        arrayBuilder.add((Boolean) object);
                    else if (object instanceof Integer)
                        arrayBuilder.add((Integer) object);
                    else if (object instanceof Long)
                        arrayBuilder.add((Long) object);
                    else if (object instanceof JsonArrayBuilder)
                        arrayBuilder.add(((JsonArrayBuilder) object).build());
                    else if (object instanceof JsonObjectBuilder)
                        arrayBuilder.add(((JsonObjectBuilder) object).build());
                    else
                        arrayBuilder.add(object.toString());
                }
            }
            builder.add(ARGUMENTS_PROPERTY_NAME, arrayBuilder);
        }

        update(builder.build());
    }

}
