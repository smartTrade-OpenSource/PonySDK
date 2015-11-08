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

package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.Parser;
import com.ponysdk.ui.server.basic.event.PNativeEvent;
import com.ponysdk.ui.server.basic.event.PNativeHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

/**
 * AddOn are used to bind server side object with javascript object
 */
public abstract class PAddOn extends PObject implements PNativeHandler {

    private static final Logger log = LoggerFactory.getLogger(PAddOn.class);

    private static final int LIMIT = 1000;

    protected boolean attached = false;
    protected List<JsonObjectBuilder> pendingDataToSend = new ArrayList<>();

    private final PElement widget;

    public PAddOn() {
        this(null);
    }

    public PAddOn(final PElement widget) {
        this.widget = widget;
        init();
        addNativeHandler(this);
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        parser.comma();
        parser.parse(Model.FACTORY, getClass().getCanonicalName());
        if (widget != null) {
            parser.comma();
            parser.parse(Model.WIDGET, widget.getID());
        }
    }

    public void update(final JsonObjectBuilder builder) {
        saveUpdate(Model.NATIVE, builder);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ADDON;
    }

    @Override
    public void onNativeEvent(final PNativeEvent event) {
        final JsonObject jsonObject = event.getJsonObject();
        try {
            if (jsonObject.containsKey("attached")) {
                attached = jsonObject.getBoolean("attached");
                if (attached) {
                    sendPendingJSONData();
                    onAttached();
                } else log.debug("Object detached " + this);
            }
        } catch (final Exception e) {
            log.error("Cannot read native event", e);
        }
    }

    public void update(final String key, final JsonObject object) {
        final JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add(key, object);
        update(objectBuilder);
    }

    protected void onAttached() {}

    protected void sendPendingJSONData() {
        final Iterator<JsonObjectBuilder> iterator = pendingDataToSend.iterator();
        while (iterator.hasNext()) {
            final JsonObjectBuilder next = iterator.next();
            update(next);
            iterator.remove();
        }
    }

    public void setJSLogLevel(final int logLevel) {
        callBindedMethod("setLog", logLevel);
    }

    protected void callBindedMethod(final String methodName, final JsonObjectBuilder args) {
        callBindedMethod(methodName, args.build());
    }

    protected void callBindedMethod(final String methodName, final JsonArrayBuilder args) {
        callBindedMethod(methodName, args.build());
    }

    protected void callBindedMethod(final String methodName, final Object... args) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("method", methodName);

        if (args.length > 0) {
            final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            for (final Object object : args) {
                if (object != null) {
                    if (object instanceof JsonValue) arrayBuilder.add(((JsonValue) object));
                    else arrayBuilder.add(object.toString());
                }
            }
            builder.add("args", arrayBuilder);
        }

        if (!attached) {
            if (pendingDataToSend.size() < LIMIT) pendingDataToSend.add(builder);
        } else {
            update(builder);
        }
    }

    public PElement asWidget() {
        return widget;
    }

}
