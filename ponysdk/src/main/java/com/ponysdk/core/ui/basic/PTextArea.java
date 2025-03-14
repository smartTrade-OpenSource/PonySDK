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
import com.ponysdk.core.model.WidgetType;

/**
 * A text box that allows multiple lines of text to be entered.
 * <h2>CSS Style Rules</h2>
 * <ul class='css'>
 * <li>.gwt-TextArea { primary style }</li>
 * <li>.gwt-TextArea-readonly { dependent style set
 * when the text area is read-only }</li>
 * </ul>
 */
public class PTextArea extends PTextBoxBase {

    private int visibleLines = 5;
    private int characterWidth = 25;

    protected PTextArea() {
        super();
    }

    protected PTextArea(final String text) {
        super(text);
    }

    @Override
    protected WidgetType getWidgetType() {
        return WidgetType.TEXT_AREA;
    }

    public int getVisibleLines() {
        return visibleLines;
    }

    public void setVisibleLines(final int visibleLines) {
        if (Objects.equals(this.visibleLines, visibleLines)) return;
        this.visibleLines = visibleLines;
        saveUpdate(ServerToClientModel.VISIBLE_LINES, visibleLines);
    }

    public int getCharacterWidth() {
        return characterWidth;
    }

    public void setCharacterWidth(final int characterWidth) {
        if (Objects.equals(this.characterWidth, characterWidth)) return;
        this.characterWidth = characterWidth;
        saveUpdate(ServerToClientModel.CHARACTER_WIDTH, characterWidth);
    }

}
