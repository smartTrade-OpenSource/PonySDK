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
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.pony-MenuBar</dt>
 * <dd>the bar (role menubar, or menu in a submenu); <code>[aria-orientation]</code></dd>
 * <dt>.pony-MenuItem</dt>
 * <dd>an item (role menuitem); <code>[aria-expanded=true]</code> while its submenu shows, <code>[aria-disabled=true]</code></dd>
 * <dt>.pony-MenuItemSeparator</dt>
 * <dd>a separator</dd>
 * <dt>.pony-MenuPopup</dt>
 * <dd>the popup of a submenu</dd>
 * </dl>
 */
public class PMenuBar extends PWidget implements Iterable<PMenuSubElement> {

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
        forEach(item -> item.attach(window, getFrame()));
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
        elt.attach(window, getFrame());
        return elt;
    }

    public <T extends PMenuSubElement> T insertElement(final T elt, final int beforeIndex) throws IndexOutOfBoundsException {
        items.add(beforeIndex, elt);
        elt.saveAdd(elt.getID(), ID, new ServerBinaryModel(ServerToClientModel.BEFORE_INDEX, beforeIndex));
        elt.attach(window, getFrame());
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
