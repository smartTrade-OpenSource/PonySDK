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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.UIObject;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

import elemental.json.JsonObject;

public abstract class PTUIObject<T extends UIObject> extends AbstractPTObject {

    private static final String PID_KEY = "pid";

    protected T uiObject;

    private Object nativeObject;

    protected abstract T createUIObject();

    @Override
    public void create(final ReaderBuffer buffer, final int objectId, final UIBuilder uiBuilder) {
        super.create(buffer, objectId, uiBuilder);
        uiObject = createUIObject();
        uiBuilder.registerUIObject(objectID, uiObject);
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.WIDGET_WIDTH == model) {
            uiObject.setWidth(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.WIDGET_HEIGHT == model) {
            uiObject.setHeight(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.PUT_PROPERTY_KEY == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.PROPERTY_VALUE
            uiObject.getElement().setPropertyString(value, buffer.readBinaryModel().getStringValue());
            return true;
        } else if (ServerToClientModel.PUT_ATTRIBUTE_KEY == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.ATTRIBUTE_VALUE
            uiObject.getElement().setAttribute(value, buffer.readBinaryModel().getStringValue());
            return true;
        } else if (ServerToClientModel.REMOVE_ATTRIBUTE_KEY == model) {
            uiObject.getElement().removeAttribute(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.STYLE_NAME == model) {
            uiObject.setStyleName(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.STYLE_PRIMARY_NAME == model) {
            uiObject.setStylePrimaryName(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.ADD_STYLE_NAME == model) {
            uiObject.addStyleName(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.REMOVE_STYLE_NAME == model) {
            uiObject.removeStyleName(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.WIDGET_VISIBLE == model) {
            uiObject.setVisible(binaryModel.getBooleanValue());
            return true;
        } else if (ServerToClientModel.ENSURE_DEBUG_ID == model) {
            uiObject.getElement().setAttribute(PID_KEY, binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.WIDGET_TITLE == model) {
            uiObject.setTitle(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.PUT_STYLE_KEY == model) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.STYLE_VALUE
            uiObject.getElement().getStyle().setProperty(value, buffer.readBinaryModel().getStringValue());
            return true;
        } else if (ServerToClientModel.REMOVE_STYLE_KEY == model) {
            uiObject.getElement().getStyle().clearProperty(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.BIND == model) {
            nativeObject = bind(binaryModel.getStringValue(), objectID, uiObject.getElement());
            return true;
        } else if (ServerToClientModel.NATIVE == model) {
            final JsonObject object = binaryModel.getJsonObject();
            sendToNative(objectID, nativeObject, (JavaScriptObject) object);
            return true;
        } else if (ServerToClientModel.TABINDEX == model) {
            uiObject.getElement().setTabIndex(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.FOCUS == model) {
            if (binaryModel.getBooleanValue()) uiObject.getElement().focus();
            else uiObject.getElement().blur();
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    public <WIDGET_TYPE extends UIObject> WIDGET_TYPE asWidget(final int objectID, final UIBuilder uiService) {
        return asWidget(uiService.getPTObject(objectID));
    }

    public <WIDGET_TYPE extends UIObject> WIDGET_TYPE asWidget(final PTObject ptObject) {
        if (ptObject instanceof PTUIObject) return ((PTUIObject<WIDGET_TYPE>) ptObject).uiObject;
        else throw new IllegalStateException("This object is not an UIObject");
    }

    private native Object bind(String functionName, int objectID, Element element) /*-{
                                                                                   var self = this;
                                                                                   var o = $wnd[functionName](objectID, element);
                                                                                   return o;
                                                                                   }-*/;

    private native void sendToNative(int objectID, Object nativeObject, JavaScriptObject data) /*-{
                                                                                               nativeObject.update(data);
                                                                                               }-*/;

}
