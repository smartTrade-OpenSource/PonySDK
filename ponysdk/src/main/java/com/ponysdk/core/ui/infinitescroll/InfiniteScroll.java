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
 *
 */

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PPanel;
import com.ponysdk.core.ui.basic.PWidget;

public class InfiniteScroll<D> extends PAddOnComposite<PPanel> {

    // visual elements
    private final PPanel body;
    // rows
    private final List<PWidget> rows = new ArrayList<>();

    // used for drawing
    private int i = 0;
    private int beginIndex = 0;
    private int maxVisibleItem = 0;

    // TXN
    private final boolean drawScheduled = false;

    private final InfiniteScrollProvider<D> dataProvider;

    private boolean started;
    private boolean shouldDrawOnStart;
    private long size;

    public InfiniteScroll(final InfiniteScrollProvider<D> dataProvider) {
        super(Element.newPFlowPanel());
        this.dataProvider = dataProvider;

        // Container
        final PPanel container = Element.newPFlowPanel();
        container.addStyleName("container");
        widget.add(container);

        // BODY
        body = Element.newPFlowPanel();
        body.addStyleName("item-body");
        container.add(body);

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

    public void start() {
        callTerminalMethod("start");
        setSize(dataProvider.getSize());
        started = true;
    }

    public void stop() {
        callTerminalMethod("stop");
        started = false;
    }

    public void setSize(final long l) {
        this.size = l;
        callTerminalMethod("setSize", String.valueOf(l));
        draw();
    }

    public void draw() {
        shouldDrawOnStart = false;
        i = 0;
        final int maxItem = (int) Math.min(maxVisibleItem, size);
        final List<D> data = dataProvider.getData(beginIndex, maxItem);
        for (; i < data.size(); i++) {
            draw(data.get(i));
        }
        for (; i < rows.size(); i++) {
            rows.get(i).setVisible(false);
        }
        callTerminalMethod("onDraw");
    }

    private void draw(final D data) {
        IsPWidget row;
        if (rows.size() <= i) {
            // create row if needed
            row = dataProvider.buildRow(data);
            rows.add(row.asWidget());
            row.asWidget().addStyleName("item");
            row.asWidget().setStyleProperty("border", "3px solid black");
            row.asWidget().setStyleProperty("padding", "3px");
            row.asWidget().setStyleProperty("margin", "3px");
            body.add(row);
        } else {
            row = rows.get(i);
            row.asWidget().setVisible(true);
            dataProvider.updateRow(i, data, row);
        }
    }

}
