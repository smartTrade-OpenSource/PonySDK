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

import org.json.JSONException;
import org.json.JSONObject;

import com.ponysdk.core.instruction.Add;
import com.ponysdk.core.instruction.AddHandler;
import com.ponysdk.core.instruction.Update;
import com.ponysdk.ui.server.basic.event.HasPAnimation;
import com.ponysdk.ui.server.basic.event.HasPBeforeSelectionHandlers;
import com.ponysdk.ui.server.basic.event.HasPSelectionHandlers;
import com.ponysdk.ui.server.basic.event.PBeforeSelectionEvent;
import com.ponysdk.ui.server.basic.event.PBeforeSelectionHandler;
import com.ponysdk.ui.server.basic.event.PSelectionEvent;
import com.ponysdk.ui.server.basic.event.PSelectionHandler;
import com.ponysdk.ui.terminal.WidgetType;
import com.ponysdk.ui.terminal.instruction.Dictionnary.HANDLER;
import com.ponysdk.ui.terminal.instruction.Dictionnary.PROPERTY;

/**
 * A panel that represents a tabbed set of pages, each of which contains another widget. Its child widgets are
 * shown as the user selects the various tabs associated with them. The tabs can contain arbitrary text, HTML,
 * or widgets.
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that the HTML page in which it is run
 * have an explicit &lt;!DOCTYPE&gt; declaration.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-TabLayoutPanel
 * <dd>the panel itself
 * <dt>.gwt-TabLayoutPanel .gwt-TabLayoutPanelTabs
 * <dd>the tab bar element
 * <dt>.gwt-TabLayoutPanel .gwt-TabLayoutPanelTab
 * <dd>an individual tab
 * <dt>.gwt-TabLayoutPanel .gwt-TabLayoutPanelTabInner
 * <dd>an element nested in each tab (useful for styling)
 * <dt>.gwt-TabLayoutPanel .gwt-TabLayoutPanelContent
 * <dd>applied to all child content widgets
 * </dl>
 * A TabLayoutPanel must have a <code>barHeight</code> attribute with a double value, and may have a
 * <code>barUnit</code> attribute with a {@link com.google.gwt.dom.client.Style.Unit Style.Unit} value.
 * <code>barUnit</code> defaults to PX.
 * <p>
 * The children of a TabLayoutPanel element are laid out in &lt;g:tab> elements. Each tab can have one widget
 * child and one of two types of header elements. A &lt;g:header> element can hold html, or a
 * &lt;g:customHeader> element can hold a widget.
 */
public class PTabLayoutPanel extends PComplexPanel implements HasPAnimation, HasPBeforeSelectionHandlers<Integer>, HasPSelectionHandlers<Integer>, PSelectionHandler<Integer> {

    private boolean animationEnabled = false;

    private final Collection<PBeforeSelectionHandler<Integer>> beforeSelectionHandlers = new ArrayList<PBeforeSelectionHandler<Integer>>();

    private final Collection<PSelectionHandler<Integer>> selectionHandlers = new ArrayList<PSelectionHandler<Integer>>();

    private Integer selectedItemIndex;

    public PTabLayoutPanel() {
        final AddHandler addHandler = new AddHandler(getID(), HANDLER.SELECTION_HANDLER);
        getPonySession().stackInstruction(addHandler);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.TAB_LAYOUT_PANEL;
    }

    public void insert(final IsPWidget widget, final String tabText, final int beforeIndex) {
        insert(asWidgetOrNull(widget), tabText, beforeIndex);
    }

    public void insert(final IsPWidget widget, final IsPWidget tabWidget, final int beforeIndex) {
        insert(asWidgetOrNull(widget), asWidgetOrNull(tabWidget), beforeIndex);
    }

    public void insert(final PWidget widget, final PWidget tabWidget, final int beforeIndex) {
        // Detach new child.
        widget.removeFromParent();

        getChildren().insert(widget, beforeIndex);

        // Adopt.
        adopt(widget);

        final Add addWidget = new Add(widget.getID(), getID());
        addWidget.put(PROPERTY.BEFORE_INDEX, beforeIndex);
        addWidget.put(PROPERTY.TAB_WIDGET, tabWidget.getID());
        getPonySession().stackInstruction(addWidget);
    }

    public void insert(final PWidget widget, final String tabText, final int beforeIndex) {
        // Detach new child.
        widget.removeFromParent();

        getChildren().insert(widget, beforeIndex);

        // Adopt.
        adopt(widget);

        final Add addWidget = new Add(widget.getID(), getID());
        addWidget.put(PROPERTY.BEFORE_INDEX, beforeIndex);
        addWidget.put(PROPERTY.TAB_TEXT, tabText);
        getPonySession().stackInstruction(addWidget);
    }

    public void add(final IsPWidget w, final IsPWidget tabWidget) {
        add(asWidgetOrNull(w), asWidgetOrNull(tabWidget));
    }

    public void add(final IsPWidget w, final String tabText) {
        add(asWidgetOrNull(w), tabText);
    }

    public void add(final PWidget w, final String tabText) {
        insert(w, tabText, getWidgetCount());
    }

    public void add(final PWidget w, final PWidget tabWidget) {
        insert(w, tabWidget, getWidgetCount());
    }

    public void selectTab(final int index) {
        this.selectedItemIndex = index;
        final Update update = new Update(ID);
        update.put(PROPERTY.SELECTED_INDEX, index);
        getPonySession().stackInstruction(update);
    }

    @Override
    public void add(final PWidget w) {
        throw new UnsupportedOperationException("A tabText parameter must be specified with add().");
    }

    @Override
    public boolean isAnimationEnabled() {
        return animationEnabled;
    }

    @Override
    public void setAnimationEnabled(final boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
        final Update update = new Update(ID);
        update.put(PROPERTY.ANIMATION, animationEnabled);
        getPonySession().stackInstruction(update);
    }

    @Override
    public void addBeforeSelectionHandler(final PBeforeSelectionHandler<Integer> handler) {
        beforeSelectionHandlers.add(handler);
        final AddHandler addHandler = new AddHandler(getID(), HANDLER.BEFORE_SELECTION_HANDLER);
        getPonySession().stackInstruction(addHandler);
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
    }

    @Override
    public void removeSelectionHandler(final PSelectionHandler<Integer> handler) {
        selectionHandlers.remove(handler);
    }

    @Override
    public Collection<PSelectionHandler<Integer>> getSelectionHandlers() {
        return Collections.unmodifiableCollection(selectionHandlers);
    }

    @Override
    public void onEventInstruction(final JSONObject eventInstruction) throws JSONException {
        String handlerKey = null;
        if (eventInstruction.has(HANDLER.KEY)) {
            handlerKey = eventInstruction.getString(HANDLER.KEY);
        }

        if (HANDLER.SELECTION_HANDLER.equals(handlerKey)) {
            for (final PSelectionHandler<Integer> handler : getSelectionHandlers()) {
                handler.onSelection(new PSelectionEvent<Integer>(this, eventInstruction.getInt(PROPERTY.INDEX)));
            }
        } else if (HANDLER.BEFORE_SELECTION_HANDLER.equals(handlerKey)) {
            for (final PBeforeSelectionHandler<Integer> handler : getBeforeSelectionHandlers()) {
                handler.onBeforeSelection(new PBeforeSelectionEvent<Integer>(this, eventInstruction.getInt(PROPERTY.INDEX)));
            }
        } else {
            super.onEventInstruction(eventInstruction);
        }
    }

    @Override
    public void onSelection(final PSelectionEvent<Integer> event) {
        selectedItemIndex = event.getSelectedItem();
    }

    public Integer getSelectedItemIndex() {
        return selectedItemIndex;
    }

}
