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
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PPanel;
import com.ponysdk.core.ui.infinitescroll.InfiniteScrollProvider.Wrapper;

public class InfiniteScrollAddon<D> extends PAddOnComposite<PPanel> {

    //UI
    private final PFlowPanel container;

    // widgets
    private final List<Wrapper> rows = new ArrayList<>();

    // used for drawing
    private int beginIndex = 0;
    private int maxVisibleItem = 20;
    private final int initmaxVisibleItem = 20;

    private final InfiniteScrollProvider<D> dataProvider;
    private int fullSize;

    public InfiniteScrollAddon(final InfiniteScrollProvider<D> dataProvider) {
        super(Element.newPFlowPanel());
        this.dataProvider = dataProvider;
        dataProvider.addHandler(e -> {
            //this.setFullSize(dataProvider.getFullSize());
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

        setFullSize(dataProvider.getFullSize());
    }

    private void setFullSize(final int fullSize) {
        this.fullSize = fullSize;
        draw();
        callTerminalMethod("setSize", fullSize);
    }

    public void refresh() {
        setFullSize(dataProvider.getFullSize());
    }

    public void setScrollTop() {
        callTerminalMethod("setScrollTop");
    }

    public void setbeginIndex(final int index) {
        this.beginIndex = index;
    }

    public void setmaxItemVisible() {
        this.maxVisibleItem = initmaxVisibleItem;
    }

    public void draw() {
        final int size = Math.min(maxVisibleItem, fullSize);
        final List<D> data;
        if (size > 0) {
            data = dataProvider.getData(beginIndex, size);
            if (data.size() != size) {
                throw new IllegalStateException();
            }
        } else {
            data = Collections.emptyList();
        }

        System.err.println(String.format("draw : beginIndex : %s, maxVisibleItem %s, dataSize %s, last index %s", beginIndex,
            maxVisibleItem, data.size(), beginIndex + data.size()));

        int fullDataIndex = beginIndex;
        int index = 0;
        //update existing widget
        while (index < Math.min(data.size(), rows.size())) {
            final Wrapper currentWidget = rows.get(index);
            final Wrapper newWidget = dataProvider.handleUI(fullDataIndex, data.get(index), currentWidget);
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
            final Wrapper newWidget = dataProvider.handleUI(fullDataIndex, data.get(index), null);
            rows.add(newWidget);
            addWidgetToContainer(index, newWidget);
            index++;
            fullDataIndex++;
        }

        // delete unused widget
        while (data.size() < rows.size()) {
            removeWidgetFromContainer(rows.remove(index));
        }

        callTerminalMethod("onDraw");
    }

    private void addWidgetToContainer(final int index, final Wrapper widget) {
        container.insert(widget.asWidget(), index);
        widget.asWidget().addStyleName("item");
    }

    private void removeWidgetFromContainer(final Wrapper widget) {
        widget.asWidget().removeFromParent();
        widget.asWidget().onDestroy();
    }

}
