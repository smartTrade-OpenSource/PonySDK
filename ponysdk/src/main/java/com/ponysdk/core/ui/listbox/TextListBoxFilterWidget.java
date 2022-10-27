/*
 * Copyright (c) 2022 PonySDK
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

import java.util.function.Consumer;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.PWidget.TabindexMode;

public class TextListBoxFilterWidget implements ListBoxFilterWidget {

    private final PTextBox textBox;

    public TextListBoxFilterWidget() {
        this.textBox = Element.newPTextBox();
    }

    @Override
    public String getFilter() {
        return textBox.getText();
    }

    @Override
    public void setFilter(final String filter) {
        textBox.setText(filter);
    }

    @Override
    public void setPlaceholder(final String placeholder) {
        textBox.setPlaceholder(placeholder);
    }

    @Override
    public void setTabindex(final TabindexMode tabindexMode) {
        textBox.setTabindex(tabindexMode);
    }

    @Override
    public void focus() {
        textBox.focus();
    }

    @Override
    public void addKeyDownAction(final Consumer<Integer> keyCodeAction) {
        textBox.addKeyDownHandler(e -> keyCodeAction.accept(e.getKeyCode()));
    }

    @Override
    public void addKeyUpAction(final Consumer<Integer> keyCodeAction) {
        textBox.addKeyUpHandler(e -> keyCodeAction.accept(e.getKeyCode()));
    }

    @Override
    public PWidget asWidget() {
        return textBox;
    }
}
