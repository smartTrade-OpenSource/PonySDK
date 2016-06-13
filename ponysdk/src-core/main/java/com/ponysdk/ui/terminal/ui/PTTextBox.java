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

package com.ponysdk.ui.terminal.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.DropHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TextBox;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.UIService;
import com.ponysdk.ui.terminal.instruction.PTInstruction;
import com.ponysdk.ui.terminal.ui.widget.mask.TextBoxMaskedDecorator;

public class PTTextBox extends PTTextBoxBase<TextBox> implements KeyPressHandler, DropHandler {

    private TextBoxMaskedDecorator maskDecorator;
    private RegExp regExp;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new ExtendedTextBox());
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.TEXT)) {
            uiObject.setText(update.getString(PROPERTY.TEXT));
        } else if (update.containsKey(PROPERTY.VALUE)) {
            uiObject.setValue(update.getString(PROPERTY.VALUE));
        } else if (update.containsKey(PROPERTY.VISIBLE_LENGTH)) {
            uiObject.setVisibleLength(update.getInt(PROPERTY.VISIBLE_LENGTH));
        } else if (update.containsKey(PROPERTY.MAX_LENGTH)) {
            uiObject.setMaxLength(update.getInt(PROPERTY.MAX_LENGTH));
        } else if (update.containsKey(PROPERTY.MASK)) {
            final boolean showMask = update.getBoolean(PROPERTY.VISIBILITY);
            final String mask = update.getString(PROPERTY.MASK);
            final String replace = update.getString(PROPERTY.REPLACEMENT_STRING);
            if (maskDecorator == null) maskDecorator = new TextBoxMaskedDecorator(cast());
            maskDecorator.setMask(mask, showMask, replace.charAt(0));
        } else if (update.containsKey(PROPERTY.FILTER)) {
            regExp = RegExp.compile(update.getString(PROPERTY.FILTER));
            uiObject.addKeyPressHandler(this);
            uiObject.addDropHandler(this);
            uiObject.sinkEvents(Event.ONPASTE);
        } else {
            super.update(update, uiService);
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
        if (regExp == null) return true;
        return regExp.exec(String.valueOf(key)) != null;
    }

    /**
     * Filters text set in the text box in a deferred action.
     */
    private void filterText() {
        if (regExp == null) return;
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
                final String pasteText = uiObject.getText();
                final StringBuilder filteredText = new StringBuilder();
                for (final char c : pasteText.toCharArray()) {
                    if (match(c)) filteredText.append(c);
                }
                uiObject.setText(filteredText.toString());
            }
        });
    }

    //

    private class ExtendedTextBox extends TextBox {

        @Override
        public void onBrowserEvent(final Event event) {
            super.onBrowserEvent(event);
            switch (event.getTypeInt()) {
                case Event.ONPASTE:
                    filterText();
                    break;
            }
        }
    }
}
