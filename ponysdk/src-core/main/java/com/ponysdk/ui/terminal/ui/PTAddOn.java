/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *	Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *	Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

package com.ponysdk.ui.terminal.ui;

import java.util.Map;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.JavascriptAddOn;
import com.ponysdk.ui.terminal.JavascriptAddOnFactory;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public class PTAddOn extends AbstractPTObject {

    private JavascriptAddOn addOn;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        final String signature = create.getString(PROPERTY.FACTORY);
        final Map<String, JavascriptAddOnFactory> factories = uiService.getJavascriptAddOnFactory();
        final JavascriptAddOnFactory factory = factories.get(signature);
        if (factory == null) throw new RuntimeException("AddOn factory not found for signature: " + signature + ". Addons registered: " + factories.keySet());

        final JSONObject params = new JSONObject();
        params.put("id", new JSONString(create.getObjectID().toString()));

        if (create.containsKey(PROPERTY.NATIVE)) {
            final JSONObject data = create.getObject(PROPERTY.NATIVE);
            params.put("data", data);
        }

        if (create.containsKey(PROPERTY.WIDGET)) {
            final long widgetID = create.getLong(PROPERTY.WIDGET);
            final PTWidget<?> object = (PTWidget<?>) uiService.getPTObject(widgetID);
            final Widget cast = object.cast();
            final Element element = cast.getElement();
            params.put("widgetID", new JSONString("" + widgetID));
            params.put("widgetElement", new JSONObject(element));
            cast.addAttachHandler(new AttachEvent.Handler() {

                @Override
                public void onAttachOrDetach(final AttachEvent event) {
                    if (event.isAttached()) {
                        addOn.onAttach(true);
                    } else {
                        addOn.onAttach(false);
                    }
                }
            });
        }

        addOn = factory.newAddOn(params.getJavaScriptObject());
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        final JSONObject data = update.getObject(PROPERTY.NATIVE);
        addOn.update(data.getJavaScriptObject());
    }
}
