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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.ponysdk.core.Parser;
import com.ponysdk.ui.server.basic.event.PNativeEvent;
import com.ponysdk.ui.server.basic.event.PNativeHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

/**
 * AddOn are used to bind server side object with javascript object
 */
public class PAddOn extends PObject implements PNativeHandler {

    private final PWidget widget;
    private final String factory;
    private final JsonObject params;

    /**
     * @param factory
     *            a javascript function that will serve as a factory for this addon
     * @param w
     *            optional widget linked to this addon
     * @param params
     *            optional parameters that will be passed to the create javascript function
     */
    public PAddOn(final String factory, final PWidget w, final JsonObject params) {
        this.widget = w;
        this.factory = factory;
        this.params = params;

        init();

        addNativeHandler(this);
    }

    public PAddOn(final PWidget w, final JsonObject params) {
        this(PAddOn.class.getName(), w, params);
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        // parser.parse(Model.addOnSignature, getSignature());
        parser.comma();
        parser.parse(Model.FACTORY, factory);
        parser.comma();
        parser.parse(Model.NATIVE, params);
        parser.comma();
        parser.parse(Model.WIDGET, widget != null ? widget.ID : null);
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
        restate(jsonObject);
    }

    protected void restate(final JsonObject jsonObject) {}

    public void update(final String key, final JsonObject object) {
        final JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add(key, object);
        update(objectBuilder);
    }

    public PWidget getWidget() {
        return widget;
    }
}
