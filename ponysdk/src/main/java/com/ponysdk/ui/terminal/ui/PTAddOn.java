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
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.model.ServerToClientModel;
import com.ponysdk.ui.terminal.JavascriptAddOn;
import com.ponysdk.ui.terminal.JavascriptAddOnFactory;
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;

public class PTAddOn extends AbstractPTObject {

    private JavascriptAddOn addOn;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        super.create(buffer, objectId, uiService);

        // ServerToClientModel.FACTORY
        final String signature = buffer.getBinaryModel().getStringValue();
        final Map<String, JavascriptAddOnFactory> factories = uiService.getJavascriptAddOnFactory();
        final JavascriptAddOnFactory factory = factories.get(signature);
        if (factory == null)
            throw new RuntimeException("AddOn factory not found for signature: " + signature + ". Addons registered: "
                    + factories.keySet());

        final JSONObject params = new JSONObject();
        params.put("id", new JSONNumber(objectId));

        final BinaryModel binaryModel = buffer.getBinaryModel();
        if (ServerToClientModel.WIDGET_ID.equals(binaryModel.getModel())) {
            final int widgetID = binaryModel.getIntValue();
            final PTWidget<?> object = (PTWidget<?>) uiService.getPTObject(widgetID);
            final Widget cast = object.cast();
            final Element element = cast.getElement();
            params.put("widgetID", new JSONString(String.valueOf(widgetID)));
            params.put("widgetElement", new JSONObject(element));
            cast.addAttachHandler(new AttachEvent.Handler() {

                @Override
                public void onAttachOrDetach(final AttachEvent event) {
                    addOn.onAttach(event.isAttached());
                }
            });
        } else {
            buffer.rewind(binaryModel);
        }

        addOn = factory.newAddOn(params.getJavaScriptObject());
        addOn.onInit();
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.NATIVE.equals(binaryModel.getModel())) {
            final JSONObject data = binaryModel.getJsonObject();
            addOn.update(data.getJavaScriptObject());
            return true;
        }
        return super.update(buffer, binaryModel);
    }
}
