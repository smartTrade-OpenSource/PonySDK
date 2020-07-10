/*
 * Copyright (c) 2019 PonySDK
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

package com.ponysdk.core.ui.datagrid2.cell;

import java.util.function.BiConsumer;

import com.ponysdk.core.ui.basic.Element;
import com.ponysdk.core.ui.basic.PTextBox;
import com.ponysdk.core.ui.basic.PWidget;
import com.ponysdk.core.ui.basic.event.PBlurEvent;
import com.ponysdk.core.ui.basic.event.PBlurHandler;
import com.ponysdk.core.ui.model.PEventType;
import com.ponysdk.core.ui.model.PKeyCodes;

/**
 * @author mbagdouri
 */
public class TextBoxExtendedCell<V> implements ExtendedCell<V>, PBlurHandler {

    private final PTextBox textBox = Element.newPTextBox();
    private ExtendedCellController<V> extendedCellController;
    private boolean focused = false;

    public TextBoxExtendedCell(final String text, final BiConsumer<V, String> columnEditFn, final int width) {
        textBox.setText(text);
        textBox.addBlurHandler(e -> {
            if (extendedCellController == null) return;
            extendedCellController.cancelExtendedMode();
        });
        textBox.addClickHandler(e -> {

        });
        textBox.stopEvent(PEventType.ONCLICK);
        textBox.setEnabledOnRequest(true);
        textBox.addKeyUpHandler(e -> {
            if (extendedCellController == null) return;
            if (e.getKeyCode() == PKeyCodes.ENTER.getCode()) {
                extendedCellController.cancelExtendedMode();
                extendedCellController.updateValue((v) -> columnEditFn.accept(v, textBox.getText()));
            } else if (e.getKeyCode() == PKeyCodes.ESCAPE.getCode()) {
                extendedCellController.cancelExtendedMode();
            }
        });
        textBox.addFocusHandler(e -> {
            focused = true;
        });
        textBox.setWidth(width + "px");
    }

    @Override
    public PWidget asWidget() {
        return textBox;
    }

    @Override
    public void setController(final ExtendedCellController<V> extendedCellController) {
        this.extendedCellController = extendedCellController;
    }

    @Override
    public void select() {
    }

    @Override
    public void unselect() {
    }

    @Override
    public void setValue(final V v) {
    }

    @Override
    public void beforeRemove() {
        textBox.removeDomHandler(this, PBlurEvent.TYPE);
    }

    @Override
    public void afterAdd() {
        if (focused) textBox.focusPreventScroll();
        textBox.addBlurHandler(this);
    }

    @Override
    public void onBlur(final PBlurEvent event) {
        focused = false;
    }

}