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

import com.ponysdk.core.instruction.Add;
import com.ponysdk.impl.theme.PonySDKTheme;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;

public class PMenuBar extends PWidget {

    // TODO warning : gwt contains 2 list 1 all items (with separator) + 1 menuItem only
    private final List<PWidget> items = new ArrayList<PWidget>();

    private final boolean vertical;

    public PMenuBar() {
        this(false);
    }

    public PMenuBar(final boolean vertical) {
        super();
        this.vertical = vertical;
        addStyleName(PonySDKTheme.MENUBAR);
        create.put(PROPERTY.MENU_BAR_IS_VERTICAL, vertical);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.MENU_BAR;
    }

    public PMenuItem addItem(final String text) {
        return addItem(new PMenuItem(text, false));
    }

    public PMenuItem addItem(final PMenuItem item) {
        return insertItem(item, items.size());
    }

    public PMenuItem addItem(final String text, final boolean asHTML, final PCommand cmd) {
        return addItem(new PMenuItem(text, asHTML, cmd));
    }

    public PMenuItem addItem(final String text, final boolean asHTML, final PMenuBar popup) {
        return addItem(new PMenuItem(text, asHTML, popup));
    }

    public PMenuItem addItem(final String text, final PCommand cmd) {
        return addItem(new PMenuItem(text, cmd));
    }

    public PMenuItem addItem(final String text, final PMenuBar popup) {
        return addItem(new PMenuItem(text, popup));
    }

    public PMenuItem insertItem(final PMenuItem item, final int beforeIndex) throws IndexOutOfBoundsException {
        items.add(beforeIndex, item);
        final Add add = new Add(item.getID(), getID());
        add.put(PROPERTY.BEFORE_INDEX, beforeIndex);
        getPonySession().stackInstruction(add);
        return item;
    }

    public void addSeparator() {
        addSeparator(new PMenuItemSeparator());
    }

    public PMenuItemSeparator addSeparator(final PMenuItemSeparator itemSeparator) {
        return insertSeparator(itemSeparator, items.size());
    }

    public PMenuItemSeparator insertSeparator(final PMenuItemSeparator itemSeparator, final int beforeIndex) throws IndexOutOfBoundsException {
        items.add(beforeIndex, itemSeparator);
        final Add add = new Add(itemSeparator.getID(), getID());
        add.put(PROPERTY.BEFORE_INDEX, beforeIndex);
        getPonySession().stackInstruction(add);
        return itemSeparator;
    }

    public boolean isVertical() {
        return vertical;
    }

}
