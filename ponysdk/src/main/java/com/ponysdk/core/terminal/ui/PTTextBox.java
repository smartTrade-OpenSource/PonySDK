/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.terminal.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.DropHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TextBox;
import com.ponysdk.core.model.ServerToClientModel;
import com.ponysdk.core.terminal.model.BinaryModel;
import com.ponysdk.core.terminal.model.ReaderBuffer;
import com.ponysdk.core.terminal.ui.widget.mask.TextBoxMaskedDecorator;

public class PTTextBox extends PTTextBoxBase<TextBox> implements KeyPressHandler, DropHandler {

    private TextBoxMaskedDecorator maskDecorator;
    private RegExp regExp;

    @Override
    protected TextBox createUIObject() {
        return new TextBox() {

            @Override
            public int getTabIndex() {
                final int tabIndex = super.getTabIndex();
                return tabIndex == -1 ? -2 : tabIndex;
            }

            @Override
            public void onBrowserEvent(final Event event) {
                super.onBrowserEvent(event);
                if (Event.ONPASTE == event.getTypeInt() && enabled) {
                    filterText();
                    if (handlePasteEnabled) Scheduler.get().scheduleDeferred(() -> sendPasteEvent(event));
                }
            }
        };
    }

    @Override
    public boolean update(final ReaderBuffer buffer, final BinaryModel binaryModel) {
        final ServerToClientModel model = binaryModel.getModel();
        if (ServerToClientModel.VISIBLE_LENGTH == model) {
            uiObject.setVisibleLength(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.MAX_LENGTH == model) {
            uiObject.setMaxLength(binaryModel.getIntValue());
            return true;
        } else if (ServerToClientModel.MASK == model) {
            final String mask = binaryModel.getStringValue();
            // ServerToClientModel.VISIBILITY
            final boolean showMask = buffer.readBinaryModel().getBooleanValue();
            // ServerToClientModel.REPLACEMENT_STRING
            final String replace = buffer.readBinaryModel().getStringValue();
            if (maskDecorator == null) maskDecorator = new TextBoxMaskedDecorator(uiObject);
            maskDecorator.setMask(mask, showMask, replace.charAt(0));
            return true;
        } else if (ServerToClientModel.REGEX_FILTER == model) {
            regExp = RegExp.compile(binaryModel.getStringValue());
            uiObject.addKeyPressHandler(this);
            uiObject.addDropHandler(this);
            uiObject.sinkEvents(Event.ONPASTE);
            return true;
        } else {
            return super.update(buffer, binaryModel);
        }
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {
        if (!match(event.getCharCode())) uiObject.cancelKey();
    }

    @Override
    public void onDrop(final DropEvent event) {
        filterText();
    }

    /**
     * Indicates if the character typed in the text box matches the regular expression.
     *
     * @param key
     *            the key character typed in the text box.
     * @return true if the character matches the regular expression, false otherwise.
     */
    private boolean match(final char key) {
        return regExp == null || regExp.exec(String.valueOf(key)) != null;
    }

    /**
     * Filters text set in the text box in a deferred action.
     */
    private void filterText() {
        if (regExp == null) return;
        Scheduler.get().scheduleDeferred(() -> {
            final String pasteText = uiObject.getText();
            final StringBuilder filteredText = new StringBuilder();
            for (final char c : pasteText.toCharArray()) {
                if (match(c)) filteredText.append(c);
            }
            uiObject.setText(filteredText.toString());
        });
    }

}
