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

package com.ponysdk.ui.terminal.ui.widget.filter;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.DropEvent;
import com.google.gwt.event.dom.client.DropHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Decorator use to filter keyboard inputs on a text box. All keys that don't match the specified regular
 * expression is not set in the text box.
 */
public class TextBoxFilterDecorator implements KeyPressHandler, FocusHandler, KeyDownHandler, DropHandler {

    private final TextBox textBox;
    private RegExp regExp;

    /**
     * Creates a text box filter decorator that wraps a text box element.
     * 
     * @param textBox
     *            the wrapped text box.
     */
    public TextBoxFilterDecorator(final TextBox textBox) {
        this.textBox = textBox;
        this.textBox.addKeyDownHandler(this);
        this.textBox.addKeyPressHandler(this);
        this.textBox.addDropHandler(this);
    }

    /**
     * Sets the pattern filter.
     * 
     * @param pattern
     *            the pattern filter to use.
     */
    public void setFilter(final String pattern) {
        regExp = RegExp.compile(pattern);
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {
        if (!match(event.getCharCode())) cancelKey();
    }

    @Override
    public void onKeyDown(final KeyDownEvent event) {
        // Nothing to do
    }

    @Override
    public void onFocus(final FocusEvent event) {
        // Nothing to do
    }

    @Override
    public void onDrop(final DropEvent event) {
        filterText();
    }

    /**
     * Fired whenever a browser event is received on the text box.
     * 
     * @param event
     *            the event received.
     */
    public void onBrowserEvent(final Event event) {
        switch (event.getTypeInt()) {
            case Event.ONPASTE:
                filterText();
                break;
        }
    }

    // Delegates

    /**
     * Cancels keyboard event handled on the text box.
     */
    public void cancelKey() {
        textBox.cancelKey();
    }

    /**
     * Gets this text box's text.
     * 
     * @return the text of the text box.
     */
    public String getText() {
        return textBox.getText();
    }

    /**
     * Sets the text in the text box.
     * 
     * @param text
     *            the text to set.
     */
    public void setText(final String text) {
        textBox.setText(text);
        filterText();
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
                final String pasteText = textBox.getText();
                final StringBuilder filteredText = new StringBuilder();
                for (final char c : pasteText.toCharArray()) {
                    if (match(c)) filteredText.append(c);
                }
                textBox.setText(filteredText.toString());
            }
        });
    }
}
