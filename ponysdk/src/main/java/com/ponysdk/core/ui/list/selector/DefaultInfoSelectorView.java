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

import com.ponysdk.core.model.PHorizontalAlignment;
import com.ponysdk.core.ui.basic.*;
import com.ponysdk.core.ui.i18n.PString;
import com.ponysdk.core.util.SetUtils;

import java.util.Set;

public class DefaultInfoSelectorView extends PHorizontalPanel implements SelectorView {

    final PLabel numberOfSelectedMessage = Element.newPLabel();
    final PAnchor selectAllAnchor = Element.newPAnchor();
    final PAnchor selectNoneAnchor = Element.newPAnchor();

    private final Set<SelectorViewListener> selectorViewListeners = SetUtils.newArraySet();

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
        selectorViewListeners.add(selectorViewListener);
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
