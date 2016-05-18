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

/**
 * A single-line text box able to filter keyboard inputs. All keys that don't match the specified regular
 * expression is not set in the text box.
 */
public class PTFilterTextBox extends PTTextBox implements KeyPressHandler, DropHandler {

    private RegExp regExp;

    @Override
    public void create(final PTInstruction create, final UIService uiService) {
        init(create, uiService, new FilterTextBox());
        uiObject.addKeyPressHandler(this);
        uiObject.addDropHandler(this);
    }

    @Override
    public void update(final PTInstruction update, final UIService uiService) {
        if (update.containsKey(PROPERTY.FILTER)) {
            regExp = RegExp.compile(update.getString(PROPERTY.FILTER));
        } else {
            super.update(update, uiService);
        }
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {
        if (!match(event.getCharCode())) cancelKey();
    }

    @Override
    public void onDrop(final DropEvent event) {
        filterText();
    }

    // Delegates

    /**
     * Cancels keyboard event handled on the text box.
     */
    public void cancelKey() {
        uiObject.cancelKey();
    }

    //

    /**
     * Indicates if the character typed in the text box matches the regular expression.
     *
     * @param key
     *            the key character typed in the text box.
     * @return true if the character matches the regular expression, false otherwise.
     */
    private boolean match(final char key) {
        return regExp.exec(String.valueOf(key)) != null;
    }

    /**
     * Filters text set in the text box in a deferred action.
     */
    private void filterText() {
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

    private class FilterTextBox extends TextBox {

        public FilterTextBox() {
            super();
            sinkEvents(Event.ONPASTE);
        }

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
