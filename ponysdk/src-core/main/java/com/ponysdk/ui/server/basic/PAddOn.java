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

import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.instruction.EntryInstruction;
import com.ponysdk.ui.server.basic.event.PNativeEvent;
import com.ponysdk.ui.server.basic.event.PNativeHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

/**
 * AddOn are used to bind server side object with javascript object
 */
public class PAddOn extends PObject implements PNativeHandler {

    private static final Logger log = LoggerFactory.getLogger(PAddOn.class);
    private final PWidget widget;

    /**
     * @param factory
     *            a javascript function that will serve as a factory for this addon
     * @param w
     *            optional widget linked to this addon
     * @param params
     *            optional parameters that will be passed to the create javascript function
     */
    public PAddOn(final String factory, final PWidget w, final JsonObject params) {
        super(new EntryInstruction(Model.FACTORY, factory), new EntryInstruction(Model.NATIVE, params), new EntryInstruction(Model.WIDGET, w != null ? w.ID : null));

        this.widget = w;

        addNativeHandler(this);
    }

    public PAddOn(final PWidget w, final JsonObject params) {
        this(PAddOn.class.getName(), w, params);
    }

    public void update(final JsonObject data) {
        saveUpdate(Model.NATIVE, data);
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

    public void update(final String key1, final Object o1) {
        try {
            final JsonObject jso = new JsonObject();
            jso.put(key1, o1);
            update(jso);
        } catch (final JSONException e) {
            log.error("Failed to encode json", e);
        }
    }

    public PWidget getWidget() {
        return widget;
    }
}
