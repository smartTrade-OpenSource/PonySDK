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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.ponysdk.core.UIContext;
import com.ponysdk.core.instruction.EntryInstruction;
import com.ponysdk.core.instruction.Parser;
import com.ponysdk.core.stm.Txn;
import com.ponysdk.ui.server.basic.event.HasPBeforeSelectionHandlers;
import com.ponysdk.ui.server.basic.event.HasPSelectionHandlers;
import com.ponysdk.ui.server.basic.event.HasPWidgets;
import com.ponysdk.ui.server.basic.event.PBeforeSelectionHandler;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;
import com.ponysdk.ui.terminal.PUnit;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.model.Model;

/**
 * A panel that stacks its children vertically, displaying only one at a time, with a header for each child
 * which the user can click to display.
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that the HTML page in which it is run
 * have an explicit &lt;!DOCTYPE&gt; declaration.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-StackLayoutPanel
 * <dd>the panel itself
 * <dt>.gwt-StackLayoutPanel .gwt-StackLayoutPanelHeader
 * <dd>applied to each header widget
 * <dt>.gwt-StackLayoutPanel .gwt-StackLayoutPanelHeader-hovering
 * <dd>applied to each header widget on mouse hover
 * <dt>.gwt-StackLayoutPanel .gwt-StackLayoutPanelContent
 * <dd>applied to each child widget
 * </dl>
 */
public class PStackLayoutPanel extends PComposite implements HasPWidgets, HasPSelectionHandlers<Integer>, HasPBeforeSelectionHandlers<Integer>, PAnimatedLayout {

    private final PWidgetCollection children = new PWidgetCollection(this);

    private final Collection<PBeforeSelectionHandler<Integer>> beforeSelectionHandlers = new ArrayList<>();

    private final Collection<PSelectionHandler<Integer>> selectionHandlers = new ArrayList<>();

    private int animationDuration;

    public PStackLayoutPanel(final PUnit unit) {
        super(new EntryInstruction(Model.UNIT, unit.ordinal()));
        initWidget(new PLayoutPanel());
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.STACKLAYOUT_PANEL;
    }

    public void add(final PWidget child, final String header, final boolean asHtml, final double headerSize) {
        child.removeFromParent();
        children.add(child);
        adopt(child);

        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_ADD);
        parser.parse(Model.OBJECT_ID, child.getID());
        parser.parse(Model.PARENT_OBJECT_ID, ID);
        parser.parse(Model.HTML, header);
        parser.parse(Model.SIZE, headerSize);
        parser.endObject();

        UIContext.get().assignParentID(child.getID(), ID);
    }

    @Override
    public boolean remove(final PWidget child) {
        if (child.getParent() != this) { return false; }
        orphan(child);
        children.remove(child);
        saveRemove(child.getID(), ID);
        return true;
    }

    @Override
    public Iterator<PWidget> iterator() {
        return children.iterator();
    }

    @Override
    public void add(final PWidget w) {
        assert false : "Single-argument add() is not supported for this widget";
    }

    @Override
    public void add(final IsPWidget w) {
        add(w.asWidget());
    }

    @Override
    public void clear() {
        final Iterator<PWidget> it = iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    private void adopt(final PWidget child) {
        assert(child.getParent() == null);
        child.setParent(this);
    }

    private void orphan(final PWidget child) {
        assert(child.getParent() == this);
        child.setParent(null);
    }

    @Override
    public void addBeforeSelectionHandler(final PBeforeSelectionHandler<Integer> handler) {
        beforeSelectionHandlers.add(handler);
        saveAddHandler(Model.HANDLER_BEFORE_SELECTION_HANDLER);
    }

    @Override
    public void removeBeforeSelectionHandler(final PBeforeSelectionHandler<Integer> handler) {
        beforeSelectionHandlers.remove(handler);
    }

    @Override
    public Collection<PBeforeSelectionHandler<Integer>> getBeforeSelectionHandlers() {
        return Collections.unmodifiableCollection(beforeSelectionHandlers);
    }

    @Override
    public void addSelectionHandler(final PSelectionHandler<Integer> handler) {
        selectionHandlers.add(handler);
        saveAddHandler(Model.HANDLER_SELECTION_HANDLER);
    }

    @Override
    public void removeSelectionHandler(final PSelectionHandler<Integer> handler) {
        selectionHandlers.remove(handler);
    }

    @Override
    public Collection<PSelectionHandler<Integer>> getSelectionHandlers() {
        return Collections.unmodifiableCollection(selectionHandlers);
    }

    public void showWidget(final PWidget widget) {
        final Parser parser = Txn.get().getTxnContext().getParser();
        parser.beginObject();
        parser.parse(Model.TYPE_UPDATE);
        parser.parse(Model.OBJECT_ID, ID);
        parser.parse(Model.OPEN, widget.getID());
        parser.endObject();
    }

    /**
     * Set the duration of the animated transition between children.
     * 
     * @param duration
     *            the duration in milliseconds.
     */
    public void setAnimationDuration(final int duration) {
        this.animationDuration = duration;
        saveUpdate(Model.ANIMATION_DURATION, duration);
    }

    @Override
    public void animate(final int duration) {
        saveUpdate(Model.ANIMATE, duration);
    }

    public int getAnimationDuration() {
        return animationDuration;
    }

}
