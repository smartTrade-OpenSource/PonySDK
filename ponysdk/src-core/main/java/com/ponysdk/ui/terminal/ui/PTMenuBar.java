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

import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.UIObject;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.Add;
import com.ponysdk.ui.terminal.instruction.Create;

public class PTMenuBar extends PTWidget {

    @Override
    public void create(Create create, UIService uiService) {
        init(new com.google.gwt.user.client.ui.MenuBar(create.getMainProperty().getBooleanProperty(PropertyKey.MENU_BAR_IS_VERTICAL)));
    }

    @Override
    public void add(Add add, UIService uiService) {

        final UIObject child = uiService.getUIObject(add.getObjectID());
        final com.google.gwt.user.client.ui.MenuBar menuBar = cast();

        final Property beforeIndexProperty = add.getMainProperty().getChildProperty(PropertyKey.BEFORE_INDEX);
        if (child instanceof PTMenuItem) {
            final PTMenuItem menuItem = (PTMenuItem) child;
            if (beforeIndexProperty != null) {
                menuBar.insertItem(menuItem.cast(), beforeIndexProperty.getIntValue());
            } else {
                menuBar.addItem(menuItem.cast());
            }
        } else {
            final PTMenuItemSeparator menuItem = (PTMenuItemSeparator) child;
            menuBar.addSeparator(menuItem.cast());
        }

    }

    @Override
    public com.google.gwt.user.client.ui.MenuBar cast() {
        return (com.google.gwt.user.client.ui.MenuBar) uiObject;
    }

}
