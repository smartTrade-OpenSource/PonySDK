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

import java.util.ArrayList;
import java.util.List;

import com.ponysdk.core.ui.basic.PWidget;

public class CompositeSelectorView implements SelectorView {

    private final List<SelectorView> views = new ArrayList<>();

    public CompositeSelectorView(final SelectorView... views) {
        for (final SelectorView selectorView : views) {
            this.views.add(selectorView);
        }
    }

    @Override
    public PWidget asWidget() {
        // TODO
        return views.get(0).asWidget();
    }

    @Override
    public void addSelectorViewListener(final SelectorViewListener selectorViewListener) {
        for (final SelectorView view : views) {
            view.addSelectorViewListener(selectorViewListener);
        }
    }

    @Override
    public void update(final SelectionMode selectionMode, final int numberOfSelectedItems, final int fullSize, final int pageSize) {
        for (final SelectorView view : views) {
            view.update(selectionMode, numberOfSelectedItems, fullSize, pageSize);
        }
    }

}
