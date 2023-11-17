/*
 * Copyright (c) 2011 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
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

import com.ponysdk.core.model.*;
import com.ponysdk.core.ui.basic.event.PLayoutResizeEvent;
import com.ponysdk.core.util.SetUtils;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A panel that adds user-positioned splitters between each of its child widgets.
 * <p>
 * This panel is used in the same way as {@link PDockLayoutPanel}, except that its children's sizes are always specified
 * in {@link PUnit#PX} units, and each pair of child widgets has a splitter between them that the user can drag.
 * </p>
 * <p>
 * This widget will <em>only</em> work in standards mode, which requires that the HTML page in which it is run have an
 * explicit &lt;!DOCTYPE&gt; declaration.
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-SplitLayoutPanel { the panel itself }</li>
 * <li>.gwt-SplitLayoutPanel .gwt-SplitLayoutPanel-HDragger { horizontal dragger
 * }</li>
 * <li>.gwt-SplitLayoutPanel .gwt-SplitLayoutPanel-VDragger { vertical dragger }
 * </li>
 * </ul>
 */
public class PSplitLayoutPanel extends PDockLayoutPanel {

    private final Set<PLayoutResizeEvent.Handler> handlers = SetUtils.newArraySet(4);
    private final Map<PWidget, SplitInfoHolder> splitInfoByWidget = new HashMap<>(4);

    protected PSplitLayoutPanel() {
        super(PUnit.PX);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SPLIT_LAYOUT_PANEL;
    }

    @Override
    public boolean remove(final PWidget w) {
        splitInfoByWidget.remove(w);
        return super.remove(w);
    }

    /**
     * Sets the minimum allowable size for the given widget.
     * <p>
     * Its associated splitter cannot be dragged to a position that would make it smaller than this size. This method
     * has no effect for the {@link com.ponysdk.core.model.PDirection#CENTER} widget.
     * </p>
     *
     * @param child   the child whose minimum size will be set
     * @param minSize the minimum size for this widget
     */
    public void setWidgetMinSize(final PWidget child, final int minSize) {
        assertIsChild(child);
        if (getMinSize(child) != minSize) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.MIN_SIZE, minSize);
                writer.write(ServerToClientModel.WIDGET_ID, child.getID());
            });
            ensureWidgetInfo(child).minSize = minSize;
        }
    }

    /**
     * Sets a size below which the slider will close completely. This can be used in conjunction with
     * {@link #setWidgetMinSize} to provide a speed-bump effect where the slider will stick to a preferred minimum size
     * before closing completely.
     * <p>
     * This method has no effect for the {@link com.ponysdk.core.model.PDirection#CENTER} widget.
     * </p>
     *
     * @param child          the child whose slider should snap closed
     * @param snapClosedSize the width below which the widget will close or -1 to disable.
     */
    public void setWidgetSnapClosedSize(final PWidget child, final int snapClosedSize) {
        assertIsChild(child);
        if (getSnapClosedSize(child) != snapClosedSize) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.SNAP_CLOSED_SIZE, snapClosedSize);
                writer.write(ServerToClientModel.WIDGET_ID, child.getID());
            });
            ensureWidgetInfo(child).snapClosedSize = snapClosedSize;
        }
    }

    /**
     * Sets whether or not double-clicking on the splitter should toggle the
     * display of the widget.
     *
     * @param child   the child whose display toggling will be allowed or not.
     * @param allowed whether or not display toggling is allowed for this widget
     */
    public void setWidgetToggleDisplayAllowed(final PWidget child, final boolean allowed) {
        assertIsChild(child);
        if (isToggleDisplayAllowed(child) != allowed) {
            saveUpdate(writer -> {
                writer.write(ServerToClientModel.TOGGLE_DISPLAY_ALLOWED, allowed);
                writer.write(ServerToClientModel.WIDGET_ID, child.getID());
            });
            ensureWidgetInfo(child).toggleDisplayAllowed = allowed;
        }
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (!isVisible()) return;
        if (instruction.containsKey(ClientToServerModel.HANDLER_RESIZE.toStringValue())) {
            final PLayoutResizeEvent resizeEvent = new PLayoutResizeEvent(this);
            final JsonArray array = instruction.getJsonArray(ClientToServerModel.HANDLER_RESIZE.toStringValue());
            for (int i = 0; i < array.size(); i++) {
                final JsonObject ws = array.getJsonObject(i);
                final int objectID = ws.getJsonNumber(ClientToServerModel.OBJECT_ID.toStringValue()).intValue();
                final PWidget w = getChild(objectID);
                if (w != null) {
                    final double widgetSize = ws.getJsonNumber(ClientToServerModel.SIZE.toStringValue()).doubleValue();
                    resizeEvent.addLayoutResizeData(new PLayoutResizeEvent.LayoutResizeData(w, widgetSize));
                }
            }
            fireLayoutResize(resizeEvent);
        } else {
            super.onClientData(instruction);
        }
    }

    private void fireLayoutResize(final PLayoutResizeEvent event) {
        for (final PLayoutResizeEvent.Handler h : handlers) {
            h.onLayoutResize(event);
        }
    }

    public void addLayoutResizeHandler(final PLayoutResizeEvent.Handler resizeHandler) {
        if (handlers.isEmpty()) saveAddHandler(HandlerModel.HANDLER_RESIZE);
        handlers.add(resizeHandler);
    }

    public void removeLayoutResizeHandler(final PLayoutResizeEvent.Handler resizeHandler) {
        handlers.remove(resizeHandler);
        if (handlers.isEmpty()) saveRemoveHandler(HandlerModel.HANDLER_RESIZE);
    }

    public Collection<PLayoutResizeEvent.Handler> getResizeHandlers() {
        return handlers;
    }

    private SplitInfoHolder getWidgetInfo(final PWidget w) {
        return splitInfoByWidget.get(w);
    }

    private SplitInfoHolder ensureWidgetInfo(final PWidget w) {
        SplitInfoHolder splitHolder = splitInfoByWidget.get(w);
        if (splitHolder != null) return splitHolder;

        splitHolder = new SplitInfoHolder();
        splitInfoByWidget.put(w, splitHolder);
        return splitHolder;
    }

    public int getMinSize(final PWidget w) {
        final SplitInfoHolder info = getWidgetInfo(w);
        if (info == null) return -1;
        return info.minSize;
    }

    public int getSnapClosedSize(final PWidget w) {
        final SplitInfoHolder info = getWidgetInfo(w);
        if (info == null) return -1;
        return info.snapClosedSize;
    }

    public boolean isToggleDisplayAllowed(final PWidget w) {
        final SplitInfoHolder info = getWidgetInfo(w);
        return info != null && info.toggleDisplayAllowed;
    }

    private static class SplitInfoHolder {

        private int minSize = -1;
        private int snapClosedSize = -1;
        private boolean toggleDisplayAllowed = false;
    }
}
