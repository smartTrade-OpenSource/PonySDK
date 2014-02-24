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

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.instruction.Update;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.server.basic.event.PNativeEvent;
import com.ponysdk.ui.server.basic.event.PNativeHandler;
import com.ponysdk.ui.terminal.Dictionnary;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * AddOn are used to bind server side object with javascript object
 */
public class PAddOn extends PObject implements PNativeHandler {

    private static final Logger log = LoggerFactory.getLogger(PAddOn.class);

    /**
     * @param factory
     *            a javascript function that will serve as a factory for this addon
     * @param w
     *            optional widget linked to this addon
     * @param params
     *            optional parameters that will be passed to the create javascript function
     */
    public PAddOn(final String factory, final PWidget w, final JSONObject params) {
        super();

        if (factory != null) create.put(PROPERTY.FACTORY, factory);
        else create.put(PROPERTY.FACTORY, getClass().getName());

        if (params != null) create.put(PROPERTY.NATIVE, params);
        if (w != null) create.put(PROPERTY.WIDGET, w.ID);

        addNativeHandler(this);
    }

    public void update(final JSONObject data) {
        final Update update = new Update(getID());
        update.put(Dictionnary.PROPERTY.NATIVE, data);
        Txn.get().getTxnContext().save(update);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.ADDON;
    }

    @Override
    public void onNativeEvent(final PNativeEvent event) {
        final JSONObject jsonObject = event.getJsonObject();
        try {
            restate(jsonObject);
        } catch (final JSONException e) {
            log.error("Failed to read json value in #" + jsonObject, e);
        }
    }

    protected void restate(final JSONObject jsonObject) throws JSONException {}

    public void update(final String key1, final Object o1) {
        try {
            final JSONObject jso = new JSONObject();
            jso.put(key1, o1);
            update(jso);
        } catch (final JSONException e) {
            log.error("Failed to encode json", e);
        }
    }
}
