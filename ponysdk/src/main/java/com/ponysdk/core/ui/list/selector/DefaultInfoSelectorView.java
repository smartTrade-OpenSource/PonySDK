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

package com.ponysdk.core.ui.list.selector;

import com.ponysdk.core.internalization.PString;
import com.ponysdk.core.tools.ListenerCollection;
import com.ponysdk.core.ui.basic.PAnchor;
import com.ponysdk.core.ui.basic.PHorizontalPanel;
import com.ponysdk.core.ui.basic.PLabel;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.alignment.PHorizontalAlignment;
import com.ponysdk.core.ui.basic.event.PClickEvent;
import com.ponysdk.core.ui.basic.event.PClickHandler;

public class DefaultInfoSelectorView extends PHorizontalPanel implements SelectorView {

    private final ListenerCollection<SelectorViewListener> selectorViewListeners = new ListenerCollection<>();

    final PLabel numberOfSelectedMessage = new PLabel();
    final PAnchor selectAllAnchor = new PAnchor();
    final PAnchor selectNoneAnchor = new PAnchor();

    public DefaultInfoSelectorView() {
        setHorizontalAlignment(PHorizontalAlignment.ALIGN_CENTER);
        setStyleName("pony-ComplexList-OptionSelectionPanel");
        setVisible(false);
        selectAllAnchor.addClickHandler(event -> {
            for (final SelectorViewListener listener : selectorViewListeners) {
                listener.onSelectionChange(SelectionMode.FULL);
            }
        });
        selectNoneAnchor.addClickHandler(event -> {
            for (final SelectorViewListener listener : selectorViewListeners) {
                listener.onSelectionChange(SelectionMode.NONE);
            }
        });
    }

    @Override
    public PWidget asWidget() {
        return this;
    }

    @Override
    public void addSelectorViewListener(final SelectorViewListener selectorViewListener) {
        selectorViewListeners.register(selectorViewListener);
    }

    @Override
    public void update(final SelectionMode selectionMode, final int numberOfSelectedItems, final int fullSize, final int pageSize) {
        switch (selectionMode) {
            case FULL:
                setVisible(true);
                clear();
                add(numberOfSelectedMessage);

                numberOfSelectedMessage.setText(PString.get("list.selector.allitemselected", numberOfSelectedItems));
                if (numberOfSelectedItems > pageSize) {
                    selectNoneAnchor.setText(PString.get("list.selector.clear"));
                    add(selectNoneAnchor);
                }
                break;
            case NONE:
                if (isVisible()) setVisible(false);
                break;
            case PAGE:
                setVisible(true);
                clear();
                add(numberOfSelectedMessage);
                numberOfSelectedMessage.setText(PString.get("list.selector.pageitemselected", numberOfSelectedItems));

                final int itemsLeft = fullSize - numberOfSelectedItems;
                selectAllAnchor.setText(PString.get("list.selector.selectitemleft", itemsLeft));
                add(selectAllAnchor);
                break;
            case PARTIAL:
                if (isVisible()) setVisible(false);
                break;
        }
    }

}
