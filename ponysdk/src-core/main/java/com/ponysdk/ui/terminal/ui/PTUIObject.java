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

import com.google.gwt.user.client.ui.UIObject;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Create;
import com.ponysdk.ui.terminal.instruction.Update;

public abstract class PTUIObject extends PTObject {

    private static final String FONT_SIZE = "fontSize";

    protected UIObject uiObject;

    protected void init(final Create create, final UIService uiService, final UIObject uiObject) {
        if (this.uiObject != null) { throw new IllegalStateException("init may only be called once."); }
        this.uiObject = uiObject;
        if (create != null) {
            this.objectID = create.getObjectID();
            uiService.registerUIObject(this.objectID, uiObject);
        }
    }

    public UIObject cast() {
        return uiObject;
    }

    @Override
    public void update(final Update update, final UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();

        switch (propertyKey) {
            case WIDGET_WIDTH:
                uiObject.setWidth(property.getValue());
                break;
            case WIDGET_HEIGHT:
                uiObject.setHeight(property.getValue());
                break;
            case WIDGET_FONT_SIZE:
                uiObject.getElement().getStyle().setProperty(FONT_SIZE, property.getValue());
                break;
            case STYLE_PROPERTY:
                uiObject.getElement().getStyle().setProperty(property.getStringProperty(PropertyKey.STYLE_KEY), property.getStringProperty(PropertyKey.STYLE_VALUE));
                break;
            case STYLE_NAME:
                uiObject.setStyleName(property.getValue());
                break;
            case ADD_STYLE_NAME:
                uiObject.addStyleName(property.getValue());
                break;
            case REMOVE_STYLE_NAME:
                uiObject.removeStyleName(property.getValue());
                break;
            case ENSURE_DEBUG_ID:
                uiObject.ensureDebugId(property.getValue());
                break;
            case WIDGET_TITLE:
                uiObject.setTitle(property.getValue());
                break;
            case WIDGET_VISIBLE:
                uiObject.setVisible(property.getBooleanValue());
                break;

            default:
                break;
        }
    }

    public UIObject asWidget(final Long objectID, final UIService uiService) {
        if (uiService.getPTObject(objectID) instanceof PTUIObject) { return ((PTUIObject) uiService.getPTObject(objectID)).cast(); }
        throw new IllegalStateException("This object is not an UIObject");
    }
}
