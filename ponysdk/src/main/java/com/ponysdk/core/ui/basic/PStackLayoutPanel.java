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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.PUnit;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.HasPWidgets;
import com.ponysdk.core.ui.basic.event.PBeforeSelectionEvent;
import com.ponysdk.core.ui.basic.event.PSelectionHandler;
import com.ponysdk.core.ui.model.ServerBinaryModel;
import com.ponysdk.core.writer.ModelWriter;

/**
 * A panel that stacks its children vertically, displaying only one at a time,
 * with a header for each child which the user can click to display.
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that
 * the HTML page in which it is run have an explicit &lt;!DOCTYPE&gt;
 * declaration.
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
public class PStackLayoutPanel extends PWidget implements HasPWidgets, PAnimatedLayout {

    private static final Logger log = LoggerFactory.getLogger(PStackLayoutPanel.class);

    private final PWidgetCollection children = new PWidgetCollection(this);

    private final List<PBeforeSelectionEvent.Handler<Integer>> beforeSelectionHandlers = new ArrayList<>();
    private final List<PSelectionHandler<Integer>> selectionHandlers = new ArrayList<>();

    private final PUnit unit;

    private Duration animationDuration;

    protected PStackLayoutPanel(final PUnit unit) {
        super();
        this.unit = unit;
    }

    @Override
    protected void enrichForCreation(final ModelWriter writer) {
        super.enrichForCreation(writer);
        writer.write(ServerToClientModel.UNIT, unit.getByteValue());
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.STACKLAYOUT_PANEL;
    }

    public void add(final PWidget child, final String header, final boolean asHtml, final double headerSize) {
        child.removeFromParent();
        children.add(child);
        adopt(child);

        child.attach(window, frame);
        child.saveAdd(child.getID(), ID, new ServerBinaryModel(ServerToClientModel.HTML, header),
            new ServerBinaryModel(ServerToClientModel.SIZE, headerSize));
    }

    @Override
    public boolean remove(final PWidget child) {
        if (child.getParent() != this) {
            return false;
        }
        orphan(child);
        children.remove(child);
        child.saveRemove(child.getID(), ID);
        return true;
    }

    @Override
    public Iterator<PWidget> iterator() {
        return children.iterator();
    }

    @Override
    public void add(final PWidget w) {
        log.error("Use #add(final PWidget child, final String header, final boolean asHtml, final double headerSize) instead");
        throw new UnsupportedOperationException(
            "Use #add(final PWidget child, final String header, final boolean asHtml, final double headerSize) instead");
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
        if (child.getParent() == null) child.setParent(this);
        else throw new IllegalStateException("Can't adopt an already widget attached to a parent");
    }

    private void orphan(final PWidget child) {
        if (child.getParent() == this) child.setParent(null);
        else throw new IllegalStateException("Can't adopt an widget attached to another parent");
    }

    public void addBeforeSelectionHandler(final PBeforeSelectionEvent.Handler<Integer> handler) {
        beforeSelectionHandlers.add(handler);
        saveAddHandler(HandlerModel.HANDLER_BEFORE_SELECTION);
    }

    public void removeBeforeSelectionHandler(final PBeforeSelectionEvent.Handler<Integer> handler) {
        beforeSelectionHandlers.remove(handler);
    }

    public void addSelectionHandler(final PSelectionHandler<Integer> handler) {
        selectionHandlers.add(handler);
        saveAddHandler(HandlerModel.HANDLER_SELECTION);
    }

    public void removeSelectionHandler(final PSelectionHandler<Integer> handler) {
        selectionHandlers.remove(handler);
    }

    public void showWidget(final PWidget widget) {
        saveUpdate(writer -> writer.write(ServerToClientModel.WIDGET_ID, widget.getID()));
    }

    @Override
    public void animate(final Duration duration) {
        saveUpdate(writer -> writer.write(ServerToClientModel.ANIMATE, duration.toMillis()));
    }

    public Duration getAnimationDuration() {
        return animationDuration;
    }

    /**
     * Set the duration of the animated transition between children.
     */
    public void setAnimationDuration(final Duration duration) {
        if (Objects.equals(animationDuration, duration)) return;
        animationDuration = duration;
        saveUpdate(ServerToClientModel.ANIMATION_DURATION, (int) duration.toMillis());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        forEach(PObject::onDestroy);
    }

    @Override
    protected String dumpDOM() {
        String DOM = "<div>";

        Iterator<PWidget> iter = children.iterator();
        while (iter.hasNext()) {
            DOM += iter.next().dumpDOM();
        }
        DOM += "</div>";
        return DOM;
    }

}
