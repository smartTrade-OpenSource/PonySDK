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
package com.ponysdk.ui.server.basic;

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.terminal.Property;
import com.ponysdk.ui.terminal.PropertyKey;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Add;

public class PMenuBar extends PWidget {

    // TODO warning : gwt contains 2 list 1 all items (with separator) + 1 menuItem only
    private final List<PWidget> items = new ArrayList<PWidget>();

    private final boolean vertical;

    public PMenuBar() {
        this(false);
    }

    public PMenuBar(boolean vertical) {
        super();
        addStyleName(PonySDKTheme.MENUBAR);
        this.vertical = vertical;
        final Property mainProperty = new Property();
        mainProperty.setProperty(PropertyKey.MENU_BAR_IS_VERTICAL, vertical);
        setMainProperty(mainProperty);
    }

    @Override
    protected WidgetType getType() {
        return WidgetType.MENU_BAR;
    }

    public PMenuItem addItem(String text) {
        return addItem(new PMenuItem(text, false));
    }

    public PMenuItem addItem(PMenuItem item) {
        return insertItem(item, items.size());
    }

    public PMenuItem addItem(String text, boolean asHTML, PCommand cmd) {
        return addItem(new PMenuItem(text, asHTML, cmd));
    }

    public PMenuItem addItem(String text, boolean asHTML, PMenuBar popup) {
        return addItem(new PMenuItem(text, asHTML, popup));
    }

    public PMenuItem addItem(String text, PCommand cmd) {
        return addItem(new PMenuItem(text, cmd));
    }

    public PMenuItem addItem(String text, PMenuBar popup) {
        return addItem(new PMenuItem(text, popup));
    }

    public PMenuItem insertItem(PMenuItem item, int beforeIndex) throws IndexOutOfBoundsException {
        items.add(beforeIndex, item);
        final Add add = new Add(item.getID(), getID());
        add.getMainProperty().setProperty(PropertyKey.BEFORE_INDEX, beforeIndex);
        getPonySession().stackInstruction(add);
        return item;
    }

    public void addSeparator() {
        addSeparator(new PMenuItemSeparator());
    }

    public PMenuItemSeparator addSeparator(PMenuItemSeparator itemSeparator) {
        return insertSeparator(itemSeparator, items.size());
    }

    public PMenuItemSeparator insertSeparator(PMenuItemSeparator itemSeparator, int beforeIndex) throws IndexOutOfBoundsException {
        items.add(beforeIndex, itemSeparator);
        final Add add = new Add(itemSeparator.getID(), getID());
        add.getMainProperty().setProperty(PropertyKey.BEFORE_INDEX, beforeIndex);
        getPonySession().stackInstruction(add);
        return itemSeparator;
    }

    public boolean isVertical() {
        return vertical;
    }

}
