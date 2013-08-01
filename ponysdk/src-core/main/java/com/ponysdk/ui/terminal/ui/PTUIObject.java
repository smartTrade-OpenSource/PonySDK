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
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.UIObject;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;

public abstract class PTUIObject<T extends UIObject> extends AbstractPTObject {

    private static final String FONT_SIZE = "fontSize";

    protected T uiObject;

    private Object nativeObject;

    protected void init(final PTInstruction create, final UIService uiService, final T uiObject) {
        if (this.uiObject != null) { throw new IllegalStateException("init may only be called once."); }
        this.uiObject = uiObject;
        if (create != null) {
            this.objectID = create.getObjectID();
            uiService.registerUIObject(this.objectID, uiObject);
        }
    }

    public T cast() {
        return uiObject;
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.WIDGET_WIDTH)) {
            uiObject.setWidth(update.getString(PROPERTY.WIDGET_WIDTH));
        } else if (update.containsKey(PROPERTY.WIDGET_HEIGHT)) {
            uiObject.setHeight(update.getString(PROPERTY.WIDGET_HEIGHT));
        } else if (update.containsKey(PROPERTY.WIDGET_FONT_SIZE)) {
            uiObject.getElement().getStyle().setProperty(FONT_SIZE, update.getString(PROPERTY.WIDGET_FONT_SIZE));
        } else if (update.containsKey(PROPERTY.PUT_PROPERTY_KEY)) {
            uiObject.getElement().setPropertyString(update.getString(PROPERTY.PUT_PROPERTY_KEY), update.getString(PROPERTY.PROPERTY_VALUE));
        } else if (update.containsKey(PROPERTY.PUT_ATTRIBUTE_KEY)) {
            uiObject.getElement().setAttribute(update.getString(PROPERTY.PUT_ATTRIBUTE_KEY), update.getString(PROPERTY.ATTRIBUTE_VALUE));
        } else if (update.containsKey(PROPERTY.REMOVE_ATTRIBUTE_KEY)) {
            uiObject.getElement().removeAttribute(update.getString(PROPERTY.REMOVE_ATTRIBUTE_KEY));
        } else if (update.containsKey(PROPERTY.STYLE_NAME)) {
            uiObject.setStyleName(update.getString(PROPERTY.STYLE_NAME));
        } else if (update.containsKey(PROPERTY.STYLE_PRIMARY_NAME)) {
            uiObject.setStylePrimaryName(update.getString(PROPERTY.STYLE_PRIMARY_NAME));
        } else if (update.containsKey(PROPERTY.ADD_STYLE_NAME)) {
            uiObject.addStyleName(update.getString(PROPERTY.ADD_STYLE_NAME));
        } else if (update.containsKey(PROPERTY.REMOVE_STYLE_NAME)) {
            uiObject.removeStyleName(update.getString(PROPERTY.REMOVE_STYLE_NAME));
        } else if (update.containsKey(PROPERTY.WIDGET_VISIBLE)) {
            uiObject.setVisible(update.getBoolean(PROPERTY.WIDGET_VISIBLE));
        } else if (update.containsKey(PROPERTY.ENSURE_DEBUG_ID)) {
            uiObject.ensureDebugId(update.getString(PROPERTY.ENSURE_DEBUG_ID));
        } else if (update.containsKey(PROPERTY.WIDGET_TITLE)) {
            uiObject.setTitle(update.getString(PROPERTY.WIDGET_TITLE));
        } else if (update.containsKey(PROPERTY.PUT_STYLE_KEY)) {
            uiObject.getElement().getStyle().setProperty(update.getString(PROPERTY.PUT_STYLE_KEY), update.getString(PROPERTY.STYLE_VALUE));
        } else if (update.containsKey(PROPERTY.REMOVE_STYLE_KEY)) {
            uiObject.getElement().getStyle().clearProperty(update.getString(PROPERTY.REMOVE_STYLE_KEY));
        } else if (update.containsKey(PROPERTY.BIND)) {
            nativeObject = bind(update.getString(PROPERTY.BIND), objectID.toString(), uiObject.getElement());
        } else if (update.containsKey(PROPERTY.NATIVE)) {
            final JSONObject object = update.getObject(PROPERTY.NATIVE);
            sendToNative(objectID.toString(), nativeObject, object.getJavaScriptObject());
        }
    }

    public UIObject asWidget(final Long objectID, final UIService uiService) {
        if (uiService.getPTObject(objectID) instanceof PTUIObject) { return ((PTUIObject<?>) uiService.getPTObject(objectID)).cast(); }
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
