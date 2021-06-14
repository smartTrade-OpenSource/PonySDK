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

package com.ponysdk.core.ui.listbox;

import java.util.List;
import java.util.function.Consumer;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.infinitescroll.InfiniteScrollAddon;
import com.ponysdk.core.ui.infinitescroll.InfiniteScrollProvider;

/**
 *
 */
public class ListBox<D, W extends ListBoxItem<D>> extends PAddOnComposite<PPanel> {

    private final InfiniteScrollAddon<D, W> infiniteScroll;
    private final ListBoxProvider<D, W> dataProvider;
    private final PSimplePanel overlay;
    private final PButton button;
    private final PTextBox textBox;
    private final PFlowPanel container;

    private class ListBoxISProvider implements InfiniteScrollProvider<D, W> {

        @Override
        public void getData(final int beginIndex, final int maxSize, final Consumer<List<D>> callback) {
            dataProvider.getData(beginIndex, maxSize, callback);
        }

        @Override
        public void getFullSize(final Consumer<Integer> callback) {
            dataProvider.getFullSize(callback);
        }

        @Override
        public W handleUI(final int index, final D data, final W oldWidget) {
            final W newWidget = dataProvider.handleUI(index, data, oldWidget);
            if (newWidget != oldWidget) {
                newWidget.addSelectionHandler(() -> this.onSelectItem(newWidget.getValue()));
            }
            return newWidget;
        }

        private void onSelectItem(final D value) {
            button.setText(value.toString());
        }

        @Override
        public void addHandler(final Consumer<D> handler) {
            dataProvider.addHandler(handler);
        }

    }

    public ListBox(final ListBoxProvider<D, W> dataProvider) {
        super(Element.newPFlowPanel());
        this.dataProvider = dataProvider;
        this.infiniteScroll = new InfiniteScrollAddon<>(new ListBoxISProvider());
        overlay = Element.newPSimplePanel();
        container = Element.newPFlowPanel();
        textBox = Element.newPTextBox();
        button = Element.newPButton();
        overlay.setStyleProperty("overflow", "auto");
        overlay.setStyleProperty("position", "fixed");
        overlay.setStyleProperty("top", "0");
        overlay.setStyleProperty("left", "0");
        overlay.setStyleProperty("bottom", "0");
        overlay.setStyleProperty("right", "0");
        overlay.setStyleProperty("z-index", "-1");
        container.setStyleProperty("position", "absolute");
        container.setStyleProperty("max-height", "170px");
        container.setStyleProperty("border", "5px solid black");
        container.setStyleProperty("display", "flex");
        container.setStyleProperty("overflow", "hidden");
        container.setStyleProperty("flex-direction", "column");
        container.setStyleProperty("z-index", "1000");
        infiniteScroll.setStyleProperty("z-index", "1");
        container.setStyleProperty("font-family", "webappsdk");
        container.add(overlay);
        overlay.addDomHandler((PClickHandler) e -> {
            container.setVisible(false);

        }, PClickEvent.TYPE);
        container.setVisible(false);
        widget.add(button);

        infiniteScroll.setStyleProperty("width", "168px");
        infiniteScroll.refresh();
        container.add(textBox);
        container.add(infiniteScroll);
        widget.add(container);
        button.addClickHandler(event -> {
            container.setVisible(!container.isVisible());
            if (container.isVisible()) {
                //TODO use CSS
                container.setStyleProperty("display", "block");
                container.setStyleProperty("display", "flex");
            }
            this.dataProvider.setFilter("");
            textBox.setText(null);
            this.infiniteScroll.setmaxItemVisible();
            this.infiniteScroll.setScrollTop();
            this.infiniteScroll.refresh();

        });
        textBox.addKeyUpHandler(event -> filter(textBox.getText()));

    }

    private void filter(final String filter) {
        if (filter == null || filter.isEmpty()) {
            dataProvider.setFilter(null);

        } else {
            final String text = filter.toLowerCase();
            dataProvider.setFilter(text);
        }
        this.infiniteScroll.setmaxItemVisible();
        this.infiniteScroll.setScrollTop();
        this.infiniteScroll.refresh();
    }

}
