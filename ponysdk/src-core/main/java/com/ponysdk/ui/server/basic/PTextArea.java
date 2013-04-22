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

package com.ponysdk.ui.server.basic;

import com.ponysdk.core.stm.TxnInteger;
import com.ponysdk.core.stm.TxnObject;
import com.ponysdk.ui.terminal.Dictionnary.PROPERTY;
import com.ponysdk.ui.terminal.WidgetType;

/**
 * A text box that allows multiple lines of text to be entered.
 * <p>
 * <img class='gallery' src='doc-files/PTextArea.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-TextArea { primary style }</li>
 * <li>.gwt-TextArea-readonly { dependent style set when the text area is read-only }</li>
 * </ul>
 */
public class PTextArea extends PTextBoxBase {

    private final TxnInteger visibleLines = new TxnInteger(0);
    private final TxnInteger characterWidth = new TxnInteger(0);

    public PTextArea() {
        this(null);
    }

    public PTextArea(final String text) {
        super();
        visibleLines.set(5);// must be the default value ?
        characterWidth.set(25);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.TEXT_AREA;
    }

    public int getVisibleLines() {
        return visibleLines.get();
    }

    public void setVisibleLines(final int visibleLines) {
        this.visibleLines.set(visibleLines);
    }

    public int getCharacterWidth() {
        return characterWidth.get();
    }

    public void setCharacterWidth(final int characterWidth) {
        this.characterWidth.set(characterWidth);
    }

    @Override
    public void beforeFlush(final TxnObject<?> txnObject) {
        if (txnObject == visibleLines) {
            saveUpdate(PROPERTY.VISIBLE_LINES, visibleLines.get());
        } else if (txnObject == characterWidth) {
            saveUpdate(PROPERTY.CHARACTER_WIDTH, characterWidth.get());
        } else {
            super.beforeFlush(txnObject);
        }
    }
}
