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
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.ui.UIObject;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.UIBuilder;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;

public abstract class PTUIObject<T extends UIObject> extends AbstractPTObject {

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
        final int modelOrdinal = binaryModel.getModel().ordinal();
        if (ServerToClientModel.WIDGET_WIDTH.ordinal() == modelOrdinal) {
            uiObject.setWidth(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.WIDGET_HEIGHT.ordinal() == modelOrdinal) {
            uiObject.setHeight(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.PUT_PROPERTY_KEY.ordinal() == modelOrdinal) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.PROPERTY_VALUE
            uiObject.getElement().setPropertyString(value, buffer.readBinaryModel().getStringValue());
            return true;
        } else if (ServerToClientModel.PUT_ATTRIBUTE_KEY.ordinal() == modelOrdinal) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.ATTRIBUTE_VALUE
            uiObject.getElement().setAttribute(value, buffer.readBinaryModel().getStringValue());
            return true;
        } else if (ServerToClientModel.REMOVE_ATTRIBUTE_KEY.ordinal() == modelOrdinal) {
            uiObject.getElement().removeAttribute(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.STYLE_NAME.ordinal() == modelOrdinal) {
            uiObject.setStyleName(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.STYLE_PRIMARY_NAME.ordinal() == modelOrdinal) {
            uiObject.setStylePrimaryName(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.ADD_STYLE_NAME.ordinal() == modelOrdinal) {
            uiObject.addStyleName(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.REMOVE_STYLE_NAME.ordinal() == modelOrdinal) {
            uiObject.removeStyleName(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.WIDGET_VISIBLE.ordinal() == modelOrdinal) {
            uiObject.setVisible(binaryModel.getBooleanValue());
            return true;
        } else if (ServerToClientModel.ENSURE_DEBUG_ID.ordinal() == modelOrdinal) {
            uiObject.getElement().setAttribute("pid", binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.WIDGET_TITLE.ordinal() == modelOrdinal) {
            uiObject.setTitle(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.PUT_STYLE_KEY.ordinal() == modelOrdinal) {
            final String value = binaryModel.getStringValue();
            // ServerToClientModel.STYLE_VALUE
            uiObject.getElement().getStyle().setProperty(value, buffer.readBinaryModel().getStringValue());
            return true;
        } else if (ServerToClientModel.REMOVE_STYLE_KEY.ordinal() == modelOrdinal) {
            uiObject.getElement().getStyle().clearProperty(binaryModel.getStringValue());
            return true;
        } else if (ServerToClientModel.BIND.ordinal() == modelOrdinal) {
            nativeObject = bind(binaryModel.getStringValue(), objectID, uiObject.getElement());
            return true;
        } else if (ServerToClientModel.NATIVE.ordinal() == modelOrdinal) {
            final JSONObject object = JSONParser.parseStrict(binaryModel.getStringValue()).isObject();
            sendToNative(objectID, nativeObject, object.getJavaScriptObject());
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
