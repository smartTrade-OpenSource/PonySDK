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

package com.ponysdk.core.ui.basic;

import java.util.Objects;

import com.ponysdk.core.model.ServerToClientModel;

/**
 * A standard single-line text box.
 * <h2>CSS Style Rules</h2>
 * <ul class='css'>
 * <li>.gwt-TextBox { primary style }</li>
 * <li>.gwt-TextBox-readonly { dependent style set when the text box is
 * read-only }</li>
 * </ul>
 */
public class PTextBox extends PTextBoxBase {

    private int maxLength;
    private int visibleLength;

    protected PTextBox() {
        this(null);
    }

    protected PTextBox(final String text) {
        super(text);
    }

    /**
     * Gets the maximum allowable length of the text box.
     *
     * @return the maximum length, in characters
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Sets the maximum allowable length of the text box.
     *
     * @param length
     *            the maximum length, in characters
     */
    public void setMaxLength(final int length) {
        if (Objects.equals(this.maxLength, length)) return;
        this.maxLength = length;
        saveUpdate(ServerToClientModel.MAX_LENGTH, length);
    }

    /**
     * Gets the number of visible characters in the text box.
     *
     * @return the number of visible characters
     */
    public int getVisibleLength() {
        return visibleLength;
    }

    /**
     * Sets the number of visible characters in the text box.
     *
     * @param length
     *            the number of visible characters
     */
    public void setVisibleLength(final int length) {
        if (Objects.equals(this.visibleLength, length)) return;
        this.visibleLength = length;
        saveUpdate(ServerToClientModel.VISIBLE_LENGTH, visibleLength);
    }

    public void applyMask(final String mask) {
        applyMask(mask, true, " ");
    }

    /**
     * Apply a mask to the textbox. Value get/set from the textbox will have the
     * mask. <br>
     * Example: ({{000}}) {{000}}.{{0000}}
     *
     * @param pattern
     *            {{[0A]+}}
     * @param showMask
     *            true to display the mask when input is empty
     * @param freeSymbol
     *            replacement char when there is no input yet
     */
    public void applyMask(final String pattern, final boolean showMask, final String freeSymbol) {
        saveUpdate(writer -> {
            writer.write(ServerToClientModel.MASK, pattern);
            writer.write(ServerToClientModel.VISIBILITY, showMask);
            writer.write(ServerToClientModel.REPLACEMENT_STRING, freeSymbol);
        });
    }

    /**
     * Sets a filter to the textbox. Value sets to the textbox will be filtered. <br>
     * Example: ([a-zA-Z0-9])
     *
     * @param regExp
     *            the regular expression to use as filter.
     */
    public void setFilter(final String regExp) {
        saveUpdate(ServerToClientModel.REGEX_FILTER, regExp);
    }

}
