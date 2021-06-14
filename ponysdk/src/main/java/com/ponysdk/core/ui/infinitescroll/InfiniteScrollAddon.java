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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.json.JsonObject;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PPanel;

/**
 * @author mzoughagh
 * @param <D> type of data
 * @param <W> type of widget
 */

public class InfiniteScrollAddon<D, W extends IsPWidget> extends PAddOnComposite<PPanel> {

    //UI
    private final PFlowPanel container;

    // widgets
    private final List<W> rows = new ArrayList<>();

    // used for drawing
    private int beginIndex = 0;
    private int maxVisibleItem = 5;
    private final int initmaxVisibleItem = 5;

    private final InfiniteScrollProvider<D, W> dataProvider;
    private int fullSize;

    /**
     * Creates an infiniteScrollAddon with the specified dataProvier
     *
     * @param dataProvider infiniteScrollAddon's dataProvier
     */

    public InfiniteScrollAddon(final InfiniteScrollProvider<D, W> dataProvider) {
        super(Element.newPFlowPanel());
        this.dataProvider = dataProvider;
        dataProvider.addHandler(e -> {
            this.maxVisibleItem = this.maxVisibleItem - 1;
            this.refresh();
        });

        widget.addStyleName("is-viewport");

        container = Element.newPFlowPanel();
        container.addStyleName("is-container");
        widget.add(container);

        // HANDLER
        this.setTerminalHandler((event) -> {
            final JsonObject jsonObj = event.getData();
            beginIndex = jsonObj.getInt("beginIndex");
            maxVisibleItem = jsonObj.getInt("maxVisibleItem");
            draw();
        });

        dataProvider.getFullSize(this::setFullSize);
    }

    private void setFullSize(final int fullSize) {
        this.fullSize = fullSize;
        draw();
        callTerminalMethod("setSize", fullSize);
    }

    /**
     * Redrawing the items after modifying parameters
     * like beginIndex and maxvisibleItem or length dataProvider's list
     */

    public void refresh() {
        dataProvider.getFullSize(this::setFullSize);
    }

    /**
     * Calling setScrollTop method js
     */

    public void setScrollTop() {
        callTerminalMethod("setScrollTop");
    }

    /**
     * @param index to set beginIndex
     */
    public void setbeginIndex(final int index) {
        this.beginIndex = index;
    }

    /**
     * Setting maxItemVisibleItem with the initial value
     */
    public void setmaxItemVisible() {
        this.maxVisibleItem = initmaxVisibleItem;
    }

    /**
     * Adding widgets to our DOM by taking into consideration
     * the number of widget (maxVisibleItem) and the beginning
     * index (beginIndex).
     * After adding, it updates existing widgets and removes
     * unused widgets.
     */
    public void draw() {
        final int size = Math.min(maxVisibleItem, fullSize - beginIndex);
        if (size > 0) {
            widget.addStyleName("is-loading");
            dataProvider.getData(beginIndex, size, this::draw);

        } else {
            this.draw(Collections.emptyList());
        }

    }

    private void addWidgetToContainer(final int index, final W widget) {
        container.insert(widget.asWidget(), index);
        widget.asWidget().addStyleName("item");
    }

    private void removeWidgetFromContainer(final W widget) {
        widget.asWidget().removeFromParent();
        widget.asWidget().onDestroy();
    }

    /**
     * Setting Infinite Scroll visibility
     *
     * @param visible boolean
     */
    public void setVisible(final boolean visible) {
        if (visible) {

            widget.setVisible(true);

        } else {
            widget.setVisible(false);
        }

    }

    /**
     * Get Infinite Scroll visibility
     *
     * @return boolean
     */
    public boolean isVisible() {
        return widget.isVisible();
    }

    public void setStyleProperty(final String name, final String value) {
        widget.setStyleProperty(name, value);
    }

    private void draw(final List<D> data) {
        final int size = Math.min(maxVisibleItem, fullSize - beginIndex);

        if (data.size() != size) {
            throw new IllegalStateException("data size doesnt match expected :" + size + " actual : " + data.size());
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
        widget.removeStyleName("is-loading");
        callTerminalMethod("onDraw");

    }

}
