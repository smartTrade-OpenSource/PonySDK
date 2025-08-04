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

import java.util.ArrayList;
import java.util.List;

import jakarta.json.JsonObject;

import com.ponysdk.core.model.ClientToServerModel;
import com.ponysdk.core.model.HandlerModel;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.model.WidgetType;
import com.ponysdk.core.ui.basic.event.PScrollEvent;
import com.ponysdk.core.ui.basic.event.PScrollEvent.PScrollHandler;

/**
 * A simple panel that wraps its contents in a scrollable area.
 */
public class PScrollPanel extends PSimplePanel {

    private List<PScrollHandler> scrollHandlers;

    protected PScrollPanel() {
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.SCROLL_PANEL;
    }

    public void setHorizontalScrollPosition(final int position) {
        saveUpdate(ServerToClientModel.HORIZONTAL_SCROLL_POSITION, position);
    }

    public void setVerticalScrollPosition(final int scrollPosition) {
        saveUpdate(ServerToClientModel.VERTICAL_SCROLL_POSITION, scrollPosition);
    }

    public void scrollToBottom() {
        scrollTo(ScrollType.BOTTOM);
    }

    public void scrollToLeft() {
        scrollTo(ScrollType.LEFT);
    }

    public void scrollToRight() {
        scrollTo(ScrollType.RIGHT);
    }

    public void scrollToTop() {
        scrollTo(ScrollType.TOP);
    }

    private void scrollTo(final ScrollType type) {
        saveUpdate(writer -> writer.write(ServerToClientModel.SCROLL_TO, type.ordinal()));
    }

    public void addScrollHandler(final PScrollHandler handler) {
        if (scrollHandlers == null) {
            scrollHandlers = new ArrayList<>();
            saveAddHandler(HandlerModel.HANDLER_SCROLL);
        }
        scrollHandlers.add(handler);
    }

    public void removeScrollHandler(final PScrollHandler handler) {
        if (scrollHandlers != null) {
            scrollHandlers.remove(handler);
            if (scrollHandlers.isEmpty()) saveRemoveHandler(HandlerModel.HANDLER_SCROLL);
        }
    }

    @Override
    public void onClientData(final JsonObject instruction) {
        if (!isVisible()) return;
        if (instruction.containsKey(ClientToServerModel.HANDLER_SCROLL.toStringValue())) {
            if (scrollHandlers != null) {
                final int height = instruction.getJsonNumber(ClientToServerModel.HANDLER_SCROLL_HEIGHT.toStringValue()).intValue();
                final int width = instruction.getJsonNumber(ClientToServerModel.HANDLER_SCROLL_WIDTH.toStringValue()).intValue();
                final int vertical = instruction.getJsonNumber(ClientToServerModel.HANDLER_SCROLL_VERTICAL.toStringValue()).intValue();
                final int horizontal = instruction.getJsonNumber(ClientToServerModel.HANDLER_SCROLL_HORIZONTAL.toStringValue())
                    .intValue();
                final PScrollEvent scrollEvent = new PScrollEvent(this, height, width, vertical, horizontal);
                scrollHandlers.forEach(handler -> handler.onScroll(scrollEvent));
            }
        } else {
            super.onClientData(instruction);
        }
    }

    private enum ScrollType {
        BOTTOM,
        LEFT,
        RIGHT,
        TOP
    }

}
