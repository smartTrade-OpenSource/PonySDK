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

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.IsPWidget;
import com.ponysdk.core.ui.basic.PAddOnComposite;
import com.ponysdk.core.ui.basic.PButton;
import com.ponysdk.core.ui.basic.PFlowPanel;
import com.ponysdk.core.ui.basic.PPanel;
import com.ponysdk.core.ui.basic.PSimplePanel;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;
import com.ponysdk.core.ui.infinitescroll.InfiniteScrollAddon;

/**
 *
 */
public class ListBox<D> extends PAddOnComposite<PPanel> {

    private final InfiniteScrollAddon<D, IsPWidget> infiniteScroll;
    private final PSimplePanel overlay;
    private final PButton button;
    private final PTextBox textBox;
    private final PFlowPanel container;

    public ListBox(final InfiniteScrollAddon infiniteScrollAddon) {
        super(Element.newPFlowPanel());
        this.infiniteScroll = infiniteScrollAddon;
        overlay = Element.newPSimplePanel();
        container = Element.newPFlowPanel();
        textBox = Element.newPTextBox();
        button = Element.newPButton();
        overlay.setStyleProperty("overflow", "auto");
        overlay.setStyleProperty("position", "fixed");
        widget.add(overlay);
        overlay.addDomHandler((PClickHandler) e -> container.setVisible(false), PClickEvent.TYPE);
        overlay.setVisible(false);
        widget.add(button);
        infiniteScroll.setStyleProperty("height", "500px");
        infiniteScroll.setStyleProperty("width", "250px");
        container.add(textBox);
        container.add(infiniteScroll);
        widget.add(container);
        button.addClickHandler(event -> container.setVisible(!container.isVisible()));
        //textBox.addKeyUpHandler(event -> checkBoxPanel.filter(textBox.getText()));
        infiniteScroll.refresh();

    }

}
