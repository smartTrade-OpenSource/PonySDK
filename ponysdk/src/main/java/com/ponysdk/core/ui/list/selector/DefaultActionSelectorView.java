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
import com.ponysdk.core.ui.basic.PMenuBar;
import com.ponysdk.core.ui.basic.PMenuItem;
import com.ponysdk.core.ui.basic.PWidget;

public class DefaultActionSelectorView extends PMenuBar implements SelectorView {

    private final ListenerCollection<SelectorViewListener> selectorViewListeners = new ListenerCollection<>();
    private final PMenuItem selectAllMenuItem;
    private final PMenuItem selectNoneMenuItem;

    public DefaultActionSelectorView() {
        final PMenuBar menuBarAction = new PMenuBar(true);
        addItem("", menuBarAction);
        selectAllMenuItem = new PMenuItem(PString.get("list.selector.all"));
        selectAllMenuItem.setCommand(getSelectAllCommand());
        addItem(selectAllMenuItem);
        selectNoneMenuItem = new PMenuItem(PString.get("list.selector.none"));
        selectNoneMenuItem.setCommand(getSelectNoneCommand());

        menuBarAction.addItem(selectAllMenuItem);
        menuBarAction.addItem(selectNoneMenuItem);
    }

    @Override
    public PWidget asWidget() {
        return this;
    }

    private Runnable getSelectAllCommand() {
        return () -> {
            for (final SelectorViewListener selectorListener : selectorViewListeners) {
                selectorListener.onSelectionChange(SelectionMode.PAGE);
            }
        };
    }

    private Runnable getSelectNoneCommand() {
        return () -> {
            for (final SelectorViewListener selectorListener : selectorViewListeners) {
                selectorListener.onSelectionChange(SelectionMode.NONE);
            }
        };
    }

    @Override
    public void addSelectorViewListener(final SelectorViewListener selectorViewListener) {
        selectorViewListeners.register(selectorViewListener);
    }

    @Override
    public void update(final SelectionMode selectionMode, final int numberOfSelectedItems, final int fullSize, final int pageSize) {
        // Nothing
    }

}
