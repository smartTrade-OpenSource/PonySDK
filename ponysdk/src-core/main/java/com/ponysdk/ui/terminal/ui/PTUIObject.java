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

import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Update;

public abstract class PTUIObject extends PTObject {

    protected UIObject uiObject;

    protected void init(UIObject uiObject) {
        if (this.uiObject != null) {
            throw new IllegalStateException("init may only be called once.");
        }
        this.uiObject = uiObject;
    }

    public UIObject cast() {
        return uiObject;
    }

    @Override
    public void update(Update update, UIService uiService) {

        final Property property = update.getMainProperty();
        final PropertyKey propertyKey = property.getKey();

        if (PropertyKey.WIDGET_WIDTH.equals(propertyKey)) {
            uiObject.setWidth(property.getValue());
        } else if (PropertyKey.WIDGET_HEIGHT.equals(propertyKey)) {
            uiObject.setHeight(property.getValue());
        } else if (PropertyKey.WIDGET_FONT_SIZE.equals(propertyKey)) {
            uiObject.getElement().getStyle().setProperty("fontSize", property.getValue());
        } else if (PropertyKey.STYLE_PROPERTY.equals(propertyKey)) {
            uiObject.getElement().getStyle().setProperty(property.getStringProperty(PropertyKey.STYLE_KEY), property.getStringProperty(PropertyKey.STYLE_VALUE));
        } else if (PropertyKey.STYLE_NAME.equals(propertyKey)) {
            uiObject.setStyleName(property.getValue());
        } else if (PropertyKey.ADD_STYLE_NAME.equals(propertyKey)) {
            uiObject.addStyleName(property.getValue());
        } else if (PropertyKey.REMOVE_STYLE_NAME.equals(propertyKey)) {
            uiObject.removeStyleName(property.getValue());
        } else if (PropertyKey.ENSURE_DEBUG_ID.equals(propertyKey)) {
            uiObject.ensureDebugId(property.getValue());
        } else if (PropertyKey.WIDGET_TITLE.equals(propertyKey)) {
            uiObject.setTitle(property.getValue());
        } else if (PropertyKey.WIDGET_VISIBLE.equals(propertyKey)) {
            // TODO remove check on instance
            final com.ponysdk.ui.terminal.UIObject parentWidget = uiService.getUIObject(update.getParentID());
            if (parentWidget instanceof com.google.gwt.user.client.ui.Tree) {
                final com.google.gwt.user.client.ui.Tree tree = (com.google.gwt.user.client.ui.Tree) parentWidget;
                final String positionPath = property.getStringProperty(PropertyKey.TREE_ITEM_POSITION_PATH);
                final String[] positions = positionPath.split("\\.");
                TreeItem item = null;
                for (int i = 0; i <= positions.length - 1; i++) {
                    if (item == null)
                        item = tree.getItem(Integer.parseInt(positions[i]));
                    else
                        item = item.getChild(Integer.parseInt(positions[i]));
                }
                item.setVisible(property.getBooleanValue());
            } else {
                uiObject.setVisible(property.getBooleanValue());
            }
        }

    }

    public Widget asWidget(Long objectID, UIService uiService) {
        final com.ponysdk.ui.terminal.ui.PTWidget child = (com.ponysdk.ui.terminal.ui.PTWidget) uiService.getUIObject(objectID);
        return child.cast();
    }

}
