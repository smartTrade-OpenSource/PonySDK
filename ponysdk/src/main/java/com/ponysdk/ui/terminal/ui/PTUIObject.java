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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.ui.UIObject;
import com.ponysdk.ui.terminal.UIBuilder;
import com.ponysdk.ui.terminal.model.BinaryModel;
import com.ponysdk.ui.terminal.model.ReaderBuffer;
import com.ponysdk.ui.terminal.model.ServerToClientModel;

public abstract class PTUIObject<T extends UIObject> extends AbstractPTObject {

    private static final String FONT_SIZE = "fontSize";

    protected T uiObject;

    private Object nativeObject;

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiService) {
        super.create(buffer, objectId, uiService);
        this.uiObject = createUIObject();
        uiService.registerUIObject(this.objectID, uiObject);
    }

    protected abstract T createUIObject();

    public T cast() {
        return uiObject;
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        if (ServerToClientModel.WIDGET_WIDTH.equals(binaryModel.getModel())) {
            uiObject.setWidth(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.WIDGET_HEIGHT.equals(binaryModel.getModel())) {
            uiObject.setHeight(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.PUT_PROPERTY_KEY.equals(binaryModel.getModel())) {
            // ServerToClientModel.PROPERTY_VALUE
            uiObject.getElement().setPropertyString(binaryModel.getStringValue(),
                    buffer.getBinaryModel().getStringValue());
            return true;
        }
        if (ServerToClientModel.PUT_ATTRIBUTE_KEY.equals(binaryModel.getModel())) {
            // ServerToClientModel.ATTRIBUTE_VALUE
            uiObject.getElement().setAttribute(binaryModel.getStringValue(), buffer.getBinaryModel().getStringValue());
            return true;
        }
        if (ServerToClientModel.REMOVE_ATTRIBUTE_KEY.equals(binaryModel.getModel())) {
            uiObject.getElement().removeAttribute(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.STYLE_NAME.equals(binaryModel.getModel())) {
            uiObject.setStyleName(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.STYLE_PRIMARY_NAME.equals(binaryModel.getModel())) {
            uiObject.setStylePrimaryName(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.ADD_STYLE_NAME.equals(binaryModel.getModel())) {
            uiObject.addStyleName(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.REMOVE_STYLE_NAME.equals(binaryModel.getModel())) {
            uiObject.removeStyleName(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.WIDGET_VISIBLE.equals(binaryModel.getModel())) {
            uiObject.setVisible(binaryModel.getBooleanValue());
            return true;
        }
        if (ServerToClientModel.ENSURE_DEBUG_ID.equals(binaryModel.getModel())) {
            uiObject.ensureDebugId(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.WIDGET_TITLE.equals(binaryModel.getModel())) {
            uiObject.setTitle(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.PUT_STYLE_KEY.equals(binaryModel.getModel())) {
            // ServerToClientModel.STYLE_VALUE
            uiObject.getElement().getStyle().setProperty(binaryModel.getStringValue(),
                    buffer.getBinaryModel().getStringValue());
            return true;
        }
        if (ServerToClientModel.REMOVE_STYLE_KEY.equals(binaryModel.getModel())) {
            uiObject.getElement().getStyle().clearProperty(binaryModel.getStringValue());
            return true;
        }
        if (ServerToClientModel.BIND.equals(binaryModel.getModel())) {
            nativeObject = bind(binaryModel.getStringValue(), String.valueOf(objectID), uiObject.getElement());
            return true;
        }
        if (ServerToClientModel.NATIVE.equals(binaryModel.getModel())) {
            final JSONObject object = JSONParser.parseStrict(binaryModel.getStringValue()).isObject();
            sendToNative(String.valueOf(objectID), nativeObject, object.getJavaScriptObject());
            return true;
        }
        return super.update(buffer, binaryModel);
    }

    public UIObject asWidget(final int objectID, final UIBuilder uiService) {
        final PTObject ptObject = uiService.getPTObject(objectID);
        return asWidget(ptObject);
    }

    public UIObject asWidget(final PTObject ptObject) {
        if (ptObject instanceof PTUIObject) {
            return ((PTUIObject<?>) ptObject).cast();
        }
        throw new IllegalStateException("This object is not an UIObject");
    }

    private native Object bind(String functionName, String objectID, Element element) /*-{
                                                                                      var self = this;
                                                                                      var o = $wnd[functionName](objectID, element);
                                                                                      return o;
                                                                                      }-*/;

    private native void sendToNative(String objectID, Object nativeObject, JavaScriptObject data) /*-{
                                                                                                  nativeObject.update(data);
                                                                                                  }-*/;

}
