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

package com.ponysdk.core.ui.basic;

import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.writer.ModelWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * A standard menu bar widget. A menu bar can contain any number of menu items,
 * each of which can either fire a {@link Runnable} or open a cascaded menu bar.
 * <h2>CSS Style Rules</h2>
 * <dl>
 * <dt>.gwt-MenuBar</dt>
 * <dd>the menu bar itself</dd>
 * <dt>.gwt-MenuBar-horizontal</dt>
 * <dd>dependent style applied to horizontal menu bars</dd>
 * <dt>.gwt-MenuBar-vertical</dt>
 * <dd>dependent style applied to vertical menu bars</dd>
 * <dt>.gwt-MenuBar .gwt-MenuItem</dt>
 * <dd>menu items</dd>
 * <dt>.gwt-MenuBar .gwt-MenuItem-selected</dt>
 * <dd>selected menu items</dd>
 * <dt>.gwt-MenuBar .gwt-MenuItemSeparator</dt>
 * <dd>section breaks between menu items</dd>
 * <dt>.gwt-MenuBar .gwt-MenuItemSeparator .menuSeparatorInner</dt>
 * <dd>inner component of section separators</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupTopLeft</dt>
 * <dd>the top left cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupTopLeftInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupTopCenter</dt>
 * <dd>the top center cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupTopCenterInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupTopRight</dt>
 * <dd>the top right cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupTopRightInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupMiddleLeft</dt>
 * <dd>the middle left cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupMiddleLeftInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupMiddleCenter</dt>
 * <dd>the middle center cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupMiddleCenterInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupMiddleRight</dt>
 * <dd>the middle right cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupMiddleRightInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupBottomLeft</dt>
 * <dd>the bottom left cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupBottomLeftInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupBottomCenter</dt>
 * <dd>the bottom center cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupBottomCenterInner</dt>
 * <dd>the inner element of the cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupBottomRight</dt>
 * <dd>the bottom right cell</dd>
 * <dt>.gwt-MenuBarPopup .menuPopupBottomRightInner</dt>
 * <dd>the inner element of the cell</dd>
 * </dl>
 * <p>
 * MenuBar elements in UiBinder template files can have a <code>vertical</code>
 * boolean attribute (which defaults to false), and may have only MenuItem
 * elements as children. MenuItems may contain HTML and MenuBars.
 * </p>
 */
public class PMenuBar extends PWidget implements Iterable<PMenuSubElement> {

    // TODO warning : gwt contains 2 list 1 all items (with separator) + 1 menuItem only
    private static final Logger log = LoggerFactory.getLogger(PMenuBar.class);

    private final List<PMenuSubElement> items = new ArrayList<>();
    private final boolean vertical;
    private boolean animationEnabled = false;

    protected PMenuBar() {
        this(false);
    }

    protected PMenuBar(final boolean vertical) {
        this.vertical = vertical;
    }

    @Override
    void init0() {
        super.init0();
        forEach(item -> item.attach(window, frame));
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        writer.write(ServerToClientModel.VERTICAL, vertical);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.MENU_BAR;
    }

    public PMenuItem addItem(final String text) {
        return addItem(new PMenuItem(text, false));
    }

    public PMenuItem addItem(final String text, final boolean asHTML, final Runnable cmd) {
        return addItem(new PMenuItem(text, asHTML, cmd));
    }

    public PMenuItem addItem(final String text, final boolean asHTML, final PMenuBar popup) {
        return addItem(new PMenuItem(text, asHTML, popup));
    }

    public PMenuItem addItem(final String text, final Runnable cmd) {
        return addItem(new PMenuItem(text, cmd));
    }

    public PMenuItem addItem(final String text, final PMenuBar popup) {
        return addItem(new PMenuItem(text, popup));
    }

    public PMenuItem addItem(final PMenuItem elt) {
        return addElement(elt);
    }

    public PMenuSubElement getItem(final int index) {
        return items.get(index);
    }

    public <T extends PMenuSubElement> T addElement(final T elt) {
        items.add(elt);
        elt.saveAdd(elt.getID(), ID);
        elt.attach(window, frame);
        return elt;
    }

    public <T extends PMenuSubElement> T insertElement(final T elt, final int beforeIndex) throws IndexOutOfBoundsException {
        items.add(beforeIndex, elt);
        elt.saveAdd(elt.getID(), ID, new ServerBinaryModel(ServerToClientModel.BEFORE_INDEX, beforeIndex));
        elt.attach(window, frame);
        return elt;
    }

    public boolean removeItem(final int index) {
        final PMenuSubElement item = items.remove(index);
        item.saveRemove(item.getID(), ID);
        return true;
    }

    public boolean removeItem(final PMenuSubElement elt) {
        final boolean removed = items.remove(elt);
        if (removed) elt.saveRemove(elt.getID(), ID);
        return removed;
    }

    public void addSeparator() {
        addElement(new PMenuItemSeparator());
    }

    public void clearItems() {
        items.clear();
        saveUpdate(writer -> writer.write(ServerToClientModel.CLEAR));
    }

    public boolean isVertical() {
        return vertical;
    }

    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    public void setAnimationEnabled(final boolean animationEnabled) {
        if (Objects.equals(this.animationEnabled, animationEnabled)) return;
        this.animationEnabled = animationEnabled;
        saveUpdate(ServerToClientModel.ANIMATION, animationEnabled);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        forEach(this::doDestroy);
    }

    private void doDestroy(PMenuSubElement item) {
        try {
            item.onDestroy();
        } catch (Exception e) {
            log.error("An error occurred while trying to process the destroy event", e);
        }
    }

    @Override
    public Iterator<PMenuSubElement> iterator() {
        return items.iterator();
    }

    @Override
    public String dumpDOM() {
        String DOM = "<ul pid=\"" + ID + "\">";

        for (PMenuSubElement item : items) {
            DOM += item.dumpDOM();
        }

        DOM += "</ul>";

        return DOM;
    }
}
