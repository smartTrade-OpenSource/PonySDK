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

/**
 * @author mzoughagh
 *  InfiniteScroll Addon
 *  */

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PPanel;
import com.ponysdk.core.ui.basic.PWidget;

public class InfiniteScrollAddon<D> extends PAddOnComposite<PPanel> {

    // visual elements
    private final PPanel container;
    // items
    private final List<PWidget> items = new ArrayList<>();

    // used for drawing
    private int i = 0;
    private int beginIndex = 0;
    private int maxVisibleItem = 20;

    private final InfiniteScrollProvider<D> dataProvider;

    private long size;

    public InfiniteScrollAddon(final InfiniteScrollProvider<D> dataProvider) {
        super(Element.newPFlowPanel());
        this.dataProvider = dataProvider;

        widget.addStyleName("is-viewport");
        // Container
        container = Element.newPFlowPanel();
        container.addStyleName("is-container");
        widget.add(container);

        // HANDLER
        this.setTerminalHandler((event) -> {
            final JsonObject jsonObj = event.getData();
            beginIndex = jsonObj.getInt("beginIndex");
            System.err.println(beginIndex);
            maxVisibleItem = jsonObj.getInt("maxVisibleItem");
            System.err.println(maxVisibleItem);
            draw();
        });
    }

    /**
     * calling start method js and setting the size
     */
    public void start() {
        callTerminalMethod("start");
        setSize(dataProvider.getSize());

    }

    /**
     * calling stop method js
     */

    public void stop() {
        callTerminalMethod("stop");
    }

    /**
     * calling setSize method js
     */
    public void setSize(final long l) {
        this.size = l;
        callTerminalMethod("setSize", String.valueOf(l));
        draw();
    }

    /**
     * Add items in DOM by using draw(D), maxVisibleItem and beginIndex.
     */
    public void draw() {
        i = 0;
        final int maxItem = (int) Math.min(maxVisibleItem, size);
        final List<D> data = dataProvider.getData(beginIndex, maxItem);
        for (; i < data.size(); i++) {
            draw(data.get(i));
        }
        for (; i < items.size(); i++) {
            items.get(i).setVisible(false);
        }
        callTerminalMethod("onDraw");
    }

    private void draw(final D data) {
        IsPWidget item;
        if (items.size() <= i) {
            // create row if needed
            item = dataProvider.buildItem(data);
            items.add(item.asWidget());
            item.asWidget().addStyleName("item");
            container.add(item);
        } else {
            item = items.get(i);
            item.asWidget().setVisible(true);
            dataProvider.updateItem(i, data, item);
        }
    }

}
