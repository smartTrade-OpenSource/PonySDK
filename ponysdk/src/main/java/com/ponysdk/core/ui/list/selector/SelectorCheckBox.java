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

import com.ponysdk.core.ui.basic.PCheckBox;
import com.ponysdk.core.ui.basic.event.PValueChangeEvent;

public class SelectorCheckBox<T> extends PCheckBox implements Selectable<T> {

    private final List<SelectableListener> listeners = new ArrayList<>();

    @Override
    public void select() {
        setValue(true);
        onValueChange(new PValueChangeEvent<>(this, true));
    }

    @Override
    public void unselect() {
        setValue(false);
        onValueChange(new PValueChangeEvent<>(this, false));
    }

    @Override
    public T getSelectedData() {
        if (getValue()) return (T) data;
        else return null;
    }

    public void onCheck() {
        listeners.forEach(SelectableListener::onSelect);
    }

    public void onUncheck() {
        listeners.forEach(SelectableListener::onUnselect);
    }

    @Override
    public void addSelectableListener(final SelectableListener selectableListener) {
        listeners.add(selectableListener);
    }

}