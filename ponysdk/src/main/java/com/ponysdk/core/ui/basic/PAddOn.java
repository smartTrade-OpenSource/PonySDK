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

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.server.application.Parser;
import com.ponysdk.core.ui.basic.event.PNativeEvent;
import com.ponysdk.core.ui.basic.event.PNativeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.*;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * AddOn are used to bind server side object with javascript object
 */
public abstract class PAddOn<T extends PObject> extends PObject implements PNativeHandler {

    private static final String ARGUMENTS_PROPERTY_NAME = "arg";
    private static final String METHOD_PROPERTY_NAME = "m";
    private static final String ATTACH_PROPERTY_NAME = "att";

    private static final Logger log = LoggerFactory.getLogger(PAddOn.class);

    private static final int LIMIT = 1000;

    protected boolean attached = false;
    protected List<JsonObjectBuilder> pendingDataToSend = new ArrayList<>();

    private final T widget;

    public PAddOn() {
        this(null);
    }

    public PAddOn(final T widget) {
        this.widget = widget;
        if(widget == null) return;

        if(PWindow.EMPTY_WINDOW_ID !=  widget.getWindowID()){
            attach(widget.getWindowID());
        }else {

        }
    }

    @Override
    protected void init0() {
        super.init0();
        setNativeHandler(this);
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        parser.parse(ServerToClientModel.FACTORY, getModuleName(getClass()));
        if (widget != null) {
            parser.parse(ServerToClientModel.WIDGET_ID, widget.getID());
        }
    }

    public static String getModuleName(final Class<?> clazz) {
        Class<?> obj = clazz;

        while (!obj.isAnnotationPresent(Javascript.class)) {
            obj = obj.getSuperclass();
            if (obj == null)
                throw new IllegalArgumentException("Annotation not found for " + clazz.getCanonicalName());
        }

        final Javascript jsAnnotation = obj.getAnnotation(Javascript.class);
        String moduleName = jsAnnotation.value();

        // if no name, take the className, because new pattern es6 classes friendly:
        // java class name == es6 class name == XXXXAddon
        if (moduleName.isEmpty())
            moduleName = obj.getCanonicalName();

        return moduleName;
    }

    public void update(final JsonObject jsonObject) {
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.NATIVE, jsonObject));
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ADDON;
    }

    @Override
    public void onNativeEvent(final PNativeEvent event) {
        final JsonObject jsonObject = event.getJsonObject();
        try {
            if (jsonObject.containsKey(ATTACH_PROPERTY_NAME)) {
                attached = jsonObject.getBoolean(ATTACH_PROPERTY_NAME);
                if (attached) {
                    sendPendingJSONData();
                    onAttached();
                } else
                    log.debug("Object detached " + this);
            }
        } catch (final Exception e) {
            log.error("Cannot read native eventbus", e);
        }
    }

    public void update(final String key, final JsonObject object) {
        final JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add(key, object);
        update(objectBuilder.build());
    }

    protected void onAttached() {
    }

    protected void sendPendingJSONData() {
        final Iterator<JsonObjectBuilder> iterator = pendingDataToSend.iterator();
        while (iterator.hasNext()) {
            final JsonObjectBuilder next = iterator.next();
            update(next.build());
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
                    else
                        arrayBuilder.add(object.toString());
                }
            }
            builder.add(ARGUMENTS_PROPERTY_NAME, arrayBuilder);
        }

        if (!attached) {
            if (pendingDataToSend.size() < LIMIT)
                pendingDataToSend.add(builder);
        } else {
            update(builder.build());
        }
    }

    public T asWidget() {
        return widget;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Javascript {

        String value() default "";

    }

}
