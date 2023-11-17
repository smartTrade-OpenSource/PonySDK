/*
 * Copyright (c) 2021 PonySDK
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

package com.ponysdk.core.ui.infinitescroll;

import com.ponysdk.core.ui.basic.*;
import jakarta.json.JsonObject;

import java.util.*;

/**
 * @param <D> Data
 * @param <W> Widget
 * @author mzoughagh
 */
public class InfiniteScrollAddon<D, W extends IsPWidget> extends PAddOnComposite<PPanel> {

    private static final String FUNCTION_ON_DRAW = "onDraw";
    private static final String FUNCTION_SET_SIZE = "setSize";
    private static final String FUNCTION_SET_SCROLL_TOP = "setScrollTop";
    private static final String FUNCTION_SHOW_INDEX = "showIndex";

    private static final String KEY_BEGIN_INDEX = "beginIndex";
    private static final String KEY_MAX_VISIBLE_ITEM = "maxVisibleItem";

    private static final String STYLE_IS_ITEM = "is-item";
    private static final String STYLE_IS_LOADING = "is-loading";
    private static final String STYLE_IS_CONTAINER = "is-container";
    private static final String STYLE_IS_VIEWPORT = "is-viewport";
    private static final String STYLE_IS_ITEM_INDEX = "is-item-index";

    //UI
    private final PFlowPanel container;
    private final List<W> rows = new ArrayList<>();
    private final Set<InfiniteScrollAddonListener> listeners = new HashSet<>();
    private final InfiniteScrollProvider<D, W> dataProvider;
    private W currentIndexWidget;
    private int startIndex = 0;
    private int currentIndex = startIndex;
    // used for drawing
    private int beginIndex = 0;
    private int maxVisibleItems = 10;
    private int fullSize;
    private boolean forceDrawNextTime;

    /**
     * Creates an infiniteScrollAddon with the specified dataProvier
     *
     * @param dataProvider infiniteScrollAddon's dataProvier
     */
    public InfiniteScrollAddon(final InfiniteScrollProvider<D, W> dataProvider) {
        super(Element.newPFlowPanel());
        this.dataProvider = dataProvider;
        dataProvider.addHandler(() -> {
            forceDrawNextTime = true;
            this.refresh();
        });

        widget.addStyleName(STYLE_IS_VIEWPORT);

        container = Element.newPFlowPanel();
        container.addStyleName(STYLE_IS_CONTAINER);
        widget.add(container);

        // HANDLER
        this.setTerminalHandler((event) -> {
            final JsonObject jsonObj = event.getData();
            this.beginIndex = jsonObj.getInt(KEY_BEGIN_INDEX);
            this.maxVisibleItems = jsonObj.getInt(KEY_MAX_VISIBLE_ITEM);
            draw();
            listeners.forEach(l -> l.onBeginIndexChanged(this.beginIndex));
        });
    }

    /**
     * Redrawing the items after modifying parameters
     * like beginIndex and maxvisibleItem or length dataProvider's list
     */
    public void refresh() {
        dataProvider.getFullSize(this::setFullSize);
    }

    public void setScrollTop() {
        callTerminalMethod(FUNCTION_SET_SCROLL_TOP);
        currentIndex = startIndex;
        updateCurrentIndexWidget();
    }

    public void setMaxItemVisible(final int maxVisibleItems) {
        this.maxVisibleItems = maxVisibleItems;
    }

    public boolean isVisible() {
        return widget.isVisible();
    }

    public void setVisible(final boolean visible) {
        if (visible) {
            widget.setVisible(true);
        } else {
            widget.setVisible(false);
        }
    }

    public void setStyleProperty(final String name, final String value) {
        widget.setStyleProperty(name, value);
    }

    public int getBeginIndex() {
        return beginIndex;
    }

    public int getFullSize() {
        return fullSize;
    }

    private void setFullSize(final int fullSize) {
        if (this.fullSize != fullSize) {
            final boolean forceDraw = forceDrawNextTime || this.fullSize == 0 || this.fullSize < maxVisibleItems;
            forceDrawNextTime = false;
            this.fullSize = fullSize;
            if (forceDraw) {
                this.beginIndex = 0;
                draw();
            }
            callTerminalMethod(FUNCTION_SET_SIZE, fullSize);
        } else {
            draw();
        }
    }

    public void setStartIndex(final int startIndex) {
        this.startIndex = startIndex;
    }

    public void showIndex(final int index) {
        if (index >= 0 && index < fullSize) {
            callTerminalMethod(FUNCTION_SHOW_INDEX, index);
            currentIndex = index;
            updateCurrentIndexWidget();
        }
    }

    public W getCurrentItemIndex() {
        for (final W w : rows) {
            if (w.asWidget().hasStyleName(STYLE_IS_ITEM_INDEX)) {
                return w;
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        listeners.clear();
        super.onDestroy();
    }

    public void addListener(final InfiniteScrollAddonListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(final InfiniteScrollAddonListener listener) {
        this.listeners.remove(listener);
    }

    private void addWidgetToContainer(final int index, final W widget) {
        container.insert(widget.asWidget(), index);
        widget.asWidget().addStyleName(STYLE_IS_ITEM);
    }

    private void removeWidgetFromContainer(final W widget) {
        container.remove(widget.asWidget());
        widget.asWidget().onDestroy();
    }

    /**
     * Adding widgets to our DOM by taking into consideration
     * the number of widget (maxVisibleItem) and the beginning
     * index (beginIndex).
     * After adding, it updates existing widgets and removes
     * unused widgets.
     */
    private void draw() {
        final int size = Math.min(maxVisibleItems, fullSize - beginIndex);
        if (size > 0) {
            widget.addStyleName(STYLE_IS_LOADING);
            dataProvider.getData(beginIndex, size, this::draw);
        } else {
            this.draw(Collections.emptyList());
        }
    }

    private void draw(final List<D> data) {
        try {
            final int size = Math.min(maxVisibleItems, fullSize - beginIndex);

            if (data.size() != size) {
                throw new IllegalStateException("Data size doesn't match expected :" + size + ". Actual : " + data.size());
            }

            int fullDataIndex = beginIndex;
            int index = 0;
            //update existing widget
            while (index < Math.min(data.size(), rows.size())) {
                final W currentWidget = rows.get(index);
                final W newWidget = dataProvider.handleUI(fullDataIndex, data.get(index), currentWidget);
                if (currentWidget != newWidget) {
                    removeWidgetFromContainer(currentWidget);
                    rows.set(index, newWidget);
                    addWidgetToContainer(index, newWidget);
                }
                index++;
                fullDataIndex++;
            }

            if (currentIndexWidget == null) {
                currentIndex = startIndex;
            }
            updateCurrentIndexWidget();

            // create missing widget
            while (rows.size() < data.size()) {
                final W newWidget = dataProvider.handleUI(fullDataIndex, data.get(index), null);
                rows.add(newWidget);
                addWidgetToContainer(index, newWidget);
                index++;
                fullDataIndex++;
            }

            // delete unused widget
            while (data.size() < rows.size()) {
                removeWidgetFromContainer(rows.remove(index));
            }
            callTerminalMethod(FUNCTION_ON_DRAW);
        } finally {
            widget.removeStyleName(STYLE_IS_LOADING);
        }
    }

    private void updateCurrentIndexWidget() {
        if (currentIndexWidget != null) {
            currentIndexWidget.asWidget().removeStyleName(STYLE_IS_ITEM_INDEX);
            currentIndexWidget = null;
        }
        if (rows.isEmpty()) return;
        int index = Math.max(startIndex, currentIndex - this.beginIndex);
        index = Math.min(index, rows.size() - 1);
        final W w = rows.get(index);
        if (w != null) {
            currentIndexWidget = w;
            currentIndexWidget.asWidget().addStyleName(STYLE_IS_ITEM_INDEX);
        }
    }

    public interface InfiniteScrollAddonListener {

        void onBeginIndexChanged(int beginIndex);

    }
}
