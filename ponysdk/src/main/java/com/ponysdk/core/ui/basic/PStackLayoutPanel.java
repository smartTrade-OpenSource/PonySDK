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

import java.time.Duration;
import java.util.*;

import com.ponysdk.core.server.application.Parser;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.PUnit;
import com.ponysdk.core.ui.basic.event.*;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.model.WidgetType;

/**
 * A panel that stacks its children vertically, displaying only one at a time, with a header for
 * each child which the user can click to display.
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that the HTML page in which
 * it is run have an explicit &lt;!DOCTYPE&gt; declaration.
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
public class PStackLayoutPanel extends PComposite
        implements HasPWidgets, HasPSelectionHandlers<Integer>, HasPBeforeSelectionHandlers<Integer>, PAnimatedLayout {

    private final PWidgetCollection children = new PWidgetCollection(this);

    private final Collection<PBeforeSelectionHandler<Integer>> beforeSelectionHandlers = new ArrayList<>();

    private final Collection<PSelectionHandler<Integer>> selectionHandlers = new ArrayList<>();

    private Duration animationDuration;

    private final PUnit unit;

    public PStackLayoutPanel(final PUnit unit) {
        super();
        this.unit = unit;
    }

    @Override
    protected void enrichOnInit(final Parser parser) {
        super.enrichOnInit(parser);
        parser.parse(ServerToClientModel.UNIT, unit.getByteValue());
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.STACKLAYOUT_PANEL;
    }

    public void add(final PWidget child, final String header, final boolean asHtml, final double headerSize) {
        child.removeFromParent();
        children.add(child);
        adopt(child);

        child.saveAdd(child.getID(), ID, new ServerBinaryModel(ServerToClientModel.HTML, header),
                new ServerBinaryModel(ServerToClientModel.SIZE, headerSize));
        child.attach(windowID);
    }

    @Override
    public boolean remove(final PWidget child) {
        if (child.getParent() != this) {
            return false;
        }
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
        assert child.getParent() == null;
        child.setParent(this);
    }

    private void orphan(final PWidget child) {
        assert child.getParent() == this;
        child.setParent(null);
    }

    @Override
    public void addBeforeSelectionHandler(final PBeforeSelectionHandler<Integer> handler) {
        beforeSelectionHandlers.add(handler);
        saveAddHandler(HandlerModel.HANDLER_BEFORE_SELECTION);
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
        saveAddHandler(HandlerModel.HANDLER_SELECTION);
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
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.WIDGET_ID, widget.getID()));
    }

    /**
     * Set the duration of the animated transition between children.
     */
    public void setAnimationDuration(final Duration duration) {
        if(Objects.equals(animationDuration,duration)) return;
        animationDuration = duration;
        saveUpdate((writer) -> writer.writeModel(ServerToClientModel.ANIMATION_DURATION, duration.toMillis()));
    }

    @Override
    public void animate(final Duration duration) {
        saveUpdate(writer -> writer.writeModel(ServerToClientModel.ANIMATE, duration.toMillis()));
    }

    public Duration getAnimationDuration() {
        return animationDuration;
    }

}
